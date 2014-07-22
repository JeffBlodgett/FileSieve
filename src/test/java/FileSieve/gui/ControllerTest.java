
package FileSieve.gui;

import FileSieve.BusinessLogic.FileManagement.FileManagerFactory;
import FileSieve.BusinessLogic.FileManagement.SwingFileManager;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import static org.mockito.Mockito.*;

/**
 * Junit test for Controller class
 * @author olgakaraseva
 */
public class ControllerTest {
    
    private Controller controller;
    private ScreenSwitcher ss;
    private final String userTempFolder = System.getProperty("java.io.tmpdir");
    private final Path fileTestFolder = new File(userTempFolder + "FileSieveControllerTestFolder").toPath();
    private final SwingFileManager fileManager = FileManagerFactory.getSwingFileManager();
    private long expectedFileBytesWithRecursion = 0; // should be 15 after constructTestPaths is performed
    private int expectedFilesWithRecursion = 0; //should be 11 after constructTestPaths is performed
    private long duplicateFileBytes;
    int totalDuplicateFiles;
    
    @Before
    public void setup() {
        controller = new Controller();
        controller.isTest = true;
        ss = new ScreenSwitcher(controller);
        totalDuplicateFiles = 3; //there are 3 duplicate files in stubbed file structure
        
        Assume.assumeFalse("folder used for tests should not pre-exist", Files.exists(fileTestFolder));
        // Construct folders and files to be used for testing
        constructTestPaths();
    }
    
    @After
    public void cleanup() throws IOException {
        assertTrue("able to delete temp folder constructed by setup() method for tests", fileManager.deletePathname(fileTestFolder));
    }
    
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    
    @Test
    public void testChangeScreen(){
        controller.changeScreen(ScreenEnum.COPYPANEL.btnText());
        testScreenChangedCorrectly(ScreenEnum.COPYPANEL);
        controller.changeScreen(ScreenEnum.RESULTPANEL.btnText());
        testScreenChangedCorrectly(ScreenEnum.RESULTPANEL);
        controller.changeScreen(ScreenEnum.SELECTPANEL.btnText());
        testScreenChangedCorrectly(ScreenEnum.SELECTPANEL);

    }
    
    @Test
    public void testChangeScreenNull(){
        //passing null instead of screen should cause NullPointerException
        thrown.expect(NullPointerException.class);
        controller.changeScreen(null);
    }
    
    @Test
    public void testChangeScreenIllegal(){
        //passing screen that is not in ScreenEnum should cause IllegalArgumentException
        thrown.expect(IllegalArgumentException.class);
        controller.changeScreen("false screen");
    }
    
    private void testScreenChangedCorrectly(ScreenEnum val){
        for (Component comp : ss.screens.getComponents() ) {
            if (comp.isVisible()) {
               assertTrue(val.screenClass().getName()+" changed correctly", 
                       comp.getClass().getName().equals(val.screenClass().getName()));
            }
        }
    }
    
    @Test
    public void testPathsAreSelected(){
        //test with null
        testSourcePath(null);
        
        //test with empty paths object
        TreePath[] pathsEmpty = {};
        testSourcePath(pathsEmpty);
        
        //test with valid object
        TreePath[] pathsNotEmpty = {new TreePath(new Object[] {"tmp", "foo", "bar"})};
        testSourcePath(pathsNotEmpty);  
    }
    
    private void testSourcePath(TreePath[] paths){
        boolean selected = controller.pathsAreSelected(paths);
        if(paths == null){
            assertFalse("if TreePath is null should return false", selected);
        } else if(paths.length == 0){
            assertFalse("if paths are empty should return false", selected);
        } else {
            assertTrue("if there is at least one path should return true", selected);
        }
    }
    

    @Test
    public void testCallCopyJob(){

        //test when no source paths are selected
        controller.callCopyJob(null, false);
        testSourcePath(null);
        
        //test with source path selected
        TreePath[] paths = {new TreePath(new Object[] {fileTestFolder+System.getProperty("file.separator")+"sourceFolder2"})};
        
        //mock the fileChooser
        JFileChooser fileChooserMock = mock(JFileChooser.class);
        when(fileChooserMock.showOpenDialog(any(JFrame.class))).thenReturn(0);
        
        //setup target
        File testTarget = new File(fileTestFolder.resolve(
                "sourceFolder1"+System.getProperty("file.separator")+"folder3").toString());
        when(fileChooserMock.getSelectedFile()).thenReturn(testTarget);       
        controller.fileChooser = fileChooserMock;
        
        //with recursion disabled should write file.dat from fileTestFolder to fileTestFolder/sourceFolder1/folder3
        //check that target folder is empty
        int filesInTarget = testTarget.listFiles().length;
        assertEquals("Target folder is empty", 0, filesInTarget);
        
        controller.callCopyJob(paths, false); 
        testScreenChangedCorrectly(ScreenEnum.COPYPANEL);
        
        //test copy screen is setup correctly
        assertTrue("Target label text is correct", testTarget.toString().equals(controller.copyScreen.targetLabel.getText()));
        
        assertEquals("Progress bar is at 0, ready for new copy operaton", 0, 
                (int) controller.copyScreen.totalProgressBar.getPercentComplete());
        assertEquals("List of files is cleared and ready to display new copy progress", 0, 
                controller.copyScreen.copyListModel.getSize());
        //correct buttons are enabled / disabled
        assertFalse("New Search button should be disabled", controller.copyScreen.newSearchBtn.isEnabled());
        assertTrue("Cancel button should be enabled", controller.copyScreen.cancelBtn.isEnabled());
        
        //wait until the files get copied
        try {
            Thread.sleep(500);
            filesInTarget = testTarget.listFiles().length;
            assertEquals("Copy operation completed successfully", 1, filesInTarget);            
        } catch (InterruptedException ex) {
            fail("Copy job was interrupted");
        }
    }
    
    @Test
    public void testStopCopyJob(){      
        //check that appropriate message is added to list
        controller.stopCopyJob(true);
        String copyJobStoppedStr = "File copying process has been stopped.";
        String lastElementText = controller.copyScreen.copyListModel
                .getElementAt(controller.copyScreen.copyListModel.size()-1).toString();
        assertTrue("Interrupted notification is displayed", copyJobStoppedStr.equals(lastElementText));
        
        
        controller.stopCopyJob(false);
        copyJobStoppedStr = "All files are copied! Hooray!";
        lastElementText = controller.copyScreen.copyListModel
                .getElementAt(controller.copyScreen.copyListModel.size()-1).toString();
        assertTrue("Interrupted notification is displayed", copyJobStoppedStr.equals(lastElementText));
        
       //correct buttons are enabled / disabled
        assertTrue("New Search button should be disabled", controller.copyScreen.newSearchBtn.isEnabled());
        assertFalse("Cancel button should be enabled", controller.copyScreen.cancelBtn.isEnabled());
    }

    @Test
    public void testCallDuplJob(){
        //dupl job is always called from select screen
        controller.changeScreen(ScreenEnum.SELECTPANEL.btnText());
        testScreenChangedCorrectly(ScreenEnum.SELECTPANEL);
        
        //test when no source paths are selected
        controller.callDuplJob(null, false);
        testSourcePath(null);
        
        //test with source path selected
        TreePath[] paths = {new TreePath(new Object[] {fileTestFolder})};

        //test with 0 duplicates (recursion disabled)
        controller.callDuplJob(paths, false);
        //screen shouldn't change because there are no duplicates when recursion is disabled
        testScreenChangedCorrectly(ScreenEnum.SELECTPANEL);
        
        //test with found duplicates (recursion enabled)
        controller.callDuplJob(paths, true);
        //screen should change to result panel
        testScreenChangedCorrectly(ScreenEnum.RESULTPANEL);
        //there should be 3 duplicates of 1 file, so the total tree length should be 4
        assertEquals("Result screen JTree is built correctly", totalDuplicateFiles+1, 
                controller.resultScreen.duplicatesList.getRowCount());
        //2 of the 3 found duplicate files should be selected
        assertEquals("Duplicate files were pre-selected correclty", totalDuplicateFiles-1, 
                controller.resultScreen.checkTree.getSelectionPaths().length);
        
        String totalBytesSearchedStr = FileSieve.gui.util.Utilities.readableFileSize(expectedFileBytesWithRecursion);
        //there are 3 duplicate files
        String duplicateBytesStr = FileSieve.gui.util.Utilities.readableFileSize(duplicateFileBytes*totalDuplicateFiles);
        String resultLabelStr = "Searched "+expectedFilesWithRecursion+" files ("+totalBytesSearchedStr+")."+
                " Found "+totalDuplicateFiles+" duplicate files ("+duplicateBytesStr+").";
        assertTrue("Result text displays correctly", controller.resultScreen.fileCntLabel.getText().equals(resultLabelStr));
    }
    

    @Test    
    public void testCallDeleteJob(){
        //test when no source paths are selected
        controller.callDeleteJob(null);
        testSourcePath(null);
        
        //test with source path selected
        DefaultTreeModel treeModel = (DefaultTreeModel) controller.resultScreen.duplicatesList.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        DefaultMutableTreeNode filename = new DefaultMutableTreeNode("file.dat");
        root.add(filename);
        File testFile = new File(fileTestFolder+System.getProperty("file.separator")+
                "sourceFolder1"+System.getProperty("file.separator")+"folder1"+
                System.getProperty("file.separator")+"file.dat");
        DefaultMutableTreeNode dupFile = new DefaultMutableTreeNode(testFile);
        filename.add(dupFile);
        controller.resultScreen.checkTree.getSelectionModel().addSelectionPath(new TreePath(dupFile.getPath()));
        TreePath[] paths = controller.resultScreen.checkTree.getSelectionPaths();
        
        assertEquals("the tree should have 1 element under root now", 1, treeModel.getChildCount(root));
        assertEquals("the tree should have 1 element under filename", 1, treeModel.getChildCount(filename));
        assertEquals("one path should be selected", 1, 
                controller.resultScreen.checkTree.getSelectionModel().getSelectionCount());
        
        //get number of files in folder
        File deletedFromFolder = new File(fileTestFolder.resolve(
                "sourceFolder1"+System.getProperty("file.separator")+"folder1").toString());
        int filesInFolder = deletedFromFolder.listFiles().length;
        
        controller.callDeleteJob(paths);
        assertEquals("the tree should have 0 elements under filename now", 0, treeModel.getChildCount(filename));
        assertEquals("no paths should be selected", 0, 
                controller.resultScreen.checkTree.getSelectionModel().getSelectionCount());
        //since it was the only file the filename node should also be deleted,
        //so nothing should be left in root
        assertEquals("the tree should have 0 elements under root", 0, treeModel.getChildCount(root));
        
        //check that result text is updated correctly
        long deletedBytes = duplicateFileBytes;
        String deletedBytesStr = FileSieve.gui.util.Utilities.readableFileSize(deletedBytes);
        String deleteResultStr = "Deleted "+paths.length+" duplicate files ("+deletedBytesStr+").";
        assertTrue("Delete results text is correct", 
                controller.resultScreen.fileCntLabel.getText().equals(deleteResultStr));
        
        //check that file is actually deleted
        assertEquals("File was deleted successfully", filesInFolder-1, deletedFromFolder.listFiles().length);
        
    }
    
    @Test
    public void testSaveDiffReport(){       
        //stub duplicates
        String fileName = "file.dat";
        File match1 = new File(fileTestFolder + System.getProperty("file.separator")+
                "sourceFolder1"+System.getProperty("file.separator")+"file.dat");
        File match2 = new File(fileTestFolder + System.getProperty("file.separator")+
                "sourceFolder1"+System.getProperty("file.separator")+"folder1"+
                System.getProperty("file.separator")+"file.dat");
        List<File> matchList = new ArrayList<>();
        matchList.add(match1);
        matchList.add(match2);
        AbstractMap.SimpleImmutableEntry<String, List<File>> matchEntry
                  = new AbstractMap.SimpleImmutableEntry<>(fileName, matchList);

        List<AbstractMap.SimpleImmutableEntry<String, List<File>>> duplicates = new ArrayList<>();
        duplicates.add(matchEntry);
        
        //mock the fileChooser
        JFileChooser fileChooserMock = mock(JFileChooser.class);
        when(fileChooserMock.showOpenDialog(any(JFrame.class))).thenReturn(0);
        
        //setup target
        File testTarget = new File(fileTestFolder.toString());
        when(fileChooserMock.getSelectedFile()).thenReturn(testTarget);       
        controller.fileChooser = fileChooserMock;
        
        //check that report is saved      
        controller.duplicates = duplicates;
        controller.saveDiffReport();
        long savedReportSize = new File(testTarget.toString()+
                System.getProperty("file.separator")+"FileSieveDiffReport.html").length();
        assertTrue("Saved report should have positive filelength", savedReportSize > 0);
        
        //calling method when duplicates are not defined should cause NullPointerException
        controller.duplicates = null;
        thrown.expect(NullPointerException.class);
        controller.saveDiffReport();
    }
    
    /*
     * Constructs a folder structure with files and subfolders for exercising methods in FileEnumeration package
     */
    private void constructTestPaths() {
        /*
            Produces a folder structure for testing as follows. Files are 0-bytes in length unless noted. Three of the
            four files named "file.dat" have the same byte length and should be identified as duplicates of each other.

            <usersTempFolder>/FileSieveControllerTestFolder/
                sourceFolder1
                    folder1
                        folder1SubFolder1
                        folder1SubFolder2
                        file.dat            (text file containing text string "test")
                        folder1File1.dat
                        folder1File2.dat
                    folder2
                        folder2SubFolder1
                        folder2SubFolder2
                        file.dat            (text file containing test string "tes")
                        folder2File1.dat
                        folder2File1.dat
                    folder3
                        (empty)
                    file.dat                (text file containing text string "test")
                    file1.dat
                    file2.dat
                sourceFolder2
                    file3.dat
                file.dat                    (text file containing text string "test")
         */

        { // Create root folder within which test file/folder hierarchy will be built and create its immediate contents
            Path sourceFolder1 = fileTestFolder.resolve("sourceFolder1");
            Path sourceFolder2 = fileTestFolder.resolve("sourceFolder2");
            Path file = fileTestFolder.resolve("file.dat");

            try {
                Files.createDirectory(fileTestFolder);

                Files.createDirectory(sourceFolder1);
                Files.createDirectory(sourceFolder2);
                Files.write(file, "test".getBytes());
                expectedFilesWithRecursion++;
                expectedFileBytesWithRecursion+=Files.size(file);
                duplicateFileBytes=Files.size(file);

            } catch (IOException e) {
                fail("Unable to create root folders and file for tests");
            }
        }

        { // Create "sourceFolder1" folder and its immediate contents
            Path folder1 = fileTestFolder.resolve("sourceFolder1/folder1");
            Path folder2 = fileTestFolder.resolve("sourceFolder1/folder2");
            Path folder3 = fileTestFolder.resolve("sourceFolder1/folder3");
            Path file = fileTestFolder.resolve("sourceFolder1/file.dat");
            Path file1 = fileTestFolder.resolve("sourceFolder1/file1.dat");
            Path file2 = fileTestFolder.resolve("sourceFolder1/file2.dat");

            try {
                Files.createDirectory(folder1);
                Files.createDirectory(folder2);
                Files.createDirectory(folder3);
                Files.write(file, "test".getBytes());
                Files.createFile(file1);
                Files.createFile(file2);
                expectedFilesWithRecursion+=3;
                expectedFileBytesWithRecursion+=Files.size(file);

            } catch (IOException e) {
                fail("Unable to create \"file1.dat\" file within testFolder");
            }
        }

        { // Construct "folder1" subfolder files and folders
            Path folder1 = fileTestFolder.resolve("sourceFolder1/folder1");
            Path folder1SubFolder1 = folder1.resolve("folder1SubFolder1");
            Path folder1SubFolder2 = folder1.resolve("folder1SubFolder2");
            Path file = folder1.resolve("file.dat");
            Path folder1File1 = folder1.resolve("folder1File1.dat");
            Path folder1File2 = folder1.resolve("folder1File2.dat");

            try {
                Files.createDirectory(folder1SubFolder1);
                Files.createDirectory(folder1SubFolder2);
                Files.write(file, "test".getBytes());
                Files.createFile(folder1File1);
                Files.createFile(folder1File2);
                expectedFilesWithRecursion+=3;
                expectedFileBytesWithRecursion+=Files.size(file);

            } catch (IOException e) {
                fail("Unable to create items for \"folder1\" subfolder");
            }
        }

        { // Construct "folder2" subfolder files and folders
            Path folder2 = fileTestFolder.resolve("sourceFolder1/folder2");
            Path folder2SubFolder1 = folder2.resolve("folder2SubFolder1");
            Path folder2SubFolder2 = folder2.resolve("folder2SubFolder2");
            Path file = folder2.resolve("file.dat");
            Path folder2File1 = folder2.resolve("folder2File1.dat");
            Path folder2File2 = folder2.resolve("folder2File2.dat");

            try {
                Files.createDirectory(folder2SubFolder1);
                Files.createDirectory(folder2SubFolder2);
                Files.write(file, "tes".getBytes());
                Files.createFile(folder2File1);
                Files.createFile(folder2File2);
                expectedFilesWithRecursion+=3;
                expectedFileBytesWithRecursion+=Files.size(file);

            } catch (IOException e) {
                fail("Unable to create items for \"folder2\" subfolder");
            }
        }

        { // Create contents of "sourceFolder2" folder
            Path file3 = fileTestFolder.resolve("sourceFolder2/file3.dat");

            try {
                Files.createFile(file3);
                expectedFilesWithRecursion++;

            } catch (IOException e) {
                fail("Unable to create \"file3.dat\" file within \"sourceFolder2\" subfolder");
            }
        }
    }
}
