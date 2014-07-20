package FileSieve.BusinessLogic.FileDifferentiation;

/**
 * Static factory class for acquiring FileDifferentiator instances.
 */
public class FileDifferentiatorFactory {

    /**
     * Private constructor - static factory class
     */
    private FileDifferentiatorFactory() { }

    /**
     * Acquires an instance of a FileDifferentiator with a default (internal) FileHashCalculator object that
     * dictates use of a file's name and byte length in determining if it has duplicates. The object's
     * "setFileHashCalculator" method can be used to provide a customer FileHashCalculator after acquisition.
     *
     * @return                      instance of a FileDifferentiator
     */
    public static FileDifferentiator getFileDifferentiator() {
        return new DuplicateFileFinder();
    }

    /**
     * Acquires an instance of a FileDifferentiator with its FileHashCalculator set to the provided instance. The
     * FileHashCalculator is a function object provided a method by which a hash code can be generated from a
     * combination of Path or BasicFileAttributes attributes.
     *
     * @param fileHashCalculator    a FileHashCalculator function object used in determining file duplicity
     * @return                      instance of a FileDifferentiator
     */
    // todo unused method with unused argument
    public static FileDifferentiator getFileDifferentiator(FileHashCalculator fileHashCalculator) {
        return new DuplicateFileFinder();
    }

} // class FileDifferentiatorFactory
