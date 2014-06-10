package FileSieve.BusinessLogic.FileManagement;

import javax.swing.SwingWorker;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * File management class defining methods for the acquisition of SwingWorker instances to be used for potentially
 * long-running file copy operations
 */
public class SwingWorkerFileManagement extends BasicFileManager<SwingWorker<Void, Integer>> {

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
    @Override
    public SwingWorker<Void, Integer> pathnameCopyProvider(Path sourcePathname, Path targetPathname, boolean overwriteFile) {
        if ((sourcePathname == null) || (targetPathname == null)) throw new NullPointerException("null pathname provided for source or target");

        return new PathnameCopyWorker(sourcePathname, targetPathname, overwriteFile);
    }

    /**
     * Convenience method for copying multiple files or folders to a target, each on its own SwingWorker (thread).
     * Uses the pathnameCopyProvider method defined by this class to return one SwingWorker instance per file/folder
     * to be copied/created.
     *
     * @param sourcePathnames   pathnames of files and folders to copy
     * @param targetFolder      pathname of folder to which files and folders are to be created/written
     * @param overwriteFiles    indicates if existing targets should be overwritten (applies only to files)
     * @return                  Map containing instances of a SwingWorkers capable of performing the copy operations
     *                          and providing progress updates in the form of an Integer value representing the
     *                          percentage of the copy operation completed
     */
    public Map<Path, SwingWorker<Void, Integer>> getPathnameCopyProviders(Set<Path> sourcePathnames, Path targetFolder, boolean overwriteFiles) {
        if ((sourcePathnames == null) || (targetFolder == null)) throw new NullPointerException("null pathname provided for sources or target");

        Map<Path, SwingWorker<Void, Integer>> swingWorkers = new TreeMap<Path, SwingWorker<Void, Integer>>();
        for (Path path : sourcePathnames) {
            swingWorkers.put(path, pathnameCopyProvider(path, targetFolder, overwriteFiles));
        }

        return swingWorkers;
    }

    /**
     * SwingWorker capable of copying a file or folder from a source pathname to a target pathname while providing
     * progress updates in the form of an Integer representing the percentage of the copy operation that is complete
     */
    private static class PathnameCopyWorker extends SwingWorker<Void, Integer> {

        // TODO "PathnameCopyWorker" is currently just a stub class that enables "SwingWorkerFileManagement" compilation

        public PathnameCopyWorker(Path sourcePathname, Path targetPathname, boolean overwriteFile) {
            if ((sourcePathname == null) || (targetPathname == null)) throw new NullPointerException("null pathname provided for source or target");
        }

        @Override
        public Void doInBackground() {
            return null;
        }

        @Override
        public void process(List<Integer> chunks) {

        }

        @Override
        public void done() {

        }

    } // class PathnameCopyWorker extends SwingWorker<Void, Integer>

} // class SwingWorkerFileManagement extends BasicFileManager<SwingWorker<Void, Integer>>
