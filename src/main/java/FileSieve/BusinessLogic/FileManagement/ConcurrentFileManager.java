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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.AbstractMap.SimpleImmutableEntry;

/**
 * File management class defining methods for the acquisition of SwingWorker instances to be used for potentially
 * long-running file copy operations
 */
class ConcurrentFileManager extends FileManager {

    private int workerThreadLimit;
    protected Map<Path, Map<Path, Integer>> copyJobs = Collections.synchronizedMap(new HashMap<Path, Map<Path, Integer>>(10));

    protected ConcurrentFileManager(int workerThreadLimit) {
        this.workerThreadLimit = workerThreadLimit;
    }

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
     * @param recursionEnabled  recursive search for files within subfolders
     * @param overwriteFile     indicates if existing files in the target path should be overwritten
     * @param ifSizeDiffers     if "overwriteExisting" argument is true, overwrites existing files only if their size differs
     */
    @Override
    public boolean copyPathname(Path sourcePathname, Path targetPathname, boolean recursionEnabled, boolean overwriteFile, boolean ifSizeDiffers) {
        if ((sourcePathname == null) || (targetPathname == null)) throw new NullPointerException("null pathname provided for source or target");

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

    protected BackgroundCopyWorker getBackgroundCopyWorker(Path sourcePathname, Path targetPathname) {
        BackgroundCopyWorker worker = null;

        // Ensure another job with the same sourcePathname is not already in progress
        if (!copyJobs.containsKey(sourcePathname)) {
            copyJobs.put(sourcePathname, Collections.synchronizedMap(new HashMap<Path, Integer>(200)));
            worker = new BackgroundCopyWorker(sourcePathname, targetPathname);
        }

        return worker;
    }

    // TODO Following code commented out but is to be worked into support for more than one concurrent worker thread
//    /**
//     * Convenience method for copying multiple files or folders to a target, each on its own SwingWorker (thread).
//     * Uses the pathnameCopyProvider method defined by this class to return one SwingWorker instance per file/folder
//     * to be copied/created.
//     *
//     * @param sourcePathnames   pathnames of files and folders to copy
//     * @param targetFolder      pathname of folder to which files and folders are to be created/written
//     * @param recursionEnabled  recursive search for files within subfolders
//     * @param overwriteFiles    indicates if existing files in the target path should be overwritten
//     * @param ifSizeDiffers     if "overwriteExisting" argument is true, overwrites existing files only if their size differs
//     * @return                  Map containing instances of a SwingWorkers capable of performing the copy operations
//     *                          and providing progress updates in the form of an Integer value representing the
//     *                          percentage of the copy operation completed
//     */
//    public Map<Path, BackgroundCopyWorker> getCopyProviders(Set<Path> sourcePathnames, Path targetFolder, boolean recursionEnabled, boolean overwriteFiles, boolean ifSizeDiffers) {
//        if ((sourcePathnames == null) || (targetFolder == null)) throw new NullPointerException("null pathname provided for sources or target");
//
//        // LinkedHashMap maintains a order via a doubly-linked list. Iterator can be used to traverse keys in the order in which they were inserted.
//        Map<Path, BackgroundCopyWorker> workers = new LinkedHashMap<Path, BackgroundCopyWorker>(sourcePathnames.size());
//        for (Path path : sourcePathnames) {
//            workers.put(path, copyProvider(path, targetFolder, recursionEnabled, overwriteFiles, ifSizeDiffers));
//        }
//
//        return workers;
//    }

    /**
     * SwingWorker capable of copying a file or folder from a source pathname to a target pathname.
     * while providing
     * progress updates in the form of an Integer representing the percentage of the copy operation that is complete
     */
    class BackgroundCopyWorker extends SwingWorker<Boolean, SimpleImmutableEntry<Path, Integer>> {

        private Path sourcePathname;
        private Path targetPathname;
        private long totalBytes = 0L;
        private long copiedBytes = 0L;
        private int totalPercentCopied = 0;
        private boolean noErrors = true;
        boolean overwriteExistingFiles = false;
        boolean overwriteIfSizeDiffers = false;
        boolean recursionEnabled = true;
        private volatile int fileProgress;

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

        public void setRecursionEnabled(boolean recursionEnabled) {
            if (getState() == StateValue.PENDING) {
                this.recursionEnabled = recursionEnabled;
            }
        }

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
            setProgress(100);

            Integer oldValue = copyJobs.get(this.sourcePathname).put(this.targetPathname, 100);
            copyJobs.remove(this.sourcePathname);

            pcs.firePropertyChange(this.sourcePathname.toString(),
                    new SimpleImmutableEntry<String, Integer>("totalCopyProgress", oldValue),
                    new SimpleImmutableEntry<String, Integer>("totalCopyProgress", 100));

            // Notify property change listeners of unhandled exceptions that occurred within this SwingWorker's doInBackground method
            try {
                get();
            } catch (InterruptedException e) {
                pcs.firePropertyChange(this.sourcePathname.toString(), null, e);
            } catch (ExecutionException e) {
                pcs.firePropertyChange(this.sourcePathname.toString(), null, e.getCause());
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
        public Boolean doInBackground() throws IllegalStateException, SecurityException, IOException {
            boolean allCopiesSuccessful = false;

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
                copyFiles(sourcePathname, targetPathname);
                allCopiesSuccessful = true;
            } catch (SecurityException e) {
                throw new SecurityException("SecurityException while reading or writing files/folders in the source or target root", e);
            } catch (IOException e) {
                throw new SecurityException("IOException while reading or writing files/folders in the source or target root", e);
            }

            return (allCopiesSuccessful && noErrors);
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
         * Called by doInBackground() method to handle folder and file copy operations. Exceptions are thrown if
         *
         *
         * @param sourcePathname        source file or folder that is to be copied
         * @param targetPathname        target file or folder to be copied
         * @throws SecurityException    thrown if
         * @throws IOException
         */
        private void copyFiles(Path sourcePathname, Path targetPathname) throws SecurityException, IOException {
            if (!sourcePathname.equals(this.targetPathname)) {
                if (!Files.exists(targetPathname)) {
                    targetPathname = Files.createDirectories(targetPathname);
                    if (targetPathname.equals(this.targetPathname)) {
                        publish(new SimpleImmutableEntry(targetPathname, 0));
                    }
                }

                if (Files.isDirectory(sourcePathname)) {
                    List<Path> filePaths = new ArrayList<Path>(50);
                    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(sourcePathname)) {
                        for (Path path : dirStream) {
                            filePaths.add(path);
                        }
                    }

                    for (Path path : filePaths) {
                        if (Files.isDirectory(path)) {
                            Path folderToCreate = path.subpath(sourcePathname.getNameCount(), path.getNameCount());

                            targetPathname = targetPathname.resolve(folderToCreate);

                            Files.createDirectory(targetPathname);
                            publish(new SimpleImmutableEntry(targetPathname, 100));

                        }

                        if ((Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) || ((Files.isDirectory(path)) && recursionEnabled)) {
                            try {
                                copyFiles(path, targetPathname);
                            } catch (SecurityException | IOException e) {
                                noErrors = false;
                                // Ignore exceptions... attempts to copy other folders/files will continue
                            }
                        }
                    }

                } else {
                    targetPathname = targetPathname.resolve(sourcePathname.getFileName());

                    long fileBytes = sourcePathname.toFile().length();
                    long soFar = 0L;
                    //publish(new AbstractMap.SimpleImmutableEntry(targetPathname.toString(), 0));

                    int sourceByte;
                    int filePercentCopied = 0;
                    int totalPercentPreviouslyCopied = totalPercentCopied;

                    try (
                            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourcePathname.toFile()));
                            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(targetPathname.toFile()));
                    ) {
                        while ((sourceByte = bis.read()) != -1) {
                            bos.write(sourceByte);

                            totalPercentCopied = (int) (++copiedBytes * 100 / totalBytes);
                            if ((getProgress() != totalPercentCopied) && (totalPercentCopied != 100)) {
                                setProgress(totalPercentCopied);
                                publish(new SimpleImmutableEntry(this.targetPathname, getProgress()));
                            }

                            filePercentCopied = (int) (++soFar * 100 / fileBytes);
                            if ((fileProgress != filePercentCopied) && (filePercentCopied != 100)) {
                                fileProgress = filePercentCopied;
                                publish(new SimpleImmutableEntry(targetPathname, fileProgress));
                            }
                        }

                        fileProgress = 100;
                        publish(new SimpleImmutableEntry(targetPathname, 100));

                    } catch (SecurityException | IOException e) {
                        noErrors = false;

                        setProgress((int) ((totalPercentPreviouslyCopied + fileBytes) * 100 / totalBytes));
                        publish(new SimpleImmutableEntry(this.targetPathname, getProgress()));
                        publish(new SimpleImmutableEntry(targetPathname, null));

                        try {
                            if (Files.exists(targetPathname) && ((targetPathname.toFile().length() == 0L) || (soFar > 0))) {
                                Files.delete(targetPathname);
                            }
                        } catch (SecurityException | IOException ex) {
                            // Ignore exceptions - this is a best attempt at deleting the written file, which was cut short
                        }
                    }
                }
            }
        }

//        private void setFileProgress(int fileProgress) {
//            if (fileProgress == this.fileProgress) return;
//
//            long oldFileProgress = this.fileProgress;
//            this.fileProgress = fileProgress;
//
//            if (getPropertyChangeSupport().hasListeners("fileProgress")) {
//                firePropertyChange("fileProgress", oldFileProgress, fileProgress);
//            }
//        }
//
//        public int getFileProgress() {
//            return fileProgress;
//        }

    } // class BackgroundCopyWorker extends SwingWorker<Boolean, AbstractMap.SimpleImmutableEntry<Path, Integer>>

} // class SwingWorkerFileManagement extends BasicFileManager
