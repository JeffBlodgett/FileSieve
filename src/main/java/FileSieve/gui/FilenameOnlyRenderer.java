package FileSieve.gui;

import java.awt.Component;
import java.io.File;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * Displays just file names instead of full paths
 * @author olgakaraseva
 */
class FilenameOnlyRenderer extends DefaultTreeCellRenderer {

    @Override
      public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, 
                        boolean expanded, boolean leaf, int row, boolean hasFocus) {
        
            File f = (File)value;
            String text = f.getName();
            return super.getTreeCellRendererComponent(tree, text, selected, expanded, leaf, row, hasFocus);
      }
    
}
