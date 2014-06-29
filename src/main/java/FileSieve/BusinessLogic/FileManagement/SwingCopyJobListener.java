package FileSieve.BusinessLogic.FileManagement;

import java.nio.file.Path;

/**
 * Interface defining methods to be used for receipt of progress updates from CopyJob instances
 */
public interface SwingCopyJobListener {

    public void UpdateCopyJobProgress(SwingCopyJob swingCopyJob, int percentProgressed);

    public void UpdatePathnameCopyProgress(SwingCopyJob swingCopyJob, Path pathnameBeingCopied, int percentProgressed);

    public void InternalCopyJobException(SwingCopyJob swingCopyJob, Throwable throwable);

} // interface SwingCopyJobListener
