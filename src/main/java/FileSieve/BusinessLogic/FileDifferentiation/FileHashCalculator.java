package FileSieve.BusinessLogic.FileDifferentiation;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Defines method(s) to be implemented by a function object for calculating a hash code for a file from attributes of
 * the file's Path and BasicFileAttributes objects. The hash code should be suitable for comparing two or more files
 * for equality per the implementation specifications.
 */
public interface FileHashCalculator {

    /**
     * Calculates a hash code for a file from attributes of the file's Path and BasicFileAttributes objects. The
     * hash code should be suitable for comparing two or more files for equality per the implementer's specifications.
     *
     * @param path                  reference to a Path instance abstracting a file
     * @param basicFileAttributes   a BasicFileAttributes object for the file
     * @return                      a calculated hash code for the file
     */
    public int calculateHash(Path path, BasicFileAttributes basicFileAttributes);

} // interface FileHashCalculator
