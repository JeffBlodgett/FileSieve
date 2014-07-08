package FileSieve.BusinessLogic.FileManagement;

/**
 * Factory class for acquiring FileManager instances
 */
public class FileManagerFactory {

    /**
     * Private constructor - class is a static factory class
     */
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

} // class FileManagerFactory
