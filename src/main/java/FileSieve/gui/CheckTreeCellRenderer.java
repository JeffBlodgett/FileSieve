package FileSieve.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

/**
 * Adds checkboxes for JTree leaves
 * Displays filesize and last modified timestamp for all leaves
 * 
 * @author olgakaraseva
 */
public class CheckTreeCellRenderer extends JPanel implements TreeCellRenderer{ 
    private DefaultTreeSelectionModel selectionModel; 
    private TreeCellRenderer delegate; 
    private JCheckBox checkBox = new JCheckBox(); 
 
    public CheckTreeCellRenderer(TreeCellRenderer delegate, DefaultTreeSelectionModel selectionModel){ 
        this.delegate = delegate; 
        this.selectionModel = selectionModel; 
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        setOpaque(false); 
        checkBox.setOpaque(false); 
    } 
 
    /**
     * Renders the tree nodes
     * @param tree                  The tree in which this renderer component is used currently
     * @param value                 The value to be displayed for the tree cell to be rendered
     * @param selected              Whether the tree cell to be rendered is selected
     * @param expanded              Whether the tree cell to be rendered is expanded
     * @param leaf                  Whether the tree cell to be rendered is a leaf
     * @param row                   The row index of the tree cell to be rendered
     * @param hasFocus              Whether the tree cell to be rendered has the focus
     * @return                      modified tree cell
     */
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, 
                     boolean expanded, boolean leaf, int row, boolean hasFocus){ 
        
        Component renderer = delegate.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus); 
        
        //take care of the checkbox state based on whether the path is selcted
        TreePath path = tree.getPathForRow(row); 
        if(path!=null){ 
            if(selectionModel.isPathSelected(path)){ 
                checkBox.setSelected(true);
            } else { 
                checkBox.setSelected(false); 
            }
        }
        removeAll();
        
        //setup the labels
        JLabel filename = new JLabel(value.toString());
        Dimension d = filename.getPreferredSize(); 
        filename.setMaximumSize(new Dimension(600,d.height));
        filename.setPreferredSize(new Dimension(600,d.height));
        
        JLabel fileSize = new JLabel();
        fileSize.setMaximumSize(new Dimension(100, d.height));
        fileSize.setPreferredSize(new Dimension(100, d.height));
        
        JLabel lastModified = new JLabel();
        
        //for leaves add checkbox and show filepath, filesize and last modified values
        if(leaf){
            add(checkBox);     
            File file = new File(value.toString());
            fileSize.setText(FileSieve.gui.util.Utilities.readableFileSize(file.length()));          
            lastModified.setText(FileSieve.gui.util.Utilities.readableDate(file.lastModified()));    
        //for folders show filename and labels for filesize and last modified fields
        } else {
            // set bold font
            Font font = filename.getFont();
            Font boldFont = new Font(font.getFontName(), Font.BOLD, font.getSize());
            filename.setFont(boldFont);
            //adjust the filename label width since it doesn't have checkbox and depth offset
            filename.setMaximumSize(new Dimension(600+checkBox.getWidth()+20,d.height));
            filename.setPreferredSize(new Dimension(600+checkBox.getWidth()+20,d.height)); 
            
            fileSize.setText("Filesize");    
            lastModified.setText("Last modified");
        } 
        
        add(filename);
        add(fileSize);
        add(lastModified);
          
        return this;
        
    } //getTreeCellRendererComponent
} 
