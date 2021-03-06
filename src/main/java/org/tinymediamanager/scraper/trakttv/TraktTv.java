/*
 * Copyright 2012 - 2015 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.scraper.trakttv;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.util.ApiKey;

import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.BaseEpisode;
import com.uwetrottmann.trakt.v2.entities.BaseMovie;
import com.uwetrottmann.trakt.v2.entities.BaseSeason;
import com.uwetrottmann.trakt.v2.entities.BaseShow;
import com.uwetrottmann.trakt.v2.entities.MovieIds;
import com.uwetrottmann.trakt.v2.entities.ShowIds;
import com.uwetrottmann.trakt.v2.entities.SyncEpisode;
import com.uwetrottmann.trakt.v2.entities.SyncErrors;
import com.uwetrottmann.trakt.v2.entities.SyncItems;
import com.uwetrottmann.trakt.v2.entities.SyncMovie;
import com.uwetrottmann.trakt.v2.entities.SyncResponse;
import com.uwetrottmann.trakt.v2.entities.SyncSeason;
import com.uwetrottmann.trakt.v2.entities.SyncShow;
import com.uwetrottmann.trakt.v2.entities.SyncStats;
import com.uwetrottmann.trakt.v2.enums.Extended;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;

import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Sync your collection and watched status with Trakt.tv<br>
 * Using best practice 2-way-sync according to http://trakt.tv/api-docs/sync<br>
 * https://github.com/UweTrottmann/trakt-java
 * 
 * @author Myron Boyle
 * 
 */

public class TraktTv {
  private static final String      CLIENT_ID    = "a8e7e30fd7fd3f397b6e079f9f023e790f9cbd80a2be57c104089174fa8c6d89";

  private static final Logger      LOGGER       = LoggerFactory.getLogger(TraktTv.class);
  private static final TraktV2     TRAKT        = createTraktApi();
  private static TraktTv           instance;
  private static MediaProviderInfo providerInfo = new MediaProviderInfo(Constants.TRAKT, "Trakt.tv",
      "Scraper for Trakt.tv; yes, we can scraper here too :)");

  private SyncResponse             response;

  private static TraktV2 createTraktApi() {
    TraktV2 api = new TraktV2();
    api.setApiKey(CLIENT_ID);
    if (LOGGER.isTraceEnabled()) {
      api.setIsDebug(true);
    }

    return api;
  }

  public static synchronized TraktTv getInstance() {
    if (instance == null) {
      instance = new TraktTv();
    }
    return instance;
  }

  public TraktTv() {
  }

  public static Map<String, String> authenticateViaPin(String pin) throws Exception {
    Map<String, String> result = new HashMap<>();

    OAuthAccessTokenResponse response = TraktV2.getAccessToken(CLIENT_ID,
        ApiKey.decryptApikey("VD2h4jmnrrYWnP1Nk49UtTNRILiWsuelJKdza7DAw+ROh1wtVf2U6PQScm7QWCOTsxN0K3QluIykKs2ZT1af1GcPz1401005bDBDss1Pz2c="),
        "urn:ietf:wg:oauth:2.0:oob", pin);

    // get tokens
    String accessToken = response.getAccessToken();
    String refreshToken = response.getRefreshToken();
    if (StringUtils.isNoneBlank(accessToken, refreshToken)) {
      result.put("accessToken", accessToken);
      result.put("refreshToken", refreshToken);
    }

    return result;
  }

  /**
   * get a new accessToken with the refreshToken
   */
  public static void refreshAccessToken() throws Exception {
    if (StringUtils.isBlank(Globals.settings.getTraktRefreshToken())) {
      throw new Exception("not trakt.tv refresh token found");
    }

    OAuthAccessTokenResponse response = TraktV2.refreshAccessToken(CLIENT_ID,
        ApiKey.decryptApikey("VD2h4jmnrrYWnP1Nk49UtTNRILiWsuelJKdza7DAw+ROh1wtVf2U6PQScm7QWCOTsxN0K3QluIykKs2ZT1af1GcPz1401005bDBDss1Pz2c="),
        "urn:ietf:wg:oauth:2.0:oob", Globals.settings.getTraktRefreshToken());

    if (StringUtils.isNoneBlank(response.getAccessToken(), response.getRefreshToken())) {
      Globals.settings.setTraktAccessToken(response.getAccessToken());
      Globals.settings.setTraktRefreshToken(response.getRefreshToken());
      TRAKT.setAccessToken(Globals.settings.getTraktAccessToken());
    }
  }

  /**
   * do we have values for user/pass/api and are we a donator?!
   * 
   * @return true/false if trakt could be called
   */
  private boolean isEnabled() {
    if (!Globals.isDonator()) {
      LOGGER.warn("Won't spawn TRAKT.TV since you are not a donator!");
      return false;
    }
    if (StringUtils.isNoneBlank(Globals.settings.getTraktAccessToken(), Globals.settings.getTraktRefreshToken())) {
      // everything seems fine; also set the access token
      TRAKT.setAccessToken(Globals.settings.getTraktAccessToken());
      return true;
    }
    return false;
  }

  // @formatter:off
  // ███╗   ███╗ ██████╗ ██╗   ██╗██╗███████╗███████╗
  // ████╗ ████║██╔═══██╗██║   ██║██║██╔════╝██╔════╝
  // ██╔████╔██║██║   ██║██║   ██║██║█████╗  ███████╗
  // ██║╚██╔╝██║██║   ██║╚██╗ ██╔╝██║██╔══╝  ╚════██║
  // ██║ ╚═╝ ██║╚██████╔╝ ╚████╔╝ ██║███████╗███████║
  // ╚═╝     ╚═╝ ╚═════╝   ╚═══╝  ╚═╝╚══════╝╚══════╝
  // @formatter:on

  /**
   * Syncs Trakt.tv collection (specified movies)<br>
   * Gets all Trakt movies from collection, matches them to ours, and sends ONLY the new ones back to Trakt
   */
  public void syncTraktMovieCollection(List<Movie> moviesInTmm) {
    if (!isEnabled()) {
      return;
    }

    // create a local copy of the list
    List<Movie> tmmMovies = new ArrayList<Movie>(moviesInTmm);
    // *****************************************************************************
    // 1) get diff of TMM <-> Trakt collection
    // *****************************************************************************
    LOGGER.info("got up to " + tmmMovies.size() + " movies for Trakt.tv collection sync");

    // get ALL Trakt movies in collection
    List<BaseMovie> traktMovies = new ArrayList<BaseMovie>();

    try {
      traktMovies = TRAKT.sync().collectionMovies(Extended.DEFAULT_MIN);
      // Extended.DEFAULT adds url, poster, fanart, banner, genres
      // Extended.MAX adds certs, runtime, and other stuff (useful for scraper!)
    }
    catch (RetrofitError e) {
      handleRetrofitError(e);
      return;
    }
    catch (OAuthUnauthorizedException e) {
      try {
        // not authorized - maybe access token revoked - relogin
        refreshAccessToken();
        traktMovies = TRAKT.sync().collectionMovies(Extended.DEFAULT_MIN);
      }
      catch (Exception e1) {
        if (e1.getCause() instanceof RetrofitError) {
          handleRetrofitError((RetrofitError) e1.getCause());
        }
        else {
          LOGGER.error("could not retrieve trakt.tv response: ", e1.getMessage());
        }
        return;
      }
    }
    LOGGER.info("You have " + traktMovies.size() + " movies in your Trakt.tv collection");

    // loop over all movies on trakt
    for (BaseMovie traktMovie : traktMovies) {
      // loop over TMM movies, and check if IMDBID match
      for (int i = tmmMovies.size() - 1; i >= 0; i--) {
        Movie tmmMovie = tmmMovies.get(i);

        if (matches(tmmMovie, traktMovie.movie.ids)) {
          // we have a movie match

          // update missing IDs (we get them for free :)
          boolean dirty = updateIDs(tmmMovie, traktMovie.movie.ids);

          if (traktMovie.collected_at != null && !(traktMovie.collected_at.toDate().equals(tmmMovie.getDateAdded()))) {
            // always set from trakt, if not matched (Trakt = master)
            LOGGER.trace("Marking movie '" + tmmMovie.getTitle() + "' as collected on " + traktMovie.collected_at.toDate() + " (was "
                + tmmMovie.getDateAddedAsString() + ")");
            tmmMovie.setDateAdded(traktMovie.collected_at.toDate());
            dirty = true;
          }

          if (dirty) {
            tmmMovie.writeNFO();
            tmmMovie.saveToDb();
          }

          // remove it from our list (no need to add)
          tmmMovies.remove(i);
        }
      }
    }

    if (tmmMovies.size() == 0) {
      LOGGER.info("Already up-to-date - no need to add anything :)");
      return;
    }

    // *****************************************************************************
    // 2) add remaining TMM movies to Trakt collection
    // *****************************************************************************
    LOGGER.debug("prepare " + tmmMovies.size() + " movies for Trakt.tv collection sync");

    List<SyncMovie> movies = new ArrayList<SyncMovie>();
    int nosync = 0;
    for (Movie tmmMovie : tmmMovies) {
      if (tmmMovie.getIdAsInt(providerInfo.getId()) != 0 || !tmmMovie.getIdAsString(Constants.IMDB).isEmpty()
          || tmmMovie.getIdAsInt(Constants.TMDB) != 0) {
        movies.add(toSyncMovie(tmmMovie, false));
      }
      else {
        // do not add to Trakt if we do not have at least one ID
        nosync++;
        continue;
      }
    }
    if (nosync > 0) {
      LOGGER.debug("skipping " + nosync + " movies, because they have not been scraped yet!");
    }

    if (movies.size() == 0) {
      LOGGER.info("no new movies for Trakt collection sync found.");
      return;
    }

    try {
      LOGGER.info("Adding " + movies.size() + " movies to Trakt.tv collection");
      SyncItems items = new SyncItems().movies(movies);
      response = TRAKT.sync().addItemsToCollection(items);
      LOGGER.info("Trakt add-to-library status:");
      printStatus(response);
    }
    catch (RetrofitError e) {
      handleRetrofitError(e);
      return;
    }
    catch (OAuthUnauthorizedException e) {
      handleRetrofitError((RetrofitError) e.getCause());
      return;
    }
  }

  /**
   * Syncs Trakt.tv collection (all movies you have)<br>
   * Gets all Trakt movies from collection, matches them to ours, and sends ONLY the new ones back to Trakt
   */
  public void syncTraktMovieCollection() {
    if (!isEnabled()) {
      return;
    }
    syncTraktMovieCollection(new ArrayList<Movie>(MovieList.getInstance().getMovies()));
  }

  /**
   * clears the whole Trakt.tv movie collection. Gets all Trakt.tv movies from your collection and removes them from the collection and the watched
   * state; a little helper to initialize the collection
   */
  public void clearTraktMovies() {
    // *****************************************************************************
    // 1) get ALL Trakt movies in collection / watched
    // *****************************************************************************
    List<BaseMovie> traktCollection = new ArrayList<BaseMovie>();
    List<BaseMovie> traktWatched = new ArrayList<BaseMovie>();
    try {
      traktCollection = TRAKT.sync().collectionMovies(Extended.DEFAULT_MIN);
      traktWatched = TRAKT.sync().watchedMovies(Extended.DEFAULT_MIN);
    }
    catch (RetrofitError e) {
      handleRetrofitError(e);
      return;
    }
    catch (OAuthUnauthorizedException e) {
      try {
        // not authorized - maybe access token revoked - relogin
        refreshAccessToken();
        traktCollection = TRAKT.sync().collectionMovies(Extended.DEFAULT_MIN);
        traktWatched = TRAKT.sync().watchedMovies(Extended.DEFAULT_MIN);
      }
      catch (Exception e1) {
        if (e1.getCause() instanceof RetrofitError) {
          handleRetrofitError((RetrofitError) e1.getCause());
        }
        else {
          LOGGER.error("could not retrieve trakt.tv response: ", e1.getMessage());
        }
        return;
      }
    }
    LOGGER.info("You have " + traktCollection.size() + " movies in your Trakt.tv collection");
    LOGGER.info("You have " + traktWatched.size() + " movies watched");

    // *****************************************************************************
    // 2) remove every movie from the COLLECTION state
    // *****************************************************************************
    List<SyncMovie> movieToRemove = new ArrayList<SyncMovie>();
    for (BaseMovie traktMovie : traktCollection) {
      movieToRemove.add(toSyncMovie(traktMovie));
    }
    if (!movieToRemove.isEmpty()) {
      try {
        SyncItems items = new SyncItems().movies(movieToRemove);
        TRAKT.sync().deleteItemsFromCollection(items);
        LOGGER.info("removed " + movieToRemove.size() + " movies from your trakt.tv collection");
      }
      catch (RetrofitError e) {
        handleRetrofitError(e);
        return;
      }
      catch (OAuthUnauthorizedException e) {
        handleRetrofitError((RetrofitError) e.getCause());
        return;
      }
    }

    // *****************************************************************************
    // 3) remove every movie from the WATCHED state
    // *****************************************************************************
    movieToRemove.clear();
    for (BaseMovie traktMovie : traktWatched) {
      movieToRemove.add(toSyncMovie(traktMovie));
    }
    if (!movieToRemove.isEmpty()) {
      try {
        SyncItems items = new SyncItems().movies(movieToRemove);
        TRAKT.sync().deleteItemsFromWatchedHistory(items);
        LOGGER.info("removed " + movieToRemove.size() + " movies from your trakt.tv watched");
      }
      catch (RetrofitError e) {
        handleRetrofitError(e);
        return;
      }
      catch (OAuthUnauthorizedException e) {
        handleRetrofitError((RetrofitError) e.getCause());
        return;
      }
    }

  }

  /**
   * Syncs Trakt.tv "seen" flag (all gives you have already marked as watched)<br>
   * Gets all watched movies from Trakt, and sets the "watched" flag on TMM movies.<br>
   * Then update the remaining TMM movies on Trakt as 'seen'.
   */
  public void syncTraktMovieWatched(List<Movie> moviesInTmm) {
    if (!isEnabled()) {
      return;
    }

    // create a local copy of the list
    List<Movie> tmmMovies = new ArrayList<Movie>(moviesInTmm);

    // *****************************************************************************
    // 1) get all Trakt watched movies and update our "watched" status
    // *****************************************************************************
    List<BaseMovie> traktMovies = new ArrayList<BaseMovie>();
    try {
      traktMovies = TRAKT.sync().watchedMovies(Extended.DEFAULT_MIN);
      // Extended.DEFAULT adds url, poster, fanart, banner, genres
      // Extended.MAX adds certs, runtime, and other stuff (useful for scraper!)
    }
    catch (RetrofitError e) {
      handleRetrofitError(e);
      return;
    }
    catch (OAuthUnauthorizedException e) {
      try {
        // not authorized - maybe access token revoked - relogin
        refreshAccessToken();
        traktMovies = TRAKT.sync().watchedMovies(Extended.DEFAULT_MIN);
      }
      catch (Exception e1) {
        if (e1.getCause() instanceof RetrofitError) {
          handleRetrofitError((RetrofitError) e1.getCause());
        }
        else {
          LOGGER.error("could not retrieve trakt.tv response: ", e1.getMessage());
        }
        return;
      }
    }
    LOGGER.info("You have " + traktMovies.size() + " movies marked as 'watched' in your Trakt.tv collection");

    // loop over all watched movies on trakt
    for (BaseMovie traktWatched : traktMovies) {
      // loop over TMM movies, and check if IMDBID match
      for (Movie tmmMovie : tmmMovies) {
        if (matches(tmmMovie, traktWatched.movie.ids)) {
          // we have a movie match

          // update missing IDs (we get them for free :)
          boolean dirty = updateIDs(tmmMovie, traktWatched.movie.ids);

          if (!tmmMovie.isWatched()) {
            // save Trakt watched status
            LOGGER.info("Marking movie '" + tmmMovie.getTitle() + "' as watched");
            tmmMovie.setWatched(true);
            dirty = true;
          }
          if (traktWatched.last_watched_at != null && !(traktWatched.last_watched_at.toDate().equals(tmmMovie.getLastWatched()))) {
            // always set from trakt, if not matched (Trakt = master)
            LOGGER.trace("Marking movie '" + tmmMovie.getTitle() + "' as watched on " + traktWatched.last_watched_at.toDate() + " (was "
                + tmmMovie.getLastWatched() + ")");
            tmmMovie.setLastWatched(traktWatched.last_watched_at.toDate());
            dirty = true;
          }

          if (dirty) {
            tmmMovie.writeNFO();
            tmmMovie.saveToDb();
          }
        }
      }
    }

    // *****************************************************************************
    // 2) mark additionally "watched" movies as 'seen' on Trakt
    // *****************************************************************************
    // Now get all TMM watched movies...
    List<Movie> tmmWatchedMovies = new ArrayList<Movie>();
    for (Movie movie : tmmMovies) {
      if (movie.isWatched()) {
        tmmWatchedMovies.add(movie);
      }
    }
    LOGGER.info("You have now " + tmmWatchedMovies.size() + " movies marked as 'watched' in your TMM database");

    // ...and subtract the already watched from Trakt
    for (int i = tmmWatchedMovies.size() - 1; i >= 0; i--) {
      for (BaseMovie traktWatched : traktMovies) {
        Movie tmmMovie = tmmWatchedMovies.get(i);
        if (matches(tmmMovie, traktWatched.movie.ids)) {
          tmmWatchedMovies.remove(i);
          break;
        }
      }
    }

    if (tmmWatchedMovies.size() == 0) {
      LOGGER.info("no new watched movies for Trakt sync found.");
      return;
    }

    LOGGER.debug("prepare " + tmmWatchedMovies.size() + " movies for Trakt.tv sync");
    List<SyncMovie> movies = new ArrayList<SyncMovie>();
    int nosync = 0;
    for (Movie tmmMovie : tmmWatchedMovies) {
      if (tmmMovie.getIdAsInt(providerInfo.getId()) != 0 || !tmmMovie.getIdAsString(Constants.IMDB).isEmpty()
          || tmmMovie.getIdAsInt(Constants.TMDB) != 0) {
        movies.add(toSyncMovie(tmmMovie, true));
      }
      else {
        // do not add to Trakt if we do not have at least one ID
        nosync++;
        continue;
      }
    }
    if (nosync > 0) {
      LOGGER.debug("skipping " + nosync + " movies, because they have not been scraped yet!");
    }

    if (movies.size() == 0) {
      LOGGER.info("no new watched movies for Trakt sync found.");
      return;
    }

    try {
      LOGGER.info("Marking " + movies.size() + " movies as 'watched' to Trakt.tv collection");
      SyncItems items = new SyncItems().movies(movies);
      response = TRAKT.sync().addItemsToWatchedHistory(items);
      LOGGER.info("Trakt mark-as-watched status:");
      printStatus(response);
    }
    catch (RetrofitError e) {
      handleRetrofitError(e);
      return;
    }
    catch (OAuthUnauthorizedException e) {
      handleRetrofitError((RetrofitError) e.getCause());
      return;
    }
  }

  /**
   * Syncs Trakt.tv "seen" flag (all movies you have already marked as watched)<br>
   * Gets all watched movies from Trakt, and sets the "watched" flag on TMM movies.<br>
   * Then update the remaining TMM movies on Trakt as 'seen'.
   */
  public void syncTraktMovieWatched() {
    if (!isEnabled()) {
      return;
    }
    syncTraktMovieWatched(MovieList.getInstance().getMovies());
  }

  // @formatter:off
  //  ████████╗██╗   ██╗███████╗██╗  ██╗ ██████╗ ██╗    ██╗███████╗
  //  ╚══██╔══╝██║   ██║██╔════╝██║  ██║██╔═══██╗██║    ██║██╔════╝
  //     ██║   ██║   ██║███████╗███████║██║   ██║██║ █╗ ██║███████╗
  //     ██║   ╚██╗ ██╔╝╚════██║██╔══██║██║   ██║██║███╗██║╚════██║
  //     ██║    ╚████╔╝ ███████║██║  ██║╚██████╔╝╚███╔███╔╝███████║
  //     ╚═╝     ╚═══╝  ╚══════╝╚═╝  ╚═╝ ╚═════╝  ╚══╝╚══╝ ╚══════╝
  // @formatter:on

  /**
   * Syncs Trakt.tv collection (gets all IDs & dates, and adds all TMM shows to Trakt)<br>
   * Do not send diffs, since this is too complicated currently :|
   */
  public void syncTraktTvShowCollection(List<TvShow> tvShowsInTmm) {
    if (!isEnabled()) {
      return;
    }

    // create a local copy of the list
    List<TvShow> tvShows = new ArrayList<TvShow>(tvShowsInTmm);
    // *****************************************************************************
    // 1) sync ALL missing show IDs & dates from trakt
    // *****************************************************************************
    List<BaseShow> traktShows = new ArrayList<BaseShow>();
    try {
      traktShows = TRAKT.sync().collectionShows(Extended.DEFAULT_MIN);
    }
    catch (RetrofitError e) {
      handleRetrofitError(e);
      return;
    }
    catch (OAuthUnauthorizedException e) {
      try {
        // not authorized - maybe access token revoked - relogin
        refreshAccessToken();
        traktShows = TRAKT.sync().collectionShows(Extended.DEFAULT_MIN);
      }
      catch (Exception e1) {
        if (e1.getCause() instanceof RetrofitError) {
          handleRetrofitError((RetrofitError) e1.getCause());
        }
        else {
          LOGGER.error("could not retrieve trakt.tv response: ", e1.getMessage());
        }
        return;
      }
    }
    LOGGER.info("You have " + traktShows.size() + " TvShows in your Trakt.tv collection");

    // remember which episodes are already in trakt
    Set<TvShowEpisode> episodesInTrakt = new HashSet<>();

    for (BaseShow traktShow : traktShows) {
      for (TvShow tmmShow : tvShows) {
        if (matches(tmmShow, traktShow.show.ids)) {
          // ok, we have a show match

          // update show IDs from trakt
          boolean dirty = updateIDs(tmmShow, traktShow.show.ids);

          // update collection date from trakt (show)
          if (traktShow.last_collected_at != null && !(traktShow.last_collected_at.toDate().equals(tmmShow.getDateAdded()))) {
            // always set from trakt, if not matched (Trakt = master)
            LOGGER.trace("Marking TvShow '" + tmmShow.getTitle() + "' as collected on " + traktShow.last_collected_at.toDate() + " (was "
                + tmmShow.getDateAddedAsString() + ")");
            tmmShow.setDateAdded(traktShow.last_collected_at.toDate());
            dirty = true;
          }

          // update collection date from trakt (episodes)
          for (BaseSeason bs : traktShow.seasons) {
            for (BaseEpisode be : bs.episodes) {
              TvShowEpisode tmmEP = tmmShow.getEpisode(bs.number, be.number);
              if (tmmEP == null) {
                continue;
              }

              episodesInTrakt.add(tmmEP);

              // update ep IDs - NOT YET POSSIBLE
              // boolean dirty = updateIDs(tmmEP, be.ids);

              if (be.collected_at != null && !(be.collected_at.toDate().equals(tmmEP.getDateAdded()))) {
                tmmEP.setDateAdded(be.collected_at.toDate());
                dirty = true;
              }
            }
          }

          if (dirty) {
            tmmShow.writeNFO();
            tmmShow.saveToDb();
          }
        }
      }
    }

    // *****************************************************************************
    // 2) add all our shows to Trakt collection (we have the physical file)
    // *****************************************************************************
    LOGGER.info("Adding " + tvShows.size() + " TvShows to Trakt.tv collection");
    // send show per show; sending all together may result too often in a timeout
    for (TvShow tvShow : tvShows) {
      SyncShow show = toSyncShow(tvShow, false, episodesInTrakt);
      if (show == null) {
        continue;
      }

      try {
        SyncItems items = new SyncItems().shows(show);
        response = TRAKT.sync().addItemsToCollection(items);

        LOGGER.debug("Trakt add-to-library status: " + tvShow.getTitle());
        printStatus(response);
      }
      catch (RetrofitError e) {
        handleRetrofitError(e);
      }
      catch (OAuthUnauthorizedException e) {
        handleRetrofitError((RetrofitError) e.getCause());
      }
    }
  }

  /**
   * Syncs Trakt.tv collection (all TvShows you have)<br>
   * Gets all Trakt shows/episodes from collection, matches them to ours, and sends ONLY the new ones back to Trakt
   */
  public void syncTraktTvShowCollection() {
    if (!isEnabled()) {
      return;
    }
    syncTraktTvShowCollection(new ArrayList<TvShow>(TvShowList.getInstance().getTvShows()));
  }

  public void syncTraktTvShowWatched(List<TvShow> tvShowsInTmm) {
    if (!isEnabled()) {
      return;
    }

    // create a local copy of the list
    List<TvShow> tvShows = new ArrayList<TvShow>(tvShowsInTmm);

    List<BaseShow> traktShows = new ArrayList<BaseShow>();
    try {
      traktShows = TRAKT.sync().watchedShows(Extended.DEFAULT_MIN);
    }
    catch (RetrofitError e) {
      handleRetrofitError(e);
      return;
    }
    catch (OAuthUnauthorizedException e) {
      try {
        // not authorized - maybe access token revoked - relogin
        refreshAccessToken();
        traktShows = TRAKT.sync().watchedShows(Extended.DEFAULT_MIN);
      }
      catch (Exception e1) {
        if (e1.getCause() instanceof RetrofitError) {
          handleRetrofitError((RetrofitError) e1.getCause());
        }
        else {
          LOGGER.error("could not retrieve trakt.tv response: ", e1.getMessage());
        }
        return;
      }
    }
    LOGGER.info("You have " + traktShows.size() + " TvShows marked as watched on Trakt.tv");
    for (BaseShow traktShow : traktShows) {
      for (TvShow tmmShow : tvShows) {
        if (matches(tmmShow, traktShow.show.ids)) {
          // ok, we have a show match

          // update show IDs from trakt
          boolean dirty = updateIDs(tmmShow, traktShow.show.ids);

          // update watched date from trakt (show)
          if (traktShow.last_watched_at != null && !(traktShow.last_watched_at.toDate().equals(tmmShow.getLastWatched()))) {
            // always set from trakt, if not matched (Trakt = master)
            LOGGER.trace("Marking TvShow '" + tmmShow.getTitle() + "' as watched on " + traktShow.last_watched_at.toDate() + " (was "
                + tmmShow.getLastWatched() + ")");
            tmmShow.setLastWatched(traktShow.last_watched_at.toDate());
            dirty = true;
          }

          // update collection date from trakt (episodes)
          for (BaseSeason bs : traktShow.seasons) {
            for (BaseEpisode be : bs.episodes) {
              TvShowEpisode tmmEP = tmmShow.getEpisode(bs.number, be.number);
              // update ep IDs - NOT YET POSSIBLE
              // boolean dirty = updateIDs(tmmEP, be.ids);

              if (tmmEP != null && be.last_watched_at != null && !(be.last_watched_at.toDate().equals(tmmEP.getLastWatched()))) {
                tmmEP.setLastWatched(be.last_watched_at.toDate());
                tmmEP.setWatched(true);
                dirty = true;
              }
            }
          }

          if (dirty) {
            tmmShow.writeNFO();
            tmmShow.saveToDb();
          }
        }
      }
    }

    // *****************************************************************************
    // 2) add all our shows to Trakt watched
    // *****************************************************************************
    LOGGER.info("Adding up to " + tvShows.size() + " TvShows as watched on Trakt.tv");
    // send show per show; sending all together may result too often in a timeout
    for (TvShow show : tvShows) {
      // get items to sync
      SyncShow sync = toSyncShow(show, true, new HashSet<TvShowEpisode>());
      if (sync == null) {
        continue;
      }

      try {
        SyncItems items = new SyncItems().shows(sync);
        response = TRAKT.sync().addItemsToWatchedHistory(items);

        LOGGER.debug("Trakt add-to-library status: " + show.getTitle());
        printStatus(response);
      }
      catch (RetrofitError e) {
        handleRetrofitError(e);
      }
      catch (OAuthUnauthorizedException e) {
        handleRetrofitError((RetrofitError) e.getCause());
      }
    }
  }

  public void syncTraktTvShowWatched() {
    if (!isEnabled()) {
      return;
    }
    syncTraktTvShowWatched(new ArrayList<TvShow>(TvShowList.getInstance().getTvShows()));
  }

  /**
   * clears the whole Trakt.tv movie collection. Gets all Trakt.tv movies from your collection and removes them from the collection and the watched
   * state; a little helper to initialize the collection
   */
  public void clearTraktTvShows() {
    // *****************************************************************************
    // 1) get ALL Trakt shows in collection / watched
    // *****************************************************************************
    List<BaseShow> traktCollection = new ArrayList<BaseShow>();
    List<BaseShow> traktWatched = new ArrayList<BaseShow>();
    try {
      traktCollection = TRAKT.sync().collectionShows(Extended.DEFAULT_MIN);
      traktWatched = TRAKT.sync().watchedShows(Extended.DEFAULT_MIN);
    }
    catch (RetrofitError e) {
      handleRetrofitError(e);
      return;
    }
    catch (OAuthUnauthorizedException e) {
      try {
        // not authorized - maybe access token revoked - relogin
        refreshAccessToken();
        traktCollection = TRAKT.sync().collectionShows(Extended.DEFAULT_MIN);
        traktWatched = TRAKT.sync().watchedShows(Extended.DEFAULT_MIN);
      }
      catch (Exception e1) {
        if (e1.getCause() instanceof RetrofitError) {
          handleRetrofitError((RetrofitError) e1.getCause());
        }
        else {
          LOGGER.error("could not retrieve trakt.tv response: ", e1.getMessage());
        }
        return;
      }
    }
    LOGGER.info("You have " + traktCollection.size() + " shows in your Trakt.tv collection");
    LOGGER.info("You have " + traktWatched.size() + " shows watched");

    // *****************************************************************************
    // 2) remove every shows from the COLLECTION state
    // *****************************************************************************
    List<SyncShow> showToRemove = new ArrayList<SyncShow>();
    for (BaseShow traktShow : traktCollection) {
      showToRemove.add(toSyncShow(traktShow));
    }
    if (!showToRemove.isEmpty()) {
      try {
        SyncItems items = new SyncItems().shows(showToRemove);
        TRAKT.sync().deleteItemsFromCollection(items);
        LOGGER.debug("removed " + showToRemove.size() + " shows from your trakt.tv collection");
      }
      catch (RetrofitError e) {
        handleRetrofitError(e);
        return;
      }
      catch (OAuthUnauthorizedException e) {
        handleRetrofitError((RetrofitError) e.getCause());
        return;
      }
    }

    // *****************************************************************************
    // 3) remove every shows from the WATCHED state
    // *****************************************************************************
    showToRemove.clear();
    for (BaseShow traktShow : traktWatched) {
      showToRemove.add(toSyncShow(traktShow));
    }
    if (!showToRemove.isEmpty()) {
      try {
        SyncItems items = new SyncItems().shows(showToRemove);
        TRAKT.sync().deleteItemsFromWatchedHistory(items);
        LOGGER.debug("removed " + showToRemove.size() + " shows from your trakt.tv watched");
      }
      catch (RetrofitError e) {
        handleRetrofitError(e);
        return;
      }
      catch (OAuthUnauthorizedException e) {
        handleRetrofitError((RetrofitError) e.getCause());
        return;
      }
    }
  }

  // @formatter:off
  // ██╗   ██╗████████╗██╗██╗     ███████╗
  // ██║   ██║╚══██╔══╝██║██║     ██╔════╝
  // ██║   ██║   ██║   ██║██║     ███████╗
  // ██║   ██║   ██║   ██║██║     ╚════██║
  // ╚██████╔╝   ██║   ██║███████╗███████║
  //  ╚═════╝    ╚═╝   ╚═╝╚══════╝╚══════╝
  // @formatter:on

  private boolean updateIDs(TvShow tmmShow, ShowIds ids) {
    boolean dirty = false;
    if (tmmShow.getIdAsString(Constants.IMDB).isEmpty() && !StringUtils.isEmpty(ids.imdb)) {
      tmmShow.setId(Constants.IMDB, ids.imdb);
      dirty = true;
    }
    if (tmmShow.getIdAsInt(Constants.TMDB) == 0 && ids.tmdb != null && ids.tmdb != 0) {
      tmmShow.setId(Constants.TMDB, ids.tmdb);
      dirty = true;
    }
    if (tmmShow.getIdAsInt(providerInfo.getId()) == 0 && ids.trakt != null && ids.trakt != 0) {
      tmmShow.setId(providerInfo.getId(), ids.trakt);
      dirty = true;
    }
    if (tmmShow.getIdAsInt(Constants.TVDB) == 0 && ids.tvdb != null && ids.tvdb != 0) {
      tmmShow.setId(Constants.TVDB, ids.tvdb);
      dirty = true;
    }

    // not used atm
    // if (tmmShow.getIdAsInt(Constants.TVRAGEID) == 0 && ids.tvrage != null && ids.tvrage != 0) {
    // tmmShow.setId(Constants.TVRAGEID, ids.tvrage);
    // dirty = true;
    // }
    return dirty;
  }

  private boolean updateIDs(Movie tmmMovie, MovieIds ids) {
    boolean dirty = false;
    if (tmmMovie.getIdAsString(Constants.IMDB).isEmpty() && !StringUtils.isEmpty(ids.imdb)) {
      tmmMovie.setId(Constants.IMDB, ids.imdb);
      dirty = true;
    }
    if (tmmMovie.getIdAsInt(Constants.TMDB) == 0 && ids.tmdb != null && ids.tmdb != 0) {
      tmmMovie.setId(Constants.TMDB, ids.tmdb);
      dirty = true;
    }
    if (tmmMovie.getIdAsInt(providerInfo.getId()) == 0 && ids.trakt != null && ids.trakt != 0) {
      tmmMovie.setId(providerInfo.getId(), ids.trakt);
      dirty = true;
    }
    return dirty;
  }

  private boolean matches(TvShow tmmShow, ShowIds ids) {
    if (ids.trakt != null && ids.trakt != 0 && ids.trakt == tmmShow.getIdAsInt(providerInfo.getId())) {
      return true;
    }
    if (StringUtils.isNotEmpty(ids.imdb) && ids.imdb.equals(tmmShow.getIdAsString(Constants.IMDB))) {
      return true;
    }
    if (ids.tmdb != null && ids.tmdb != 0 && ids.tmdb == tmmShow.getIdAsInt(Constants.TMDB)) {
      return true;
    }
    if (ids.tvdb != null && ids.tvdb != 0 && ids.tvdb == tmmShow.getIdAsInt(Constants.TVDB)) {
      return true;
    }
    // not used atm
    // if (ids.tvrage != null && ids.tvrage != 0 && ids.tvrage == tmmShow.getIdAsInt(Constants.TVRAGEID)) {
    // return true;
    // }
    return false;
  }

  private boolean matches(Movie tmmMovie, MovieIds ids) {
    if (ids.trakt != null && ids.trakt != 0 && ids.trakt == tmmMovie.getIdAsInt(providerInfo.getId())) {
      return true;
    }
    if (StringUtils.isNotEmpty(ids.imdb) && ids.imdb.equals(tmmMovie.getIdAsString(Constants.IMDB))) {
      return true;
    }
    if (ids.tmdb != null && ids.tmdb != 0 && ids.tmdb == tmmMovie.getIdAsInt(Constants.TMDB)) {
      return true;
    }
    return false;
  }

  private SyncMovie toSyncMovie(Movie tmmMovie, boolean watched) {
    boolean hasId = false;
    SyncMovie movie = null;

    MovieIds ids = new MovieIds();
    if (!tmmMovie.getIdAsString(Constants.IMDB).isEmpty()) {
      ids.imdb = tmmMovie.getIdAsString(Constants.IMDB);
      hasId = true;
    }
    if (tmmMovie.getIdAsInt(Constants.TMDB) != 0) {
      ids.tmdb = tmmMovie.getIdAsInt(Constants.TMDB);
      hasId = true;
    }
    if (tmmMovie.getIdAsInt(providerInfo.getId()) != 0) {
      ids.trakt = tmmMovie.getIdAsInt(providerInfo.getId());
      hasId = true;
    }

    if (!hasId) {
      return movie;
    }

    // we have to decide what we send; trakt behaves differenty when sending data to sync collection and sync history.
    if (watched) {
      // sync history
      if (tmmMovie.isWatched() && tmmMovie.getLastWatched() == null) {
        // watched in tmm and not in trakt -> sync
        movie = new SyncMovie().id(ids).watchedAt(new DateTime(tmmMovie.getLastWatched()));
      }
    }
    else {
      // sync collection
      movie = new SyncMovie().id(ids).collectedAt(new DateTime(tmmMovie.getDateAdded()));
    }

    return movie;
  }

  private SyncMovie toSyncMovie(BaseMovie baseMovie) {
    SyncMovie movie = new SyncMovie().id(baseMovie.movie.ids).collectedAt(baseMovie.collected_at).watchedAt(baseMovie.last_watched_at);
    return movie;
  }

  private SyncShow toSyncShow(TvShow tmmShow, boolean watched, Set<TvShowEpisode> episodesInTrakt) {
    boolean hasId = false;

    SyncShow show = null;
    ShowIds ids = new ShowIds();
    if (!tmmShow.getIdAsString(Constants.IMDB).isEmpty()) {
      ids.imdb = tmmShow.getIdAsString(Constants.IMDB);
      hasId = true;
    }
    if (tmmShow.getIdAsInt(Constants.TMDB) != 0) {
      ids.tmdb = tmmShow.getIdAsInt(Constants.TMDB);
      hasId = true;
    }
    if (tmmShow.getIdAsInt(Constants.TVDB) != 0) {
      ids.tvdb = tmmShow.getIdAsInt(Constants.TVDB);
      hasId = true;
    }
    if (tmmShow.getIdAsInt(providerInfo.getId()) != 0) {
      ids.trakt = tmmShow.getIdAsInt(providerInfo.getId());
      hasId = true;
    }
    // not used atm
    // if (tmmShow.getIdAsInt(Constants.TVRAGEID) != 0) {
    // ids.tvrage = tmmShow.getIdAsInt(Constants.TVRAGEID);
    // hasId = true;
    // }

    if (!hasId) {
      return show;
    }

    ArrayList<SyncSeason> ss = new ArrayList<SyncSeason>();
    boolean foundS = false;
    for (TvShowSeason tmmSeason : tmmShow.getSeasons()) {
      boolean foundEP = false;
      ArrayList<SyncEpisode> se = new ArrayList<SyncEpisode>();
      for (TvShowEpisode tmmEp : tmmSeason.getEpisodes()) {
        // we have to decide what we send; trakt behaves differenty when sending data to
        // sync collection and sync history.
        if (watched) {
          // sync history
          if (tmmEp.isWatched() && tmmEp.getLastWatched() == null) {
            // watched in tmm and not in trakt -> sync
            se.add(new SyncEpisode().number(tmmEp.getEpisode()).watchedAt(new DateTime(tmmEp.getLastWatched())));
            foundEP = true;
          }
        }
        else {
          // sync collection
          if (!episodesInTrakt.contains(tmmEp)) {
            se.add(new SyncEpisode().number(tmmEp.getEpisode()).collectedAt(new DateTime(tmmEp.getDateAdded())));
            foundEP = true;
          }
        }
      }
      if (foundEP) {
        // do not send empty seasons
        foundS = true;
        ss.add(new SyncSeason().number(tmmSeason.getSeason()).episodes(se));
      }
    }

    if (foundS) {
      // we have at least one season/episode, so add it
      show = new SyncShow().id(ids).collectedAt(new DateTime(tmmShow.getDateAdded())).seasons(ss);
    }

    // if nothing added, do NOT send an empty show (to add all)
    return show;
  }

  private SyncShow toSyncShow(BaseShow baseShow) {
    // TODO: used only on clear() - so we don't need the episodes? TBC
    ArrayList<SyncSeason> ss = new ArrayList<SyncSeason>();
    for (BaseSeason baseSeason : baseShow.seasons) {
      ArrayList<SyncEpisode> se = new ArrayList<SyncEpisode>();
      for (BaseEpisode baseEp : baseSeason.episodes) {
        se.add(new SyncEpisode().number(baseEp.number).collectedAt(new DateTime(baseEp.collected_at)).watchedAt(new DateTime(baseEp.collected_at)));
      }
      ss.add(new SyncSeason().number(baseSeason.number).episodes(se));
    }
    SyncShow show = new SyncShow().id(baseShow.show.ids).collectedAt(new DateTime(baseShow.last_collected_at))
        .watchedAt(new DateTime(baseShow.last_watched_at)).seasons(ss);
    return show;
  }

  /**
   * prints some trakt response status
   * 
   * @param reponse
   *          the reponse
   */
  private void printStatus(SyncResponse resp) {
    if (resp != null) {
      String info = getStatusString(resp.added);
      if (!info.isEmpty()) {
        LOGGER.debug("Added       : " + info);
      }
      info = getStatusString(resp.existing);
      if (!info.isEmpty()) {
        LOGGER.debug("Existing    : " + info);
      }
      info = getStatusString(resp.deleted);
      if (!info.isEmpty()) {
        LOGGER.debug("Deleted     : " + info);
      }
      info = getStatusString(resp.not_found);
      if (!info.isEmpty()) {
        LOGGER.debug("Errors      : " + info);
      }
    }
  }

  private String getStatusString(SyncStats ss) {
    if (ss == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder(50);

    if (ss.movies != null && ss.movies > 0) {
      sb.append(ss.movies + " Movies ");
    }
    if (ss.shows != null && ss.shows > 0) {
      sb.append(ss.shows + " Shows ");
    }
    if (ss.seasons != null && ss.seasons > 0) {
      sb.append(ss.seasons + " Seasons ");
    }
    if (ss.episodes != null && ss.episodes > 0) {
      sb.append(ss.episodes + " Episodes");
    }

    return sb.toString();
  }

  private String getStatusString(SyncErrors ss) {
    if (ss == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder(50);

    // TODO: iterate over error array and display which did not work
    if (ss.movies != null && ss.movies.size() > 0) {
      sb.append(ss.movies.size() + " Movies ");
    }
    if (ss.shows != null && ss.shows.size() > 0) {
      sb.append(ss.shows.size() + " Shows ");
    }
    if (ss.seasons != null && ss.seasons.size() > 0) {
      sb.append(ss.seasons.size() + " Seasons ");
    }
    if (ss.episodes != null && ss.episodes.size() > 0) {
      sb.append(ss.episodes.size() + " Episodes");
    }

    return sb.toString();
  }

  /**
   * handles the retrofit errors<br>
   * (which is always thrown when http status != 200)<br>
   * and print some error msg
   * 
   * @param re
   *          the Retrofit error
   */
  private void handleRetrofitError(RetrofitError re) {
    Response r = re.getResponse();
    String msg = "";
    if (r != null) {
      msg = r.getStatus() + " " + r.getReason();
      if (r.getBody() != null && r.getBody().length() > 0) {
        try {
          InputStream in = r.getBody().in();
          String body = " - " + IOUtils.toString(in, "UTF-8");
          in.close();
          LOGGER.trace(body);
        }
        catch (IOException e1) {
          LOGGER.warn("IOException on Trakt error", e1);
        }
      }
    }
    else {
      msg = re.getMessage();
    }
    LOGGER.error("Trakt error (wrong settings?) " + msg);
    MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, msg, "Settings.trakttv"));
  }

  // /**
  // * Maps scraper Genres to internal TMM genres
  // */
  // private MediaGenres getTmmGenre(String genre) {
  // MediaGenres g = null;
  // if (genre == null || genre.isEmpty()) {
  // return g;
  // }
//    // @formatter:off
//    else if (genre.equals("Action"))           { g = MediaGenres.ACTION; }
//    else if (genre.equals("Adventure"))        { g = MediaGenres.ADVENTURE; }
//    else if (genre.equals("Animation"))        { g = MediaGenres.ANIMATION; }
//    else if (genre.equals("Comedy"))           { g = MediaGenres.COMEDY; }
//    else if (genre.equals("Children"))         { g = MediaGenres.FAMILY; }
//    else if (genre.equals("Crime"))            { g = MediaGenres.CRIME; }
//    else if (genre.equals("Documentary"))      { g = MediaGenres.DOCUMENTARY; }
//    else if (genre.equals("Drama"))            { g = MediaGenres.DRAMA; }
//    else if (genre.equals("Family"))           { g = MediaGenres.FAMILY; }
//    else if (genre.equals("Fantasy"))          { g = MediaGenres.FANTASY; }
//    else if (genre.equals("Film Noir"))        { g = MediaGenres.FILM_NOIR; }
//    else if (genre.equals("History"))          { g = MediaGenres.HISTORY; }
//    else if (genre.equals("Game Show"))        { g = MediaGenres.GAME_SHOW; }
//    else if (genre.equals("Home and Garden"))  { g = MediaGenres.DOCUMENTARY; }
//    else if (genre.equals("Horror"))           { g = MediaGenres.HORROR; }
//    else if (genre.equals("Indie"))            { g = MediaGenres.INDIE; }
//    else if (genre.equals("Music"))            { g = MediaGenres.MUSIC; }
//    else if (genre.equals("Mini Series"))      { g = MediaGenres.SERIES; }
//    else if (genre.equals("Musical"))          { g = MediaGenres.MUSICAL; }
//    else if (genre.equals("Mystery"))          { g = MediaGenres.MYSTERY; }
//    else if (genre.equals("News"))             { g = MediaGenres.NEWS; }
//    else if (genre.equals("Reality"))          { g = MediaGenres.REALITY_TV; }
//    else if (genre.equals("Romance"))          { g = MediaGenres.ROMANCE; }
//    else if (genre.equals("Science Fiction"))  { g = MediaGenres.SCIENCE_FICTION; }
//    else if (genre.equals("Sport"))            { g = MediaGenres.SPORT; }
//    else if (genre.equals("Special Interest")) { g = MediaGenres.INDIE; }
//    else if (genre.equals("Soap"))             { g = MediaGenres.SERIES; }
//    else if (genre.equals("Suspense"))         { g = MediaGenres.SUSPENSE; }
//    else if (genre.equals("Talk Show"))        { g = MediaGenres.TALK_SHOW; }
//    else if (genre.equals("Thriller"))         { g = MediaGenres.THRILLER; }
//    else if (genre.equals("War"))              { g = MediaGenres.WAR; }
//    else if (genre.equals("Western"))          { g = MediaGenres.WESTERN; }
//    else if (genre.equals("No Genre"))         { return null; }
//    // @formatter:on
  // if (g == null) {
  // g = MediaGenres.getGenre(genre);
  // }
  // return g;
  // }
}
