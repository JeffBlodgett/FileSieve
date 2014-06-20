package FileSieve.gui;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Component;

public class SelectScreen extends JPanel{
	
	private ScreenSwitcher ss;
	
	public SelectScreen(ScreenSwitcher parentSs){
		
		ss = parentSs;
		this.setLayout(new BorderLayout(10,10));
		
		
		//Select Source Filepath label
		JLabel srcLabel = new JLabel();
		srcLabel.setText("Select Folders");
		srcLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		//Source Filepath list
		JList srcFilepathList = new JList();
		
		//Source Filepath this
		JScrollPane srcScrollPane = new JScrollPane();
		srcScrollPane.setSize(200, 200);
		srcScrollPane.setViewportView(srcFilepathList);
		srcScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		//options checkboxes
		JCheckBox subfoldersCb = new JCheckBox("include subfolders");
		subfoldersCb.setAlignmentX(Component.RIGHT_ALIGNMENT); 
		
		//Buttons		
		JButton copyBtn = new JButton("Copy To");
		copyBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
		copyBtn.addActionListener(new changeScreenAction(ss));
		
		JButton findDupsBtn = new JButton("Find Duplicate Files");
		findDupsBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
		findDupsBtn.addActionListener(new changeScreenAction(ss));
		
		//set box layout for source path components
		JPanel srcPane = new JPanel();
		srcPane.setLayout(new BoxLayout(srcPane, BoxLayout.PAGE_AXIS));
		srcPane.add(srcLabel);
		srcPane.add(Box.createRigidArea(new Dimension(0,5)));
		srcPane.add(srcScrollPane);
		srcPane.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
		
		this.add(srcPane, BorderLayout.CENTER);
		
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
		
		buttonPane.add(copyBtn);		
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(subfoldersCb);	
		buttonPane.add(findDupsBtn);
		
		this.add(buttonPane, BorderLayout.PAGE_END);
	}

}