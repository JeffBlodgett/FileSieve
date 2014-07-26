package FileSieve.BusinessLogic.FileManagement;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;

/**
 * A FileManager type for return by the FileManagerFactory. This implementation returns SwingCopyJob class instances
 * for handling folder/file copies. SwingCopyJob instances use an internal SwingWorker to handle copy operations.
 */
public interface SwingFileManager extends FileOpener, FileDeleter, FileCopier<SwingCopyJob, SwingCopyJobListener, Path> {

    /* Hybrid interface - does nothing more than bring three other interfaces together into a unified type.
       JavaDoc comments are provided for implementation-specific (non-generic) FileCopier methods. */

    /**
     * Method for copying one or more folders or files to a target folder. The method returns (and starts execution of)
     * an instance of a SwingCopyJob that handles the copy operation on a background thread using a SwingWorker.
     *
     * @param sourcePathnames           pathnames of folders and/or files to copy
     * @param targetPathname            pathname of folder into which to copy sourcePathnames items
     * @param recursionEnabled          boolean value specifying if a recursive search for files/folders within subfolders of the sourcePathnames items should be carried out
     * @param overwriteExistingFiles    indicates if existing files in the target path should be overwritten if found to be similar to those currently being copied
     * @param fileComparator            Function object to be used in determining if two files are similar/dissimilar. If dissimilar, the file in the destination path is
     *                                  overwritten depending on the value of the overwriteExistingFiles parameter
     * @return                          an instance of a SwingCopyJob
     * @throws java.io.IOException      thrown if attempted access of a path in sourcePathnames generates an IOException
     */
    public SwingCopyJob copyPathnames(Set<Path> sourcePathnames, Path targetPathname, boolean recursionEnabled, boolean overwriteExistingFiles, Comparator<Path> fileComparator) throws IOException, IllegalStateException;

    /**
     * Convenience method providing similar functionality to the copyPathnames method but taking only one pathname
     * (as opposed to Set of pathnames) to be copied to a target destination folder. The method returns (and starts
     * the execution of) an instance of a SwingCopyJob that handles the copy operation on a background thread using a
     * SwingWorker.
     *
     * @param sourcePathname            pathname of folder or file to copy
     * @param targetPathname            pathname of folder into which to copy sourcePathnames items
     * @param recursionEnabled          boolean value specifying if a recursive search for files/folders within subfolders of the sourcePathnames items should be carried out
     * @param overwriteExistingFiles    indicates if existing files in the target path should be overwritten if found to be similar to those currently being copied
     * @param fileComparator            Function object to be used in determining if two files are similar/dissimilar. If dissimilar, the file in the destination path is
     *                                  overwritten depending on the value of the overwriteExistingFiles parameter
     * @return                          an instance of a SwingCopyJob
     * @throws IOException              thrown if attempted access of a path in sourcePathnames generates an IOException
     * @throws IllegalStateException    thrown if there is another, ongoing copy job that is copying a file with the same pathname to the same destination folder
     */
    public SwingCopyJob copyPathname(Path sourcePathname, Path targetPathname, boolean recursionEnabled, boolean overwriteExistingFiles, Comparator<Path> fileComparator) throws IOException, IllegalStateException;

    /**
     * Specifies a listener to which progress updates for copy operations may be forwarded. Newly instantiated/returned
     * SwingCopyJob instances are configured to send updates to this listener via its SwingCopyJobListener interface.
     *
     * @param listener      instance of a type defining
     */
    public void setCopyOperationsListener(SwingCopyJobListener listener);

} // interface SwingFileManager extends FileOpener, FileDeleter, FileCopier<SwingCopyJob, SwingCopyJobListener, Path>
