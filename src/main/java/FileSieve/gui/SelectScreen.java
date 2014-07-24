package FileSieve.gui;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeSelectionModel;

/**
 * View for selecting source and target filepaths
 * Uses FileTreeModel to populate the list source folders
 * Allows to select only folders both as sources and as target
 * @author olgakaraseva
 */

public class SelectScreen extends JPanel{
	
        private Controller controller;
        private JTree srcFilepathTree;
        private JCheckBox subfoldersCb;
	
	SelectScreen(Controller cntrl){
            
            controller = cntrl;
            setLayout(new BorderLayout(10,10));	
		
            //Select Source Filepath label
            JLabel srcLabel = new JLabel();
            srcLabel.setText("Select Folders:");
            srcLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            //instructions label
            JLabel instructLabel1 = new JLabel("To select multiple folders use SHIFT for consecutive"); 
            instructLabel1.setAlignmentX(Component.RIGHT_ALIGNMENT);
            JLabel instructLabel2 = new JLabel("and CNTRL for non-consecutive selection"); 
            instructLabel2.setAlignmentX(Component.RIGHT_ALIGNMENT);
		
            //Source Filepath tree
            TreeModel model = new FileTreeModel(new File(System.getProperty("user.home")));
            srcFilepathTree = new JTree(model);
            srcFilepathTree.setCellRenderer(new FilenameOnlyRenderer()); 
            srcFilepathTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		
            //Scroll pane for Filepath tree
            JScrollPane srcScrollPane = new JScrollPane();
            srcScrollPane.setSize(200, 200);
            srcScrollPane.setViewportView(srcFilepathTree);
            srcScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            //options checkboxes
            subfoldersCb = new JCheckBox("include subfolders");
            subfoldersCb.setAlignmentX(Component.LEFT_ALIGNMENT); 
            subfoldersCb.setSelected(true);
		
            //Buttons		
            JButton copyBtn = new JButton("Copy To");
            copyBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            copyBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    controller.callCopyJob(srcFilepathTree.getSelectionPaths(), subfoldersCb.isSelected());
                }
            });

		
            JButton findDupsBtn = new JButton("Find Duplicate Files");
            findDupsBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            findDupsBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    controller.callDuplJob(srcFilepathTree.getSelectionPaths(), subfoldersCb.isSelected());
                }
            });
		
            //set box layout and add components
            JPanel srcPane = new JPanel();
            srcPane.setLayout(new BoxLayout(srcPane, BoxLayout.PAGE_AXIS));
            srcPane.add(srcLabel);
            srcPane.add(Box.createRigidArea(new Dimension(0,5)));
            srcPane.add(srcScrollPane);
            srcPane.setBorder(BorderFactory.createEmptyBorder(20,20,0,20));
		
            add(srcPane, BorderLayout.CENTER);
		
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));		
            buttonPane.add(copyBtn);			
            buttonPane.add(findDupsBtn);
            buttonPane.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            JPanel bottomPane = new JPanel();
            bottomPane.setLayout(new BoxLayout(bottomPane, BoxLayout.LINE_AXIS));
            
            JPanel bottomLeftPane = new JPanel();
            bottomLeftPane.setLayout(new BoxLayout(bottomLeftPane, BoxLayout.PAGE_AXIS));
            bottomLeftPane.add(subfoldersCb);
            bottomLeftPane.add(buttonPane);
            bottomLeftPane.setAlignmentY(Component.TOP_ALIGNMENT);
            bottomLeftPane.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            
            JPanel bottomRightPane = new JPanel();
            bottomRightPane.setLayout(new BoxLayout(bottomRightPane, BoxLayout.PAGE_AXIS));
            bottomRightPane.add(instructLabel1);
            bottomRightPane.add(instructLabel2);
            bottomRightPane.setAlignmentY(Component.TOP_ALIGNMENT);
            bottomRightPane.setAlignmentX(Component.RIGHT_ALIGNMENT);
            
            bottomPane.add(bottomLeftPane);
            bottomPane.add(Box.createHorizontalGlue());
            bottomPane.add(bottomRightPane);
            bottomPane.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
		
            add(bottomPane, BorderLayout.PAGE_END);
	}

}