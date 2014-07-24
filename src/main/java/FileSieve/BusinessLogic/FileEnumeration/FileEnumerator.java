package FileSieve.BusinessLogic.FileEnumeration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;

/**
 * Defines methods for enumeration of files and folders within given source paths.
 */
public interface FileEnumerator {

    /**
     * Returns a count of the number of files discovered during the most recently completed file enumeration.
     * The count excludes folders.
     *
     * @return  a count of the number of discovered files from the most recent path discovery
     */
    public int getFileCount();

    /**
     * Returns the sum of the bytes of the files discovered during the most recently completed path discovery.
     *
     * @return  sum of the bytes of discovered files from the most recent path discovery
     */
    public long getByteCount();

    /**
     * Returns a list of discovered folders and files amongst a list of provided pathnames, including the paths of
     * empty folders. The returned Map is a LinkedHashMap that maintains insertion order while also preventing
     * duplicate keys. The map's keys are the discovered folder/file paths, while values are set to instances of the
     * BasicFileAttributes class, containing attributes for each folder or file. The provided list of paths to
     * enumerate may contain folders or files. File paths in the passed list are, effectively, added to the returned
     * Map since folders are given order priority in the returned Map. Folders, followed by files, within each
     * discovered folder are ordered lexicographically.
     *
     * WARNING:
     * The Paths in the returned Map are instances of "DiscoveredPath". Some methods of the java.io.File and
     * java.nio.file.Files classes may throw exceptions if passed a DiscoveredPath. A DiscoveredPath's getPath() method
     * may be used to extract the decorated Path as produced by the default file system provider.
     *
     * @param pathsToEnumerate  list of paths with the pathnames of folders and specific files to be included in the returned Map
     * @param recursiveSearch   boolean parameter indicating if path discovery should extend to subfolders
     * @return                  DiscoveredPath objects, decorating Paths which specify discovered folders/files, and their Path's BasicFileAttributes
     * @throws IOException      thrown if an I/O exception occurs
     */
    public Map<Path, BasicFileAttributes> getPathnames(List<Path> pathsToEnumerate, boolean recursiveSearch) throws IOException;

    /**
     * Convenience method (overload) for the getPathnames method. Works the same as the getPathnames(List<Path> , boolean)
     * version of this method but assumes a recursive search (boolean value of "true") for its second parameter.
     *
     * @param pathsToEnumerate  list of paths with the pathnames of folders and specific files to be included in the returned Map
     * @return                  discovered files/folders and their BasicFileAttributes
     * @throws IOException      thrown if an I/O exception occurs
     */
    public Map<Path, BasicFileAttributes> getPathnames(List<Path> pathsToEnumerate) throws IOException;

    /**
     * Convenience method (overload) for the getPathnames method. Works the same as the getPathnames(List<Path> , boolean)
     * version of this method but takes a reference to a single Path object, rather than a List<Path>, as it first
     * parameter.
     *
     * @param pathToEnumerate   a single Path within which to discover folders/files
     * @param recursiveSearch   boolean parameter indicating if searches should extend to subfolders
     * @return                  discovered folders/files and their BasicFileAttributes
     * @throws IOException      thrown if an I/O exception occurs
     */
    Map<Path, BasicFileAttributes> getPathnames(Path pathToEnumerate, boolean recursiveSearch) throws IOException;

    /**
     * Convenience method (overload) for the getPathnames method. Works the same as the getPathnames(List<Path> , boolean)
     * version of this method but takes a reference to a single Path object, rather than a List<Path>, as its first
     * parameter and assumes a recursive search (boolean value of "true") for its second parameter.
     *
     * @param pathToEnumerate   a single Path within which to discover files and folders.
     * @return                  discovered files/folders and their BasicFileAttributes
     * @throws IOException      thrown if an I/O exception occurs
     */
    public Map<Path, BasicFileAttributes> getPathnames(Path pathToEnumerate) throws IOException;

} // interface FileEnumerator
