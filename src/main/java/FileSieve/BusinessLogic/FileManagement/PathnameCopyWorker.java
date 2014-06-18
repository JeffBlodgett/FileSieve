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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * SwingWorker capable of copying a file or folder from a source pathname to a target pathname.
 * while providing
 * progress updates in the form of an Integer representing the percentage of the copy operation that is complete
 */
public class PathnameCopyWorker extends SwingWorker<Boolean, PathnameCopyWorker.StringIntegerPair> {

    // TODO Further testing/debugging needed, conditions governing the overwriting of existing files need to be handled

    private Path sourcePathname;
    private Path targetPathname;
    private long totalBytes = 0L;
    private long copiedBytes = 0L;
    private int totalPercentCopied = 0;
    private boolean noErrors = true;
    private final Map<String, Integer> pathsCreated;
    boolean overwriteExistingFiles = false;
    boolean overwriteIfSizeDiffers = false;
    boolean recursionEnabled = true;
    private volatile int fileProgress;

    public PathnameCopyWorker(Path sourcePathname, Path targetPathname) throws IllegalArgumentException, SecurityException {
        if ((sourcePathname == null) || (targetPathname == null)) {
            throw new IllegalArgumentException("null pathname provided for source or target");
        }

        this.sourcePathname = sourcePathname;
        this.targetPathname = targetPathname;

        if (Files.isDirectory(sourcePathname)) {
            pathsCreated = Collections.synchronizedMap(new LinkedHashMap<String, Integer>(100));
        } else {
            pathsCreated = Collections.synchronizedMap(new LinkedHashMap<String, Integer>(1));
        }
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
     * This method is executed on a background thread when the SwingWorker's execute() method is called. Exceptions
     * thrown by this method are "swallowed" and thrown as type "ExecutionException", with e.getCause() containing
     * the details of the exception that was encountered, when the worker's get() method is called.
     *
     * @return                          boolean true if all operations in the
     * @throws IllegalStateException    thrown if the sourcePathname does not exist
     * @throws SecurityException        thrown if read access on the sourcePathname or write access on the targetPathname has not been granted
     * @throws IOException              thrown if an IOException occurs
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

    @Override
    public void process(List<StringIntegerPair> chunks) {
        synchronized (pathsCreated) {
            for (StringIntegerPair pair : chunks) {
                pathsCreated.put(pair.getPath(), pair.getValue());
            }
        }
    }

    public Map<String, Integer> getCreatedPathnames() {
        return pathsCreated;
    }

    @Override
    public void done() {
        setProgress(100);
    }

    private void retrieveTotalBytes(Path sourcePathname) throws SecurityException, IOException {
        if (Files.isDirectory(sourcePathname)) {
            if (recursionEnabled) {
                try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(sourcePathname)) {
                    for (Path path : dirStream) {
                        // Exclude target folder if it is a subfolder of the source folder
                        if (Files.isDirectory(path) && (!path.equals(this.targetPathname))) {
                            retrieveTotalBytes(path);
                        } else {
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
                publish(new StringIntegerPair(targetPathname.toString() + File.separator, 100));
            }

            if (Files.isDirectory(sourcePathname)) {
                List<Path> filePaths = null;
                //if (recursionEnabled) {
                filePaths = new ArrayList<Path>(50);
                try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(sourcePathname);) {
                    for (Path path : dirStream) {
                        filePaths.add(path);
                    }
                }
                //}

                //if (filePaths.size() > 0) { // (filePaths != null) &&
                for (Path path : filePaths) {
                    Path pathnameToCopy = path.subpath(sourcePathname.getNameCount(), path.getNameCount());

                    if (Files.isDirectory(path)) {
                        targetPathname = targetPathname.resolve(pathnameToCopy);

                        // Create folder - ensures folder is created even if empty
                        Path sourceFolderName = sourcePathname.getName(sourcePathname.getNameCount() - 1);
                        Files.createDirectory(targetPathname.resolve(sourceFolderName));

                        if (recursionEnabled) {
                            try {
                                copyFiles(path, targetPathname);
                            } catch (SecurityException | IOException e) {
                                noErrors = false;
                                // Ignore exceptions... attempts to copy other folders/files will continue
                            }
                        }

                    } else {
                        try {
                            copyFiles(path, targetPathname);
                        } catch (SecurityException | IOException e) {
                            noErrors = false;
                            // Ignore exceptions... attempts to copy other folders/files will continue
                        }
                    }
                }
                //}
//                else if (!sourcePathname.equals(this.sourcePathname)) {
//                    // Create empty directory
//                    Path sourceFolderName = sourcePathname.getName(sourcePathname.getNameCount() - 1);
//                    Files.createDirectory(targetPathname.resolve(sourceFolderName));
//                }

            } else {
                targetPathname = targetPathname.resolve(sourcePathname.getFileName());

                long fileBytes = sourcePathname.toFile().length();
                long soFar = 0L;
                publish(new StringIntegerPair(targetPathname.toString(), 0));
                setFileProgress(0);

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
                        if (getProgress() != totalPercentCopied) setProgress(totalPercentCopied);

                        filePercentCopied = (int) (++soFar * 100 / fileBytes);
                        if (getFileProgress() != filePercentCopied) setFileProgress(filePercentCopied);
                    }

                    if (getProgress() != 100) setProgress(100);
                    publish(new StringIntegerPair(targetPathname.toString(), 100));
                    if (getFileProgress() != 100) setFileProgress(100);

                } catch (SecurityException | IOException e) {
                    noErrors = false;

                    setProgress((int) ((totalPercentPreviouslyCopied + fileBytes) * 100 / totalBytes));
                    publish(new StringIntegerPair(targetPathname.toString(), null));

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

    void setFileProgress(int fileProgress) {
        if (fileProgress == this.fileProgress) return;

        long oldFileProgress = this.fileProgress;
        this.fileProgress = fileProgress;

        if (getPropertyChangeSupport().hasListeners("fileProgress")) {
            firePropertyChange("fileProgress", oldFileProgress, fileProgress);
        }
    }

    public int getFileProgress() {
        return fileProgress;
    }

    protected static class StringIntegerPair {

        private String path;
        private Integer value;

        public StringIntegerPair(String path, Integer value) {
            this.path = path;
            this.value = value;
        }

        public String getPath() {
            return path;
        }

        public int getValue() {
            return value;
        }

    } // class StringIntegerPair

} // class PathnameCopyWorker extends SwingWorker<Boolean, PathIntegerPair>
