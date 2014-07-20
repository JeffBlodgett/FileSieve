package FileSieve.BusinessLogic.FileEnumeration;

/**
 * Static factory class for acquiring FileEnumerator objects.
 *
 * todo why do you need a factory for this class? Did you have more than one type of  Enumerators?
 */
public class FileEnumeratorFactory {

    /**
     * Private constructor - only static methods will be available
     */
    private FileEnumeratorFactory() { }

    /**
     * Returns a FileEnumerator object for use in discovering files/folders in one or more source paths.
     *
     * @return  a FileEnumerator object
     */
    static public FileEnumerator getFileEnumerator() {
        return new FileDiscoverer();
    }

} // class FileEnumeratorFactory
