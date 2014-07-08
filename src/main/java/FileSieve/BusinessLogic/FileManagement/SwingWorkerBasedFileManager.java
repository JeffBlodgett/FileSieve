package FileSieve.BusinessLogic.FileManagement;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Concrete file manager class which inherits FileCopier and FileDeleter implementations from the AbstractFileManager
 * class and implements FileCopier. This class is package-private.
 */
final class SwingWorkerBasedFileManager extends AbstractFileManager<SwingCopyJob, SwingCopyJobListener, Path> implements SwingFileManager {

    private int workerLimit;
    private SwingCopyJobListener swingCopyJobListener;

    protected SwingWorkerBasedFileManager(int workerThreadLimit) {
        this.workerLimit = workerThreadLimit;
    }

    /**
     * Copy's a list of source pathnames (folders and/or files) to a destination folder, with or without recursion
     *
     * @param sourcePathnames           pathnames of folders and/or files to copy
     * @param targetPathname            pathname of file or folder to create/write
     * @param recursionEnabled          recursive search for and copying of files and subfolders within subfolders of the paths in the sourcePathnames list
     * @param overwriteExistingFiles    indicates if files pre-existing files found in the target path should be
     *                                  overwritten if the fileComparator determines that they are different than their
     *                                  source path equivalents
     * @param fileComparator            compares two files for equality
     * @return                          an instance of CopyJob for use in tracking and ascertaining the status of the
     *                                  copy job
     * @throws IOException              not thrown by this implementation
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

        return SwingCopyJob.getCopyJob(sourcePathnames, targetPathname, recursionEnabled, overwriteExistingFiles, fileComparator, swingCopyJobListener);
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
