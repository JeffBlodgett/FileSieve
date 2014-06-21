package FileSieve.BusinessLogic.FileManagement;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface defining file management operations
 */
interface FileCopier {

    /**
     * Method for copying a file or creating a folder to/within a target pathname. Method may handle the copy operation
     * itself or return a provider (such as a SwingWorker) capable of handling the copy operation on a separate thread.
     *
     * @param sourcePathname    pathname of file or folder to copy
     * @param targetPathname    pathname of file or folder to create/write
     * @param recursionEnabled  recursive search for files within subfolders
     * @param overwriteExisting indicates if existing files in the target path should be overwritten
     * @param ifSizeDiffers     if "overwriteExisting" argument is true, overwrites existing files only if their size differs
     * @return                  boolean true if the copy operation is begun or has succeeded, false if not
     */
    public boolean copyPathname(Path sourcePathname, Path targetPathname, boolean recursionEnabled, boolean overwriteExisting, boolean ifSizeDiffers);

} // interface FileManager
