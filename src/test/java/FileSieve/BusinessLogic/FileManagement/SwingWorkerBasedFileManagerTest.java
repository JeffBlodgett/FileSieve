package FileSieve.BusinessLogic.FileManagement;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

/**
 * JUnit testing for the ConcurrentFileManager class
 */
public class SwingWorkerBasedFileManagerTest implements SwingCopyJobListener {

    private final SwingFileManager swingFileManager = FileManagerFactory.getSwingFileManager();
    private final String userTempFolder = System.getProperty("java.io.tmpdir");
    private final Path file = new File(userTempFolder + "swingWorkerFileManagementTestFile.txt").toPath();
    private final Path folder = new File(userTempFolder + "swingWorkerFileManagementTestFolder").toPath();
    private final Path subFolder = new File(folder + "/subFolderForSwingWorkerFileManagementTests").toPath();
    private final Path anotherFolder = new File(userTempFolder + "swingWorkerFileManagementTestFolder2").toPath();
    private static boolean deletePathnameTestsPassed = false;
    private final long TWO_MEGABYTES = 2097152;

    @Before
    public void setup() {
        Assume.assumeTrue("temporary file and folder used for tests should not pre-exist", !Files.exists(file) && !Files.exists(folder));
        swingFileManager.setCopyOperationsListener(this);
    }

    @After
    public void cleanup() {
        try {
            Files.deleteIfExists(file);

            if (deletePathnameTestsPassed) {
                swingFileManager.deletePathname(folder);
                swingFileManager.deletePathname(anotherFolder);
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
            Assert.assertFalse("Attempted deletion of non-existent file should return false", swingFileManager.deletePathname(file));
        } catch (SecurityException | IOException e) {
            Assert.fail(e.getClass().getSimpleName() + " while attempting to delete non-existent file \"" + file.toFile().getName() + "\" in temp folder");
        }
        try {
            Assert.assertFalse("Attempted deletion of non-existent folder should return false", swingFileManager.deletePathname(folder));
        } catch (SecurityException | IOException e) {
            Assert.fail(e.getClass().getSimpleName() + " while attempting to delete non-existent folder \"" + folder.toFile().getName() + "\" in temp folder");
        }

        try {
            Files.createFile(file);
        } catch (SecurityException | IOException e) {
            Assert.fail(e.getClass().getSimpleName() + " while attempting to create \"" + file.toFile().getName() + "\" file in temp folder");
        }
        try {
            Assert.assertTrue(swingFileManager.deletePathname(file));
        } catch (SecurityException | IOException e) {
            Assert.fail(e.getClass().getSimpleName() +  "while attempting to delete \"" + file.toFile().getName() + "\" file in temp folder");
        }

        try {
            Files.createDirectory(folder);
        } catch (SecurityException | IOException e) {
            Assert.fail(e.getClass().getSimpleName() + "while attempting to create \"" + folder.toFile().getName() + "\" folder in temp folder");
        }
        try {
            Assert.assertTrue(swingFileManager.deletePathname(folder));
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
            Assert.assertTrue(swingFileManager.deletePathname(folder));
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
            ((AbstractFileManager) swingFileManager).setDesktopOpenDisabled(true);   // Prevents file from being open in next statement
            swingFileManager.openPathname(file);
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
    public void testCopyPathname() {
        if (!deletePathnameTestsPassed) Assert.fail("testings of pathnameCopyProviders method depends on deletePathname testing, one or assertions for which failed");

        // Prepare a temp folder with files and folders to copy
        try {
            Files.createDirectory(folder);          // creates folder named "swingWorkerFileManagementTestFolder" in user's temp folder
        } catch (SecurityException | IOException e) {
            Assert.fail(e.getClass().getSimpleName() + "while attempting to create \"" + folder.toFile().getName() + "\" folder in temp folder");
        }
        Path fileToCopy = new File(folder + "/fileToCopy.dat").toPath();
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileToCopy.toFile()))) {
            int aByte = 0;
            for (long i = 0; i < TWO_MEGABYTES; i++) {
                bos.write(aByte);
            }
        } catch (IOException e) {
            Assert.fail("IOException while attempting to create temporary file #1 for use in testing file copy operations");
        }
        try {
            Files.createDirectory(subFolder);   // Create an empty subfolder under the source folder
        } catch (IOException e) {
            Assert.fail("IOException while attempting to create temporary file #2 for use in testing file copy operations");
        }
        Path anotherFileToCopy = new File(subFolder + "/anotherFileToCopy.dat").toPath();
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(anotherFileToCopy.toFile()))) {
            int aByte = 0;
            for (long i = 0; i < TWO_MEGABYTES; i++) {
                bos.write(aByte);
            }
        } catch (IOException e) {
            Assert.fail("IOException while attempting to create temporary file #2 for use in testing file copy operations");
        }

        // ASSERTION SET 1: Copy a single file to a non-existing target folder (recursion enabled but isn't applicable in the case of a file as the sourcePathname)
        SwingCopyJob swingCopyJob1 = null;
        try {
            swingCopyJob1 = swingFileManager.copyPathname(fileToCopy, anotherFolder, true, false, null);
            // IllegalStateException should not occur within this test
        } catch (IOException | IllegalStateException e) {
            // IOException not thrown by this FileManager implementation
        }
        if (swingCopyJob1 != null) {
            try {
                swingCopyJob1.awaitCompletion(); // rethrows exceptions from within SwingWorker internals
                //Thread.sleep(10000);
                try {
                    Assert.assertEquals("copying of a file to a new folder, created one pathname in target folder", 1, getChildCount(anotherFolder));
                } catch (IOException e) {
                    Assert.fail("IOException while getting count of child Path objects in folder \"" + anotherFolder + "\"");
                }

                Path fileNameToCheck = fileToCopy.getName(fileToCopy.getNameCount() - 1);
                Assert.assertTrue("copying of a file to a new folder, copy of file exists in targetPathname", Files.isRegularFile(new File(anotherFolder.resolve(fileNameToCheck).toString()).toPath()));
            } catch (InterruptedException e) {
                Assert.fail("InterruptedException during BackgroundCopyWorker execution, copying of a file to a new folder");
            } catch (ExecutionException e) {
                Assert.fail(e.getCause().getClass().getSimpleName() + " during BackgroundCopyWorker execution, copying of a file to a new folder - " + e.getCause().getMessage());
            }
            try {
                swingFileManager.deletePathname(anotherFolder);
            } catch (IOException e) {
                Assert.fail("IOException while deleting folder following assertion of non-recursive copying of a folder - " + anotherFolder);
            }
        } else {
            Assert.fail("copying of a file to a new folder, could not create copy job");
        }

        // ASSERTION SET 2: Copy folder contents to a non-existing target folder with folder recursion disabled
        SwingCopyJob swingCopyJob2 = null;
        try {
            swingCopyJob2 = swingFileManager.copyPathname(folder, anotherFolder, false, false, null);
            // IllegalStateException should not occur within this test
        } catch (IOException e) {
            // Ignore exception - not thrown by this FileManager implementation
        }
        if (swingCopyJob2 != null) {
            try {
                swingCopyJob2.awaitCompletion();

                try {
                    Assert.assertEquals("non recursive copying of a folder, created two pathnames in target folder", 2, getChildCount(anotherFolder));
                } catch (IOException e) {
                    Assert.fail("IOException while getting count of child Path objects in folder \"" + anotherFolder + "\"");
                }

                Path fileNameToCheck = fileToCopy.getName(fileToCopy.getNameCount() - 1);
                Assert.assertTrue("non-recursive copying of a folder, copied a file into the target folder", Files.isRegularFile(anotherFolder.resolve(fileNameToCheck), LinkOption.NOFOLLOW_LINKS));

                Path folderNameToCheck = subFolder.getName(subFolder.getNameCount() - 1);
                Assert.assertTrue("non-recursive copying of a folder, created an empty folder within target folder", Files.isDirectory(anotherFolder.resolve(folderNameToCheck)));
            } catch (InterruptedException e) {
                Assert.fail("InterruptedException during BackgroundCopyWorker execution, non-recursive copying of a folder");
            } catch (ExecutionException e) {
                Assert.fail(e.getCause().getClass().getSimpleName() + " during BackgroundCopyWorker execution, non-recursive copying of a folder - " + e.getCause().getMessage());
            }
            try {
                swingFileManager.deletePathname(anotherFolder);
            } catch (IOException e) {
                Assert.fail("IOException while deleting folder following assertion of non-recursive copying of a folder: " + anotherFolder);
            }
        } else {
            Assert.fail("non-recursive copying of a folder, could not create copy job");
        }

        // ASSERTION SET 3: Copy a folder, with the contents of all subfolders, to a target folder (folder recursion enabled)
        SwingCopyJob swingCopyJob3 = null;
        try {
            swingCopyJob3 = swingFileManager.copyPathname(folder, anotherFolder, true, false, null);
            // IllegalStateException should not occur within this test
        } catch (IOException e) {
            // Ignore exception - not thrown by this FileManager implementation
        }
        if (swingCopyJob3 != null) {
            try {
                swingCopyJob3.awaitCompletion();

                try {
                    Assert.assertEquals("recursive copying of a folder, created two pathnames in target folder", 3, getChildCount(anotherFolder));
                } catch (IOException e) {
                    Assert.fail("IOException while getting count of child Path objects in folder \"" + anotherFolder + "\"");
                }

                Path fileNameToCheck = fileToCopy.getName(fileToCopy.getNameCount() - 1);
                Assert.assertTrue("recursive copying of a folder, copied a file into the target folder", Files.isRegularFile(anotherFolder.resolve(fileNameToCheck), LinkOption.NOFOLLOW_LINKS));

                Path folderNameToCheck = subFolder.getName(subFolder.getNameCount() - 1);
                Path folderPathToCheck = anotherFolder.resolve(folderNameToCheck);
                Assert.assertTrue("recursive copying of a folder, created a folder within target folder", Files.isDirectory(folderPathToCheck));

                fileNameToCheck = anotherFileToCopy.getName(anotherFileToCopy.getNameCount() - 1);
                Assert.assertTrue("recursive copying of a folder, copied a file into a folder within the target folder", Files.isRegularFile(folderPathToCheck.resolve(fileNameToCheck), LinkOption.NOFOLLOW_LINKS));
            } catch (InterruptedException e) {
                Assert.fail("InterruptedException during BackgroundCopyWorker execution, recursive copying of a folder");
            } catch (ExecutionException e) {
                Assert.fail(e.getCause().getClass().getSimpleName() + " during BackgroundCopyWorker execution, recursive copying of a folder - " + e.getCause().getMessage());
            }
        } else {
            Assert.fail("recursive copying of a folder, could not create copy job");
        }

        Assert.assertEquals("tracked copy jobs have been been removed from class' internal map", 0, SwingCopyJob.swingCopyJobs.size());

        // Cleanup
        try {
            swingFileManager.deletePathname(folder);
            swingFileManager.deletePathname(anotherFolder);
        } catch (IOException e) {
            Assert.fail("IOException while attempting to delete temporary folder tree");
        }
    }

    /**
     * Helper method for counting the number of subfolders and files within a given pathname
     *
     * @param pathname  pathname (folder or file) to search
     * @return          number of files and subfolders found within the given pathname
     */
    private int getChildCount(Path pathname) throws IOException {
        int count = 0;

        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(pathname)) {
            for (Path path : dirStream) {
                if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    count++;
                } else if (Files.isDirectory(path)) {
                    count += 1 + getChildCount(path);
                }
            }
        }

        return count;
    }

    @Override
    public void UpdateCopyJobProgress(SwingCopyJob swingCopyJob, int percentProgressed) {
        //System.out.println(copyJob.getDestinationFolder() + "    " + percentProgressed + "%");
    }

    @Override
    public void UpdatePathnameCopyProgress(SwingCopyJob swingCopyJob, Path pathnameBeingCopied, int percentProgressed) {
        //System.out.println(copyJob.getDestinationFolder() + "    " + pathnameBeingCopied + "    " + percentProgressed + "%");
    }

    @Override
    public void InternalCopyJobException(SwingCopyJob swingCopyJob, Throwable throwable) {

    }

} // class ConcurrentFileManagerTest
