package FileSieve.BusinessLogic.FileEnumeration;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

/**
 * Interface defining methods for enumerating files/folders
 */
public interface IFileEnumerator {

    /**
     * Acquires a list of discovered files and folders under a given pathname. The list is in the form of a Map with
     * keys set to discovered file/folder paths and values set to instances of the BasicFileAttributes class, which
     * contains attributes for each file/folder.
     *
     * @param pathname                  pathname of folder to be searched
     * @param searchSubFolders          boolean parameter specifying if search should extend to subfolders
     * @return                          discovered files/folders and their attributes
     */
    public Map<Path, BasicFileAttributes> getPaths(Path pathname, boolean searchSubFolders);

    /**
     * Convenience method (overload) for calling getPaths method with recursion enabled.
     *
     * @param pathname                  pathname to be searched
     * @return                          discovered files/folders and their attributes
     */
    public Map<Path, BasicFileAttributes> getPaths(String pathname);
    
} // interface IFileEnumerator
