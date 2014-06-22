package FileSieve.BusinessLogic.FileManagement;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface defining an operation for a file copier. Interface is package-private.
 *
 * @param <T>   The type of the object returned by the copyPathname method. In a simple implementation this may
 *              simply be a Boolean object that indicates if the copy operation was started or completed successfully.
 */
interface FileCopier<T> {

    /**
     * Method for copying a file or creating a folder to/within a target pathname. Method may handle the copy operation
     * itself or return a provider (such as a SwingWorker) capable of handling the copy operation on a separate thread.
     *
     * @param sourcePathname        pathname of file or folder to copy
     * @param targetPathname        pathname of file or folder to create/write
     * @param recursionEnabled      recursive search for files within subfolders
     * @param overwriteExisting     indicates if existing files in the target path should be overwritten
     * @param ifSizeDiffers         if "overwriteExisting" argument is true, overwrites existing files only if their size differs
     * @return                      implementer defined return type
     * @throws java.io.IOException  implementer defined reason
     */
    public T copyPathname(Path sourcePathname, Path targetPathname, boolean recursionEnabled, boolean overwriteExisting, boolean ifSizeDiffers) throws IOException;;

} // interface FileManager
