package FileSieve.Persistence.Reports;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

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

        report = DiffReportFactory.getDiffReport(diffResults);

    }

    @After
    public void cleanup() throws Exception {

    }

    @Test
    public void testGetReport() throws Exception {
        String expected = "<html>\n" +
                " <head>\n" +
                "  <title>FileSieve Diff Report</title>\n" +
                " </head>\n" +
                " <body>\n" +
                "  <h1>FileSieve Diff Report</h1>\n" +
                "  <div>\n" +
                "   <div class=\" fileName\">\n" +
                "    " + fileName + "\n" +
                "   </div>\n" +
                "   <div class=\" match\">\n" +
                "    " + match1.getPath() + "\n" +
                "   </div>\n" +
                "   <div class=\" match\">\n" +
                "    " + match2.getPath() + "\n" +
                "   </div>\n" +
                "  </div>\n" +
                " </body>\n" +
                "</html>";
        assertEquals(expected,report.getReport());
    }

    @Test
    public void testSave() throws Exception {

    }
}