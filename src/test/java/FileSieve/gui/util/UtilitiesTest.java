
package FileSieve.gui.util;

import java.util.Calendar;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * Junit test for Utilities class
 * @author olgakaraseva
 */
public class UtilitiesTest {
    
    @Test
    public void testReadableFileSize(){
        String fileSize1 = Utilities.readableFileSize(100);
        assertTrue("File size should be 100 B", fileSize1.equals("100 B"));
        
        String fileSize2 = Utilities.readableFileSize(1024*100);
        assertTrue("File size should be 100 Kb", fileSize2.equals("100 Kb"));
        
        String fileSize3 = Utilities.readableFileSize(1024*1024*100);
        assertTrue("File size should be 100 Mb", fileSize3.equals("100 Mb"));
    }
    
    @Test
    public void testReadableDate(){
        Calendar calInstance = Calendar.getInstance();
        calInstance.set(2014, 0, 1, 5, 5); //January 1, 2014 @ 5:05am
        long testDateStamp1 = calInstance.getTimeInMillis();
        String readableDate1 = "01/01/14 5:05";
        
        calInstance.set(2010, 2, 5, 20, 18); //March 5, 2010 @ 8:18pm
        long testDateStamp2 = calInstance.getTimeInMillis();
        String readableDate2 = "03/05/10 20:18";
        
        String getDate1 = Utilities.readableDate(testDateStamp1);
        assertTrue("Conversion correct - January 1, 2014 @ 5:05am", getDate1.equals(readableDate1));
        
        String getDate2 = Utilities.readableDate(testDateStamp2);
        assertTrue("Conversion correct - March 5, 2010 @ 8:18pm", getDate2.equals(readableDate2));
    }
}
