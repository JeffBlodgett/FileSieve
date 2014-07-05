package FileSieve.BusinessLogic.FileDifferentiator;

import FileSieve.BusinessLogic.FileEnumeration.FileDiscoverer;
import FileSieve.BusinessLogic.FileManagement.FileManagerFactory;
import FileSieve.BusinessLogic.FileManagement.SwingFileManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DuplicateFileFinderTest {

    private final FileDiscoverer fileDiscoverer = new FileDiscoverer();
    private final SwingFileManager fileManager = FileManagerFactory.getSwingFileManager();
    private final String userTempFolder = System.getProperty("java.io.tmpdir");
    private final Path fileEnumerationTestFolder = new File(userTempFolder + "FileDifferentiationTestFolder").toPath();

    @Before
    public void setup() {

    }

    @After
    public void cleanup() throws IOException {

    }

    @Test
    public void testGetPathnamesWithoutRecursion() throws IOException {

    }

    /**
     * Constructs a folder structure with files and subfolders for exercising methods in FileEnumeration package
     */
    private void constructTestPaths() {
        /*
            Produces a folder structure for testing as follows:

            <usersTempFolder>/FileEnumerationTestFolder/
                sourceFolder1
                    folder1
                        folder1SubFolder1
                        folder1SubFolder2
                        folder1File1.dat
                        folder1File2.dat
                    folder2
                        folder2SubFolder1
                        folder2SubFolder2
                        folder2File1.dat
                        folder2File1.dat
                    folder3
                        (empty)
                    file1.dat
                    file2.dat
                sourceFolder2
                    file3.dat
                file.dat
         */

        try { // Create folder within which test file/folder hierarchy will be built
            Files.createDirectory(fileEnumerationTestFolder);
            Files.createDirectory(fileEnumerationTestFolder.resolve("sourceFolder2"));
            Files.createDirectory(fileEnumerationTestFolder.resolve("sourceFolder1"));
        } catch (IOException e) {
            Assert.fail("Unable to create folders for tests");
        }

        try {
            Files.createFile(fileEnumerationTestFolder.resolve("file.dat"));
        } catch (IOException e) {
            Assert.fail("Unable to create \"file.dat\" file within testFolder");
        }

        try {
            Files.createFile(fileEnumerationTestFolder.resolve("sourceFolder1/file1.dat"));
        } catch (IOException e) {
            Assert.fail("Unable to create \"file1.dat\" file within testFolder");
        }

        try {
            Files.createFile(fileEnumerationTestFolder.resolve("sourceFolder1/file2.dat"));
        } catch (IOException e) {
            Assert.fail("Unable to create \"file2.dat\" file within testFolder");
        }

        { // Construct "folder1" subfolder with some folders and files of its own
            Path folder1 = fileEnumerationTestFolder.resolve("sourceFolder1/folder1");
            Path folder1SubFolder1 = folder1.resolve("folder1SubFolder1");
            Path folder1SubFolder2 = folder1.resolve("folder1SubFolder2");
            Path folder1File1 = folder1.resolve("folder1File1.dat");
            Path folder1File2 = folder1.resolve("folder1File2.dat");

            try {
                Files.createDirectory(folder1);
                Files.createDirectory(folder1SubFolder1);
                Files.createDirectory(folder1SubFolder2);
                Files.createFile(folder1File1);
                Files.createFile(folder1File2);
            } catch (IOException e) {
                Assert.fail("Unable to create \"folder1\" folder hierarchy within test folder");
            }
        }

        { // Construct "folder2" subfolder with some folders and files of its own
            Path folder2 = fileEnumerationTestFolder.resolve("sourceFolder1/folder2");
            Path folder2SubFolder1 = folder2.resolve("folder2SubFolder1");
            Path folder2SubFolder2 = folder2.resolve("folder2SubFolder2");
            Path folder2File1 = folder2.resolve("folder2File1.dat");
            Path folder2File2 = folder2.resolve("folder2File2.dat");

            try {
                Files.createDirectory(folder2);
                Files.createDirectory(folder2SubFolder1);
                Files.createDirectory(folder2SubFolder2);
                Files.createFile(folder2File1);
                Files.createFile(folder2File2);
            } catch (IOException e) {
                Assert.fail("Unable to create \"folder2\" folder hierarchy with test folder");
            }
        }

        { // Create empty folder named "folder3"
            Path folder3 = fileEnumerationTestFolder.resolve("sourceFolder1/folder3");
            try {
                Files.createDirectory(folder3);
            } catch (IOException e) {
                Assert.fail("Unable to create \"folder3\" folder within test folder");
            }
        }

        try {
            Files.createFile(fileEnumerationTestFolder.resolve("sourceFolder2/file3.dat"));
        } catch (IOException e) {
            Assert.fail("Unable to create \"file3.dat\" file within test folder");
        }
    }

} // class DuplicateFileFinderTest
