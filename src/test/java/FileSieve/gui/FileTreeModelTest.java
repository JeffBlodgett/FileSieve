
package FileSieve.gui;


import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author olgakaraseva
 */
public class FileTreeModelTest {
    
    private FileTreeModel testTreeModel;
    private File testFile;
    private File testDir;
    private File testSubDir;
    private final String userTempFolder = System.getProperty("java.io.tmpdir");
    
    @Before
    public void setup() {
        // create a known state (also known as a baseline)
        testTreeModel = new FileTreeModel(new File(userTempFolder));
        testDir = new File(userTempFolder + "/TestDirectory");
        testSubDir = new File(testDir+"/SubDir");
        testFile = new File(testDir + "/TestFile.txt");
        try {
            testDir.mkdir();
            testSubDir.mkdir();
            testFile.createNewFile();
        } catch (IOException ex) {
            Logger.getLogger(FileTreeModelTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @After
    public void cleanup() {
        if(testFile.exists()){
            testFile.delete();
        }
       if(testSubDir.exists()){
            testSubDir.delete();
       }
       if(testDir.exists()){
           testDir.delete();
       }
    }
    
    @Test
    public void testConstruction() {
        String root = testTreeModel.getRoot().toString()+"/";
        assertTrue("root setup correctly", root.equals(userTempFolder));
    }
    
    @Test
    public void testIsLeaf() {
        assertTrue("File is leaf", testTreeModel.isLeaf(testFile));
        assertFalse("Directory is not leaf", testTreeModel.isLeaf(testDir));
    }
    
    @Test
    public void testGetChildCount(){
        assertEquals("Count directories only", testTreeModel.getChildCount(testDir), 1);
    }
    
    @Test
    public void testGetChild(){
        String firstChild = testTreeModel.getChild(testDir, 0).toString();
        assertTrue("first child is testSubDir", testSubDir.toString().equals(firstChild));
    }
    
    @Test
    public void testGetIndexOfChild(){
        int subDirInd = testTreeModel.getIndexOfChild(testDir, testSubDir);
        assertEquals("subdir is the first child", subDirInd, 0);
    }
    
}
