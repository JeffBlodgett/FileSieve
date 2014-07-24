package FileSieve.BusinessLogic.FileManagement;

import FileSieve.BusinessLogic.FileEnumeration.DiscoveredPath;
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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * JUnit testing for the ConcurrentFileManager class
 */
public class SwingWorkerBasedFileManagerTest implements SwingCopyJobListener {

    private final SwingFileManager swingFileManager = FileManagerFactory.getSwingFileManager();
    private final String userTempFolder = System.getProperty("java.io.tmpdir");
    private final Path fileManagementTestFolder = new File(userTempFolder + "FileManagementTestFolder").toPath();
    private final Set<Path> pathnames = new LinkedHashSet<Path>(30);
    private static boolean deletePathnameTestsPassed = false;
    private int pathsCopied = 0;
    private Object lockObject = new Object();

    @Before
    public void setup() throws IOException {
        Assume.assumeTrue("temporary folder used for tests should not pre-exist", !Files.exists(fileManagementTestFolder));

        /* Set "this" test class as the copy job listener to receive notifications from jobs created using the
           SwingFileManager. */
        swingFileManager.setCopyOperationsListener(this);

        // Construct folders and files to be used for testing
        constructTestPaths();
    }

    @After
    public void cleanup() throws IOException {
        if (deletePathnameTestsPassed) {
            swingFileManager.deletePathname(fileManagementTestFolder);
        }
    }

    @Test
    public void testDeletePathname() throws IOException {
        Path nonExistentFile = fileManagementTestFolder.resolve("nonExistentFile.txt");
        Path nonExistentFolder = fileManagementTestFolder.resolve("nonExistentFolder");

        Assert.assertFalse("Attempted deletion of non-existent file should return false", swingFileManager.deletePathname(nonExistentFile));
        Assert.assertFalse("Attempted deletion of non-existent folder should return false", swingFileManager.deletePathname(nonExistentFolder));

        /* Contents of "deletionsTestFolder" folder for testing deletePathname method:

               deletionsTestFolder
                   deletionsFolder1
                   deletionsFolder2
                       deletionsFile1.txt
                   deletionsFolder3
                       deletionsFolder4
                           deletionsFile2.txt
                       deletionsFile3.txt
                   deletionsFile4.txt
         */

        Assert.assertTrue("able to delete a file", swingFileManager.deletePathname(fileManagementTestFolder.resolve("deletionsTestFolder/deletionsFile4.txt")));
        Assert.assertTrue("able to delete an empty folder", swingFileManager.deletePathname(fileManagementTestFolder.resolve("deletionsTestFolder/deletionsFolder1")));
        Assert.assertTrue("able to delete a folder with one file and no subfolders", swingFileManager.deletePathname(fileManagementTestFolder.resolve("deletionsTestFolder/deletionsFolder2")));
        Assert.assertTrue("able to delete a folder with files/subfolders", swingFileManager.deletePathname(fileManagementTestFolder.resolve("deletionsTestFolder/deletionsFolder3")));

        deletePathnameTestsPassed = true;
    }

    @Test
    public void testOpenPathname() throws IOException {
        Path openPathnameTestFile = fileManagementTestFolder.resolve("openPathnameTestFile.txt");

        // Prevent file from being opened in next statement
        ((AbstractFileManager)swingFileManager).setDesktopOpenDisabled(true);
        try {
            swingFileManager.openPathname(openPathnameTestFile);
        } catch (UnsupportedOperationException e) {
            Assert.fail("current platform is headless, does not support the Desktop class, or does not support the Desktop.Action.OPEN action");
        }
    }

    @Test
    public void testCopyPathname_SingleFile() throws IOException {
        if (!deletePathnameTestsPassed) {
            Assert.fail("testing of copyPathname method depends on deletePathname testing, one or assertions for which failed");
        }

        Path fileToCopy = fileManagementTestFolder.resolve("file4.dat");
        Path targetFolder = fileManagementTestFolder.resolve("targetFolder");

        // Copy a single file to a non-existing target folder (recursion enabled but isn't applicable in the case of a file as the sourcePathname)
        SwingCopyJob swingCopyJob = swingFileManager.copyPathname(fileToCopy, targetFolder, true, false, null);
        if (swingCopyJob != null) {
            try {
                // exceptions that occur on internal SwingWorker's background thread are rethrown by this method
                swingCopyJob.awaitCompletion();

                Assert.assertEquals("copying of a file to a new folder, created one pathname in target folder", 1, getChildCount(targetFolder));

                Path fileForWhichToCheckExistence = targetFolder.resolve(fileToCopy.getFileName());
                Assert.assertTrue("copying of a file to a new folder, copy of file exists in targetPathname", Files.isRegularFile(fileForWhichToCheckExistence, LinkOption.NOFOLLOW_LINKS));

            } catch (InterruptedException e) {
                Assert.fail("InterruptedException during BackgroundCopyWorker execution, copying of a file to a new folder");

            } catch (ExecutionException e) {
                Assert.fail(e.getCause().getClass().getSimpleName() + " during BackgroundCopyWorker execution, copying of a file to a new folder: Message: " + e.getCause().getMessage());

            } finally {
                if (!swingFileManager.deletePathname(targetFolder)) {
                    Assert.fail("unable to delete \"targetFolder\" folder following single-file copy test");
                }
            }
        } else {
            Assert.fail("Unable to create or retrieve a SwingCopyJob instance");
        }

        Assert.assertEquals("tracked copy jobs have been been removed from SwingCopyJob class' internal map", 0, SwingCopyJob.swingCopyJobs.size());
    }

    @Test
    public void testCopyPathname_SingleFolder_NoRecursion() throws IOException {
        if (!deletePathnameTestsPassed) {
            Assert.fail("testing of copyPathname method depends on deletePathname testing, one or assertions for which failed");
        }

        Path folderToCopy = fileManagementTestFolder.resolve("sourceFolder1");
        Path targetFolder = fileManagementTestFolder.resolve("targetFolder");

        // Copy folder contents to a non-existing target folder with folder recursion disabled
        SwingCopyJob swingCopyJob = swingFileManager.copyPathname(folderToCopy, targetFolder, false, false, null);
        if (swingCopyJob != null) {
            try {
                // exceptions that occur on internal SwingWorker's background thread are rethrown by this method
                swingCopyJob.awaitCompletion();

                Assert.assertEquals("non recursive copying of a folder created 1 folder in the target folder", 1, getChildCount(targetFolder));

                Path aFolderForWhichToCheckExistence = targetFolder.resolve(folderToCopy.getFileName());
                Assert.assertTrue("non-recursive copying of a folder, created folder in the target folder", Files.isDirectory(aFolderForWhichToCheckExistence, LinkOption.NOFOLLOW_LINKS));

            } catch (InterruptedException e) {
                Assert.fail("InterruptedException during BackgroundCopyWorker execution, non-recursive copying of a folder");

            } catch (ExecutionException e) {
                Assert.fail(e.getCause().getClass().getSimpleName() + " during BackgroundCopyWorker execution, non-recursive copying of a folder. Message: " + e.getCause().getMessage());

            } finally {
                if (!swingFileManager.deletePathname(targetFolder)) {
                    Assert.fail("unable to delete \"targetFolder\" folder following non-recursive folder copy test");
                }
            }
        } else {
            Assert.fail("Unable to create or retrieve a SwingCopyJob instance");
        }

        Assert.assertEquals("tracked copy job has been been removed from SwingCopyJob class' internal map", 0, SwingCopyJob.swingCopyJobs.size());
    }

    @Test
    public void testCopyPathname_SingleFolder_WithRecursion() throws IOException {
        if (!deletePathnameTestsPassed) {
            Assert.fail("testing of copyPathname method depends on deletePathname testing, one or assertions for which failed");
        }

        Path folderToCopy = fileManagementTestFolder.resolve("sourceFolder1");
        Path targetFolder = fileManagementTestFolder.resolve("targetFolder");

        // Copy a folder, with the contents of all subfolders (recursion option = true), to a non-existent target folder
        SwingCopyJob swingCopyJob = swingFileManager.copyPathname(folderToCopy, targetFolder, true, false, null);
        if (swingCopyJob != null) {
            try {
                // exceptions that occur on internal SwingWorker's background thread are rethrown by this method
                swingCopyJob.awaitCompletion();

                Assert.assertEquals("recursive copying of a folder created 17 files/folders in target folder", 17, getChildCount(targetFolder));

                Path aFileForWhichToCheckExistence = targetFolder.resolve(folderToCopy.getFileName().resolve("folder2/folder2File1.dat"));
                Assert.assertTrue("recursive copying of a folder, copied a file into the target folder", Files.isRegularFile(targetFolder.resolve(aFileForWhichToCheckExistence), LinkOption.NOFOLLOW_LINKS));

                Path aFolderForWhichToCheckExistence = targetFolder.resolve(folderToCopy.getFileName().resolve("folder2/folder2SubFolder2"));
                Assert.assertTrue("recursive copying of a folder created a folder within a subfolder of the target folder", Files.isDirectory(aFolderForWhichToCheckExistence));

            } catch (InterruptedException e) {
                Assert.fail("InterruptedException during BackgroundCopyWorker execution, recursive copying of a folder");

            } catch (ExecutionException e) {
                Assert.fail(e.getCause().getClass().getSimpleName() + " during BackgroundCopyWorker execution, recursive copying of a folder. Message: " + e.getCause().getMessage());

            } finally {
                if (!swingFileManager.deletePathname(targetFolder)) {
                    Assert.fail("unable to delete \"targetFolder\" folder following recursive folder copy test");
                }
            }
        } else {
            Assert.fail("Unable to create or retrieve a SwingCopyJob instance");
        }

        Assert.assertEquals("tracked copy jobs have been been removed from SwingCopyJob class' internal map", 0, SwingCopyJob.swingCopyJobs.size());
    }

    @Test
    public void testCopyPathnames_MultipleFoldersAndFiles_WithRecursion() throws IOException {
        if (!deletePathnameTestsPassed) {
            Assert.fail("testing of copyPathname method depends on deletePathname testing, one or assertions for which failed");
        }

        Set<Path> pathsToCopy = new LinkedHashSet<>(5);
        pathsToCopy.add(fileManagementTestFolder.resolve("deletionsTestFolder"));
        pathsToCopy.add(fileManagementTestFolder.resolve("sourceFolder1"));
        pathsToCopy.add(fileManagementTestFolder.resolve("sourceFolder2"));
        pathsToCopy.add(fileManagementTestFolder.resolve("file4.dat"));
        pathsToCopy.add(fileManagementTestFolder.resolve("openPathnameTestFile.txt"));

        Path targetFolder = fileManagementTestFolder.resolve("targetFolder");

        // Copy a folder, with the contents of all subfolders (recursion option = true), to a non-existent target folder
        SwingCopyJob swingCopyJob = swingFileManager.copyPathnames(pathsToCopy, targetFolder, true, false, null);
        if (swingCopyJob != null) {
            try {
                // exceptions that occur on internal SwingWorker's background thread are rethrown by this method
                swingCopyJob.awaitCompletion();

                Assert.assertEquals("recursive copying of a folder created 30 files/folders in target folder", 30, getChildCount(targetFolder));

                Path aFileForWhichToCheckExistence = targetFolder.resolve("sourceFolder1/folder2/folder2File1.dat");
                Assert.assertTrue("recursive copying of a folder, copied a file into the target folder", Files.isRegularFile(targetFolder.resolve(aFileForWhichToCheckExistence), LinkOption.NOFOLLOW_LINKS));

                Path aFolderForWhichToCheckExistence = targetFolder.resolve("sourceFolder1/folder2/folder2SubFolder2");
                Assert.assertTrue("recursive copying of a folder created a folder within a subfolder of the target folder", Files.isDirectory(aFolderForWhichToCheckExistence));

            } catch (InterruptedException e) {
                Assert.fail("InterruptedException during BackgroundCopyWorker execution, recursive copying of a folder");

            } catch (ExecutionException e) {
                Assert.fail(e.getCause().getClass().getSimpleName() + " during BackgroundCopyWorker execution, recursive copying of a folder. Message: " + e.getCause().getMessage());
            }
        } else {
            Assert.fail("Unable to create or retrieve a SwingCopyJob instance");
        }

        /* Repeat the above, but enable overwriting of existing files (if different) and make a file size change to the
           "folder2File1.dat" source file. It should be the only file that gets overwritten. */
        Path folder2File1 = fileManagementTestFolder.resolve("sourceFolder1/folder2/folder2File1.dat");
        Files.write(folder2File1, "change it up".getBytes());
        int newByteSizeOfFile = "change it up".getBytes().length;

        /* Copy a folder, with the contents of all subfolders (recursion option = true), to an existing target folder
           with the overwrite files options set to true */
        swingCopyJob = swingFileManager.copyPathnames(pathsToCopy, targetFolder, true, true, null);

        if (swingCopyJob != null) {
            try {
                // exceptions that occur on internal SwingWorker's background thread are rethrown by this method
                swingCopyJob.awaitCompletion();

                Assert.assertEquals("recursive copying of a folder created 30 files/folders in target folder", 30, getChildCount(targetFolder));

                Path aFileForWhichToCheckExistence = targetFolder.resolve("sourceFolder1/folder2/folder2File1.dat");
                Assert.assertTrue("recursive copying of a folder, copied a file into the target folder", Files.isRegularFile(targetFolder.resolve(aFileForWhichToCheckExistence), LinkOption.NOFOLLOW_LINKS));

                Path aFolderForWhichToCheckExistence = targetFolder.resolve("sourceFolder1/folder2/folder2SubFolder2");
                Assert.assertTrue("recursive copying of a folder created a folder within a subfolder of the target folder", Files.isDirectory(aFolderForWhichToCheckExistence));

                Assert.assertEquals("changed file was overwritten in target folder", newByteSizeOfFile, targetFolder.resolve("sourceFolder1/folder2/folder2File1.dat").toFile().length());

            } catch (InterruptedException e) {
                Assert.fail("InterruptedException during BackgroundCopyWorker execution, recursive copying of a folder");

            } catch (ExecutionException e) {
                Assert.fail(e.getCause().getClass().getSimpleName() + " during BackgroundCopyWorker execution, recursive copying of a folder: Message: " + e.getCause().getMessage());

            } finally {
                if (!swingFileManager.deletePathname(targetFolder)) {
                    Assert.fail("unable to delete \"targetFolder\" folder following recursive folder copy test");
                }
            }
        } else {
            Assert.fail("Unable to create or retrieve a SwingCopyJob instance");
        }

        Assert.assertEquals("tracked copy jobs have been been removed from SwingCopyJob class' internal map", 0, SwingCopyJob.swingCopyJobs.size());
    }

    @Test
    public void testCopyPathnames_MultipleFoldersAndFiles_NoRecursion() throws IOException {
        if (!deletePathnameTestsPassed) {
            Assert.fail("testing of copyPathname method depends on deletePathname testing, one or assertions for which failed");
        }

        Path targetFolder = fileManagementTestFolder.resolve("targetFolder");

        /* Simulate the output of a FileEnumerator in which the "FileManagementTestFolder" has been recursively searched
           for paths (folders and files) */
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("deletionsTestFolder"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("sourceFolder1"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("sourceFolder2"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("file4.dat"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("openPathnameTestFile.txt"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("deletionsTestFolder/deletionsFolder1"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("deletionsTestFolder/deletionsFolder2"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("deletionsTestFolder/deletionsFolder3"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("deletionsTestFolder/deletionsFile4.txt"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("deletionsTestFolder/deletionsFolder2/deletionsFile1.txt"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("deletionsTestFolder/deletionsFolder3/deletionsFolder4"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("deletionsTestFolder/deletionsFolder3/deletionsFile3.txt"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("deletionsTestFolder/deletionsFolder3/deletionsFolder4/deletionsFile2.txt"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("sourceFolder1/folder1"),fileManagementTestFolder ));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("sourceFolder1/folder2"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("sourceFolder1/folder3"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("sourceFolder1/file.dat"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("sourceFolder1/file1.dat"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("sourceFolder1/file2.dat"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("sourceFolder1/folder1/folder1SubFolder1"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("sourceFolder1/folder1/folder1SubFolder2"),fileManagementTestFolder ));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("sourceFolder1/folder1/file.dat"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("sourceFolder1/folder1/folder1File1.dat"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("sourceFolder1/folder1/folder1File2.dat"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("sourceFolder1/folder2/folder2SubFolder1"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("sourceFolder1/folder2/folder2SubFolder2"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("sourceFolder1/folder2/file.dat"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("sourceFolder1/folder2/folder2File1.dat"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("sourceFolder1/folder2/folder2File2.dat"), fileManagementTestFolder));
        pathnames.add(new DiscoveredPath(fileManagementTestFolder.resolve("sourceFolder2/file3.dat"), fileManagementTestFolder));

        /* Copy a series (Set) of paths, as they would be output by FileEnumeration, to an existing target folder
           with the overwrite files options set to true */
        SwingCopyJob swingCopyJob = swingFileManager.copyPathnames(pathnames, targetFolder, false, false, null);
        if (swingCopyJob != null) {
            try {
                // exceptions that occur on internal SwingWorker's background thread are rethrown by this method
                swingCopyJob.awaitCompletion();

                Assert.assertEquals("31 paths created in the target folder", 31, getChildCount(targetFolder));

                Path aFileForWhichToCheckExistence = targetFolder.resolve("FileManagementTestFolder/sourceFolder1/folder2/folder2File1.dat");
                Assert.assertTrue("copied a particular file into the target folder", Files.isRegularFile(targetFolder.resolve(aFileForWhichToCheckExistence), LinkOption.NOFOLLOW_LINKS));

                Path aFolderForWhichToCheckExistence = targetFolder.resolve("FileManagementTestFolder/sourceFolder1/folder2/folder2SubFolder2");
                Assert.assertTrue("created a particular folder within a subfolder of the target folder", Files.isDirectory(aFolderForWhichToCheckExistence));

            } catch (InterruptedException e) {
                Assert.fail("InterruptedException during BackgroundCopyWorker execution");

            } catch (ExecutionException e) {
                Assert.fail(e.getCause().getClass().getSimpleName() + " during BackgroundCopyWorker execution: Message: " + e.getCause().getMessage());

            } finally {
                if (!swingFileManager.deletePathname(targetFolder)) {
                    Assert.fail("unable to delete \"targetFolder\" folder");
                }
            }
        } else {
            Assert.fail("Unable to create or retrieve a SwingCopyJob instance");
        }

        Assert.assertEquals("tracked copy jobs have been been removed from SwingCopyJob class' internal map", 0, SwingCopyJob.swingCopyJobs.size());
    }

    @Test
    public void testCopyPathnames_Cancel() throws IOException {
        if (!deletePathnameTestsPassed) {
            Assert.fail("testing of copyPathname method depends on deletePathname testing, one or assertions for which failed");
        }

        Set<Path> pathsToCopy = new LinkedHashSet<>(5);
        pathsToCopy.add(fileManagementTestFolder.resolve("deletionsTestFolder"));
        pathsToCopy.add(fileManagementTestFolder.resolve("sourceFolder1"));
        pathsToCopy.add(fileManagementTestFolder.resolve("sourceFolder2"));
        pathsToCopy.add(fileManagementTestFolder.resolve("file4.dat"));
        pathsToCopy.add(fileManagementTestFolder.resolve("openPathnameTestFile.txt"));

        Path targetFolder = fileManagementTestFolder.resolve("targetFolder");

        pathsCopied = 0;

        // Copy a folder, with the contents of all subfolders (recursion option = true), to a non-existent target folder
        SwingCopyJob swingCopyJob = swingFileManager.copyPathnames(pathsToCopy, targetFolder, true, false, null);
        if (swingCopyJob != null) {
            try {
                // Let the copy job proceed until at least 2 paths have been copied or 50ms have passed before cancelling it
                while(pathsCopied <= 1) {
                    synchronized(lockObject) {
                        lockObject.wait(50);
                    }
                }
                Assert.assertTrue("job was successfully issued a cancel request", swingCopyJob.cancelJob());

                // exceptions that occur on internal SwingWorker's background thread are rethrown by this method
                swingCopyJob.awaitCompletion();

                int pathsCreated = getChildCount(targetFolder);
                Assert.assertTrue("cancelling of the recursive copying of a pathname created at least 1 but less than 30 files/folders in the target folder due to job cancellation", (pathsCreated > 1) && (pathsCreated < 30));

            } catch (InterruptedException e) {
                Assert.fail("InterruptedException during BackgroundCopyWorker execution, cancelling of the recursive copying of a pathname");

            } catch (ExecutionException e) {
                Assert.fail(e.getCause().getClass().getSimpleName() + " during BackgroundCopyWorker execution, cancelling of the recursive copying of a pathname: Message: " + e.getCause().getMessage());

            } finally {
                if (!swingFileManager.deletePathname(targetFolder)) {
                    Assert.fail("unable to delete \"targetFolder\" folder following cancellation of recursive copying of a pathname");
                }
            }
        } else {
            Assert.fail("Unable to create or retrieve a SwingCopyJob instance");
        }

        Assert.assertEquals("tracked copy jobs have been been removed from SwingCopyJob class' internal map", 0, SwingCopyJob.swingCopyJobs.size());
    }

    @Test
    public void copyPathname_PathsExceeding260Characters() throws IOException, ExecutionException, InterruptedException {
        Path aLongPath = new File("abcdefghijklmnopqrstuvwxyz1/abcdefghijklmnopqrstuvwxyz2/abcdefghijklmnopqrstuvwxyz3/abcdefghijklmnopqrstuvwxyz4/abcdefghijklmnopqrstuvwxyz5/abcdefghijklmnopqrstuvwxyz6/abcdefghijklmnopqrstuvwxyz7/abcdefghijklmnopqrstuvwxyz8/abcdefghijklmnopqrstuvwxyz9/abcdefghijklmnopqrstuvwxyz10/abcdefghijklmnopqrstuvwxyz11").toPath();

        Path sourcePath = fileManagementTestFolder.resolve("longPaths");
        Path targetFolder = fileManagementTestFolder.resolve("targetFolder");

        Path pathInWhichToSpawnTestFiles = sourcePath.resolve(aLongPath);

        // Define paths to create under sourcePath folder for testing
        Path file1 = pathInWhichToSpawnTestFiles.resolve("file1.txt");
        Path folder1 = pathInWhichToSpawnTestFiles.resolve("folder1");
        Path folder2 = pathInWhichToSpawnTestFiles.resolve("folder2");
        Path folder1SubFolder1 = folder1.resolve("folder1SubFolder1");
        Path folder1SubFolder2 = folder1.resolve("folder1SubFolder2");
        Path folder1File1 = folder1.resolve("folder1File1.txt");
        Path folder1File2 = folder1.resolve("folder1File2.txt");
        Path folder1SubFolder1Folder1 = folder1SubFolder1.resolve("folder1SubFolder1Folder1");
        Path folder1SubFolder1File1 = folder1SubFolder1.resolve("folder1SubFolder1File1");

        // Create folders
        pathInWhichToSpawnTestFiles.toFile().mkdirs();
        folder1.toFile().mkdir();
        folder2.toFile().mkdir();
        folder1SubFolder1.toFile().mkdir();
        folder1SubFolder2.toFile().mkdir();
        folder1SubFolder1Folder1.toFile().mkdir();

        // Create files
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file1.toFile()))) { bos.write("file1".getBytes()); }
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(folder1File1.toFile()))) { bos.write("folder1File1".getBytes()); }
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(folder1File2.toFile()))) { bos.write("folder1File2".getBytes()); }
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(folder1SubFolder1File1.toFile()))) { bos.write("folder1SubFolder1File1".getBytes()); }

        SwingCopyJob swingCopyJob = swingFileManager.copyPathname(sourcePath, targetFolder, true, false, null);
        if (swingCopyJob != null) {
            try {
                // exceptions that occur on internal SwingWorker's background thread are rethrown by this method
                swingCopyJob.awaitCompletion();

                Assert.assertEquals("21 files/folders were created under the destination folder", 21, getChildCount(targetFolder));
            } finally {
                if (!swingFileManager.deletePathname(targetFolder)) {
                    Assert.fail("unable to delete destination folder");
                }
            }
        } else {
            Assert.fail("Unable to create or retrieve a SwingCopyJob instance");
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

        if (Files.exists(pathname, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(pathname)) {
                for (Path path : dirStream) {
                    if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                        count++;
                    } else if (Files.isDirectory(path)) {
                        count += 1 + getChildCount(path);
                    }
                }
            }
        }

        return count;
    }

    @Override
    public void UpdateCopyJobProgress(SwingCopyJob swingCopyJob, int percentProgressed) {
        // System.out.println(swingCopyJob.getDestinationFolder() + "    " + percentProgressed + "%");
    }

    @Override
    public void UpdatePathnameCopyProgress(SwingCopyJob swingCopyJob, Path pathnameBeingCopied, int percentProgressed) {
        // System.err.println(pathnameBeingCopied + " (" + percentProgressed + "%)");
        // System.out.println(pathnameBeingCopied + "    " + swingCopyJob.getDestinationFolder() + "    " + percentProgressed + "%");

        if (percentProgressed == 100) {
            pathsCopied++;

            if (pathsCopied > 1) {
                synchronized(lockObject) {
                    lockObject.notify();
                }
            }
        }
    }

    @Override
    public void InternalCopyJobException(SwingCopyJob swingCopyJob, Throwable throwable) {
        // System.err.println(throwable.getMessage());
    }

    /**
     * Constructs a folder hierarchy with files and subfolders for exercising methods
     */
    private void constructTestPaths() throws IOException {
        final long TWO_MEGABYTES = 2097152;
        final int oneByteOfData = 0;

        /*
            Produces a folder structure for testing as follows. Files are 0-bytes in length unless noted. Three of the
            four files named "file.dat" have the same byte length and should be identified as duplicates of each other.

            <usersTempFolder>/FileManagementTestFolder/
                deletionsTestFolder
                    deletionsFolder1
                    deletionsFolder2
                        deletionsFile1.txt
                    deletionsFolder3
                        deletionsFolder4
                            deletionsFile2.txt
                        deletionsFile3.txt
                    deletionsFile4.txt
                sourceFolder1
                    folder1
                        folder1SubFolder1
                        folder1SubFolder2
                        file.dat            (2-megabyte file)
                        folder1File1.dat
                        folder1File2.dat
                    folder2
                        folder2SubFolder1
                        folder2SubFolder2
                        file.dat            (2-megabyte file)
                        folder2File1.dat
                        folder2File2.dat
                    folder3
                        (empty)
                    file.dat                (2-megabyte file)
                    file1.dat               (2-megabyte file)
                    file2.dat
                sourceFolder2
                    file3.dat
                file4.dat                    (2-megabyte file)
                openPathnameTestFile.txt    (text file containing text "some text")
         */

        { // Create contents for "deletionsTestFolder" folder
            Path deletionsTestFolder = fileManagementTestFolder.resolve("deletionsTestFolder");

            Path deletionsFolder1 = deletionsTestFolder.resolve("deletionsFolder1");
            Path deletionsFolder2 = deletionsTestFolder.resolve("deletionsFolder2");
            Path deletionsFile1 = deletionsFolder2.resolve("deletionsFile1.txt");
            Path deletionsFolder3 = deletionsTestFolder.resolve("deletionsFolder3");
            Path deletionsFolder4 = deletionsFolder3.resolve("deletionsFolder4");
            Path deletionsFile2 = deletionsFolder4.resolve("deletionsFile2.txt");
            Path deletionsFile3 = deletionsFolder3.resolve("deletionsFile3.txt");
            Path deletionsFile4 = deletionsTestFolder.resolve("deletionsFile4.txt");

            Files.createDirectory(fileManagementTestFolder);

            Files.createDirectory(deletionsTestFolder);
            Files.createDirectory(deletionsFolder1);
            Files.createDirectory(deletionsFolder2);
            Files.createFile(deletionsFile1);
            Files.createDirectory(deletionsFolder3);
            Files.createDirectory(deletionsFolder4);
            Files.createFile(deletionsFile2);
            Files.createFile(deletionsFile3);
            Files.createFile(deletionsFile4);
        }

        { // Create root folder within which test file/folder hierarchy will be built and create its immediate contents
            Path sourceFolder1 = fileManagementTestFolder.resolve("sourceFolder1");
            Path sourceFolder2 = fileManagementTestFolder.resolve("sourceFolder2");
            Path file4 = fileManagementTestFolder.resolve("file4.dat");
            Path openPathnameTestFile = fileManagementTestFolder.resolve("openPathnameTestFile.txt");

            try {
                Files.createDirectory(sourceFolder1);
                Files.createDirectory(sourceFolder2);
                try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file4.toFile()))) {
                    for (long i = 0; i < TWO_MEGABYTES; i++) {
                        bos.write(oneByteOfData);
                    }
                }
                Files.write(openPathnameTestFile, "some text".getBytes());
            } catch (IOException e) {
                Assert.fail("Unable to create root folders and file for tests");
            }
        }

        { // Create "sourceFolder1" folder's immediate contents
            Path folder1 = fileManagementTestFolder.resolve("sourceFolder1/folder1");
            Path folder2 = fileManagementTestFolder.resolve("sourceFolder1/folder2");
            Path folder3 = fileManagementTestFolder.resolve("sourceFolder1/folder3");
            Path file = fileManagementTestFolder.resolve("sourceFolder1/file.dat");
            Path file1 = fileManagementTestFolder.resolve("sourceFolder1/file1.dat");
            Path file2 = fileManagementTestFolder.resolve("sourceFolder1/file2.dat");

            try {
                Files.createDirectory(folder1);
                Files.createDirectory(folder2);
                Files.createDirectory(folder3);
                try ( BufferedOutputStream bos1 = new BufferedOutputStream(new FileOutputStream(file.toFile()));
                      BufferedOutputStream bos2 = new BufferedOutputStream(new FileOutputStream(file1.toFile())) ) {
                    for (long i = 0; i < TWO_MEGABYTES; i++) {
                        bos1.write(oneByteOfData);
                        bos2.write(oneByteOfData);
                    }
                }
                Files.createFile(file2);
            } catch (IOException e) {
                Assert.fail("Unable to create \"file1.dat\" file within testFolder");
            }
        }

        { // Construct "folder1" subfolder's contents
            Path folder1 = fileManagementTestFolder.resolve("sourceFolder1/folder1");
            Path folder1SubFolder1 = folder1.resolve("folder1SubFolder1");
            Path folder1SubFolder2 = folder1.resolve("folder1SubFolder2");
            Path file = folder1.resolve("file.dat");
            Path folder1File1 = folder1.resolve("folder1File1.dat");
            Path folder1File2 = folder1.resolve("folder1File2.dat");

            try {
                Files.createDirectory(folder1SubFolder1);
                Files.createDirectory(folder1SubFolder2);
                try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file.toFile()))) {
                    for (long i = 0; i < TWO_MEGABYTES; i++) {
                        bos.write(oneByteOfData);
                    }
                }
                Files.createFile(folder1File1);
                Files.createFile(folder1File2);
            } catch (IOException e) {
                Assert.fail("Unable to create items for \"folder1\" subfolder");
            }
        }

        { // Construct "folder2" subfolder's contents
            Path folder2 = fileManagementTestFolder.resolve("sourceFolder1/folder2");
            Path folder2SubFolder1 = folder2.resolve("folder2SubFolder1");
            Path folder2SubFolder2 = folder2.resolve("folder2SubFolder2");
            Path file = folder2.resolve("file.dat");
            Path folder2File1 = folder2.resolve("folder2File1.dat");
            Path folder2File2 = folder2.resolve("folder2File2.dat");

            Files.createDirectory(folder2SubFolder1);
            Files.createDirectory(folder2SubFolder2);
            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file.toFile()))) {
                for (long i = 0; i < TWO_MEGABYTES; i++) {
                    bos.write(oneByteOfData);
                }
            }
            Files.createFile(folder2File1);
            Files.createFile(folder2File2);
        }

        { // Create contents of "sourceFolder2" folder
            Path file3 = fileManagementTestFolder.resolve("sourceFolder2/file3.dat");

            Files.createFile(file3);
        }
    }

} // class ConcurrentFileManagerTest
