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
package org.tinymediamanager.ui.tvshows.settings;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.text.html.HTMLEditorKit;

import org.apache.commons.lang3.StringUtils;
import org.imgscalr.Scalr;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.observablecollections.ObservableCollections;
import org.jdesktop.swingbinding.JTableBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.scraper.CountryCode;
import org.tinymediamanager.scraper.MediaLanguages;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.config.IConfigureableMediaProvider;
import org.tinymediamanager.scraper.mediaprovider.IMediaProvider;
import org.tinymediamanager.ui.TableColumnResizer;
import org.tinymediamanager.ui.UTF8Control;
import org.tinymediamanager.ui.panels.MediaScraperConfigurationPanel;
import org.tinymediamanager.ui.panels.ScrollablePanel;
import org.tinymediamanager.ui.tvshows.TvShowScraperMetadataPanel;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

/**
 * The Class TvShowScraperSettingsPanel.
 * 
 * @author Manuel Laggner
 */
public class TvShowScraperSettingsPanel extends ScrollablePanel {
  private static final long           serialVersionUID = 4999827736720726395L;
  /**
   * @wbp.nls.resourceBundle messages
   */
  private static final ResourceBundle BUNDLE           = ResourceBundle.getBundle("messages", new UTF8Control());              //$NON-NLS-1$

  private Settings                    settings         = Settings.getInstance();
  private List<TvShowScraper>         scrapers         = ObservableCollections.observableList(new ArrayList<TvShowScraper>());
  private List<ArtworkScraper>        artworkScrapers  = ObservableCollections.observableList(new ArrayList<ArtworkScraper>());

  /** UI components */
  private JComboBox                   cbScraperTmdbLanguage;
  private JComboBox                   cbCountry;
  private JCheckBox                   chckbxAutomaticallyScrapeImages;
  private JPanel                      panelScraperMetadata;
  private JPanel                      panelScraperMetadataContainer;
  private JPanel                      panelArtworkScrapers;
  private JSeparator                  separator_1;
  private JRadioButton                rdbtnThumbWithPostfix;
  private JRadioButton                rdbtnThumbWoPostfix;
  private JLabel                      lblNewLabel;
  private ButtonGroup                 btnGroupThumbFilenaming;
  private JScrollPane                 scrollPaneScraper;
  private JTable                      tableScraper;
  private JPanel                      panelScraperDetails;
  private JTextPane                   tpScraperDescription;
  private JScrollPane                 scrollPaneArtworkScraper;
  private JTable                      tableArtworkScraper;
  private JPanel                      panelArtworkScraperDetails;
  private JTextPane                   tpArtworkScraperDescription;
  private JPanel                      panelScraperOptions;
  private JPanel                      panelArtworkScraperOptions;

  /**
   * Instantiates a new movie scraper settings panel.
   */
  public TvShowScraperSettingsPanel() {
    // data init
    MediaScraper defaultMediaScraper = TvShowList.getInstance().getDefaultMediaScraper();
    int selectedIndex = 0;
    int counter = 0;
    for (MediaScraper scraper : TvShowList.getInstance().getAvailableMediaScrapers()) {
      TvShowScraper tvShowScraper = new TvShowScraper(scraper);
      if (scraper.equals(defaultMediaScraper)) {
        tvShowScraper.defaultScraper = true;
        selectedIndex = counter;
      }
      scrapers.add(tvShowScraper);
      counter++;
    }
    List<String> enabledArtworkProviders = settings.getTvShowSettings().getTvShowArtworkScrapers();
    int artworkSelectedIndex = -1;
    int counterAW = 0;
    for (MediaScraper scraper : TvShowList.getInstance().getAvailableArtworkScrapers()) {
      ArtworkScraper artworkScraper = new ArtworkScraper(scraper);
      if (enabledArtworkProviders.contains(artworkScraper.getScraperId())) {
        artworkScraper.active = true;
        if (artworkSelectedIndex < 0) {
          artworkSelectedIndex = counterAW;
        }
      }
      artworkScrapers.add(artworkScraper);
      counterAW++;
    }
    // UI init
    setLayout(new FormLayout(new ColumnSpec[] { FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), FormSpecs.RELATED_GAP_COLSPEC, },
        new RowSpec[] { FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
            FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, }));
    JPanel panelTvShowScrapers = new JPanel();
    panelTvShowScrapers.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), BUNDLE.getString("scraper.metadata.defaults"),
        TitledBorder.LEADING, TitledBorder.TOP, null, null)); // $NON-NLS-1$
    add(panelTvShowScrapers, "2, 2, fill, top");
    panelTvShowScrapers.setLayout(new FormLayout(
        new ColumnSpec[] { FormSpecs.DEFAULT_COLSPEC, FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), FormSpecs.RELATED_GAP_COLSPEC,
            ColumnSpec.decode("default:grow"), },
        new RowSpec[] { FormSpecs.LABEL_COMPONENT_GAP_ROWSPEC, RowSpec.decode("100dlu:grow"), FormSpecs.LABEL_COMPONENT_GAP_ROWSPEC,
            FormSpecs.DEFAULT_ROWSPEC, FormSpecs.LABEL_COMPONENT_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.LABEL_COMPONENT_GAP_ROWSPEC,
            FormSpecs.DEFAULT_ROWSPEC, FormSpecs.LABEL_COMPONENT_GAP_ROWSPEC, }));

    scrollPaneScraper = new JScrollPane();
    panelTvShowScrapers.add(scrollPaneScraper, "1, 2, 3, 1, fill, fill");

    tableScraper = new JTable();
    tableScraper.setRowHeight(29);
    scrollPaneScraper.setViewportView(tableScraper);

    panelScraperDetails = new JPanel();
    panelTvShowScrapers.add(panelScraperDetails, "5, 2, fill, fill");
    panelScraperDetails.setLayout(new FormLayout(new ColumnSpec[] { FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("200dlu:grow"), },
        new RowSpec[] { FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, RowSpec.decode("default:grow"), }));

    tpScraperDescription = new JTextPane();
    tpScraperDescription.setOpaque(false);
    tpScraperDescription.setEditorKit(new HTMLEditorKit());
    panelScraperDetails.add(tpScraperDescription, "2, 2, fill, fill");

    panelScraperOptions = new JPanel();
    panelScraperOptions.setLayout(new FlowLayout(FlowLayout.LEFT));
    panelScraperDetails.add(panelScraperOptions, "2, 4, fill, fill");

    JSeparator separator = new JSeparator();
    panelTvShowScrapers.add(separator, "1, 4, 5, 1");

    JLabel lblScraperLanguage = new JLabel(BUNDLE.getString("Settings.preferredLanguage")); //$NON-NLS-1$
    panelTvShowScrapers.add(lblScraperLanguage, "1, 6, right, default");

    cbScraperTmdbLanguage = new JComboBox(MediaLanguages.values());
    panelTvShowScrapers.add(cbScraperTmdbLanguage, "3, 6");

    JLabel lblCountry = new JLabel(BUNDLE.getString("Settings.certificationCountry")); //$NON-NLS-1$
    panelTvShowScrapers.add(lblCountry, "1, 8, right, default");

    cbCountry = new JComboBox(CountryCode.values());
    panelTvShowScrapers.add(cbCountry, "3, 8, fill, default");

    btnGroupThumbFilenaming = new ButtonGroup();

    panelArtworkScrapers = new JPanel();
    panelArtworkScrapers.setBorder(new TitledBorder(null, BUNDLE.getString("Settings.images"), TitledBorder.LEADING, TitledBorder.TOP, null, null));//$NON-NLS-1$
    add(panelArtworkScrapers, "2, 4, fill, fill");
    panelArtworkScrapers.setLayout(new FormLayout(
        new ColumnSpec[] { FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC, FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
            FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), FormSpecs.RELATED_GAP_COLSPEC, },
        new RowSpec[] { FormSpecs.LABEL_COMPONENT_GAP_ROWSPEC, RowSpec.decode("75dlu:grow"), FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
            FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.LABEL_COMPONENT_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
            FormSpecs.LABEL_COMPONENT_GAP_ROWSPEC, }));

    scrollPaneArtworkScraper = new JScrollPane();
    panelArtworkScrapers.add(scrollPaneArtworkScraper, "2, 2, 3, 1, fill, fill");

    tableArtworkScraper = new JTable();
    tableArtworkScraper.setRowHeight(29);
    scrollPaneArtworkScraper.setViewportView(tableArtworkScraper);

    panelArtworkScraperDetails = new JPanel();
    panelArtworkScrapers.add(panelArtworkScraperDetails, "6, 2, fill, fill");
    panelArtworkScraperDetails.setLayout(new FormLayout(new ColumnSpec[] { FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("200dlu:grow"), },
        new RowSpec[] { FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, RowSpec.decode("default:grow"), }));

    tpArtworkScraperDescription = new JTextPane();
    tpArtworkScraperDescription.setEditorKit(new HTMLEditorKit());
    tpArtworkScraperDescription.setOpaque(false);
    panelArtworkScraperDetails.add(tpArtworkScraperDescription, "2, 2, fill, fill");

    panelArtworkScraperOptions = new JPanel();
    panelArtworkScraperOptions.setLayout(new FlowLayout(FlowLayout.LEFT));
    panelArtworkScraperDetails.add(panelArtworkScraperOptions, "2, 4, fill, fill");

    separator_1 = new JSeparator();
    panelArtworkScrapers.add(separator_1, "2, 4, 5, 1");

    lblNewLabel = new JLabel(BUNDLE.getString("image.thumb.naming")); //$NON-NLS-1$
    panelArtworkScrapers.add(lblNewLabel, "2, 6");
    rdbtnThumbWithPostfix = new JRadioButton("<dynamic>-thumb.ext");
    btnGroupThumbFilenaming.add(rdbtnThumbWithPostfix);
    panelArtworkScrapers.add(rdbtnThumbWithPostfix, "4, 6");
    rdbtnThumbWithPostfix.setSelected(settings.getTvShowSettings().isUseRenamerThumbPostfix());

    rdbtnThumbWoPostfix = new JRadioButton("<dynamic>.ext");
    btnGroupThumbFilenaming.add(rdbtnThumbWoPostfix);
    panelArtworkScrapers.add(rdbtnThumbWoPostfix, "4, 8");
    rdbtnThumbWoPostfix.setSelected(!settings.getTvShowSettings().isUseRenamerThumbPostfix());

    panelScraperMetadataContainer = new JPanel();
    panelScraperMetadataContainer.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"),
        BUNDLE.getString("scraper.metadata.defaults"), TitledBorder.LEADING, TitledBorder.TOP, null, new Color(51, 51, 51))); //$NON-NLS-1$
    add(panelScraperMetadataContainer, "2, 6, fill, top");
    panelScraperMetadataContainer.setLayout(new FormLayout(new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), },
        new RowSpec[] { FormFactory.DEFAULT_ROWSPEC, FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC, }));

    panelScraperMetadata = new TvShowScraperMetadataPanel(settings.getTvShowScraperMetadataConfig());
    panelScraperMetadataContainer.add(panelScraperMetadata, "1, 1, 2, 1, fill, default");

    chckbxAutomaticallyScrapeImages = new JCheckBox(BUNDLE.getString("Settings.default.autoscrape")); //$NON-NLS-1$
    panelScraperMetadataContainer.add(chckbxAutomaticallyScrapeImages, "2, 3");

    initDataBindings();

    // adjust table columns
    // Checkbox and Logo shall have minimal width
    TableColumnResizer.setMaxWidthForColumn(tableScraper, 0, 2);
    TableColumnResizer.setMaxWidthForColumn(tableScraper, 1, 2);
    TableColumnResizer.adjustColumnPreferredWidths(tableScraper, 5);

    TableColumnResizer.setMaxWidthForColumn(tableArtworkScraper, 0, 2);
    TableColumnResizer.setMaxWidthForColumn(tableArtworkScraper, 1, 2);
    TableColumnResizer.adjustColumnPreferredWidths(tableArtworkScraper, 5);

    // implement listener to simulate button group
    tableScraper.getModel().addTableModelListener(new TableModelListener() {
      @Override
      public void tableChanged(TableModelEvent arg0) {
        // click on the checkbox
        if (arg0.getColumn() == 0) {
          int row = arg0.getFirstRow();
          TvShowScraper changedScraper = scrapers.get(row);
          // if flag inNFO was changed, change all other trailers flags
          if (changedScraper.getDefaultScraper()) {
            settings.getTvShowSettings().setTvShowScraper(changedScraper.getScraperId());
            for (TvShowScraper scraper : scrapers) {
              if (scraper != changedScraper) {
                scraper.setDefaultScraper(Boolean.FALSE);
              }
            }
          }
        }
      }
    });

    // implement selection listener to load settings
    tableScraper.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        int index = tableScraper.convertRowIndexToModel(tableScraper.getSelectedRow());
        if (index > -1) {
          panelScraperOptions.removeAll();
          if (scrapers.get(index).getMediaProvider() instanceof IConfigureableMediaProvider) {
            panelScraperOptions.add(new MediaScraperConfigurationPanel((IConfigureableMediaProvider) scrapers.get(index).getMediaProvider()));
          }
          panelScraperOptions.revalidate();
        }
      }
    });

    tableArtworkScraper.getModel().addTableModelListener(new TableModelListener() {
      @Override
      public void tableChanged(TableModelEvent arg0) {
        // click on the checkbox
        if (arg0.getColumn() == 0) {
          int row = arg0.getFirstRow();
          ArtworkScraper changedScraper = artworkScrapers.get(row);
          if (changedScraper.active) {
            settings.getTvShowSettings().addTvShowArtworkScraper(changedScraper.getScraperId());
          }
          else {
            settings.getTvShowSettings().removeTvShowArtworkScraper(changedScraper.getScraperId());
          }
        }
      }
    });
    // implement selection listener to load settings
    tableArtworkScraper.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        int index = tableArtworkScraper.convertRowIndexToModel(tableArtworkScraper.getSelectedRow());
        if (index > -1) {
          panelArtworkScraperOptions.removeAll();
          if (artworkScrapers.get(index).getMediaProvider() instanceof IConfigureableMediaProvider) {
            panelArtworkScraperOptions
                .add(new MediaScraperConfigurationPanel((IConfigureableMediaProvider) artworkScrapers.get(index).getMediaProvider()));
          }
          panelArtworkScraperOptions.revalidate();
        }
      }
    });

    // select default TV show scraper
    if (counter > 0) {
      tableScraper.getSelectionModel().setSelectionInterval(selectedIndex, selectedIndex);
    }

    // select default artwork scraper
    if (counterAW > 0) {
      tableArtworkScraper.getSelectionModel().setSelectionInterval(artworkSelectedIndex, artworkSelectedIndex);
    }

    ItemListener itemListener = new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        checkChanges();
      }
    };
    rdbtnThumbWithPostfix.addItemListener(itemListener);
    rdbtnThumbWoPostfix.addItemListener(itemListener);
  }

  /**
   * Check changes.
   */
  public void checkChanges() {
    settings.getTvShowSettings().setUseRenamerThumbPostfix(rdbtnThumbWithPostfix.isSelected());
  }

  /*****************************************************************************************************
   * helper classes
   ****************************************************************************************************/
  public static class TvShowScraper extends AbstractModelObject {
    private MediaScraper scraper;
    private Icon         scraperLogo;
    private boolean      defaultScraper;

    public TvShowScraper(MediaScraper scraper) {
      this.scraper = scraper;
      if (scraper.getMediaProvider() == null || scraper.getMediaProvider().getProviderInfo() == null
          || scraper.getMediaProvider().getProviderInfo().getProviderLogo() == null) {
        scraperLogo = new ImageIcon();
      }
      else {
        scraperLogo = getScaledIcon(new ImageIcon(scraper.getMediaProvider().getProviderInfo().getProviderLogo()));
      }
    }

    private ImageIcon getScaledIcon(ImageIcon original) {
      Canvas c = new Canvas();
      FontMetrics fm = c.getFontMetrics(new JPanel().getFont());

      int height = (int) (fm.getHeight() * 2f);
      int width = original.getIconWidth() / original.getIconHeight() * height;

      BufferedImage scaledImage = Scalr.resize(com.bric.image.ImageLoader.createImage(original.getImage()), Scalr.Method.QUALITY,
          Scalr.Mode.AUTOMATIC, width, height, Scalr.OP_ANTIALIAS);
      return new ImageIcon(scaledImage);
    }

    public String getScraperId() {
      return scraper.getId();
    }

    public String getScraperName() {
      return scraper.getName();
    }

    public String getScraperDescription() {
      // first try to get the localized version
      String description = null;
      try {
        description = BUNDLE.getString("scraper." + scraper.getId() + ".hint"); //$NON-NLS-1$
      }
      catch (Exception ignored) {
      }

      if (StringUtils.isBlank(description)) {
        // try to get a scraper text
        description = scraper.getDescription();
      }

      return description;
    }

    public Icon getScraperLogo() {
      return scraperLogo;
    }

    public Boolean getDefaultScraper() {
      return defaultScraper;
    }

    public void setDefaultScraper(Boolean newValue) {
      Boolean oldValue = this.defaultScraper;
      this.defaultScraper = newValue;
      firePropertyChange("defaultScraper", oldValue, newValue);
    }

    public IMediaProvider getMediaProvider() {
      return scraper.getMediaProvider();
    }
  }

  public class ArtworkScraper extends AbstractModelObject {
    private MediaScraper scraper;
    private Icon         scraperLogo;
    private boolean      active;

    public ArtworkScraper(MediaScraper scraper) {
      this.scraper = scraper;
      if (scraper.getMediaProvider().getProviderInfo().getProviderLogo() == null) {
        scraperLogo = new ImageIcon();
      }
      else {
        scraperLogo = getScaledIcon(new ImageIcon(scraper.getMediaProvider().getProviderInfo().getProviderLogo()));
      }
    }

    private ImageIcon getScaledIcon(ImageIcon original) {
      Canvas c = new Canvas();
      FontMetrics fm = c.getFontMetrics(getFont());

      int height = (int) (fm.getHeight() * 2f);
      int width = original.getIconWidth() / original.getIconHeight() * height;

      BufferedImage scaledImage = Scalr.resize(com.bric.image.ImageLoader.createImage(original.getImage()), Scalr.Method.QUALITY,
          Scalr.Mode.AUTOMATIC, width, height, Scalr.OP_ANTIALIAS);
      return new ImageIcon(scaledImage);
    }

    public String getScraperId() {
      return scraper.getId();
    }

    public String getScraperName() {
      return scraper.getName();
    }

    public String getScraperDescription() {
      // first try to get the localized version
      String description = null;
      try {
        description = BUNDLE.getString("scraper." + scraper.getId() + ".hint"); //$NON-NLS-1$
      }
      catch (Exception ignored) {
      }

      if (StringUtils.isBlank(description)) {
        // try to get a scraper text
        description = scraper.getDescription();
      }

      return description;
    }

    public Icon getScraperLogo() {
      return scraperLogo;
    }

    public Boolean getActive() {
      return active;
    }

    public void setActive(Boolean newValue) {
      Boolean oldValue = this.active;
      this.active = newValue;
      firePropertyChange("active", oldValue, newValue);
    }

    public IMediaProvider getMediaProvider() {
      return scraper.getMediaProvider();
    }
  }

  @SuppressWarnings("rawtypes")
  protected void initDataBindings() {
    BeanProperty<Settings, MediaLanguages> settingsBeanProperty_8 = BeanProperty.create("tvShowSettings.scraperLanguage");
    BeanProperty<JComboBox, Object> jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding<Settings, MediaLanguages, JComboBox, Object> autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_8, cbScraperTmdbLanguage, jComboBoxBeanProperty);
    autoBinding_7.bind();
    //
    BeanProperty<Settings, CountryCode> settingsBeanProperty_9 = BeanProperty.create("tvShowSettings.certificationCountry");
    AutoBinding<Settings, CountryCode, JComboBox, Object> autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_9, cbCountry, jComboBoxBeanProperty);
    autoBinding_8.bind();
    //
    BeanProperty<Settings, Boolean> settingsBeanProperty = BeanProperty.create("tvShowSettings.scrapeBestImage");
    BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding<Settings, Boolean, JCheckBox, Boolean> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty, chckbxAutomaticallyScrapeImages, jCheckBoxBeanProperty);
    autoBinding.bind();
    //
    JTableBinding<TvShowScraper, List<TvShowScraper>, JTable> jTableBinding = SwingBindings.createJTableBinding(UpdateStrategy.READ_WRITE, scrapers,
        tableScraper);
    //
    BeanProperty<TvShowScraper, Boolean> tvShowScraperBeanProperty = BeanProperty.create("defaultScraper");
    jTableBinding.addColumnBinding(tvShowScraperBeanProperty).setColumnName("Default").setColumnClass(Boolean.class);
    //
    BeanProperty<TvShowScraper, Icon> tvShowScraperBeanProperty_1 = BeanProperty.create("scraperLogo");
    jTableBinding.addColumnBinding(tvShowScraperBeanProperty_1).setColumnName("Logo").setColumnClass(Icon.class);
    //
    BeanProperty<TvShowScraper, String> tvShowScraperBeanProperty_2 = BeanProperty.create("scraperName");
    jTableBinding.addColumnBinding(tvShowScraperBeanProperty_2).setColumnName("Name").setEditable(false);
    //
    jTableBinding.bind();
    //
    BeanProperty<JTable, String> jTableBeanProperty = BeanProperty.create("selectedElement.scraperDescription");
    BeanProperty<JTextPane, String> jTextPaneBeanProperty = BeanProperty.create("text");
    AutoBinding<JTable, String, JTextPane, String> autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ, tableScraper, jTableBeanProperty,
        tpScraperDescription, jTextPaneBeanProperty);
    autoBinding_3.bind();
    //
    JTableBinding<ArtworkScraper, List<ArtworkScraper>, JTable> jTableBinding_1 = SwingBindings.createJTableBinding(UpdateStrategy.READ_WRITE,
        artworkScrapers, tableArtworkScraper);
    //
    BeanProperty<ArtworkScraper, Boolean> artworkScraperBeanProperty = BeanProperty.create("active");
    jTableBinding_1.addColumnBinding(artworkScraperBeanProperty).setColumnName("Active").setColumnClass(Boolean.class);
    //
    BeanProperty<ArtworkScraper, Icon> artworkScraperBeanProperty_1 = BeanProperty.create("scraperLogo");
    jTableBinding_1.addColumnBinding(artworkScraperBeanProperty_1).setColumnName("Logo").setEditable(false).setColumnClass(ImageIcon.class);
    //
    BeanProperty<ArtworkScraper, String> artworkScraperBeanProperty_2 = BeanProperty.create("scraperName");
    jTableBinding_1.addColumnBinding(artworkScraperBeanProperty_2).setColumnName("Name").setEditable(false).setColumnClass(String.class);
    //
    jTableBinding_1.bind();
    //
    BeanProperty<JTextPane, String> jTextPaneBeanProperty_1 = BeanProperty.create("text");
    AutoBinding<JTable, String, JTextPane, String> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ, tableArtworkScraper,
        jTableBeanProperty, tpArtworkScraperDescription, jTextPaneBeanProperty_1);
    autoBinding_1.bind();
  }
}
