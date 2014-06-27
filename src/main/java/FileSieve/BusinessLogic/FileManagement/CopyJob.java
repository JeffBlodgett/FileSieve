package FileSieve.BusinessLogic.FileManagement;

import javax.swing.SwingWorker;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.AbstractMap.SimpleImmutableEntry;

public final class CopyJob {

    // protected (instead of private) access only for testing purposes
    protected static final Map<CopyJob, Map<Path, Integer>> copyJobs = Collections.synchronizedMap(new HashMap<CopyJob, Map<Path, Integer>>(10));

    private final Object lockObject = new Object();
    private final List<CopyJobListener> copyJobListeners = Collections.synchronizedList(new ArrayList<CopyJobListener>(10));
    private final BackgroundCopyWorker worker;
    private final boolean recursiveCopy;
    private final Path pathBeingCopied;
    private final Path destinationFolder;
    private final boolean overwriteExistingFiles;
    private Throwable internalWorkerException = null;
    private final Comparator<Path> fileComparator;

    protected static CopyJob getCopyJob(Path pathToBeCopied, Path destinationFolder, boolean recursiveCopy, boolean overwriteExistingFiles, Comparator<Path> fileComparator, CopyJobListener copyJobListener) throws IllegalStateException {
        if (pathToBeCopied == null) {
            throw new NullPointerException("null reference passed for \"pathToBeCopied\" parameter");
        }
        if (destinationFolder == null) {
            throw new NullPointerException("null reference passed for \"destinationFolder\" parameter");
        }

        CopyJob returnJob = null;

        // Ensure another job with the same parameters used for determining CopyJob equality is not already in progress
        synchronized (copyJobs) {
            for (CopyJob copyJob : copyJobs.keySet()) {
                if ((copyJob.pathBeingCopied.equals(pathToBeCopied)) &&
                    (copyJob.destinationFolder.equals(destinationFolder)) &&
                    (copyJob.recursiveCopy == recursiveCopy) &&
                    (copyJob.overwriteExistingFiles == overwriteExistingFiles)
                   ) {
                    if ((copyJobListener != null) && (!copyJob.copyJobListeners.contains(copyJobListener))) {
                        // Add passed CopyJobListener to existing CopyJob instance's listener list
                        copyJob.copyJobListeners.add(copyJobListener);
                    }
                    returnJob = copyJob;
                } else if (copyJob.destinationFolder.startsWith(destinationFolder)) {
                    throw new IllegalStateException("Another copy job is writing to the specified destination folder");
                }
            }

            if (returnJob == null) {
                if (fileComparator == null) fileComparator = FileComparator.getInstance();

                returnJob = new CopyJob(pathToBeCopied, destinationFolder, recursiveCopy, overwriteExistingFiles, fileComparator, copyJobListener);
                CopyJob.copyJobs.put(returnJob, Collections.synchronizedMap(new HashMap<Path, Integer>(200)));
            }
        }

        return returnJob;
    }

    /**
     * Private constructor for use by enclosing class' static factory method "getCopyJob".
     *
     * @param pathBeingCopied
     * @param destinationFolder
     * @param recursiveCopy
     * @param overwriteExistingFiles    boolean true if the CopyJob
     * @param fileComparator            Function object of type Comparator<Path> defining a compare method with which
     *                                  to compare two files. The compare method should define what it means for two
     *                                  regular (existing) files to be the same. If the method evaluates to 0, the
     *                                  file in the destination path will be overwritten only if the
     *                                  overwriteExistingFiles parameter has been set to true. If an implementation
     *                                  is not provided, the CopyJob will use a default implementation that defines
     *                                  equality using the lowercase form of the file names and their uncompressed
     *                                  length in bytes.
     * @param copyJobListener
     */
    private CopyJob(Path pathBeingCopied, Path destinationFolder, boolean recursiveCopy, boolean overwriteExistingFiles, Comparator<Path> fileComparator, CopyJobListener copyJobListener) {
        this.pathBeingCopied = pathBeingCopied;
        this.destinationFolder = destinationFolder;
        this.recursiveCopy = recursiveCopy;
        this.fileComparator = fileComparator;
        this.overwriteExistingFiles = overwriteExistingFiles;

        // May be null if no listener is to receive progress updates
        if (copyJobListener != null) {
            this.copyJobListeners.add(copyJobListener);
        }

        worker = new BackgroundCopyWorker(this);
        worker.execute();

    }

    /**
     * Returns true if the passed CopyJob is considered equivalent to this CopyJob.
     *
     * @param obj   reference to CopyJob to be compared to this instance for equality
     * @return      boolean true if the passed CopyJob is equivalent in state to this CopyJob
     */
    @Override
    public boolean equals(Object obj) {
        boolean result = false;

        if (obj != null) {
            if (obj == this) {
                result = true;
            } else if (obj instanceof CopyJob) {
                CopyJob passedInstance = (CopyJob)obj;

                if ( ((passedInstance.pathBeingCopied.equals(this.pathBeingCopied)) &&
                     (passedInstance.destinationFolder.equals(this.destinationFolder))) &&
                     (passedInstance.recursiveCopy == this.recursiveCopy) && (passedInstance.overwriteExistingFiles == this.overwriteExistingFiles)
                   ) {
                    result = true;
                }
            }
        }

        return result;
    }

    public boolean Cancel() {
        return worker.cancel(true);
    }

    /**
     * Returns the Path of the file or folder being copied.
     *
     * @return  Path object representing the file or folder being copied
     */
    public Path getPathBeingCopied() {
        return this.pathBeingCopied;
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

        private final CopyJob thisCopyJob;
        private static final int COPY_JOB_AT_100_PERCENT = 100;
        private static final int PATHNAME_COPY_AT_100_PERCENT = 100;
        private static final int ONE_HUNDRED_PERCENT = 100;
        private static final int ZERO_PERCENT = 0;
        private long totalBytes = 0L;
        private long copiedBytes = 0L;
        private int totalPercentCopied = 0;

        protected BackgroundCopyWorker(CopyJob enclosingCopyJob) throws IllegalArgumentException {
            if (enclosingCopyJob == null) {
                throw new IllegalArgumentException("null pathname provided for \"enclosingCopyJob\" parameter");
            }

            this.thisCopyJob = enclosingCopyJob;
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
                Map<Path, Integer> copyJobProgressions = copyJobs.get(thisCopyJob);

                boolean isCopyJobUpdate = false;    // (as opposed to a progress update for a particular file or subfolder)

                /* Updates coming from the background thread are identified using the targeted file or folder of the
                   of the operation or the copy jobs destination folder */
                if (pair.getKey().equals(thisCopyJob.destinationFolder)) {
                    isCopyJobUpdate = true;
                }

                Integer oldValue = copyJobProgressions.put(pair.getKey(), pair.getValue());
                if ((oldValue == null) || (!oldValue.equals(pair.getValue()))) {
                    // Notify CopyJobListener
                    if (thisCopyJob.copyJobListeners.size() > 0) {
                        synchronized (thisCopyJob.copyJobListeners) {
                            for (CopyJobListener listener : thisCopyJob.copyJobListeners) {
                                if (isCopyJobUpdate) {
                                    listener.UpdateCopyJobProgress(thisCopyJob, pair.getValue().intValue());
                                } else {
                                    listener.UpdatePathnameCopyProgress(thisCopyJob, pair.getKey(), pair.getValue().intValue());
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

            copyJobs.remove(thisCopyJob);

            if (thisCopyJob.copyJobListeners.size() > 0) {
                synchronized (thisCopyJob.copyJobListeners) {
                    try {
                        get();
                    } catch (InterruptedException | ExecutionException e) {
                        for (CopyJobListener listener : thisCopyJob.copyJobListeners) {
                            listener.InternalCopyJobException(thisCopyJob, e);
                        }
                    }

                    for (CopyJobListener listener : thisCopyJob.copyJobListeners) {
                        listener.UpdateCopyJobProgress(thisCopyJob, COPY_JOB_AT_100_PERCENT);
                    }

                    thisCopyJob.copyJobListeners.clear();
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
                if (Files.exists(thisCopyJob.pathBeingCopied)) {
                    retrieveTotalBytes(thisCopyJob.pathBeingCopied);
                } else {
                    throw new IllegalStateException("source pathname does not exist");
                }
            } catch (SecurityException e) {
                throw new SecurityException("missing read access on source path (root, subfolder, or file) and/or write access on target path", e);
            } catch (IOException e) {
                throw new IOException("IOException while calculating bytes to copy", e);
            }

            try {
                copyPaths(thisCopyJob.pathBeingCopied, thisCopyJob.destinationFolder);
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
                            if (Files.isDirectory(path) && (!path.equals(thisCopyJob.destinationFolder))) {
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
                if (!sourcePath.equals(thisCopyJob.destinationFolder)) {

                    if (!Files.exists(targetPath)) {
                        targetPath = Files.createDirectories(targetPath);
                        if (targetPath.equals(thisCopyJob.destinationFolder)) {
                            publish(new AbstractMap.SimpleImmutableEntry(targetPath, ZERO_PERCENT));
                        }
                    }

                    if (Files.isDirectory(sourcePath)) {
                        List<Path> filePaths = new ArrayList<Path>(50);
                        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(sourcePath)) {
                            for (Path path : dirStream) {
                                filePaths.add(path);
                            }
                        }

                        for (Path path : filePaths) {
                            if (Files.isDirectory(path)) {
                                Path folderToCreate = path.subpath(sourcePath.getNameCount(), path.getNameCount());

                                targetPath = targetPath.resolve(folderToCreate);

                                Files.createDirectory(targetPath);
                                publish(new SimpleImmutableEntry(targetPath, PATHNAME_COPY_AT_100_PERCENT));

                            }

                            if ((Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) || ((Files.isDirectory(path)) && thisCopyJob.recursiveCopy)) {
                                // Recursive call: copy file or folder specified by "path" variable to the "targetPath" folder
                                copyPaths(path, targetPath);
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

            if ((!filesAreSimilar) || (filesAreSimilar && overwriteExistingFiles))  {
                long soFar = 0L;    // file bytes copied thus far
                int sourceByte;
                int filePercentCopied;
                int totalPercentPreviouslyCopied = totalPercentCopied;
                int pathnameProgress = ZERO_PERCENT;

                try (
                        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileToCopy.toFile()));
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(target.toFile()));
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
                            publish(new SimpleImmutableEntry(thisCopyJob.destinationFolder, totalPercentCopied));
                        }

                        /* Update the progress of the individual file copy if progress has incremented by at least 1 percent
                           and is not yet 100 percent complete */
                        filePercentCopied = (int) (++soFar * ONE_HUNDRED_PERCENT / fileBytes);
                        if ((pathnameProgress != filePercentCopied) && (filePercentCopied < ONE_HUNDRED_PERCENT)) {
                            pathnameProgress = filePercentCopied;
                            publish(new SimpleImmutableEntry(target, pathnameProgress));
                        }
                    }

                    // No need to set "pathnameProgress" variable to 100... just publish and move on
                    publish(new SimpleImmutableEntry(target, PATHNAME_COPY_AT_100_PERCENT));

                } catch (IOException e) {
                    // Backtrack... set+publish the total job progress to the value had prior to this attempted file
                    // and set+publish the progress for the failed copy as 0 percent
                    setProgress((int) ((totalPercentPreviouslyCopied + fileBytes) * ONE_HUNDRED_PERCENT / totalBytes));
                    publish(new SimpleImmutableEntry(thisCopyJob.destinationFolder, getProgress()));
                    publish(new SimpleImmutableEntry(target, 0));

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
                    publish(new SimpleImmutableEntry(thisCopyJob.destinationFolder, totalPercentCopied));
                }
            }
        }

    } // static class BackgroundCopyWorker extends SwingWorker<Void, SimpleImmutableEntry<Path, Integer>>

    /**
     * Default file comparator (function object) for use by CopyJob instances in determining if two files are similar.
     */
    private static class FileComparator implements Comparator<Path> {

        public static final FileComparator INSTANCE = new FileComparator();

        public static FileComparator getInstance() {
            return INSTANCE;
        }

        private FileComparator() { }

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
