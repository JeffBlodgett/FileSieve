package FileSieve.Persistence.Preferences;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import javax.swing.JFrame;

public class WindowPlacementPreferencesTest {
    private WindowPlacementPreferences windowPrefs;
    private JFrame uiWindow;

    @Before
    public void setup() {
        uiWindow = new JFrame();
        uiWindow.setBounds(DEFAULT_LEFT, DEFAULT_TOP, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        uiWindow.setVisible(true);
        windowPrefs = new WindowPlacementPreferences(uiWindow);
        windowPrefs.save();
    }

    @After
    public void cleanup() {
        //TODO: Set preferences back to original values
    }

    @Test
    public void getWindowWidthTest() {
        Assert.assertEquals(windowPrefs.getWindowWidth(), DEFAULT_WIDTH);
    }

    @Test
    public void getWindowHeightTest() {
        Assert.assertEquals(windowPrefs.getWindowHeight(), DEFAULT_HEIGHT);
    }

    @Test
    public void getWindowTopTest() {
        Assert.assertEquals(windowPrefs.getWindowTop(), DEFAULT_TOP);
    }

    @Test
    public void getWindowLeftTest() {
        Assert.assertEquals(windowPrefs.getWindowLeft(), DEFAULT_LEFT);
    }

    @Test
    public void getScreenTest() {
        Assert.assertEquals(windowPrefs.getScreen(), 0);
    }

    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 200;
    private static final int DEFAULT_TOP = 50;
    private static final int DEFAULT_LEFT = 75;
}