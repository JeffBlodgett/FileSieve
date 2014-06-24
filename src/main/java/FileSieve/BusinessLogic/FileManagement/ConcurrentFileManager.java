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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.AbstractMap.SimpleImmutableEntry;

/**
 * Concrete file manager class that inherits FileCopier and FileDeleter interface implementations from abstract class
 * "FileManager" and provides implementations for the FileCopier interface's copyPathname method.
 */
class ConcurrentFileManager extends FileManager<Boolean> {

    private int workerThreadLimit;
    protected Map<Path, Map<Path, Integer>> copyJobs = Collections.synchronizedMap(new HashMap<Path, Map<Path, Integer>>(10));

    protected ConcurrentFileManager(int workerThreadLimit) {
        this.workerThreadLimit = workerThreadLimit;
    }

    /**
     * Copy's a file or folder, recursively or not, to a target pathname.
     * // TODO overwriteFile and ifSizeDiffers arguments are currently unused
     *
     * @param sourcePathname        pathname of file or folder to copy
     * @param targetPathname        pathname of file or folder to create/write
     * @param recursionEnabled      recursive search for and copying of files and subfolders with subfolders of the sourcePathname
     * @param overwriteFile         indicates if existing files in the target path should be overwritten
     * @param ifSizeDiffers         if "overwriteExisting" argument is true, overwrites existing files only if their size differs
     * @return                      Boolean value indicating if copy job was started
     * @throws java.io.IOException  never thrown by this implementation
     */
    @Override
    public Boolean copyPathname(Path sourcePathname, Path targetPathname, boolean recursionEnabled, boolean overwriteFile, boolean ifSizeDiffers) {
        if (sourcePathname == null) throw new NullPointerException("null path provided for sourcePathname parameter");
        if (targetPathname == null) throw new NullPointerException("null path provided for targetPathname parameter");

        if (!Files.exists(sourcePathname)) throw new IllegalArgumentException("file or folder specified by sourcePathname parameter does not exist");

        boolean copyJobStarted = false;

        BackgroundCopyWorker copyJob = getBackgroundCopyWorker(sourcePathname, targetPathname);
        if (copyJob != null) {
            copyJob.setOverwriteExistingFiles(overwriteFile);
            copyJob.setOverwriteIfSizeDiffers(ifSizeDiffers);
            copyJob.setRecursionEnabled(recursionEnabled);

            copyJob.execute();
            copyJobStarted = true;
        }

        return copyJobStarted;
    }

    private BackgroundCopyWorker getBackgroundCopyWorker(Path sourcePathname, Path targetPathname) {
        BackgroundCopyWorker worker = null;

        // Ensure another job with the same sourcePathname is not already in progress
        if (!copyJobs.containsKey(sourcePathname)) {
            copyJobs.put(sourcePathname, Collections.synchronizedMap(new HashMap<Path, Integer>(200)));
            worker = new BackgroundCopyWorker(sourcePathname, targetPathname);
        }

        return worker;
    }

    /**
     * SwingWorker capable of copying a file or folder from a source pathname to a target pathname. A
     * PropertyChangeSupport object of the enclosing class is used to fire events to indicate overall progress of the
     * "copy job", progress of each file copy or subfolder creation, and th;e when the copy job has finished.
     * Original concept taken from post by "Eng.Fouad" on stackoverflow.com
     * http://stackoverflow.com/questions/13574461/need-to-have-jprogress-bar-to-measure-progress-when-copying-directories-and-file
     */
    class BackgroundCopyWorker extends SwingWorker<Void, SimpleImmutableEntry<Path, Integer>> {

        private Path sourcePathname;
        private Path targetPathname;
        private long totalBytes = 0L;
        private long copiedBytes = 0L;
        private int totalPercentCopied = 0;
        //private boolean noErrors = true;
        boolean overwriteExistingFiles = false;
        boolean overwriteIfSizeDiffers = false;
        boolean recursionEnabled = true;
        private static final int COPY_JOB_AT_100_PERCENT = 100;
        private static final int PATHNAME_COPY_AT_100_PERCENT = 100;
        private static final int ONE_HUNDRED_PERCENT = 100;
        private static final int ZERO_PERCENT = 0;

        protected BackgroundCopyWorker(Path sourcePathname, Path targetPathname) throws IllegalArgumentException, SecurityException {
            if ((sourcePathname == null) || (targetPathname == null)) {
                throw new IllegalArgumentException("null pathname provided for source or target");
            }

            this.sourcePathname = sourcePathname;
            this.targetPathname = targetPathname;
        }

        public void setOverwriteExistingFiles(boolean overwriteExistingFiles) {
            if (getState() == StateValue.PENDING) {
                this.overwriteExistingFiles = overwriteExistingFiles;
            }
        }

        public boolean getOverwriteExistingFiles() {
            return overwriteExistingFiles;
        }

        public void setOverwriteIfSizeDiffers(boolean overwriteIfSizeDiffers) {
            if (getState() == StateValue.PENDING) {
                this.overwriteIfSizeDiffers = overwriteIfSizeDiffers;
            }
        }

        public boolean getOverwriteIfSizeDiffers() {
            return overwriteIfSizeDiffers;
        }

        /**
         * Enables or disables a recursive search for and copying of files and folders that lie within folders found
         * in the constructor's given sourcePathname. Attempts to set this value have no effect if the copy job has
         * already begun, or has finished.
         *
         * @param recursionEnabled  the true or false setting to apply to the instances
         * @return                  the new setting applied to the instance by this method
         */
        public boolean setRecursionEnabled(boolean recursionEnabled) {
            if (getState() == StateValue.PENDING) {
                this.recursionEnabled = recursionEnabled;
            }

            return this.recursionEnabled;
        }

        /**
         * Returns a boolean value indicating if the worker is set to perform recursive search and copies of folders
         * found within the sourcePathname provided to the instance's constructor.
         *
         * @return
         */
        public boolean getRecursionEnabled() {
            return recursionEnabled;
        }

        /**
         * Fires property changes to notify property change listeners of updates published by the SwingWorker's
         * working thread. This method is called on the EDT.
         *
         * @param chunks    Updates published by the SwingWorkers working thread ("doInBackground" method)
         */
        @Override
        public void process(List<SimpleImmutableEntry<Path, Integer>> chunks) {
            for (SimpleImmutableEntry<Path, Integer> pair : chunks) {
                Map<Path, Integer> copyJobProgressions = copyJobs.get(this.sourcePathname);

                String descriptor;
                if (pair.getKey().equals(this.targetPathname)) {
                    descriptor = "totalCopyProgress";
                } else {
                    descriptor = this.targetPathname.toString();
                }

                Integer oldValue = copyJobProgressions.put(pair.getKey(), pair.getValue());
                if ((oldValue == null) || (!oldValue.equals(pair.getValue()))) {
                    // Fire property change event on "pcs" (property change support) object of this ConcurrentFileManager's inherited FileManager class
                    pcs.firePropertyChange(this.sourcePathname.toString(),
                                           new SimpleImmutableEntry<String, Integer>(descriptor, oldValue),
                                           new SimpleImmutableEntry<String, Integer>(descriptor, pair.getValue()));
                }
            }
        }

        /**
         * Called on the EDT when the SwingWorker has finished all work
         */
        @Override
        public void done() {
            setProgress(COPY_JOB_AT_100_PERCENT);

            Integer oldValue = copyJobs.get(this.sourcePathname).put(this.targetPathname, COPY_JOB_AT_100_PERCENT);
            copyJobs.remove(this.sourcePathname);

            /* Notify property change listeners of unhandled exceptions that occurred within this SwingWorker's
               doInBackground method */
            try {
                get();
            } catch (InterruptedException | ExecutionException e) {
                pcs.firePropertyChange(this.sourcePathname.toString(), null, e);
            }

            pcs.firePropertyChange(this.sourcePathname.toString(),
                    new SimpleImmutableEntry<String, Integer>("totalCopyProgress", oldValue),
                    new SimpleImmutableEntry<String, Integer>("totalCopyProgress", COPY_JOB_AT_100_PERCENT));
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
                if (Files.exists(sourcePathname)) {
                    retrieveTotalBytes(sourcePathname);
                } else {
                    throw new IllegalStateException("source pathname does not exist");
                }
            } catch (SecurityException e) {
                throw new SecurityException("missing read access on source path (root, subfolder, or file) and/or write access on target path", e);
            } catch (IOException e) {
                throw new IOException("IOException while calculating bytes to copy", e);
            }

            try {
                copyPaths(sourcePathname, targetPathname);
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
                        // Exclude target folder if it is a subfolder of the source folder
                        if (Files.isDirectory(path) && (!path.equals(this.targetPathname))) {
                            retrieveTotalBytes(path);
                        } else if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                            totalBytes += path.toFile().length();
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
            if (!sourcePath.equals(this.targetPathname)) {

                if (!Files.exists(targetPath)) {
                    targetPath = Files.createDirectories(targetPath);
                    if (targetPath.equals(this.targetPathname)) {
                        publish(new SimpleImmutableEntry(targetPath, ZERO_PERCENT));
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

                        if ((Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) || ((Files.isDirectory(path)) && recursionEnabled)) {
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

        /**
         * Private helper method, called by copyPaths method, for copying a single file (the sourcePathname)
         *
         * @param fileToCopy            file to copy, passed as a Path
         * @param copyTarget            folder to which copy is to be placed
         * @throws SecurityException    thrown if the security manager denies read access to the original file or write
         *                              access to the folder to contain the copy
         * @throws IOException          throw if an IOException occurs during read/write operations
         */
        private void copyFile(Path fileToCopy, Path copyTarget) throws SecurityException, IOException {
            copyTarget = copyTarget.resolve(fileToCopy.getFileName());

            long fileBytes = fileToCopy.toFile().length();  // size of file in bytes
            long soFar = 0L;                                // file bytes copied thus far

            int sourceByte;
            int filePercentCopied;
            int totalPercentPreviouslyCopied = totalPercentCopied;
            int pathnameProgress = ZERO_PERCENT;

            try (
                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileToCopy.toFile()));
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(copyTarget.toFile()));
            ) {
                /* Copy file one byte at time. BufferedInputStream and BufferedOutputStream have, well, buffers so
                   so this isn't as slow as it might at first seem */
                while ((sourceByte = bis.read()) != -1) {
                    bos.write(sourceByte);

                    /* Update copy job's total progress if progress has incremented by at least 1 percent and is not
                       yet 100 percent complete */
                    totalPercentCopied = (int) (++copiedBytes * ONE_HUNDRED_PERCENT / totalBytes);
                    if ((getProgress() != totalPercentCopied) && (totalPercentCopied < ONE_HUNDRED_PERCENT)) {
                        setProgress(totalPercentCopied);
                        publish(new SimpleImmutableEntry(this.targetPathname, getProgress()));
                    }

                    /* Update the progress of the individual file copy if progress has increment by at least 1 percent
                       and is not yet 100 percent complete */
                    filePercentCopied = (int) (++soFar * ONE_HUNDRED_PERCENT / fileBytes);
                    if ((pathnameProgress != filePercentCopied) && (filePercentCopied < ONE_HUNDRED_PERCENT)) {
                        pathnameProgress = filePercentCopied;
                        publish(new SimpleImmutableEntry(copyTarget, pathnameProgress));
                    }
                }

                pathnameProgress = PATHNAME_COPY_AT_100_PERCENT;
                publish(new SimpleImmutableEntry(copyTarget, PATHNAME_COPY_AT_100_PERCENT));

            } catch (IOException e) {
                setProgress((int) ((totalPercentPreviouslyCopied + fileBytes) * ONE_HUNDRED_PERCENT / totalBytes));
                publish(new SimpleImmutableEntry(this.targetPathname, getProgress()));
                publish(new SimpleImmutableEntry(copyTarget, null));

                try {
                    if (Files.exists(copyTarget) && ((copyTarget.toFile().length() == 0L) || (soFar > 0L))) {
                        Files.delete(copyTarget);
                        throw new IOException("An IOException occurred while copying a file. An incomplete copy was not left in the destination folder.", e);
                    }
                } catch (IOException ex) {
                    throw new IOException("An IOException occurred while copying a file. An incomplete copy may have been left in the destination folder.", ex);
                }
            }
        }

    } // class BackgroundCopyWorker extends SwingWorker<Void, AbstractMap.SimpleImmutableEntry<Path, Integer>>

} // class SwingWorkerFileManagement extends BasicFileManager
