
package FileSieve;

import FileSieve.gui.Controller;
import FileSieve.gui.ScreenSwitcher;
import javax.swing.SwingUtilities;

/**
 * Main class which runs the application
 * @author olgakaraseva
 */
public class FileSieve {
    public static void main(String[] args) {           
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() { 
                Controller controller = new Controller();
                ScreenSwitcher view = new ScreenSwitcher(controller); 
            }
        });  
    }
}
