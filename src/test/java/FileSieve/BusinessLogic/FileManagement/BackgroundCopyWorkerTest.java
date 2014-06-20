package FileSieve.BusinessLogic.FileManagement;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

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
 * JUnit testing for the BackgroundCopyWorker class
 */
public class BackgroundCopyWorkerTest {

    private final FileManager<BackgroundCopyWorker> fileManager = FileManagerFactory.getFileManager();
    private final String userTempFolder = System.getProperty("java.io.tmpdir");
    private final Path file = new File(userTempFolder + "swingWorkerFileManagementTestFile.txt").toPath();
    private final Path folder = new File(userTempFolder + "swingWorkerFileManagementTestFolder").toPath();
    private final Path subFolder = new File(folder + "/subFolderForSwingWorkerFileManagementTests").toPath();
    private final Path anotherFolder = new File(userTempFolder + "swingWorkerFileManagementTestFolder2").toPath();

    @Before
    public void setup() {
        Assume.assumeTrue("temporary file and folder used for tests should not pre-exist", !Files.exists(file) && !Files.exists(folder));
    }

    @After
    public void cleanup() {
        try {
            Files.deleteIfExists(file);

            // May not work but worth a shot
            fileManager.deletePathname(folder);
            fileManager.deletePathname(anotherFolder);
        } catch (SecurityException | IOException e) {
            // Ignore exceptions
        }
    }



} // class BackgroundCopyWorkerTest
