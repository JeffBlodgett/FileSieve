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

class FileDiscoverer implements FileEnumerator {

    @Override
    public Map<Path, BasicFileAttributes> getPathnames(Path rootPathname, boolean recursiveSearch) throws IOException {
        if ((rootPathname == null) || (!Files.exists(rootPathname))) throw new IllegalArgumentException("provided rootPathname is null or abstracts a non-existent file or folder");

        Map<Path, BasicFileAttributes> pathMap = Collections.synchronizedMap(new LinkedHashMap<Path, BasicFileAttributes>(50));
        List<Path> directoryContents = new ArrayList<Path>(50);

        if (Files.isRegularFile(rootPathname, LinkOption.NOFOLLOW_LINKS)) {
            pathMap.put(rootPathname, Files.readAttributes(rootPathname, BasicFileAttributes.class));
        } else {
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(rootPathname)) {
                for (Path path : dirStream) {
                    directoryContents.add(path);
                }
            }

            if (directoryContents.size() > 0) {
                Path[] arrayOfPaths = directoryContents.toArray(new Path[directoryContents.size()]);
                Arrays.sort(arrayOfPaths, FileComparator.getInstance());
                directoryContents.clear();

                for (Path path : arrayOfPaths) {
                    pathMap.put(path, Files.readAttributes(path, BasicFileAttributes.class));
                }

                if (recursiveSearch) {
                    for (Path path : arrayOfPaths) {
                        if (Files.isDirectory(path)) {
                            pathMap.putAll(getPathnames(path, true));
                        }
                    }
                }
            }
        }

        return pathMap;
    }

    @Override
    public Map<Path, BasicFileAttributes> getPathnames(Path pathname) throws IOException{
        return getPathnames(pathname, true);
    }

    /**
     * File comparator (function object) for use by instances in sorting an array of Path objects
     * lexicographically by name. Protected access, rather than private, for testing purposes only.
     */
    protected static class FileComparator implements Comparator<Path> {

        public static final FileComparator INSTANCE = new FileComparator();

        public static FileComparator getInstance() {
            return INSTANCE;
        }

        private FileComparator() { }

        public int compare(Path path1, Path path2) {
            int result = 0;

//            Path name1 = path1.getFileName();
//            Path name2 = path2.getFileName();

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

}
