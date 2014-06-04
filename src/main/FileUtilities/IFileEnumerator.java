package FileUtilities;

import java.nio.file.Path;
import java.util.List;

/**
 * Interface defining utility methods for enumerating files, and identifying duplicates files,
 * within a given folder pathname.
 */
public interface IFileEnumerator {

    /**
     * Method for discovering and returning Path objects for files within a given folder (pathname).
     * Returns a Path object for each discovered file or empty folder until no further files/folders are discoverable,
     * at which point the method returns a Null reference.
     *
     * @param strFileOrFolderPathname   pathname of file or pathname of folder to be searched for files
     * @param searchSubFolders          boolean parameter specifying if search should extend to subfolders (recursive)
     * @return                          path object for each subsequent method call with the same pathname string, Null
     *                                  reference if there are no further files or empty folders to return
     */
    public Path getFile(String strFileOrFolderPathname, boolean searchSubFolders);

    /**
     * Convenience method for calling getFile method with recursion enabled.
     *
     * @param strFileOrFolderPathname   pathname of file or pathname of folder to be searched for files
     * @return                          path object for each subsequent method call with the same pathname string, Null
     *                                  reference if there are no further files or empty folders to return
     */
    public Path getFile(String strFileOrFolderPathname);

    /**
     * Acquires a list of all discovered files within a given folder (pathname).
     *
     * @param strFileOrFolderPathname   pathname of folder to be searched
     * @param searchSubFolder           boolean parameter specifying if search should extend to subfolders
     * @return                          list of the discovered files
     */
    public List<Path> getFiles(String strFileOrFolderPathname, boolean searchSubFolder);

    /**
     * Convenience method for calling getFiles method with recursion enabled.
     *
     * @param strFileOrFolderPathname
     * @return
     */
    public List<Path> getFiles(String strFileOrFolderPathname);

    /**
     * Acquires a list of duplicated files within a given list of files.
     *
     * @param fileList                  List of files to be searched for duplicates
     * @return                          List of file lists - each list contains files identified as duplicates of each other
     */
    public List<List<Path>> getDuplicates(List<Path> fileList);
    
} // interface IFileEnumerator
