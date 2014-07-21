package FileSieve.Persistence.Preferences;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import javax.swing.JFrame;
import java.awt.*;
import java.util.*;
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
        uiWindow.setVisible(false);
    }

    @Test
    public void loadTest() throws Exception {
        windowPrefs.save();

        // set to random
        uiWindow.setBounds(2, 2, 2, 2);

        windowPrefs.load(uiWindow);

        Assert.assertEquals("After load the window width should match the saved setting.",
                uiWindow.getWidth(), DEFAULT_WIDTH);
        Assert.assertEquals("After load the window height should match the saved setting.",
                uiWindow.getHeight(), DEFAULT_HEIGHT);
        Assert.assertEquals("After load the window location (X axis) should match the saved setting.",
                uiWindow.getX(), DEFAULT_LEFT);
        Assert.assertEquals("After load the window location (Y axis) should match the saved setting.",
                uiWindow.getY(), DEFAULT_TOP);

        windowPrefs.clear();

        windowPrefs.load(uiWindow);
        //screen should be centered if no prefs exist.
        Assert.assertNotEquals("Calling load with no preferences set should center the window.",
                uiWindow.getX(), 0);
        Assert.assertNotEquals("Calling load with no preferences set should center the window.",
                uiWindow.getY(), 0);

        //test screen centering if window is off-screen.
        Point currentWindowLocation = new Point();
        currentWindowLocation.setLocation(DEFAULT_LEFT, DEFAULT_TOP);

        Rectangle currentScreen = CalculateUsableDisplayBounds(currentWindowLocation);

        uiWindow.setBounds(currentScreen.width - (DEFAULT_WIDTH/2),
                currentScreen.height - (DEFAULT_HEIGHT/2),
                DEFAULT_WIDTH,DEFAULT_HEIGHT);

        windowPrefs.save();

        windowPrefs.load(uiWindow);
        Assert.assertNotEquals("Preferences set with the window off screen should center the window on load.",
                uiWindow.getX(), currentScreen.width - (DEFAULT_WIDTH/2));
        Assert.assertNotEquals("Preferences set with the window off screen should center the window on load.",
                uiWindow.getY(), currentScreen.height - (DEFAULT_HEIGHT/2));
    }

    @Test
    public void saveTest() {
        //these two tests assert that the preference does not already equal the value passed
        //because save hasn't been called
        //but if the previously saved setting matches our test values then the assert would fail.
        if (originalWidth != DEFAULT_WIDTH) {
            Assert.assertNotEquals("Width setting should not be saved to preferences until save is called.",
                    windowPrefs.getWindowWidth(), DEFAULT_WIDTH);
        }
        if (originalHeight != DEFAULT_HEIGHT) {
            Assert.assertNotEquals("Height setting should not be saved to preferences until save is called.",
                    windowPrefs.getWindowHeight(), DEFAULT_HEIGHT);
        }

        windowPrefs = new WindowPlacementPreferences(uiWindow);
        windowPrefs.save();

        Assert.assertEquals("Width setting should be returned from preferences after saving.",
                windowPrefs.getWindowWidth(), DEFAULT_WIDTH);

        windowPrefs = new WindowPlacementPreferences(null);
        windowPrefs.save();

        Assert.assertEquals("Width setting should remain unchanged if null value passed to preferences.",
                windowPrefs.getWindowWidth(), DEFAULT_WIDTH);
    }

    @Test
    public void getWindowWidthTest() {
        windowPrefs.save();
        Assert.assertEquals("Width setting should be returned from preferences after saving.",
                windowPrefs.getWindowWidth(), DEFAULT_WIDTH);
    }

    @Test
    public void getWindowHeightTest() {
        windowPrefs.save();
        Assert.assertEquals("Height setting should be returned from preferences after saving.",
                windowPrefs.getWindowHeight(), DEFAULT_HEIGHT);
    }

    @Test
    public void getWindowTopTest() {
        windowPrefs.save();
        Assert.assertEquals("Location (y axis) setting should be returned from preferences after saving.",
                windowPrefs.getWindowTop(), DEFAULT_TOP);
    }

    @Test
    public void getWindowLeftTest() {
        windowPrefs.save();
        Assert.assertEquals("Location (x axis) setting should be returned from preferences after saving.",
                windowPrefs.getWindowLeft(), DEFAULT_LEFT);
    }

    @Test
    public void setDefaultSizeTest() throws Exception {
        windowPrefs.clear();

        windowPrefs.setDefaultSize(500, 501);
        Assert.assertEquals("Default width should be returned if set and no preferences saved.",
                windowPrefs.getWindowWidth(), 500);
        Assert.assertEquals("Default height should be returned if set and no preferences saved.",
                windowPrefs.getWindowHeight(), 501);
    }

    @Test
    public void clearTest() throws Exception {
        windowPrefs.clear();

        Assert.assertFalse("getPrefSet should always return false immediately after prefs are clear.",
                windowPrefs.getPrefsSet());
        Assert.assertEquals("After clear the window width should be 0.",
                windowPrefs.getWindowWidth(), 0);
        Assert.assertEquals("After clear the window height should be 0.",
                windowPrefs.getWindowHeight(), 0);
        Assert.assertEquals("After clear the location (x axis) should be 0.",
                windowPrefs.getWindowLeft(), 0);
        Assert.assertEquals("After clear the location (y axis) should be 0.",
                windowPrefs.getWindowTop(), 0);
    }


    /**
     * This is a copy of the CalculateUsableDisplayBounds function from WindowPlacementPreferences.
     * It is used here to get the window bounds to test the repositioning features of the load method.
     *
     * @param currentWindowLocation     Point object with x,y coordinate used to identify display.
     * @return      Rectangle object with bounds of usable space on the identified
     *              display device.
     */
    private static Rectangle CalculateUsableDisplayBounds(Point currentWindowLocation) throws HeadlessException {
        Rectangle bounds = null;
        java.util.List<Point> displayCenterPoints = new ArrayList<>();
        java.util.List<Rectangle> displayBounds = new ArrayList<>();

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (GraphicsDevice gd : ge.getScreenDevices()) {
            GraphicsConfiguration dgc = gd.getDefaultConfiguration();

            Rectangle gcBounds = dgc.getBounds();
            displayBounds.add(gcBounds);

            displayCenterPoints.add(new Point( (gcBounds.x + (gcBounds.width / 2)), (gcBounds.y + (gcBounds.height / 2)) ));

            if (gcBounds.contains(currentWindowLocation)) {
                Insets screenInsets  = Toolkit.getDefaultToolkit().getScreenInsets(dgc);

                int minX = (int)(gcBounds.getMinX() + screenInsets.left);
                int minY = (int)(gcBounds.getMinY() + screenInsets.top);
                int maxX = (int)(gcBounds.getMaxX() - screenInsets.right);
                int maxY = (int)(gcBounds.getMaxY() - screenInsets.bottom);

                bounds = new Rectangle(minX, minY, (maxX - minX), (maxY - minY));
            }
        }

        if (displayCenterPoints.isEmpty()) {
            throw new HeadlessException("No display devices detected.");
        } else if (bounds == null) {
            // Current Window location is not within the bounds of any detected display.
            // Identify the display whose center point is closest to the window's location...
            java.util.List<Integer> distances = new ArrayList<>();

            for (Point p : displayCenterPoints) {
                distances.add(Double.valueOf(Math.abs(currentWindowLocation.distance(p))).intValue());
            }

            int indexWithMinDistance = 0;
            Integer minDistance = null;
            for (int i = 0; i < distances.size(); i++) {
                if (minDistance == null) {
                    minDistance = distances.get(i);
                    indexWithMinDistance = i;
                } else {
                    minDistance = Math.min(minDistance, distances.get(i));
                    if (minDistance.equals(distances.get(i))) indexWithMinDistance = i;
                }
            }

            bounds = displayBounds.get(indexWithMinDistance);
        }

        return bounds;
    }
}