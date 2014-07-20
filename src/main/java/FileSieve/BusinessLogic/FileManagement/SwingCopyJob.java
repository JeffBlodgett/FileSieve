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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final AtomicBoolean backgroundThreadIsRunning = new AtomicBoolean(false);
    private Throwable internalWorkerException = null;

    /**
     * Static factory method for creating or retrieving a reference to an equivalent (and ongoing) SwingCopyJob.
     *
     * @param pathsToBeCopied           list of Path objects abstracting folders and/or files to copy
     * @param destinationFolder         destination folder to which file and folder copies are to be placed
     * @param recursiveCopy             boolean value indicating whether or not folders should be search recursively for additional folders and files
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

    /**
     * Cancels the copy job. Callers of this method may wish to follow it's use with a call to the awaitCompletion()
     * method, which blocks until the job's background thread has truly completed.
     *
     * @return  boolean true if the request to cancel was successfully issued to the background worker thread, false
     *          if the request to cancel was ignored (typically because the job has already been completed)
     */
    public boolean cancelJob() {
        return worker.cancel(false);
    }

    /**
     * Returns a list of the files and/or folders being copied.
     *
     * @return  List<Path> representing the files and/or folders being copied
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
        return ((worker.getState() == SwingWorker.StateValue.DONE) && (!backgroundThreadIsRunning.get()));
    }

    /**
     * Blocks until the copy job has completed. This method rethrows internal exceptions that may have occurred
     * within the background thread. Such an exception may cause premature termination of the copy job.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void awaitCompletion() throws InterruptedException, ExecutionException {
        synchronized (lockObject) {
            while ((worker.getState() != SwingWorker.StateValue.DONE) || (backgroundThreadIsRunning.get())) {
                try {
                    lockObject.wait();
                } catch (InterruptedException e) {
                    // catch spurious interrupts

                    // todo ACK -this looks like a dropped exception (not handled) this almost always a bad, bad practice
                    // errors will happen and then ignored but the problem might cause issues later and will be very hard to debug.
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
     * Extended SwingWorker for internal use by SwingCopyJob. Executes the copy operation on a background thread.
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
        private List<Path> foldersCreatedInTarget = new ArrayList<>();

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
         * background thread was cancelled using via the cancel() method (enclosing class' cancelJob() method), in
         * which case the method is called on the caller's thread (which may or may not be the EDT).
         */
        @Override
        public void done() {
            setProgress(COPY_JOB_AT_100_PERCENT);

            swingCopyJobs.remove(thisSwingCopyJob);

            if (thisSwingCopyJob.swingCopyJobListeners.size() > 0) {
                synchronized (thisSwingCopyJob.swingCopyJobListeners) {
                    try {
                        // This method blocks until the background thread (doInBackground() method)) has completed its work
                        get();
                    } catch (CancellationException e) {
                        // Background task was cancelled via cancel() method
                    } catch (InterruptedException | ExecutionException e) {

                        // todo - almost duplicated code in the catch and the finally. - not so good - try to avoid.
                        internalWorkerException = e;
                        for (SwingCopyJobListener listener : thisSwingCopyJob.swingCopyJobListeners) {
                            listener.InternalCopyJobException(thisSwingCopyJob, e);
                        }
                    } finally {
                        for (SwingCopyJobListener listener : thisSwingCopyJob.swingCopyJobListeners) {
                            listener.UpdateCopyJobProgress(thisSwingCopyJob, COPY_JOB_AT_100_PERCENT);
                        }

                        thisSwingCopyJob.swingCopyJobListeners.clear();

                        // Signal that the background thread has completed its work (exception or otherwise)
                        backgroundThreadIsRunning.set(false);
                    }
                }
            }

            // Unblock a caller of the awaitCompletion() method is the enclosing class
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
            backgroundThreadIsRunning.set(true);

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

            // todo when I see code like this I it scream two methods rather than one.

            try {
                for (Path path : thisSwingCopyJob.pathsBeingCopied) {
                    if (!isCancelled()) {
                        copyPaths(path, thisSwingCopyJob.destinationFolder);
                    }
                }
            } catch (SecurityException e) {
                throw new SecurityException("SecurityException while reading or writing files/folders in the source or target root", e);
            } catch (IOException e) {
                throw new SecurityException("IOException while reading or writing files/folders in the source or target root", e);
            }


            // todo maybe a comment that returning null is OK in this weird return Void situation otherwise methods should really not return
            // null - it puts the burden of the check on the client which can forget to do it, in which case you have a bug -
            // in this case, I understand it is OK but a comment would help.
            return null;
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

            // todo another crazy long method - perhaps the worst case yet. It is pretty hard to follow
            // consider breaking into smaller methods with doc.

            if (!isCancelled()) {
                if (!sourcePath.equals(thisSwingCopyJob.destinationFolder)) {

                    if (!Files.exists(targetPath)) {
                        targetPath.toFile().mkdirs();
                        if (targetPath.equals(thisSwingCopyJob.destinationFolder)) {
                            publish(new AbstractMap.SimpleImmutableEntry<>(targetPath, ZERO_PERCENT));
                        }
                    }

                    if (Files.isDirectory(sourcePath)) {
                        if (recursiveCopy) {
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
                                        newTargetPath.toFile().mkdir();
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
                                            newTargetPath.toFile().mkdir();
                                        }

                                        copyFile(path, newTargetPath);
                                    } else {
                                        copyFile(path, targetPath);
                                    }
                                }
                            }
                        } else {
                            Path pathToCreateInTargetFolder = null;

                            // Determine if the folder's parent was previously created within the target folder
                            for (int i = foldersCreatedInTarget.size() - 1; i >= 0; --i) {
                                if (sourcePath.getParent().endsWith(foldersCreatedInTarget.get(i))) {
                                    pathToCreateInTargetFolder = foldersCreatedInTarget.get(i).resolve(sourcePath.getFileName());
                                    break;
                                }
                            }

                            /* If folder's parent was previously created within the target folder then create the
                               folder within its parent (folder), else create the folder in the root of the target */
                            if (pathToCreateInTargetFolder != null) {
                                Path newPathToCreate = targetPath.resolve(pathToCreateInTargetFolder);
                                if (!Files.exists(newPathToCreate)) {
                                    newPathToCreate.toFile().mkdir();
                                    foldersCreatedInTarget.add(pathToCreateInTargetFolder);
                                }
                            } else if (!Files.exists(targetPath.resolve(sourcePath.getFileName()))) {
                                targetPath.resolve(sourcePath.getFileName()).toFile().mkdir();
                                foldersCreatedInTarget.add(sourcePath.getFileName());
                            }
                        }

                    } else {
                        Path parentPathCreatedInTargetFolder = null;

                        // Determine if the file's parent folder was created within the target folder
                        for (int i = foldersCreatedInTarget.size() - 1; i >= 0; --i) {
                            if (sourcePath.getParent().endsWith(foldersCreatedInTarget.get(i))) {
                                parentPathCreatedInTargetFolder = foldersCreatedInTarget.get(i);
                                break;
                            }
                        }

                        if (parentPathCreatedInTargetFolder != null) {
                            copyFile(sourcePath, targetPath.resolve(parentPathCreatedInTargetFolder));
                        } else {
                            // Copy file to the root of the target folder
                            copyFile(sourcePath, targetPath);
                        }

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

        // todo I think it is great that you are documenting private methods but be consistent in doing so.

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
