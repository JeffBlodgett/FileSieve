package FileSieve.BusinessLogic.FileManagement;

/**
 * Factory class for acquiring FileManager instances
 */
public class FileManagerFactory {

    private FileManagerFactory() { }

    /**
     * Returns a FileManager instance for handling common file management operations
     * Only a single worker thread is used per long-running operation (e.g. a file or folder copy).
     * The returned FileManager object's generic parameter of "Boolean" specifies the type of the object
     * returned by the "copyPathname" method.
     *
     * @return                  a FileManager<Boolean> instance
     */
    public static FileManager<Boolean> getFileManager() {
        return new ConcurrentFileManager(1);
    }

    /**
     * Returns a FileManager instance for handling common file management operations
     * TODO given time... not yet part of the public API
     *
     * @param workerThreads     maximum number of worker threads for long-running operations (e.g. a file or folder copy)
     * @return                  a FileManager<Boolean> instance
     */
    private static FileManager<Boolean> getFileManager(int workerThreads) {
        return new ConcurrentFileManager(workerThreads);
    }

} // class FileManagerFactory
