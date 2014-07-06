package FileSieve.gui;

import javax.swing.*;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;


/**
 * Main application JFrame with CardLayout JPanel which switches the screens 
 * @author olgakaraseva
 */
public final class ScreenSwitcher {
    
    JPanel screens; //a panel that uses CardLayout
    private Controller controller;
    
    public ScreenSwitcher(Controller cntrl){
        controller = cntrl;
        
        //Create and set up the window.
        JFrame mainFrame = new JFrame("File Sieve");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        //Set up the content pane.
        addComponentToPane(mainFrame.getContentPane());   
        mainFrame.setSize(600, 400);
        mainFrame.pack();
        
        //Display the window.     
        mainFrame.setVisible(true);
    }
    
    public void addComponentToPane(Container pane) {
        
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
        controller.setScreens(screens, copyScreen);
    }
    
}