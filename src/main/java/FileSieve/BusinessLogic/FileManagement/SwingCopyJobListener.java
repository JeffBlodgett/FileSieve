package FileSieve.BusinessLogic.FileManagement;

import java.nio.file.Path;

/**
 * Defines methods to be used for receipt of progress updates from SwingCopyJob class instances.
 */
public interface SwingCopyJobListener {

    /**
     * Called by SwingCopyJob instances, for which this listener is registered, when the overall copy job has progressed
     * by at least one percent. Currently, if the job is cancelled or has terminated due to an internal exception on its
     * worker thread then the job progress is set to one hundred percent.
     * one hundred percent.
     *
     * @param swingCopyJob          reference to the SwingCopyJob from which the update is coming
     * @param percentProgressed     an integer value indicating the percentage of the copy job that has been completed
     */
    public void UpdateCopyJobProgress(SwingCopyJob swingCopyJob, int percentProgressed);

    /**
     * Called by SwingCopyJob instances, for which this listener is registered, when copy progress for the file or
     * folder currently being copied has progressed by at least one percent.
     *
     * @param swingCopyJob          reference to the SwingCopyJob from which the update is coming
     * @param pathnameBeingCopied   Path instance with pathname of file or folder being copied
     * @param percentProgressed     percent progress
     */
    public void UpdatePathnameCopyProgress(SwingCopyJob swingCopyJob, Path pathnameBeingCopied, int percentProgressed);

    /**
     * Called by SwingCopyJob instances, for which this listener is registered, when the SwingCopyJob has
     * encountered an internal error on its worker thread and halted prematurely.
     *
     * @param swingCopyJob          reference to the SwingCopyJob from which the update is coming
     * @param throwable             reference to the exception detected on the SwingCopyJob instance's worker thread
     */
    public void InternalCopyJobException(SwingCopyJob swingCopyJob, Throwable throwable);

} // interface SwingCopyJobListener
