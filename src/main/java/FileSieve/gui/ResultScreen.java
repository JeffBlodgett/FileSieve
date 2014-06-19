package FileSieve.gui;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Component;

public class ResultScreen extends JPanel{
	
	private ScreenSwitcher ss;
	
	public ResultScreen(ScreenSwitcher parentSs){
		
		ss = parentSs;
		this.setLayout(new BorderLayout(10,10));
		
		
		//Labels
		JLabel resultLabel = new JLabel();
		resultLabel.setText("Results");
		resultLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel fileCntLabel = new JLabel();
		fileCntLabel.setText("Searched x files, found x duplicates");
		fileCntLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel selectedFilesLabel = new JLabel();
		selectedFilesLabel.setText("Selected 0 files, 0 Kb");
		selectedFilesLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		
		
		//Source Filepath list
		JList duplicatesList = new JList();
		
		//Source Filepath pane
		JScrollPane resultScrollPane = new JScrollPane();
		resultScrollPane.setViewportView(duplicatesList);
		resultScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		//Buttons
		JButton copyBtn = new JButton("Copy To");
		copyBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
		copyBtn.addActionListener(new changeScreenAction(ss));
		
		JButton deleteBtn = new JButton("Delete");
		deleteBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
		
		JButton reportBtn = new JButton("Export Report");
		reportBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
		
		JButton newSearchBtn = new JButton("New Search");
		newSearchBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
		newSearchBtn.addActionListener(new changeScreenAction(ss));
		
		JPanel titlePane = new JPanel();
		titlePane.setLayout(new BoxLayout(titlePane, BoxLayout.LINE_AXIS));
		titlePane.add(resultLabel);
		titlePane.add(Box.createHorizontalGlue());
		titlePane.add(reportBtn);
		titlePane.add(newSearchBtn);
		titlePane.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		//set box layout for source path components
		JPanel resultsPane = new JPanel();
		resultsPane.setLayout(new BoxLayout(resultsPane, BoxLayout.PAGE_AXIS));
		resultsPane.add(titlePane);
		resultsPane.add(Box.createRigidArea(new Dimension(0,5)));
		resultsPane.add(resultScrollPane);
		
		JPanel totalsPane = new JPanel();
		totalsPane.setLayout(new BoxLayout(totalsPane, BoxLayout.LINE_AXIS));
		totalsPane.add(fileCntLabel);
		totalsPane.add(Box.createHorizontalGlue());
		totalsPane.add(selectedFilesLabel);
		totalsPane.setAlignmentX(Component.LEFT_ALIGNMENT);
		resultsPane.add(totalsPane);
		
		resultsPane.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
		this.add(resultsPane, BorderLayout.CENTER);
		
		
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
		
		buttonPane.add(copyBtn);
		buttonPane.add(deleteBtn);
		
		this.add(buttonPane, BorderLayout.PAGE_END);
		
	}

}