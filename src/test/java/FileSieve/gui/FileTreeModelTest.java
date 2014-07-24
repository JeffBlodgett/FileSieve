package FileSieve.gui;


import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Junit test for FileTreeModel class
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
        testTreeModel = new FileTreeModel(new File(userTempFolder));
        testDir = new File(userTempFolder + System.getProperty("file.separator")+"TestDirectory");
        testSubDir = new File(testDir+System.getProperty("file.separator")+"SubDir");
        testFile = new File(testDir + System.getProperty("file.separator")+"TestFile.txt");
        try {
            testDir.mkdir();
            testSubDir.mkdir();
            testFile.createNewFile();
        } catch (IOException ex) {
            fail("Couldn't create test files");
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
        String root = testTreeModel.getRoot().toString()+System.getProperty("file.separator");
        assertTrue("root setup correctly", root.equals(userTempFolder));
    }
    
    @Test 
    public void testConstructionFile(){
        FileTreeModel testTreeModelFile = new FileTreeModel(testFile);
        assertTrue("when passing a file to constructor root should be leaf", 
                testTreeModelFile.isLeaf(testTreeModelFile.getRoot()));
    }
    
    @Test
    public void testIsLeaf() {
        assertTrue("File is leaf", testTreeModel.isLeaf(testFile));
        assertFalse("Directory is not leaf", testTreeModel.isLeaf(testDir));
    }
    
    @Test
    public void testGetChildCount(){
        assertEquals("Count directories only", testTreeModel.getChildCount(testDir), 1);
        assertEquals("Files shouldn't have children", testTreeModel.getChildCount(testFile), 0);
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
