package FileSieve;

import FileSieve.gui.Controller;
import FileSieve.gui.ScreenSwitcher;
import javax.swing.SwingUtilities;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Main class which runs the application
 * @author olgakaraseva
 */
public class FileSieve {

    public static void main(String[] args) {
        //printClassPath();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() { 
                Controller controller = new Controller();
                ScreenSwitcher view = new ScreenSwitcher(controller); 
            }
        });  
    }

    /**
     * Utility method, for use in troubleshooting only, for printing out items on the classpath
     */
    private static void printClassPath() {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        URL[] urls = ((URLClassLoader)cl).getURLs();
        for(URL url: urls){
            System.out.println(url.getFile());
        }
    }

}
