package FileSieve.gui;

import java.io.File;
import java.nio.file.Path;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.mockito.Mockito.*;

/**
 * Junit test for CopyJobListener class
 * @author olgakaraseva
 */
public class CopyJobListenerTest {
    
    private Controller controller;
    private CopyScreen copyScreen;
    private CopyJobListener listener;
    private int totalFiles;
    private long totalBytes;
    private static final int ONE_HUNDRED_PERCENT = 100;
    private File testFile1;
    private Path testPath1;
    private File testFile2;
    private Path testPath2;
    private Path target;
    
    @Before
    public void setup() {
        controller = new Controller();
        copyScreen = new CopyScreen(controller);
        totalFiles = 2;
        totalBytes = 600;
        
        //mock the files
        testFile1 = mock(File.class);
        when(testFile1.length()).thenReturn((long)300);
        when(testFile1.getPath()).thenReturn("test1.txt");
        when(testFile1.isFile()).thenReturn(true);
        
        testPath1 = mock(Path.class);
        when(testPath1.toFile()).thenReturn(testFile1);
        
        testFile2 = mock(File.class);
        when(testFile2.length()).thenReturn((long)300);
        when(testFile2.getPath()).thenReturn("test2.txt"); 
        when(testFile2.isFile()).thenReturn(false);
        
        testPath2 = mock(Path.class);
        when(testPath2.toFile()).thenReturn(testFile2);
        
        target = mock(Path.class);
       
        listener = new CopyJobListener(controller, copyScreen, totalFiles, totalBytes);
    }
    
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    
    @Test
    public void testUpdateCopyJobProgress(){       
        int percentDone = 60;
        listener.UpdateCopyJobProgress(null, percentDone);
        int progressBarPosition =  (int) (copyScreen.totalProgressBar.getPercentComplete()*ONE_HUNDRED_PERCENT);
        assertEquals("progress bar shows correct percentage", percentDone, progressBarPosition);
        
        //test 100 percent
        percentDone = 100;
        //exception is thrown by controller because no copy job is actually initialzed
        thrown.expect(NullPointerException.class); 
        listener.UpdateCopyJobProgress(null, percentDone);
    }

    
    @Test
    public void testUpdatePathnameCopyProgress(){
        //test individual file percentage
        int percentDone = 60;
        String fileProgressStr = "Creating "+testPath1.toString()+" ("+percentDone+"%)";
        listener.UpdatePathnameCopyProgress(null, testPath1, percentDone);
        assertTrue("individual progress shows correctly", fileProgressStr.equals(copyScreen.copyListModel.get(0).toString()));
        
        //test agian with same file - it should update
        percentDone = 70;
        fileProgressStr = "Creating "+testPath1.toString()+" ("+percentDone+"%)";
        listener.UpdatePathnameCopyProgress(null, testPath1, percentDone);
        assertTrue("individual progress shows correctly", fileProgressStr.equals(copyScreen.copyListModel.get(0).toString()));
        assertEquals("indivdual progress updates, no new list items are created", 1, copyScreen.copyListModel.getSize());
                
        //test if not file
        listener.UpdatePathnameCopyProgress(null, testPath2, percentDone);
        assertEquals("if path is directory it shouldn't be added to list", 1, copyScreen.copyListModel.getSize());
        
        //test file copy completed
        percentDone = 100;
        listener.UpdatePathnameCopyProgress(null, testPath1, percentDone);
        String totalBytesStr = FileSieve.gui.util.Utilities.readableFileSize(totalBytes);
        String bytesDoneStr = FileSieve.gui.util.Utilities.readableFileSize(300);
        String copyProgressTxt = "Copied 1 of "+totalFiles+" files ("+
                bytesDoneStr+" of "+totalBytesStr+")";
        assertTrue("Copy progress text is updated correctly", copyProgressTxt.equals(copyScreen.progressTxt.getText()));
        
    }

}
