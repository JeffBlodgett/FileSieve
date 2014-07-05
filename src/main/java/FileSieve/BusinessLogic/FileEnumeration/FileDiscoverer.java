package FileSieve.BusinessLogic.FileEnumeration;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for the discovery of files and folders in given search paths
 */
public class FileDiscoverer implements FileEnumerator {

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
    @Override
    public Map<Path, BasicFileAttributes> getPathnames(List<Path> pathsToEnumerate, boolean recursiveSearch) throws IOException {
        if ((pathsToEnumerate == null) || (pathsToEnumerate.size() == 0)) {
            throw new IllegalArgumentException("no paths to existing files or folder were provided for enumeration");
        }

        // Map to be returned
        Map<Path, BasicFileAttributes> pathMap;

        // Test to ensure Path objects abstract existing files or folders
        List<Path> sourcePaths = new ArrayList<Path>(pathsToEnumerate.size());
        for (Path path : pathsToEnumerate) {
            if ((path != null) && (Files.exists(path, LinkOption.NOFOLLOW_LINKS))) {
                sourcePaths.add(path);
            }
        }

        // Sort paths lexicographically, with folders first
        Path[] rootPathArray = null;
        if (sourcePaths.size() == 0) {
            throw new IllegalArgumentException("no path(s) to existing files or folders were provided for enumeration");
        } else {
            rootPathArray = sourcePaths.toArray(new Path[sourcePaths.size()]);
            Arrays.sort(rootPathArray, FileComparator.getInstance());
            sourcePaths.clear();
        }

        // Initialize Map... it is time to do so if we got this far
        pathMap = Collections.synchronizedMap(new LinkedHashMap<Path, BasicFileAttributes>(50));

        for (Path rootPath : rootPathArray) {
            List<Path> directoryContents = new ArrayList<Path>(25);

            if (Files.isRegularFile(rootPath, LinkOption.NOFOLLOW_LINKS)) {
                // Add file path to Map
                pathMap.put(rootPath, Files.readAttributes(rootPath, BasicFileAttributes.class));
            } else {
                // Enumerate contents of directory
                try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(rootPath)) {
                    for (Path path : dirStream) {
                        directoryContents.add(path);
                    }
                }

                if (directoryContents.size() > 0) {
                    // Sort paths lexicographically, with folders first
                    Path[] pathsInDirectory = directoryContents.toArray(new Path[directoryContents.size()]);
                    Arrays.sort(pathsInDirectory, FileComparator.getInstance());
                    directoryContents.clear();

                    // Add paths to Map
                    for (Path path : pathsInDirectory) {
                        pathMap.put(path, Files.readAttributes(path, BasicFileAttributes.class));
                    }

                    // Recursively call method with current directory as the sole path to enumerate
                    if (recursiveSearch) {
                        for (Path path : pathsInDirectory) {
                            if (Files.isDirectory(path)) {
                                pathMap.putAll(getPathnames(path, true));
                            }
                        }
                    }
                }
            }
        }

        return pathMap;
    }

    /**
     * Convenience method (overload) for the getPathnames method. Works the same as the getPathnames(List<Path> , boolean)
     * version of this method but assumes a recursive search (boolean value of "true" for second parameter).
     *
     * @param pathsToEnumerate  List<Path> containing pathnames of files and folders to be included in output list
     * @return                  discovered files/folders and their BasicFileAttributes
     * @throws IOException      thrown if an I/O exception occurs
     */
    @Override
    public Map<Path, BasicFileAttributes> getPathnames(List<Path> pathsToEnumerate) throws IOException{
        return getPathnames(pathsToEnumerate, true);
    }

    /**
     * Convenience method (overload) for the getPathnames method. Works the same as the getPathnames(List<Path> , boolean)
     * version of this method but takes a reference to a single Path object, rather than a List<Path>, as it first
     * parameter.
     *
     * @param pathToEnumerate   a single Path within which to enumeration files and folders.
     * @return                  discovered files/folders and their BasicFileAttributes
     * @throws IOException      thrown if an I/O exception occurs
     */
    @Override
    public Map<Path, BasicFileAttributes> getPathnames(Path pathToEnumerate, boolean recursiveSearch) throws IOException {
        if ((pathToEnumerate == null) || (!Files.exists(pathToEnumerate, LinkOption.NOFOLLOW_LINKS))) {
            throw new IllegalArgumentException("no path to existing an file or folder was provided for enumeration");
        }

        List<Path> paths = new ArrayList<Path>(1);
        paths.add(pathToEnumerate);

        return getPathnames(paths, recursiveSearch);
    }

    /**
     * Convenience method (overload) for the getPathnames method. Works the same as the getPathnames(List<Path> , boolean)
     * version of this method but takes a reference to a single Path object, rather than a List<Path>, as its first
     * parameter and assumes a recursive search (boolean value of "true") for its second parameter.
     *
     * @param pathToEnumerate   a single Path within which to enumeration files and folders.
     * @return                  discovered files/folders and their BasicFileAttributes
     * @throws IOException      thrown if an I/O exception occurs
     */
    public Map<Path, BasicFileAttributes> getPathnames(Path pathToEnumerate) throws IOException {
        if ((pathToEnumerate == null) || (!Files.exists(pathToEnumerate, LinkOption.NOFOLLOW_LINKS))) {
            throw new IllegalArgumentException("no path to existing an file or folder was provided for enumeration");
        }

        List<Path> paths = new ArrayList<Path>(1);
        paths.add(pathToEnumerate);

        return getPathnames(paths, true);
    }

    /**
     * File comparator (function object) for use by instances in sorting an array of Path objects
     * lexicographically by name, with folder names listed first and file names listed second.
     */
    private static class FileComparator implements Comparator<Path> {

        public static final FileComparator INSTANCE = new FileComparator();

        public static FileComparator getInstance() {
            return INSTANCE;
        }

        private FileComparator() { }

        public int compare(Path path1, Path path2) {
            int result = 0;

            if (Files.isDirectory(path1) && Files.isRegularFile(path2, LinkOption.NOFOLLOW_LINKS)) {
                result = -1;
            } else if (Files.isRegularFile(path1, LinkOption.NOFOLLOW_LINKS) && Files.isDirectory(path2)) {
                result = 1;
            } else {
                result = path1.compareTo(path2);
            }

            return result;
        }

    } // class FileComparator implements Comparator<Path>

} // class FileDiscoverer implements FileEnumerator
