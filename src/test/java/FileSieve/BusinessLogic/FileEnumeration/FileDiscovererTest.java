package FileSieve.BusinessLogic.FileEnumeration;

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
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JUnit testing for the FileDiscoverer class
 */
public class FileDiscovererTest {

    private final FileDiscoverer fileDiscoverer = new FileDiscoverer();
    private final SwingFileManager fileManager = FileManagerFactory.getSwingFileManager();
    private final String userTempFolder = System.getProperty("java.io.tmpdir");
    private final Path fileEnumerationTestFolder = new File(userTempFolder + "FileEnumerationTestFolder").toPath();
    private boolean recursiveSearches;
    private final List<Path> listOfPaths = new ArrayList<Path>(20);
    private long expectedFileBytesWithRecursion = 0;
    private long expectedFileBytesWithoutRecursion = 0;

    @Before
    public void setup() {
        Assume.assumeFalse("folder used for tests should not pre-exist", Files.exists(fileEnumerationTestFolder));

        // Construct folders and files to be used for testing
        constructTestPaths();

        listOfPaths.add(fileEnumerationTestFolder.resolve("file.dat"));
        listOfPaths.add(fileEnumerationTestFolder.resolve("sourceFolder2"));
        listOfPaths.add(fileEnumerationTestFolder.resolve("sourceFolder1"));
    }

    @After
    public void cleanup() throws IOException {
        Assert.assertTrue("able to delete temp folder constructed by setup() method for tests", fileManager.deletePathname(fileEnumerationTestFolder));
    }

    /**
     * Tests method getPathnames(List<Path>, boolean)
     *
     * @throws IOException
     */
    @Test
    public void testGetPathnamesWithoutRecursion() throws IOException {
        recursiveSearches = false;    // prepare for non-recursive searches
        commonTestCode(fileDiscoverer.getPathnames(listOfPaths, false));

        Assert.assertEquals("file discovery found 4 files", 4, fileDiscoverer.getFileCountFromLastEnumeration());
        Assert.assertEquals("file discovery reports proper number of bytes for sum of file byte lengths", expectedFileBytesWithoutRecursion, fileDiscoverer.getTotalFileByteCountFromLastEnumeration());
    }

    /**
     * Tests method getPathnames(List<Path>)
     *
     * @throws IOException
     */
    @Test
    public void testGetPathnamesWithRecursion() throws IOException {
        recursiveSearches = true;     // prepare for recursive searches
        commonTestCode(fileDiscoverer.getPathnames(listOfPaths));

        Assert.assertEquals("file discovery found 8 files", 8, fileDiscoverer.getFileCountFromLastEnumeration());
        Assert.assertEquals("file discovery reports proper number of bytes for sum of file byte lengths", expectedFileBytesWithRecursion, fileDiscoverer.getTotalFileByteCountFromLastEnumeration());
    }

    /**
     * Code written to be called from either of the above tests
     *
     * @param discoveredPaths   Map with path objects for discovered files and folders
     */
    private void commonTestCode(Map<Path, BasicFileAttributes> discoveredPaths) {
        int index = 0;

        for (Path path : discoveredPaths.keySet()){
            ++index;

            switch (index) {
                case 1:
                    Assert.assertTrue("first path in Map is that of \"folder1\" folder", path.getFileName().toString().equals("folder1"));
                    break;
                case 2:
                    Assert.assertTrue("second path in Map is that of \"folder2\" folder", path.getFileName().toString().equals("folder2"));
                    break;
                case 3:
                    Assert.assertTrue("third path in Map is that of \"folder3\" folder", path.getFileName().toString().equals("folder3"));
                    break;
                case 4:
                    Assert.assertTrue("fourth path in Map is that of \"file1.dat\" file", path.getFileName().toString().equals("file1.dat"));
                    break;
                case 5:
                    Assert.assertTrue("fifth path in Map is that of \"file2.dat\" file", path.getFileName().toString().equals("file2.dat"));

                    if (!recursiveSearches) {
                        Assert.assertTrue("should only be 7 paths in the Map since recursion is disabled (5 paths in \"sourceFolder1\" plus 1 in \"sourceFolder2\" plus file path that was passed as a source)", discoveredPaths.size() == 5 + 1 + 1);
                        return;
                    }
                    break;
                case 6:
                    Assert.assertTrue("sixth path in Map is that of \"folder1SubFolder1\" folder", path.getFileName().toString().equals("folder1SubFolder1"));
                    break;
                case 7:
                    Assert.assertTrue("seventh path in Map is that of \"folder1SubFolder2\" folder", path.getFileName().toString().equals("folder1SubFolder2"));
                    break;
                case 8:
                    Assert.assertTrue("eighth path in Map is that of \"folder1File1.dat\" file", path.getFileName().toString().equals("folder1File1.dat"));
                    break;
                case 9:
                    Assert.assertTrue("ninth path in Map is that of \"folder1File2.dat\" file", path.getFileName().toString().equals("folder1File2.dat"));
                    break;
                case 10:
                    Assert.assertTrue("tenth path in Map is that of \"folder2SubFolder1\" folder", path.getFileName().toString().equals("folder2SubFolder1"));
                    break;
                case 11:
                    Assert.assertTrue("eleventh path in Map is that of \"folder2SubFolder2\" folder", path.getFileName().toString().equals("folder2SubFolder2"));
                    break;
                case 12:
                    Assert.assertTrue("twelfth path in Map is that of \"folder2File1.dat\" file", path.getFileName().toString().equals("folder2File1.dat"));
                    break;
                case 13:
                    Assert.assertTrue("thirteenth path in Map is that of \"folder2File2.dat\" file", path.getFileName().toString().equals("folder2File2.dat"));
                    break;
                case 14:
                    Assert.assertTrue("fourteenth path in Map is that of \"file3.dat\" file", path.getFileName().toString().equals("file3.dat"));
                    break;
                case 15:
                    Assert.assertTrue("fifteenth path in Map is that of \"file.dat\" file", path.getFileName().toString().equals("file.dat"));
                    break;
                default:
                    Assert.fail("there should only be 15 paths in the Map for an enumeration with recursive searching enabled");
            }
        }
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
            Path file = fileEnumerationTestFolder.resolve("file.dat");
            Files.createFile(file); // 0 bytes in length (no content) - i.e. expectedFileBytesWith/outRecursion... += 0;
        } catch (IOException e) {
            Assert.fail("Unable to create \"file.dat\" file within testFolder");
        }

        try {
            Path file1 = fileEnumerationTestFolder.resolve("sourceFolder1/file1.dat");
            Files.write(file1, "file1".getBytes());

            expectedFileBytesWithRecursion += "file1".getBytes().length;
            expectedFileBytesWithoutRecursion += "file1".getBytes().length;
        } catch (IOException e) {
            Assert.fail("Unable to create \"file1.dat\" file within testFolder");
        }

        try {
            Path file2 = fileEnumerationTestFolder.resolve("sourceFolder1/file2.dat");
            Files.write(file2, "file2".getBytes());

            expectedFileBytesWithRecursion += "file2".getBytes().length;
            expectedFileBytesWithoutRecursion += "file2".getBytes().length;
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
                Files.write(folder1File1, "folder1File1".getBytes());
                Files.write(folder1File2, "folder1File2".getBytes());

                expectedFileBytesWithRecursion += "folder1File1".getBytes().length;
                expectedFileBytesWithRecursion += "folder1File2".getBytes().length;
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
                Files.write(folder2File1, "folder2File1".getBytes());
                Files.write(folder2File2, "folder2File2".getBytes());

                expectedFileBytesWithRecursion += "folder2File1".getBytes().length;
                expectedFileBytesWithRecursion += "folder2File2".getBytes().length;
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
            Path file3 = fileEnumerationTestFolder.resolve("sourceFolder2/file3.dat");
            Files.write(file3, "file3".getBytes());

            expectedFileBytesWithRecursion += "file3".getBytes().length;
            expectedFileBytesWithoutRecursion += "file3".getBytes().length;
        } catch (IOException e) {
            Assert.fail("Unable to create \"file3.dat\" file within test folder");
        }
    }

} // class FileDiscovererTest
