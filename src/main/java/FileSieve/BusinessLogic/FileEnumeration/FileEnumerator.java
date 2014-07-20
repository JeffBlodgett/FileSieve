package FileSieve.BusinessLogic.FileEnumeration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;

// todo - great level of documentation here - nice work
/**
 * Package-private interface defining methods for enumeration of files and folders within given source paths.
 */
public interface FileEnumerator {

    /**
     * Returns a count for the number of files discovered during the most recently completed file enumeration.
     * The count excludes folders.
     *
     * @return  count of the number of discovered files from most recent enumeration
     */
    public int getFileCount();

    /**
     * Returns the sum of the bytes of the files discovered during the most recently completed file enumeration.
     *
     * @return  sum of the bytes of discovered files from most recent enumeration
     */
    public long getByteCount();

    /**
     * Returns a list of discovered files and folders amongst a list of provided pathnames. The returned list is in the
     * form of a LinkedHashMap with its keys set to discovered file/folder paths and its values set to instances of the
     * BasicFileAttributes class, containing attributes for each file or folder. The provided list of paths to
     * enumerate may contain files or folders. File paths in the passed list are added to the returned Map last
     * whereas folders are given priority as folders as searched. Folders and files within a particular parent folder
     * are ordered lexicographically.
     *
     * @param pathsToEnumerate  List<Path> containing pathnames of files and folders to be included in output list
     * @param recursiveSearch   boolean parameter indicating if searches should extend to subfolders
     * @return                  discovered files/folders and their BasicFileAttributes
     * @throws IOException      thrown if an I/O exception occurs
     */
    public Map<Path, BasicFileAttributes> getPathnames(List<Path> pathsToEnumerate, boolean recursiveSearch) throws IOException;

    /**
     * Convenience method (overload) for the getPathnames method. Works the same as the getPathnames(List<Path> , boolean)
     * version of this method but assumes a recursive search (boolean value of "true") for its second parameter.
     *
     * @param pathsToEnumerate  List<Path> containing pathnames of files and folders to be included in output list
     * @return                  discovered files/folders and their BasicFileAttributes
     * @throws IOException      thrown if an I/O exception occurs
     */
    public Map<Path, BasicFileAttributes> getPathnames(List<Path> pathsToEnumerate) throws IOException;

    /**
     * Convenience method (overload) for the getPathnames method. Works the same as the getPathnames(List<Path> , boolean)
     * version of this method but takes a reference to a single Path object, rather than a List<Path>, as it first
     * parameter.
     *
     * @param pathToEnumerate   a single Path within which to enumeration files and folders.
     * @param recursiveSearch   boolean parameter indicating if searches should extend to subfolders
     * @return                  discovered files/folders and their BasicFileAttributes
     * @throws IOException      thrown if an I/O exception occurs
     */
    Map<Path, BasicFileAttributes> getPathnames(Path pathToEnumerate, boolean recursiveSearch) throws IOException;

    /**
     * Convenience method (overload) for the getPathnames method. Works the same as the getPathnames(List<Path> , boolean)
     * version of this method but takes a reference to a single Path object, rather than a List<Path>, as its first
     * parameter and assumes a recursive search (boolean value of "true") for its second parameter.
     *
     * @param pathToEnumerate   a single Path within which to enumeration files and folders.
     * @return                  discovered files/folders and their BasicFileAttributes
     * @throws IOException      thrown if an I/O exception occurs
     */
    public Map<Path, BasicFileAttributes> getPathnames(Path pathToEnumerate) throws IOException;

} // interface FileEnumerator
