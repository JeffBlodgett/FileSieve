package FileSieve.BusinessLogic.FileManagement;

import javax.swing.SwingWorker;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * File management class defining methods for the acquisition of SwingWorker instances to be used for potentially
 * long-running file copy operations
 */
public abstract class FileManagerForConcurrency extends BasicFileManager<SwingWorker<Void, Integer>> {

    /**
     * Method for copying a file or creating a folder to/within a target pathname. Implementers construct one
     * SwingWorker instance per files or folder to be copied/created. SwingWorker instance is return. The caller is
     * then responsible for starting and monitoring the SwingWorker (worker thread). This permits progress status
     * for the copy operation to be reported to the GUI (for use by, for example, a JProgressBar).
     * See second post at following link for hints:<br>
     * http://stackoverflow.com/questions/13574461/need-to-have-jprogress-bar-to-measure-progress-when-copying-directories-and-file
     *
     * @param sourcePathname    pathname of file or folder to copy
     * @param targetPathname    pathname of file or folder to create/write
     * @param overwriteFile     indicates if an existing target file should be overwritten
     * @return                  instance of a SwingWorker capable of performing the copy operation and providing
     *                          progress updates in the form of an Integer value representing the percentage of the
     *                          copy operation completed
     */
    abstract public SwingWorker<Void, Integer> getPathnameCopyWorker(Path sourcePathname, Path targetPathname, boolean overwriteFile);

    /**
     * Convenience method for copying multiple files or folders to a target, each on its own SwingWorker (thread).
     * Uses the copyPathname method defined by this interface.
     *
     * @param sourcePathnames   pathnames of files and folders to copy
     * @param targetFolder      pathname of folder to which files and folders are to be created/written
     * @param overwriteFiles    indicates if existing targets should be overwritten (applies only to files)
     * @return                  Map containing instances of a SwingWorkers capable of performing the copy operations
     *                          and providing progress updates in the form of an Integer value representing the
     *                          percentage of the copy operation completed
     */
    abstract public Map<Path, SwingWorker<Void, Integer>> getPathnameCopyWorkers(Set<Path> sourcePathnames, Path targetFolder, boolean overwriteFiles);

} // abstract class ConcurrentFileManager extends BasicFileManager<SwingWorker<Void, Integer>>
