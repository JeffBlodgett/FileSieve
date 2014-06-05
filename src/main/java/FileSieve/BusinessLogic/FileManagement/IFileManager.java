package FileSieve.BusinessLogic.FileManagement;

import java.nio.file.Path;

/**
 * Interface defining file management operations.
 */
public interface IFileManager {

    /**
     * Method for copying a file or empty folder to a target. Implementers should perform the copy operation
     * on a separate (non-blocking) thread.
     *
     * @param sourcePathname    pathname of file or folder to copy
     * @param targetPathname    pathname of file or folder to write
     * @param overwriteFile     indicates if existing target should be overwritten (applies only to files)
     */
    public void copyFile(Path sourcePathname, Path targetPathname, boolean overwriteFile);

} // class IFileManager
