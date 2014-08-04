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
     * @param sourcePathnames           list (Set<Path>) of pathnames of folders and/or files to copy
     * @param targetPathname            pathname of folder into which to copy sourcePathnames items
     * @param recursionEnabled          boolean value specifying if a recursive search for files/folders within subfolders of folders within the pathsToBeCopied list should be carried out
     * @param overwriteExistingFiles    indicates if existing files in the target path should be overwritten if found to be similar to those currently being copied
     * @param fileComparator            Function object of type Comparator<Path> defining a compare method with which
     *                                  to compare two files. The compare method should define what it means for two
     *                                  regular files to be the same. If the method evaluates to 0 (equal) then the
     *                                  file in the destination path will be overwritten only if the
     *                                  overwriteExistingFiles parameter has been set to true. If an implementation
     *                                  is not provided then the copy job will use a default implementation that defines
     *                                  equality using the lowercase form of the file names and their uncompressed
     *                                  length in bytes.
     * @return                          an instance of SwingCopyJob for use in tracking and controlling the copy job
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
     * @param fileComparator            Function object of type Comparator<Path> defining a compare method with which
     *                                  to compare two files. The compare method should define what it means for two
     *                                  regular files to be the same. If the method evaluates to 0 (equal) then the
     *                                  file in the destination path will be overwritten only if the
     *                                  overwriteExistingFiles parameter has been set to true. If an implementation
     *                                  is not provided then the copy job will use a default implementation that defines
     *                                  equality using the lowercase form of the file names and their uncompressed
     *                                  length in bytes.
     * @return                          an instance of SwingCopyJob for use in tracking and controlling the copy job
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
