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
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles the actual work (folder and file copies) for a SwingCopyJob.
 * This class has package-private access.
 */
class CopyJobWorkDelegate implements Runnable {

    private static final long ONE_HUNDRED_PERCENT = 100L;
    private static final long ZERO_PERCENT = 0L;
    private final boolean recursiveCopy;
    private final CopyWorkResultsReceiver copyWorkDispatcher;
    private final Set<Path> pathsBeingCopied;
    private final AtomicBoolean jobCancelled;
    private final Path destinationFolder;
    private final Comparator<Path> fileComparator;
    private final boolean overwriteExistingFiles;
    private final List<Path> foldersCreatedInTarget = new ArrayList<>();
    private final Object fileCopyLock = new Object();
    private final Object shutdownLock = new Object();
    private int copyPathsRecursionLevel = 0;
    private AtomicInteger fileCopyThreadCount = new AtomicInteger(0);
    private final ThreadPoolExecutor threadPoolExecutor;

    protected CopyJobWorkDelegate(CopyWorkResultsReceiver copyWorkResultsReceiver, Set<Path> pathsBeingCopied, Path destinationFolder, boolean recursiveCopy, boolean overwriteExistingFiles, int fileCopyThreadLimit, Comparator<Path> fileComparator) {
        if (copyWorkResultsReceiver == null) {
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

        final int maximumPoolSize = fileCopyThreadLimit;
        final int corePoolSize = maximumPoolSize;
        final int keepAliveTime = 5000;
        threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>());
        threadPoolExecutor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                synchronized(fileCopyLock) {
                    if (!jobCancelled.get()) {
                        try {
                            fileCopyLock.wait();    // Notified by a completing FileCopy thread
                            Thread.sleep(50L);      // Short delay to ensure completing FileCopy thread has had a chance to finalize
                            executor.execute(r);
                        } catch (InterruptedException e) {
                            copyWorkDispatcher.workerException(e);
                        }
                    }
                }
            }
        });
        threadPoolExecutor.prestartAllCoreThreads();

        this.copyWorkDispatcher = copyWorkResultsReceiver;
        this.pathsBeingCopied = pathsBeingCopied;
        this.destinationFolder = destinationFolder;
        this.recursiveCopy = recursiveCopy;
        this.fileComparator = fileComparator;
        this.overwriteExistingFiles = overwriteExistingFiles;

        jobCancelled = new AtomicBoolean(false);
    }

    protected void setFileCopyThreadLimit(int fileCopyThreadLimit) {
        if (!threadPoolExecutor.isShutdown()) {
            if (fileCopyThreadLimit > threadPoolExecutor.getMaximumPoolSize()) {
                threadPoolExecutor.setMaximumPoolSize(fileCopyThreadLimit);
                threadPoolExecutor.setCorePoolSize(fileCopyThreadLimit);
            } else if (fileCopyThreadLimit < threadPoolExecutor.getMaximumPoolSize()) {
                threadPoolExecutor.setCorePoolSize(fileCopyThreadLimit);
                threadPoolExecutor.setMaximumPoolSize(fileCopyThreadLimit);
            }
        }
    }

    protected void stopWorkers() {
        jobCancelled.set(true);
        threadPoolExecutor.shutdown();

        try {
            threadPoolExecutor.awaitTermination(Integer.MAX_VALUE, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            throw new RuntimeException("InterruptedException occurred while awaiting termination of FileCopy worker pool", e);
        }
    }

    @Override
    public void run() {
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

        synchronized (fileCopyLock) {
            while ((fileCopyThreadCount.get() > 0) && (!jobCancelled.get())) {
                try {
                    fileCopyLock.wait();
                } catch (InterruptedException e) {
                    copyWorkDispatcher.workerException(e);
                }
            }
        }

        try {
            stopWorkers();
        } catch (RuntimeException e) {
            copyWorkDispatcher.workerException(e);
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
                        fileCopyThreadCount.incrementAndGet();
                        threadPoolExecutor.execute(new CopyFile(sourcePath, targetPath.resolve(parentPathCreatedInTargetFolder)));
                    } else {
                        fileCopyThreadCount.incrementAndGet();
                        threadPoolExecutor.execute(new CopyFile(sourcePath, targetPath));
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

                            fileCopyThreadCount.incrementAndGet();
                            threadPoolExecutor.execute(new CopyFile(path, newTargetPath));
                        } else {
                            fileCopyThreadCount.incrementAndGet();
                            threadPoolExecutor.execute(new CopyFile(path, targetPath));
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
    private void publishWork(SimpleImmutableEntry<Path, Long> result) {
        copyWorkDispatcher.receiveWorkResults(result);
    }

    /**
     * Runnable that copies a specific file to a target folder.
     */
    private class CopyFile implements Runnable {

        private final Path fileToCopy;
        private Path target;

        /**
         * Constructor for the CopyFile Runnable.
         *
         * @param fileToCopy    file to copy, passed as a Path instance
         * @param target        folder within which copy is to be placed
         */
        protected CopyFile(Path fileToCopy, Path target) {
            if (fileToCopy == null) {
                throw new IllegalArgumentException("\"fileToCopy\" parameter cannot be null");
            }
            if (target == null) {
                throw new IllegalArgumentException("\"target\" parameter cannot be null");
            }

            this.fileToCopy = fileToCopy;
            this.target = target;
        }

        @Override
        public void run() {
            target = target.resolve(fileToCopy.getFileName());

            long fileBytes = fileToCopy.toFile().length();  // size of file in bytes

            boolean filesAreSimilar = false;
            if (fileComparator.compare(fileToCopy, target) == 0) {
                filesAreSimilar = true;
            }

            if ((!filesAreSimilar) || overwriteExistingFiles)  {
                long soFar = 0L;    // file bytes copied thus far
                int sourceByte;
                long filePercentCopied = ZERO_PERCENT;
                long pathnameProgress = ZERO_PERCENT;
                final long oneMegabyte = 1048576L;
                long reportedBytes = 0;
                long nextReportingThreshold = oneMegabyte;

                try (
                        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileToCopy.toFile()));
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(target.toFile()))
                ) {
                    /* Copy file one byte at time. BufferedInputStream and BufferedOutputStream have buffers so this
                       isn't as slow as it might at first seem */
                    while (((sourceByte = bis.read()) != -1) && (!jobCancelled.get())) {
                        if (soFar == 0L) {
                            publishWork(new SimpleImmutableEntry<>(target, pathnameProgress));
                        }

                        bos.write(sourceByte);

                        ++soFar;

                        // Update progress every 1 MB
                        if (soFar == nextReportingThreshold) {
                            publishWork(new SimpleImmutableEntry<>(destinationFolder, oneMegabyte));
                            reportedBytes += oneMegabyte;
                            nextReportingThreshold += oneMegabyte;

                            /* Update the progress of the individual file copy if progress has incremented by at least 1 percent
                               and is not yet 100 percent complete */
                            filePercentCopied = (int) (soFar * ONE_HUNDRED_PERCENT / fileBytes);
                            if ((pathnameProgress != filePercentCopied) && (filePercentCopied < ONE_HUNDRED_PERCENT)) {
                                pathnameProgress = filePercentCopied;
                                publishWork(new SimpleImmutableEntry<>(target, pathnameProgress));
                            }
                        }
                    }

                    if (jobCancelled.get() && (filePercentCopied < ONE_HUNDRED_PERCENT)) {
                        // Job was cancelled - delete file fragment and handle reporting
                        bos.close();

                        if (!target.toFile().delete()) {
                            throw new IOException("Unable to delete incomplete file fragment \"" + target.toString() + "\" following job cancellation");
                        }

                        // Backtrack... publish the percent copy progress for the file as 0 and roll back the total bytes copied
                        if (soFar > 0) {
                            publishWork(new SimpleImmutableEntry<>(target, ZERO_PERCENT));
                            if (reportedBytes > 0) {
                                publishWork(new SimpleImmutableEntry<>(destinationFolder, -reportedBytes));
                            }
                        }
                    } else {
                        // No need to set "pathnameProgress" variable to 100... publish completion of the file copy and move on
                        publishWork(new SimpleImmutableEntry<>(target, ONE_HUNDRED_PERCENT));
                        long unreportedBytesCopied;
                        if ((unreportedBytesCopied = soFar - reportedBytes) > 0) {
                            publishWork(new SimpleImmutableEntry<>(destinationFolder, unreportedBytesCopied));
                        }
                    }

                } catch (IOException e) {
                    // Backtrack... set+publish the total job progress to the value had prior to this attempted file
                    // copy and set+publish the progress for the failed copy to/as 0 percent
                    if (soFar > 0) {
                        publishWork(new SimpleImmutableEntry<>(target, ZERO_PERCENT));
                        if (reportedBytes > 0) {
                            publishWork(new SimpleImmutableEntry<>(destinationFolder, -reportedBytes));
                        }
                    }

                    if ((target.toFile().exists()) && ((target.toFile().length() == 0L) || (soFar > 0L))) {
                        if (!target.toFile().delete()) {
                            copyWorkDispatcher.workerException(new IOException("An IOException occurred while copying file \"" + fileToCopy.toString() + "\". An incomplete copy was left in the destination folder.", e));
                        }
                        copyWorkDispatcher.workerException(new IOException("An IOException occurred while copying file \"" + fileToCopy.toString() + "\". An incomplete copy was not left in the destination folder.", e));
                    } else {
                        copyWorkDispatcher.workerException(new IOException("An IOException occurred while copying file \"" + fileToCopy.toString() + "\".", e));
                    }
                }
            } else {
                publishWork(new SimpleImmutableEntry<>(destinationFolder, fileBytes));
            }

            fileCopyThreadCount.decrementAndGet();
            synchronized (fileCopyLock) {
                fileCopyLock.notify();
            }
        }

    }

}
