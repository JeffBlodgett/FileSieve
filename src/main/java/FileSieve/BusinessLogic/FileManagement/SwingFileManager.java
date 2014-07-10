package FileSieve.BusinessLogic.FileManagement;

import java.nio.file.Path;

/**
 * Defines an implementation-specific FileManager type to be returned by method(s) of the FileManagerFactory
 * static factory class.
 */
public interface SwingFileManager extends FileOpener, FileDeleter, FileCopier<SwingCopyJob, SwingCopyJobListener, Path> {

    // Hybrid interface - does nothing more than bring three other interfaces together into a unified type

} // interface SwingFileManager extends FileOpener, FileDeleter, FileCopier<SwingCopyJob, SwingCopyJobListener, Path>
