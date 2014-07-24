package FileSieve.BusinessLogic.FileEnumeration;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;

/**
 * Decorates a Path object, adding the source folder within which the Path was discovered by a FileEnumerator instance.
 * The creation of this class was necessary to avoid changes to the FileEnumeration and FileManagement packages' APIs
 * (late stage project development change) while enabling the specification of the paths of the original source folders
 * in the Map returned by a FileEnumerator. SwingCopyJob instances, spawned by a SwingFileManager, are able to interpret
 * the additional data in a DiscoveredPath instance and create the original source folders in the destination folder of
 * a copy operation.
 *
 * Many methods of the java.io.File and java.nio.file.Files classes will throw exceptions if passed this type since
 * a DiscoveredPath is not a super type of Java's hidden, concrete "WindowsPath" or "UnixPath" Path implementations. A
 * DiscoveredPath's getPath() method may be used to extract the decorated Path as produced by the default file system
 * provider.
 */
public class DiscoveredPath implements Path {

    private final Path path;
    private final Path sourceFolder;

    /**
     * Constructs a DiscoveredPath instance with the provided path. The source folder is set to null.
     *
     * @param path              the Path instance that is decorated by this DiscoveredPath
     */
    public DiscoveredPath(Path path) {

        if (path == null) {
            throw new IllegalArgumentException("Path reference cannot be null");
        }

        this.path = path;
        this.sourceFolder = Paths.get("");
    }

    /**
     * Constructs a DiscoveredPath instance with the provided path and specific source folder.
     *
     * @param path              the Path instance that is decorated by this DiscoveredPath
     * @param sourceFolder      the source folder within which the path was discovered
     */
    public DiscoveredPath(Path path, Path sourceFolder) {
        if (path == null) {
            throw new IllegalArgumentException("Path reference cannot be null");
        }

        this.path = path;

        if (sourceFolder != null) {
            this.sourceFolder = sourceFolder;
        } else {
            this.sourceFolder = Paths.get("");
        }
    }

    public Path getSourceFolder() {
        return this.sourceFolder;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public int compareTo(Path other) {
        return path.compareTo(other);
    }

    @Override
    public boolean endsWith(Path other) {
        return path.endsWith(other);
    }

    @Override
    public boolean endsWith(String other) {
        return path.endsWith(other);
    }

    @Override
    public boolean equals(Object other) {
        return path.equals(other);
    }

    @Override
    public Path getFileName() {
        return path.getFileName();
    }

    @Override
    public FileSystem getFileSystem() {
        return path.getFileSystem();
    }

    @Override
    public Path getName(int index) {
        return path.getName(index);
    }

    @Override
    public int getNameCount() {
        return path.getNameCount();
    }

    @Override
    public Path getParent() {
        return path.getParent();
    }

    @Override
    public Path getRoot() {
        return path.getRoot();
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public boolean isAbsolute() {
        return path.isAbsolute();
    }

    @Override
    public Iterator<Path> iterator() {
        return path.iterator();
    }

    @Override
    public Path normalize() {
        return path.normalize();
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        return path.register(watcher, events);
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        return path.register(watcher, events, modifiers);
    }

    @Override
    public Path relativize(Path other) {
        return path.relativize(other);
    }

    @Override
    public Path resolve(Path other) {
        return path.resolve(other);
    }

    @Override
    public Path resolve(String other) {
        return path.resolve(other);
    }

    @Override
    public Path resolveSibling(Path other) {
        return path.resolveSibling(other);
    }

    @Override
    public Path resolveSibling(String other) {
        return path.resolveSibling(other);
    }

    @Override
    public boolean startsWith(Path other) {
        return path.startsWith(other);
    }

    @Override
    public boolean startsWith(String other) {
        return path.startsWith(other);
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        return path.subpath(beginIndex, endIndex);
    }

    @Override
    public Path toAbsolutePath() {
        return path.toAbsolutePath();
    }

    @Override
    public File toFile() {
        return path.toFile();
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        return path.toRealPath();
    }

    @Override
    public String toString() {
        return path.toString();
    }

    @Override
    public URI toUri() {
        return path.toUri();
    }

} // class DiscoveredPath extends Path
