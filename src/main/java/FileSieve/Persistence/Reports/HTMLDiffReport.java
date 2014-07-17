package FileSieve.Persistence.Reports;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * A DiffReport class for outputting HTML reports
 */
public class HTMLDiffReport implements DiffReport {
    private static String TEMPLATE_DIR = "templates";
    private static String TEMPLATE_NAME = "DiffReport.html";
    private List<SimpleImmutableEntry<String, List<File>>> fileDiffResults;
    private Document reportDoc;
    private String baseDir;
    private String systemFileSeparator;

    public HTMLDiffReport(List<SimpleImmutableEntry<String, List<File>>> diffResults) throws IOException {
        fileDiffResults = diffResults;

        baseDir = System.getProperty("user.dir");
        systemFileSeparator = System.getProperty("file.separator");

        buildReport();
    }

    /**
     * Returns the contents of the report
     *
     * @return string with report contents
     */
    public String getReport() {
        return reportDoc.toString();
    }

    /**
     * Saves the report to the specified path.
     *
     * @param savePath path and filename for where to save the file
     * @throws java.io.IOException
     */
    public void save(String savePath) throws IOException {
        try(FileWriter fw = new FileWriter(savePath)) {
            fw.write(reportDoc.toString());
        }
    }

    /**
     * Builds the report based on a template file.
     *
     * @throws java.io.IOException
     */
    private void buildReport() throws IOException {
        File input = new File(baseDir + systemFileSeparator + TEMPLATE_DIR + systemFileSeparator + TEMPLATE_NAME);

        reportDoc = Jsoup.parse(input, null);
        Element diffReportContainer = reportDoc.select(".diffReport").first();
        Element diffResultTable = diffReportContainer.select(".diffResult").first().clone();

        diffReportContainer.empty();

        for (SimpleImmutableEntry<String, List<File>> diffResult : fileDiffResults)
            diffReportContainer.appendChild(getDiffTable(diffResultTable, diffResult));
    }

    /**
     * Creates a "table"* with a heading of the file name and a list of all matching files.
     * The element is appended to the document passed to it, but it is also returned, where it can be modified further.
     * *table is a reference to the visual style that will eventually be applied, but it's an HTML div tag.
     *
     * @param diffResultTable the jsoup Document element being used to form this report.
     * @param diffResult the diff result to represent in table form
     *
     * @return a jsoup Element representing a div containing this file's listing of matches
     */
    private Element getDiffTable(Element diffResultTable, SimpleImmutableEntry<String, List<File>> diffResult) {
        Element diffTable = diffResultTable.clone();
        Element diffFileNameContainer = diffTable.select(".fileName").first();
        Element diffMatchContainer = diffResultTable.select(".match").first().clone();

        for (Element element : diffTable.select(".match"))
            element.remove();

        diffFileNameContainer.text(diffResult.getKey());

        for (File f : diffResult.getValue()) {
            Element container = diffMatchContainer.clone();
            container.text(f.getPath());
            diffTable.appendChild(container);
        }
        return diffTable;
    }
}