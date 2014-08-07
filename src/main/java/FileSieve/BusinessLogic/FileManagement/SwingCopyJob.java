package FileSieve.BusinessLogic.FileManagement;

import FileSieve.BusinessLogic.FileEnumeration.DiscoveredPath;

import javax.swing.SwingWorker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Copy-job implementation, instances of which may be instantiated using one of a SwingFileManager instance's copyPathname(s) method.
 */
public final class SwingCopyJob {

    // The static "swingCopyJobs" Map has protected instead of private access for testing purposes only
    protected static final Map<SwingCopyJob, Map<Path, Integer>> swingCopyJobs = Collections.synchronizedMap(new HashMap<SwingCopyJob, Map<Path, Integer>>(10));

    private final Object lockObject = new Object();
    private final List<SwingCopyJobListener> swingCopyJobListeners = Collections.synchronizedList(new ArrayList<SwingCopyJobListener>(10));
    private final EdtUpdater worker;
    private final boolean recursiveCopy;
    private final Set<Path> pathsBeingCopied;
    private final Path destinationFolder;
    private final boolean overwriteExistingFiles;
    private final Comparator<Path> fileComparator;
    private final AtomicBoolean backgroundThreadIsRunning = new AtomicBoolean(false);
    private SwingCopyJobException internalWorkerException = null;

    /**
     * Extracts the decorated (wrapped) Path from a DiscoveredPath instance.
     *
     * @param path  a Path instance
     * @return      the Path decorated by a DiscoveredPath instance, or the same Path as that provided
     */
    private static Path extractPath(Path path) {
        if (path instanceof DiscoveredPath) {
            return ((DiscoveredPath) path).getPath();
        } else {
            return path;
        }
    }

    /**
     * Static factory method for creating or retrieving a reference to an equivalent (ongoing) SwingCopyJob.
     *
     * @param pathsToBeCopied           a list of Path objects abstracting folders and/or files to copy
     * @param destinationFolder         destination folder to which file and folder copies are to be placed
     * @param recursiveCopy             boolean value specifying if a recursive search for files/folders within subfolders of folders within the pathsToBeCopied list should be carried out
     * @param overwriteExistingFiles    indicates if existing files in the target path should be overwritten if found to be similar to those currently being copied
     * @param fileComparator            Function object of type Comparator<Path> defining a compare method with which
     *                                  to compare two files. The compare method should define what it means for two
     *                                  regular files to be the same. If the method evaluates to 0 (equal) then the
     *                                  file in the destination path will be overwritten only if the
     *                                  overwriteExistingFiles parameter has been set to true. If an implementation
     *                                  is not provided then the copy job will use a default implementation that defines
     *                                  equality using the lowercase form of the file names and their uncompressed
     *                                  length in bytes.
     * @param swingCopyJobListener      a reference to a CopyJobListener which is to receive copy job progress updates
     * @return                          an instance of SwingCopyJob for use in tracking and controlling the copy job
     * @throws IllegalStateException    thrown if the destination folder is being written to by a dissimilar copy job
     * @throws IOException              thrown if an IOException is encountered while converting source paths to real paths
     */
    protected static SwingCopyJob getCopyJob(Set<Path> pathsToBeCopied, Path destinationFolder, boolean recursiveCopy, boolean overwriteExistingFiles, Comparator<Path> fileComparator, SwingCopyJobListener swingCopyJobListener) throws IllegalStateException, IOException {
        if (pathsToBeCopied == null) {
            throw new IllegalArgumentException("null reference passed for \"pathsToBeCopied\" parameter");
        }
        if ((destinationFolder == null) || (destinationFolder.getFileName().toString().isEmpty())) {
            throw new IllegalArgumentException("null reference passed for \"destinationFolder\" parameter");
        }
        if ((destinationFolder.toFile().exists()) && (!Files.isDirectory(destinationFolder, LinkOption.NOFOLLOW_LINKS))) {
            throw new IllegalArgumentException("the destination (target) path specified is not a folder");
        }

        // Convert paths to real paths
        Set<Path> realPaths = new LinkedHashSet<>(pathsToBeCopied.size());
        for (Path path : pathsToBeCopied) {
            if ((path == null) || (!extractPath(path).toFile().exists())) {
                throw new IllegalArgumentException("a path included within the \"sourcePathnames\" list does not exist");
            } else {
                if (path instanceof DiscoveredPath) {
                    Path realPath = path.toRealPath(LinkOption.NOFOLLOW_LINKS);
                    Path sourceFolder = ((DiscoveredPath)path).getSourceFolder();

                    realPaths.add(new DiscoveredPath(realPath, sourceFolder));
                } else {
                    realPaths.add(path.toRealPath(LinkOption.NOFOLLOW_LINKS));
                }
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

            /* Throw an IllegalStateException if there is another, ongoing copy job that is copying a file with the
               same pathname to the same destination folder */
            if (jobToReturn == null) {
                for (Path pathToCopy : realPaths) {
                    if (Files.isRegularFile(extractPath(pathToCopy), LinkOption.NOFOLLOW_LINKS)) {
                        for (SwingCopyJob swingCopyJob : swingCopyJobs.keySet()) {
                            for (Path path : swingCopyJob.pathsBeingCopied) {
                                if ((Files.isRegularFile(extractPath(path), LinkOption.NOFOLLOW_LINKS)) &&
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
     * Private constructor for use by class' static getCopyJob factory method.
     *
     * @param pathsBeingCopied          list (Set<Path>) of pathnames of folders and/or files to copy
     * @param destinationFolder         pathname of folder into which to copy sourcePathnames items
     * @param recursiveCopy             boolean value specifying if a recursive search for files/folders within subfolders of folders within the pathsToBeCopied list should be carried out
     * @param overwriteExistingFiles    indicates if files pre-existing files found in the target path should be overwritten if the fileComparator determines that they are different than their
     *                                  source path equivalents
     * @param fileComparator            Function object of type Comparator<Path> defining a compare method with which
     *                                  to compare two files. The compare method should define what it means for two
     *                                  regular files to be the same. If the method evaluates to 0 (equal) then the
     *                                  file in the destination path will be overwritten only if the
     *                                  overwriteExistingFiles parameter has been set to true. If an implementation
     *                                  is not provided then the copy job will use a default implementation that defines
     *                                  equality using the lowercase form of the file names and their uncompressed
     *                                  length in bytes.
     * @param swingCopyJobListener      a reference to a CopyJobListener which is to receive copy job progress updates
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

        worker = new EdtUpdater(this);
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
                    boolean allPathsAreFiles = true;

                    // Check to determine if the each copy job is copying the same set of source paths
                    if (this.pathsBeingCopied.size() == passedInstance.pathsBeingCopied.size()) {
                        for (Path path : this.pathsBeingCopied) {
                            if (!passedInstance.pathsBeingCopied.contains(path)) {
                                listsAreSame = false;
                                break;
                            }

                            /* Check to determine if all the source paths in the lists are regular files, in which
                               case it makes no difference if recursion option is enabled. */
                            if (!Files.isRegularFile(extractPath(path), LinkOption.NOFOLLOW_LINKS)) {
                                allPathsAreFiles = false;
                            }
                        }
                    }

                    // Jobs are equal if they have the same source paths to copy
                    if (listsAreSame) {
                        if (allPathsAreFiles) {
                            // Jobs are considered equal if only files are being copied
                            result = true;
                        } else if (passedInstance.recursiveCopy == this.recursiveCopy) {
                            /* If one or more folders are being copied then the jobs are equal only if they are both
                               using the same recursion setting. */
                            result = true;
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Cancels the copy job. Callers of this method may wish to follow it's use with a call to the awaitCompletion()
     * method, which blocks until the job's background thread has truly completed. Do not, however, call the
     * awaitCompletion() method from the EDT as a thread deadlock will occur.
     *
     * @return  boolean true if the request to cancel was successfully issued to the background worker thread, false
     *          if the request to cancel was ignored (typically because the job has already been completed)
     */
    public boolean cancelJob() {
        return worker.cancelWork();
    }

    /**
     * Returns a list of the files and/or folders being copied.
     *
     * @return  a copy of the list (List<Path>) of pathnames representing the files and/or folders being copied
     */
    public List<Path> getPathsBeingCopied() {
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
        return ((worker.getState() == SwingWorker.StateValue.STARTED) || (backgroundThreadIsRunning.get()));
    }

    /**
     * Blocks until the copy job has completed. This method rethrows internal exceptions that may have occurred
     * within the background thread. Such an exception may cause premature termination of the copy job.
     * Warning: Calling this method from the EDT will result in a thread deadlock.
     *
     * @throws SwingCopyJobException    thrown if an exception was thrown on the copy job's internal worker thread
     */
    public void awaitCompletion() throws SwingCopyJobException, InterruptedException {
        synchronized (lockObject) {
            while ((worker.getState() != SwingWorker.StateValue.DONE) || (backgroundThreadIsRunning.get())) {
                lockObject.wait();
            }
        }

        if (internalWorkerException != null) {
            throw internalWorkerException;
        }
    }

    /**
     * Updates the EDT on copy job progress. This class' background thread handles forwarding of job updates to the
     * EDT. It delegates actual folder and file creation in the destination folder to a background thread (an
     * instance of CopyJobWorkDelegate).
     */
    private class EdtUpdater extends SwingWorker<Void, SimpleImmutableEntry<Path, Integer>> implements CopyWorkResultsReceiver {

        private static final int ONE_HUNDRED_PERCENT = 100;
        private final SwingCopyJob thisSwingCopyJob;
        private final Thread myWorker;
        private final CopyJobWorkDelegate copyJobWorkDelegate;
        private final AtomicReference<Exception> workerException = new AtomicReference<>(null);
        private final Object swingUpdaterLock = new Object();
        private final AtomicBoolean workCompleted = new AtomicBoolean(false);
        private final ConcurrentLinkedQueue<SimpleImmutableEntry<Path, Integer>> workResultsQueue = new ConcurrentLinkedQueue<>();
        private final AtomicBoolean jobCancelled = new AtomicBoolean(false);

        protected boolean cancelWork() {
            boolean result = !jobCancelled.getAndSet(true);
            copyJobWorkDelegate.cancelWork();
            return result;
        }

        /**
         * Constructor for the EdtUpdater class
         *
         * @param enclosingSwingCopyJob         reference to the enclosing SwingCopyJob instance
         * @throws IllegalArgumentException     throw if a null reference is passed for the enclosingSwingCopyJob parameter
         */
        private EdtUpdater(SwingCopyJob enclosingSwingCopyJob) throws IllegalArgumentException {
            if (enclosingSwingCopyJob == null) {
                throw new IllegalArgumentException("null pathname provided for \"enclosingSwingCopyJob\" parameter");
            }

            this.thisSwingCopyJob = enclosingSwingCopyJob;

            copyJobWorkDelegate = new CopyJobWorkDelegate(this, pathsBeingCopied, destinationFolder, recursiveCopy, overwriteExistingFiles, fileComparator);
            myWorker = new Thread(copyJobWorkDelegate);
            myWorker.setName("CopyJobDelegate");
        }

        @Override   // CopyWorkResultsReceiver
        public void workerException(Exception exception) {
            this.workerException.set(exception);

            // Notify worker thread of exception
            synchronized(swingUpdaterLock) {
                swingUpdaterLock.notify();
            }
        }

        @Override   // CopyWorkResultsReceiver
        public void receiveWorkResults(SimpleImmutableEntry<Path, Integer> result) {
            workResultsQueue.add(result);

            // Notify worker thread of work received
            synchronized(swingUpdaterLock) {
                swingUpdaterLock.notify();
            }
        }

        @Override   // CopyWorkResultsReceiver
        public void workCompleted() {
            this.workCompleted.set(true);

            // Notify worker thread of work received
            synchronized(swingUpdaterLock) {
                swingUpdaterLock.notify();
            }
        }

        /**
         * This method is executed on a background thread when the SwingWorker's execute() method is called. Exceptions
         * thrown by this method are thrown as type "ExecutionException", with Throwable.getCause() containing the details
         * of the exception encountered, when the worker's get() method is called. If no exceptions were thrown, the get()
         * method returns the (boolean) value returned by this method.
         *
         * @throws IllegalStateException    thrown if the sourcePathname does not exist
         * @throws SecurityException        thrown if read access on the sourcePathname or write access on the targetPathname has not been granted
         * @throws java.io.IOException      thrown if an IOException occurs
         * @return                          This method is a member of an extended SwingWorker whose specified return
         *                                  value is always null. The return value should be ignored in all cases.
         */
        @Override
        public Void doInBackground() throws IllegalStateException, SecurityException, IOException, InterruptedException {
            backgroundThreadIsRunning.set(true);

            // Start the CopyJobWorkDelegate and wait for its completion, or an exception, to be thrown
            myWorker.start();

            while ((myWorker.isAlive()) && (!workCompleted.get())) {
                synchronized(swingUpdaterLock) {
                    swingUpdaterLock.wait();
                }

                SimpleImmutableEntry<Path, Integer> result;
                while ((result = workResultsQueue.poll()) != null) {
                    if (result.getKey().equals(destinationFolder)) {
                        setProgress(result.getValue());
                    }
                    publish(result);
                }

                Exception e = workerException.get();
                if (e != null) {
                    if (e instanceof SecurityException) {
                        throw (SecurityException) e;
                    } else if (e instanceof IOException) {
                        throw (IOException) e;
                    } else if (e instanceof IllegalStateException) {
                        throw (IllegalStateException) e;
                    } else {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }
            }

            return null;
        }

        /**
         * Notifies registered CopyJobListeners of progress updates published by the background thread.
         * This method is called on the EDT.
         *
         * @param updates   collection of updates published by the SwingWorkers' working thread (i.e. the "doInBackground" method)
         */
        @Override
        public void process(List<SimpleImmutableEntry<Path, Integer>> updates) {
            for (SimpleImmutableEntry<Path, Integer> pair : updates) {
                Map<Path, Integer> copyJobProgressions = swingCopyJobs.get(thisSwingCopyJob);

                /* Ensure this SwingCopyJob is still being tracked before attempting to "put" data. This situation
                   can arise when the job was cancelled via the cancel() method. The cancel() method call's the Done()
                   method could, which may remove the reference to the job prior to chunks of data (i.e. updates) being
                   processed by this method. */
                if (copyJobProgressions != null) {
                    boolean isCopyJobUpdate = false;    // as opposed to a progress update for a particular file or subfolder

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
        }

        /**
         * Notifies registered CopyJobListeners that (a) the copy job been has been completed and (b) of any internal
         * exception that might have caused the job to terminate early. This method is called on the EDT unless the
         * background thread was cancelled using the cancel() method (the enclosing class' cancelJob() method), in
         * which case the method is called on the caller's thread, which may or may not be the EDT.
         */
        @Override
        public void done() {
            setProgress(ONE_HUNDRED_PERCENT);

            swingCopyJobs.remove(thisSwingCopyJob);

            if (thisSwingCopyJob.swingCopyJobListeners.size() > 0) {
                synchronized (thisSwingCopyJob.swingCopyJobListeners) {
                    try {
                        // This method blocks until the background thread (doInBackground() method)) has completed its work
                        get();
                    } catch (InterruptedException | ExecutionException e) {
                        // Note: a CancellationException should not occur here, so don't catch it
                        internalWorkerException = new SwingCopyJobException(e);
                    }

                    if ((internalWorkerException == null) && (!jobCancelled.get())) {
                        for (SwingCopyJobListener listener : thisSwingCopyJob.swingCopyJobListeners) {
                            listener.UpdateCopyJobProgress(thisSwingCopyJob, ONE_HUNDRED_PERCENT);
                        }
                    }

                    for (SwingCopyJobListener listener : thisSwingCopyJob.swingCopyJobListeners) {
                        listener.JobFinished(thisSwingCopyJob, internalWorkerException);
                    }

                    thisSwingCopyJob.swingCopyJobListeners.clear();
                }
            }

            // Signal that the background thread has completed its work (exception or otherwise)
            backgroundThreadIsRunning.set(false);

            // Unblock a caller of the awaitCompletion() method is the enclosing class
            synchronized (lockObject) {
                lockObject.notify();
            }
        }

    }

    /**
     * Default file comparator (function object) for use by CopyJob instances in determining if two files are similar.
     */
    private static class DefaultFileComparator implements Comparator<Path> {

        public static final DefaultFileComparator INSTANCE = new DefaultFileComparator();

        public static DefaultFileComparator getInstance() {
            return INSTANCE;
        }

        private DefaultFileComparator() {
        }

        public int compare(Path path1, Path path2) {
            int result = -1;

            if ((path1 != null) && (path2 != null)) {
                if (Files.isRegularFile(extractPath(path1), LinkOption.NOFOLLOW_LINKS) && Files.isRegularFile(extractPath(path2), LinkOption.NOFOLLOW_LINKS)) {
                    if (path1.getFileName().toString().toLowerCase().equals(path2.getFileName().toString().toLowerCase())) {
                        if (path1.toFile().length() == path2.toFile().length()) {
                            result = 0;
                        }
                    }
                }
            }

            return result;
        }

    }

}
