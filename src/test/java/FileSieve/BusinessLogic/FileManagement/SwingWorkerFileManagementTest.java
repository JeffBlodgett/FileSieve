package FileSieve.BusinessLogic.FileManagement;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import javax.swing.SwingWorker;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * JUnit testing of SwingWorkerFileManagement class
 */
public class SwingWorkerFileManagementTest {

    private FileManager<PathnameCopyWorker> fileManager;
    private String userTempFolder;
    private Path file;
    private Path folder;
    private Path subFolder;
    private Path anotherFolder;
    private static boolean deletePathnameTestsPassed = false;

    @Before
    public void setup() {
        fileManager = FileManagerFactory.getSwingWorkerFileManager();
        userTempFolder = System.getProperty("java.io.tmpdir");
        file = new File(userTempFolder + "swingWorkerFileManagementTestFile.txt").toPath();
        folder = new File(userTempFolder + "swingWorkerFileManagementTestFolder").toPath();
        subFolder = new File(folder + "/subFolderForSwingWorkerFileManagementTests").toPath();
        anotherFolder = new File(userTempFolder + "swingWorkerFileManagementTestFolder2").toPath();

        Assume.assumeTrue("temporary file and folder used for tests should not pre-exist", !Files.exists(file) && !Files.exists(folder));
    }

    @After
    public void cleanup() {
        try {
            Files.deleteIfExists(file);

            if (deletePathnameTestsPassed) {
                fileManager.deletePathname(folder);
                fileManager.deletePathname(anotherFolder);
            }
        } catch (SecurityException | IOException e) {
            // Ignore exceptions
        }
    }

    @Test
    public void testDeletePathname() {
        Path folderFile1 = new File(folder + "/testFile1.txt").toPath();
        Path folderFile2 = new File(folder + "/testFile2.txt").toPath();
        Path subFolderFile1 = new File(subFolder + "/testDeletePathname1.txt").toPath();
        Path subFolderFile2 = new File(subFolder + "/testDeletePathname2.txt").toPath();

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

        deletePathnameTestsPassed = true;
    }

    @Test
    public void testOpenPathname() {
        try {
            Files.createFile(file);
        } catch (IOException e) {
            Assert.fail(e.getClass().getSimpleName() + "while attempting to create temporary file for use in testing \"openPathname\" method");
        }

        try {
            ((BasicFileManager)fileManager).setDesktopOpenDisabled(true);   // Prevents file from being open in next statement
            fileManager.openPathname(file);
        } catch (UnsupportedOperationException e) {
            Assert.fail("current platform does not support the Desktop class, does not support the Desktop.Action.OPEN action, or is headless");
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
        if (deletePathnameTestsPassed == false) Assert.fail("testings of pathnameCopyProviders method depends on deletePathname testing, one or assertions for which failed");

        try {
            Files.createDirectory(folder);          // creates folder named "swingWorkerFileManagementTestFolder" in user's temp folder
        } catch (SecurityException | IOException e) {
            Assert.fail(e.getClass().getSimpleName() + "while attempting to create \"" + folder.toFile().getName() + "\" folder in temp folder");
        }
        Path fileToCopy = new File(folder + "/fileToCopy.txt").toPath();
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileToCopy.toFile()))) {
            int aByte = 0;
            for (long i = 0; i < 2000000L; i++) {
                bos.write(aByte);
            }
        } catch (IOException e) {
            Assert.fail("IOException while attempting to create temporary file for use in testing file copy operation");
        }

//        // ASSERTION SET 1: Copy a single file to a non-existing target folder (recursion enabled but isn't applicable in the case of a file as the sourcePathname)
//        PathnameCopyWorker worker1 = fileManager.copyProvider(fileToCopy, anotherFolder, true, false, false);
//        try {
//            worker1.execute();
//
//            // SwingWorker.get() method causes code execution in current thread to block until SwingWorker's state is "DONE"
//            Assert.assertTrue("copying of a file to a new folder, PathnameCopyWorker.get() method returns true", worker1.get());
//            Assert.assertTrue("copying of a file to a new folder, copy of file exists in targetPathname", Files.exists(new File(anotherFolder + "/fileToCopy.txt").toPath()));
//        } catch (InterruptedException e) {
//            Assert.fail("InterruptedException during PathnameCopyWorker execution, copying of a file to a new folder");
//        } catch (ExecutionException e) {
//            Assert.fail(e.getCause().getClass().getSimpleName() + " during PathnameCopyWorker execution, copying of a file to a new folder - " + e.getCause().getMessage());
//        }
//        try {
//            fileManager.deletePathname(anotherFolder);
//        } catch (IOException e) {
//            Assert.fail("IOException while deleting folder following assertion of non-recursive copying of a folder - " + anotherFolder);
//        }

        // ASSERTION SET 2: Copy a single folder to a non-existing target folder (recursion disabled)
        PathnameCopyWorker worker2 = fileManager.copyProvider(folder, anotherFolder, false, false, false);
        try {
            worker2.execute();
            Assert.assertTrue("non-recursive copying of a folder, PathnameCopyWorker.get() method returns true", worker2.get());
            for (String str : worker2.getCreatedPathnames().keySet()) {
                System.out.println(str);
            }
            System.out.println();

            int count = 0;
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(anotherFolder);) {
                for (Path path : dirStream) {
                    count++;
                }
            } catch (IOException e) {
                // Ignore exceptions
            }
            Path sourceFolderName = folder.getName(folder.getNameCount() - 1);
            Assert.assertTrue("non-recursive copying of a folder, created a single empty folder within target folder", (count == 1) && Files.isDirectory(anotherFolder.resolve(sourceFolderName)));

        } catch (InterruptedException e) {
            Assert.fail("InterruptedException during PathnameCopyWorker execution, non-recursive copying of a folder");
        } catch (ExecutionException e) {
            Assert.fail(e.getCause().getClass().getSimpleName() + " during PathnameCopyWorker execution, non-recursive copying of a folder - " + e.getCause().getMessage() );
        }
        try {
            fileManager.deletePathname(anotherFolder);
        } catch (IOException e) {
            Assert.fail("IOException while deleting folder following assertion of non-recursive copying of a folder: " + anotherFolder);
        }


        // ASSERTION SET 3: Copy the a folder, including its contents, to a target folder

        // ASSERTION SET 4: Repeat

        // Cleanup
        try {
            fileManager.deletePathname(folder);
            fileManager.deletePathname(anotherFolder);
        } catch (IOException e) {
            Assert.fail("IOException while attempting to delete temporary folder tree");
        }
    }

    @Test
    public void testGetPathnameCopyProviders() {
        // TODO test for getPathnameCopyProviders method
    }

} // class SwingWorkerFileManagementTest
