package FileSieve.Persistence.Preferences;

import java.awt.*;
import javax.swing.JFrame;
import java.util.prefs.Preferences;

/**
 * Class for saving/retrieving properties of the user window
 */
public class WindowPlacementPreferences {
    private Preferences prefNode;
    private JFrame frame;

    public WindowPlacementPreferences() {
        Preferences root = Preferences.userRoot();
        prefNode = root.node("/com/filesieve/windowposition");
    }

    public WindowPlacementPreferences(JFrame jframe) {
        this();
        frame = jframe;
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
            setScreen();
        }
    }

    /**
     * Gets values from storage and sets them into the JFrame passed in.
     *
     * @param jframe                    JFrame to position based on user preferences
     */
    public void load(JFrame jframe) {
        jframe.setBounds(getWindowLeft(), getWindowTop(), getWindowWidth(), getWindowHeight());
        //TODO: code to set the frame's current screen
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
        return prefNode.getInt(USER_WINDOW_WIDTH, 0);
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
        return prefNode.getInt(USER_WINDOW_HEIGHT, 0);
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
     * Returns the previous screen hosting the window.
     *
     * @return                          int screen number
     */
    public int getScreen() {
        return prefNode.getInt(USER_SCREEN, 0);
    }

    /**
     * Sets the screen holding the window in user's preference store
     */
    private void setScreen() {
        //frame must be instantiated and visible to get current screen.
        if (frame != null && frame.isVisible()) {
            //TODO: Get screen number from frame object
        }
        prefNode.putInt(USER_SCREEN, 0);
    }

    private static final String USER_WINDOW_TOP = "USER_WINDOW_TOP";
    private static final String USER_WINDOW_LEFT = "USER_WINDOW_LEFT";
    private static final String USER_WINDOW_WIDTH = "USER_WINDOW_WIDTH";
    private static final String USER_WINDOW_HEIGHT = "USER_WINDOW_HEIGHT";
    private static final String USER_SCREEN = "USER_SCREEN";
}