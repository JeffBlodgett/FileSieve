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

    @Before
    public void setup() {
        Assume.assumeFalse("folder used for tests should not pre-exist", Files.exists(fileEnumerationTestFolder));
        constructTestPaths();
    }

    @After
    public void cleanup() throws IOException {
        Assert.assertTrue("able to delete temp folder constructed by setup() method for tests", fileManager.deletePathname(fileEnumerationTestFolder));
    }

    /**
     * Tests method getPathnames(Path, boolean)
     *
     * @throws IOException
     */
    @Test
    public void testGetPathnamesWithoutRecursion() throws IOException {
        recursiveSearches = false;    // set-up for non-recursive searches

        commonTestCode(fileDiscoverer.getPathnames(fileEnumerationTestFolder, false));
    }

    /**
     * Tests method getPathnames(Path)
     *
     * @throws IOException
     */
    @Test
    public void testGetPathnamesWithRecursion() throws IOException {
        recursiveSearches = true;     // set-up for recursive searches

        commonTestCode(fileDiscoverer.getPathnames(fileEnumerationTestFolder));
    }

    /**
     * Some code written to be called from either of the above tests
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
                        Assert.assertTrue("there are no further paths in Map since recursion is disabled", discoveredPaths.size() == 5);
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
                default:
                    Assert.fail("should only be 13 paths in Map");
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
         */

        try { // Create file inside
            Files.createDirectory(fileEnumerationTestFolder);
        } catch (IOException e) {
            Assert.fail("Unable to create folder for tests");
        }

        try {
            Files.createFile(new File(fileEnumerationTestFolder + "/file1.dat").toPath());
        } catch (IOException e) {
            Assert.fail("Unable to create \"file1.dat\" file within testFolder");
        }

        try {
            Files.createFile(new File(fileEnumerationTestFolder + "/file2.dat").toPath());
        } catch (IOException e) {
            Assert.fail("Unable to create \"file1.dat\" file within testFolder");
        }

        { // Construct "folder1" subfolder with some folders and files of its own
            Path folder1 = new File(fileEnumerationTestFolder + "/folder1").toPath();
            Path folder1SubFolder1 = new File(folder1 + "/folder1SubFolder1").toPath();
            Path folder1SubFolder2 = new File(folder1 + "/folder1SubFolder2").toPath();
            Path folder1File1 = new File(folder1 + "/folder1File1.dat").toPath();
            Path folder1File2 = new File(folder1 + "/folder1File2.dat").toPath();

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
            Path folder2 = new File(fileEnumerationTestFolder + "/folder2").toPath();
            Path folder2SubFolder1 = new File(folder2 + "/folder2SubFolder1").toPath();
            Path folder2SubFolder2 = new File(folder2 + "/folder2SubFolder2").toPath();
            Path folder2File1 = new File(folder2 + "/folder2File1.dat").toPath();
            Path folder2File2 = new File(folder2 + "/folder2File2.dat").toPath();

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
            Path folder3 = new File(fileEnumerationTestFolder + "/folder3").toPath();

            try {
                Files.createDirectory(folder3);
            } catch (IOException e) {
                Assert.fail("Unable to create \"folder3\" folder within test folder");
            }
        }
    }

} // class FileDiscovererTest
