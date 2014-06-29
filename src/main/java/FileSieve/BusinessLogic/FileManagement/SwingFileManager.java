package FileSieve.BusinessLogic.FileManagement;

import java.nio.file.Path;

/**
 * Defines an implementation-specific FileManager type to be returned by at least one method of the static
 * FileManagerFactory class.
 */
public interface SwingFileManager extends FileOpener, FileDeleter, FileCopier<SwingCopyJob, SwingCopyJobListener, Path> {

    // Hybrid interface that does nothing more than bring three other interfaces together into a hybrid interface

} // interface SwingFileManager extends FileOpener, FileDeleter, FileCopier<SwingCopyJob, SwingCopyJobListener>
