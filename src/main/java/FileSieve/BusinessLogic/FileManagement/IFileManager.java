package FileSieve.BusinessLogic.FileManagement;

import javax.swing.SwingWorker;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Interface defining file management operations
 */
public interface IFileManager {

    /**
     * Method for copying a file or creating a folder to/within a target pathname on the current thread.
     *
     * @param sourcePathname    pathname of file or folder to copy
     * @param targetPathname    pathname of file or folder to create/write
     */
    public void copyPathname(Path sourcePathname, Path targetPathname);

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
     * @return
     */
    public SwingWorker<Void, Integer> copyPathname(Path sourcePathname, Path targetPathname, boolean overwriteFile);

    /**
     * Convenience method for copying multiple files or folders to a target, each on its own SwingWorker (thread).
     * Uses the copyPathname method defined by this interface.
     *
     * @param sourcePathnames   pathnames of files and folders to copy
     * @param targetFolder      pathname of folder to which files and folders are to be created/written
     * @param overwriteFiles    indicates if existing target should be overwritten (applies only to files)
     */
    public List<SwingWorker<Void, Integer>> copyPathnames(Set<Path> sourcePathnames, Path targetFolder, boolean overwriteFiles);

    /**
     * Method for deleting a file or folder
     *
     * @param pathname          pathname of file or folder to delete
     */
    public void deleteFile(Path pathname);

    /**
     * Opens the given file or folder using the application registered on the host system for opening files of the
     * pathname type. The default file browser is used if the pathname specified is a directory.
     *
     * @param pathname          pathname of a file or folder to be opened
     */
    public void openPathname(Path pathname);

} // class IFileManager
