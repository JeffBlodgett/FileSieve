package FileSieve.BusinessLogic.FileManagement;

import javax.swing.SwingWorker;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * Interface defining file management operations
 */
public interface FileManager<T> {

    /**
     * Method for copying a file or creating a folder to/within a target pathname on the current thread.
     *
     * @param sourcePathname    pathname of file or folder to copy
     * @param targetPathname    pathname of file or folder to create/write
     * @param overwriteFile     indicates if an existing target file should be overwritten
     * @return                  implementer-defined object, for indicating copy success or returning an object
     *                          capable of handling a long-running copy operation
     */
    public T copyPathname(Path sourcePathname, Path targetPathname, boolean overwriteFile);

    /**
     * Method for deleting a file or folder
     *
     * @param pathname          pathname of file or folder to delete
     * @return                  boolean value indicating if file was successfully deleted
     * @throws IOException      thrown if an I/O error occurs
     */
    public boolean deleteFile(Path pathname) throws IOException;

    /**
     * Opens the given file or folder using the application registered on the host system for opening files of the
     * pathname type. The default file browser is used if the pathname specified is a directory.
     *
     * @param pathname          pathname of a file or folder to be opened
     * @throws IOException      thrown if the specified file has no associated application or the associated application fails to be launched
     */
    public void openPathname(Path pathname) throws IOException;

} // class FileManager
