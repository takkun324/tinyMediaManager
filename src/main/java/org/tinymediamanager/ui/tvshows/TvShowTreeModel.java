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
package org.tinymediamanager.ui.tvshows;

import static org.tinymediamanager.core.Constants.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.ui.tvshows.TvShowExtendedMatcher.SearchOptions;

/**
 * The Class TvShowTreeModel.
 * 
 * @author Manuel Laggner
 */
public class TvShowTreeModel implements TreeModel {
    private TvShowRootTreeNode      root       = new TvShowRootTreeNode();
    private List<TreeModelListener> listeners  = new ArrayList<TreeModelListener>();
    private Map<Object, TreeNode>   nodeMap    = Collections.synchronizedMap(new HashMap<Object, TreeNode>());
    private TvShowList              tvShowList = TvShowList.getInstance();
    private PropertyChangeListener  propertyChangeListener;
    private TvShowExtendedMatcher   matcher    = new TvShowExtendedMatcher();

    /**
     * Instantiates a new tv show tree model.
     * 
     * @param tvShows
     *          the tv shows
     */
    public TvShowTreeModel(List<TvShow> tvShows) {

        propertyChangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange( PropertyChangeEvent evt ) {

                String prop = evt.getPropertyName();
                Object valu = evt.getNewValue();
                Object sorc = evt.getSource();

                // added a tv show
                if ( ADDED_TV_SHOW.equals(prop) && valu instanceof TvShow) {
                    TvShow tvShow = (TvShow) valu;
                    addTvShow(tvShow);
                }

                // removed a tv show
                if ( REMOVED_TV_SHOW.equals(prop) && valu instanceof TvShow) {
                    TvShow tvShow = (TvShow) valu;
                    removeTvShow(tvShow);
                }

                // added a season
                if ( ADDED_SEASON.equals(prop) && valu instanceof TvShowSeason) {
                    TvShowSeason season = (TvShowSeason) valu;
                    // need to lock it here, because of nested calls
                    synchronized (root) {
                        addTvShowSeason(season, season.getTvShow());
                    }
                }

                // added an episode
                if ( ADDED_EPISODE.equals(prop) && valu instanceof TvShowEpisode) {
                    TvShowEpisode episode = (TvShowEpisode) valu;
                    addTvShowEpisode(episode, episode.getTvShow().getSeasonForEpisode(episode));
                }

                // removed an episode
                if ( REMOVED_EPISODE.equals(prop) && valu instanceof TvShowEpisode) {
                    TvShowEpisode episode = (TvShowEpisode) valu;
                    removeTvShowEpisode(episode);
                }

                // changed the season/episode nr of an episode
                if (( SEASON.equals(prop) || EPISODE.equals(prop)) && sorc instanceof TvShowEpisode) {
                    // simply remove it from the tree and read it
                    TvShowEpisode episode = (TvShowEpisode) sorc;
                    removeTvShowEpisode(episode);
                    addTvShowEpisode(episode, episode.getTvShow().getSeasonForEpisode(episode));
                }

                // update on changes of tv show
                if (sorc instanceof TvShow && (TITLE.equals(prop) || HAS_NFO_FILE.equals(prop) || HAS_IMAGES.equals(prop) )) {
                    // inform listeners (root - to update the sum)
                    TreeModelEvent event = new TreeModelEvent(this, root.getPath(), null, null);
                    for (TreeModelListener listener : listeners) {
                        listener.treeNodesChanged(event);
                    }
                }

                // update on changes of episode
                if ( sorc instanceof TvShowEpisode ) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodeMap.get(sorc);

                    if ( node != null ) {
                        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                        int index = parent.getIndex(node);

                        if ( index >= 0 ){
                            TreeModelEvent event = new TreeModelEvent(this, parent.getPath(), new int[] { index }, new Object[] { node });

                            for ( TreeModelListener listener : listeners ){
                                try   { listener.treeNodesChanged(event); }
                                catch ( Exception e ) {}
                            }
                        }
                    }
                }

                return;
            } // end :: void PropertyChange(...)
        }; //end :: void PropertyChangeListener(){ ...

        // install a property change listener to the TvShowList
        tvShowList.addPropertyChangeListener(propertyChangeListener);

        // build initial tree
        for (TvShow tvShow : tvShows) {
            addTvShow(tvShow);
        }

        return;
    }
    
    /* *** GOAL ***
     *
     * DomainObjectA > DomainObjectB > DomainObjectC
     * BRANCH        > BRANCH        > LEAF
     * GROUP         > GROUP         > ITEM
     * TvShows       > Seasons       > Episodes
     *
     * @TODO Custom JTree Sorting
     * Note: Node Sorting - https://community.oracle.com/thread/1356694?start=0&tstart=0
     *  The "column" or "data/String" can be assigned to the domain-object itself. Then the Node(s) take it & sort with it from there.
     *
     * DomainObjectA.getTreeModel( root ); //GET THE ENTIRE TREE OF THE NODE'S TYPE (EX: Tree of all TvShow objects)  
     * |    
     * |    * TreeModel             model  = new TreeModel();
     * |    * List< DomainObjectA > listItems = this.getItems( model, null ); //List of items representing itself. (ex: TvShows)
     * |    * |     *
     * |    * |     * //==========================================
     * |    * |     * // DomainObject.getItems( TreeModel model )     
     * |    * |     * //==========================================
     * |    * |     *
     * |    * |     * for ( DomainObjectA item : listItems ) (branches)
     * |    * |     * {
     * |    * |     *   //If the node does not exist
     * |    * |     *   if ( null == (GroupNode) nodeMap.get( item )){
     * |    * |     *
     * |    * |     *       DefaultMutableTreeNode nodeItem = new GroupNode( item );
     * |    * |     *       model.put( item, nodeItem );
     * |    * |     *   }
     * |    * |     *
     * |    * |     *   //If DomainObjectA items are GROUPS (FYI: We're still in our calling-object [ex:TvShow])
     * |    * |     *   if ( item.isGroup() )
     * |    * |     *   {
     * |    * |     *        
     * |    * |     *       List< DomainObjectB > listSubItems = item.getSubItems( model, item == parent );       // DomainObjectA implements TmmTreeModel{ getSubItems(), isGroup=false, }
     * |    * |     *       |   * 
     * |    * |     *       |   * //==================================================================
     * |    * |     *       |   * // DomainObject.getSubItems( TreeModel model, DomainObjectA parent )     
     * |    * |     *       |   * //==================================================================
     * |    * |     *       |   *
     * |    * |     *       |   * List< DomainObjectA > listItems = this.getItems(); //List of items representing itself. (ex: TvShows)
     * |    * |     *       |   * 
     * |    * |     *       |   * 
     * |    * |     *       |   * 
     * |    * |     *       |   * 
     * |    * |     *       |   * 
     * |    * |     *       |   * for ( DomainObjectB item : listItems ) (branches)
     * |    * |     *       |   * {
     * |    * |     *       |   *   ...calling the same generic FOR-LOOP as the previous one.
     * |    * |     *       |   * }
     * |    * |     *   }
     * |    * |     *   //Else "item" is a child-less leaf
     * |    * |     *   else {
     * |    * |     *       
     * |    * |     *   
     * |    * |     *  
     * |    * |     *   
     * |    * |     *  
     * |    * |     *   
     * |    * |     *  
     * |    * |     *   
     * |    *  
     * |    *   
     * |    *   
     * |    *   //Branches are ALWAYS containers for more nodes.
     * |    *   TreeModelBranchNode branch = addBranch( DomainObjectB group );
     * |        |
     * |        |     * TreeModelBranchNode branch = new TreeModelBranchNode();
     * |        |     * 
     * |        |     * return branch; 
     * |    * }
     * |    *  
     * |    *  
     * |    *  
     * |    * 
     * |    *   
     * |    *
     * |    *
     * 
     * 
     */

    /* *** addTvShow - ADD "CHILD-GROUP" TO "ROOT"                      addGroup( grp_node_parent = root, grp_obj_child = objChild );
     * 1. RETRIEVE PARENT NODE :: n/a (parent node is 'root')
     * 
     * 2. Create a node for the passed "child-group" object             if( grp_obj_child instanceof MyObj ) MyObjNode grp_node_child = new MyObjNode( grp_obj_child );
     * 2a. Assign our new-node to its parent node (root, in this case)  grp_node_parent.add( grp_node_child );
     * 2b. Map the node to the user's data-object representing it.      this.treeNodeDomainMap.put( grp_obj_child, grp_node_child );
     * 
     * 3. For each "child" to the user's data-object...                 for ( grp_node_childs_grpchild : new ArrayList< objSubChild >( objChild.getChildren() )){
     * 3a. If that node has not been mapped, add it.                        if ( null == (nodeSubChild) nodeMap.get( grp_node_childs_grpchild )){  }                        
     * 3b. Go over this again and add the "children" to this child-object (that we've just already added).
     *                                                                      for
     *                                                                      }
     * 4. Add each "child" object of those added in 3b. 
     */
    /**
     * Adds the tv show to the 'this.root', including all seasons & episodes therein; creating the node-hierarchy as it goes.
     * 
     * @param tvShow The domain-object representing a Tv Show.
     */
    private void addTvShow( TvShow tvShow ){
        synchronized ( root ){

            //RETRIEVE PARENT NODE 
            //ACTUALLY, IT'S ROOT SO WE DONT NEED TO!  :P

            //CREATE A NODE FOR THE PASSED-IN OBJECT
            DefaultMutableTreeNode tvShowNode = new TvShowTreeNode(tvShow);
            root.add(tvShowNode);               //ASSIGN OUR NEW-NODE TO IT'S PARENT
            nodeMap.put( tvShow, tvShowNode );  //MAP OUR NODE TO IT'S ASSOSIATED OBJECT FOR LATER REFERENCE

            //FOR EACH SEASON (nodemap-key) in the given array
            for ( TvShowSeason season : new ArrayList<TvShowSeason>( tvShow.getSeasons() )){

                //THE NODE HASN'T BEEN CREATED YET.
                if ( null == (TvShowSeasonTreeNode) nodeMap.get(season) ){ //IF nodeMap.get(season) returns NULL

                    // NODE - ADD THE 'SEASON' TO THE 'TVSHOW'
                    addTvShowSeason( season, tvShow );
                }

                // NODE - ADD THE 'EPISODE' NODES TO THE SEASON
                for ( TvShowEpisode episode : new ArrayList<TvShowEpisode>( season.getEpisodes() )){

                    // NODE - ADD THE 'SEASON' TO THE 'TVSHOW'
                    addTvShowEpisode( episode, season );
                }
            }            

            //...inform listeners...

            //GET CHILD-INDEX OF "CHILD-NODE" (tvShowNode) WITHIN THE "PARENT-NODE" (root)
            int index = getIndexOfChild( root, tvShowNode );
            
            if ( index > -1 ){ //INDEX WAS FOUND, A CHILD-NODE EXISTS
                TreeModelEvent event = new TreeModelEvent( this, root.getPath(), new int[] { index }, new Object[] { tvShow });

                for ( TreeModelListener listener : listeners ){

                    // catch problems with adding new nodes in a filtered tree
                    try {
                        listener.treeNodesInserted( event );
                    }
                    catch ( NullPointerException npe ) {
                    }
                }
            }
        }
        tvShow.addPropertyChangeListener( propertyChangeListener );
    }

    /**
     * Removes the tv show.
     * 
     * @param tvShow the tv show
     */
    private void removeTvShow(TvShow tvShow) {
        synchronized ( root ){
            TvShowTreeNode child = (TvShowTreeNode) nodeMap.get(tvShow);
            DefaultMutableTreeNode parent = root;
            if (child != null) {
                int index = getIndexOfChild(parent, child);

                nodeMap.remove(tvShow);
                for (TvShowEpisode episode : new ArrayList<TvShowEpisode>(tvShow.getEpisodes())) {
                    nodeMap.remove(episode);
                    episode.removePropertyChangeListener(propertyChangeListener);
                }

                tvShow.removePropertyChangeListener(propertyChangeListener);

                child.removeAllChildren();
                child.removeFromParent();

                // inform listeners
                if (index > -1) {
                    TreeModelEvent event = new TreeModelEvent(this, parent.getPath(), new int[] { index }, new Object[] { child });
                    for (TreeModelListener listener : listeners) {
                        // catch problems with removing nodes in a filtered tree
                        try {
                            listener.treeNodesRemoved(event);
                        }
                        catch (NullPointerException npe) {
                        }
                    }
                }
            }
        }
    }

    
    /* *** addTvShow - ADD "CHILD-GROUP" TO "PARENT-GROUP"
     * 1. Retrieve the Parent Node to work with.
     * 1a. If 
     * 
     * 2. Create a node for the passed "child-group" object
     * 2a. 
     */    
    /**
     * Adds the tv show season.
     * 
     * @param season
     *          the season
     * @param tvShow
     *          the tv show
     */
    private void addTvShowSeason(TvShowSeason season, TvShow tvShow) {

        //1. RETRIEVE PARENT NODE 
        TvShowTreeNode parent = (TvShowTreeNode) nodeMap.get(tvShow);
        
        //IF THE PARENT NODE WAS NOT FOUND
        if ( parent == null ){
            // Why/When would this occur? When the tvShow node hasn't been added.
            // But why would we encounter the Season object before the TvShow object?
            
            //there's nothing more to do. (CHANGED BY KEVIN-J  :P )
            return;
        }

        //2. CREATE A NODE FOR THE PASSED-IN OBJECT
        TvShowSeasonTreeNode child = new TvShowSeasonTreeNode(season);        
        parent.add(child);          //2a. ASSIGN OUR NEW-NODE TO IT'S PARENT
        nodeMap.put(season, child); //2b. MAP OUR NODE TO IT'S ASSOSIATED OBJECT FOR LATER REFERENCE

        //...inform the listeners...
        
        //3. GET CHILD-INDEX OF "CHILD-NODE" (child) WITHIN THE "PARENT-NODE" (parent)
        int index = getIndexOfChild(parent, child);

        //4. IF THE INDEX WAS FOUND, INFORM ALL LISTENERS OF THE PARENT-TYPE OBJECT // tv show listeners (of the season we're adding)
        if ( index > -1 ){

            TreeModelEvent event = new TreeModelEvent(
                                        /* Object   source         */ this,
                                        /* Object[] path           */ parent.getPath(), //(an array of TreePath objects)
                                        /* int[]    child_indicies */ new int[]{ index },
                                        /* Object[] children       */ new Object[]{ child }
            );
            for ( TreeModelListener listener : listeners ){

                // catch problems with adding new nodes in a filtered tree
                try { listener.treeNodesInserted(event); //TELL THE LISTENER A NODE(S) HAS BEEN INSERTED INTO THE TREE
                } catch (NullPointerException npe) {     //WHAT KIND OF PROBLEMS WHEN ADDING NODES IN A FILTERED TREE?
                }
            }
        }

        //4. IF THE INDEX WAS FOUND, INFORM ALL LISTENERS OF THE PARENT-TYPE OBJECT // root listeners, to update the sum
        TreeModelEvent event = new TreeModelEvent( this, root.getPath(), null, null ); //note: the "null, null" indicates that this event is for the ROOT-NODE when launching "treeNodesChanged()"
        for ( TreeModelListener listener : listeners ){ 

            try { listener.treeNodesChanged(event);
            } catch (NullPointerException npe) { // catch problems with changing nodes in a filtered tree
            }
        }
        return;
    }

    /**
     * Adds the tv show episode.
     * 
     * @param episode
     *          the episode
     * @param season
     *          the season
     */
    private void addTvShowEpisode(TvShowEpisode episode, TvShowSeason season) {

        synchronized ( root ){

            // get the tv show season node
            TvShowSeasonTreeNode parent = (TvShowSeasonTreeNode) nodeMap.get(season);

            // no parent (season) here - recreate it
            if (parent == null) {
                addTvShowSeason(season, episode.getTvShow());
                parent = (TvShowSeasonTreeNode) nodeMap.get(season);
            }

            TvShowEpisodeTreeNode child = new TvShowEpisodeTreeNode(episode);
            if (parent != null) {
                parent.add(child);
                nodeMap.put(episode, child);

                int index = getIndexOfChild(parent, child);

                // inform listeners
                if (index > -1) {
                    TreeModelEvent event = new TreeModelEvent( this, parent.getPath(), new int[] { index }, new Object[] { child });
                    for (TreeModelListener listener : listeners) {
                        // catch problems with adding new nodes in a filtered tree
                        try {
                            listener.treeNodesInserted(event);
                        }
                        catch (NullPointerException npe) {
                        }
                    }
                }

                // inform listeners (root - to update the sum)
                TreeModelEvent event = new TreeModelEvent(this, root.getPath(), null, null);
                for (TreeModelListener listener : listeners) {
                    // catch problems with changing nodes in a filtered tree
                    try {
                        listener.treeNodesChanged(event);
                    }
                    catch (NullPointerException npe) {
                    }
                }
            }
        }
        episode.addPropertyChangeListener(propertyChangeListener);
    }

    /**
     * Removes the tv show episode.
     * 
     * @param episode
     *          the episode
     * @param season
     *          the season
     */
    private void removeTvShowEpisode(TvShowEpisode episode) {
        synchronized (root) {
            // get the tv show season node
            TvShowEpisodeTreeNode child = (TvShowEpisodeTreeNode) nodeMap.get(episode);
            TvShowSeasonTreeNode parent = null;
            if (child != null) {
                parent = (TvShowSeasonTreeNode) child.getParent();
            }

            if (parent != null && child != null) {
                int index = getIndexOfChild(parent, child);
                parent.remove(child);
                nodeMap.remove(episode);
                episode.removePropertyChangeListener(propertyChangeListener);

                // inform listeners
                if (index > -1) {
                    TreeModelEvent event = new TreeModelEvent(this, parent.getPath(), new int[] { index }, new Object[] { child });
                    for (TreeModelListener listener : listeners) {
                        // catch problems with removing nodes in a filtered tree
                        try {
                            listener.treeNodesRemoved(event);
                        }
                        catch (NullPointerException npe) {
                        }
                    }
                }

                // remove tv show if there is no more episode in it
                if (parent.getChildCount() == 0) {
                    TvShowSeason season = null;
                    for (Entry<Object, TreeNode> entry : nodeMap.entrySet()) {
                        if (entry.getValue() == parent) {
                            season = (TvShowSeason) entry.getKey();
                        }
                    }
                    if (season != null) {
                        removeTvShowSeason(season);
                    }
                }
            }
        }
    }

    /**
     * Removes the tv show season.
     * 
     * @param season
     *          the season
     */
    private void removeTvShowSeason(TvShowSeason season) {
        synchronized (root) {
            TvShowSeasonTreeNode child = (TvShowSeasonTreeNode) nodeMap.get(season);
            TvShowTreeNode parent = null;
            if (child != null) {
                parent = (TvShowTreeNode) child.getParent();
            }

            if (parent != null && child != null) {
                int index = getIndexOfChild(parent, child);
                parent.remove(child);
                nodeMap.remove(season);

                // inform listeners
                if (index > -1) {
                    TreeModelEvent event = new TreeModelEvent(this, parent.getPath(), new int[] { index }, new Object[] { child });
                    for (TreeModelListener listener : listeners) {
                        // catch problems with removing nodes in a filtered tree
                        try {
                            listener.treeNodesRemoved(event);
                        }
                        catch (NullPointerException npe) {
                        }
                    }
                }
            }
        }
    }

    @Override
    public void addTreeModelListener(TreeModelListener listener) {
        listeners.add(listener);
    }

    @Override
    public Object getChild(Object parent, int index) {
        int count = 0;
        int childCount = getChildCountInternal(parent);
        for (int i = 0; i < childCount; i++) {
            Object child = getChildInternal(parent, i);
            if (matches(child)) {
                if (count == index) {
                    return child;
                }
                count++;
            }
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        int count = 0;
        int childCount = getChildCountInternal(parent);
        for (int i = 0; i < childCount; i++) {
            Object child = getChildInternal(parent, i);
            if (matches(child)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getIndexOfChild(Object parent, Object childToFind) {
        int childCount = getChildCountInternal(parent);
        for (int i = 0; i < childCount; i++) {
            Object child = getChildInternal(parent, i);
            if (matches(child)) {
                if (childToFind.equals(child)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * matches( node )
     * @param Object node
     * @return 
     */
    private boolean matches(Object node) {
        Object bean = null;
        if (node instanceof TvShowTreeNode) {
            bean = (TvShow) ((TvShowTreeNode) node).getUserObject();
        }

        // if the node is a TvShowSeasonNode, we have to check the parent TV show and its episodes
        if (node instanceof TvShowSeasonTreeNode) {
            bean = (TvShowSeason) ((TvShowSeasonTreeNode) node).getUserObject();
        }

        // if the node is a TvShowEpisodeNode, we have to check the parent TV show and the episode
        if (node instanceof TvShowEpisodeTreeNode) {
            bean = (TvShowEpisode) ((TvShowEpisodeTreeNode) node).getUserObject();
        }

        if (bean == null) {
            return true;
        }

        return matcher.matches(bean);
    }

    @Override
    public Object getRoot() {
        return root;
    }

    @Override
    public boolean isLeaf(Object node) {
        // root is never a leaf
        if (node == root) {
            return false;
        }

        if (node instanceof TvShowTreeNode || node instanceof TvShowSeasonTreeNode) {
            return false;
        }

        if (node instanceof TvShowEpisodeTreeNode) {
            return true;
        }

        return getChildCount(node) == 0;
    }

    @Override
    public void removeTreeModelListener(TreeModelListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void valueForPathChanged(TreePath arg0, Object arg1) {
    }

    private int getChildCountInternal(Object node) {
        if (node == null) {
            return 0;
        }
        return ((TreeNode) node).getChildCount();
    }

    private Object getChildInternal(Object parent, int index) {
        return ((TreeNode) parent).getChildAt(index);
    }

    public void setFilter(SearchOptions option, Object filterArg) {
        if (matcher.searchOptions.containsKey(option)) {
            matcher.searchOptions.remove(option);
        }
        matcher.searchOptions.put(option, filterArg);
    }

    public void removeFilter(SearchOptions option) {
        if (matcher.searchOptions.containsKey(option)) {
            matcher.searchOptions.remove(option);
        }
    }

    public void filter(JTree tree) {
        TreePath selection = tree.getSelectionPath();
        List<TreePath> currOpen = getCurrExpandedPaths(tree);
        reload();
        reExpandPaths(tree, currOpen);
        restoreSelection(selection, tree);
    }

    private List<TreePath> getCurrExpandedPaths(JTree tree) {
        List<TreePath> paths = new ArrayList<TreePath>();
        Enumeration<TreePath> expandEnum = tree.getExpandedDescendants(new TreePath(root.getPath()));
        if (expandEnum == null) {
            return null;
        }

        while (expandEnum.hasMoreElements()) {
            paths.add(expandEnum.nextElement());
        }

        return paths;
    }

    private void reExpandPaths(JTree tree, List<TreePath> expPaths) {
        if (expPaths == null) {
            return;
        }
        for (TreePath tp : expPaths) {
            tree.expandPath(tp);
        }
    }

    private void reload() {
        TreeModelEvent event = new TreeModelEvent(this, root.getPath(), null, null);
        for (TreeModelListener listener : listeners) {
            listener.treeStructureChanged(event);
        }
    }

    private void restoreSelection(TreePath path, JTree tree) {
        if (path != null) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) path.getLastPathComponent();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) child.getParent();
            if (getIndexOfChild(parent, child) > -1) {
                tree.setSelectionPath(path);
                return;
            }
        }

        // search first valid node to select
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) getRoot();
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
            if (getIndexOfChild(root, child) > -1) {
                tree.setSelectionPath(new TreePath(child.getPath()));
                break;
            }
        }
    }
}
