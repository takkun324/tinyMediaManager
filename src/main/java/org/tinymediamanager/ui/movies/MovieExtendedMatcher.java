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
package org.tinymediamanager.ui.movies;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.core.movie.MovieMediaSource;
import org.tinymediamanager.core.movie.MovieSearchOptions;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieActor;
import org.tinymediamanager.core.movie.entities.MovieProducer;
import org.tinymediamanager.scraper.Certification;
import org.tinymediamanager.scraper.MediaGenres;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * The Class MoviesExtendedMatcher.
 * 
 * @author Manuel Laggner
 */
public class MovieExtendedMatcher implements Matcher<Movie> {
  private Map<MovieSearchOptions, Object> searchOptions;

  /**
   * Instantiates a new movies extended matcher.
   * 
   * @param searchOptions
   *          the search options
   */
  public MovieExtendedMatcher(Map<MovieSearchOptions, Object> searchOptions) {
    this.searchOptions = searchOptions;
  }

  @Override
  public boolean matches(Movie movie) {
    // not null
    if (movie == null) {
      return false;
    }

    // check duplicates
    if (searchOptions.containsKey(MovieSearchOptions.DUPLICATES)) {
      if (!movie.isDuplicate()) {
        return false;
      }
    }

    // check against watched flag
    if (searchOptions.containsKey(MovieSearchOptions.WATCHED)) {
      boolean watched = (Boolean) searchOptions.get(MovieSearchOptions.WATCHED);
      boolean result = !(movie.isWatched() ^ watched);
      if (result == false) {
        return false;
      }
    }

    // check against genre
    if (searchOptions.containsKey(MovieSearchOptions.GENRE)) {
      MediaGenres genre = (MediaGenres) searchOptions.get(MovieSearchOptions.GENRE);
      if (!movie.getGenres().contains(genre)) {
        return false;
      }
    }

    // check against certification
    if (searchOptions.containsKey(MovieSearchOptions.CERTIFICATION)) {
      Certification cert = (Certification) searchOptions.get(MovieSearchOptions.CERTIFICATION);
      if (cert != movie.getCertification()) {
        return false;
      }
    }

    // check against cast member
    if (searchOptions.containsKey(MovieSearchOptions.CAST)) {
      String castSearch = (String) searchOptions.get(MovieSearchOptions.CAST);
      if (!containsCast(movie, castSearch)) {
        return false;
      }
    }

    // check against tag
    if (searchOptions.containsKey(MovieSearchOptions.TAG)) {
      String tag = (String) searchOptions.get(MovieSearchOptions.TAG);
      if (!containsTag(movie, tag)) {
        return false;
      }
    }

    // check against MOVIESET
    if (searchOptions.containsKey(MovieSearchOptions.MOVIESET)) {
      Boolean isInSet = (Boolean) searchOptions.get(MovieSearchOptions.MOVIESET);
      if ((movie.getMovieSet() != null) != isInSet) {
        return false;
      }
    }

    // check against video format
    if (searchOptions.containsKey(MovieSearchOptions.VIDEO_FORMAT)) {
      String videoFormat = (String) searchOptions.get(MovieSearchOptions.VIDEO_FORMAT);
      if (videoFormat == MediaFile.VIDEO_FORMAT_HD || videoFormat == MediaFile.VIDEO_FORMAT_SD) {
        if (videoFormat == MediaFile.VIDEO_FORMAT_HD && !isVideoHD(movie.getMediaInfoVideoFormat())) {
          return false;
        }
        if (videoFormat == MediaFile.VIDEO_FORMAT_SD && isVideoHD(movie.getMediaInfoVideoFormat())) {
          return false;
        }
      }
      else {
        if (videoFormat != movie.getMediaInfoVideoFormat()) {
          return false;
        }
      }
    }

    // check against video codec
    if (searchOptions.containsKey(MovieSearchOptions.VIDEO_CODEC)) {
      String videoCodec = (String) searchOptions.get(MovieSearchOptions.VIDEO_CODEC);
      if (!videoCodec.equals(movie.getMediaInfoVideoCodec())) {
        return false;
      }
    }

    // check against audio codec
    if (searchOptions.containsKey(MovieSearchOptions.AUDIO_CODEC)) {
      String audioCodec = (String) searchOptions.get(MovieSearchOptions.AUDIO_CODEC);
      if (!containsAudioCodec(movie, audioCodec)) {
        return false;
      }
    }

    // check against datasource
    if (searchOptions.containsKey(MovieSearchOptions.DATASOURCE)) {
      String datasource = (String) searchOptions.get(MovieSearchOptions.DATASOURCE);
      if (!new File(datasource).equals(new File(movie.getDataSource()))) {
        return false;
      }
    }

    // check against missing metadata
    if (searchOptions.containsKey(MovieSearchOptions.MISSING_METADATA)) {
      if (movie.isScraped()) {
        return false;
      }
    }

    // check against missing artwork
    if (searchOptions.containsKey(MovieSearchOptions.MISSING_ARTWORK)) {
      if (movie.getHasImages()) {
        return false;
      }
    }

    // check against missing subtitles
    if (searchOptions.containsKey(MovieSearchOptions.MISSING_SUBTITLES)) {
      if (movie.hasSubtitles()) {
        return false;
      }
    }

    // check against new movies
    if (searchOptions.containsKey(MovieSearchOptions.NEW_MOVIES)) {
      if (!movie.isNewlyAdded()) {
        return false;
      }
    }

    // check against movie source
    if (searchOptions.containsKey(MovieSearchOptions.MEDIA_SOURCE)) {
      MovieMediaSource mediaSource = (MovieMediaSource) searchOptions.get(MovieSearchOptions.MEDIA_SOURCE);
      if (movie.getMediaSource() != mediaSource) {
        return false;
      }
    }

    // check against year
    if (searchOptions.containsKey(MovieSearchOptions.YEAR)) {
      Integer year = (Integer) searchOptions.get(MovieSearchOptions.YEAR);
      if (!movie.getYear().equals(year.toString())) {
        return false;
      }
    }

    return true;
  }

  private boolean isVideoHD(String videoFormat) {
    if (videoFormat == MediaFile.VIDEO_FORMAT_720P) {
      return true;
    }
    if (videoFormat == MediaFile.VIDEO_FORMAT_1080P) {
      return true;
    }
    if (videoFormat == MediaFile.VIDEO_FORMAT_4K) {
      return true;
    }
    if (videoFormat == MediaFile.VIDEO_FORMAT_8K) {
      return true;
    }
    return false;
  }

  private boolean containsAudioCodec(Movie movie, String codec) {
    List<MediaFile> videoFiles = movie.getMediaFiles(MediaFileType.VIDEO);

    if (videoFiles.size() == 0) {
      return false;
    }

    MediaFile mf = videoFiles.get(0);
    for (MediaFileAudioStream stream : mf.getAudioStreams()) {
      if (codec.equals(stream.getCodec())) {
        return true;
      }
    }

    return false;
  }

  private boolean containsTag(Movie movie, String tag) {
    for (String tagInMovie : movie.getTags()) {
      if (tagInMovie.equals(tag)) {
        return true;
      }
    }

    return false;
  }

  private boolean containsCast(Movie movie, String name) {
    if (StringUtils.isNotEmpty(name)) {
      Pattern pattern = Pattern.compile("(?i)" + Pattern.quote(name));
      java.util.regex.Matcher matcher = null;

      // director
      if (StringUtils.isNotEmpty(movie.getDirector())) {
        matcher = pattern.matcher(movie.getDirector());
        if (matcher.find()) {
          return true;
        }
      }

      // writer
      if (StringUtils.isNotEmpty(movie.getWriter())) {
        matcher = pattern.matcher(movie.getWriter());
        if (matcher.find()) {
          return true;
        }
      }

      // actors
      for (MovieActor cast : movie.getActors()) {
        if (StringUtils.isNotEmpty(cast.getName())) {
          matcher = pattern.matcher(cast.getName());
          if (matcher.find()) {
            return true;
          }
        }
      }

      // producers
      for (MovieProducer producer : movie.getProducers()) {
        if (StringUtils.isNotEmpty(producer.getName())) {
          matcher = pattern.matcher(producer.getName());
          if (matcher.find()) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
