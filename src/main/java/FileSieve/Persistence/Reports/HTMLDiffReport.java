package FileSieve.Persistence.Reports;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * A DiffReport class for outputting HTML reports
 */
public class HTMLDiffReport implements DiffReport {
    private static String REPORT_TITLE = "FileSieve Diff Report";
    private List<SimpleImmutableEntry<String, List<File>>> fileDiffResults;
    private Document reportDoc;

    public HTMLDiffReport(List<SimpleImmutableEntry<String, List<File>>> diffResults) {
        fileDiffResults = diffResults;
    }

    /**
     * Returns the contents of the report
     *
     * @return                          string with report contents
     */
    public String getReport() {
        reportDoc = Document.createShell("");
        reportDoc.title(REPORT_TITLE);
        Element header = reportDoc.body().appendElement("h1").text(REPORT_TITLE);
        for (SimpleImmutableEntry<String, List<File>> diffResult : fileDiffResults) {
            getDiffTable(reportDoc, diffResult);
        }
        return reportDoc.toString();
    }

    /**
     * Saves the report to the specified path.
     *
     * @param savePath         path and filename for where to save the file
     * @throws java.io.IOException
     */
    public void save(String savePath) throws IOException {
        try(FileWriter fw = new FileWriter(savePath)) {
            fw.write(reportDoc.toString());
        }
    }

    /**
     * Creates a "table"* with a heading of the file name and a list of all matching files.
     * The element is appended to the document passed to it, but it is also returned, where it can be modified further.
     *      *table is a reference to the visual style that will eventually be applied, but it's an HTML div tag.
     *
     * @param doc           the jsoup Document element being used to form this report.
     * @param diffResult    the diff result to represent in table form
     *
     * @return              a jsoup Element representing a div containing this file's listing of matches
     */
    private Element getDiffTable(Document doc, SimpleImmutableEntry<String, List<File>> diffResult) {
        Element diffTable = doc.body().appendElement("div");
        diffTable.appendElement("div").text(diffResult.getKey()).addClass("fileName");
        for (File f : diffResult.getValue()) {
            diffTable.appendElement("div").text(f.getPath()).addClass("match");
        }
        return diffTable;
    }
}