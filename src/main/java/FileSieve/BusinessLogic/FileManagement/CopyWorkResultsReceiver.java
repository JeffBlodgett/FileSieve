package FileSieve.BusinessLogic.FileManagement;

import java.nio.file.Path;
import java.util.AbstractMap.SimpleImmutableEntry;

/**
 * Defines methods for use by a CopyJobWorkDelegate instance in passing work results and statistics back to a
 * SwingCopyJob instance. This class has package-private access.
 */
interface CopyWorkResultsReceiver {

    /**
     * Provides a means by which a CopyJobWorkDelegate may convey an exception to a SwingCopyJob's background worker
     * thread.
     *
     * @param exception     any exception thrown which causes the CopyJobWorkDelegate to terminate operations
     */
    public void workerException(Exception exception);

    /**
     * Provides the means by which progress updates may be conveyed by a CopyJobWorkDelegate to a SwingCopyJob.
     *
     * @param result    A unit of work representing a progress update for the copy of a specific file, folder, or a
     *                  progress update for the overall copy job. The update consists of the Path instance representing
     *                  the item being updating and an Integer representing the percentage of the copy that has been
     *                  completed.
     */
    public void receiveWorkResults(SimpleImmutableEntry<Path, Integer> result);

    /**
     * Provides a means by which a CopyJobWorkDelegate instance can communicate a "work complete" message to a
     * SwingCopyJob.
     */
    public void workCompleted();

}
