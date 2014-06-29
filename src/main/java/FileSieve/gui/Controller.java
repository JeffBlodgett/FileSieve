
package FileSieve.gui;

import java.awt.CardLayout;
import java.io.File;
import java.nio.file.Path;
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
    
    /**
     * Called from ScreenSwitcher to initialize screen switching panel in controller
     * @param scrn      a panel that uses CardLayout
     */
    protected void setScreens(JPanel scrn){
        screens = scrn;
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
                
                /*TO DO: call copy jobs for each select source filepath
                 * Get list of files which are about to be copied and initialize 
                 * the Copy Screen with this info
                 */
                
                changeScreen(ScreenEnum.COPYPANEL.btnText());
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
    
    //helper method to display alerts to user
    private void displayAlert(String msg){
        JOptionPane.showMessageDialog(screens, msg, "Can't proceed", JOptionPane.WARNING_MESSAGE);
    }
}
