package FileSieve.gui;

import FileSieve.Persistence.Preferences.WindowPlacementPreferences;
import javax.swing.*;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;


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
         
        
        //on window close save window placement preferences
        mainFrame.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        mainFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                windowPrefs.save();
                System.exit(0); 
            }
        });
        
        //Set up the content pane.
        addComponentToPane(mainFrame.getContentPane()); 
        
        //Display the window.     
        mainFrame.setVisible(true);
        
    }
    
    private void addComponentToPane(Container pane) {
        
        //set window title
	JLabel windowLabel = new JLabel();
	windowLabel.setFont(new java.awt.Font("Arial", Font.BOLD, 20));
	windowLabel.setText("File Sieve");
	windowLabel.setHorizontalAlignment(JLabel.CENTER);
	pane.add(windowLabel, BorderLayout.NORTH);
        
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