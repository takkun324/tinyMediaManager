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
package org.tinymediamanager.ui.collections;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.TextField;
import java.util.ResourceBundle;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.UTF8Control;
import org.tinymediamanager.ui.moviesets.MovieSetTreeNode;
import org.tinymediamanager.ui.moviesets.MovieTreeNode;
import org.tinymediamanager.ui.tvshows.TvShowEpisodeTreeNode;
import org.tinymediamanager.ui.tvshows.TvShowSeasonTreeNode;
import org.tinymediamanager.ui.tvshows.TvShowTreeNode;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * class CellRendererCollections
 * 
 * @author Manuel Laggner
 * @author Kevin Jackson
 */
public class CellRendererCollections implements TreeCellRenderer {

  private static final ResourceBundle BUNDLE             = ResourceBundle.getBundle("messages", new UTF8Control()); //$NON-NLS-1$
  private static final Color          ROW_COLOR_EVEN     = new Color(241, 245, 250);
  private static final Color          ROW_COLOR_ODD      = Color.WHITE;
  private DefaultTreeCellRenderer     defaultRenderer    = new DefaultTreeCellRenderer();

  private JPanel    panelGrouptype    = new JPanel();    //movieSetPanel
  private JPanel    panelGroup        = new JPanel();    //movieSetPanel
  private JPanel    panelSubgroup     = new JPanel();    //movieSetPanel
  private JPanel    panelItem         = new JPanel();    //moviePanel
  private JPanel    panelItemUnsorted = new JPanel();    //moviePanel

  private JLabel lblLabelNum   = new JLabel();
  private JLabel lblLabel      = new JLabel();
  private JLabel lblLabelSmall = new JLabel();

  private JLabel lblHasNfo     = new JLabel();    //movieNfoLabel
  private JLabel lblHasImg     = new JLabel();    //movieImageLabel
  private JLabel lblHasSub     = new JLabel();
  private JLabel lblFilename   = new JLabel();
  private JLabel lblPathname   = new JLabel();


  private JLabel    lblGrouptype = new JLabel();
  private JLabel    lblGroup     = new JLabel();
  private JLabel    lblSubgroup  = new JLabel();

  private TextField txtTitle     = new TextField(); //movieSetTitle
  private TextField txtRating    = new TextField(); //movieTitle
  private TextField txtVotes     = new TextField(); //movieSetInfo

  /*    _
   _|-TvShow
  _||-Episode
  |||-Movie
  ||| 
  --- title
  |-| showtitle
  -|| originaltitle
  -|| set - MovieSet Title
  -|- sorttitle - MovieSet Title with a count/order for this video
  --- rating
  -|- year
  -|| top250
  --- votes
  |-| season
  |-| episode
  |-| uniqueid
  -|| outline
  --- plot
  -|| tagline
  -|| runtime
  --| thumb  - url
  -|| fanart - url
  --- mpaa - (age rating)
  ||- episodeguide (url:theTvDb)
  ||- id (?)
  --- premiered (date)
  --| watched - boolean
  --| playcount - int
  |-| credits
  |-| director (multiple)
  |-| aired (year)
  ||- status (ex: "Ended")
  -|- genre (multiple tags inside)
  --- studio
  --- Actor (multiple)
  --|   - name
  --|   - role
  --|   - thumb
  -|| certification (US Ex: TV-MA)
  -|| path
  -|| filename
  -|| List-IDs
  -||   - tmdb
  -||   - imdb
  -|| tmdb_id
  -|| trailer - Youtube URL?
  -|| country
  -|| fileinfo
  -||   + Video
  -||     - codec
  -||     - aspect
  -||     - width
  -||     - height
  -||     - durationinseconds
  -||   + Audio
  -||     - codec
  -||     - language
  -||     - channels
  -|| Producer (multiple)
  -||   - name
  -||   - role
  -|| languages
  -|| source (ex: BlueRay)
   */

  /**
   * Constructor
   */
  public CellRendererCollections(){

    panelGrouptype.setLayout( new FormLayout( //Type: Movie || TvShow
        /* 1 Cols */ new ColumnSpec[] { 
            ColumnSpec.decode("min:grow"), 
            ColumnSpec.decode("min:grow"),
            ColumnSpec.decode("min:grow"),
            ColumnSpec.decode("min:grow"),
            ColumnSpec.decode("min:grow"),
            ColumnSpec.decode("min:grow"),
            ColumnSpec.decode("min:grow"),
            ColumnSpec.decode("min:grow"),
            ColumnSpec.decode("min:grow"),
            ColumnSpec.decode("min:grow"),
        },
        /* 2 Rows */ new    RowSpec[] { 
            FormFactory.DEFAULT_ROWSPEC,
            FormFactory.DEFAULT_ROWSPEC,
        } //2 Rows
    ));    
    panelGroup.setLayout( new FormLayout( //TvShow & MovieSet
        /* 1 Cols */ new ColumnSpec[] { 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            ColumnSpec.decode("min:grow"), 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            ColumnSpec.decode("center:20px"), 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
        },
        /* 2 Rows */ new    RowSpec[] { 
            FormFactory.DEFAULT_ROWSPEC,
            FormFactory.DEFAULT_ROWSPEC,
        } //2 Rows
    ));
    panelSubgroup.setLayout( new FormLayout( //TvShowSeason
        new ColumnSpec[] { //9 Cols 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            ColumnSpec.decode("min:grow"), 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            ColumnSpec.decode("center:20px"), 
            ColumnSpec.decode("min:grow"),
        },
        new RowSpec[] { //2 Rows
            FormFactory.DEFAULT_ROWSPEC,
            FormFactory.DEFAULT_ROWSPEC,
        } //2 Rows
    ));
    panelItem.setLayout( new FormLayout( //TvShowEpisode & Movie
        //   BLANK  NUMBER  NFO  IMG  SUB  TITLE  FILENAME  PATH 
        new ColumnSpec[] { //9 Cols 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            ColumnSpec.decode("min:grow"), 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
            ColumnSpec.decode("center:20px"), 
            ColumnSpec.decode("min:grow"),
        },
        new RowSpec[] { //2 Rows
            FormFactory.DEFAULT_ROWSPEC,
            FormFactory.DEFAULT_ROWSPEC,
        } //2 Rows
    ));
    panelItemUnsorted.setLayout( new FormLayout( //TvShowEpisode & Movie
        //   BLANK  NUMBER  NFO  IMG  SUB  TITLE  FILENAME  PATH 
        new ColumnSpec[] { //9 Cols 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            ColumnSpec.decode("min:grow"), 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC, 
            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
            ColumnSpec.decode("center:20px"), 
            ColumnSpec.decode("min:grow"),
        },
        new RowSpec[] { //2 Rows
            FormFactory.DEFAULT_ROWSPEC,
            FormFactory.DEFAULT_ROWSPEC,
        } //2 Rows
    ));
    panel_grouptype: { //TvShows || Movies

      //note: panel.add( row, col, span-rows = 1, span-cols = 1 );
      lblLabel.      setHorizontalAlignment( JLabel.LEFT); lblLabel.      setMinimumSize( new Dimension(0,0) ); panelGrouptype.add( lblLabel,      "1, 1, 9, 1"  ); TmmFontHelper.changeFont( lblLabel,      Font.BOLD );  
      lblLabelSmall. setHorizontalAlignment( JLabel.LEFT); lblLabelSmall. setMinimumSize( new Dimension(0,0) ); panelGrouptype.add( lblLabelSmall, "2, 1, 9, 1"  ); TmmFontHelper.changeFont( lblLabelSmall, 0.816     ); 
    }    

    panel_group: { //TvShow & MovieSet

      //lblHasImg.     setHorizontalAlignment( JLabel.LEFT); lblHasImg.     setMinimumSize( new Dimension(0,0) ); panelGrouptype.add( lblHasImg,     "1, 1"  );
      //lblHasNfo.     setHorizontalAlignment( JLabel.LEFT); lblHasNfo.     setMinimumSize( new Dimension(0,0) ); panelGrouptype.add( lblHasNfo,     "1, 1"  );
      lblLabel.      setHorizontalAlignment( JLabel.LEFT); lblLabel.      setMinimumSize( new Dimension(0,0) ); panelGroup.add( lblLabel,      "1, 2, 6, 1"  ); TmmFontHelper.changeFont( lblLabel,      Font.BOLD ); 
      lblLabelSmall. setHorizontalAlignment( JLabel.LEFT); lblLabelSmall. setMinimumSize( new Dimension(0,0) ); panelGroup.add( lblLabelSmall, "2, 2, 6, 1"  ); TmmFontHelper.changeFont( lblLabelSmall, 0.816     ); 

    }
    panel_subgroup: { //TvShowSeason

      //lblHasImg.     setHorizontalAlignment( JLabel.LEFT ); lblHasImg.     setMinimumSize( new Dimension(0,0) ); panelGrouptype.add( lblHasImg,     "1, 1"  );
      //lblHasNfo.     setHorizontalAlignment( JLabel.LEFT ); lblHasNfo.     setMinimumSize( new Dimension(0,0) ); panelGrouptype.add( lblHasNfo,     "1, 1"  );
      lblLabel.      setHorizontalAlignment( JLabel.LEFT ); lblLabel.      setMinimumSize( new Dimension(0,0) ); panelSubgroup.add( lblLabel,      "1, 2, 7, 1"  ); TmmFontHelper.changeFont( lblLabel,      Font.BOLD ); 
      lblLabelSmall. setHorizontalAlignment( JLabel.LEFT ); lblLabelSmall. setMinimumSize( new Dimension(0,0) ); panelSubgroup.add( lblLabelSmall, "2, 2, 7, 1"  ); TmmFontHelper.changeFont( lblLabelSmall, 0.816     ); 

    }
    panel_item: { //TvShowEpisodes & Movies

      lblLabelNum.   setHorizontalAlignment( JLabel.LEFT ); lblLabelNum.   setMinimumSize( new Dimension(0,0) ); panelItem.add( lblLabelNum,   " 4  1, 2, 1"  );
      lblLabel.      setHorizontalAlignment( JLabel.LEFT ); lblLabel.      setMinimumSize( new Dimension(0,0) ); panelItem.add( lblLabel,      " 5, 1"        ); TmmFontHelper.changeFont( lblLabel,      Font.BOLD );
      lblLabelSmall. setHorizontalAlignment( JLabel.LEFT ); lblLabelSmall. setMinimumSize( new Dimension(0,0) ); panelItem.add( lblLabelSmall, " 5, 2"        ); TmmFontHelper.changeFont( lblLabelSmall, 0.816     ); 
      lblHasImg.     setHorizontalAlignment( JLabel.LEFT ); lblHasImg.     setMinimumSize( new Dimension(0,0) ); panelItem.add( lblHasImg,     " 6  1, 2, 1"  );
      lblHasNfo.     setHorizontalAlignment( JLabel.LEFT ); lblHasNfo.     setMinimumSize( new Dimension(0,0) ); panelItem.add( lblHasNfo,     " 7, 1, 2, 1"  );
      lblHasSub.     setHorizontalAlignment( JLabel.LEFT ); lblHasSub.     setMinimumSize( new Dimension(0,0) ); panelItem.add( lblHasSub,     " 8, 1, 2, 1"  );
      lblFilename.   setHorizontalAlignment( JLabel.LEFT ); lblFilename.   setMinimumSize( new Dimension(0,0) ); panelItem.add( lblFilename,   " 9, 1, 1, 1"  );
      lblPathname.   setHorizontalAlignment( JLabel.LEFT ); lblPathname.   setMinimumSize( new Dimension(0,0) ); panelItem.add( lblPathname,   "10, 1, 1, 1"  );
    }
    panel_item_unsorted: { //TvShowEpisodes & Movies

      lblLabelNum.   setHorizontalAlignment( JLabel.LEFT ); lblLabelNum.   setMinimumSize( new Dimension(0,0) ); panelItemUnsorted.add( lblLabelNum,   " 4  1, 2, 1"  );
      lblLabel.      setHorizontalAlignment( JLabel.LEFT ); lblLabel.      setMinimumSize( new Dimension(0,0) ); panelItemUnsorted.add( lblLabel,      " 5, 1"        ); TmmFontHelper.changeFont( lblLabel,      Font.BOLD );
      lblLabelSmall. setHorizontalAlignment( JLabel.LEFT ); lblLabelSmall. setMinimumSize( new Dimension(0,0) ); panelItemUnsorted.add( lblLabelSmall, " 5, 2"        ); TmmFontHelper.changeFont( lblLabelSmall, 0.816     ); 
      lblHasImg.     setHorizontalAlignment( JLabel.LEFT ); lblHasImg.     setMinimumSize( new Dimension(0,0) ); panelItemUnsorted.add( lblHasImg,     " 6  1, 2, 1"  );
      lblHasNfo.     setHorizontalAlignment( JLabel.LEFT ); lblHasNfo.     setMinimumSize( new Dimension(0,0) ); panelItemUnsorted.add( lblHasNfo,     " 7, 1, 2, 1"  );
      lblHasSub.     setHorizontalAlignment( JLabel.LEFT ); lblHasSub.     setMinimumSize( new Dimension(0,0) ); panelItemUnsorted.add( lblHasSub,     " 8, 1, 2, 1"  );
      lblFilename.   setHorizontalAlignment( JLabel.LEFT ); lblFilename.   setMinimumSize( new Dimension(0,0) ); panelItemUnsorted.add( lblFilename,   " 9, 1, 1, 1"  );
      lblPathname.   setHorizontalAlignment( JLabel.LEFT ); lblPathname.   setMinimumSize( new Dimension(0,0) ); panelItemUnsorted.add( lblPathname,   "10, 1, 1, 1"  );

    }
    return;
  }

  /**
   * getTreeCellRendererComponent( tree, value, is_sel, is_expanded, is_leaf, row hasFocus ){...
   * @return JPanel The UI for this row, be it an item or group.
   */
  @Override
  public Component getTreeCellRendererComponent( JTree tree, Object obj, boolean is_sel, boolean is_expanded, boolean is_leaf, int row, boolean hasFocus ){
    Component returnValue = null;

    if ( null != obj ){

      //TEST - Would it be beneficial to place this object within the Movie/MovieSet object? Probably not...  Static creation? Neah...
      //if ( MovieSetTreeNode.instanceOf( val )) MovieSetTreeNode.getTreeCellRendererComponent( JTree tree, Object val, boolean is_sel, boolean is_expanded, boolean is_leaf, int row, boolean hasFocus );


      group_Type: if ( obj instanceof String ){ //TODO: This need a proper way to separate TvShows and Movies. For now, I'm cheating by passing a string.

        String userObject = (String) obj;
        if ( StringUtils.isNotBlank( userObject )) 
          lblLabel.setText( userObject );           

        returnValue = panelItem;
      }//end :: MOVIE TREE NODE
    }

    group_MovieSet: if ( obj instanceof MovieSetTreeNode ){

      Object userObject = ( (MovieSetTreeNode) obj ).getUserObject();

      if ( userObject instanceof MovieSet ){
        MovieSet movieSet = (MovieSet) userObject;

        lblLabel.      setText( StringUtils.isNotBlank( movieSet.getTitle() ) ? movieSet.getTitle() : BUNDLE.getString( "tmm.unknowntitle" )); //$NON-NLS-1$
        lblLabelSmall. setText( movieSet.getMovies().size() + " Movies"                             );
        lblHasImg.     setIcon( movieSet.getHasImages() ? IconManager.CHECKMARK : IconManager.CROSS );

        panelGroup.setEnabled( tree.isEnabled() );
        panelGroup.invalidate();

        returnValue = panelGroup;
      }
    }

    group_TvShowSeries: if ( obj instanceof TvShowTreeNode ){   

      Object userObject = ( (TvShowTreeNode) obj ).getUserObject();          
      if ( userObject instanceof TvShow ){

        TvShow tvShow = (TvShow) userObject;

        lblLabel.setText(( StringUtils.isBlank( tvShow.getYear() ) || "0".equals( tvShow.getYear() )) ? tvShow.getTitleSortable() : tvShow.getTitleSortable()+" ("+tvShow.getYear()+")" );

        if ( StringUtils.isBlank( lblLabel.getText() ))
          lblLabel.setText(BUNDLE.getString("tmm.unknowntitle")); //$NON-NLS-1$

        lblLabel.setIcon( tvShow.isNewlyAdded() ? IconManager.NEW : null );

        lblLabelSmall. setText( tvShow.getSeasons().size()+" "+BUNDLE.getString("metatag.seasons")+" - " +tvShow.getEpisodes().size()+" "+ BUNDLE.getString("metatag.episodes") );
        lblHasNfo.     setIcon(tvShow.getHasNfoFile() ? IconManager.CHECKMARK : IconManager.CROSS);
        lblHasImg.     setIcon(tvShow.getHasImages() ? IconManager.CHECKMARK : IconManager.CROSS);

        panelGroup.setEnabled(tree.isEnabled());
        panelGroup.invalidate();

        returnValue = panelGroup;
      }
    }

    group_TvShowSeason: { if ( obj instanceof TvShowSeasonTreeNode ){   

      Object userObject = ( (TvShowSeasonTreeNode) obj ).getUserObject();
      if ( userObject instanceof TvShowSeason ) {

        TvShowSeason season = (TvShowSeason) userObject;
        lblLabel.setText(BUNDLE.getString("metatag.season")+" "+season.getSeason());

        panelSubgroup.setEnabled( tree.isEnabled() ); 

        lblLabel.setIcon( season.isNewlyAdded() ? IconManager.NEW : null );

        panelSubgroup.invalidate();
        returnValue = panelSubgroup;
      }
    }

    item_Movie: { if ( obj instanceof MovieTreeNode ){

      Object userObject = ( (MovieTreeNode) obj ).getUserObject();
      if ( userObject instanceof Movie ){

        Movie movie = (Movie) userObject;

        lblLabel.  setText(( StringUtils.isNotBlank( movie.getTitle() )) ? movie.getTitle() : BUNDLE.getString( "tmm.unknowntitle" ));        
        lblHasNfo. setIcon( movie.getHasNfoFile() ? IconManager.CHECKMARK : IconManager.CROSS );
        lblHasImg. setIcon( movie.getHasImages()  ? IconManager.CHECKMARK : IconManager.CROSS );

        panelItem. setEnabled( tree.isEnabled() );
        panelItem. invalidate();

        returnValue = panelItem;
      }//end :: MOVIE TREE NODE
    }

    item_TvShowEpisode: if ( obj instanceof TvShowEpisodeTreeNode ){

      Object userObject = ( (TvShowEpisodeTreeNode) obj ).getUserObject(); // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
      if ( userObject instanceof TvShowEpisode ){
        TvShowEpisode episode = (TvShowEpisode) userObject;

        lblLabel.setText(( episode.getEpisode() > 0 ) ? ( episode.getEpisode()+". "+episode.getTitle() ) : episode.getTitle() );

        if ( StringUtils.isBlank( lblLabel.getText()) ) {
          lblLabel.setText( BUNDLE.getString( "tmm.unknowntitle" )); //$NON-NLS-1$
        }

        lblLabel.setIcon( episode.isNewlyAdded() ?  IconManager.NEW : null );

        panelItem.setEnabled( tree.isEnabled() );

        lblHasNfo.setIcon( episode.getHasNfoFile() ? IconManager.CHECKMARK : IconManager.CROSS);
        lblHasImg.setIcon( episode.getHasImages()  ? IconManager.CHECKMARK : IconManager.CROSS);
        lblHasSub.setIcon( episode.hasSubtitles()  ? IconManager.CHECKMARK : IconManager.CROSS);

        panelItem.invalidate();
        returnValue = panelItem;
      }
    }

    }//end :: if ( null != obj )

    if ( null == returnValue )
      returnValue = defaultRenderer.getTreeCellRendererComponent( tree, obj, is_sel, is_expanded, is_leaf, row, hasFocus );

    // paint background
    if ( is_sel ) returnValue.setBackground( defaultRenderer.getBackgroundSelectionColor() );
    else          returnValue.setBackground( row % 2 == 0 ? ROW_COLOR_EVEN : ROW_COLOR_ODD   );
    return        returnValue;
    }
  }
}
