package FileSieve.BusinessLogic.FileDifferentiation;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

/**
 * Interface specializing in identification of file differences.
 */
public interface IFileDifferentiator {

    /**
     * Returns a list of duplicated files within a given list of files.
     *
     * @param pathNames                 list of files to be searched for duplicates
     * @return                          list of file lists - each list contains files identified as duplicates of each other
     */
    public Map<Path, BasicFileAttributes> getDuplicatedFiles(Map<Path, BasicFileAttributes> pathNames);

    /**
     * Returns a list of files that are missing or otherwise different from those in a reference folder.
     *
     * @param referencePathName         pathname of reference folder
     * @param targetPathName            pathname of folder containing files to be compared to those in the reference
     * @param <T>                       class extending BasicFileAttributes class, inheriting basic
     *                                  attributes, and encompassing reason(s) for file difference(s)
     * @return
     */
    public <T extends BasicFileAttributes> Map<Path, T> getFileDifferences(Map<Path, BasicFileAttributes> referencePathName, Map<Path, BasicFileAttributes> targetPathName);

} // interface IFileDifferentiator
