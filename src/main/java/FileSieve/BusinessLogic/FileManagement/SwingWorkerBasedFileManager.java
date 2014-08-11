package FileSieve.BusinessLogic.FileManagement;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Concrete file manager class which inherits FileCopier and FileDeleter implementations from AbstractFileManager
 * and provides for FileCopier implementations. This class is package-private.
 */
final class SwingWorkerBasedFileManager extends AbstractFileManager<SwingCopyJob, SwingCopyJobListener, Path> implements SwingFileManager {

    private int fileCopyThreadLimit;
    private SwingCopyJobListener swingCopyJobListener;

    /**
     * Constructor for SwingWorkerBasedFileManager class.
     *
     * @param fileCopyThreadLimit   maximum number of concurrent file copy threads
     */
    protected SwingWorkerBasedFileManager(int fileCopyThreadLimit) {
        this.fileCopyThreadLimit = fileCopyThreadLimit;
    }

    /**
     * Sets the maximum number of concurrent file copy threads.
     *
     * @param fileCopyThreadLimit
     */
    @Override
    public void setFileCopyThreadLimit(int fileCopyThreadLimit) {
        this.fileCopyThreadLimit = fileCopyThreadLimit;
    }

    /**
     * Copy's a list of source pathnames (folders and/or files) to a destination folder, with or without recursion
     *
     * @param sourcePathnames           list (Set<Path>) of pathnames of folders and/or files to copy
     * @param targetPathname            pathname of folder into which to copy sourcePathnames items
     * @param recursionEnabled          boolean value specifying if a recursive search for files/folders within subfolders of folders within the pathsToBeCopied list should be carried out
     * @param overwriteExistingFiles    indicates if files pre-existing files found in the target path should be overwritten if the fileComparator determines that they are different than their
     *                                  source path equivalents
     * @param fileComparator            Function object of type Comparator<Path> defining a compare method with which
     *                                  to compare two files. The compare method should define what it means for two
     *                                  regular files to be the same. If the method evaluates to 0 (equal) then the
     *                                  file in the destination path will be overwritten only if the
     *                                  overwriteExistingFiles parameter has been set to true. If an implementation
     *                                  is not provided then the copy job will use a default implementation that defines
     *                                  equality using the lowercase form of the file names and their uncompressed
     *                                  length in bytes.
     * @return                          an instance of SwingCopyJob for use in tracking and controlling the copy job
     * @throws IOException              thrown if an IOException is encountered while converting source paths to real paths
     * @throws IllegalStateException    thrown by the CopyJob class's getCopyJob method if the destination folder is
     *                                  being written to by a dissimilar copy job
     */
    @Override
    public SwingCopyJob copyPathnames(Set<Path> sourcePathnames, Path targetPathname, boolean recursionEnabled, boolean overwriteExistingFiles, Comparator<Path> fileComparator) throws IOException {
        if (sourcePathnames == null) {
            throw new NullPointerException("null reference provided for sourcePathnames parameter");
        }
        if (targetPathname == null) {
            throw new NullPointerException("null path provided for targetPathname parameter");
        }

        return SwingCopyJob.getCopyJob(sourcePathnames, targetPathname, recursionEnabled, overwriteExistingFiles, fileCopyThreadLimit, fileComparator, swingCopyJobListener);
    }

    @Override
    public SwingCopyJob copyPathname(Path sourcePathname, Path targetPathname, boolean recursionEnabled, boolean overwriteExistingFiles, Comparator<Path> fileComparator) throws IOException {
        if (sourcePathname == null) {
            throw new NullPointerException("null reference provided for sourcePathname parameter");
        }
        if (targetPathname == null) {
            throw new NullPointerException("null path provided for targetPathname parameter");
        }

        Set<Path> sourcePathnames = new LinkedHashSet<>(1);
        sourcePathnames.add(sourcePathname);

        return copyPathnames(sourcePathnames, targetPathname, recursionEnabled, overwriteExistingFiles, fileComparator);
    }

    /**
     * Sets the SwingCopyJobListener to which CopyJob instances returned by the "copyPathname" method forward progress
     * notifications.
     *
     * @param swingCopyJobListener   Reference to a SwingCopyJobListener
     */
    @Override
    public void setCopyOperationsListener(SwingCopyJobListener swingCopyJobListener) {
        this.swingCopyJobListener = swingCopyJobListener;
    }

} // class SwingWorkerFileManagement extends BasicFileManager
