/*
 * Copyright 2012 - 2014 Manuel Laggner
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
package org.tinymediamanager.ui.tvshows.panels.episode;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.ColumnLayout;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.UTF8Control;
import org.tinymediamanager.ui.components.ColumnImageLabel;
import org.tinymediamanager.ui.components.ImageLabel.Position;
import org.tinymediamanager.ui.components.StarRater;
import org.tinymediamanager.ui.converter.WatchedIconConverter;
import org.tinymediamanager.ui.panels.MediaInformationLogosPanel;
import org.tinymediamanager.ui.tvshows.TvShowEpisodeSelectionModel;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;

import static org.tinymediamanager.core.Constants.SEASON_POSTER;
import static org.tinymediamanager.core.Constants.THUMB;

/**
 * The Class TvShowEpisodeInformationPanel.
 * 
 * @author Manuel Laggner
 */
public class TvShowEpisodeInformationPanel extends JPanel {
  private static final long           serialVersionUID = 2032708149757390567L;
  private static final ResourceBundle BUNDLE           = ResourceBundle.getBundle("messages", new UTF8Control()); //$NON-NLS-1$

  private TvShowEpisodeSelectionModel tvShowEpisodeSelectionModel;

  /** UI components */
  private JSplitPane                  splitPaneVertical;
  private JPanel                      panelTop;
  private StarRater                   panelRatingStars;
  private JLabel                      lblTvShowName;
  private JLabel                      lblRating;
  private JLabel                      lblVoteCount;
  private JLabel                      lblEpisodeTitle;
  private ColumnImageLabel            lblEpisodeThumb;
  private ColumnImageLabel            lblSeasonPoster;
  private JPanel                      panelBottom;
  private JTextPane                   tpOverview;
  private MediaInformationLogosPanel  panelLogos;
  private JPanel                      panelActors;
  private JPanel                      panelDetails;
  private JPanel                      panelWatched;
  private JLabel                      lblWatched;
  private JPanel                      panelLeft;
  private JLabel                      lblSeasonPosterSize;
  private JLabel                      lblEpisodeThumbSize;
  private JLabel                      lblPlot;

  /**
   * Instantiates a new tv show information panel.
   * 
   * @param tvShowEpisodeSelectionModel
   *          the tv show selection model
   */
  public TvShowEpisodeInformationPanel(TvShowEpisodeSelectionModel tvShowEpisodeSelectionModel) {
    this.tvShowEpisodeSelectionModel = tvShowEpisodeSelectionModel;
    setLayout(new FormLayout(new ColumnSpec[] { FormFactory.UNRELATED_GAP_COLSPEC, ColumnSpec.decode("180px:grow"),
        FormFactory.UNRELATED_GAP_COLSPEC, ColumnSpec.decode("500px:grow(3)"), FormFactory.UNRELATED_GAP_COLSPEC, }, new RowSpec[] {
        FormFactory.PARAGRAPH_GAP_ROWSPEC, RowSpec.decode("fill:default:grow"), FormFactory.PARAGRAPH_GAP_ROWSPEC, }));

    panelLeft = new JPanel();
    panelLeft.setLayout(new ColumnLayout());
    add(panelLeft, "2, 2, fill, fill");

    lblSeasonPoster = new ColumnImageLabel(false);
    lblSeasonPoster.setDefaultAspectRatio(2 / 3f);

    panelLeft.add(lblSeasonPoster);
    lblSeasonPoster.setAlternativeText(BUNDLE.getString("image.notfound.poster")); //$NON-NLS-1$
    lblSeasonPoster.setPosition(Position.BOTTOM_LEFT);
    lblSeasonPoster.enableLightbox();

    lblSeasonPosterSize = new JLabel(BUNDLE.getString("mediafiletype.season_poster")); //$NON-NLS-1$
    panelLeft.add(lblSeasonPosterSize);
    panelLeft.add(Box.createVerticalStrut(20));

    lblEpisodeThumb = new ColumnImageLabel(false);
    lblEpisodeThumb.setDefaultAspectRatio(16 / 9f);
    panelLeft.add(lblEpisodeThumb);
    lblEpisodeThumb.setAlternativeText(BUNDLE.getString("image.notfound.thumb")); //$NON-NLS-1$
    lblEpisodeThumb.setPosition(Position.BOTTOM_LEFT);
    lblEpisodeThumb.enableLightbox();

    lblEpisodeThumbSize = new JLabel(BUNDLE.getString("mediafiletype.thumb")); //$NON-NLS-1$
    panelLeft.add(lblEpisodeThumbSize);

    splitPaneVertical = new JSplitPane();
    splitPaneVertical.setBorder(null);
    splitPaneVertical.setResizeWeight(0.5);
    splitPaneVertical.setContinuousLayout(true);
    splitPaneVertical.setOneTouchExpandable(true);
    splitPaneVertical.setOrientation(JSplitPane.VERTICAL_SPLIT);
    add(splitPaneVertical, "4, 2, fill, fill");

    panelTop = new JPanel();
    panelTop.setBorder(null);
    splitPaneVertical.setTopComponent(panelTop);
    panelTop.setLayout(new FormLayout(new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("200px:grow"),
        FormFactory.RELATED_GAP_COLSPEC, FormFactory.DEFAULT_COLSPEC, }, new RowSpec[] { RowSpec.decode("fill:default"), FormFactory.DEFAULT_ROWSPEC,
        FormFactory.NARROW_LINE_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC, FormFactory.NARROW_LINE_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC,
        FormFactory.NARROW_LINE_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC, FormFactory.NARROW_LINE_GAP_ROWSPEC, RowSpec.decode("fill:36px"),
        FormFactory.NARROW_LINE_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC, FormFactory.NARROW_LINE_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC,
        RowSpec.decode("top:50px:grow(2)"), }));

    JPanel panelTvShowHeader = new JPanel();
    panelTop.add(panelTvShowHeader, "2, 1, 3, 1, fill, top");
    panelTvShowHeader.setBorder(null);
    panelTvShowHeader.setLayout(new BorderLayout(0, 0));

    JPanel panelMovieTitle = new JPanel();
    panelTvShowHeader.add(panelMovieTitle, BorderLayout.NORTH);
    panelMovieTitle.setLayout(new BorderLayout(0, 0));
    lblTvShowName = new JLabel("");
    panelMovieTitle.add(lblTvShowName);
    TmmFontHelper.changeFont(lblTvShowName, 1.33, Font.BOLD);

    panelWatched = new JPanel();
    panelMovieTitle.add(panelWatched, BorderLayout.EAST);

    lblWatched = new JLabel("");
    panelWatched.add(lblWatched);

    JPanel panelRatingTagline = new JPanel();
    panelTvShowHeader.add(panelRatingTagline, BorderLayout.CENTER);
    panelRatingTagline.setLayout(new FormLayout(new ColumnSpec[] { FormFactory.DEFAULT_COLSPEC, FormFactory.DEFAULT_COLSPEC,
        ColumnSpec.decode("default:grow"), }, new RowSpec[] { FormFactory.LINE_GAP_ROWSPEC, RowSpec.decode("24px"), FormFactory.DEFAULT_ROWSPEC, }));

    lblRating = new JLabel("");
    panelRatingTagline.add(lblRating, "2, 2, left, center");

    lblVoteCount = new JLabel("");
    panelRatingTagline.add(lblVoteCount, "3, 2, left, center");

    panelRatingStars = new StarRater(10, 1);
    panelRatingTagline.add(panelRatingStars, "1, 2, left, top");
    panelRatingStars.setEnabled(false);

    lblEpisodeTitle = new JLabel();
    panelRatingTagline.add(lblEpisodeTitle, "1, 3, 3, 1, default, center");

    JSeparator separator = new JSeparator();
    panelTop.add(separator, "2, 4, 3, 1");

    panelDetails = new TvShowEpisodeDetailsPanel(tvShowEpisodeSelectionModel);
    panelTop.add(panelDetails, "2, 6, 3, 1");

    panelTop.add(new JSeparator(), "2, 8, 3, 1");

    panelLogos = new MediaInformationLogosPanel();
    panelTop.add(panelLogos, "2, 10, 3, 1, left, default");

    panelTop.add(new JSeparator(), "2, 12, 3, 1");

    lblPlot = new JLabel(BUNDLE.getString("metatag.plot")); //$NON-NLS-1$
    lblPlot.setFont(lblPlot.getFont().deriveFont(Font.BOLD));
    panelTop.add(lblPlot, "2, 14");

    JScrollPane scrollPaneOverview = new JScrollPane();
    scrollPaneOverview.setBorder(null);
    tpOverview = new JTextPane();
    tpOverview.setOpaque(false);
    tpOverview.setEditable(false);
    scrollPaneOverview.setViewportView(tpOverview);

    JPanel panelOverview = new JPanel();
    panelTop.add(panelOverview, "2, 15, 3, 1, fill, fill");
    panelOverview.setLayout(new FormLayout(new ColumnSpec[] { ColumnSpec.decode("default:grow"), }, new RowSpec[] { FormFactory.LINE_GAP_ROWSPEC,
        RowSpec.decode("fill:default:grow"), }));
    panelOverview.add(scrollPaneOverview, "1, 2, fill, fill");

    panelBottom = new JPanel();
    panelBottom.setLayout(new FormLayout(new ColumnSpec[] { ColumnSpec.decode("200px:grow"), FormFactory.RELATED_GAP_COLSPEC,
        FormFactory.DEFAULT_COLSPEC, }, new RowSpec[] { FormFactory.LINE_GAP_ROWSPEC, RowSpec.decode("default:grow"), }));
    splitPaneVertical.setBottomComponent(panelBottom);

    panelActors = new TvShowEpisodeCastPanel(tvShowEpisodeSelectionModel);
    panelBottom.add(panelActors, "1, 2, fill, fill");
    // beansbinding init
    initDataBindings();

    // manual coded binding
    PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        String property = propertyChangeEvent.getPropertyName();
        Object source = propertyChangeEvent.getSource();
        // react on selection of a movie and change of a movie
        if (source instanceof TvShowEpisodeSelectionModel || (source instanceof TvShowEpisode && Constants.MEDIA_FILES.equals(property))) {
          TvShowEpisode episode = null;
          if (source instanceof TvShowEpisodeSelectionModel) {
            TvShowEpisodeSelectionModel model = (TvShowEpisodeSelectionModel) source;
            episode = model.getSelectedTvShowEpisode();

          }
          if (source instanceof TvShowEpisode) {
            episode = (TvShowEpisode) source;
          }

          setSeasonPoster(episode);
          setEpisodeThumb(episode);
          panelLogos.setMediaInformationSource(episode);
        }
        if ((source.getClass() == TvShowEpisode.class && THUMB.equals(property))) {
          TvShowEpisode episode = (TvShowEpisode) source;
          setEpisodeThumb(episode);
        }
        if ((source.getClass() == TvShowEpisode.class && SEASON_POSTER.equals(property))) {
          TvShowEpisode episode = (TvShowEpisode) source;
          setSeasonPoster(episode);
        }
      }
    };

    this.tvShowEpisodeSelectionModel.addPropertyChangeListener(propertyChangeListener);
  }

  protected void initDataBindings() {
    BeanProperty<TvShowEpisodeSelectionModel, String> tvShowEpisodeSelectionModelBeanProperty = BeanProperty
        .create("selectedTvShowEpisode.tvShow.title");
    BeanProperty<JLabel, String> jLabelBeanProperty = BeanProperty.create("text");
    AutoBinding<TvShowEpisodeSelectionModel, String, JLabel, String> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowEpisodeSelectionModel, tvShowEpisodeSelectionModelBeanProperty, lblTvShowName, jLabelBeanProperty);
    autoBinding.bind();
    //
    BeanProperty<TvShowEpisodeSelectionModel, String> tvShowEpisodeSelectionModelBeanProperty_1 = BeanProperty
        .create("selectedTvShowEpisode.titleForUi");
    AutoBinding<TvShowEpisodeSelectionModel, String, JLabel, String> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowEpisodeSelectionModel, tvShowEpisodeSelectionModelBeanProperty_1, lblEpisodeTitle, jLabelBeanProperty);
    autoBinding_1.bind();
    //
    BeanProperty<TvShowEpisodeSelectionModel, String> tvShowEpisodeSelectionModelBeanProperty_3 = BeanProperty.create("selectedTvShowEpisode.plot");
    BeanProperty<JTextPane, String> jTextPaneBeanProperty = BeanProperty.create("text");
    AutoBinding<TvShowEpisodeSelectionModel, String, JTextPane, String> autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowEpisodeSelectionModel, tvShowEpisodeSelectionModelBeanProperty_3, tpOverview, jTextPaneBeanProperty);
    autoBinding_3.bind();
    //
    BeanProperty<TvShowEpisodeSelectionModel, Float> tvShowEpisodeSelectionModelBeanProperty_4 = BeanProperty.create("selectedTvShowEpisode.rating");
    BeanProperty<StarRater, Float> starRaterBeanProperty = BeanProperty.create("rating");
    AutoBinding<TvShowEpisodeSelectionModel, Float, StarRater, Float> autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowEpisodeSelectionModel, tvShowEpisodeSelectionModelBeanProperty_4, panelRatingStars, starRaterBeanProperty);
    autoBinding_4.bind();
    //
    AutoBinding<TvShowEpisodeSelectionModel, Float, JLabel, String> autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowEpisodeSelectionModel, tvShowEpisodeSelectionModelBeanProperty_4, lblRating, jLabelBeanProperty);
    autoBinding_5.bind();
    //
    BeanProperty<TvShowEpisodeSelectionModel, Boolean> tvShowEpisodeSelectionModelBeanProperty_10 = BeanProperty
        .create("selectedTvShowEpisode.watched");
    BeanProperty<JLabel, Icon> jLabelBeanProperty_1 = BeanProperty.create("icon");
    AutoBinding<TvShowEpisodeSelectionModel, Boolean, JLabel, Icon> autoBinding_11 = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowEpisodeSelectionModel, tvShowEpisodeSelectionModelBeanProperty_10, lblWatched, jLabelBeanProperty_1);
    autoBinding_11.setConverter(new WatchedIconConverter());
    autoBinding_11.bind();
  }

  private void setSeasonPoster(TvShowEpisode tvShowEpisode) {
    lblSeasonPoster.clearImage();
    lblSeasonPoster.setImagePath(tvShowEpisode.getTvShowSeason().getPoster());
    Dimension posterSize = tvShowEpisode.getTvShowSeason().getPosterSize();
    if (posterSize.width > 0 && posterSize.height > 0) {
      lblSeasonPosterSize.setText(BUNDLE.getString("mediafiletype.season_poster") + " - " + posterSize.width + "x" + posterSize.height); //$NON-NLS-1$
    }
    else {
      lblSeasonPosterSize.setText(BUNDLE.getString("mediafiletype.season_poster")); //$NON-NLS-1$
    }
  }

  private void setEpisodeThumb(TvShowEpisode tvShowEpisode) {
    lblEpisodeThumb.clearImage();
    lblEpisodeThumb.setImagePath(tvShowEpisode.getArtworkFilename(MediaFileType.THUMB));
    Dimension thumbSize = tvShowEpisode.getArtworkDimension(MediaFileType.THUMB);
    if (thumbSize.width > 0 && thumbSize.height > 0) {
      lblEpisodeThumbSize.setText(BUNDLE.getString("mediafiletype.thumb") + " - " + thumbSize.width + "x" + thumbSize.height); //$NON-NLS-1$
    }
    else {
      lblEpisodeThumbSize.setText(BUNDLE.getString("mediafiletype.thumb")); //$NON-NLS-1$
    }
  }
}