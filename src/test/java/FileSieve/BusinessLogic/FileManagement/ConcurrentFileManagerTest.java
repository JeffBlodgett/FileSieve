package FileSieve.BusinessLogic.FileManagement;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JUnit testing for the ConcurrentFileManager class
 */
public class ConcurrentFileManagerTest {

    private final FileManager fileManager = FileManagerFactory.getFileManager();
    private final String userTempFolder = System.getProperty("java.io.tmpdir");
    private final Path file = new File(userTempFolder + "swingWorkerFileManagementTestFile.txt").toPath();
    private final Path folder = new File(userTempFolder + "swingWorkerFileManagementTestFolder").toPath();
    private final Path subFolder = new File(folder + "/subFolderForSwingWorkerFileManagementTests").toPath();
    private final Path anotherFolder = new File(userTempFolder + "swingWorkerFileManagementTestFolder2").toPath();
    private static boolean deletePathnameTestsPassed = false;
    private final CopyJobCompletionListener jobCompletionListener = new CopyJobCompletionListener(fileManager);

    @Before
    public void setup() {
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
            ((FileManager)fileManager).setDesktopOpenDisabled(true);   // Prevents file from being open in next statement
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
    public void testCopyPathname() {
        if (deletePathnameTestsPassed == false) Assert.fail("testings of pathnameCopyProviders method depends on deletePathname testing, one or assertions for which failed");

        // Prepare a temp folder with files and folders to copy
        try {
            Files.createDirectory(folder);          // creates folder named "swingWorkerFileManagementTestFolder" in user's temp folder
        } catch (SecurityException | IOException e) {
            Assert.fail(e.getClass().getSimpleName() + "while attempting to create \"" + folder.toFile().getName() + "\" folder in temp folder");
        }
        Path fileToCopy = new File(folder + "/fileToCopy.dat").toPath();
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileToCopy.toFile()))) {
            int aByte = 0;
            for (long i = 0; i < 2000000L; i++) {
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
            for (long i = 0; i < 2000000L; i++) {
                bos.write(aByte);
            }
        } catch (IOException e) {
            Assert.fail("IOException while attempting to create temporary file #2 for use in testing file copy operations");
        }

        // ASSERTION SET 1: Copy a single file to a non-existing target folder (recursion enabled but isn't applicable in the case of a file as the sourcePathname)
        ConcurrentFileManager.BackgroundCopyWorker worker1 = ((ConcurrentFileManager)fileManager).getBackgroundCopyWorker(fileToCopy, anotherFolder);
        worker1.setRecursionEnabled(true);
        worker1.setOverwriteExistingFiles(false);
        worker1.setOverwriteIfSizeDiffers(false);
        try {
            worker1.execute();

            // SwingWorker.get() method causes code execution in current thread to block until SwingWorker's state is "DONE"
            Assert.assertTrue("copying of a file to a new folder, BackgroundCopyWorker.get() method returns true", worker1.get());

            Assert.assertEquals("copying of a file to a new folder, created one pathname in target folder", 1, getChildCount(anotherFolder));

            Path fileNameToCheck = fileToCopy.getName(fileToCopy.getNameCount() - 1);
            Assert.assertTrue("copying of a file to a new folder, copy of file exists in targetPathname", Files.isRegularFile(new File(anotherFolder.resolve(fileNameToCheck).toString()).toPath()));
        } catch (InterruptedException e) {
            Assert.fail("InterruptedException during BackgroundCopyWorker execution, copying of a file to a new folder");
        } catch (ExecutionException e) {
            Assert.fail(e.getCause().getClass().getSimpleName() + " during BackgroundCopyWorker execution, copying of a file to a new folder - " + e.getCause().getMessage());
        }
        try {
            fileManager.deletePathname(anotherFolder);
        } catch (IOException e) {
            Assert.fail("IOException while deleting folder following assertion of non-recursive copying of a folder - " + anotherFolder);
        }

        // ASSERTION SET 2: Copy folder contents to a non-existing target folder with folder recursion disabled
        ConcurrentFileManager.BackgroundCopyWorker worker2 = ((ConcurrentFileManager)fileManager).getBackgroundCopyWorker(folder, anotherFolder);
        worker2.setRecursionEnabled(false);
        worker2.setOverwriteExistingFiles(false);
        worker2.setOverwriteIfSizeDiffers(false);
        try {
            worker2.execute();
            Assert.assertTrue("non-recursive copying of a folder, BackgroundCopyWorker.get() method returns true", worker2.get());

            Assert.assertEquals("non recursive copying of a folder, created two pathnames in target folder", 2, getChildCount(anotherFolder));

            Path fileNameToCheck = fileToCopy.getName(fileToCopy.getNameCount() - 1);
            Assert.assertTrue("non-recursive copying of a folder, copied a file into the target folder", Files.isRegularFile(anotherFolder.resolve(fileNameToCheck), LinkOption.NOFOLLOW_LINKS));

            Path folderNameToCheck = subFolder.getName(subFolder.getNameCount() - 1);
            Assert.assertTrue("non-recursive copying of a folder, created an empty folder within target folder", Files.isDirectory(anotherFolder.resolve(folderNameToCheck)));
        } catch (InterruptedException e) {
            Assert.fail("InterruptedException during BackgroundCopyWorker execution, non-recursive copying of a folder");
        } catch (ExecutionException e) {
            Assert.fail(e.getCause().getClass().getSimpleName() + " during BackgroundCopyWorker execution, non-recursive copying of a folder - " + e.getCause().getMessage() );
        }
        try {
            fileManager.deletePathname(anotherFolder);
        } catch (IOException e) {
            Assert.fail("IOException while deleting folder following assertion of non-recursive copying of a folder: " + anotherFolder);
        }

        try { Thread.sleep(500); } catch (InterruptedException e) { }

        // ASSERTION SET 3: Copy a folder, with the contents of all subfolders, to a target folder (folder recursion enabled)
        ConcurrentFileManager.BackgroundCopyWorker worker3 = ((ConcurrentFileManager)fileManager).getBackgroundCopyWorker(folder, anotherFolder);
        worker3.setRecursionEnabled(true);
        worker3.setOverwriteExistingFiles(false);
        worker3.setOverwriteIfSizeDiffers(false);
//        try {
            worker3.execute();
            //Assert.assertTrue("recursive copying of a folder, BackgroundCopyWorker.get() method returns true", worker3.get());

            fileManager.addPropertyChangeListener(folder.toString(), jobCompletionListener);
            jobCompletionListener.setJob(folder);
            jobCompletionListener.waitForJobCompletion();

            Assert.assertEquals("recursive copying of a folder, created two pathnames in target folder", 3, getChildCount(anotherFolder));

            Path fileNameToCheck = fileToCopy.getName(fileToCopy.getNameCount() - 1);
            Assert.assertTrue("recursive copying of a folder, copied a file into the target folder", Files.isRegularFile(anotherFolder.resolve(fileNameToCheck), LinkOption.NOFOLLOW_LINKS));

            Path folderNameToCheck = subFolder.getName(subFolder.getNameCount() - 1);
            Path folderPathToCheck = anotherFolder.resolve(folderNameToCheck);
            Assert.assertTrue("recursive copying of a folder, created a folder within target folder", Files.isDirectory(folderPathToCheck));

            fileNameToCheck = anotherFileToCopy.getName(anotherFileToCopy.getNameCount() - 1);
            Assert.assertTrue("recursive copying of a folder, copied a file into a folder within the target folder", Files.isRegularFile(folderPathToCheck.resolve(fileNameToCheck), LinkOption.NOFOLLOW_LINKS));
//        } catch (InterruptedException e) {
//            Assert.fail("InterruptedException during BackgroundCopyWorker execution, recursive copying of a folder");
//        } catch (ExecutionException e) {
//            Assert.fail(e.getCause().getClass().getSimpleName() + " during BackgroundCopyWorker execution, recursive copying of a folder - " + e.getCause().getMessage() );
//        }

        Assert.assertEquals("tracked copy jobs have been been removed from class' internal map", 0, ((ConcurrentFileManager)fileManager).copyJobs.size());

        // Cleanup
        try {
            fileManager.deletePathname(folder);
            fileManager.deletePathname(anotherFolder);
        } catch (IOException e) {
            Assert.fail("IOException while attempting to delete temporary folder tree");
        }
    }

    private int getChildCount(Path pathname) {
        int count = 0;

        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(pathname);) {
            for (Path path : dirStream) {
                if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    count++;
                } else if (Files.isDirectory(path)) {
                    count += 1 + getChildCount(path);
                }
            }
        } catch (IOException e) {
            // Ignore exceptions
        }

        return count;
    }

    /**
     * Custom PropertyChangeLister for detecting when a copy job has been completed.
     */
    private static class CopyJobCompletionListener implements PropertyChangeListener {

        private FileManager propertyChangeSupporter;
        private Path jobName;
        private AtomicBoolean completionDetected = new AtomicBoolean(false);
        private Object lock = new Object();

        /**
         * Constructor for CopyJobCompletionListener class
         *
         * @param propertyChangeSupporter   the FileManager with which the listener will be registered/unregistered
         */
        public CopyJobCompletionListener(FileManager propertyChangeSupporter) {
            if (propertyChangeSupporter == null) throw new IllegalArgumentException("null reference to PropertyChangeSupport object");
            this.propertyChangeSupporter = propertyChangeSupporter;
        }

        /**
         *
         * @param sourcePathBeingCopied
         */
        public void setJob(Path sourcePathBeingCopied) {
            if (sourcePathBeingCopied != null) {
                if (jobName != null) propertyChangeSupporter.removePropertyChangeListener(jobName.toString(), this);

                completionDetected.set(false);
                propertyChangeSupporter.addPropertyChangeListener(sourcePathBeingCopied.toString(), this);
                jobName = sourcePathBeingCopied;
            }
        }

        public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();

            if (propertyName.equals(jobName.toString())) {
                Object newValue = e.getNewValue();
                Object oldValue = e.getOldValue();

                if ((newValue != null) && (newValue instanceof SimpleImmutableEntry) && (((SimpleImmutableEntry<String, Integer>)newValue).getKey().equals("totalCopyProgress")) && (((SimpleImmutableEntry<String, Integer>)newValue).getValue() == 100)) {
                    completionDetected.set(true);
                    propertyChangeSupporter.removePropertyChangeListener(jobName.toString(), this);
                    jobName = null;

                    synchronized(lock) {
                        lock.notify();
                    }
                }


            }
        }

        public void waitForJobCompletion() {
            if (jobName != null) {
                synchronized (lock) {
                    while (!completionDetected.get()) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            // Ignore interrupt
                        }
                    }
                }
                completionDetected.set(false);
            } else {
                throw new IllegalStateException("job name (sourcePathname) has not been provided");
            }
        }
    }

} // class ConcurrentFileManagerTest
