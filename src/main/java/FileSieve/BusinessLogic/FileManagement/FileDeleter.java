package FileSieve.BusinessLogic.FileManagement;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface defining an operation for a file deleter. Interface is package-private.
 */
interface FileDeleter {

    /**
     * Method for deleting a file or folder
     *
     * @param pathname                  pathname of file or folder to delete
     * @return                          boolean value indicating if file was successfully deleted
     * @throws java.io.IOException      implementer defined reason
     */
    public boolean deletePathname(Path pathname) throws IOException;

} // interface FileDeleter
