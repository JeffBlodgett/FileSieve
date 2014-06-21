package FileSieve.BusinessLogic.FileManagement;

import java.io.IOException;
import java.nio.file.Path;

interface FileOpener {

    /**
     * Opens the given file or folder using the application registered on the host system for opening files of the
     * pathname type. The default file browser is used if the pathname specified is a directory.
     *
     * @param pathname          pathname of a file or folder to be opened
     * @throws java.io.IOException      thrown if the specified file has no associated application or the associated application fails to be launched
     */
    public void openPathname(Path pathname) throws IOException;

} // interface FileOpener
