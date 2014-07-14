package FileSieve.Persistence.Reports;

import java.io.IOException;

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
     * @throws java.io.IOException
     */
    public void save(String savePath) throws IOException;
}