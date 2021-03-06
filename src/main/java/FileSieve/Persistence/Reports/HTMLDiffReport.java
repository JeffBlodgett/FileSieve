package FileSieve.Persistence.Reports;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * A DiffReport class for outputting HTML reports
 */
public class HTMLDiffReport implements DiffReport {

    private static String TEMPLATE_DIR = "resources";
    private static String TEMPLATE_NAME = "DiffReportTemplate.html";
    private List<SimpleImmutableEntry<String, List<File>>> fileDiffResults;
    private List<String> deletedFiles;
    private Document reportDoc;

    public HTMLDiffReport(List<SimpleImmutableEntry<String, List<File>>> diffResults) throws IOException {
        fileDiffResults = diffResults;
        deletedFiles = new ArrayList<>();

        buildReport();
    }

    public HTMLDiffReport(List<SimpleImmutableEntry<String, List<File>>> diffResults, List<String> deletedPaths) throws IOException {
        fileDiffResults = diffResults;
        deletedFiles = deletedPaths;

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
        try (FileWriter fw = new FileWriter(savePath)) {
            fw.write(reportDoc.toString());
        }
    }

    /**
     * Builds the report based on a template file.
     *
     * @throws java.io.IOException
     */
    private void buildReport() throws IOException {
        try (InputStream template = Thread.currentThread().getContextClassLoader().getResourceAsStream(TEMPLATE_DIR + "/" + TEMPLATE_NAME)) {
            reportDoc = Jsoup.parse(template, null, "");
            Element diffReportContainer = reportDoc.select(".diffReport").first();
            Element diffResultTable = diffReportContainer.select(".diffResult").first().clone();

            diffReportContainer.empty();

            for (SimpleImmutableEntry<String, List<File>> diffResult : fileDiffResults) {
                diffReportContainer.appendChild(getDiffTable(diffResultTable, diffResult));
            }
        }
    }

    /**
     * Creates a "table"* with a heading of the file name and a list of all matching files.
     * The element is appended to the document passed to it, but it is also returned, where it can be modified further.
     * *table is a reference to the visual style that will eventually be applied, but it's an HTML div tag.
     *
     * @param diffResultTable   the jsoup Document element being used to form this report.
     * @param diffResult        the diff result to represent in table form
     *
     * @return                  a jsoup Element representing a div containing this file's listing of matches
     */
    private Element getDiffTable(Element diffResultTable, SimpleImmutableEntry<String, List<File>> diffResult) {
        Element diffTable = diffResultTable.clone();
        Element diffFileNameContainer = diffTable.select(".fileName").first();
        Element diffMatchesContainer = diffTable.select(".matches").first();
        Element diffMatchContainer = diffResultTable.select(".match").first().clone();

        for (Element element : diffTable.select(".match"))
            element.remove();

        diffFileNameContainer.text(diffResult.getKey());

        for (File f : diffResult.getValue()) {
            Element container = diffMatchContainer.clone();

            if (deletedFiles.contains(f.getPath())) {
                container.addClass("deleted");
            }

            container.text(f.getPath());
            diffMatchesContainer.appendChild(container);
        }
        return diffTable;
    }

}