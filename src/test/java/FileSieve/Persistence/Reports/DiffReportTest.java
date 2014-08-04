package FileSieve.Persistence.Reports;

import FileSieve.BusinessLogic.FileManagement.FileManagerFactory;
import FileSieve.BusinessLogic.FileManagement.SwingFileManager;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class DiffReportTest {

    private SwingFileManager swingFileManager = FileManagerFactory.getSwingFileManager();
    private DiffReport report;
    private List<AbstractMap.SimpleImmutableEntry<String, List<File>>> diffResults;
    private List<String> deletedPaths;
    private String fileName;
    private String diffReportTestFolder;
    private File match1;
    private File match2;
    private String outputFilePath;

    @Before
    public void setup() throws Exception {
        diffReportTestFolder = System.getProperty("java.io.tmpdir") + "/FileSieveDiffReportTestDir";

        Assume.assumeTrue("temporary folder used for tests should not pre-exist", !(new File(diffReportTestFolder).exists()));
        new File(diffReportTestFolder).mkdir();

        outputFilePath = diffReportTestFolder + "/test.html";
        fileName = "test.txt";
        match1 = new File(diffReportTestFolder + "/test.txt");
        match2 = new File(diffReportTestFolder + "/test2.txt");
        List<File> matchList = new ArrayList<>();
        matchList.add(match1);
        matchList.add(match2);
        AbstractMap.SimpleImmutableEntry<String, List<File>> matchEntry = new AbstractMap.SimpleImmutableEntry<>(fileName, matchList);

        diffResults = new ArrayList<>();
        diffResults.add(matchEntry);

        deletedPaths = new ArrayList<>();
        deletedPaths.add(match2.getPath());
    }

    @After
    public void cleanup() throws IOException {
        swingFileManager.deletePathname(new File(diffReportTestFolder).toPath());
    }

    @Test
    public void testGetReport() throws IOException {
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
    public void testSave() throws IOException {
        report = DiffReportFactory.getDiffReport(diffResults, deletedPaths);
        String results = report.getReport();

        report.save(diffReportTestFolder + "/test.html");

        File outputFile = new File(outputFilePath);
        assertTrue(outputFile.exists());

        Scanner scanner = null;
        String output = "";
        try {
            scanner = new Scanner(outputFile);
            output = scanner.useDelimiter("\\Z").next();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }

        assertEquals("Save result should match the string result.", results, output);
    }

}