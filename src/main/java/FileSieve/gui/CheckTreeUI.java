package FileSieve.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreePath;

/**
 * Extended BasicTreeUI - grey background and bold text on parent nodes,
 * white and regular text on child nodes (leaves)
 * @author olgakaraseva
 */
public class CheckTreeUI extends BasicTreeUI {
    
    @Override
    protected void paintRow( Graphics g, Rectangle clipBounds, Insets insets, Rectangle bounds,
        TreePath path, int row, boolean isExpanded, boolean hasBeenExpanded, boolean isLeaf ){
        
        Graphics g2 = g.create();

        if(isLeaf){
            g2.setColor( Color.WHITE );
        } else {
            g2.setColor( new Color( 230, 230, 230 ) );
        }
        
        g2.fillRect( 0, bounds.y, tree.getWidth(), bounds.height );
        g2.dispose();

        super.paintRow(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
    }
    
    /**
     * Sets depth offset so renderer will place labels correctly
     * @param i         offset for children in pixels
     */
    public void updateDepthOffset(int i) {
        depthOffset = i;
    }
    
}