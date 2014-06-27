package FileSieve.BusinessLogic.FileManagement;

import java.nio.file.Path;

/**
 * Interface defining methods to be used for receipt of progress updates from CopyJob instances
 */
public interface CopyJobListener {

    public void UpdateCopyJobProgress(CopyJob copyJob, int percentProgressed);

    public void UpdatePathnameCopyProgress(CopyJob copyJob, Path pathnameBeingCopied, int percentProgressed);

    public void InternalCopyJobException(CopyJob copyJob, Throwable throwable);

} // interface CopyJobListener
