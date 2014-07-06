package FileSieve.Persistence.Preferences;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import javax.swing.JFrame;
import java.util.prefs.BackingStoreException;

public class WindowPlacementPreferencesTest {
    private WindowPlacementPreferences windowPrefs;
    private JFrame uiWindow;
    private int originalWidth;
    private int originalHeight;
    private int originalLeft;
    private int originalTop;
    private boolean hadOriginalPrefs;

    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 200;
    private static final int DEFAULT_LEFT = 75;
    private static final int DEFAULT_TOP = 50;

    @Before
    public void setup() {
        uiWindow = new JFrame();
        uiWindow.setBounds(DEFAULT_LEFT, DEFAULT_TOP, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        uiWindow.setVisible(true);
        windowPrefs = new WindowPlacementPreferences(uiWindow);

        //if the preferences were previously set we need to get the values so we can return them after the tests
        hadOriginalPrefs = windowPrefs.getPrefsSet();
        if (hadOriginalPrefs) {
            originalWidth = windowPrefs.getWindowWidth();
            originalHeight = windowPrefs.getWindowHeight();
            originalLeft = windowPrefs.getWindowLeft();
            originalTop = windowPrefs.getWindowTop();
        }

        windowPrefs.save();
    }

    @After
    public void cleanup() throws BackingStoreException {
        if (hadOriginalPrefs) {
            uiWindow.setBounds(originalLeft, originalTop, originalWidth, originalHeight);
            uiWindow.setVisible(true);
            windowPrefs = new WindowPlacementPreferences(uiWindow);
            windowPrefs.save();
        } else {
            windowPrefs.clear();
        }
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

}