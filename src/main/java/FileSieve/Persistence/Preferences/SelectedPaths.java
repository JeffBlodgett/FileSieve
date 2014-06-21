package FileSieve.Persistence.Preferences;

import java.util.prefs.Preferences;

/**
 *  Class for saving/retrieving selected paths for comparison
 */
public class SelectedPaths {
    private Preferences prefNode;
    private String refPath;
    private String targetPath;

    /**
     * .
     */
    public SelectedPaths() {
        Preferences root = Preferences.userRoot();
        prefNode = root.node("/com/filesieve/selectedpaths");
    }

    /**
     * initialize and set the reference paths.
     */
    public SelectedPaths(String referencePathName, String targetPathName) {
        this();
        refPath = referencePathName;
        targetPath = targetPathName;
    }

    public void save() {
        setReferencePathName(refPath);
        setTargetPathName(targetPath);
    }

    /**
     * Returns the previously selected reference path
     *
     * @return                          string reference path
     */
    public String getReferencePathName() {
        return prefNode.get(DEFAULT_REFERENCEPATH, "");
    }

    /**
     * Sets selected reference path
     */
    private void setReferencePathName(String pathName) {
        prefNode.put(DEFAULT_REFERENCEPATH, pathName);
    }

    /**
     * Returns the previously selected target path
     *
     * @return                          string target path
     */
    public String getTargetPathName() {
        return prefNode.get(DEFAULT_TARGETPATH, "");
    }

    /**
     * Sets selected target path
     */
    private void setTargetPathName(String pathName) {
        prefNode.put(DEFAULT_TARGETPATH, pathName);
    }

    private static final String DEFAULT_REFERENCEPATH = "DEFAULT_REFERENCEPATH";
    private static final String DEFAULT_TARGETPATH = "DEFAULT_TARGETPATH";
}
