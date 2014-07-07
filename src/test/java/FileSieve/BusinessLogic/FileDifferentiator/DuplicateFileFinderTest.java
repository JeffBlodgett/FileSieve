package FileSieve.BusinessLogic.FileDifferentiator;

import FileSieve.BusinessLogic.FileDifferentiation.FileDifferentiator;
import FileSieve.BusinessLogic.FileDifferentiation.FileDifferentiatorFactory;
import FileSieve.BusinessLogic.FileManagement.FileManagerFactory;
import FileSieve.BusinessLogic.FileManagement.SwingFileManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DuplicateFileFinderTest {

    private final FileDifferentiator fileDifferentiator = FileDifferentiatorFactory.getFileDifferentiator();
    private final SwingFileManager fileManager = FileManagerFactory.getSwingFileManager();
    private final String userTempFolder = System.getProperty("java.io.tmpdir");
    private final Path fileDifferentiationTestFolder = new File(userTempFolder + "FileDifferentiationTestFolder").toPath();
    private final Map<Path, BasicFileAttributes> pathnames = new LinkedHashMap<Path, BasicFileAttributes>(20);

    @Before
    public void setup() {
        Assume.assumeFalse("folder used for tests should not pre-exist", Files.exists(fileDifferentiationTestFolder));

        // Construct folders and files to be used for testing
        constructTestPaths();
    }

    @After
    public void cleanup() throws IOException {
        Assert.assertTrue("able to delete temp folder constructed by setup() method for tests", fileManager.deletePathname(fileDifferentiationTestFolder));
    }

    @Test
    public void testGetDuplicatedFiles() {
        // List<SimpleImmutableEntry<String, List<File>>> getDuplicatedFiles(Map<Path, BasicFileAttributes> pathnames)

        List<SimpleImmutableEntry<String, List<File>>> duplicates = fileDifferentiator.getDuplicatedFiles(pathnames);

        Assert.assertTrue("1 file with name of \"file.dat\" was found to have duplicates", (duplicates.size() == 1) && (duplicates.get(0).getKey().equals("file.dat")));

        List<File> fileDotDatDuplications = duplicates.get(0).getValue();

        Assert.assertTrue("3 copies of \"file.dat\" were found", fileDotDatDuplications.size() == 3);
        Assert.assertFalse("list of copies does not include the version of \"file.dat\" found in the \"folder2\" subfolder", fileDotDatDuplications.contains(fileDifferentiationTestFolder.resolve("sourceFolder1/folder2/file.dat").toFile()));

        Assert.assertTrue("first reported copy is the one in the root of the test folder", fileDotDatDuplications.get(0).toPath().getParent().getFileName().toString().equals(fileDifferentiationTestFolder.getFileName().toString()));
        Assert.assertTrue("second reported copy is the one in the \"sourceFolder1\" subfolder", fileDotDatDuplications.get(1).toPath().getParent().getFileName().toString().equals("sourceFolder1"));
        Assert.assertTrue("third reported copy is the one in the \"folder1\" subfolder", fileDotDatDuplications.get(2).toPath().getParent().getFileName().toString().equals("folder1"));
    }

    /**
     * Constructs a folder hierarchy with files and subfolders for exercising methods and add pathnames to the
     * "pathnames" Map in the same order in which a FileDiscoverer object would do so.
     */
    private void constructTestPaths() {
        /*
            Produces a folder structure for testing as follows. Files are 0-bytes in length unless noted. Three of the
            four files named "file.dat" have the same byte length and should be identified as duplicates of each other.

            <usersTempFolder>/FileEnumerationTestFolder/
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
            Path sourceFolder1 = fileDifferentiationTestFolder.resolve("sourceFolder1");
            Path sourceFolder2 = fileDifferentiationTestFolder.resolve("sourceFolder2");
            Path file = fileDifferentiationTestFolder.resolve("file.dat");

            try {
                Files.createDirectory(fileDifferentiationTestFolder);

                Files.createDirectory(sourceFolder1);
                Files.createDirectory(sourceFolder2);
                Files.write(file, "test".getBytes());

                pathnames.put(sourceFolder1, Files.readAttributes(sourceFolder1, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
                pathnames.put(sourceFolder2, Files.readAttributes(sourceFolder2, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
                pathnames.put(file, Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));

            } catch (IOException e) {
                Assert.fail("Unable to create root folders and file for tests");
            }
        }

        { // Create "sourceFolder1" folder and its immediate contents
            Path folder1 = fileDifferentiationTestFolder.resolve("sourceFolder1/folder1");
            Path folder2 = fileDifferentiationTestFolder.resolve("sourceFolder1/folder2");
            Path folder3 = fileDifferentiationTestFolder.resolve("sourceFolder1/folder3");
            Path file = fileDifferentiationTestFolder.resolve("sourceFolder1/file.dat");
            Path file1 = fileDifferentiationTestFolder.resolve("sourceFolder1/file1.dat");
            Path file2 = fileDifferentiationTestFolder.resolve("sourceFolder1/file2.dat");


            try {
                Files.createDirectory(folder1);
                Files.createDirectory(folder2);
                Files.createDirectory(folder3);
                Files.write(file, "test".getBytes());
                Files.createFile(file1);
                Files.createFile(file2);

                pathnames.put(folder1, Files.readAttributes(folder1, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
                pathnames.put(folder2, Files.readAttributes(folder2, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
                pathnames.put(folder3, Files.readAttributes(folder3, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
                pathnames.put(file, Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
                pathnames.put(file1, Files.readAttributes(file1, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
                pathnames.put(file2, Files.readAttributes(file2, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
            } catch (IOException e) {
                Assert.fail("Unable to create \"file1.dat\" file within testFolder");
            }
        }

        { // Construct "folder1" subfolder files and folders
            Path folder1 = fileDifferentiationTestFolder.resolve("sourceFolder1/folder1");
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

                pathnames.put(folder1SubFolder1, Files.readAttributes(folder1SubFolder1, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
                pathnames.put(folder1SubFolder2, Files.readAttributes(folder1SubFolder2, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
                pathnames.put(file, Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
                pathnames.put(folder1File1, Files.readAttributes(folder1File1, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
                pathnames.put(folder1File2, Files.readAttributes(folder1File2, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
            } catch (IOException e) {
                Assert.fail("Unable to create items for \"folder1\" subfolder");
            }
        }

        { // Construct "folder2" subfolder files and folders
            Path folder2 = fileDifferentiationTestFolder.resolve("sourceFolder1/folder2");
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

                pathnames.put(folder2SubFolder1, Files.readAttributes(folder2SubFolder1, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
                pathnames.put(folder2SubFolder2, Files.readAttributes(folder2SubFolder2, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
                pathnames.put(file, Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
                pathnames.put(folder2File1, Files.readAttributes(folder2File1, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
                pathnames.put(folder2File2, Files.readAttributes(folder2File2, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
            } catch (IOException e) {
                Assert.fail("Unable to create items for \"folder2\" subfolder");
            }
        }

        { // Create contents of "sourceFolder2" folder
            Path file3 = fileDifferentiationTestFolder.resolve("sourceFolder2/file3.dat");

            try {
                Files.createFile(file3);

                pathnames.put(file3, Files.readAttributes(file3, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
            } catch (IOException e) {
                Assert.fail("Unable to create \"file3.dat\" file within \"sourceFolder2\" subfolder");
            }
        }

        Assert.assertTrue("Map containing pathnames contains 20 entries", pathnames.size() == 20);
    }

} // class DuplicateFileFinderTest
