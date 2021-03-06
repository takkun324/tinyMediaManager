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
package org.tinymediamanager.ui.tvshows.dialogs;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.tinymediamanager.Globals;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.MediaEpisode;
import org.tinymediamanager.scraper.MediaScrapeOptions;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.MediaType;
import org.tinymediamanager.scraper.mediaprovider.ITvShowMetadataProvider;
import org.tinymediamanager.ui.EqualsLayout;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.UTF8Control;
import org.tinymediamanager.ui.components.EnhancedTextField;
import org.tinymediamanager.ui.dialogs.TmmDialog;
import org.tinymediamanager.ui.tvshows.TvShowEpisodeChooserModel;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.AdvancedTableModel;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

/**
 * The TvShowEpisodeChooserDialog is used for searching a special episode
 * 
 * @author Manuel Laggner
 */
public class TvShowEpisodeChooserDialog extends TmmDialog implements ActionListener {
  private static final long                                serialVersionUID = 3317576458848699068L;
  /**
   * @wbp.nls.resourceBundle messages
   */
  private static final ResourceBundle                      BUNDLE           = ResourceBundle.getBundle("messages", new UTF8Control()); //$NON-NLS-1$

  private TvShowEpisode                                    episode;
  private MediaScraper                                     mediaScraper;
  private MediaEpisode                                     metadata;
  private ObservableElementList<TvShowEpisodeChooserModel> episodeEventList;

  private JTable                                           table;
  private JTextArea                                        taPlot;
  private JTextField                                       textField;

  public TvShowEpisodeChooserDialog(TvShowEpisode ep, MediaScraper mediaScraper) {
    super(BUNDLE.getString("tvshowepisode.choose"), "episodeChooser"); //$NON-NLS-1$
    setBounds(5, 5, 600, 400);

    this.episode = ep;
    this.mediaScraper = mediaScraper;
    this.metadata = new MediaEpisode(mediaScraper.getId());
    episodeEventList = new ObservableElementList<TvShowEpisodeChooserModel>(
        GlazedLists.threadSafeList(new BasicEventList<TvShowEpisodeChooserModel>()), GlazedLists.beanConnector(TvShowEpisodeChooserModel.class));
    SortedList<TvShowEpisodeChooserModel> sortedEpisodes = new SortedList<TvShowEpisodeChooserModel>(
        GlazedListsSwing.swingThreadProxyList(episodeEventList), new EpisodeComparator());

    getContentPane().setLayout(
        new FormLayout(new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("590px:grow"), FormFactory.RELATED_GAP_COLSPEC, },
            new RowSpec[] { FormFactory.RELATED_GAP_ROWSPEC, RowSpec.decode("fill:405px:grow"), FormFactory.RELATED_GAP_ROWSPEC,
                RowSpec.decode("fill:37px"), FormFactory.RELATED_GAP_ROWSPEC, }));
    {
      JSplitPane splitPane = new JSplitPane();
      getContentPane().add(splitPane, "2, 2, fill, fill");

      JPanel panelLeft = new JPanel();
      panelLeft.setLayout(
          new FormLayout(new ColumnSpec[] { FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("150dlu:grow"), FormSpecs.RELATED_GAP_COLSPEC, },
              new RowSpec[] { FormSpecs.LINE_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, RowSpec.decode("200dlu:grow"),
                  FormSpecs.RELATED_GAP_ROWSPEC, }));

      textField = EnhancedTextField.createSearchTextField();
      panelLeft.add(textField, "2, 2, fill, default");
      textField.setColumns(10);

      JScrollPane scrollPane = new JScrollPane();
      scrollPane.setMinimumSize(new Dimension(200, 23));
      panelLeft.add(scrollPane, "2, 4, fill, fill");
      splitPane.setLeftComponent(panelLeft);

      MatcherEditor<TvShowEpisodeChooserModel> textMatcherEditor = new TextComponentMatcherEditor<TvShowEpisodeChooserModel>(textField,
          new TvShowEpisodeChooserModelFilterator());
      FilterList<TvShowEpisodeChooserModel> textFilteredEpisodes = new FilterList<TvShowEpisodeChooserModel>(sortedEpisodes, textMatcherEditor);
      AdvancedTableModel<TvShowEpisodeChooserModel> episodeTableModel = GlazedListsSwing.eventTableModelWithThreadProxyList(textFilteredEpisodes,
          new EpisodeTableFormat());
      DefaultEventSelectionModel<TvShowEpisodeChooserModel> selectionModel = new DefaultEventSelectionModel<TvShowEpisodeChooserModel>(
          textFilteredEpisodes);
      final List<TvShowEpisodeChooserModel> selectedEpisodes = selectionModel.getSelected();

      selectionModel.addListSelectionListener(new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
          if (e.getValueIsAdjusting()) {
            return;
          }
          // display first selected episode
          if (!selectedEpisodes.isEmpty()) {
            TvShowEpisodeChooserModel episode = selectedEpisodes.get(0);
            taPlot.setText(episode.getOverview());
          }
          else {
            taPlot.setText("");
          }
          taPlot.setCaretPosition(0);
        }
      });

      table = new JTable(episodeTableModel);
      table.setSelectionModel(selectionModel);
      scrollPane.setViewportView(table);

      JPanel panelRight = new JPanel();
      panelRight.setLayout(
          new FormLayout(new ColumnSpec[] { FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("150dlu:grow"), FormSpecs.RELATED_GAP_COLSPEC, },
              new RowSpec[] { FormSpecs.LINE_GAP_ROWSPEC, RowSpec.decode("default:grow"), FormSpecs.RELATED_GAP_ROWSPEC, }));
      JScrollPane scrollPane_1 = new JScrollPane();
      panelRight.add(scrollPane_1, "2, 2, fill, fill");
      splitPane.setRightComponent(panelRight);

      taPlot = new JTextArea();
      taPlot.setEditable(false);
      taPlot.setWrapStyleWord(true);
      taPlot.setLineWrap(true);
      scrollPane_1.setViewportView(taPlot);
      splitPane.setDividerLocation(300);

    }
    JPanel bottomPanel = new JPanel();
    getContentPane().add(bottomPanel, "2, 4, fill, top");

    bottomPanel.setLayout(new FormLayout(
        new ColumnSpec[] { FormFactory.LABEL_COMPONENT_GAP_COLSPEC, FormFactory.DEFAULT_COLSPEC, FormFactory.RELATED_GAP_COLSPEC,
            ColumnSpec.decode("default:grow"), FormFactory.DEFAULT_COLSPEC, FormFactory.RELATED_GAP_COLSPEC, },
        new RowSpec[] { FormFactory.LINE_GAP_ROWSPEC, RowSpec.decode("25px"), FormFactory.RELATED_GAP_ROWSPEC, }));

    JPanel buttonPane = new JPanel();
    bottomPanel.add(buttonPane, "5, 2, fill, fill");
    EqualsLayout layout = new EqualsLayout(5);
    layout.setMinWidth(100);
    buttonPane.setLayout(layout);
    final JButton okButton = new JButton(BUNDLE.getString("Button.ok")); //$NON-NLS-1$
    okButton.setToolTipText(BUNDLE.getString("tvshow.change"));
    okButton.setIcon(IconManager.APPLY);
    buttonPane.add(okButton);
    okButton.setActionCommand("OK");
    okButton.addActionListener(this);

    JButton cancelButton = new JButton(BUNDLE.getString("Button.cancel")); //$NON-NLS-1$
    cancelButton.setToolTipText(BUNDLE.getString("edit.discard"));
    cancelButton.setIcon(IconManager.CANCEL);
    buttonPane.add(cancelButton);
    cancelButton.setActionCommand("Cancel");
    cancelButton.addActionListener(this);

    // column widths
    table.getColumnModel().getColumn(0).setMaxWidth(50);
    table.getColumnModel().getColumn(1).setMaxWidth(50);

    SearchTask task = new SearchTask();
    task.execute();

    MouseListener mouseListener = new MouseListener() {

      @Override
      public void mouseReleased(MouseEvent e) {
      }

      @Override
      public void mousePressed(MouseEvent e) {
      }

      @Override
      public void mouseExited(MouseEvent e) {
      }

      @Override
      public void mouseEntered(MouseEvent e) {
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() >= 2) {
          actionPerformed(new ActionEvent(okButton, ActionEvent.ACTION_PERFORMED, "OK"));
        }
      }

    };
    table.addMouseListener(mouseListener);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    // assign episode
    if ("OK".equals(e.getActionCommand())) {
      int row = table.getSelectedRow();
      if (row >= 0) {
        row = table.convertRowIndexToModel(row);
        TvShowEpisodeChooserModel episode = episodeEventList.get(row);
        if (episode != TvShowEpisodeChooserModel.emptyResult) {
          metadata = episode.getMediaEpisode();
        }

        setVisible(false);
      }
    }

    // cancel
    if ("Cancel".equals(e.getActionCommand())) {
      setVisible(false);
    }
  }

  public MediaEpisode getMetadata() {
    return metadata;
  }

  private class SearchTask extends SwingWorker<Void, Void> {
    @Override
    public Void doInBackground() {
      MediaScrapeOptions options = new MediaScrapeOptions(MediaType.TV_EPISODE);
      options.setLanguage(Globals.settings.getTvShowSettings().getScraperLanguage());
      options.setCountry(Globals.settings.getTvShowSettings().getCertificationCountry());
      for (Entry<String, Object> entry : episode.getTvShow().getIds().entrySet()) {
        options.setId(entry.getKey(), entry.getValue().toString());
      }

      try {
        for (MediaEpisode episode : ((ITvShowMetadataProvider) mediaScraper.getMediaProvider()).getEpisodeList(options)) {
          episodeEventList.add(new TvShowEpisodeChooserModel(mediaScraper, episode));
        }
      }
      catch (Exception e) {
      }

      return null;
    }
  }

  private class TvShowEpisodeChooserModelFilterator implements TextFilterator<TvShowEpisodeChooserModel> {
    @Override
    public void getFilterStrings(List<String> baseList, TvShowEpisodeChooserModel model) {
      baseList.add(model.getTitle());
      baseList.add(model.getOverview());
    }
  }

  private class EpisodeComparator implements Comparator<TvShowEpisodeChooserModel> {
    @Override
    public int compare(TvShowEpisodeChooserModel o1, TvShowEpisodeChooserModel o2) {
      if (o1.getSeason() < o2.getSeason()) {
        return -1;
      }

      if (o1.getSeason() > o2.getSeason()) {
        return 1;
      }

      if (o1.getEpisode() < o2.getEpisode()) {
        return -1;
      }

      if (o1.getEpisode() > o2.getEpisode()) {
        return 1;
      }

      return 0;
    }
  }

  private class EpisodeTableFormat implements TableFormat<TvShowEpisodeChooserModel> {
    @Override
    public int getColumnCount() {
      return 3;
    }

    @Override
    public String getColumnName(int column) {
      switch (column) {
        case 0:
          return BUNDLE.getString("metatag.season"); //$NON-NLS-1$

        case 1:
          return BUNDLE.getString("metatag.episode"); //$NON-NLS-1$

        case 2:
          return BUNDLE.getString("metatag.title"); //$NON-NLS-1$
      }
      return null;
    }

    @Override
    public Object getColumnValue(TvShowEpisodeChooserModel baseObject, int column) {
      switch (column) {
        case 0:
          return baseObject.getSeason();

        case 1:
          return baseObject.getEpisode();

        case 2:
          return baseObject.getTitle();
      }
      return null;
    }

  }
}
