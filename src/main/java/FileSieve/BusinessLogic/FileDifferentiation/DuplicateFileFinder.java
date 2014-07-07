package FileSieve.BusinessLogic.FileDifferentiation;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of a FileDifferentiator for identifying duplicate files amongst a passed list (Map) of Path objects.
 * This class has package-private access.
 */
class DuplicateFileFinder implements FileDifferentiator, FileHashCalculator {

    private FileHashCalculator fileHashCalculator = null;

    protected DuplicateFileFinder() { }

    protected DuplicateFileFinder(FileHashCalculator fileHashCalculator) {
        this.setFileHashCalculator(fileHashCalculator);
    }

    @Override
    public List<SimpleImmutableEntry<String, List<File>>> getDuplicatedFiles(Map<Path, BasicFileAttributes> pathnames) {
        if ((pathnames == null) || (pathnames.size() == 0)) {
            throw new IllegalArgumentException("no pathnames provided");
        }

        // List to be returned
        List<SimpleImmutableEntry<String, List<File>>> result;

        // HashMap to be used internally in determining if a file has a duplicate. The keys for this Map are hash codes
        // generated for the pathnames
        Map<Integer, Path> hashMap = new HashMap<Integer, Path>(pathnames.size());

        // HashMap to be used internally in accumulating duplicates
        Map<Integer, List<Path>> duplicates = new HashMap<Integer, List<Path>>(pathnames.size() / 2);

        synchronized(pathnames) {
            for (Path path : pathnames.keySet()) {
                Path key = path;
                BasicFileAttributes value = pathnames.get(path);
                int hash;

                if ((key != null) && (value != null) && (value.isRegularFile())) {
                    if (fileHashCalculator == null) {
                        hash = calculateHash(key, value);
                    } else {
                        hash = fileHashCalculator.calculateHash(key, value);
                    }

                    if (hashMap.containsKey(hash)) {
                        if (!duplicates.containsKey(hash)) {
                            List<Path> newList = new ArrayList<Path>(4);
                            newList.add(hashMap.get(hash));
                            newList.add(key);
                            duplicates.put(hash, newList);
                        } else {
                            List<Path> existingList = duplicates.get(hash);
                            existingList.add(key);
                        }
                    } else {
                        hashMap.put(hash, key);
                    }
                }
            }
        }

        // Initialize the resulting HashSet
        result = new ArrayList<SimpleImmutableEntry<String, List<File>>>(duplicates.size());

        for (List<Path> list : duplicates.values()) {
            String fileName = list.get(0).getFileName().toString();
            List<File> files = new ArrayList<File>(list.size());

            for (Path path : list) {
                files.add(path.toFile());
            }

            result.add(new SimpleImmutableEntry<String, List<File>>(fileName, files));
        }

        return result;
    }

    @Override
    public void setFileHashCalculator(FileHashCalculator fileHashCalculator) {
        this.fileHashCalculator = fileHashCalculator;
    }

    /**
     * Default implementation for calculating a hash code for a file from attributes of its Path and BasicFileAttributes
     * objects. This implementation factors in only the name and byte length of a file, the latter coming from the
     * passed BasicFileAttributes for the file.
     *
     * @param path                  Path object for which to generate a hash code
     * @param basicFileAttributes   BasicFileAttributes object for the passed Path
     * @return                      a hash code suitable for use by the class in determining if two or more files
     *                              are equal to equal to each other
     */
    @Override
    public int calculateHash(Path path, BasicFileAttributes basicFileAttributes) {
        int result = 17;
        result = 31 * result + path.getFileName().hashCode();
        result = 31 * result + (int)(basicFileAttributes.size() ^ (basicFileAttributes.size() >>> 32));
        return result;
    }

} // class DuplicateFileFinder implements FileDifferentiator, FileHashCalculator
