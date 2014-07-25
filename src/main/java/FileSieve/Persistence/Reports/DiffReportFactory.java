package FileSieve.Persistence.Reports;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;


/**
 *  Factory for diff reports
 */
public class DiffReportFactory {
    /**
     * private constructor to prevent instantiation of factory
     */
    private DiffReportFactory() {

    }

    /**
     * Static factory method to return DiffReports (for now just the HTMLDiffReport)
     *
     * @param diffResults               the results of a FileDifferentiator object
     * @return                          an instance of a new DiffReport object
     * @throws java.io.IOException
     */
    public static DiffReport getDiffReport(List<SimpleImmutableEntry<String, List<File>>> diffResults)
            throws IOException {
        return new HTMLDiffReport(diffResults);
    }

    /**
     * Static factory method to return DiffReports (for now just the HTMLDiffReport)
     *
     * @param diffResults               the results of a FileDifferentiator object
     * @return                          an instance of a new DiffReport object
     * @throws java.io.IOException
     */
    public static DiffReport getDiffReport(List<SimpleImmutableEntry<String, List<File>>> diffResults,
                                                   List<String> deletedPaths)
            throws IOException {
        return new HTMLDiffReport(diffResults, deletedPaths);
    }

}
