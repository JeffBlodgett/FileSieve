package FileSieve.Persistence.Reports;

/**
 * Interface defining methods for report output of diff results
 */
public interface DiffReport {
    /**
     * Returns the contents of the report
     *
     * @return                          string with report contents
     */
    public String getReport();

    /**
     * Saves the report to the specified path.
     *
     * @param savePath         path and filename for where to save the file
     */
    public void save(String savePath);
}