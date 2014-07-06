
package FileSieve.gui;

import java.io.File;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

/**
 *
 * @author olgakaraseva
 */
public class CopyJobListenerTest {
    
    private Controller controller;
    private CopyScreen copyScreen;
    private CopyJobListener listener;
    private int totalFiles;
    private long totalBytes;
    private static final int ONE_HUNDRED_PERCENT = 100;
    
    @Before
    public void setup() {
        controller = new Controller();
        copyScreen = new CopyScreen(controller);
        totalFiles = 3;
        totalBytes = 900;
        listener = new CopyJobListener(copyScreen, totalFiles, totalBytes);
    }
    
    @Test
    public void testUpdateCopyJobProgress(){
        File testFile = mock(File.class);
        when(testFile.length()).thenReturn((long)300);
        when(testFile.getPath()).thenReturn("test.txt");
        
        
        //test individual file percentage
        int percentDone = 60;
        String fileProgressStr = "Copying "+testFile.getPath()+" ("+percentDone+"%)";
        listener.UpdateCopyJobProgress(testFile, percentDone);
        assertTrue("individual progress shows correctly", fileProgressStr.equals(copyScreen.copyListModel.get(0).toString()));
        
        //test JBar position
        percentDone = 100;
        int totalPercentDone = (int) (testFile.length()*ONE_HUNDRED_PERCENT/totalBytes);
        listener.UpdateCopyJobProgress(testFile, percentDone);
        int progressBarPosition =  (int) (copyScreen.totalProgressBar.getPercentComplete()*ONE_HUNDRED_PERCENT);
        assertEquals("progress bar shows correct percentage", totalPercentDone, progressBarPosition);
        
        //test total texts
        String totalsTxt = "Copied 1 of 3 files (300 B of 900 B)";
        assertTrue("Totals text shows correctly", totalsTxt.equals(copyScreen.progressTxt.getText()));
        
    }

}
