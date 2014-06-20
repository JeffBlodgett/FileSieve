package FileSieve.BusinessLogic.FileManagement;

/**
 * Factory class for acquiring FileManager instances
 */
public class FileManagerFactory {

    private FileManagerFactory() { }

    /**
     * Returns a FileManager instance for handling common file management operations
     * Only a single worker thread is used per long-running operation (e.g. a file or folder copy)
     *
     * @return                  a FileManager instance
     */
    public static FileManager getFileManager() {     // BackgroundCopyWorker
        return new ConcurrentFileManager(1);
    }

    /**
     * Returns a FileManager instance for handling common file management operations
     *
     * @param workerThreads     maximum number of worker threads for long-running operations (e.g. a file or folder copy)
     * @return                  a FileManager instance
     */
    private static FileManager getFileManager(int workerThreads) {     // BackgroundCopyWorker

        // TODO Under development - not yet part of the public API

        return new ConcurrentFileManager(workerThreads);
    }

} // class FileManagerFactory
