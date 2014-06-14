package FileSieve.BusinessLogic.FileManagement;

public class FileManagerFactory {

    public static FileManager<PathnameCopyWorker> getSwingWorkerFileManager() {
        return new SwingWorkerFileManager();
    }

}
