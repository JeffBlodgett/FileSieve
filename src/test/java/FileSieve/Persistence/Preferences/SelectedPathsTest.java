package FileSieve.Persistence.Preferences;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

public class SelectedPathsTest {
    private SelectedPaths selectedPaths;
    private String referencePath;
    private String targetPath;

    @Before
    public void setup() {
        referencePath = System.getProperty("user.dir");
        targetPath = System.getProperty("user.dir");
        selectedPaths = new SelectedPaths(referencePath, targetPath);
    }

    @After
    public void cleanup() {
        //TODO: set paths back to original values
    }

    @Test
    public void getReferencePathTest() {
        String badPath = referencePath = System.getProperty("user.dir") + "bad";
        Assert.assertNotEquals("Reference path should not match bad path", selectedPaths.getReferencePathName(), badPath);
    }

    @Test
    public void getTargetPathTest() {
        String badPath = referencePath = System.getProperty("user.dir") + "bad";
        Assert.assertNotEquals("Target path should not match bad path", selectedPaths.getTargetPathName(), badPath);
    }
}
