package FileSieve.BusinessLogic.FileEnumeration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

/**
 * Interface defining methods for enumerating files/folders
 */
public interface FileEnumerator {

    /**
     * Acquires a list of discovered files and folders under a given pathname. The list is in the form of a Map with
     * keys set to discovered file/folder paths and values set to instances of the BasicFileAttributes class, which
     * contains attributes for each file/folder.
     *
     * @param rootPathname      pathname of folder to be searched
     * @param recursiveSearch   boolean parameter specifying if search should extend to subfolders
     * @return                  discovered files/folders and their BasicFileAttributes
     */
    public Map<Path, BasicFileAttributes> getPathnames(Path rootPathname, boolean recursiveSearch) throws IOException;

    /**
     * Convenience method (overload) for calling getPaths method with recursion enabled.
     *
     * @param rootPathname      pathname to be searched
     * @return                  discovered files/folders and their attributes
     */
    public Map<Path, BasicFileAttributes> getPathnames(Path rootPathname) throws IOException;
    
} // interface FileEnumerator
