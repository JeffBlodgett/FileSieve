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

        if (hadOriginalPrefs) {
            windowPrefs.load(uiWindow);

            Assert.assertEquals(uiWindow.getWidth(), originalWidth);
            Assert.assertEquals(uiWindow.getHeight(), originalHeight);
            Assert.assertEquals(uiWindow.getX(), originalLeft);
            Assert.assertEquals(uiWindow.getY(), originalTop);
        } else {
            windowPrefs.save();

            // set to random
            uiWindow.setBounds(2, 2, 2, 2);

            windowPrefs.load(uiWindow);

            Assert.assertEquals(uiWindow.getWidth(), DEFAULT_WIDTH);
            Assert.assertEquals(uiWindow.getHeight(), DEFAULT_HEIGHT);
            Assert.assertEquals(uiWindow.getX(), DEFAULT_LEFT);
            Assert.assertEquals(uiWindow.getY(), DEFAULT_TOP);
        }

        windowPrefs.clear();

        windowPrefs.load(uiWindow);

        Assert.assertFalse(windowPrefs.getPrefsSet());
        Assert.assertEquals(uiWindow.getWidth(), 0);
        Assert.assertEquals(uiWindow.getHeight(), 0);

        //screen should be centered if no prefs exist.
        Assert.assertNotEquals(uiWindow.getX(), 0);
        Assert.assertNotEquals(uiWindow.getY(), 0);

        //test screen centering if window is off-screen.
        Point currentWindowLocation = new Point();
        currentWindowLocation.setLocation(DEFAULT_LEFT, DEFAULT_TOP);

        Rectangle currentScreen = CalculateUsableDisplayBounds(currentWindowLocation);

        uiWindow.setBounds(currentScreen.width - (DEFAULT_WIDTH/2),
                currentScreen.height - (DEFAULT_HEIGHT/2),
                DEFAULT_WIDTH,DEFAULT_HEIGHT);

        windowPrefs.save();

        windowPrefs.load(uiWindow);
        Assert.assertNotEquals(uiWindow.getX(), currentScreen.width - (DEFAULT_WIDTH/2));
        Assert.assertNotEquals(uiWindow.getY(), currentScreen.height - (DEFAULT_HEIGHT/2));
    }

    @Test
    public void saveTest() {
        if (originalWidth != DEFAULT_WIDTH) {
            Assert.assertNotEquals(windowPrefs.getWindowWidth(), DEFAULT_WIDTH);
        }
        if (originalHeight != DEFAULT_HEIGHT) {
            Assert.assertNotEquals(windowPrefs.getWindowHeight(), DEFAULT_HEIGHT);
        }

        windowPrefs = new WindowPlacementPreferences(uiWindow);
        windowPrefs.save();

        Assert.assertEquals(windowPrefs.getWindowWidth(), DEFAULT_WIDTH);
        Assert.assertEquals(windowPrefs.getWindowHeight(), DEFAULT_HEIGHT);

        windowPrefs = new WindowPlacementPreferences(null);
        windowPrefs.save();

        Assert.assertEquals(windowPrefs.getWindowWidth(), DEFAULT_WIDTH);
        Assert.assertEquals(windowPrefs.getWindowHeight(), DEFAULT_HEIGHT);
    }

    @Test
    public void getWindowWidthTest() {
        windowPrefs.save();
        Assert.assertEquals(windowPrefs.getWindowWidth(), DEFAULT_WIDTH);
    }

    @Test
    public void getWindowHeightTest() {
        windowPrefs.save();
        Assert.assertEquals(windowPrefs.getWindowHeight(), DEFAULT_HEIGHT);
    }

    @Test
    public void getWindowTopTest() {
        windowPrefs.save();
        Assert.assertEquals(windowPrefs.getWindowTop(), DEFAULT_TOP);
    }

    @Test
    public void getWindowLeftTest() {
        windowPrefs.save();
        Assert.assertEquals(windowPrefs.getWindowLeft(), DEFAULT_LEFT);
    }

    @Test
    public void setDefaultSizeTest() throws Exception {
        windowPrefs.clear();

        windowPrefs.setDefaultSize(500, 501);
        Assert.assertEquals(windowPrefs.getWindowWidth(), 500);
        Assert.assertEquals(windowPrefs.getWindowHeight(), 501);
    }

    @Test
    public void clearTest() throws Exception {
        windowPrefs.clear();

        Assert.assertFalse(windowPrefs.getPrefsSet());
        Assert.assertEquals(windowPrefs.getWindowWidth(), 0);
        Assert.assertEquals(windowPrefs.getWindowHeight(), 0);
        Assert.assertEquals(windowPrefs.getWindowLeft(), 0);
        Assert.assertEquals(windowPrefs.getWindowTop(), 0);
    }


    /**
     * Identifies the display on which the form currently resides and returns the bounds
     * of the display as a Rectangle, taking into consideration the space occupied by the
     * taskbar or other screen insets. If the form's location does not reside on any
     * detected display then the bounds of the closest display is returned. The
     * coordinates of the returned rectangle (display bounds) are relative to the
     * overall virtual desktop.
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