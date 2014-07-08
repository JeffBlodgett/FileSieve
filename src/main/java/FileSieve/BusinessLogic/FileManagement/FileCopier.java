package FileSieve.BusinessLogic.FileManagement;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;

/**
 * Interface defining an operation for a file copier. Interface is package-private.
 *
 * @param <T>   The type of the object returned by the copyPathname method. In a simple implementation this may
 *              be a Boolean object indicating if the copy operation was started or completed successfully.
 * @param <L>   The type of the listener which is to receive copy notifications, settable via the
 *              interface's "setCopyOperationsListener" method.
 * @Param <C>   The type of the Comparator object to be used by the copyPathname method in determining if two
 *              files are similar.
 */
interface FileCopier<T, L, C> {

    /**
     * Method for copying a file or creating a folder to/within a target pathname. The method might handle the copy
     * operation on the current thread or return a provider (such as a SwingWorker) capable of handling the copy
     * operation on a separate thread.
     *
     * @param sourcePathnames           pathnames of folder and/or files to copy
     * @param targetPathname            pathname of file or folder to create/write
     * @param recursionEnabled          recursive search for files within subfolders
     * @param overwriteExistingFiles    indicates if existing files in the target path should be overwritten
     * @param fileComparator            used in determining if two files are dissimilar, in which case the file
     *                                  in the destination path is overwritten depending on the value of the
     *                                  overwriteExistingFiles parameter
     * @return                          implementer defined return type
     * @throws IOException              thrown for implementer defined reason(s)
     */
    public T copyPathnames(Set<Path> sourcePathnames, Path targetPathname, boolean recursionEnabled, boolean overwriteExistingFiles, Comparator<C> fileComparator) throws IOException;

    /**
     * Convenience method providing similar functionality to the copyPathnames method but taking only one pathname,
     * rather than a list of pathnames, to be copied to a target destination folder. The method might handle the copy
     * operation on the current thread or return a provider (such as a SwingWorker) capable of handling the copy operation on a separate thread.
     *
     * @param sourcePathname            pathname of file or folder to copy
     * @param targetPathname            pathname of file or folder to create/write
     * @param recursionEnabled          recursive search for files within subfolders
     * @param overwriteExistingFiles    indicates if existing files in the target path should be overwritten
     * @param fileComparator            used in determining if two files are dissimilar, in which case the file
     *                                  in the destination path is overwritten depending on the value of the
     *                                  overwriteExistingFiles parameter
     * @return                          implementer defined return type
     * @throws IOException              thrown for implementer defined reason(s)
     */
    public T copyPathname(Path sourcePathname, Path targetPathname, boolean recursionEnabled, boolean overwriteExistingFiles, Comparator<C> fileComparator) throws IOException;

    /**
     * Specifies a listener to which progress updates for copy operations may be forwarded.
     *
     * @param listener      instance of a type defining
     */
    public void setCopyOperationsListener(L listener);

} // interface FileCopier<T, L>
