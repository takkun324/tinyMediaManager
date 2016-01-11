package org.tinymediamanager.ui.collections;

import java.util.Collections;
import java.util.Comparator;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.ui.tvshows.TvShowSeasonTreeNode;
// Possible Entities to be listed:


/**
 * The Class TreeNodeCollectionItem.
 * This class represents the non-group ui-object in a jTree.
 * 
 * @author Manuel Laggner
 */
public class TreeNodeCollectionItem extends DefaultMutableTreeNode {


  /** The Constant serialVersionUID. */
  private static final long    serialVersionUID = -1316609340104597133L;


  /** The node comparator.
*/
  private static Comparator<TreeNode> nodeComparator;


  /**
   * Instantiates a new tv show tree node.
   * @param userObject
   */
  public TreeNodeCollectionItem( Object userObject ){
    super( userObject );

    if ( null == nodeComparator ){
        /**
         * Comparator nodeComparator
         * Determines how each season is ordered within the TvShowTree.
         * This method returns a negative integer, zero, or a positive integer as the specified String is greater than, equal to, or less than this String, ignoring case considerations.
         *
         * @return Assuming a sort-order ascending, returns a nevative integer if o1 comes after 02, zero if they are equal, or a positive integer if 01 comes before 02. 
         */
        nodeComparator = new Comparator<TreeNode>() {
          @Override
          public int compare(TreeNode o1, TreeNode o2) {

              if (o1 instanceof TvShowSeasonTreeNode && o2 instanceof TvShowSeasonTreeNode) { // IF BOTH ARE SeasonTreeNodes...
              TvShowSeasonTreeNode node1 = (TvShowSeasonTreeNode) o1; TvShowSeason tvShowSeason1 = (TvShowSeason) node1.getUserObject();
              TvShowSeasonTreeNode node2 = (TvShowSeasonTreeNode) o2; TvShowSeason tvShowSeason2 = (TvShowSeason) node2.getUserObject();
              return tvShowSeason1.getSeason() - tvShowSeason2.getSeason();
            }
            return o1.toString().compareToIgnoreCase( o2.toString() );
          }
        };
    }


    //GENERIC COMPARATOR
    nodeComparator = new Comparator<TreeNode>() {
      @Override
      public int compare(TreeNode o1, TreeNode o2) {
/*
	  if (o1 instanceof TvShowSeasonTreeNode && o2 instanceof TvShowSeasonTreeNode) { // IF BOTH ARE SeasonTreeNodes...

	      TreeNodeCollectionItem node1 = (TreeNodeCollectionItem) o1; //Copy the nodes
	      TreeNodeCollectionItem node2 = (TreeNodeCollectionItem) o2;

	      TvShowSeason uObj1 = (TvShowSeason) node1.getUserObject(); //Swap their "UserObject"s accordingly.
	      TvShowSeason uObj2 = (TvShowSeason) node2.getUserObject();

	      return uObj1.getSeason() - uObj2.getSeason(); //return their value-difference (ie: Sort order by integer).
	  }
*/	  return o1.toString().compareToIgnoreCase( o2.toString() ); //NOT SEASON NODEX, return their sort-order.
      }
    };
    return; //end :: TvShowTreeNode( userObject )
  }

  /**
   * Provides the right name of the node for display.
   * If this node is a TvShow, the title will be displayed instead.
   * 
   * @return the string
   */
  @Override
  public String toString() {

    if ( getUserObject() instanceof TvShow ){
      TvShow tvShow = (TvShow) getUserObject();
      return tvShow.getTitle();
    }

    return super.toString(); //Otherwise, fallback.
  }




  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.tree.DefaultMutableTreeNode#insert(javax.swing.tree.MutableTreeNode, int)
   */
  @SuppressWarnings("unchecked")
  @Override
  public void insert( MutableTreeNode newChild, int childIndex ){
    if ( this.children != null ){
      int index = Collections.binarySearch(this.children, newChild, nodeComparator);

      if ( index < 0 ){
        super.insert(newChild, -index - 1);
	return;
      }

      else if (index >= 0) {
        super.insert(newChild, index);
	return;
      }
    }

    super.insert(newChild, childIndex);
    return;
  }
}
