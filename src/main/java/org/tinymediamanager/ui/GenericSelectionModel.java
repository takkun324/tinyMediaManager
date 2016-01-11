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
package org.tinymediamanager.ui.moviesets;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;

/**
 * GenericSelectionModel
 * 
 * @author Kevin Jackson
 */
public class GenericSelectionModel extends AbstractModelObject {
    private static final String    SELECTED_MOVIE_SET = "itemSelected";

    Class classItem   = null; //Nodes that we are concerned with.
    Class classChild  = null; //Child-Item class (if applicable) (typically used when deleting child items)
    
    private Object                 itemSelected; //MovieSet selectedMovieSet
    private Object                 itemInital;   //MovieSet initialMovieSet = new MovieSet("")
    private PropertyChangeListener propertyChangeListener;
    private JTree                  tree;
    
    /**
     * https://docs.oracle.com/javase/tutorial/reflect/member/ctorInstance.html
     * @param c The class object to instantiate (ex: Class.forName("myClassNameAsString")).
     * @param params An array of arguments to pass to the class's constructor.
     * @return
     */
    Object tmpMakeInstance( Class c, Object[] params ){
        
        if ( null == params ) params = new Object[]{}; //if no arguments are passed, use an empty object array.

        Class[] types = {Double.TYPE, this.getClass()};        
        return ( c.getConstructor(types) ).newInstance(params);
    }

    /**
     * Constructor.
     * @param JTree tree
     * @param String classChild
     * @param String classItem
     * @see org.tinymediamanager.ui.moviesets.MovieSetPanel
     * @see org.tinymediamanager.ui.moviesets.MovieSetSelectionModel
     */
    public GenericSelectionModel( JTree tree, String classItem, String classChild ) {
        itemSelected = itemInital;

        this.classItem   = Class.forName(classItem); //@see java.lang.reflect
        this.classChild  = Class.forName(classChild);
        this.tree        = tree;
        this.itemInital  = this.tmpMakeInstance( this.classChild, new String[]{""} ); //new MovieSet("");

        propertyChangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange( PropertyChangeEvent evt ){ firePropertyChange(evt); }
        };
    }
    
    /**
     * @see org.tinymediamanager.ui.moviesets.GenericSelectionModel.GenericSelectionModel()
     */
    public GenericSelectionModel( JTree tree, String classItem ){
        itemSelected = itemInital;

        this.classItem = Class.forName(classItem);
        this.tree      = tree;

        propertyChangeListener = new PropertyChangeListener(){
            @Override
            public void propertyChange( PropertyChangeEvent evt ){ firePropertyChange(evt); }
        };
    }

    /**
     * Sets the selected movie set.
     * 
     * @param movieSet
     *          the new selected movie set
     * @see org.tinymediamanager.ui.moviesets.MovieSetSelectedModel.setSelectedMovieSet()
     */
    public void setSelected( Object item /* ==> MovieSetmovieSet */ ){
        Object oldValue = this.itemSelected;//MovieSet oldValue = this.itemSelected;

        this.itemSelected = ( item == null ) ? itemInital : item;

        if ( null != oldValue     ){ oldValue.removePropertyChangeListener(propertyChangeListener);  } //remove the PropertyChangeListener from the old item
        if ( null != itemSelected ){ itemSelected.addPropertyChangeListener(propertyChangeListener); } //add the PropertyChangeListener to the new item
        firePropertyChange( SELECTED_MOVIE_SET, oldValue, this.itemSelected );
    }

    /**
     * Gets the selected movie set.
     * 
     * @return the selected movie set
     */
    public Object getSelected(){ //MovieSet
        return itemSelected;
    }

    /**
     * Gets the selected movie sets
     * @TODO Move into ea. object type (ex: Movie, MovieSet, TvShow, TvShowSeason, TvShowEpisode ). Until then, the receiving function will need to cast to the correct type. 
     * 
     * @return the selected movie sets
     * @see org.tinymediamanager.ui.moviesets.MovieSetSelectedModel.setSelectedMovieSets()
     */
    public List<Object> getSelectedItems() { //public List<MovieSet> getSelectedMovieSets() {
        List<Object> list_items_sel = new ArrayList<Object>(); //List<MovieSet> selectedMovieSets = new ArrayList<MovieSet>();
        TreePath[] paths = tree.getSelectionPaths();

        if (paths != null) return list_items_sel;

        //filter out all movie sets from the selection
        for ( TreePath path : paths ){
            if ( path.getPathCount() > 1 ){ //movie-sets are at a path-count of 1
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.getUserObject() instanceof MovieSet) {
                    MovieSet movieSet = (MovieSet) node.getUserObject();
                    list_items_sel.add(movieSet);
                }
            }
        }

        return list_items_sel;
    }

    /**
     * get all selected movies. selected movie sets will NOT return all their movies.
     * @return list of all selected movies
     */
    public List<Movie> getSelectedMovies() {
        List<Movie> selectedMovies = new ArrayList<Movie>();
        TreePath[] paths = tree.getSelectionPaths();

        // filter out all movie sets from the selection
        if (paths == null) 
            return selectedMovies;
        
        for (TreePath path : paths) {
            if (path.getPathCount() > 1) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.getUserObject() instanceof Movie) {
                    Movie movie = (Movie) node.getUserObject();
                    selectedMovies.add(movie);
                }
            }
        }

        return selectedMovies;
    }

    /**
     * get all selected movies. selected movie sets will return all their movies
     * @return list of all selected movies
     */
    public List<Movie> getSelectedMoviesRecursive() {
        List<Movie> selectedMovies = new ArrayList<Movie>();

        TreePath[] paths = tree.getSelectionPaths();

        // filter out all movie sets from the selection
        if (paths != null) {
            for (TreePath path : paths) {
                if (path.getPathCount() > 1) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (node.getUserObject() instanceof MovieSet) {
                        MovieSet movieSet = (MovieSet) node.getUserObject();
                        for (Movie movie : movieSet.getMovies()) {
                            if (!selectedMovies.contains(movie)) {
                                selectedMovies.add(movie);
                            }
                        }
                    }
                    if (node.getUserObject() instanceof Movie) {
                        Movie movie = (Movie) node.getUserObject();
                        if (!selectedMovies.contains(movie)) {
                            selectedMovies.add(movie);
                        }
                    }
                }
            }
        }

        return selectedMovies;
    }
}
