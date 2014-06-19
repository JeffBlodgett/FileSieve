package FileSieve.gui;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Component;

public class CopyScreen extends JPanel{
	
	private ScreenSwitcher ss;
	
	public CopyScreen(ScreenSwitcher parentSs){

		ss = parentSs;
		this.setLayout(new BorderLayout(10,10));		
		
		//Copy Files label
		JLabel copyLabel = new JLabel();
		copyLabel.setText("Copying files to ");
		copyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		
		//Total Labels
		JLabel progressTxt = new JLabel();
		progressTxt.setText("Copied x (0 Kb) of x (0 Kb) files");
		progressTxt.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		//Copy Files list
		JList copyList = new JList();
		
		//Copy Files pane
		JScrollPane copyScrollPane = new JScrollPane();
		copyScrollPane.setSize(200, 200);
		copyScrollPane.setViewportView(copyList);
		copyScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		//Buttons	
		JButton cancelBtn = new JButton("Cancel");
		cancelBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
		JButton newSearchBtn = new JButton("New Search");
		newSearchBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
		newSearchBtn.addActionListener(new changeScreenAction(ss));
		
		//Progress bar
		JLabel totalProgressLabel = new JLabel();
		totalProgressLabel.setText("Total Progress:");
		totalProgressLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		JProgressBar totalProgressBar = new JProgressBar(0, 100);
		totalProgressBar.setValue(0);
        totalProgressBar.setStringPainted(true);
        
        JPanel progressPane = new JPanel();
        progressPane.setLayout(new BoxLayout(progressPane, BoxLayout.LINE_AXIS));
        progressPane.add(totalProgressLabel);
        progressPane.add(Box.createRigidArea(new Dimension(10,0)));
        progressPane.add(totalProgressBar);
        progressPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressPane.setBorder(BorderFactory.createEmptyBorder(10,0,10,0));
		
		//set box layout for source path components
		JPanel copyPane = new JPanel();
		copyPane.setLayout(new BoxLayout(copyPane, BoxLayout.PAGE_AXIS));
		copyPane.add(copyLabel);
		copyPane.add(Box.createRigidArea(new Dimension(0,5)));
		copyPane.add(copyScrollPane);
		copyPane.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
		copyPane.add(progressPane);
		copyPane.add(progressTxt);
		
		this.add(copyPane, BorderLayout.CENTER);
		
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
		
		buttonPane.add(cancelBtn);
		buttonPane.add(newSearchBtn);
		
		this.add(buttonPane, BorderLayout.PAGE_END);
		 
		
	}

}