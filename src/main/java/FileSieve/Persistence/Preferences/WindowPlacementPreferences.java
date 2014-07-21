package FileSieve.Persistence.Preferences;

import java.awt.*;
import javax.swing.JFrame;
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.List;

/**
 * Class for saving/retrieving properties of the user window
 */
public class WindowPlacementPreferences {
    private Preferences prefNode;
    private JFrame frame;
    private boolean prefsSet = false;

    private int defaultWidth = 0;
    private int defaultHeight = 0;

    private static final String USER_WINDOW_TOP = "USER_WINDOW_TOP";
    private static final String USER_WINDOW_LEFT = "USER_WINDOW_LEFT";
    private static final String USER_WINDOW_WIDTH = "USER_WINDOW_WIDTH";
    private static final String USER_WINDOW_HEIGHT = "USER_WINDOW_HEIGHT";

    public WindowPlacementPreferences() {
        prefNode = Preferences.userNodeForPackage( getClass() );
        prefsSet = getPrefsSet();
    }

    public WindowPlacementPreferences(JFrame jframe) {
        this();
        frame = jframe;
    }

    /**
     * Sets default window dimensions if user settings don't yet exist.
     */
    public void setDefaultSize(int width, int height){
        defaultWidth = width;
        defaultHeight = height;
    }

    /**
     * Sets all values from the provided JFrame
     */
    public void save() {
        if (frame != null) {
            setWindowWidth(frame.getWidth());
            setWindowHeight(frame.getHeight());
            setWindowTop(frame.getY());
            setWindowLeft(frame.getX());
            prefsSet = true;
        }
    }

    /**
     * Gets values from storage and sets them into the JFrame passed in.
     *
     * @param jframe                    JFrame to position based on user preferences
     */
    public void load(JFrame jframe) {
        jframe.setBounds(getWindowLeft(), getWindowTop(), getWindowWidth(), getWindowHeight());
        setScreen(jframe);
    }

    /**
     * Returns the previous Y coordinate for the top of the window.
     *
     * @return                          int pixels from top of screen
     */
    public int getWindowTop() {
        return prefNode.getInt(USER_WINDOW_TOP, 0);
    }

    /**
     * Sets the previous Y coordinate for the top of the window in user's preference store
     */
    private void setWindowTop(int windowTop) {
        prefNode.putInt(USER_WINDOW_TOP, windowTop);
    }

    /**
     * Returns the previous X coordinate for the left edge of the window.
     *
     * @return                          int pixels from left edge of screen
     */
    public int getWindowLeft() {
        return prefNode.getInt(USER_WINDOW_LEFT, 0);
    }

    /**
     * Sets the previous X coordinate for the left edge of the window in user's preference store
     *
     * @param windowLeft                int position of window's x coordinate
     */
    private void setWindowLeft(int windowLeft) {
        prefNode.putInt(USER_WINDOW_LEFT, windowLeft);
    }

    /**
     * Returns the previous width of the window.
     *
     * @return                          int pixel width
     */
    public int getWindowWidth() {
        return prefNode.getInt(USER_WINDOW_WIDTH, defaultWidth);
    }

    /**
     * Sets the width of the window in user's preference store
     *
     * @param width                     int width of frame
     */
    private void setWindowWidth(int width) {
        prefNode.putInt(USER_WINDOW_WIDTH, width);
    }

    /**
     * Returns the previous height of the window.
     *
     * @return                          int pixel height
     */
    public int getWindowHeight() {
        return prefNode.getInt(USER_WINDOW_HEIGHT, defaultHeight);
    }

    /**
     * Sets the height of the window in user's preference store
     *
     * @param height                    int height of frame
     */
    private void setWindowHeight(int height) {
        prefNode.putInt(USER_WINDOW_HEIGHT, height);
    }

    /**
     * Returns whether the user's preferences were previously saved.
     *
     * @return                          boolean where true indicates the preferences already existed, default false.
     */
    public boolean getPrefsSet() {
        boolean hasKeys;
        try {
            hasKeys = (prefNode.keys().length > 0);
        } catch (BackingStoreException ex) {
            hasKeys = false;
        }
        return hasKeys;
    }

    /**
     * Clears the window placement preferences.
     *
     * @throws BackingStoreException
     */
    public void clear() throws BackingStoreException {
        prefNode.clear();
        prefsSet = false;
    }

    /**
     * Checks to see if the window is fully within the bounds of the screen
     * if not sets the window to be centered in the screen
     *
     * @param jFrame                    frame to be checked and repositioned if necessary.
     */
    private void setScreen(JFrame jFrame) throws HeadlessException {
        if (!prefsSet) {
            //passing null to setLocationRelativeTo centers the frame on the current screen.
            jFrame.setLocationRelativeTo(null);
        } else {
            Point currentWindowLocation = new Point();
            currentWindowLocation.setLocation(jFrame.getX(), jFrame.getY());

            Rectangle currentScreen = CalculateUsableDisplayBounds(currentWindowLocation);
            if (!currentScreen.contains(jFrame.getBounds())) {
                jFrame.setLocationRelativeTo(null);
            }

        }
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
        List<Point> displayCenterPoints = new ArrayList<>();
        List<Rectangle> displayBounds = new ArrayList<>();

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
            List<Integer> distances = new ArrayList<>();

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