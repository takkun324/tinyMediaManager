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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import javafx.scene.control.TreeView;

import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.ui.moviesets.MovieSetRootTreeNode;
import org.tinymediamanager.ui.moviesets.MovieSetTreeNode;
import org.tinymediamanager.ui.moviesets.MovieTreeNode;

/**
 * Class CollectionTreeModel.
 * 
 * Controls the behaviors of the (???) object.
 * Tells the CellRenderer how the tree is (to be) ordered.
 * 
 * Used in MovieSetPanel.
 *
 * @author Manuel Laggner
 */
public class TreeModelCollection implements TreeModel {

    //private MovieSetRootTreeNode    treeNodeRoot = new MovieSetRootTreeNode();
    
    private List<TreeModelListener> listeners    = new ArrayList<TreeModelListener>();
    private Map<Object, TreeNode>   treeNodeMap  = Collections.synchronizedMap(new HashMap<Object, TreeNode>()); //A 2-D map of all MovieSets to Movies
    private PropertyChangeListener  propertyChangeListener;
    
    private MovieList               listMovies = MovieList.getInstance();
    private TvShowList              listShows  = TvShowList.getInstance();

    /**
     * Instantiates a new movie set tree model.
     * 
     * @param movieSets
     *          the movie sets
     */
    public TreeModelCollection( List<MovieSet> movieSets ){

        propertyChangeListener = new PropertyChangeListener(){
            @Override
            public void propertyChange(PropertyChangeEvent evt){
                return;
            }//end :: propertyChange(...)
        };//end :: propertyChangeListener(){...}


        // build initial tree with the provided parameter
        treeNodeMap.put( null, new MovieSetTreeNode("Movies") );

        for ( MovieSet movieSet : movieSets ){      
            DefaultMutableTreeNode setNode  = new MovieSetTreeNode(movieSet);
            treeNodeMap.put( movieSet, setNode );

            for ( Movie movie : movieSet.getMovies() ){

                DefaultMutableTreeNode movieNode = new MovieTreeNode( movie );

                setNode.add(            movieNode );  //add 'movieNode' as a child of 'setNode'
                treeNodeMap.put( movie, movieNode );  //map 'movie' to 'movieNode'
            }
            treeNodeRoot.add( setNode ); //add 'setNode' to the root in the tree.

            movieSet.addPropertyChangeListener(propertyChangeListener); // implement change listener
        }
        listMovies.addPropertyChangeListener(propertyChangeListener);
        listShows.addPropertyChangeListener(propertyChangeListener);

        treeNodeRoot.sort();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
     */
    @Override
    public Object getChild(Object parent, int index) {
        return ((TreeNode) parent).getChildAt(index);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.TreeModel#getRoot()
     */
    @Override
    public Object getRoot() {
        return treeNodeRoot;
    }

    /*
     * (non-Javadoc)
     * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
     */
    public int getChildCount(Object parent) {
        return ( (TreeNode) parent ).getChildCount();
    }

    /*
     * (non-Javadoc)
     * @see javax.swing.tree.TreeModel#isLeaf(java.lang.Object)
     */
    public boolean isLeaf( Object node ){

        // root is never a leaf
        if ( node == treeNodeRoot ){
            return false;
        }

        if ( node instanceof MovieSetTreeNode ){
            MovieSetTreeNode mstnode = (MovieSetTreeNode) node;
            if ( mstnode.getUserObject() instanceof MovieSet ){
                return false;
            }
        }

        return getChildCount(node) == 0;
    }

    /*
     * (non-Javadoc)
     * @see javax.swing.tree.TreeModel#getIndexOfChild(java.lang.Object, java.lang.Object)
     */
    public int getIndexOfChild(Object parent, Object child) {
        return ( (TreeNode) parent ).getIndex( (TreeNode) child );
    }

    /*
     * (non-Javadoc)
     * @see javax.swing.tree.TreeModel#addTreeModelListener(javax.swing.event. TreeModelListener)
     */
    public void addTreeModelListener( TreeModelListener x ){ listeners.add(x); return; }

    /*
     * (non-Javadoc)
     * @see javax.swing.tree.TreeModel#removeTreeModelListener(javax.swing.event. TreeModelListener)
     */
    public void removeTreeModelListener( TreeModelListener x ){ listeners.remove(x); return; }

    /*
     * (non-Javadoc)
     * @see javax.swing.tree.TreeModel#valueForPathChanged(javax.swing.tree.TreePath, java.lang.Object)
     */
    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    /**
     * Adds the movie set to the tree's root node.
     * @param movieSet
     *          the movie set
     */
    public void addNodeParent( MovieSet movieSet ){
        synchronized ( treeNodeRoot ){
            MovieSetTreeNode child = new MovieSetTreeNode(movieSet);
            treeNodeMap.put(movieSet, child);
            // add the node
            treeNodeRoot.add(child);
            treeNodeRoot.sort();

            int index = treeNodeRoot.getIndex(child);

            // inform listeners
            TreeModelEvent event = new TreeModelEvent(this, treeNodeRoot.getPath(), new int[] { index }, new Object[] { child });

            for (TreeModelListener listener : listeners) {
                listener.treeNodesInserted(event);
            }
        }

        movieSet.addPropertyChangeListener(propertyChangeListener);
    }

    /**
     * Adds the movie to the given movie set.
     * @param movieSet The MovieSet to add the movie to.
     * @param movie    The Movie to add to the MovieSet.
     */
    private void addNodeChild( MovieSet movieSet, Movie movie ){
        synchronized ( treeNodeRoot ){
            
            MovieSetTreeNode parent = (MovieSetTreeNode) treeNodeMap.get(movieSet);
            MovieTreeNode    child  = new MovieTreeNode(movie);
            
            if ( parent != null ){
                treeNodeMap.put( movie, child );
                parent.add(child);
                int index = parent.getIndex(child);

                // inform listeners
                TreeModelEvent event = new TreeModelEvent( this, parent.getPath(), new int[]{index}, new Object[]{child} );
                for ( TreeModelListener listener : listeners ){
                    listener.treeNodesInserted(event);
                }
            }
        }
    }

    /**
     * removeMovie( MovieSet, Movie )
     * Removes the movie.
     * @param movieSet
     * @param movie
     */
    private void removeNodeChild( MovieSet movieSet, Movie movie ){
        synchronized (treeNodeRoot) {

            // get the movie set node
            MovieSetTreeNode parent = (MovieSetTreeNode) treeNodeMap.get(movieSet);
            MovieTreeNode    child  = (MovieTreeNode)    treeNodeMap.get(movie);

            if (parent != null && child != null && parent.isNodeChild(child)) {
                int index = parent.getIndex(child);
                parent.remove(child);
                treeNodeMap.remove(movie);

                // inform listeners
                TreeModelEvent event = new TreeModelEvent( this, parent.getPath(), new int[]{index}, new Object[] {child} );
                for (TreeModelListener listener : listeners) {
                    listener.treeNodesRemoved(event);
                }
            }
        }
    }

    /**
     * Removes the movie set.
     * @param movieSet
     */
    public void removeNodeParent( MovieSet movieSet ){ //removeMovieSet
        synchronized ( treeNodeRoot ){

            MovieSetTreeNode node = (MovieSetTreeNode) treeNodeMap.get( movieSet );
            int index = treeNodeRoot.getIndex( node );

            treeNodeMap.remove( movieSet ); movieSet.removePropertyChangeListener( propertyChangeListener );

            for ( Movie movie : movieSet.getMovies() ){
                treeNodeMap.remove( movie ); movie.removePropertyChangeListener( propertyChangeListener );
            }

            node.removeAllChildren();
            node.removeFromParent();

            // inform listeners
            TreeModelEvent event = new TreeModelEvent( this, treeNodeRoot.getPath(), new int[]{index}, new Object[]{node} );

            for ( TreeModelListener listener : listeners ){
                listener.treeNodesRemoved(event);
            }
        }
    }

    /**
     * remove( TreePath )
     * @param path
     */
    public void remove(TreePath path) {
        synchronized (treeNodeRoot) {

            DefaultMutableTreeNode node   = (DefaultMutableTreeNode) path.getLastPathComponent();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) path.getParentPath().getLastPathComponent();
            int                    index  = parent.getIndex(node);

            // MOVIESET :: remove a movieset and all referenced movies
            if ( node.getUserObject() instanceof MovieSet ){                        //MovieSet
                MovieSet movieSet = (MovieSet) node.getUserObject();                //MovieSet

                for ( Movie movie : movieSet.getMovies() ){                         //Movie/MovieSet
                    movie.      setMovieSet(null);                                  //Movie
                    movie.      writeNFO();                                         //Movie
                    movie.      saveToDb();                                         //Movie
                    treeNodeMap.remove(movie);                                      //Movie
                }

                movieSet.   removeAllMovies();                                      //MovieSet
                movieSet.   removePropertyChangeListener( propertyChangeListener ); //MovieSet
                listMovies. removeMovieSet( movieSet );                             //MovieSet
                treeNodeMap.remove( movieSet );                                     //MovieSet
                node.       removeAllChildren();
                node.       removeFromParent();

                // inform listeners
                TreeModelEvent event = new TreeModelEvent( this, parent.getPath(), new int[] { index }, new Object[] { node });

                for ( TreeModelListener listener : listeners ) 
                    listener.treeNodesRemoved( event );
            }

            // MOVIE remove a movie
            if ( node.getUserObject() instanceof Movie ){
                Movie    movie    = (Movie) node.getUserObject();
                MovieSet movieSet = movie.getMovieSet();

                if ( movieSet != null )
                    movieSet.removeMovie(movie);

                treeNodeMap.remove(movie);

                movie.setMovieSet(null);
                movie.writeNFO();
                movie.saveToDb();

                // here we do not need to inform listeners - is already done via
                // propertychangesupport (movieSet.removeMovie)
            }
        }
    }

    /**
     * Sorts all child nodes within a parent.
     * All immediate-child nodes, one level after the given parent-node, will be sorted.
     * 
     * @param movieSet The
     */
    public void sortChildren( MovieSet parent ){ //sortMoviesInMovieSet
        synchronized ( treeNodeRoot ){
            MovieSetTreeNode node = (MovieSetTreeNode) treeNodeMap.get(parent);
            node.sort();

            // inform listeners
            TreeModelEvent event = new TreeModelEvent( this, node.getPath() );

            for ( TreeModelListener listener : listeners ){
                listener.treeStructureChanged(event);
            }
        }
    }

    /**
     * Sorts all parent-nodes.
     * All first-nodes, after root, will be sorted.
     */
    public void sortParents() { //sortMovieSets
        synchronized ( treeNodeRoot ){
            treeNodeRoot.sort();

            // inform listeners
            TreeModelEvent event = new TreeModelEvent(this, treeNodeRoot.getPath());

            for ( TreeModelListener listener : listeners ){
                listener.treeStructureChanged(event);
            }
        }
    }
}
