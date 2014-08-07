package FileSieve.gui;

import FileSieve.Persistence.Preferences.WindowPlacementPreferences;
import javax.swing.*;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;


/**
 * Main JFrame with CardLayout JPanel to switch the screens 
 * @author olgakaraseva
 */
public class ScreenSwitcher {
    
    JPanel screens; //a panel that uses CardLayout
    private Controller controller;
    private WindowPlacementPreferences windowPrefs; //keeps window placement preferences
    private final static int DEFAULT_WIDTH = 1000;
    private final static int DEFAULT_HEIGHT = 600;
    private final static int MINIMAL_WIDTH = 300; //main frame can't be smaller than that
    
    public ScreenSwitcher(Controller cntrl){
        controller = cntrl;
        
        //Create and set up the window.
        JFrame mainFrame = new JFrame("File Sieve");
  
        //Load window placement preferences
        windowPrefs = new WindowPlacementPreferences(mainFrame);
        boolean hadOriginalPrefs = windowPrefs.getPrefsSet();

        //if no window placement preferences are saved set the defaults
        if(!hadOriginalPrefs){
            windowPrefs.setDefaultSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        } 
        //place the window according to saved preferences or defaults
        windowPrefs.load(mainFrame);
        
        //ensure that frame width is not too small
        if(mainFrame.getWidth() <= MINIMAL_WIDTH){
            try {
                windowPrefs.clear();
                windowPrefs.setDefaultSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
                windowPrefs.load(mainFrame);
            } catch (BackingStoreException ex) {
                //set the frame size
                mainFrame.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
            }
            
        }
        
        //on window close save window placement preferences
        mainFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        mainFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                windowPrefs.save();

                if (controller.swingCopyJob != null) {
                    controller.swingCopyJob.cancelJob();
                }
            }
        });
        
        //Set up the content pane.
        addComponentToPane(mainFrame.getContentPane()); 
        
        //Display the window.     
        mainFrame.setVisible(true);
        
    }
    
    private void addComponentToPane(Container pane) {
        
        //set screens
        SelectScreen selectScreen = new SelectScreen(controller);
        ResultScreen resultScreen = new ResultScreen(controller);
        CopyScreen copyScreen = new CopyScreen(controller);
        
        screens = new JPanel(new CardLayout());

        screens.add(selectScreen, ScreenEnum.SELECTPANEL.btnText());
        screens.add(resultScreen, ScreenEnum.RESULTPANEL.btnText());
        screens.add(copyScreen, ScreenEnum.COPYPANEL.btnText());
        
        pane.add(screens, BorderLayout.CENTER);
        
        //pass screens to controller
        controller.setScreens(screens, copyScreen, resultScreen);
    } 

}