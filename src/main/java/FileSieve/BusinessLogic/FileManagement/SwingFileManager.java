package FileSieve.BusinessLogic.FileManagement;

import java.nio.file.Path;

/**
 * Defines a FileManager type to be returned by the static FileManagerFactory class.
 */
public interface SwingFileManager extends FileOpener, FileDeleter, FileCopier<CopyJob, CopyJobListener, Path> {

    // Hybrid interface providing a type definition

} // interface SwingFileManager extends FileOpener, FileDeleter, FileCopier<CopyJob, CopyJobListener>
