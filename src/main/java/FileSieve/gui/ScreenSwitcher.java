package FileSieve.gui;

import javax.swing.*;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;

public class ScreenSwitcher {
    JPanel screens; //a panel that uses CardLayout
    final static String SELECTPANEL = "New Search";
    final static String RESULTPANEL = "Find Duplicate Files";
    final static String COPYPANEL = "Copy To";
    
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame mainFrame = new JFrame("File Sieve");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        //Create and set up the content pane.
        ScreenSwitcher screenSwitch = new ScreenSwitcher();
        screenSwitch.addComponentToPane(mainFrame.getContentPane());
        
        mainFrame.setSize(600, 400);
        //Display the window.
        mainFrame.pack();
        mainFrame.setVisible(true);
    }
    
    public void addComponentToPane(Container pane) {
        
        //set window title
		JLabel windowLabel = new JLabel();
		windowLabel.setFont(new java.awt.Font("Arial", Font.BOLD, 20));
		windowLabel.setText("File Sieve");
		windowLabel.setHorizontalAlignment(JLabel.CENTER);
		pane.add(windowLabel, BorderLayout.NORTH);
        
        SelectScreen selectScreen = new SelectScreen(this);
        ResultScreen resultScreen = new ResultScreen(this);
        CopyScreen copyScreen = new CopyScreen(this);
        
        screens = new JPanel(new CardLayout());
        screens.add(selectScreen, SELECTPANEL);
        screens.add(resultScreen, RESULTPANEL);
        screens.add(copyScreen, COPYPANEL);
        
        pane.add(screens, BorderLayout.CENTER);

    }
    
    public void itemStateChanged(String evt) {
        CardLayout cl = (CardLayout)(screens.getLayout());
        cl.show(screens, evt);
    }
    
    
    public static void main(String[] args) {
        
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createAndShowGUI();
            }
        });
    }
}