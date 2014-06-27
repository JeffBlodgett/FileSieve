package FileSieve.BusinessLogic.FileManagement;

/**
 * Factory class for acquiring FileManager instances
 */
public class FileManagerFactory {

    private FileManagerFactory() { }

    /**
     * Returns a FileManager instance for handling common file management operations.
     * One worker thread is used per long-running operation (e.g. a file or folder copy).
     *
     * @return                  a FileManager instance
     */
    public static SwingFileManager getSwingFileManager() {
        return new SwingWorkerBasedFileManager(1);
    }

    /**
     * Returns a FileManager instance for handling common file management operations
     * TODO given time... not yet part of the public API
     *
     * @param workerThreads     maximum number of worker threads for long-running operations (e.g. a file or folder copy)
     * @return                  a FileManager<Boolean> instance
     */
    private static SwingFileManager getSwingFileManager(int workerThreads) {
        return new SwingWorkerBasedFileManager(workerThreads);
    }

} // class FileManagerFactory
