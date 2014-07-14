package FileSieve.gui;

import FileSieve.BusinessLogic.FileManagement.FileManagerFactory;
import FileSieve.BusinessLogic.FileManagement.SwingFileManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * Turns a JTree into a checkbox tree
 * Code taken from http://www.jroller.com/santhosh/entry/jtree_with_checkboxes
 * Modified by olgakaraseva to adjust to current app
 * @author Santhosh Kumar T - santhosh@in.fiorano.com
 */
public class CheckTreeManager extends MouseAdapter { 
    private DefaultTreeSelectionModel selectionModel;
    private JTree tree; 
    private int selectLevel; //on which depth level nodes can be selected
    int hotspot = new JCheckBox().getPreferredSize().width; //protected so test could access it
    private SwingFileManager swingFileManager; //used to open files
    boolean isTest = false; //used to skip some gui methods for test purposes
 
    public CheckTreeManager(JTree tree, int selectLevel){ 
        this.tree = tree;
        this.selectLevel = selectLevel;
        selectionModel = new DefaultTreeSelectionModel();
        selectionModel.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION); 
        tree.setCellRenderer(new CheckTreeCellRenderer(tree.getCellRenderer(), selectionModel));
        CheckTreeUI treeUI = new CheckTreeUI();
        treeUI.updateDepthOffset(20);
        tree.setUI(treeUI);
        tree.addMouseListener(this);  
        swingFileManager = FileManagerFactory.getSwingFileManager();
    } 
    
    /**
     * Catches mouse clicks and adds node to selection model if click is on checkbox
     * or opens file if click is on node but not on checkbox.
     * Clicks are processed only for JTree depth level specified by selectLevel parameter
     * If click is outside the node nothing happens
     * @param me            Mouse click event
     */
    @Override
    public void mouseClicked(MouseEvent me){ 
        TreePath path = tree.getPathForLocation(me.getX(), me.getY());
        //proceed only if click is on the node of specified depth level
        if(path != null && path.getPathCount() == selectLevel){
            //if click is on node checkbox select the path
            if(me.getX()<=tree.getPathBounds(path).x+hotspot){ 

                boolean selected = selectionModel.isPathSelected(path);  
 
                if(selected) {
                    selectionModel.removeSelectionPath(path); 
                } else {
                    selectionModel.addSelectionPath(path); 
                }
                tree.treeDidChange(); 
            } else {
                //on double click open the file
                if(me.getClickCount() == 2){
                    Path filePath = Paths.get(path.getLastPathComponent().toString());
                    try {
                        //don't open file during testing
                        if(!isTest){
                            swingFileManager.openPathname(filePath);
                        }
                    } catch (IOException ioe) {
                        if(!isTest){
                            JOptionPane.showMessageDialog(null, "Cannot open file. "+ioe.getMessage(), 
                                    "Can't proceed", JOptionPane.WARNING_MESSAGE);
                        }
                    } //IOException
                } //clicked twice
            } //clicked outside checkbox
        } //path is not null and is leaf
    } //mouseClicked
    
    /**
     * Retrieve checkbox tree selection model
     * @return          selectionModel for checkbox tree
     */
    public DefaultTreeSelectionModel getSelectionModel(){ 
        return selectionModel; 
    } 
    
    /**
     * Convenience method to get selected paths
     * @return          selected paths
     */
    public TreePath[] getSelectionPaths(){
        return selectionModel.getSelectionPaths(); 
    }
}