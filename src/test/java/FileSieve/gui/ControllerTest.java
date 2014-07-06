
package FileSieve.gui;

import java.awt.Component;
import javax.swing.JTree;
import javax.swing.tree.TreePath;
import org.junit.Before;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static org.mockito.Mockito.*;

/**
 * 
 * @author olgakaraseva
 */
public class ControllerTest {
    
    private Controller controller;
    private ScreenSwitcher ss;
    
    @Before
    public void setup() {
        controller = new Controller();
        ss = new ScreenSwitcher(controller);
    }
    
    /**
     * Test that the screens switch correctly
     */
    @Test
    public void testChangeScreen(){
        testScreenChangedCorrectly(ScreenEnum.COPYPANEL);
        testScreenChangedCorrectly(ScreenEnum.RESULTPANEL);
        testScreenChangedCorrectly(ScreenEnum.SELECTPANEL);

    }
    
     /**
     * Test "Find Duplicate Files" changes screen correctly
     */
    @Test
    public void testCallDuplJob(){
        JTree testTree = mock(JTree.class);
        TreePath[] paths = {new TreePath(new Object[] {"tmp", "foo", "bar"})};
        when(testTree.getSelectionPaths()).thenReturn(paths);
        testScreenChangedCorrectly(ScreenEnum.RESULTPANEL);
    }
    
     /**
     * Test "Copy To" changes screen correctly
     */
    @Test
    public void testCallCopyJob(){
        JTree testTree = mock(JTree.class);
        TreePath[] paths = {new TreePath(new Object[] {"tmp", "foo", "bar"})};
        when(testTree.getSelectionPaths()).thenReturn(paths);
        testScreenChangedCorrectly(ScreenEnum.COPYPANEL);
    }
    
    
    private void testScreenChangedCorrectly(ScreenEnum val){
        controller.changeScreen(val.btnText());
        for (Component comp : ss.screens.getComponents() ) {
            if (comp.isVisible()) {
               assertTrue("Select Screen changed correctly", comp.getClass().getName().equals(val.screenClass().getName()));
            }
        }
    }
}
