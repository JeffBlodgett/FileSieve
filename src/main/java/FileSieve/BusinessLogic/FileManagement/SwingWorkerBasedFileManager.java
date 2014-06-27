package FileSieve.BusinessLogic.FileManagement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Concrete file manager class which inherits FileCopier and FileDeleter implementations from abstract class
 * FileManager and implements FileCopier.
 */
final class SwingWorkerBasedFileManager extends AbstractFileManager<CopyJob, CopyJobListener, Path> implements SwingFileManager {

    private int workerLimit;
    private CopyJobListener copyJobListener;

    protected SwingWorkerBasedFileManager(int workerThreadLimit) {
        this.workerLimit = workerThreadLimit;
    }

    /**
     * Copy's a file or folder, recursively or not, to a destination folder
     *
     * @param sourcePathname            pathname of file or folder to copy
     * @param targetPathname            pathname of file or folder to create/write
     * @param recursionEnabled          recursive search for and copying of files and subfolders with subfolders of the
     *                                  sourcePathname
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
    public CopyJob copyPathname(Path sourcePathname, Path targetPathname, boolean recursionEnabled, boolean overwriteExistingFiles, Comparator<Path> fileComparator) throws IOException, IllegalStateException {
        if (sourcePathname == null) throw new NullPointerException("null path provided for sourcePathname parameter");
        if (targetPathname == null) throw new NullPointerException("null path provided for targetPathname parameter");

        if (!Files.exists(sourcePathname)) throw new IllegalArgumentException("file or folder specified by \"sourcePathname\" parameter does not exist");

        return CopyJob.getCopyJob(sourcePathname, targetPathname, recursionEnabled, overwriteExistingFiles, fileComparator, copyJobListener);
    }

    /**
     * Sets the CopyJobListener to which CopyJob instances returned by the "copyPathname" method forward progress
     * notifications.
     *
     * @param copyJobListener   Reference to a CopyJobListener
     */
    @Override
    public void setCopyOperationsListener(CopyJobListener copyJobListener) {
        this.copyJobListener = copyJobListener;
    }

} // class SwingWorkerFileManagement extends BasicFileManager
