package FileSieve.Persistence.Preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 *  Class for saving/retrieving selected paths for comparison
 */
public class SelectedPaths {
    private Preferences prefNode;
    private List<String> refPath;
    private String targetPath;

    private static final String DEFAULT_REFERENCEPATH = "DEFAULT_REFERENCEPATH";
    private static final String DEFAULT_TARGETPATH = "DEFAULT_TARGETPATH";
    private static final String MULTIPATH_SEPARATOR = "\t";

    /**
     * default constructor, gets the user preferences for this class
     */
    public SelectedPaths() {
        prefNode = Preferences.userNodeForPackage(getClass());
    }

    /**
     * initialize and set the reference paths.
     */
    public SelectedPaths(List<String> referencePathName, String targetPathName) {
        this();
        refPath = referencePathName;
        targetPath = targetPathName;
    }

    /**
     * Saves both reference and target paths
     */
    public void save() {
        setReferencePathNames(refPath);
        setTargetPathName(targetPath);
    }

    /**
     * Returns the previously selected reference path
     *
     * @return                          string reference path
     */
    public List<String> getReferencePathNames() {
        String savedPaths = prefNode.get(DEFAULT_REFERENCEPATH, "");
        return new ArrayList<>(Arrays.asList(savedPaths.split(MULTIPATH_SEPARATOR)));
    }

    /**
     * Sets selected reference paths
     *
     * @param pathNames                 A list of strings representing the selected reference paths.
     */
    private void setReferencePathNames(List<String> pathNames) {
        StringBuilder compiledPaths = new StringBuilder();

        for (String s : pathNames) {
            if (compiledPaths.length() > 0) {
                compiledPaths.append(MULTIPATH_SEPARATOR);
            }
            compiledPaths.append(s);
        }

        prefNode.put(DEFAULT_REFERENCEPATH, compiledPaths.toString());
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
     * Clears the selected path preferences.
     *
     * @throws BackingStoreException
     */
    public void clear() throws BackingStoreException {
        prefNode.clear();
    }
}
