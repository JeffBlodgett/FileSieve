package FileSieve.BusinessLogic.FileManagement;

import FileSieve.BusinessLogic.FileEnumeration.DiscoveredPath;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles the actual work (folder and file copies) for a SwingCopyJob.
 * This class has package-private access.
 */
class CopyJobWorkDelegate implements Runnable {

    private static final int ONE_HUNDRED_PERCENT = 100;
    private static final int ZERO_PERCENT = 0;
    private final boolean recursiveCopy;
    private final CopyWorkResultsReceiver copyWorkDispatcher;
    private final Set<Path> pathsBeingCopied;
    private final AtomicBoolean jobCancelled;
    private final Path destinationFolder;
    private final Comparator<Path> fileComparator;
    private final boolean overwriteExistingFiles;
    private final List<Path> foldersCreatedInTarget = new ArrayList<>();
    private long totalBytes = 0L;
    private long copiedBytes = 0L;
    private int totalPercentCopied = 0;
    private int copyPathsRecursionLevel = 0;

    protected CopyJobWorkDelegate(CopyWorkResultsReceiver copyWorkDispatcher, Set<Path> pathsBeingCopied, Path destinationFolder, boolean recursiveCopy, boolean overwriteExistingFiles, Comparator<Path> fileComparator) {
        if (copyWorkDispatcher == null) {
            throw new IllegalArgumentException("\"copyWorkDispatcher\" parameter cannot be null");
        }
        if (pathsBeingCopied == null) {
            throw new IllegalArgumentException("\"pathsBeingCopied\" parameter cannot be null");
        }
        if (destinationFolder == null) {
            throw new IllegalArgumentException("\"destinationFolder\" parameter cannot be null");
        }
        if (fileComparator == null) {
            throw new IllegalArgumentException("\"fileComparator\" parameter cannot be null");
        }

        this.copyWorkDispatcher = copyWorkDispatcher;
        this.pathsBeingCopied = pathsBeingCopied;
        this.destinationFolder = destinationFolder;
        this.recursiveCopy = recursiveCopy;
        this.fileComparator = fileComparator;
        this.overwriteExistingFiles = overwriteExistingFiles;

        jobCancelled = new AtomicBoolean(false);
    }

    protected void cancelWork() {
        jobCancelled.set(true);
    }

    @Override
    public void run() {
        try {
            for (Path path : pathsBeingCopied) {
                if (extractPath(path).toFile().exists()) {
                    retrieveTotalBytes(path);
                } else {
                    copyWorkDispatcher.workerException(new IllegalStateException("source pathname does not exist"));
                }
            }
        } catch (SecurityException e) {
            copyWorkDispatcher.workerException(new SecurityException("SecurityException while calculating bytes to copy", e));
        } catch (IOException e) {
            copyWorkDispatcher.workerException(new IOException("IOException while calculating bytes to copy", e));
        }  catch (Exception e) {
            copyWorkDispatcher.workerException(new Exception(e.getClass().getSimpleName() + " while calculating bytes to copy", e));
        }

        try {
            for (Path path : pathsBeingCopied) {
                if (!jobCancelled.get()) {
                    copyPaths(path, destinationFolder);
                }
            }
        } catch (SecurityException e) {
            copyWorkDispatcher.workerException(new SecurityException("SecurityException while reading or writing files/folders in the source or target", e));
        } catch (IOException e) {
            copyWorkDispatcher.workerException(new IOException("IOException while reading or writing files/folders in the source or target", e));
        } catch (Exception e) {
            copyWorkDispatcher.workerException(new Exception(e.getClass().getSimpleName() + " while reading or writing files/folders in the source or target", e));
        }

        copyWorkDispatcher.workCompleted();
    }

    /**
     * Utility method called by doInBackground() method to handle file and folder copy operations
     *
     * @param sourcePath            folder or file to be copied
     * @param targetPath            destination folder to which copy is to be created
     * @throws SecurityException    thrown if the security manager is unable to access a file or folder
     * @throws IOException          thrown if an IOException occurs during a read or write operation
     */
    private void copyPaths(Path sourcePath, Path targetPath) throws SecurityException, IOException {
        if (!jobCancelled.get()) {
            if (!sourcePath.equals(destinationFolder)) {

                if (!extractPath(targetPath).toFile().exists()) {
                    if (!targetPath.toFile().mkdirs()) {
                        throw new IOException("Unable to create destination folder using File.mkdirs() method");
                    }

                    if (targetPath.equals(destinationFolder)) {
                        publishWork(new SimpleImmutableEntry<>(targetPath, ZERO_PERCENT));
                    }
                }

                if (Files.isDirectory(extractPath(sourcePath))) {
                    copyFolder(sourcePath, targetPath);

                } else {
                    // Create the file's source folder inside the destination folder if provided via a DiscoveredPath instance
                    if (sourcePath instanceof DiscoveredPath) {
                        Path sourceFolder = ((DiscoveredPath)sourcePath).getSourceFolder();

                        if (!sourceFolder.getFileName().toString().isEmpty()) {
                            sourceFolder = sourceFolder.getFileName();
                            Path pathToCreateInTargetFolder = targetPath.resolve(sourceFolder);

                            if (!foldersCreatedInTarget.contains(sourceFolder)) {
                                if (!pathToCreateInTargetFolder.toFile().exists()) {
                                    if (!pathToCreateInTargetFolder.toFile().mkdirs()) {
                                        throw new IOException("Unable to create \"" + pathToCreateInTargetFolder + "\" folder using File.mkdir() method");
                                    }
                                }
                                foldersCreatedInTarget.add(sourceFolder);
                            }
                        }
                    }

                        /* Determine if the file's parent folder was previously created within the target folder,
                           searching the "foldersCreateInTarget" list in reverse order */
                    Path parentPathCreatedInTargetFolder = null;
                    for (int i = foldersCreatedInTarget.size() - 1; i >= 0; --i) {
                        if (sourcePath.getParent().endsWith(foldersCreatedInTarget.get(i))) {
                            parentPathCreatedInTargetFolder = foldersCreatedInTarget.get(i);
                            break;
                        }
                    }

                        /* If file's parent was previously created within the target folder then create the file copy
                           within that parent, else create the file in the root of the target */
                    if (parentPathCreatedInTargetFolder != null) {
                        copyFile(sourcePath, targetPath.resolve(parentPathCreatedInTargetFolder));
                    } else {
                        copyFile(sourcePath, targetPath);
                    }

                }
            }
        }
    }

    private void copyFolder(Path sourcePath, Path targetPath) throws SecurityException, IOException {
        if (recursiveCopy) {
            List<Path> filePaths = new ArrayList<>(50);
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(extractPath(sourcePath))) {
                for (Path path : dirStream) {
                    filePaths.add(path);
                }
            }

            for (Path path : filePaths) {
                if (!jobCancelled.get()) {
                    if (Files.isDirectory(extractPath(path))) {
                        Path newTargetPath;
                        if (copyPathsRecursionLevel == 0) {
                            newTargetPath = targetPath.resolve(sourcePath.getFileName().resolve(path.getFileName()));
                        } else {
                            newTargetPath = targetPath.resolve(path.getFileName());
                        }

                        if (!extractPath(newTargetPath).toFile().exists()) {
                            if (!newTargetPath.toFile().mkdirs()) {
                                throw new IOException("Unable to create \"" + newTargetPath + "\" folder using File.mkdirs()");
                            }
                        }

                        publishWork(new SimpleImmutableEntry<>(newTargetPath, ONE_HUNDRED_PERCENT));

                        ++copyPathsRecursionLevel;
                        try {
                            copyPaths(path, newTargetPath);
                        } finally {
                            --copyPathsRecursionLevel;
                        }

                    } else if (Files.isRegularFile(extractPath(path), LinkOption.NOFOLLOW_LINKS)) {
                        if (copyPathsRecursionLevel == 0) {
                            Path newTargetPath = targetPath.resolve(sourcePath.getFileName());

                            if (!extractPath(newTargetPath).toFile().exists()) {
                                if (!newTargetPath.toFile().mkdirs()) {
                                    throw new IOException("Unable to create \"" + newTargetPath + "\" folder using File.mkdir() method");
                                }
                            }

                            copyFile(path, newTargetPath);
                        } else {
                            copyFile(path, targetPath);
                        }
                    }
                }
            }

        } else {
            // Create the folder's source folder inside the destination folder if provided via a DiscoveredPath instance
            if (sourcePath instanceof DiscoveredPath) {
                Path sourceFolder = ((DiscoveredPath)sourcePath).getSourceFolder();

                if (!sourceFolder.getFileName().toString().isEmpty()) {
                    sourceFolder = sourceFolder.getFileName();
                    Path pathToCreateInTargetFolder = targetPath.resolve(sourceFolder);

                    if (!foldersCreatedInTarget.contains(sourceFolder)) {
                        if (!pathToCreateInTargetFolder.toFile().exists()) {
                            if (!pathToCreateInTargetFolder.toFile().mkdirs()) {
                                throw new IOException("Unable to create \"" + pathToCreateInTargetFolder + "\" folder using File.mkdir() method (boolean false returned)");
                            }
                        }
                        foldersCreatedInTarget.add(sourceFolder);
                    }
                }
            }

                /* Determine if the folder's parent was previously created within the target folder, searching the
                   "foldersCreateInTarget" list in reverse order */
            Path pathToCreateInTargetFolder = null;
            for (int i = foldersCreatedInTarget.size() - 1; i >= 0; --i) {
                if (sourcePath.getParent().endsWith(foldersCreatedInTarget.get(i))) {
                    pathToCreateInTargetFolder = foldersCreatedInTarget.get(i).resolve(sourcePath.getFileName());
                    break;
                }
            }

                /* If folder's parent was previously created within the target folder then create the folder within
                   that parent, else create the folder in the root of the target */
            if (pathToCreateInTargetFolder != null) {
                Path newPathToCreate = targetPath.resolve(pathToCreateInTargetFolder);
                if (!foldersCreatedInTarget.contains(pathToCreateInTargetFolder)) {
                    if (!extractPath(newPathToCreate).toFile().exists()) {
                        if (!newPathToCreate.toFile().mkdirs()) {
                            throw new IOException("Unable to create \"" + newPathToCreate + "\" folder using File.mkdir() method (boolean false returned)");
                        }
                    }
                    foldersCreatedInTarget.add(pathToCreateInTargetFolder);
                }
            } else {
                if (!targetPath.resolve(sourcePath.getFileName()).toFile().exists()) {
                    if (!targetPath.resolve(sourcePath.getFileName()).toFile().mkdirs()) {
                        throw new IOException("Unable to create \"" + targetPath.resolve(sourcePath.getFileName()) + "\" folder using File.mkdir() method (boolean false returned)");
                    }
                }
                foldersCreatedInTarget.add(sourcePath.getFileName());
            }
        }
    }

    /**
     * Private helper method, called by "copyPaths" method, for copying a single file
     *
     * @param fileToCopy            file to copy, passed as a Path instance
     * @param target                folder within which copy is to be placed
     * @throws SecurityException    thrown if the security manager denies read-access to the original file or
     *                              write-access to the destination folder
     * @throws IOException          thrown if an IOException occurs during read/write operations
     */
    private void copyFile(Path fileToCopy, Path target) throws SecurityException, IOException {
        target = target.resolve(fileToCopy.getFileName());

        int previousPercentCopied;
        long fileBytes = fileToCopy.toFile().length();  // size of file in bytes

        boolean filesAreSimilar = false;
        if (fileComparator.compare(fileToCopy, target) == 0) {
            filesAreSimilar = true;
        }

        if ((!filesAreSimilar) || overwriteExistingFiles)  {
            long soFar = 0L;    // file bytes copied thus far
            int sourceByte;
            int filePercentCopied = ZERO_PERCENT;
            int pathnameProgress = ZERO_PERCENT;

            try (
                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileToCopy.toFile()));
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(target.toFile()))
            ) {
                /* Copy file one byte at time. BufferedInputStream and BufferedOutputStream have buffers so
                   so this isn't as slow as it might at first seem */
                while (((sourceByte = bis.read()) != -1) && (!jobCancelled.get())) {
                    bos.write(sourceByte);

                    // Update copy job's overall progress
                    previousPercentCopied = totalPercentCopied;
                    totalPercentCopied = (int) (++copiedBytes * ONE_HUNDRED_PERCENT / totalBytes);
                    if ((totalPercentCopied != previousPercentCopied) && (totalPercentCopied < ONE_HUNDRED_PERCENT)) {
                        publishWork(new SimpleImmutableEntry<>(destinationFolder, totalPercentCopied));
                    }

                    /* Update the progress of the individual file copy if progress has incremented by at least 1 percent
                       and is not yet 100 percent complete */
                    filePercentCopied = (int) (++soFar * ONE_HUNDRED_PERCENT / fileBytes);
                    if ((pathnameProgress != filePercentCopied) && (filePercentCopied < ONE_HUNDRED_PERCENT)) {
                        pathnameProgress = filePercentCopied;
                        publishWork(new SimpleImmutableEntry<>(target, pathnameProgress));
                    }
                }

                // Delete file fragment
                if (jobCancelled.get() && (filePercentCopied < ONE_HUNDRED_PERCENT)) {
                    bos.close();

                    if (!target.toFile().delete()) {
                        throw new IOException("Unable to delete incomplete file fragment \"" + target.toString() + "\" following job cancellation");
                    }

                    /* Backtrack... set and publish the total job progress as the value had prior to this attempted
                       file-copy and set+publish the progress for the failed copy to/as 0 percent */
                    if (soFar >0) {
                        copiedBytes -= soFar;
                        publishWork(new SimpleImmutableEntry<>(target, ZERO_PERCENT));
                    }
                    previousPercentCopied = totalPercentCopied;
                    totalPercentCopied = (int) (copiedBytes * ONE_HUNDRED_PERCENT / totalBytes);
                    if ((totalPercentCopied != previousPercentCopied) && (totalPercentCopied < ONE_HUNDRED_PERCENT)) {
                        publishWork(new SimpleImmutableEntry<>(destinationFolder, totalPercentCopied));
                    }
                } else {
                    // No need to set "pathnameProgress" variable to 100... publish completion of the file copy and move on
                    publishWork(new SimpleImmutableEntry<>(target, ONE_HUNDRED_PERCENT));
                }

            } catch (IOException e) {
                // Backtrack... set+publish the total job progress to the value had prior to this attempted file
                // copy and set+publish the progress for the failed copy to/as 0 percent
                if (soFar > 0) {
                    copiedBytes -= soFar;
                    publishWork(new SimpleImmutableEntry<>(target, ZERO_PERCENT));
                }
                previousPercentCopied = totalPercentCopied;
                totalPercentCopied = (int) (copiedBytes * ONE_HUNDRED_PERCENT / totalBytes);
                if ((totalPercentCopied != previousPercentCopied) && (totalPercentCopied < ONE_HUNDRED_PERCENT)) {
                    publishWork(new SimpleImmutableEntry<>(destinationFolder, totalPercentCopied));
                }

                try {
                    if ((target.toFile().exists()) && ((target.toFile().length() == 0L) || (soFar > 0L))) {
                        if (!target.toFile().delete()) {
                            throw new IOException("An IOException occurred while copying file \"" + fileToCopy.toString() + "\". An incomplete copy was left in the destination folder.", e);
                        }
                        throw new IOException("An IOException occurred while copying file \"" + fileToCopy.toString() + "\". An incomplete copy was not left in the destination folder.", e);
                    } else {
                        throw new IOException("An IOException occurred while copying file \"" + fileToCopy.toString() + "\".", e);
                    }
                } catch (IOException ex) {
                    throw new IOException("An IOException occurred while copying file \"" + fileToCopy.toString() + "\". An incomplete copy may have been left in the destination folder.", ex);
                }
            }
        } else {
            copiedBytes += fileBytes;
            previousPercentCopied = totalPercentCopied;
            totalPercentCopied = (int) (copiedBytes * ONE_HUNDRED_PERCENT / totalBytes);
            if ((totalPercentCopied != previousPercentCopied) && (totalPercentCopied < ONE_HUNDRED_PERCENT)) {
                publishWork(new SimpleImmutableEntry<>(destinationFolder, totalPercentCopied));
            }
        }
    }

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
     * Send work result to the registered CopyWorkDispatcher
     *
     * @param result    result to provide
     */
    private void publishWork(SimpleImmutableEntry<Path, Integer> result) {
        copyWorkDispatcher.receiveWorkResults(result);
    }

    private void retrieveTotalBytes(Path sourcePathname) throws SecurityException, IOException {
        if (Files.isDirectory(extractPath(sourcePathname))) {
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(extractPath(sourcePathname))) {
                for (Path path : dirStream) {
                    if (!jobCancelled.get()) {
                        // Exclude target folder if it is a subfolder of the source folder
                        if (Files.isDirectory(extractPath(path), LinkOption.NOFOLLOW_LINKS) && (!path.equals(destinationFolder) && recursiveCopy)) {
                            retrieveTotalBytes(path);
                        } else if (Files.isRegularFile(extractPath(path), LinkOption.NOFOLLOW_LINKS)) {
                            totalBytes += path.toFile().length();
                        }
                    }
                }
            }
        } else {
            totalBytes += sourcePathname.toFile().length();
        }
    }

}
