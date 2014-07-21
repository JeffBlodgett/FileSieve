package FileSieve.gui;

import FileSieve.BusinessLogic.FileDifferentiation.FileDifferentiator;
import FileSieve.BusinessLogic.FileDifferentiation.FileDifferentiatorFactory;
import FileSieve.BusinessLogic.FileEnumeration.FileEnumerator;
import FileSieve.BusinessLogic.FileEnumeration.FileEnumeratorFactory;
import FileSieve.BusinessLogic.FileManagement.FileManagerFactory;
import FileSieve.BusinessLogic.FileManagement.SwingCopyJob;
import FileSieve.BusinessLogic.FileManagement.SwingCopyJobListener;
import FileSieve.BusinessLogic.FileManagement.SwingFileManager;
import FileSieve.Persistence.Preferences.SelectedPaths;
import FileSieve.Persistence.Reports.DiffReport;
import FileSieve.Persistence.Reports.DiffReportFactory;
import java.awt.CardLayout;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.tree.TreePath;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * Connects the view to model
 * Populates the view based on results obtained from the model
 * Calls model methods when certain actions occur in view (i.e. button is clicked)
 * 
 * @author olgakaraseva
 */
public class Controller {
    
    private JPanel screens; //a panel that uses CardLayout
    CopyScreen copyScreen; 
    ResultScreen resultScreen;
    private FileEnumerator fileEnumerator;
    private FileDifferentiator fileDifferentiator; 
    static SwingFileManager swingFileManager; //protected so CheckTreeManager could access it (or does it create its own?)
    private SwingCopyJob swingCopyJob; 
    private DiffReport diffReport;
    List<SimpleImmutableEntry<String, List<File>>> duplicates; //protected so test could mock it
    JFileChooser fileChooser; //protected so test could mock it
    boolean isTest = false; //used to skip some gui methods for test purposes
    
    public Controller(){
       fileEnumerator = FileEnumeratorFactory.getFileEnumerator();
       fileDifferentiator = FileDifferentiatorFactory.getFileDifferentiator();
       swingFileManager = FileManagerFactory.getSwingFileManager();
       fileChooser = new JFileChooser();
       fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    }
    
    /**
     * Called from ScreenSwitcher to initialize screen switching panel in controller
     * @param scrn      a panel that uses CardLayout
     * @param cpyscrn   CopyScreen instance as set by ScreenSwitcher
     * @param rsltscrn  ResultScreen instance as set by ScreenSwitcher
     */
    protected void setScreens(JPanel scrn, CopyScreen cpyscrn, ResultScreen rsltscrn){
        screens = scrn;
        copyScreen = cpyscrn;
        resultScreen = rsltscrn;
    }
    
    /**
     * Switches to new screen
     * @param screenName                which screen should be displayed
     * @throws NullPointerException     if no screenName is provided
     * @throws IllegalArgumentException if screenName is not available, i.e. not in ScreenEnum
     */
    protected void changeScreen(String screenName){
        if(screenName == null){
            throw new NullPointerException("screenName cannot be null");
        }
        if(!ScreenEnum.isMember(screenName)){
            throw new IllegalArgumentException("There is no such screen");
        }
        CardLayout cl = (CardLayout)(screens.getLayout());
        cl.show(screens, screenName);
    }
    
     /**
     * Checks whether at least one filepath has been selected
     * Shows alert if no filepaths are selected
     * @param paths                selected filepaths
     */
    protected boolean pathsAreSelected(TreePath[] paths){ 
        boolean selected;
        //proceed only if source is selected
        if(paths != null && paths.length > 0){
            selected = true;
        } else {
            //if nothing is selected alert the user
            displayAlert("Select at least one folder or file");
            selected = false;
        }
        return selected;
    }
    
    /**
     * Gets source and target filepaths and calls copy job
     * @param paths                     selected filepaths
     * @param includeSubfolders         whether to copy subfolders or not
     */
    protected void callCopyJob(TreePath[] paths, boolean includeSubfolders){
        
        //proceed only if source paths are selected
        if(pathsAreSelected(paths)){
            
            //load saved target folder and set fileChooser
            SelectedPaths loadSelectedPaths = new SelectedPaths();
            if(!loadSelectedPaths.getTargetPathName().equals("")){
                File savedTarget = new File(loadSelectedPaths.getTargetPathName());
                fileChooser.setCurrentDirectory(new File(savedTarget.getParent()));
            }
            //open target filepath selection window
            int retval = fileChooser.showOpenDialog(screens);
            
            
            //if target filepath is selected
            if (retval == JFileChooser.APPROVE_OPTION) {
                File target = fileChooser.getSelectedFile();
                Path targetPath = target.toPath();
                
                //convert TreePath[] paths to List<Path>
                List<Path> listOfPaths = new ArrayList<Path>(paths.length);
                List<String> sourcePathsToSave = new ArrayList<String>(paths.length);
                for(TreePath path : paths){
                    Path addPath = Paths.get(path.getLastPathComponent().toString());
                    listOfPaths.add(addPath);
                    sourcePathsToSave.add(path.getLastPathComponent().toString());
                }
                
                //save selected paths
                SelectedPaths saveSelectedPaths = new SelectedPaths(sourcePathsToSave, target.toString());
                
                saveSelectedPaths.save();

                try{
                    //get all files and folders in selected source paths
                    Map<Path, BasicFileAttributes> discoveredPaths = fileEnumerator.getPathnames(listOfPaths, includeSubfolders);
                    int totalFiles = fileEnumerator.getFileCount();
                    long totalBytes = fileEnumerator.getByteCount();
                    
                    //convert a Map to a Set
                    Set<Path> pathsToCopy = new LinkedHashSet<Path>(discoveredPaths.keySet());
                    
                    changeScreen(ScreenEnum.COPYPANEL.btnText());
                    setupCopyScreen(target.toString(), totalFiles, totalBytes);
                    
                    //don't add listener in test in order to be able to test that the screen was setup correctly
                    if(!isTest){ 
                        SwingCopyJobListener swingCopyJobListener = new CopyJobListener(this, copyScreen, totalFiles, totalBytes);
                        swingFileManager.setCopyOperationsListener(swingCopyJobListener);
                    }
                    //recursion is disabled for SwingCopyJob since
                    //FileEnumerator has done all the work and there is no need to do it again
                    swingCopyJob = swingFileManager.copyPathnames(pathsToCopy, targetPath, false, false, null);
                    
                } catch(IOException ioe){
                    displayAlert("Error accessing files: "+ioe.getMessage());
                }
            } //target is selected
        } //sources are selected
       
    } //callCopyJob
    
    /**
     * Prepares the copy screen to display new copy process
     * @param targetFolder              where files will be copied to
     * @param totalFiles                total number of files to be copied
     * @param totalBytes                total number of bytes to be copied
     * @throws NullPointerException     if copyScreen is not initialized
     */
    private void setupCopyScreen(String targetFolder, int totalFiles, long totalBytes){
        if(copyScreen == null){
            throw new NullPointerException("Copy screen is not initialized.");
        }
        //update the labels texts
        copyScreen.targetLabel.setText(targetFolder);
        String totalBytesStr = FileSieve.gui.util.Utilities.readableFileSize(totalBytes);
        copyScreen.progressTxt.setText("Copied 0 of "+totalFiles+" files (0 B of "+totalBytesStr+")");
        
        //clear the list of files to be copied
        copyScreen.copyListModel.clear();
        copyScreen.totalProgressBar.setValue(0);
        
        //reset buttons active state
        copyScreen.newSearchBtn.setEnabled(false);
        copyScreen.cancelBtn.setEnabled(true);

    } //setupCopyScreen
    
    /**
     * Finishes file copying process
     * @param interrupted           indicates whether the copying job was cancelled (interrupted)
     *                              or the copy job successfully finished
     * @throws NullPointerException if swingCopyJob is not initialized 
     *                              (can't stop copy job if it didn't start)
     *                              if copyScreen is not initialized
     */
    protected void stopCopyJob(Boolean interrupted){
        if(copyScreen == null){
            throw new NullPointerException("Copy screen is not initialized.");
        }
        //add empty row to list
        copyScreen.copyListModel.addElement(" ");
        
        if(interrupted){
            //swingCopyJob is always null in tests, so allowing this code to run in test
            //will always through exception and there will be no way to test notifications
            if(!isTest){
                if(swingCopyJob == null){
                    throw new NullPointerException("Copy job is not initialized - can't stop it");
                }
                swingCopyJob.cancelJob();
            }
            copyScreen.copyListModel.addElement("File copying process has been stopped.");
        } else {
            copyScreen.copyListModel.addElement("All files are copied! Hooray!");
        }
        
        //reset buttons active state
        copyScreen.newSearchBtn.setEnabled(true);
        copyScreen.cancelBtn.setEnabled(false);
        
    } //stopCopyJob
    
    /**
     * Gets source filepaths and calls find duplicates job
     * @param paths                     provides selected source filepaths
     * @param includeSubfolders         search for duplicate files in subfolders or not
     */
    protected void callDuplJob(TreePath[] paths, boolean includeSubfolders){
        //proceed only if source paths are selected
        if(pathsAreSelected(paths)){
            
            //convert TreePath[] paths to List<Path>
            List<Path> listOfPaths = new ArrayList<Path>(paths.length);
            for(TreePath path : paths){
                Path addPath = Paths.get(path.getLastPathComponent().toString());
                listOfPaths.add(addPath);
            }

            try{
                //get all files and folders in selected source paths
                Map<Path, BasicFileAttributes> discoveredPaths = fileEnumerator.getPathnames(listOfPaths, includeSubfolders);
                int totalFilesSearched = fileEnumerator.getFileCount();
                long totalBytesSearched = fileEnumerator.getByteCount();
                
                //find duplicate files for all selected paths
                duplicates = fileDifferentiator.getDuplicatedFiles(discoveredPaths);

                //if duplicates are found go to result screen
                if(duplicates.size() > 0){
                    changeScreen(ScreenEnum.RESULTPANEL.btnText());
                    setupResultScreen(duplicates, totalFilesSearched, totalBytesSearched);
                //otherwise stay on select screen and notify the user that there are no duplicates
                } else {
                    displayAlert("No duplicate files are found");
                }
            } catch(IOException ioe){
                displayAlert("Error accessing files: "+ioe.getMessage());
            }
        } // sources are selected
        
    } // callDuplJob
    
    /**
     * Populates the result screen with found duplicates list
     * @param foundDuplicates           list of found duplicates
     * @param totalFilesSearched        how many files has been compared
     * @param totalBytesSearched        how many bytes there are in files that were compared
     * @throws NullPointerException     if result screen is not initialized
     */
    private void setupResultScreen(List<AbstractMap.SimpleImmutableEntry<String, List<File>>> foundDuplicates,
                                    int totalFilesSearched, long totalBytesSearched){
        
        if(resultScreen == null){
            throw new NullPointerException("Result screen is not initialized.");
        }
        //used to calculate total filesize for all duplicates
        int duplicateCount = 0;
        long duplicateBytes = 0;
        
        //remove all nodes from Jtree
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) resultScreen.duplicatesList.getModel().getRoot();
        root.removeAllChildren();
        DefaultTreeModel treeModel = (DefaultTreeModel) resultScreen.duplicatesList.getModel();
        treeModel.reload();
        resultScreen.duplicatesList.treeDidChange();
        resultScreen.duplicatesList.setRootVisible(true);
        
        //unselect any previously selected paths
        TreePath[] oldSelectedPaths = resultScreen.checkTree.getSelectionPaths();
        resultScreen.checkTree.getSelectionModel().removeSelectionPaths(oldSelectedPaths);
        
        //populate jTree with duplicate files discovered by FileDifferentiator
        for(SimpleImmutableEntry<String, List<File>> duplicate : foundDuplicates){
            DefaultMutableTreeNode filename = new DefaultMutableTreeNode(duplicate.getKey());
            root.add(filename);
            boolean firstChild = true; //first node in duplicate files list for a particular filename
            for(File f : duplicate.getValue()){
                DefaultMutableTreeNode dupFile = new DefaultMutableTreeNode(f);
                filename.add(dupFile);
                duplicateCount++;
                duplicateBytes += f.length();
                
                //select all files except first one so user can easily delete all duplicates
                if(firstChild){
                     firstChild = false;
                } else {
                    resultScreen.checkTree.getSelectionModel().addSelectionPath(new TreePath(dupFile.getPath()));
                }
            }
        }
        
        //expand root and all rows
        int row = 0;
        while(row < resultScreen.duplicatesList.getRowCount()){
            resultScreen.duplicatesList.expandRow(row);
            row++;
        }
        resultScreen.duplicatesList.setRootVisible(false);
        resultScreen.duplicatesList.setToggleClickCount(0); //prevents from expanding / collapsing tree nodes
        
        //setup results text
        String totalBytesSearchedStr = FileSieve.gui.util.Utilities.readableFileSize(totalBytesSearched);
        String duplicateBytesStr = FileSieve.gui.util.Utilities.readableFileSize(duplicateBytes);
        resultScreen.fileCntLabel.setText("Searched "+totalFilesSearched+" files ("+totalBytesSearchedStr+")."+
                " Found "+duplicateCount+" duplicate files ("+duplicateBytesStr+").");
        
    } //setupResultScreen
    
    /**
     * Deletes selected paths
     * @param paths         paths to be deleted
     */ 
    protected void callDeleteJob(TreePath[] paths){
        //proceed only if paths are selected
        if(pathsAreSelected(paths)){
            //reconfirm user wants to delete the selected files
            int confirmDelete = JOptionPane.NO_OPTION;
            //skip confirmation for tests
            if(!isTest){ 
                confirmDelete = JOptionPane.showConfirmDialog(screens, 
                        "Are you sure you want to delete selected files?",
                        "WARNING", JOptionPane.YES_NO_OPTION);
            }
           
            if(confirmDelete == JOptionPane.YES_OPTION || isTest){

                DefaultTreeModel treeModel = (DefaultTreeModel) resultScreen.duplicatesList.getModel();
                long deletedBytes = 0;
                for(TreePath path : paths){
                    
                    try{    
                        File deletedFile = new File(path.getLastPathComponent().toString());
                        deletedBytes += deletedFile.length();
                        long tempBytes = deletedFile.length();
                        //delete the file
                        Path filePath = Paths.get(path.getLastPathComponent().toString());
                        boolean deletedSuccessfully = swingFileManager.deletePathname(filePath);
                        if(deletedSuccessfully){
                            //remove selected paths from JTree
                            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                            treeModel.removeNodeFromParent(node);
                            //if there are no leaves left in the branch remove the branch
                            if(parent.getChildCount() == 0){
                                treeModel.removeNodeFromParent(parent);
                            }
                            resultScreen.duplicatesList.treeDidChange();
                            //remove selected paths from selection model
                            resultScreen.checkTree.getSelectionModel().removeSelectionPath(path);
                        } else {
                            //file was not deleted, so extract its filesize from total deleted bytes
                            deletedBytes -= tempBytes;
                        }
                    } catch(IOException ioe){
                        displayAlert("Couldn't delete file "+path.getLastPathComponent()+". "+ioe.getMessage());
                    }
                }
                //update result text
                String deletedBytesStr = FileSieve.gui.util.Utilities.readableFileSize(deletedBytes);
                resultScreen.fileCntLabel.setText("Deleted "+paths.length+" duplicate files ("+deletedBytesStr+").");
                
            } //deletion confirmed
            
        } // paths are selected
        
    } //callDeleteJob
    
    
    protected void saveDiffReport(){ 
        if(duplicates == null){
            throw new NullPointerException("No duplicates are found yet");
        }
        //open target filepath selection window
        int retval = fileChooser.showOpenDialog(screens);
            
        //if target filepath is selected
        if (retval == JFileChooser.APPROVE_OPTION) {
            String targetPath = fileChooser.getSelectedFile().toString();
            try {
                diffReport = DiffReportFactory.getDiffReport(duplicates);
                if(!isTest){
                   JOptionPane.showMessageDialog(screens, "Report is saved", "Success", JOptionPane.INFORMATION_MESSAGE); 
                }
            } catch (IOException ex) {
                displayAlert("Couldn't create the report");
            }
            try {
                diffReport.save(targetPath+"/FileSieveDiffReport.html");
            } catch (IOException|NullPointerException ex) {
                displayAlert("Couldn't save the report");
            }
        }
    }
    
    //helper method to display alerts to user
    private void displayAlert(String msg){
        //maybe add condition if not test?
        if(!isTest){
            JOptionPane.showMessageDialog(screens, msg, "Can't proceed", JOptionPane.WARNING_MESSAGE);
        }
    }
}
