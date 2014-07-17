package FileSieve.Persistence.Reports;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class DiffReportTest {
    private DiffReport report;
    private List<AbstractMap.SimpleImmutableEntry<String, List<File>>> diffResults;
    private String fileName;
    private String baseDir;
    private String systemFileSeparator;
    private File match1;
    private File match2;

    @Before
    public void setup() throws Exception {
        baseDir = System.getProperty("user.dir");
        systemFileSeparator = System.getProperty("file.separator");
        fileName = "test.txt";
        match1 = new File(baseDir + systemFileSeparator + "test.txt");
        match2 = new File(baseDir + systemFileSeparator + "test2.txt");
        List<File> matchList = new ArrayList<>();
        matchList.add(match1);
        matchList.add(match2);
        AbstractMap.SimpleImmutableEntry<String, List<File>> matchEntry
                  = new AbstractMap.SimpleImmutableEntry<>(fileName, matchList);

        diffResults = new ArrayList<>();
        diffResults.add(matchEntry);
    }

    @After
    public void cleanup() throws Exception {

    }

    @Test
    public void testGetReport() throws Exception {
        report = DiffReportFactory.getDiffReport(diffResults);
        String results = report.getReport();

        Document diffReport = Jsoup.parse(results);
        Element diffFileName = diffReport.select(".fileName").first();
        Element diffMatch1 = diffReport.select(".match").first();
        Element diffMatch2 = diffReport.select(".match").last();

        String expectedFileName = "<div class=\"fileName\">\n" +
                "  " + fileName + " \n" +
                "</div>";
        String expectedMatch1 =  "<div class=\"match\">\n" +
                "  " + match1.getPath() + " \n" +
                "</div>";
        String expectedMatch2 =  "<div class=\"match\">\n" +
                "  " + match2.getPath() + " \n" +
                "</div>";

        assertEquals(expectedFileName,diffFileName.toString());
        assertEquals(expectedMatch1,diffMatch1.toString());
        assertEquals(expectedMatch2,diffMatch2.toString());
    }

    @Test
    public void testSave() throws Exception {

    }
}