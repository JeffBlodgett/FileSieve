package FileSieve.BusinessLogic.FileManagement;

import java.io.IOException;
import java.nio.file.Path;

interface FileDeleter {

    /**
     * Method for deleting a file or folder
     *
     * @param pathname          pathname of file or folder to delete
     * @return                  boolean value indicating if file was successfully deleted
     * @throws java.io.IOException      thrown if an I/O error occurs
     */
    public boolean deletePathname(Path pathname) throws IOException;

} // interface FileDeleter
