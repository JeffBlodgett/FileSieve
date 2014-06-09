package FileSieve.BusinessLogic.FileManagement;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Basic file management with default implementations for deleting or opening files and folders
 *
 * @param <T>   A type, unused by the methods of this abstract class
 */
public abstract class BasicFileManager<T> implements FileManager<T> {

    /**
     * Deletes a given file or folder
     *
     * @param pathname                          pathname of file or folder to delete
     * @return                                  true if the file or folder was deleted, false if it did not exist
     * @throws NullPointerException             thrown if provided pathname is null (RunTimeException)
     * @throws SecurityException                thrown if the SecurityManager.checkDelete method throws a SecurityException (RunTimeException)
     * @throws DirectoryNotEmptyException       thrown if pathname is a directory and is not empty (IOException)
     * @throws IOException                      thrown if some other I/O error occurs
     */
    public boolean deleteFile(Path pathname) throws NullPointerException, SecurityException, DirectoryNotEmptyException, IOException  {
        if (pathname == null) throw new NullPointerException();
        return Files.deleteIfExists(pathname);
    }

    /**
     * Opens the given file or folder using the application registered on the host system for opening files of the
     * pathname type. The default file browser is used if the pathname specified is a directory.
     *
     * @param pathname                          pathname of a file or folder to be opened
     * @throws NullPointerException             thrown if provided pathname is null (RunTimeException)
     * @throws UnsupportedOperationException    thrown if the current platform does not support the Desktop class or Desktop.Action.OPEN action ((RunTimeException)
     * @throws IOException                      thrown if the specified file has no associated application or the associated application fails to be launched
     */
    public void openPathname(Path pathname) throws NullPointerException, UnsupportedOperationException, IOException {
        if (pathname == null) throw new NullPointerException();

        Desktop desktop = null;
        if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.OPEN)) {
            desktop = Desktop.getDesktop();
            desktop.open(pathname.toFile());
        } else {
            throw new UnsupportedOperationException();
        }
    }

} // abstract class BasicFileManager<T> implements FileManager<T>
