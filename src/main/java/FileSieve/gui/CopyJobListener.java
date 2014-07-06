
package FileSieve.gui;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Listens to the progress of copying operation and updates the GUI
 * @author olgakaraseva
 */
public class CopyJobListener {
    
    private static final int ONE_HUNDRED_PERCENT = 100;
    private int percentDone;
    private int totalFiles;
    private long totalBytes;
    private int filesDone;
    private long bytesDone;
    private CopyScreen copyScreen;
    private ArrayList<File> copiedFiles; //to keep track of which files are added to
                                         //display and which are just updating percentage
    
    /**
     * @param cpscrn     Copy Screen instance
     * @param totFiles   total Files to be copied
     * @param totBytes   total bytes to be copied
     */
    CopyJobListener(CopyScreen cpscrn, int totFiles, long totBytes){
        copyScreen = cpscrn;
        percentDone = 0;
        totalFiles = totFiles;
        filesDone = 0;
        totalBytes = totBytes;
        bytesDone = 0;
        copiedFiles = new ArrayList<>();
    }
    
    /**
     * Updates GUI with current state of copying process for individual file
     * @param fileBeingCopied       which file should be updated
     * @param percentProgressed     how many percents have been copied
     */
    public void UpdateCopyJobProgress(File fileBeingCopied, int percentProgressed){
        
        String fileProgress = "Copying "+fileBeingCopied.getPath()+" ("+percentProgressed+"%)";
        int fileIndex = copiedFiles.indexOf((Object) fileBeingCopied);
        
        //check if file is already in the list and add it or update it
        if(fileIndex != -1){
            copyScreen.copyListModel.set(fileIndex, fileProgress);
            if(percentProgressed == ONE_HUNDRED_PERCENT){
                bytesDone += copiedFiles.get(fileIndex).length();
                filesDone++;
                updateTotalProgress();
            }
        } else {
            //indexes have to be the same in copyListModel and copiedFiles
            copiedFiles.add(fileBeingCopied);
            copyScreen.copyListModel.addElement(fileProgress);
        }
    }
   
    // Updates total progress for all the files that should be copied
    private void updateTotalProgress(){
        percentDone = (int) (bytesDone*ONE_HUNDRED_PERCENT/totalBytes); //based on bytes
        
        copyScreen.totalProgressBar.setValue(percentDone);
        copyScreen.progressTxt.setText("Copied "+filesDone+" of "+totalFiles+" files ("+
                readableFileSize(bytesDone)+" of "+readableFileSize(totalBytes)+")");
    }
    
    // Converts file size to readable format
    private static String readableFileSize(long size) {
        if(size <= 0) return "0";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
