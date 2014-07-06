package FileSieve.BusinessLogic.FileDifferentiation;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Defines method(s) for a function object for calculating a hash code from a Path object and its BasicFileAttributes.
 * The hash code should be suitable for comparing two or more files for equality (as defined by the implementer).
 */
public interface FileHashCalculator {

    public int calculateHash(Path path, BasicFileAttributes basicFileAttributes);

}
