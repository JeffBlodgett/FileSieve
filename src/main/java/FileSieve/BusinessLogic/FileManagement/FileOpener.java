package FileSieve.BusinessLogic.FileManagement;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface defining an operation for a file opener. Interface is package-private.
 */
interface FileOpener {

    /**
     * Opens the given file or folder using the application registered on the host system for opening files of the
     * pathname type. The default file browser is used if the pathname specified is a directory.
     *
     * @param pathname                  pathname of a file or folder to be opened
     * @throws java.io.IOException      implementer defined reason
     */
    public void openPathname(Path pathname) throws IOException;

} // interface FileOpener
