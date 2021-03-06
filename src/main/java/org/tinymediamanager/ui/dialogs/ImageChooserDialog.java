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
package org.tinymediamanager.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.apache.commons.lang3.StringUtils;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.scraper.MediaArtwork;
import org.tinymediamanager.scraper.MediaArtwork.ImageSizeAndUrl;
import org.tinymediamanager.scraper.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.MediaScrapeOptions;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.MediaType;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.scraper.mediaprovider.IMediaArtworkProvider;
import org.tinymediamanager.ui.EqualsLayout;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.ToggleButtonUI;
import org.tinymediamanager.ui.UTF8Control;
import org.tinymediamanager.ui.WrapLayout;
import org.tinymediamanager.ui.components.EnhancedTextField;
import org.tinymediamanager.ui.components.ImageLabel;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * The Class ImageChooser. Let the user choose the right image for the media entity
 * 
 * @author Manuel Laggner
 */
public class ImageChooserDialog extends TmmDialog {
  private static final long           serialVersionUID = 8193355920006275933L;
  /**
   * @wbp.nls.resourceBundle messages
   */
  private static final ResourceBundle BUNDLE           = ResourceBundle.getBundle("messages", new UTF8Control()); //$NON-NLS-1$
  private static final Logger         LOGGER           = LoggerFactory.getLogger(ImageChooserDialog.class);

  public enum ImageType {
    POSTER, FANART, BANNER, SEASON, LOGO, CLEARART, DISC, THUMB;
  }

  private DownloadTask         task;
  private List<MediaScraper>   artworkScrapers;
  private List<String>         extraThumbs;
  private List<String>         extraFanarts;

  /** UI components */
  private final JPanel         contentPanel    = new JPanel();
  private JProgressBar         progressBar;
  private JLabel               lblProgressAction;
  private JPanel               panelImages;
  private ImageLabel           imageLabel;
  private JScrollPane          scrollPane;
  private ImageType            type;
  private MediaType            mediaType;
  private ButtonGroup          buttonGroup     = new ButtonGroup();
  private List<JToggleButton>  buttons         = new ArrayList<JToggleButton>();

  private final Action         actionOK        = new OkAction();
  private final Action         actionCancel    = new CancelAction();
  private final ToggleButtonUI toggleButtonUI  = new ToggleButtonUI();
  private final Action         actionLocalFile = new LocalFileChooseAction();
  private JTextField           tfImageUrl;

  /**
   * Instantiates a new image chooser dialog.
   * 
   * @param ids
   *          the ids
   * @param type
   *          the type
   * @param artworkScrapers
   *          the artwork providers
   * @param imageLabel
   *          the image label
   * @param extraThumbs
   *          the extra thumbs
   * @param extraFanarts
   *          the extra fanarts
   * @param mediaType
   *          the media for for which artwork has to be chosen
   */
  public ImageChooserDialog(final HashMap<String, Object> ids, ImageType type, List<MediaScraper> artworkScrapers, ImageLabel imageLabel,
      List<String> extraThumbs, List<String> extraFanarts, MediaType mediaType) {
    super("", "imageChooser");
    this.imageLabel = imageLabel;
    this.type = type;
    this.mediaType = mediaType;
    this.artworkScrapers = artworkScrapers;
    this.extraThumbs = extraThumbs;
    this.extraFanarts = extraFanarts;

    switch (type) {
      case FANART:
        setTitle(BUNDLE.getString("image.choose.fanart")); //$NON-NLS-1$
        break;

      case POSTER:
        setTitle(BUNDLE.getString("image.choose.poster")); //$NON-NLS-1$
        break;

      case BANNER:
        setTitle(BUNDLE.getString("image.choose.banner")); //$NON-NLS-1$
        break;

      case SEASON:
        Object season = ids.get("tvShowSeason");
        if (season != null) {
          setTitle(BUNDLE.getString("image.choose.season") + " - " + BUNDLE.getString("metatag.season") + " " + season); //$NON-NLS-1$
        }
        else {
          setTitle(BUNDLE.getString("image.choose.season")); //$NON-NLS-1$
        }
        break;

      case CLEARART:
        setTitle(BUNDLE.getString("image.choose.clearart")); //$NON-NLS-1$
        break;

      case DISC:
        setTitle(BUNDLE.getString("image.choose.disc")); //$NON-NLS-1$
        break;

      case LOGO:
        setTitle(BUNDLE.getString("image.choose.logo")); //$NON-NLS-1$
        break;

      case THUMB:
        setTitle(BUNDLE.getString("image.choose.thumb")); //$NON-NLS-1$
        break;
    }

    setBounds(5, 5, 1000, 590);

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    contentPanel.setLayout(new FormLayout(
        new ColumnSpec[] { FormFactory.LABEL_COMPONENT_GAP_COLSPEC, ColumnSpec.decode("258px:grow"), FormFactory.RELATED_GAP_COLSPEC,
            FormFactory.DEFAULT_COLSPEC, FormFactory.LABEL_COMPONENT_GAP_COLSPEC, },
        new RowSpec[] { FormFactory.LINE_GAP_ROWSPEC, RowSpec.decode("fill:266px:grow"), FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC,
            FormFactory.RELATED_GAP_ROWSPEC, }));
    {
      scrollPane = new JScrollPane();
      scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      contentPanel.add(scrollPane, "2, 2, 3, 1, fill, fill");
      {
        panelImages = new JPanel();
        scrollPane.setViewportView(panelImages);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panelImages.setLayout(new WrapLayout(FlowLayout.LEFT));
      }
    }
    {
      tfImageUrl = new EnhancedTextField(BUNDLE.getString("image.inserturl")); //$NON-NLS-1$
      contentPanel.add(tfImageUrl, "2, 4, fill, default");
      tfImageUrl.setColumns(10);
      JButton btnAddImage = new JButton(BUNDLE.getString("image.downloadimage")); //$NON-NLS-1$
      btnAddImage.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (StringUtils.isNotBlank(tfImageUrl.getText())) {
            downloadAndPreviewImage(tfImageUrl.getText());
          }
        }
      });
      contentPanel.add(btnAddImage, "4, 4");

    }
    {
      JPanel bottomPane = new JPanel();
      getContentPane().add(bottomPane, BorderLayout.SOUTH);
      bottomPane.setLayout(new FormLayout(
          new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC, FormFactory.DEFAULT_COLSPEC, FormFactory.RELATED_GAP_COLSPEC,
              ColumnSpec.decode("default:grow"), FormFactory.RELATED_GAP_COLSPEC, FormFactory.DEFAULT_COLSPEC, FormFactory.RELATED_GAP_COLSPEC, },
          new RowSpec[] { FormFactory.NARROW_LINE_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC, FormFactory.NARROW_LINE_GAP_ROWSPEC,
              RowSpec.decode("23px:grow"), FormFactory.RELATED_GAP_ROWSPEC, }));
      {
        if (type == ImageType.FANART && extraFanarts != null && extraThumbs != null) {
          JPanel panelExtraButtons = new JPanel();
          bottomPane.add(panelExtraButtons, "2, 2, fill, bottom");
          panelExtraButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
          {
            if (mediaType == MediaType.MOVIE && MovieModuleManager.MOVIE_SETTINGS.isImageExtraThumbs()) {
              JLabel labelThumbs = new JLabel("Extrathumbs:");
              panelExtraButtons.add(labelThumbs);
              JButton btnMarkExtrathumbs = new JButton("");
              btnMarkExtrathumbs.setMargin(new Insets(0, 0, 0, 0));
              btnMarkExtrathumbs.setIcon(IconManager.CHECK_ALL);
              btnMarkExtrathumbs.setToolTipText(BUNDLE.getString("image.extrathumbs.markall")); //$NON-NLS-1$
              btnMarkExtrathumbs.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent arg0) {
                  for (JToggleButton button : buttons) {
                    if (button.getClientProperty("MediaArtworkExtrathumb") instanceof JCheckBox) {
                      JCheckBox chkbx = (JCheckBox) button.getClientProperty("MediaArtworkExtrathumb");
                      chkbx.setSelected(true);
                    }
                  }
                }
              });
              panelExtraButtons.add(btnMarkExtrathumbs);
              JButton btnUnMarkExtrathumbs = new JButton("");
              btnUnMarkExtrathumbs.setMargin(new Insets(0, 0, 0, 0));
              btnUnMarkExtrathumbs.setIcon(IconManager.UNCHECK_ALL);
              btnUnMarkExtrathumbs.setToolTipText(BUNDLE.getString("image.extrathumbs.unmarkall")); //$NON-NLS-1$
              btnUnMarkExtrathumbs.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                  for (JToggleButton button : buttons) {
                    if (button.getClientProperty("MediaArtworkExtrathumb") instanceof JCheckBox) {
                      JCheckBox chkbx = (JCheckBox) button.getClientProperty("MediaArtworkExtrathumb");
                      chkbx.setSelected(false);
                    }
                  }
                }
              });
              panelExtraButtons.add(btnUnMarkExtrathumbs);
            }
            if (mediaType == MediaType.MOVIE && MovieModuleManager.MOVIE_SETTINGS.isImageExtraThumbs()
                && MovieModuleManager.MOVIE_SETTINGS.isImageExtraFanart())

            {
              JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
              separator.setPreferredSize(new Dimension(2, 16));
              panelExtraButtons.add(separator);
            }
            if (mediaType == MediaType.MOVIE && MovieModuleManager.MOVIE_SETTINGS.isImageExtraFanart())

            {
              JLabel labelFanart = new JLabel("Extrafanart:");
              panelExtraButtons.add(labelFanart);
              JButton btnMarkExtrafanart = new JButton("");
              btnMarkExtrafanart.setMargin(new Insets(0, 0, 0, 0));
              btnMarkExtrafanart.setIcon(IconManager.CHECK_ALL);
              btnMarkExtrafanart.setToolTipText(BUNDLE.getString("image.extrafanart.markall")); //$NON-NLS-1$
              btnMarkExtrafanart.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                  for (JToggleButton button : buttons) {
                    if (button.getClientProperty("MediaArtworkExtrafanart") instanceof JCheckBox) {
                      JCheckBox chkbx = (JCheckBox) button.getClientProperty("MediaArtworkExtrafanart");
                      chkbx.setSelected(true);
                    }
                  }
                }
              });
              panelExtraButtons.add(btnMarkExtrafanart);
              JButton btnUnMarkExtrafanart = new JButton("");
              btnUnMarkExtrafanart.setMargin(new Insets(0, 0, 0, 0));
              btnUnMarkExtrafanart.setIcon(IconManager.UNCHECK_ALL);
              btnUnMarkExtrafanart.setToolTipText(BUNDLE.getString("image.extrafanart.unmarkall")); //$NON-NLS-1$
              btnUnMarkExtrafanart.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                  for (JToggleButton button : buttons) {
                    if (button.getClientProperty("MediaArtworkExtrafanart") instanceof JCheckBox) {
                      JCheckBox chkbx = (JCheckBox) button.getClientProperty("MediaArtworkExtrafanart");
                      chkbx.setSelected(false);
                    }
                  }
                }
              });
              panelExtraButtons.add(btnUnMarkExtrafanart);
            }
          }
        }
      }

      {
        progressBar = new JProgressBar();
        bottomPane.add(progressBar, "2, 4");
      }
      {
        lblProgressAction = new JLabel("");
        bottomPane.add(lblProgressAction, "4, 4");
      }
      {

        JPanel buttonPane = new JPanel();
        EqualsLayout layout = new EqualsLayout(5);
        buttonPane.setLayout(layout);
        layout.setMinWidth(100);
        bottomPane.add(buttonPane, "6, 4, fill, top");
        JButton okButton = new JButton(BUNDLE.getString("Button.ok")); //$NON-NLS-1$
        okButton.setAction(actionOK);
        okButton.setActionCommand("OK");
        buttonPane.add(okButton);

        getRootPane().setDefaultButton(okButton);

        JButton btnAddFile = new JButton(BUNDLE.getString("Button.addfile")); //$NON-NLS-1$
        btnAddFile.setAction(actionLocalFile);
        buttonPane.add(btnAddFile);

        JButton cancelButton = new JButton(BUNDLE.getString("Button.cancel")); //$NON-NLS-1$
        cancelButton.setAction(actionCancel);
        cancelButton.setActionCommand("Cancel");
        buttonPane.add(cancelButton);
      }

    }

    task = new DownloadTask(ids, this.artworkScrapers);
    task.execute();
  }

  private void startProgressBar(String description) {
    lblProgressAction.setText(description);
    progressBar.setVisible(true);
    progressBar.setIndeterminate(true);
  }

  private void stopProgressBar() {
    lblProgressAction.setText("");
    progressBar.setVisible(false);
    progressBar.setIndeterminate(false);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void addImage(BufferedImage originalImage, final MediaArtwork artwork) {
    Point size = null;

    GridBagLayout gbl = new GridBagLayout();

    switch (type) {
      case FANART:
      case CLEARART:
      case THUMB:
      case DISC:
        gbl.columnWidths = new int[] { 130 };
        gbl.rowHeights = new int[] { 180 };
        size = ImageCache.calculateSize(300, 150, originalImage.getWidth(), originalImage.getHeight(), true);
        break;

      case BANNER:
      case LOGO:
        gbl.columnWidths = new int[] { 130 };
        gbl.rowHeights = new int[] { 120 };
        size = ImageCache.calculateSize(300, 100, originalImage.getWidth(), originalImage.getHeight(), true);
        break;

      case POSTER:
      default:
        gbl.columnWidths = new int[] { 180 };
        gbl.rowHeights = new int[] { 270 };
        size = ImageCache.calculateSize(150, 250, originalImage.getWidth(), originalImage.getHeight(), true);
        break;

    }

    gbl.columnWeights = new double[] { Double.MIN_VALUE };
    gbl.rowWeights = new double[] { Double.MIN_VALUE };
    JPanel imagePanel = new JPanel();
    imagePanel.setLayout(gbl);

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 3;
    gbc.insets = new Insets(5, 5, 5, 5);

    JToggleButton button = new JToggleButton();
    button.setBackground(Color.white);
    button.setUI(toggleButtonUI);
    button.setMargin(new Insets(10, 10, 10, 10));
    ImageIcon imageIcon = new ImageIcon(Scalr.resize(originalImage, Scalr.Method.BALANCED, Scalr.Mode.AUTOMATIC, size.x, size.y, Scalr.OP_ANTIALIAS));
    button.setIcon(imageIcon);
    button.putClientProperty("MediaArtwork", artwork);

    buttonGroup.add(button);
    buttons.add(button);
    imagePanel.add(button, gbc);

    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.LAST_LINE_START;
    gbc.insets = new Insets(0, 5, 0, 0);

    JComboBox cb = null;
    if (artwork.getImageSizes().size() > 0) {
      cb = new JComboBox(artwork.getImageSizes().toArray());
    }
    else {
      cb = new JComboBox(new String[] { originalImage.getWidth() + "x" + originalImage.getHeight() });
    }
    button.putClientProperty("MediaArtworkSize", cb);
    imagePanel.add(cb, gbc);

    // should we provide an option for extrathumbs
    if (mediaType == MediaType.MOVIE && type == ImageType.FANART && MovieModuleManager.MOVIE_SETTINGS.isImageExtraThumbs()) {
      gbc = new GridBagConstraints();
      gbc.gridx = 1;
      gbc.gridy = 1;
      gbc.anchor = GridBagConstraints.LINE_END;
      JLabel label = new JLabel("Extrathumb");
      imagePanel.add(label, gbc);

      gbc = new GridBagConstraints();
      gbc.gridx = 2;
      gbc.gridy = 1;
      gbc.anchor = GridBagConstraints.LINE_END;
      JCheckBox chkbx = new JCheckBox();
      button.putClientProperty("MediaArtworkExtrathumb", chkbx);
      imagePanel.add(chkbx, gbc);
    }

    // should we provide an option for extrafanart
    if (mediaType == MediaType.MOVIE && type == ImageType.FANART && MovieModuleManager.MOVIE_SETTINGS.isImageExtraFanart()) {
      gbc = new GridBagConstraints();
      gbc.gridx = 1;
      gbc.gridy = MovieModuleManager.MOVIE_SETTINGS.isImageExtraThumbs() ? 2 : 1;
      gbc.anchor = GridBagConstraints.LINE_END;
      JLabel label = new JLabel("Extrafanart");
      imagePanel.add(label, gbc);

      gbc = new GridBagConstraints();
      gbc.gridx = 2;
      gbc.gridy = MovieModuleManager.MOVIE_SETTINGS.isImageExtraThumbs() ? 2 : 1;
      gbc.anchor = GridBagConstraints.LINE_END;
      JCheckBox chkbx = new JCheckBox();
      button.putClientProperty("MediaArtworkExtrafanart", chkbx);
      imagePanel.add(chkbx, gbc);
    }

    /* show image button */
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.anchor = GridBagConstraints.LAST_LINE_START;
    gbc.gridwidth = 3;
    gbc.insets = new Insets(0, 0, 0, 0);
    JButton btnShowImage = new JButton("<html><font color=\"#0000CF\"><u>" + BUNDLE.getString("image.showoriginal") + "</u></font></html>");
    btnShowImage.setBorderPainted(false);
    btnShowImage.setFocusPainted(false);
    btnShowImage.setContentAreaFilled(false);
    btnShowImage.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        ImagePreviewDialog dialog = new ImagePreviewDialog(artwork.getDefaultUrl());
        dialog.setVisible(true);
      }
    });
    imagePanel.add(btnShowImage, gbc);

    panelImages.add(imagePanel);
    panelImages.validate();
    panelImages.getParent().validate();
  }

  private void downloadAndPreviewImage(String url) {
    Runnable task = new Runnable() {
      @Override
      public void run() {
        try {
          final MediaArtwork art = new MediaArtwork();
          art.setDefaultUrl(tfImageUrl.getText());
          art.setPreviewUrl(tfImageUrl.getText());
          switch (type) {
            case BANNER:
              art.setType(MediaArtworkType.BANNER);
              break;
            case CLEARART:
              art.setType(MediaArtworkType.CLEARART);
              break;
            case DISC:
              art.setType(MediaArtworkType.DISC);
              break;
            case FANART:
              art.setType(MediaArtworkType.BACKGROUND);
              break;
            case LOGO:
              art.setType(MediaArtworkType.LOGO);
              break;
            case POSTER:
              art.setType(MediaArtworkType.POSTER);
              break;
            case SEASON:
              art.setType(MediaArtworkType.SEASON);
              break;
            case THUMB:
              art.setType(MediaArtworkType.THUMB);
              break;
            default:
              return;
          }

          Url url = new Url(art.getPreviewUrl());
          Image image = Toolkit.getDefaultToolkit().createImage(url.getBytes());
          final BufferedImage bufferedImage = com.bric.image.ImageLoader.createImage(image);

          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              addImage(bufferedImage, art);

              // scroll down
              JScrollBar vertical = scrollPane.getVerticalScrollBar();
              vertical.setValue(vertical.getMaximum());
            }
          });
          tfImageUrl.setText("");
        }
        catch (Exception e) {
          LOGGER.error("could not download manually entered image url: " + tfImageUrl.getText());
        }
      }
    };
    task.run();
  }

  private class OkAction extends AbstractAction {
    private static final long serialVersionUID = -1255049344169945137L;

    public OkAction() {
      putValue(NAME, BUNDLE.getString("Button.ok")); //$NON-NLS-1$
      putValue(SHORT_DESCRIPTION, BUNDLE.getString("image.seteselected")); //$NON-NLS-1$
      putValue(SMALL_ICON, IconManager.APPLY);
      putValue(LARGE_ICON_KEY, IconManager.APPLY);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      MediaArtwork artwork = null;
      ImageSizeAndUrl resolution = null;

      // get selected button
      for (JToggleButton button : buttons) {
        if (button.isSelected()) {
          Object clientProperty = button.getClientProperty("MediaArtwork");
          if (clientProperty instanceof MediaArtwork) {
            artwork = (MediaArtwork) clientProperty;
            clientProperty = button.getClientProperty("MediaArtworkSize");
            // try to get the size
            if (clientProperty instanceof JComboBox) {
              @SuppressWarnings("rawtypes")
              JComboBox cb = (JComboBox) clientProperty;
              if (cb.getSelectedItem() instanceof ImageSizeAndUrl) {
                resolution = (ImageSizeAndUrl) cb.getSelectedItem();
              }
            }
            break;
          }
        }
      }

      // nothing selected
      if (artwork == null) {
        JOptionPane.showMessageDialog(null, BUNDLE.getString("image.noneselected")); //$NON-NLS-1$
        return;
      }

      if (artwork != null) {
        imageLabel.clearImage();
        if (resolution != null) {
          imageLabel.setImageUrl(resolution.getUrl());
        }
        else {
          imageLabel.setImageUrl(artwork.getDefaultUrl());
        }
      }

      // extrathumbs
      if (mediaType == MediaType.MOVIE && type == ImageType.FANART && extraThumbs != null && MovieModuleManager.MOVIE_SETTINGS.isImageExtraThumbs()) {
        processExtraThumbs();
      }

      // extrafanart
      if (mediaType == MediaType.MOVIE && type == ImageType.FANART && extraThumbs != null && MovieModuleManager.MOVIE_SETTINGS.isImageExtraFanart()) {
        processExtraFanart();
      }

      task.cancel(true);
      setVisible(false);
    }

    /**
     * Process extra thumbs.
     */
    private void processExtraThumbs() {
      extraThumbs.clear();
      // get extrathumbs
      for (JToggleButton button : buttons) {
        if (button.getClientProperty("MediaArtworkExtrathumb") instanceof JCheckBox
            && button.getClientProperty("MediaArtwork") instanceof MediaArtwork
            && button.getClientProperty("MediaArtworkSize") instanceof JComboBox) {
          JCheckBox chkbx = (JCheckBox) button.getClientProperty("MediaArtworkExtrathumb");
          if (chkbx.isSelected()) {
            MediaArtwork artwork = (MediaArtwork) button.getClientProperty("MediaArtwork");
            @SuppressWarnings("rawtypes")
            JComboBox cb = (JComboBox) button.getClientProperty("MediaArtworkSize");
            if (cb.getSelectedItem() instanceof ImageSizeAndUrl) {
              ImageSizeAndUrl size = (ImageSizeAndUrl) cb.getSelectedItem();
              if (size != null) {
                extraThumbs.add(size.getUrl());
              }
              else {
                extraThumbs.add(artwork.getDefaultUrl());
              }
            }
            else if (cb.getSelectedItem() instanceof String) {
              extraThumbs.add(artwork.getDefaultUrl());
            }
          }
        }
      }
    }

    /**
     * Process extra fanart.
     */
    private void processExtraFanart() {
      extraFanarts.clear();
      // get extrafanart
      for (JToggleButton button : buttons) {
        if (button.getClientProperty("MediaArtworkExtrafanart") instanceof JCheckBox
            && button.getClientProperty("MediaArtwork") instanceof MediaArtwork
            && button.getClientProperty("MediaArtworkSize") instanceof JComboBox) {
          JCheckBox chkbx = (JCheckBox) button.getClientProperty("MediaArtworkExtrafanart");
          if (chkbx.isSelected()) {
            MediaArtwork artwork = (MediaArtwork) button.getClientProperty("MediaArtwork");
            @SuppressWarnings("rawtypes")
            JComboBox cb = (JComboBox) button.getClientProperty("MediaArtworkSize");
            if (cb.getSelectedItem() instanceof ImageSizeAndUrl) {
              ImageSizeAndUrl size = (ImageSizeAndUrl) cb.getSelectedItem();
              if (size != null) {
                extraFanarts.add(size.getUrl());
              }
              else {
                extraFanarts.add(artwork.getDefaultUrl());
              }
            }
            else if (cb.getSelectedItem() instanceof String) {
              extraFanarts.add(artwork.getDefaultUrl());
            }
          }
        }
      }
    }
  }

  private class CancelAction extends AbstractAction {
    private static final long serialVersionUID = 403327079655572423L;

    public CancelAction() {
      putValue(NAME, BUNDLE.getString("Button.cancel")); //$NON-NLS-1$
      putValue(SHORT_DESCRIPTION, BUNDLE.getString("Button.cancel")); //$NON-NLS-1$
      putValue(SMALL_ICON, IconManager.CANCEL);
      putValue(LARGE_ICON_KEY, IconManager.CANCEL);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      task.cancel(true);
      setVisible(false);
    }
  }

  private class DownloadTask extends SwingWorker<Void, DownloadChunk> {
    private HashMap<String, Object> ids;
    private List<MediaScraper>      artworkScrapers;
    private boolean                 imagesFound = false;

    public DownloadTask(HashMap<String, Object> ids, List<MediaScraper> artworkScrapers) {
      this.ids = ids;
      this.artworkScrapers = artworkScrapers;
    }

    @Override
    public Void doInBackground() {
      if (ids.isEmpty()) {
        JOptionPane.showMessageDialog(null, BUNDLE.getString("image.download.noid")); //$NON-NLS-1$
        return null;
      }

      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          startProgressBar(BUNDLE.getString("image.download.progress")); //$NON-NLS-1$
        }
      });

      if (artworkScrapers == null || artworkScrapers.size() == 0) {
        return null;
      }

      // get images from all artworkproviders
      for (MediaScraper scraper : artworkScrapers) {
        try {
          IMediaArtworkProvider artworkProvider = (IMediaArtworkProvider) scraper.getMediaProvider();
          MediaScrapeOptions options = new MediaScrapeOptions(mediaType);
          if (mediaType == MediaType.MOVIE || mediaType == MediaType.MOVIE_SET) {
            options.setLanguage(MovieModuleManager.MOVIE_SETTINGS.getScraperLanguage());
            options.setCountry(MovieModuleManager.MOVIE_SETTINGS.getCertificationCountry());
          }
          else if (mediaType == MediaType.TV_SHOW) {
            options.setLanguage(Globals.settings.getTvShowSettings().getScraperLanguage());
            options.setCountry(Globals.settings.getTvShowSettings().getCertificationCountry());
          }
          else {
            continue;
          }
          switch (type) {
            case POSTER:
              options.setArtworkType(MediaArtworkType.POSTER);
              break;

            case FANART:
              options.setArtworkType(MediaArtworkType.BACKGROUND);
              break;

            case BANNER:
              options.setArtworkType(MediaArtworkType.BANNER);
              break;

            case SEASON:
              options.setArtworkType(MediaArtworkType.SEASON);
              break;

            case CLEARART:
              options.setArtworkType(MediaArtworkType.CLEARART);
              break;

            case DISC:
              options.setArtworkType(MediaArtworkType.DISC);
              break;

            case LOGO:
              options.setArtworkType(MediaArtworkType.LOGO);
              break;

            case THUMB:
              options.setArtworkType(MediaArtworkType.THUMB);
              break;
          }

          // populate ids
          for (Entry<String, Object> entry : ids.entrySet()) {
            Object v = entry.getValue();
            if (v != null) {
              options.setId((String) entry.getKey(), v.toString());
            }
          }

          // get the artwork
          List<MediaArtwork> artwork = artworkProvider.getArtwork(options);
          if (artwork == null) {
            continue;
          }

          // display all images
          for (MediaArtwork art : artwork) {
            if (isCancelled()) {
              return null;
            }

            Url url = null;
            try {
              url = new Url(art.getPreviewUrl());
              Image image = Toolkit.getDefaultToolkit().createImage(url.getBytes());
              BufferedImage bufferedImage = com.bric.image.ImageLoader.createImage(image);

              DownloadChunk chunk = new DownloadChunk();
              chunk.artwork = art;
              chunk.image = bufferedImage;
              publish(chunk);
              imagesFound = true;
              // addImage(bufferedImage, art);
            }
            catch (InterruptedException ingored) {
              LOGGER.warn("Thread interrupted");
            }
            catch (Exception e) {
              LOGGER.error("DownloadTask displaying", e);
            }

          }
        }
        catch (Exception e) {
          LOGGER.error("DownloadTask", e);
        }
      } // end foreach scraper

      return null;
    }

    @Override
    protected void process(List<DownloadChunk> chunks) {
      for (DownloadChunk chunk : chunks) {
        addImage(chunk.image, chunk.artwork);
      }
    };

    @Override
    public void done() {
      if (!imagesFound) {
        JLabel lblNothingFound = new JLabel(BUNDLE.getString("image.download.nothingfound"));//$NON-NLS-1$
        TmmFontHelper.changeFont(lblNothingFound, 1.33);
        panelImages.add(lblNothingFound);
        panelImages.validate();
        panelImages.getParent().validate();
      }
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          stopProgressBar();
        }
      });
    }
  }

  @Override
  public void pack() {
    // do not pack - it would look weird
  }

  private class DownloadChunk {
    private BufferedImage image;
    private MediaArtwork  artwork;
  }

  private class LocalFileChooseAction extends AbstractAction {
    private static final long serialVersionUID = -1178325861474276709L;

    public LocalFileChooseAction() {
      putValue(NAME, BUNDLE.getString("image.choose.file")); //$NON-NLS-1$
      putValue(SHORT_DESCRIPTION, BUNDLE.getString("image.choose.file")); //$NON-NLS-1$
      putValue(SMALL_ICON, IconManager.FILE_OPEN);
      putValue(LARGE_ICON_KEY, IconManager.FILE_OPEN);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      File file = TmmUIHelper.selectFile(BUNDLE.getString("image.choose")); //$NON-NLS-1$
      if (file != null && file.exists() && file.isFile()) {
        String fileName = file.getPath();
        imageLabel.clearImage();
        imageLabel.setImageUrl("file:/" + fileName);
        task.cancel(true);
        setVisible(false);
      }
    }
  }
}
