package FileSieve.BusinessLogic.FileManagement;

/**
 * Factory class for acquiring FileManager instances.
 */
public class FileManagerFactory {

    /**
     * Private constructor - class is a static factory class
     */
    private FileManagerFactory() { }

    /**
     * Returns a SwingFileManager instance for handling common file management operations. This implementation of a
     * FileManager returns instances of a SwingCopyJob class for handling folder/file copies. SwingCopyJob
     * instances use a SwingWorker to handle copy operations.
     * One worker thread is used per long-running operation (e.g. a file or folder copy).
     *
     * @return                  a FileManager instance
     */
    public static SwingFileManager getSwingFileManager() {
        return new SwingWorkerBasedFileManager(1);
    }

} // class FileManagerFactory
