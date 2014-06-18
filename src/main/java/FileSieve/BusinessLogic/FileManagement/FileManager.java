package FileSieve.BusinessLogic.FileManagement;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface defining file management operations
 *
 * @param <T>   Type returned by the interface's copyProvider method
 */
public interface FileManager<T> {

    /**
     * Method for copying a file or creating a folder to/within a target pathname. Method may handle the copy operation
     * itself or return a provider (such as a SwingWorker) capable of handling the copy operation on a separate thread.
     *
     * @param sourcePathname    pathname of file or folder to copy
     * @param targetPathname    pathname of file or folder to create/write
     * @param recursionEnabled  recursive search for files within subfolders
     * @param overwriteExisting indicates if existing files in the target path should be overwritten
     * @param ifSizeDiffers     if "overwriteExisting" argument is true, overwrites existing files only if their size differs
     * @return                  implementer-defined object, for indicating copy success or returning a worker object
     *                          capable of handling a longer-running copy operation
     */
    public T copyProvider(Path sourcePathname, Path targetPathname, boolean recursionEnabled, boolean overwriteExisting, boolean ifSizeDiffers);

    /**
     * Method for deleting a file or folder
     *
     * @param pathname          pathname of file or folder to delete
     * @return                  boolean value indicating if file was successfully deleted
     * @throws IOException      thrown if an I/O error occurs
     */
    public boolean deletePathname(Path pathname) throws IOException;

    /**
     * Opens the given file or folder using the application registered on the host system for opening files of the
     * pathname type. The default file browser is used if the pathname specified is a directory.
     *
     * @param pathname          pathname of a file or folder to be opened
     * @throws IOException      thrown if the specified file has no associated application or the associated application fails to be launched
     */
    public void openPathname(Path pathname) throws IOException;

} // interface FileManager
