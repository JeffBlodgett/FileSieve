package FileSieve.BusinessLogic.FileManagement;

import javax.swing.SwingWorker;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.AbstractMap.SimpleImmutableEntry;

/**
 * Copy job
 */
public final class SwingCopyJob {

    // swingCopyJobs Map has protected access instead of private only for testing purposes
    protected static final Map<SwingCopyJob, Map<Path, Integer>> swingCopyJobs = Collections.synchronizedMap(new HashMap<SwingCopyJob, Map<Path, Integer>>(10));

    private final Object lockObject = new Object();
    private final List<SwingCopyJobListener> swingCopyJobListeners = Collections.synchronizedList(new ArrayList<SwingCopyJobListener>(10));
    private final BackgroundCopyWorker worker;
    private final boolean recursiveCopy;
    private final Set<Path> pathsBeingCopied;
    private final Path destinationFolder;
    private final boolean overwriteExistingFiles;
    private final Comparator<Path> fileComparator;
    private Throwable internalWorkerException = null;

    /**
     * Static factory method for creating or retrieving a reference to an equivalent (and ongoing) SwingCopyJob.
     *
     * @param pathsToBeCopied           list of Path objects abstracting folders or files to copy
     * @param destinationFolder         target folder to which file and folder copies are to be placed
     * @param recursiveCopy
     * @param overwriteExistingFiles
     * @param fileComparator
     * @param swingCopyJobListener
     * @return
     * @throws IllegalStateException
     * @throws IOException              thrown if an IOException is encountered while converting source paths to real paths
     */
    protected static SwingCopyJob getCopyJob(Set<Path> pathsToBeCopied, Path destinationFolder, boolean recursiveCopy, boolean overwriteExistingFiles, Comparator<Path> fileComparator, SwingCopyJobListener swingCopyJobListener) throws IllegalStateException, IOException {
        if (pathsToBeCopied == null) {
            throw new IllegalArgumentException("null reference passed for \"pathsToBeCopied\" parameter");
        }
        if ((destinationFolder == null) || (destinationFolder.getNameCount() == 0)) {
            throw new IllegalArgumentException("null reference passed for \"destinationFolder\" parameter");
        }

        // Convert paths to real paths. This has the added benefit of "cloning" passed Path objects.
        Set<Path> realPaths = new LinkedHashSet<>(pathsToBeCopied.size());
        for (Path path : pathsToBeCopied) {
            if ((path == null) || (!Files.exists(path, LinkOption.NOFOLLOW_LINKS))) {
                throw new IllegalArgumentException("a path included within the \"sourcePathnames\" list does not exist");
            } else {
                realPaths.add(path.toRealPath(LinkOption.NOFOLLOW_LINKS));
            }
        }

        SwingCopyJob jobToReturn = null;

        /* Create a SwingCopyJob from the passed parameters but don't start (execute) its SwingWorker yet whereas we
           want to check for the existence of a similar ongoing job. */
        SwingCopyJob newJob = new SwingCopyJob(realPaths, destinationFolder, recursiveCopy, overwriteExistingFiles, fileComparator, swingCopyJobListener);

        synchronized (swingCopyJobs) {
            // Ensure a similar job is not already in progress
            for (SwingCopyJob swingCopyJob : swingCopyJobs.keySet()) {
                if (newJob.equals(swingCopyJob)) {
                    jobToReturn = swingCopyJob;
                    break;
                }
            }

            /* If a path to be copied is a file, throw an IllegalStateException if there is a pre-existing job that is
               copying a file with the same name to the same destination folder */
            if (jobToReturn == null) {
                for (Path pathToCopy : realPaths) {
                    if (Files.isRegularFile(pathToCopy, LinkOption.NOFOLLOW_LINKS)) {
                        for (SwingCopyJob swingCopyJob : swingCopyJobs.keySet()) {
                            for (Path path : swingCopyJob.pathsBeingCopied) {
                                if ((Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) &&
                                        (path.getFileName().equals(pathToCopy.getFileName())) &&
                                        (swingCopyJob.destinationFolder.equals(destinationFolder))
                                        ) {
                                    throw new IllegalStateException("Another copy job is writing to the specified destination folder");
                                }
                            }
                        }
                    }
                }
            }
        }

        if (jobToReturn == null) {
            jobToReturn = newJob;
            SwingCopyJob.swingCopyJobs.put(jobToReturn, new HashMap<Path, Integer>(200));
            jobToReturn.worker.execute();
        } else {

            if (swingCopyJobListener != null) {
                jobToReturn.swingCopyJobListeners.add(swingCopyJobListener);
            }
        }

        return jobToReturn;
    }

    /**
     * Private constructor for use by enclosing class' static factory method.
     *
     * @param pathsBeingCopied
     * @param destinationFolder
     * @param recursiveCopy
     * @param overwriteExistingFiles    boolean true if the CopyJob
     * @param fileComparator            Function object of type Comparator<Path> defining a compare method with which
     *                                  to compare two files. The compare method should define what it means for two
     *                                  regular files to be the same. If the method evaluates to 0 (equal) then the
     *                                  file in the destination path will be overwritten only if the
     *                                  overwriteExistingFiles parameter has been set to true. If an implementation
     *                                  is not provided, the CopyJob will use a default implementation that defines
     *                                  equality using the lowercase form of the file names and their uncompressed
     *                                  length in bytes.
     * @param swingCopyJobListener
     */
    private SwingCopyJob(Set<Path> pathsBeingCopied, Path destinationFolder, boolean recursiveCopy, boolean overwriteExistingFiles, Comparator<Path> fileComparator, SwingCopyJobListener swingCopyJobListener) {
        this.pathsBeingCopied = pathsBeingCopied;
        this.destinationFolder = destinationFolder;
        this.recursiveCopy = recursiveCopy;
        this.overwriteExistingFiles = overwriteExistingFiles;

        if (fileComparator == null) {
            this.fileComparator = DefaultFileComparator.getInstance();
        } else {
            this.fileComparator = fileComparator;
        }

        // May be null if no listener is to receive progress updates
        if (swingCopyJobListener != null) {
            this.swingCopyJobListeners.add(swingCopyJobListener);
        }

        worker = new BackgroundCopyWorker(this);
    }

    /**
     * Returns true if the passed SwingCopyJob is considered equivalent to this SwingCopyJob.
     *
     * @param obj   reference to an object to be compared to this instance for equality
     * @return      boolean true if the passed object is a SwingCopyJob that is equivalent in state to this SwingCopyJob
     */
    @Override
    public boolean equals(Object obj) {
        boolean result = false;

        if (obj != null) {
            if (obj == this) {
                result = true;
            } else if (obj instanceof SwingCopyJob) {
                SwingCopyJob passedInstance = (SwingCopyJob)obj;

                // May be equal if destination folders and overwrite settings are the same
                if ((passedInstance.destinationFolder.equals(this.destinationFolder)) && (passedInstance.overwriteExistingFiles == this.overwriteExistingFiles)) {
                    boolean listsAreSame = true;
                    boolean allFiles = true;

                    // Check to determine if the each copy job is copying the same set of source paths
                    if (this.pathsBeingCopied.size() == passedInstance.pathsBeingCopied.size()) {
                        for (Path path : this.pathsBeingCopied) {
                            if (!passedInstance.pathsBeingCopied.contains(path)) {
                                listsAreSame = false;
                                break;
                            }

                            /* Check to determine if all the source paths in the lists are regular files, in which
                               case it makes no difference if recursion option is enabled. */
                            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                                allFiles = false;
                            }
                        }
                    }

                    // Jobs are equal if they have the same source paths to copy
                    if (listsAreSame) {
                        if (allFiles) {
                            // Jobs are considered equal if only files are being copied
                            result = true;
                        } else if (passedInstance.recursiveCopy == this.recursiveCopy) {
                            /* If one or more folders are being copied then the jobs are equal only if they are both
                               using the same setting for recursion. */
                            result = true;
                        }
                    }
                }
            }
        }

        return result;
    }

    public boolean Cancel() {
        return worker.cancel(true);
    }

    /**
     * Returns a list of the files and/or folders being copied.
     *
     * @return  List<Path> representing the files and/or folders being copied
     */
    public List<Path> getPathBeingCopied() {
        List<Path> copiedList = new ArrayList<>(this.pathsBeingCopied.size());

        for (Path path : this.pathsBeingCopied) {
            copiedList.add(path.toFile().toPath());
        }

        return copiedList;
    }

    /**
     * Returns the path of the destination folder to which files and folders are copied.
     *
     * @return  Path object representing the destination folder for the copy job
     */
    public Path getDestinationFolder() {
        return this.destinationFolder;
    }

    /**
     * Returns a boolean value of true if files and folders in subfolders of folders found in the sourcePathname are
     * searched and copied recursively, false if not.
     *
     * @return  boolean true if files and folders in subfolders of folders in the sourcePathname are searched and
     *          copied recursively, false if not.
     */
    public boolean isRecursive() {
        return this.recursiveCopy;
    }

    /**
     * Returns true if the copy job is running, false if not.
     *
     * @return  boolean true if the copy job is running, false if not
     */
    public boolean isRunning() {
        return (worker.getState() == SwingWorker.StateValue.DONE);
    }

    /**
     * Blocks until the copy job has been completed. This method rethrows any internal exception
     * that may have occurred within the background thread. Such an exception  causing premature termination of the copy job.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void awaitCompletion() throws InterruptedException, ExecutionException {
        synchronized (lockObject) {
            while (!(worker.getState() == SwingWorker.StateValue.DONE)) {
                try {
                    lockObject.wait();
                } catch (InterruptedException e) {
                    // Ignore interrupt
                }
            }
        }

        if (internalWorkerException != null) {
            if (internalWorkerException instanceof InterruptedException) {
                throw (InterruptedException) internalWorkerException;
            } else if (internalWorkerException instanceof ExecutionException) {
                throw (ExecutionException) internalWorkerException;
            }
        }
    }

    /**
     *
     */
    private class BackgroundCopyWorker extends SwingWorker<Void, SimpleImmutableEntry<Path, Integer>> {

        private final SwingCopyJob thisSwingCopyJob;
        private static final int COPY_JOB_AT_100_PERCENT = 100;
        private static final int PATHNAME_COPY_AT_100_PERCENT = 100;
        private static final int ONE_HUNDRED_PERCENT = 100;
        private static final int ZERO_PERCENT = 0;
        private long totalBytes = 0L;
        private long copiedBytes = 0L;
        private int totalPercentCopied = 0;
        private int copyPathsRecursionLevel = 0;

        protected BackgroundCopyWorker(SwingCopyJob enclosingSwingCopyJob) throws IllegalArgumentException {
            if (enclosingSwingCopyJob == null) {
                throw new IllegalArgumentException("null pathname provided for \"enclosingCopyJob\" parameter");
            }

            this.thisSwingCopyJob = enclosingSwingCopyJob;
        }

        /**
         * Notifies registered CopyJobListeners of progress updates published by the background thread.
         * This method is called on the EDT.
         *
         * @param updates   Updates published by the SwingWorkers working thread ("doInBackground" method)
         */
        @Override
        public void process(List<SimpleImmutableEntry<Path, Integer>> updates) {
            for (SimpleImmutableEntry<Path, Integer> pair : updates) {
                Map<Path, Integer> copyJobProgressions = swingCopyJobs.get(thisSwingCopyJob);

                boolean isCopyJobUpdate = false;    // (as opposed to a progress update for a particular file or subfolder)

                /* Updates coming from the background thread are identified using the targeted file or folder of the copy
                   operation or the copy job's overall destination folder */
                if (pair.getKey().equals(thisSwingCopyJob.destinationFolder)) {
                    isCopyJobUpdate = true;
                }

                Integer oldValue = copyJobProgressions.put(pair.getKey(), pair.getValue());
                if ((oldValue == null) || (!oldValue.equals(pair.getValue()))) {
                    // Notify SwingCopyJobListener
                    if (thisSwingCopyJob.swingCopyJobListeners.size() > 0) {
                        synchronized (thisSwingCopyJob.swingCopyJobListeners) {
                            for (SwingCopyJobListener listener : thisSwingCopyJob.swingCopyJobListeners) {
                                if (isCopyJobUpdate) {
                                    listener.UpdateCopyJobProgress(thisSwingCopyJob, pair.getValue());
                                } else {
                                    listener.UpdatePathnameCopyProgress(thisSwingCopyJob, pair.getKey(), pair.getValue());
                                }
                            }
                        }
                    }
                }
            }
        }

        /**
         * Notifies registered CopyJobListeners that (a) the copy job been has been completed and (b) of any internal
         * exception that might have caused the job to terminate early. This method is called on the EDT.
         */
        @Override
        public void done() {
            setProgress(COPY_JOB_AT_100_PERCENT);

            swingCopyJobs.remove(thisSwingCopyJob);

            if (thisSwingCopyJob.swingCopyJobListeners.size() > 0) {
                synchronized (thisSwingCopyJob.swingCopyJobListeners) {
                    try {
                        get();
                    } catch (InterruptedException | ExecutionException e) {
                        for (SwingCopyJobListener listener : thisSwingCopyJob.swingCopyJobListeners) {
                            listener.InternalCopyJobException(thisSwingCopyJob, e);
                        }
                    }

                    for (SwingCopyJobListener listener : thisSwingCopyJob.swingCopyJobListeners) {
                        listener.UpdateCopyJobProgress(thisSwingCopyJob, COPY_JOB_AT_100_PERCENT);
                    }

                    thisSwingCopyJob.swingCopyJobListeners.clear();
                }
            }

            synchronized (lockObject) {
                lockObject.notify();
            }
        }

        /**
         * This method is executed on a background thread when the SwingWorker's execute() method is called. Exceptions
         * thrown by this method are thrown as type "ExecutionException", with Throwable.getCause() containing the details
         * of the exception encountered, when the worker's get() method is called. If no exceptions were thrown, the get()
         * method returns the (boolean) value returned by this method.
         *
         * @return                          boolean true if all operations in the
         * @throws IllegalStateException    thrown if the sourcePathname does not exist
         * @throws SecurityException        thrown if read access on the sourcePathname or write access on the targetPathname has not been granted
         * @throws java.io.IOException              thrown if an IOException occurs
         */
        @Override
        public Void doInBackground() throws IllegalStateException, SecurityException, IOException {
            try {
                for (Path path : thisSwingCopyJob.pathsBeingCopied) {
                    if (Files.exists(path)) {
                        retrieveTotalBytes(path);
                    } else {
                        throw new IllegalStateException("source pathname does not exist");
                    }
                }
            } catch (SecurityException e) {
                throw new SecurityException("missing read access on source path (root, subfolder, or file) and/or write access on target path", e);
            } catch (IOException e) {
                throw new IOException("IOException while calculating bytes to copy", e);
            }

            try {
                for (Path path : thisSwingCopyJob.pathsBeingCopied) {
                    copyPaths(path, thisSwingCopyJob.destinationFolder);
                }
            } catch (SecurityException e) {
                throw new SecurityException("SecurityException while reading or writing files/folders in the source or target root", e);
            } catch (IOException e) {
                throw new SecurityException("IOException while reading or writing files/folders in the source or target root", e);
            }

            return null;
        }

        private void retrieveTotalBytes(Path sourcePathname) throws SecurityException, IOException {
            if (Files.isDirectory(sourcePathname)) {
                try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(sourcePathname)) {
                    for (Path path : dirStream) {
                        if (!isCancelled()) {
                            // Exclude target folder if it is a subfolder of the source folder
                            if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) && (!path.equals(thisSwingCopyJob.destinationFolder) && thisSwingCopyJob.recursiveCopy)) {
                                retrieveTotalBytes(path);
                            } else if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                                totalBytes += path.toFile().length();
                            }
                        }
                    }
                }
            } else {
                totalBytes += sourcePathname.toFile().length();
            }
        }

        /**
         * Utility method called by doInBackground() method to handle file and folder copy operations
         *
         * @param sourcePath            source file or folder that is to be copied
         * @param targetPath            target file or folder to be copied
         * @throws SecurityException    thrown if the security manager is unable to access a file or folder as requested
         * @throws IOException          thrown if an IOException occurs during a read or write operation
         */
        private void copyPaths(Path sourcePath, Path targetPath) throws SecurityException, IOException {
            if (!isCancelled()) {
                if (!sourcePath.equals(thisSwingCopyJob.destinationFolder)) {

                    if (!Files.exists(targetPath)) {
                        targetPath = Files.createDirectories(targetPath);
                        if (targetPath.equals(thisSwingCopyJob.destinationFolder)) {
                            publish(new AbstractMap.SimpleImmutableEntry<>(targetPath, ZERO_PERCENT));
                        }
                    }

                    if (Files.isDirectory(sourcePath)) {
                        List<Path> filePaths = new ArrayList<>(50);
                        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(sourcePath)) {
                            for (Path path : dirStream) {
                                filePaths.add(path);
                            }
                        }

                        for (Path path : filePaths) {
                            if (Files.isDirectory(path)) {
                                Path newTargetPath;
                                if (copyPathsRecursionLevel == 0) {
                                    newTargetPath = targetPath.resolve(sourcePath.getFileName().resolve(path.getFileName()));
                                } else {
                                    newTargetPath = targetPath.resolve(path.getFileName());
                                }

                                if (!Files.exists(newTargetPath)) {
                                    Files.createDirectory(newTargetPath);
                                }

                                publish(new SimpleImmutableEntry<>(newTargetPath, PATHNAME_COPY_AT_100_PERCENT));

                                if (thisSwingCopyJob.recursiveCopy) {
                                    ++copyPathsRecursionLevel;
                                    try {
                                        copyPaths(path, newTargetPath);
                                    } finally {
                                        --copyPathsRecursionLevel;
                                    }
                                }

                            } else if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                                if (copyPathsRecursionLevel == 0) {
                                    Path newTargetPath = targetPath.resolve(sourcePath.getFileName());

                                    if (!Files.exists(newTargetPath)) {
                                        Files.createDirectory(newTargetPath);
                                    }

                                    copyFile(path, newTargetPath);
                                } else {
                                    copyFile(path, targetPath);
                                }
                            }
                        }

                    } else {
                        // Copy file specified by "sourcePath" parameter to the folder specified by the "targetPath"
                        copyFile(sourcePath, targetPath);
                    }
                }
            }
        }

        /**
         * Private helper method, called by copyPaths method, for copying a single file (the sourcePathname)
         *
         * @param fileToCopy            file to copy, passed as a Path
         * @param target                folder to which copy is to be placed
         * @throws SecurityException    thrown if the security manager denies read access to the original file or write
         *                              access to the folder to contain the copy
         * @throws IOException          throw if an IOException occurs during read/write operations
         */
        private void copyFile(Path fileToCopy, Path target) throws SecurityException, IOException {
            target = target.resolve(fileToCopy.getFileName());

            long fileBytes = fileToCopy.toFile().length();  // size of file in bytes

            boolean filesAreSimilar = false;
            if (fileComparator.compare(fileToCopy, target) == 0) {
                filesAreSimilar = true;
            }

            if ((!filesAreSimilar) || overwriteExistingFiles)  {
                long soFar = 0L;    // file bytes copied thus far
                int sourceByte;
                int filePercentCopied;
                int totalPercentPreviouslyCopied = totalPercentCopied;
                int pathnameProgress = ZERO_PERCENT;

                try (
                        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileToCopy.toFile()));
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(target.toFile()))
                ) {
                /* Copy file one byte at time. BufferedInputStream and BufferedOutputStream have, well, buffers so
                   so this isn't as slow as it might at first seem */
                    while (((sourceByte = bis.read()) != -1) && (!isCancelled())) {
                        bos.write(sourceByte);

                        /* Update copy job's total progress if progress has incremented by at least 1 percent and is not
                           yet 100 percent complete */
                        totalPercentCopied = (int) (++copiedBytes * ONE_HUNDRED_PERCENT / totalBytes);
                        if ((getProgress() != totalPercentCopied) && (totalPercentCopied < ONE_HUNDRED_PERCENT)) {
                            setProgress(totalPercentCopied);
                            publish(new SimpleImmutableEntry<>(thisSwingCopyJob.destinationFolder, totalPercentCopied));
                        }

                        /* Update the progress of the individual file copy if progress has incremented by at least 1 percent
                           and is not yet 100 percent complete */
                        filePercentCopied = (int) (++soFar * ONE_HUNDRED_PERCENT / fileBytes);
                        if ((pathnameProgress != filePercentCopied) && (filePercentCopied < ONE_HUNDRED_PERCENT)) {
                            pathnameProgress = filePercentCopied;
                            publish(new SimpleImmutableEntry<>(target, pathnameProgress));
                        }
                    }

                    // No need to set "pathnameProgress" variable to 100... just publish and move on
                    publish(new SimpleImmutableEntry<>(target, PATHNAME_COPY_AT_100_PERCENT));

                } catch (IOException e) {
                    // Backtrack... set+publish the total job progress to the value had prior to this attempted file
                    // and set+publish the progress for the failed copy as 0 percent
                    setProgress((int) ((totalPercentPreviouslyCopied + fileBytes) * ONE_HUNDRED_PERCENT / totalBytes));
                    publish(new SimpleImmutableEntry<>(thisSwingCopyJob.destinationFolder, getProgress()));
                    publish(new SimpleImmutableEntry<>(target, 0));

                    try {
                        if (Files.exists(target) && ((target.toFile().length() == 0L) || (soFar > 0L))) {
                            Files.delete(target);
                            throw new IOException("An IOException occurred while copying file \"" + fileToCopy.toString() + "\". An incomplete copy was not left in the destination folder.", e);
                        } else {
                            throw new IOException("An IOException occurred while copying file \"" + fileToCopy.toString() + "\".", e);
                        }
                    } catch (IOException ex) {
                        throw new IOException("An IOException occurred while copying file \"" + fileToCopy.toString() + "\". An incomplete copy may have been left in the destination folder.", ex);
                    }
                }
            } else {
                totalPercentCopied = (int) (++fileBytes * ONE_HUNDRED_PERCENT / totalBytes);
                if ((getProgress() != totalPercentCopied) && (totalPercentCopied < ONE_HUNDRED_PERCENT)) {
                    setProgress(totalPercentCopied);
                    publish(new SimpleImmutableEntry<>(thisSwingCopyJob.destinationFolder, totalPercentCopied));
                }
            }
        }

    } // static class BackgroundCopyWorker extends SwingWorker<Void, SimpleImmutableEntry<Path, Integer>>

    /**
     * Default file comparator (function object) for use by CopyJob instances in determining if two files are similar.
     */
    private static class DefaultFileComparator implements Comparator<Path> {

        public static final DefaultFileComparator INSTANCE = new DefaultFileComparator();

        public static DefaultFileComparator getInstance() {
            return INSTANCE;
        }

        private DefaultFileComparator() { }

        public int compare(Path path1, Path path2) {
            int result = -1;

            if ((path1 != null) && (path2 != null)) {
                if (Files.isRegularFile(path1, LinkOption.NOFOLLOW_LINKS) && Files.isRegularFile(path2, LinkOption.NOFOLLOW_LINKS)) {
                    if (path1.getFileName().toString().toLowerCase().equals(path2.getFileName().toString().toLowerCase())) {
                        if (path1.toFile().length() == path2.toFile().length()) {
                            result = 0;
                        }
                    }
                }
            }

            return result;
        }

    } // class FileComparator implements Comparator<Path>

} // class CopyJob
