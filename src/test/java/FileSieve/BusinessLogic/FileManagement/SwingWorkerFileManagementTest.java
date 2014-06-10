package FileSieve.BusinessLogic.FileManagement;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import javax.swing.SwingWorker;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JUnit testing of SwingWorkerFileManagement class
 */
public class SwingWorkerFileManagementTest {

    private FileManager<SwingWorker<Void, Integer>> fileManager;
    private String userTempFolder;
    private Path file;
    private Path folder;

    @Before
    public void setup() {
        fileManager = new SwingWorkerFileManagement();
        userTempFolder = System.getProperty("java.io.tmpdir");
        file = new File(userTempFolder + "testDeletePathname.txt").toPath();
        folder = new File(userTempFolder + "testDeletePathnameFolder").toPath();

        Assume.assumeTrue("temporary file and folder used for tests should not pre-exist", !Files.exists(file) && !Files.exists(folder));
    }

    @After
    public void cleanup() {
        try {
            Files.deleteIfExists(file);
            fileManager.deletePathname(folder); // May not work, but better than nothing
        } catch (SecurityException | IOException e) {
            // Ignore exceptions
        }
    }

    @Test
    public void testDeletePathname() {
        Path folderFile1 = new File(userTempFolder + "testDeletePathnameFolder" + "/testDeletePathname1.txt").toPath();
        Path folderFile2 = new File(userTempFolder + "testDeletePathnameFolder" + "/testDeletePathname2.txt").toPath();
        Path subFolder = new File(userTempFolder + "testDeletePathnameFolder" + "/testDeletePathnameSubFolder").toPath();
        Path subFolderFile1 = new File(userTempFolder + "testDeletePathnameFolder" + "/testDeletePathnameSubFolder/testDeletePathname1.txt").toPath();
        Path subFolderFile2 = new File(userTempFolder + "testDeletePathnameFolder" + "/testDeletePathnameSubFolder/testDeletePathname2.txt").toPath();

        try {
            Assert.assertFalse("Attempted deletion of non-existent file should return false", fileManager.deletePathname(file));
        } catch (SecurityException | IOException e) {
            Assert.fail(e.getClass().getSimpleName() + " while attempting to delete non-existent file \"" + file.toFile().getName() + "\" in temp folder");
        }
        try {
            Assert.assertFalse("Attempted deletion of non-existent folder should return false", fileManager.deletePathname(folder));
        } catch (SecurityException | IOException e) {
            Assert.fail(e.getClass().getSimpleName() + " while attempting to delete non-existent folder \"" + folder.toFile().getName() + "\" in temp folder");
        }

        try {
            Files.createFile(file);
        } catch (SecurityException | IOException e) {
            Assert.fail(e.getClass().getSimpleName() + " while attempting to create \"" + file.toFile().getName() + "\" file in temp folder");
        }
        try {
            Assert.assertTrue(fileManager.deletePathname(file));
        } catch (SecurityException | IOException e) {
            Assert.fail(e.getClass().getSimpleName() +  "while attempting to delete \"" + file.toFile().getName() + "\" file in temp folder");
        }

        try {
            Files.createDirectory(folder);
        } catch (SecurityException | IOException e) {
            Assert.fail(e.getClass().getSimpleName() + "while attempting to create \"" + folder.toFile().getName() + "\" folder in temp folder");
        }
        try {
            Assert.assertTrue(fileManager.deletePathname(folder));
        } catch (SecurityException | IOException e) {
            Assert.fail(e.getClass().getSimpleName() + "while attempting to delete folder hierarchy in temp folder");
        }

        try {
            Files.createDirectory(folder);
            Files.createFile(folderFile1);
            Files.createFile(folderFile2);
            Files.createDirectory(subFolder);
            Files.createFile(subFolderFile1);
            Files.createFile(subFolderFile2);
        } catch (SecurityException | IOException e) {
            Assert.fail(e.getClass().getSimpleName() + "while attempting to create a folder hierarchy in the temp folder");
        }
        try {
            Assert.assertTrue(fileManager.deletePathname(folder));
        } catch (SecurityException | IOException e) {
            Assert.fail(e.getClass().getSimpleName() + "while attempting to delete a folder hierarchy in the temp folder");
        }
    }

    @Test
    public void testOpenPathname() {
        try {
            Files.createFile(file);
        } catch (IOException e) {
            Assert.fail(e.getClass().getSimpleName() + "while attempting to create temporary folder for use in testing \"openPathname\" method");
        }

        try {
            fileManager.openPathname(file);
        } catch (UnsupportedOperationException e) {
            Assert.fail("current platform does not support the Desktop class or does not support the Desktop.Action.OPEN action");
        } catch (SecurityException e) {
            Assert.fail("insufficient access permissions");
        } catch (IOException e) {
            Assert.fail("the specified file has no associated application or the associated application failed to be launched");
        }

        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            Assert.fail("Unable to delete temporary file used for testing \"openPathname\" method");
        }
    }

    @Test
    public void testPathnameCopyProvider() {
        // TODO test for pathnameCopyProvider method
    }

    @Test
    public void testGetPathnameCopyProviders() {
        // TODO test for getPathnameCopyProviders method
    }

} // class SwingWorkerFileManagementTest
