package FileSieve.Persistence.Preferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import java.util.ArrayList;
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
        targetPath = System.getProperty("user.dir");
        selectedPaths = new SelectedPaths(referencePaths, targetPath);
        hadOriginalPrefs = selectedPaths.getPrefsSet();
        if (hadOriginalPrefs) {
            originalTargetPath = selectedPaths.getTargetPathName();
            originalReferencePaths = selectedPaths.getReferencePathNames();
        }
        selectedPaths.save();
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
    public void getReferencePathTest() {
        List<String> badPaths = new ArrayList<>();
        badPaths.add(System.getProperty("user.dir") + "bad");
        Assert.assertNotEquals("Reference path should not match bad path", selectedPaths.getReferencePathNames(), badPaths);
    }

    @Test
    public void getTargetPathTest() {
        String badPath = System.getProperty("user.dir") + "bad";
        Assert.assertNotEquals("Target path should not match bad path", selectedPaths.getTargetPathName(), badPath);
    }
}
