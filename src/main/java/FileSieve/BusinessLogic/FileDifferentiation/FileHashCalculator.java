package FileSieve.BusinessLogic.FileDifferentiation;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Defines a function object with a hashCode method to be used for calculating the
 */
public interface FileHashCalculator {

    public int calculateHash(Path path, BasicFileAttributes bfa);

}
