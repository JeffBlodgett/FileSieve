package FileSieve.BusinessLogic.FileManagement;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * File management class defining methods for the acquisition of SwingWorker instances to be used for potentially
 * long-running file copy operations
 */
class SwingWorkerFileManager extends BasicFileManager<PathnameCopyWorker> {

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
     * @param overwriteFile     indicates if existing files in the target path should be overwritten
     * @param ifSizeDiffers     if "overwriteExisting" argument is true, overwrites existing files only if their size differs
     * @return                  instance of a SwingWorker capable of performing the copy operation and providing
     *                          progress updates in the form of an Integer value representing the percentage of the
     *                          copy operation completed
     */
    @Override
    public PathnameCopyWorker pathnameCopyProvider(Path sourcePathname, Path targetPathname, boolean overwriteFile, boolean ifSizeDiffers) {
        if ((sourcePathname == null) || (targetPathname == null)) throw new NullPointerException("null pathname provided for source or target");

        PathnameCopyWorker worker = new PathnameCopyWorker(sourcePathname, targetPathname);
        worker.setOverwriteExistingFiles(overwriteFile);
        worker.setOverwriteIfSizeDiffers(ifSizeDiffers);

        return worker;
    }

    /**
     * Convenience method for copying multiple files or folders to a target, each on its own SwingWorker (thread).
     * Uses the pathnameCopyProvider method defined by this class to return one SwingWorker instance per file/folder
     * to be copied/created.
     *
     * @param sourcePathnames   pathnames of files and folders to copy
     * @param targetFolder      pathname of folder to which files and folders are to be created/written
     * @param overwriteFiles    indicates if existing files in the target path should be overwritten
     * @param ifSizeDiffers     if "overwriteExisting" argument is true, overwrites existing files only if their size differs
     * @return                  Map containing instances of a SwingWorkers capable of performing the copy operations
     *                          and providing progress updates in the form of an Integer value representing the
     *                          percentage of the copy operation completed
     */
    public Map<Path, PathnameCopyWorker> getPathnameCopyProviders(Set<Path> sourcePathnames, Path targetFolder, boolean overwriteFiles, boolean ifSizeDiffers) {
        if ((sourcePathnames == null) || (targetFolder == null)) throw new NullPointerException("null pathname provided for sources or target");

        Map<Path, PathnameCopyWorker> workers = new LinkedHashMap<Path, PathnameCopyWorker>(sourcePathnames.size());
        for (Path path : sourcePathnames) {
            workers.put(path, pathnameCopyProvider(path, targetFolder, overwriteFiles, ifSizeDiffers));
        }

        return workers;
    }

} // class SwingWorkerFileManagement extends BasicFileManager
