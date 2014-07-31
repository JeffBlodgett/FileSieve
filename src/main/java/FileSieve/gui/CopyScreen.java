package FileSieve.gui;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * View for displaying copying progress
 * Allows to cancel copy job
 * @author olgakaraseva
 */

public class CopyScreen extends JPanel{
	
    private Controller controller;
    JLabel targetLabel;
    JLabel progressTxt;
    JList<String> copyList;
    DefaultListModel<String> copyListModel;
    JProgressBar totalProgressBar;
    JButton cancelBtn;
    JButton newSearchBtn;
	
    CopyScreen(Controller cntrl){

	controller = cntrl;
	setLayout(new BorderLayout(10,10));		
		
	//Copy Files label
	JLabel copyLabel = new JLabel();
	copyLabel.setText("Copying files to ");
	copyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        targetLabel = new JLabel();
        targetLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		
	//Total Label
	progressTxt = new JLabel();
	progressTxt.setAlignmentX(Component.LEFT_ALIGNMENT);
		
	//Copy Files list
    copyListModel = new DefaultListModel<>();
    copyList = new JList<>(copyListModel);
		
	//Scroll pane for copy files list
	JScrollPane copyScrollPane = new JScrollPane();
	copyScrollPane.setSize(200, 200);
	copyScrollPane.setViewportView(copyList);
	copyScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

	//Buttons	
	cancelBtn = new JButton("Cancel");
	cancelBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        cancelBtn.setEnabled(false);
        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.stopCopyJob(true);
            }
        });
        
	newSearchBtn = new JButton("New Search");
	newSearchBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
	newSearchBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.changeScreen(e.getActionCommand());
            }
        });
		
	//Progress bar
	JLabel totalProgressLabel = new JLabel();
	totalProgressLabel.setText("Total Progress:");
	totalProgressLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
	totalProgressBar = new JProgressBar(0, 100);
	totalProgressBar.setValue(0);
    totalProgressBar.setStringPainted(true);
        
    //add components to box layout
    JPanel progressPane = new JPanel();
    progressPane.setLayout(new BoxLayout(progressPane, BoxLayout.LINE_AXIS));
    progressPane.add(totalProgressLabel);
    progressPane.add(Box.createRigidArea(new Dimension(10,0)));
    progressPane.add(totalProgressBar);
    progressPane.setAlignmentX(Component.LEFT_ALIGNMENT);
    progressPane.setBorder(BorderFactory.createEmptyBorder(10,0,10,0));
		
	//set box layout for source path components
    JPanel copyPaneTxt = new JPanel();
    copyPaneTxt.setLayout(new BoxLayout(copyPaneTxt, BoxLayout.LINE_AXIS));
    copyPaneTxt.add(copyLabel);
    copyPaneTxt.add(targetLabel);
    copyPaneTxt.setAlignmentX(Component.LEFT_ALIGNMENT);
        
	JPanel copyPane = new JPanel();
	copyPane.setLayout(new BoxLayout(copyPane, BoxLayout.PAGE_AXIS));
	copyPane.add(copyPaneTxt);
	copyPane.add(Box.createRigidArea(new Dimension(0,5)));
	copyPane.add(copyScrollPane);
	copyPane.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
	copyPane.add(progressPane);
	copyPane.add(progressTxt);
		
	add(copyPane, BorderLayout.CENTER);
		
	JPanel buttonPane = new JPanel();
	buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
	buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
		
	buttonPane.add(cancelBtn);
	buttonPane.add(newSearchBtn);
		
	add(buttonPane, BorderLayout.PAGE_END);
	
    }

}