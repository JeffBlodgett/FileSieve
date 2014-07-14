package FileSieve.gui;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import javax.swing.JCheckBox;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Junit test for CheckTreeManager class
 * @author olgakaraseva
 */
public class CheckTreeManagerTest {
    
    private JTree treeToTransform; // a tree that will be transformed to checkbox tree
    private CheckTreeManager checkTreeManager; // class under test
    private static final int SELECT_LEVEL = 3; //only 3d level tree nodes can be selected
    
    @Before
    public void setup() {
        //stub a JTree
        DefaultMutableTreeNode allfiles = new DefaultMutableTreeNode("Duplicate files");
        
        for(int filename = 0; filename < 3; filename++){
            DefaultMutableTreeNode dupFilename = new DefaultMutableTreeNode("File "+filename);
            for(int duplicate = 0; duplicate < 3; duplicate++){
                DefaultMutableTreeNode dupFile = new DefaultMutableTreeNode("Folder "+duplicate+"/File "+filename);
                dupFilename.add(dupFile);
            }
            allfiles.add(dupFilename);
        }
        
        treeToTransform = new JTree(allfiles);
          
        //convert it to checkbox tree
        checkTreeManager = new CheckTreeManager(treeToTransform, SELECT_LEVEL);
        checkTreeManager.isTest = true; // we're in testing mode - prevents files from being opened
    }
    
    @Test
    public void testGetSelectionPaths(){
        //add 2 paths to selection model
        TreePath selectPath0 = treeToTransform.getPathForRow(1);
        checkTreeManager.getSelectionModel().addSelectionPath(selectPath0);
        TreePath selectPath1 = treeToTransform.getPathForRow(2);
        checkTreeManager.getSelectionModel().addSelectionPath(selectPath1);
        
        TreePath[] selectedPaths = checkTreeManager.getSelectionPaths();
        for(int i=0; i<selectedPaths.length; i++){
            assertTrue("selectedPaths are correct", selectedPaths[i].getLastPathComponent().toString().equals("File "+i));
        }
    }
    
    @Test
    public void testMouseClicked(){     
        //expand all tree rows since only last level nodes can actually be selected 
        int row = 0;
        while(row < treeToTransform.getRowCount()){
            treeToTransform.expandRow(row);
            row++;
        }
        
        TreePath[] selectedPaths = checkTreeManager.getSelectionPaths();
        assertEquals("by default no nodes are selected", 0, selectedPaths.length);
        
        //mock the mouse event
        MouseEvent me = mock(MouseEvent.class);
        
        //click 2nd level node
        int testRow = SELECT_LEVEL-2; //Row 1 = File 0
        Rectangle rowBounds = treeToTransform.getRowBounds(testRow);
        when(me.getX()).thenReturn(rowBounds.x+1);
        when(me.getY()).thenReturn(rowBounds.y+1);
        
        checkTreeManager.mouseClicked(me);
        selectedPaths = checkTreeManager.getSelectionPaths();
        assertEquals("2nd level node shouldn't react to clicks", 0, selectedPaths.length);
        
        //click outside the treenode
        when(me.getX()).thenReturn(rowBounds.x+rowBounds.width+10);
        checkTreeManager.mouseClicked(me);
        assertEquals("Clicks outside the treenode should be disregarded", 0, selectedPaths.length);
        
        //click 3d level node
        testRow = SELECT_LEVEL-1; //Row 2 = Folder 0/File 0
        rowBounds = treeToTransform.getRowBounds(testRow);
        when(me.getX()).thenReturn(rowBounds.x+1);
        when(me.getY()).thenReturn(rowBounds.y+1);
        
        checkTreeManager.mouseClicked(me);
        selectedPaths = checkTreeManager.getSelectionPaths();
        assertEquals("node was selected on checkbox click", 1, selectedPaths.length);
        for(TreePath path : selectedPaths){
            assertTrue("correct node was selected and no other nodes were selected", 
                    path.getLastPathComponent().toString().equals("Folder 0/File 0"));
        }
        
        //clicking it again should deselect it
        checkTreeManager.mouseClicked(me);
        selectedPaths = checkTreeManager.getSelectionPaths();
        assertEquals("node was deselected on second checkbox click", 0, selectedPaths.length);
        
        //double click outside the checkbox = on filename
        int hotspot = new JCheckBox().getPreferredSize().width;
        when(me.getX()).thenReturn(rowBounds.x+hotspot+5);
        when(me.getClickCount()).thenReturn(1);
        checkTreeManager.mouseClicked(me);
        //clicking one time does nothing
        
        when(me.getClickCount()).thenReturn(2);
        checkTreeManager.mouseClicked(me);
        assertEquals("click outside checkbox shouldn't select node", 0, selectedPaths.length);
    }
    
}
