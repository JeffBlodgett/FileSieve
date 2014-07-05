package FileSieve.BusinessLogic.FileDifferentiation;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Package-private interface specializing in the identification of duplicated files.
 */
interface FileDifferentiator {

    /**
     * Returns a list of duplicated files within a given list of files.
     *
     * @param pathnames     a Map containing paths and basic file attributes of files to be analyzed for duplicates
     * @return              a list containing key-value pairs with the name of a files found to have at least one
     *                      duplicate as the keys and lists of pathnames of the duplicated files as values
     */
    public List<SimpleImmutableEntry<String, List<File>>> getDuplicatedFiles(Map<Path, BasicFileAttributes> pathnames);

//    /**
//     * Returns a list of files that are missing or otherwise different from those in a reference folder.
//     *
//     * @param referencePathName         pathname of reference folder
//     * @param targetPathName            pathname of folder containing files to be compared to those in the reference
//     * @param <T>                       class extending BasicFileAttributes class, inheriting basic
//     *                                  attributes, and encompassing reason(s) for file difference(s)
//     * @return
//     */
//    public <T extends BasicFileAttributes> Map<Path, T> getFileDifferences(Map<Path, BasicFileAttributes> referencePathName, Map<Path, BasicFileAttributes> targetPathName);

} // interface FileDifferentiator
