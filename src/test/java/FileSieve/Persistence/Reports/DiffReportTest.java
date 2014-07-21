package FileSieve.Persistence.Reports;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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
    private String outputFilePath;

    @Before
    public void setup() throws Exception {
        baseDir = System.getProperty("user.dir");
        systemFileSeparator = System.getProperty("file.separator");
        outputFilePath = baseDir + systemFileSeparator + "test.html";
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
        try {
            File output = new File(outputFilePath);
            output.delete();
        } catch (Exception x) {
            System.err.println(x);
        }
    }

    @Test
    public void testGetReport() throws Exception {
        report = DiffReportFactory.getDiffReport(diffResults);
        String results = report.getReport();

        Document diffReport = Jsoup.parse(results);
        Element diffFileName = diffReport.select(".fileName").first();
        Element diffMatch1 = diffReport.select(".match").first();
        Element diffMatch2 = diffReport.select(".match").last();

        assertEquals("The fileName element's text should match the file name passed in.",
                fileName,diffFileName.text());
        assertEquals("The first match element's text should match the path of the first match passed in.",
                match1.getPath(),diffMatch1.text());
        assertEquals("The second match element's text should match the path of the second match passed in.",
                match2.getPath(),diffMatch2.text());
    }

    @Test
    public void testSave() throws Exception {
        report = DiffReportFactory.getDiffReport(diffResults);
        String results = report.getReport();

        report.save(baseDir + systemFileSeparator + "test.html");

        File outputFile = new File(outputFilePath);
        assertTrue(outputFile.exists());

        String output = new Scanner(outputFile).useDelimiter("\\Z").next();

        assertEquals("Save result should match the string result.", results, output);
    }
}