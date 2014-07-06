
package FileSieve.gui;

import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

/**
 * Controller for the app
 * @author olgakaraseva
 */
public class Controller {
    
    private JPanel screens; //a panel that uses CardLayout
    private CopyScreen copyScreen;
    
    javax.swing.Timer t;
    
    /**
     * Called from ScreenSwitcher to initialize screen switching panel in controller
     * @param scrn      a panel that uses CardLayout
     */
    protected void setScreens(JPanel scrn, CopyScreen cpyscrn){
        screens = scrn;
        copyScreen = cpyscrn;
    }
    /**
     * Switches to new screen
     * @param screenName    which screen should be displayed
     */
    protected void changeScreen(String screenName){
        CardLayout cl = (CardLayout)(screens.getLayout());
        cl.show(screens, screenName);
    }
    
    /**
     * Gets source and target filepaths and calls copy jobs
     * @param srcTree                   provides selected source filepaths
     * @param includeSubfolders         whether to copy subfolders or not
     * @throws NullPointerException     if null srcTree is provided
     */
    protected void callCopyJob(JTree srcTree, boolean includeSubfolders){
        //don't allow null JTree
        if(srcTree == null){ throw new NullPointerException("no Tree is provided"); }

        TreePath[] paths = srcTree.getSelectionPaths(); //array of source filepaths
        Path targetPath;
        
        //proceed only if source is selected
        if(paths != null){
            
            //open target filepath selection window
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int retval = fileChooser.showOpenDialog(screens);

            //if target filepath is selected
            if (retval == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                targetPath = file.toPath();
                
                /*TO DO: call copy jobs for each selected source filepath?
                 * Get list (or # and total bytes) of files which are about to be copied and initialize 
                 * the Copy Screen with this info
                 */
                
                //stubs for # and total bytes received from FileManager
                int totalFiles = 2;
                long totalBytes = 5000;
                
                changeScreen(ScreenEnum.COPYPANEL.btnText());
                setupCopyScreen(file.toString(), totalFiles, totalBytes);
            }
        } else {
            //if no source filepath is selected alert the user
            displayAlert("Select at least one folder");
        }    
       
    }
    /**
     * Convenience method for copying files
     * @param srcTree           provides selected source filepaths
     */
    protected void callCopyJob(JTree srcTree){
        callCopyJob(srcTree, false);
    }
    
    /**
     * Gets source filepaths and calls find duplicates job
     * @param srcTree               provides selected source filepaths
     * @param includeSubfolders     whether to search for duplicate files in subfolders or not
     * @throws NullPointerException     if null srcTree is provided
     */
    protected void callDuplJob(JTree srcTree, boolean includeSubfolders){
         //don't allow null JTree
        if(srcTree == null){ throw new NullPointerException("no Tree is provided"); }
        
        TreePath[] paths = srcTree.getSelectionPaths(); //array of source filepaths
        
        //proceed only if source is selected
        if(paths != null){
            
            /*TO DO: call FileDifferentioator getDuplicatedFiles method,
             * get the list of duplicate files and initialize the Results screen
             * with this info
             */
            
            changeScreen(ScreenEnum.RESULTPANEL.btnText()); 
        } else {
            //if nothing is selected alert the user
            displayAlert("Select at least one folder");
        }
        
    }
    
    private void setupCopyScreen(String targetFolder, int totalFiles, long totalBytes){ 
        copyScreen.targetLabel.setText(targetFolder);
        
        //clear the list of files to be copied
        copyScreen.copyListModel.clear();
        copyScreen.totalProgressBar.setValue(0);
        
        //reset buttons active state
        copyScreen.newSearchBtn.setEnabled(false);
        copyScreen.cancelBtn.setEnabled(true);
            
        //stub of files for now
        ArrayList<File> filesToCopy = new ArrayList<>();
        filesToCopy.add(new File("testfiles/CopyScreen.png"));
        filesToCopy.add(new File("testfiles/SelectScreen.png"));
        
        totalBytes = 0;
        for(File f : filesToCopy){
            totalBytes += f.length();
        }
        
        CopyJobListener copyJobListener = new CopyJobListener(copyScreen, filesToCopy.size(), totalBytes);
          
        //immitate copying process
        t = new javax.swing.Timer(100, new CopyJobListenerMock(copyJobListener, filesToCopy));
        t.start();
    }
    
    //stub to immitate copying processs. Will remove
    private class CopyJobListenerMock implements ActionListener {
        CopyJobListener copyJobListener;
        ArrayList<File> filesToCopy;
        int percent = 0;
        int curFile = 0;

        private CopyJobListenerMock(CopyJobListener cpyJobListener, ArrayList<File> filesToCpy) {
            copyJobListener = cpyJobListener;
            filesToCopy = filesToCpy;
        }
        
        @Override
    	public void actionPerformed(ActionEvent e) {
            copyJobListener.UpdateCopyJobProgress(filesToCopy.get(curFile), percent);
            if(percent == 100){
                percent = 0;
                curFile++;
            } else {
                percent+=10;
            }
            if(curFile == filesToCopy.size()){
                stopCopyJob(false);
            }
        }
    }
    
    /**
     * Finishes file copying process
     */
    protected void stopCopyJob(Boolean interrupted){
        
        //Cancel copying
        t.stop();
        
        //add empty row to list
        copyScreen.copyListModel.addElement(" ");
        //if all files are copied
        if(interrupted){
            copyScreen.copyListModel.addElement("File copying process has been stopped.");
        } else {
            copyScreen.copyListModel.addElement("All files are copied! Hooray!");
        }
        
        //reset buttons active state
        copyScreen.newSearchBtn.setEnabled(true);
        copyScreen.cancelBtn.setEnabled(false);
    }
    
    //helper method to display alerts to user
    private void displayAlert(String msg){
        JOptionPane.showMessageDialog(screens, msg, "Can't proceed", JOptionPane.WARNING_MESSAGE);
    }
}
