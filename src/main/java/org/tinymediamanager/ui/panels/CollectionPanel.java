/**
 * CollectionPanel
 * 
 * Lists entire video-collection for meta-editing.
 * @author Kevin Jackson <kJackson324@Gmail.com>
 *
 * 
 * TREE NODE  ::
 * MODEL      ::
 * 
 * @see org.tinymediamanager.core.movie.entities.Movie
 * @see org.tinymediamanager.core.tvshow.entities.TvShow
 * 
 * @see org.tinymediamanager.core.movie.MovieList
 * @see org.tinymediamanager.core.tvshow.TvShowList
 */
package org.tinymediamanager.ui.panels;
  
  /**
   * TODO: Can list rows of TVShows with meta-data in columns.
   * TODO: Can group TVShows together.
   * TODO: Row Headers (numbering the shows)
   * TODO: Column Headers (at top)
   * TODO: Can move around the column order.
   * TODO: Can list a group of Movies.
   * TODO: Can list Movie Collections.
   * TODO: Can click-and-drag Movies onto MovieSets in-order-to create a new MovieSet.
   * TODO: Can click-and-drag Movies into MovieSets in-order-to assign it into a MovieSet at the dropped index.
   */

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.ListCellEditor;
import org.tinymediamanager.ui.TreeUI;
import org.tinymediamanager.ui.UTF8Control;
import org.tinymediamanager.ui.collections.CellRendererCollections;
import org.tinymediamanager.ui.components.ZebraJTree;
import org.tinymediamanager.ui.movies.MovieSelectionModel;


import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.Cell;
import javafx.scene.control.MultipleSelectionModel; //@see https://stackoverflow.com/questions/31267157/treeview-certain-treeitems-are-not-allowed-to-be-selected
import javafx.scene.control.SelectionModel;         //@see https://stackoverflow.com/questions/31267157/treeview-certain-treeitems-are-not-allowed-to-be-selected
import javafx.util.Callback;


import org.tinymediamanager.ui.moviesets.MovieSetSelectionModel;
import org.tinymediamanager.ui.moviesets.MovieSetTreeModel;
import org.tinymediamanager.ui.tvshows.TvShowEpisodeSelectionModel;
import org.tinymediamanager.ui.tvshows.TvShowSeasonSelectionModel;
import org.tinymediamanager.ui.tvshows.TvShowTreeModel;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jtattoo.plaf.AbstractLookAndFeel;
import com.jtattoo.plaf.JTattooUtilities;

/**
 * A split-panel that shows a tree of movie & tv shows on one side with an editing dialog on the other.
 * Enables the user to edit one or more titles, anywhere in their collection, without the need to keep switching dialogs.
 */
public class CollectionPanel extends JPanel {
  
  private static final long serialVersionUID = -7321985612407375362L;
  private static final java.util.ResourceBundle BUNDLE = java.util.ResourceBundle.getBundle("messages", new UTF8Control()); //$NON-NLS-1$

    
    /*
     * EXAMPLE :: Collection List (excludes status-icon indicators)
     * ============================================================
     * 
     *   NODE TYPE                   Filename                 | Title          | Show  : Season
     *                                                                         | Movie : N/A
     *                                                                               | Show  : Episode
     *                                                                               | Movie : Series Order (1 if !in a set)
     *                                                                                      | SubTitle     | Tags                  | Path
     * 
     * NodeCollectionGroup - Node + Movies
     * 
     * NodeCollectionGroup - Grup | + Unidentified
     * 
     * NodeCollectionItem  - Movi | | - ...
     * 
     * NodeCollectionGroup - Grup | + Indivisual Releases
     * 
     * NodeCollectionItem  - Movi | | - Aladdin.mkv              | Ghostbusters   |     |   1 |               | Cartoon,Fantasy       | /path/loc/...
     * NodeCollectionItem  - Movi | | - Ghostbusters.mkv         | Ghostbusters   |     |   1 |               | Sci-Fi,Paranormal,... | /path/loc/...
     * NodeCollectionItem  - Movi | | - ...
     * 
     * NodeCollectionGroup - Grup | + Movie Sets
     * 
     * NodeCollectionGroup - Set  | | + Alien
     * 
     * NodeCollectionItem  - Movi | | | - alien.mkv              | Alien          |     |   1 |               | Sci-Fi,Horror         | /path/loc/... | ...
     * NodeCollectionItem  - Movi | | | - Aliens.mkv             | Aliens         |     |   2 |               | Sci-Fi,Horror         | /path/loc/... | ...
     * NodeCollectionItem  - Movi | | | - AliensII.mkv           | Aliens 2       |     |   3 | Aliens Return | Sci-Fi,Horror         | /path/loc/... | ...
     * NodeCollectionItem  - Node | | - ...
     * 
     * NodeCollectionGroup - Node + TV Shows
     * 
     * NodeCollectionGroup - Grup | + Unidentified
     * NodeCollectionGroup - Grup | + The Brak Show
     * 
     * NodeCollectionGroup - Sson | | + Season 1
     * 
     * NodeCollectionItem  - Show | | | - The_Brack_Show_1x01.mkv | The Brak Show | s01 | e01 | EpisodeTitle  |                       | /path/loc/... | ...
     * NodeCollectionItem  - Show | | | - The_Brack_Show_1x02.mkv | The Brak Show | s01 | e01 | EpisodeTitle  |                       | /path/loc/... | ...
     * NodeCollectionItem  - Show | | | - ...
     * 
     * NodeCollectionItem  - Grup | + ...
     * 
     */
    
    /* MAIN COLUMN LISTINGS
     * ======================
     * Scraped Info :: Boolean ::
     * Scraped Imgs :: Boolean ::
     * Filename     :: String  :: 
     * Filepath     :: String  :: 
     * List Name    :: String  :: Official name of media gathered from scraper (blank if not identified).
     * Season       :: int     :: TV Show only, otherwise blank.
     * Episode      :: int     :: 
     * Genre        :: String  ::
     * Tags         :: String  :: List of comma-delimited tag-names.
     */

    /* SIDE PANEL FEATURES
     * ====================
     * Detail          :: Shows more/less the standard detail-panel for the FIRST selected item.
     * Rename/Orginize :: Rename and/or orginize files according to a provided pattern.
     * Edit            :: Manually edit fields for selected files (fields shown only if one item is selected).
     */

    /* L-CLICK OPERATIONS
     * ============================
     * Click & Hold :: Can temporarly reorder the list (for batch operations)
     * Dbl-Click    :: Open the Node's respected "Details" panel.
     */

    /* R-CLICK MENU OPERATIONS
     * ============================
     * Copy/Paste ::
     * Autonumber :: 
     * Rename     :: Single-File only
     * Tag Add
     * Tag Remove
     *
     *
     *
     *
     *
     */
         
    
    //obj.getGroup(0).removeFile();
    
    //obj.getGroup(0).removeGroup();
    
    
    // Other functions
    // ================
    // obj.groupBy( column ) // Sort groups within collections.
    // obj.sortBy( column )  // Sort list within all groups.
  
  // SELECTION MODELS
  public       MovieSetSelectionModel selModelMovieSet;    // MovieSet
  public          MovieSelectionModel selModelMovie;       // Movie
  //public       TvShowSelectionModel selModelShow;        // Show   --X--
  public   TvShowSeasonSelectionModel selModelShowSeason;  // Show's Season
  public  TvShowEpisodeSelectionModel selModelShowEpisode; // Show's Episode
  
  // TREE MODELS
  private MovieSetTreeModel treeModelMovieSet;
  private   TvShowTreeModel treeModelShow;
  //TODO: No MovieTreeModel Why? -- Because A Movie is a UserObject with no heiarchey of their own?   --> Already represented in the MoviesetTreeModel

  // USEROBJECT LISTS
  private MovieList         listMovies = MovieList.getInstance();
  private TvShowList        listShows  = TvShowList.getInstance();
  //TODO: No MovieSetList   Why? --> Already represented in the MovieList with "MovieList.getMovieSetList
  
  /*//TODO ADD THESE ITEMS
  private final Action      actionAdd    = new ActionCollectionAdd(false); //actionMovieSetAdd
  private final Action      actionRemove = new ActionCollectionRemove(false); //actionMovieSetRemove
  private final Action      actionSearch = new ActionCollectionSearch(false); //actionMovieSetSearch
  private final Action      actionEdit   = new ActionCollectionEdit(false); //actionMovieSetEdit
  */

  private JTree             tree;
  private JPanel            panelRight;
  private JMenu             menu;
  
  private int               width = 0;


  /**
   * Constructor
   */
  public CollectionPanel(){
      super();
      
      /*
      
      TreeItem<String> root = new TreeItem<String>("Root Node");
      root.setExpanded(true);
      root.getChildren().addAll(
          new TreeItem<String>("Item 1"),
          new TreeItem<String>("Item 2"),
          new TreeItem<String>("Item 3")
      );
      TreeView<String> treeView = new TreeView<String>( root );      
      treeView.setCellFactory( new Callback<TreeView<String>, TreeCell<String>>(){ @Override public TreeCell<String> call( TreeView<String> list ){ return new MoneyFormatCell(); }});
      
      */
      
      this.setLayout( new FormLayout(
          new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),   FormFactory.RELATED_GAP_COLSPEC, },
          new RowSpec[]    { FormFactory.RELATED_GAP_ROWSPEC, RowSpec.decode("fill:default:grow"), FormFactory.DEFAULT_ROWSPEC,     }
      ));
  
      //Tree Panel for the Split Window.
      JPanel panelTree = new JPanel();
      tree_panel: {
          panelTree.setLayout( new FormLayout(
              new ColumnSpec[] { FormFactory.LABEL_COMPONENT_GAP_COLSPEC, ColumnSpec.decode("250px:grow"), },
              new RowSpec[]    { FormFactory.LINE_GAP_ROWSPEC,            FormFactory.DEFAULT_ROWSPEC,     FormFactory.RELATED_GAP_ROWSPEC, RowSpec.decode("fill:322px:grow"), }
          ));
          
          //TREE-TOOLBAR
          JToolBar toolBar;
          JButton  btnAdd;
          JButton  btnRemove;
          JButton  btnSearch;
          JButton  btnEdit;
          {   /*/ TREE TOOLBAR
              toolBar = new JToolBar();
              toolBar.setRollover  (true );
              toolBar.setFloatable (false);
              toolBar.setOpaque    (false);
              panelTree.add(toolBar, "2, 2");
        
              JButton btnAdd    = new JButton(""); btnAdd.    setAction( actionMovieSetAdd    ); toolBar.add(btnAdd);
              JButton btnRemove = new JButton(""); btnRemove. setAction( actionMovieSetRemove ); toolBar.add(btnRemove);
              JButton btnSearch = new JButton(""); btnSearch. setAction( actionMovieSetSearch ); toolBar.add(btnSearch);
              JButton btnEdit   = new JButton(""); btnEdit.   setAction( actionMovieSetEdit   ); toolBar.add(btnEdit);
              //*/
          }
      
      
          treeModelShow       = new   TvShowTreeModel( listShows.  getTvShows()      );
          treeModelMovieSet   = new MovieSetTreeModel( listMovies. getMovieSetList() );
          //TODO: No MovieTreeModel Why?

          TreeUI ui;
          //DefaultTreeModel dtm = new DefaultTreeModel( new DefaultMutableTreeNode() );
          tree: {
              tree = new ZebraJTree( /*treeModelMovieSet*/ ){ //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                private static final long serialVersionUID = 8881757869311476200L;
          
                @Override
                public void paintComponent(Graphics g) {
                  width = this.getWidth();
                  super.paintComponent(g);
                }
              }; //end :: new ZebraJTree( treeModelMovieSet ){...
              
              selModelMovieSet    = new      MovieSetSelectionModel(tree); //Why tree and no others dont use it?
              selModelMovie       = new         MovieSelectionModel();
              selModelShowSeason  = new  TvShowSeasonSelectionModel();
              selModelShowEpisode = new TvShowEpisodeSelectionModel();
          
              //*/ CREATE THE TREE
                ui = new TreeUI() {
                    @Override
                    protected void paintRow( Graphics g,   Rectangle clipBounds, Insets  insets,          Rectangle bounds, TreePath path,
                                             int      row, boolean   isExpanded, boolean hasBeenExpanded, boolean   isLeaf
                                             ){
                      bounds.width = width - bounds.x;
                      super.paintRow(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
                  }
              };
              
              tree.setEditable(true);
              tree.setUI(ui);
              tree.setRootVisible(false);
              tree.setShowsRootHandles(true);
              tree.setCellRenderer(new CellRendererCollections());
              tree.setRowHeight(0);
                          
              JPanel panelHeader;
              header: {
            
                  panelHeader = new JPanel() {
                      private static final long serialVersionUID = -6646766582759138262L;        
                      @Override
                      public void paintComponent( Graphics g ){
                          super.paintComponent( g );
                          JTattooUtilities.fillHorGradient(g, AbstractLookAndFeel.getTheme().getColHeaderColors(), 0, 0, getWidth(), getHeight());
                          return;
                      }
                  };
                  
                  panelHeader.setLayout( new FormLayout(
                      new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("center:20px"), ColumnSpec.decode("center:20px"), },
                      new RowSpec[]    { FormFactory.DEFAULT_ROWSPEC, }
                  ));
              }
    
              JScrollPane scrollPane = new JScrollPane();
              panelTree.add( scrollPane, "2, 4, fill, fill" );
              scrollPane.setColumnHeaderView(panelHeader);
              scrollPane.setViewportView( tree );
          
              JLabel lblMovieSetColumn = new JLabel(BUNDLE.getString("tmm.movieset")); lblMovieSetColumn.setHorizontalAlignment(JLabel.CENTER); panelHeader.add(lblMovieSetColumn, "2, 1");
              JLabel lblNfoColumn      = new JLabel( ""                             ); lblNfoColumn.     setHorizontalAlignment(JLabel.CENTER); lblNfoColumn.  setIcon(IconManager.INFO ); lblNfoColumn.  setToolTipText(BUNDLE.getString("tmm.nfo")    ); panelHeader.add(lblNfoColumn,  "4, 1");  
              JLabel lblImageColumn    = new JLabel( ""                             ); lblImageColumn.   setHorizontalAlignment(JLabel.CENTER); lblImageColumn.setIcon(IconManager.IMAGE); lblImageColumn.setToolTipText(BUNDLE.getString("tmm.images") ); panelHeader.add(lblImageColumn,"5, 1");
    
          }
          //end :: TREE HEADER
          
          final JPanel panelRight = new JPanel();
          panelRight.setLayout(new CardLayout(0, 0));
          
      
          /*/ FORGET THE DETAILED INFO FOR THE SELECTED ITEM
          JPanel panelSet   = new MovieSetInformationPanel( selModelMovieSet ); panelRight.add(panelSet,   "movieSet" );
          JPanel panelMovie = new    MovieInformationPanel( selModelMovie    ); panelRight.add(panelMovie, "movie"    );
          //*/
          
          JPanel panelMovieSetCount = new JPanel();
          this.add(panelMovieSetCount, "2, 3, left, fill");
      
          JLabel lblMovieSets = new JLabel( BUNDLE.getString( "tmm.moviesets" )); panelMovieSetCount.add(lblMovieSets);
          JLabel lblMovieSetCount = new JLabel("0");
          
          panelMovieSetCount.add(lblMovieSetCount);
      
    
          
          listeners: { /* //Also enable the action & selection declarations
              
              selections: {
                  tree.addTreeSelectionListener( new TreeSelectionListener(){
                      @Override
                      public void valueChanged( TreeSelectionEvent e ){
                          
                          DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                          
                          if ( node == null) {
                              selModelMovieSet.setSelectedMovieSet(null);
                              return;
                          }
                          
                          Object obj = node.getUserObject();
                          
                          if ( obj instanceof MovieSet ){
                              MovieSet movieSet = (MovieSet) obj;
                              selModelMovieSet.setSelectedMovieSet(movieSet);
                              CardLayout cl = (CardLayout) (panelRight.getLayout());
                              cl.show(panelRight, "movieSet"); //$NON-NLS-1$
                          }
                          
                          if ( obj instanceof Movie ){            
                              Movie movie = (Movie) obj;
                              selModelMovie.setSelectedMovie(movie);
                              CardLayout cl = (CardLayout) (panelRight.getLayout());
                              cl.show(panelRight, "movie"); //$NON-NLS-1$
                          }       
                          
                          return;
                      }
                  });
              }
          
              MouseListener mouseListener;
              listener_actions: {
                  
                  mouseListener = new MouseAdapter(){ // add double click listener
                    @Override
                    public void mousePressed( MouseEvent e ){
                      if ( 2 == e.getClickCount() ){
                        actionEdit.actionPerformed( new ActionEvent( e, 0, "" ));
                      }
                    }
                  };
              }
              tree.addMouseListener(mouseListener);
              // */
          }
          
          BeanProperty< MovieList, Integer > bpMovieList;
          BeanProperty< JLabel,    String  > bpJLabel;
          data_bindings: {
          
              bpMovieList = BeanProperty.create( "movieSetCount" );
              bpJLabel    = BeanProperty.create( "text"          );
              
              AutoBinding< MovieList, Integer, JLabel, String > autoBinding = Bindings.createAutoBinding(
                  UpdateStrategy.READ, listMovies, bpMovieList, lblMovieSetCount, bpJLabel
              );  autoBinding.bind();
          }
      
          // selecting first movie set at startup
          if ( null != listMovies.getMovieSetList() && 0 < listMovies.getMovieSetList().size() ){
            
            DefaultMutableTreeNode firstLeaf = (DefaultMutableTreeNode) ( (DefaultMutableTreeNode) tree.getModel().getRoot() ).getFirstChild();
            tree.setSelectionPath( new TreePath( (( DefaultMutableTreeNode ) firstLeaf.getParent() ).getPath() ));
            tree.setSelectionPath( new TreePath( firstLeaf.getPath() ));
          }
          
      }//end :: tree_panel namespace
      
      JSplitPane splitPane = new JSplitPane();
      splitPane.setContinuousLayout( true );
      splitPane.setLeftComponent( panelTree ); //LEFT WINDOW-PANE
      splitPane.setRightComponent( panelRight );
      
      this.add( splitPane, "2, 2, fill, fill" );
      return;
    } //end :: Constructor



  /**
   *
   */
  public static JPanel getExampleTree(){
    
    /*
     * Things every JTable needs to know.
     * - Calculate - The max width of every column in Each table.
     *                     - Table : int maxColumnWidths[]
     * - 
     */
/*    
    //CollectionPanel    obj = new ;
    TreeNodeCollection grp = obj.addGroup();
    

    TreeNodeCollection grpMovieSets = obj.addGroup(); //returns a ref to the new group.
    for ( listMovieSets :: MovieSet movieSet ){

	TreeNodeCollectionItem grpMovieSet = obj.addGroup();

	listMovies = movieSet.getMovies(); 
	for ( listMovies : Movie movie ){ grpMovieSet.addItem( new MovieNode( movie )); }
    }



    MovieList         movieList = MovieList.getInstance(); // ./core/MovieList
    List < MovieSet > movieSets = new MovieSetTreeModel( movieList.getMovieSetList() );
    List < Movie    > movies    = new MovieSetTreeModel( movieList.getMovieSetList() );

    MovieList         tvShowList = TvShowList.getInstance(); // ./core/TvShowList
    List < MovieSet > movieSets = new MovieSetTreeModel( movieList.getMovieSetList() );

    Map  < Object, TreeNode > nodeMap   = Collections.synchronizedMap( new HashMap< Object, TreeNode >() );

    // build initial tree
    for ( MovieSet movieSet : movieSets ){
      DefaultMutableTreeNode setNode = new MovieSetTreeNode(movieSet);

      nodeMap.put(movieSet, setNode);

      for ( Movie movie : movieSet.getMovies() ){
        DefaultMutableTreeNode movieNode = new MovieTreeNode(movie);
        setNode.add( movieNode        );
        nodeMap.put( movie, movieNode );
      }
      root.add(setNode);

      // implement change listener
      movieSet.addPropertyChangeListener(propertyChangeListener);
    }



    TreeNodeCollection c = obj.addGroup(); //returns a ref to the new group.
    for ( listMovies :: Movie movie ){
	if ( movie.set.getLength() )
	    obj.addItem( new MovieNode( movie )); //returns a ref to the new group.
    }

    TreeNodeCollection c = obj.addGroup(); //returns a ref to the new group.
    for ( listShows :: TvShow show ){

	TreeNodeCollectionItem g = obj.addGroup(); //returns a ref to the new group.

	listSeasons = show.getSeasons();

	for ( listSeasons : TvShowSeason season ){

	    TreeNodeCollection g = obj.addGroup(); //returns a ref to the new group.

	    for ( listSeasons : TvShowEpisode episode ){

		TreeNodeCollection g = obj.addGroup(); //returns a ref to the new group.
	    }	    
	}
    }

    obj = new TableGrouped(); //Reminder: a JPanel obj.
    obj.setHeader( new String[]{ "Filename", "Title", "S#", "E#", "Subtitle", "Aired", "Source", "Path" });
    
    g.addFile();                         //returns a ref to the same group.
    g.addFile();        
    g.addFile();        
    
    TableGroupedRows g2 = obj.addGroup(); 
    g2.addFile();      
    g2.addFile();        
    
    TableGroupedRows g3 = obj.addGroup(); 
    g3.addFile();      
    g3.addFile();       
    g3.addFile(); 


*/
    JPanel panel = new JPanel();
    return panel;
  }

  /**
   * MySlider
   */
  static class MySlider extends AbstractCellEditor implements ListCellRenderer, ListCellEditor, ChangeListener{
        private JSlider slider = new JSlider(0, 5);

        public MySlider(){
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);
            slider.setMajorTickSpacing(1);
            return;
        }

        public void stateChanged(ChangeEvent e){
            fireEditingStopped();
        }

        public boolean isCellEditable( EventObject anEvent ) {
          if ( anEvent instanceof MouseEvent ){
                return ( (MouseEvent) anEvent ).getClickCount() >= 1;
          }
          return true;
        }

        protected void fireEditingStopped(){  super.fireEditingStopped(); slider.removeChangeListener(this);
          return;
        }
        
        protected void fireEditingCanceled(){ super.fireEditingCanceled(); slider.removeChangeListener(this);
          return;
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus){
            slider.setValue(( (Integer) value ).intValue() );
            return slider;
        }
        
        public Object getCellEditorValue(){
            return new Integer( slider.getValue() );
        }

        public Component getListCellEditorComponent(JList list, Object value, boolean isSelected, int index){
            slider.setValue(( (Integer) value ).intValue() );
            slider.addChangeListener(this);
            return slider;
        }
  } //end :: static class MySlider...
}
