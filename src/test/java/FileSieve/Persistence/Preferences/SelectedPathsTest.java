package FileSieve.Persistence.Preferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.BackingStoreException;

public class SelectedPathsTest {
    private SelectedPaths selectedPaths;
    private List<String> referencePaths;
    private String targetPath;
    private List<String> originalReferencePaths;
    private String originalTargetPath;
    private boolean hadOriginalPrefs;

    @Before
    public void setup() {
        referencePaths = new ArrayList<>();
        referencePaths.add(System.getProperty("user.dir"));
        referencePaths.add(System.getProperty("user.dir") + "test");
        targetPath = System.getProperty("user.dir");
        selectedPaths = new SelectedPaths(referencePaths, targetPath);
        hadOriginalPrefs = selectedPaths.getPrefsSet();
        if (hadOriginalPrefs) {
            originalTargetPath = selectedPaths.getTargetPathName();
            originalReferencePaths = selectedPaths.getReferencePathNames();
        }
    }

    @After
    public void cleanup() throws BackingStoreException {
        if (hadOriginalPrefs) {
            selectedPaths = new SelectedPaths(originalReferencePaths, originalTargetPath);
            selectedPaths.save();
        } else {
            selectedPaths.clear();
        }
    }

    @Test
    public void saveTest() throws Exception  {
        Assert.assertNotEquals("The reference paths should not match before saving.",
                selectedPaths.getReferencePathNames(), referencePaths);
        Assert.assertNotEquals("The target path should not match before saving.",
                selectedPaths.getTargetPathName(), targetPath);

        selectedPaths.save();

        Assert.assertTrue("getPrefSet should always return true immediately after a save.",
                selectedPaths.getPrefsSet());
        Assert.assertEquals("The reference paths should match before after saving.",
                selectedPaths.getReferencePathNames(), referencePaths);
        Assert.assertEquals("The target paths should match before after saving.",
                selectedPaths.getTargetPathName(), targetPath);
    }

    @Test
    public void getReferencePathTest() throws Exception  {
        selectedPaths.save();

        List<String> badPaths = new ArrayList<>();
        badPaths.add(System.getProperty("user.dir") + "bad");
        Assert.assertNotEquals("Reference path should not match bad path",
                selectedPaths.getReferencePathNames(), badPaths);
    }

    @Test
    public void getTargetPathTest() throws Exception  {
        selectedPaths.save();

        String badPath = System.getProperty("user.dir") + "bad";
        Assert.assertNotEquals("Target path should not match bad path",
                selectedPaths.getTargetPathName(), badPath);
    }

    @Test
    public void clearTest() throws Exception {
        selectedPaths.clear();

        Assert.assertEquals("After clear the target path should be empty.",
                selectedPaths.getTargetPathName(), "");
        Assert.assertEquals("After clear the reference paths should return an empty array list.",
                selectedPaths.getReferencePathNames(),
                new ArrayList<>(Arrays.asList("")));
    }

    @Test
    public void getPrefsSetTest() throws Exception {
        selectedPaths = new SelectedPaths(referencePaths, targetPath);
        selectedPaths.save();

        Assert.assertTrue("getPrefSet should always return true immediately after a save.",
                selectedPaths.getPrefsSet());

        selectedPaths.clear();

        Assert.assertFalse("getPrefSet should always return false immediately after prefs are clear.",
                selectedPaths.getPrefsSet());
    }
}
