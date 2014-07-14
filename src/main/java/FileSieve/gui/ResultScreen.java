package FileSieve.gui;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * View for displaying found duplicate files
 * Allows to select duplicate files and either copy or delete them
 * Also allows to export duplicate files report
 * @author olgakaraseva
 */

public class ResultScreen extends JPanel{
	
    private Controller controller;
    JTree duplicatesList;
    CheckTreeManager checkTree;
    JLabel fileCntLabel;
    private static final int SELECT_LEVEL = 3; //only 3d level tree nodes can be selected
	
    ResultScreen(Controller cntrl){
		
	controller = cntrl;
	setLayout(new BorderLayout(10,10));	
		
        //Labels
	JLabel resultLabel = new JLabel();
	resultLabel.setText("Found duplicate files:");
	resultLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
	fileCntLabel = new JLabel();
	fileCntLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel instructionsLabel = new JLabel("(click on filename to open the file)");
        instructionsLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
			
	//Duplicate files list
        DefaultMutableTreeNode allfiles = new DefaultMutableTreeNode("Duplicate files");
	duplicatesList = new JTree(allfiles);
        //turn it into checkTree
        checkTree = new CheckTreeManager(duplicatesList, SELECT_LEVEL);
        	
	//scroll pane for duplicate files list
	JScrollPane resultScrollPane = new JScrollPane();
	resultScrollPane.setViewportView(duplicatesList);
	resultScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
		
	//Buttons
	JButton copyBtn = new JButton("Copy To");
	copyBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        copyBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.callCopyJob(checkTree.getSelectionPaths(), false);
            }
        });
		
	JButton deleteBtn = new JButton("Delete");
	deleteBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
        deleteBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.callDeleteJob(checkTree.getSelectionPaths());
            }
        });
		
	JButton reportBtn = new JButton("Export Report");
	reportBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
        reportBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //to do
            }
        });
		
	JButton newSearchBtn = new JButton("New Search");
	newSearchBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
	newSearchBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.changeScreen(e.getActionCommand());
            }
        });
	
        //add components to box layout
	JPanel titlePane = new JPanel();
	titlePane.setLayout(new BoxLayout(titlePane, BoxLayout.LINE_AXIS));
	titlePane.add(resultLabel);
	titlePane.add(Box.createHorizontalGlue());
        titlePane.add(instructionsLabel);
        titlePane.add(Box.createRigidArea(new Dimension(10,0)));
	titlePane.add(reportBtn);
	titlePane.add(newSearchBtn);
	titlePane.setAlignmentX(Component.LEFT_ALIGNMENT);
		
	JPanel resultsPane = new JPanel();
	resultsPane.setLayout(new BoxLayout(resultsPane, BoxLayout.PAGE_AXIS));
	resultsPane.add(titlePane);
	resultsPane.add(Box.createRigidArea(new Dimension(0,5)));
	resultsPane.add(resultScrollPane);
		
	JPanel totalsPane = new JPanel();
	totalsPane.setLayout(new BoxLayout(totalsPane, BoxLayout.LINE_AXIS));
	totalsPane.add(fileCntLabel);
	totalsPane.setAlignmentX(Component.LEFT_ALIGNMENT);
	resultsPane.add(totalsPane);	
	resultsPane.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        
	add(resultsPane, BorderLayout.CENTER);	
		
	JPanel buttonPane = new JPanel();
	buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
	buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));	
	buttonPane.add(copyBtn);
	buttonPane.add(deleteBtn);
		
	add(buttonPane, BorderLayout.PAGE_END);
		
    }

}