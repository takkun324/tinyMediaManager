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
package org.tinymediamanager.ui.wizard;

import java.awt.Cursor;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.swingbinding.JListBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.connector.MovieConnectors;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.UTF8Control;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

/**
 * The class MovieSourcePanel is used to maintain the movie data sources in the wizard
 * 
 * @author Manuel Laggner
 */
class MovieSourcePanel extends JPanel {
  private static final long           serialVersionUID = -8346420911623937902L;
  /**
   * @wbp.nls.resourceBundle messages
   */
  private static final ResourceBundle BUNDLE           = ResourceBundle.getBundle("messages", new UTF8Control()); //$NON-NLS-1$

  private final MovieSettings         settings         = MovieModuleManager.MOVIE_SETTINGS;

  private JList<String>               listDataSources;
  private JCheckBox                   chckbxMultipleMoviesPerFolder;
  private JComboBox<MovieConnectors>  cbNfoFormat;

  public MovieSourcePanel() {
    initComponents();
    initDataBindings();
  }

  /*
   * init components
   */
  private void initComponents() {
    setLayout(new FormLayout(new ColumnSpec[] { FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), FormSpecs.RELATED_GAP_COLSPEC, },
        new RowSpec[] { FormSpecs.LINE_GAP_ROWSPEC, RowSpec.decode("default:grow"), FormSpecs.LINE_GAP_ROWSPEC, }));
    JPanel panelMovieDataSources = new JPanel();

    add(panelMovieDataSources, "2, 2, fill, fill");
    panelMovieDataSources.setLayout(new FormLayout(
        new ColumnSpec[] { FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC, FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
            FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("50dlu:grow"), FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
            FormSpecs.RELATED_GAP_COLSPEC, },
        new RowSpec[] { FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.LINE_GAP_ROWSPEC,
            RowSpec.decode("70dlu:grow"), FormSpecs.UNRELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
            RowSpec.decode("default:grow"), FormSpecs.RELATED_GAP_ROWSPEC, }));

    JLabel lblMovieDataSources = new JLabel(BUNDLE.getString("wizard.movie.datasources")); //$NON-NLS-1$
    panelMovieDataSources.add(lblMovieDataSources, "2, 2, 7, 1");

    JTextPane tpDatasourceHint = new JTextPane();
    tpDatasourceHint.setText(BUNDLE.getString("wizard.datasource.hint")); //$NON-NLS-1$
    tpDatasourceHint.setOpaque(false);
    panelMovieDataSources.add(tpDatasourceHint, "2, 3, 7, 1, fill, fill");

    JScrollPane scrollPaneDataSources = new JScrollPane();
    panelMovieDataSources.add(scrollPaneDataSources, "2, 5, 5, 1, fill, fill");

    listDataSources = new JList<>();
    scrollPaneDataSources.setViewportView(listDataSources);

    JPanel panelMovieSourcesButtons = new JPanel();
    panelMovieDataSources.add(panelMovieSourcesButtons, "8, 5, fill, top");
    panelMovieSourcesButtons.setLayout(new FormLayout(new ColumnSpec[] { FormSpecs.DEFAULT_COLSPEC, },
        new RowSpec[] { FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, }));

    JButton btnAdd = new JButton(IconManager.LIST_ADD);
    btnAdd.setToolTipText(BUNDLE.getString("Button.add")); //$NON-NLS-1$
    btnAdd.setMargin(new Insets(2, 2, 2, 2));
    btnAdd.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        File file = TmmUIHelper.selectDirectory(BUNDLE.getString("Settings.datasource.folderchooser")); //$NON-NLS-1$
        if (file != null && file.exists() && file.isDirectory()) {
          MovieModuleManager.MOVIE_SETTINGS.addMovieDataSources(file.getAbsolutePath());
        }
      }
    });

    panelMovieSourcesButtons.add(btnAdd, "1, 1, fill, top");

    JButton btnRemove = new JButton(IconManager.LIST_REMOVE);
    btnRemove.setToolTipText(BUNDLE.getString("Button.remove")); //$NON-NLS-1$
    btnRemove.setMargin(new Insets(2, 2, 2, 2));
    btnRemove.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        int row = listDataSources.getSelectedIndex();
        if (row != -1) { // nothing selected
          String path = MovieModuleManager.MOVIE_SETTINGS.getMovieDataSource().get(row);
          String[] choices = { BUNDLE.getString("Button.continue"), BUNDLE.getString("Button.abort") }; //$NON-NLS-1$
          int decision = JOptionPane.showOptionDialog(null, String.format(BUNDLE.getString("Settings.movie.datasource.remove.info"), path),
              BUNDLE.getString("Settings.datasource.remove"), JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, choices,
              BUNDLE.getString("Button.abort")); //$NON-NLS-1$
          if (decision == 0) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            MovieModuleManager.MOVIE_SETTINGS.removeMovieDataSources(path);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          }
        }
      }
    });
    panelMovieSourcesButtons.add(btnRemove, "1, 3, fill, top");

    JLabel lblAllowMultipleMoviesPerFolder = new JLabel(BUNDLE.getString("Settings.multipleMovies")); //$NON-NLS-1$
    panelMovieDataSources.add(lblAllowMultipleMoviesPerFolder, "2, 7, right, default");

    chckbxMultipleMoviesPerFolder = new JCheckBox("");
    panelMovieDataSources.add(chckbxMultipleMoviesPerFolder, "4, 7");

    JTextPane tpMultipleMoviesHint = new JTextPane();
    TmmFontHelper.changeFont(tpMultipleMoviesHint, 0.833);
    tpMultipleMoviesHint.setBackground(UIManager.getColor("Panel.background"));
    tpMultipleMoviesHint.setText(BUNDLE.getString("Settings.multipleMovies.hint")); //$NON-NLS-1$
    tpMultipleMoviesHint.setEditable(false);
    panelMovieDataSources.add(tpMultipleMoviesHint, "6, 7, 3, 1, fill, fill");

    JPanel panel = new JPanel();
    panelMovieDataSources.add(panel, "2, 9, 3, 1, fill, fill");
    panel.setLayout(
        new FormLayout(new ColumnSpec[] { ColumnSpec.decode("right:default"), FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), },
            new RowSpec[] { RowSpec.decode("26px"), }));

    JLabel lblNfoFormat = new JLabel(BUNDLE.getString("Settings.nfoFormat"));
    panel.add(lblNfoFormat, "1, 1, right, default");

    cbNfoFormat = new JComboBox<>();
    panel.add(cbNfoFormat, "3, 1, fill, default");
    cbNfoFormat.setModel(new DefaultComboBoxModel<>(MovieConnectors.values()));

    JTextPane tpNfoHint = new JTextPane();
    tpNfoHint.setText(BUNDLE.getString("wizard.nfo.hint")); //$NON-NLS-1$
    tpNfoHint.setOpaque(false);
    panelMovieDataSources.add(tpNfoHint, "6, 9, 3, 1, fill, fill");
  }

  /*
   * init data bindings
   */
  @SuppressWarnings("rawtypes")
  protected void initDataBindings() {
    BeanProperty<MovieSettings, MovieConnectors> settingsBeanProperty_10 = BeanProperty.create("movieConnector");
    BeanProperty<JComboBox<MovieConnectors>, Object> jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding<MovieSettings, MovieConnectors, JComboBox<MovieConnectors>, Object> autoBinding_9 = Bindings
        .createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_10, cbNfoFormat, jComboBoxBeanProperty);
    autoBinding_9.bind();
    //
    BeanProperty<MovieSettings, Boolean> settingsBeanProperty_2 = BeanProperty.create("detectMovieMultiDir");
    BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding<MovieSettings, Boolean, JCheckBox, Boolean> autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_2, chckbxMultipleMoviesPerFolder, jCheckBoxBeanProperty);
    autoBinding_2.bind();
    //
    BeanProperty<MovieSettings, List<String>> settingsBeanProperty_4 = BeanProperty.create("movieDataSource");
    JListBinding<String, MovieSettings, JList> jListBinding_1 = SwingBindings.createJListBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_4, listDataSources);
    jListBinding_1.bind();
  }
}
