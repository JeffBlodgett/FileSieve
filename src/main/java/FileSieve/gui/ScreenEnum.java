package FileSieve.gui;



/**
 * Enumeration for 3 app screens and their related info:
 * which button calls the screen and the screen class
 * @author olgakaraseva
 */
public enum ScreenEnum {
    SELECTPANEL("New Search", SelectScreen.class),
    RESULTPANEL("Find Duplicate Files", ResultScreen.class),
    COPYPANEL("Copy To", CopyScreen.class);
    
    private final String btnText;
    private final Class screenClass;
    // Constructor
    ScreenEnum(String btnText, Class screen){
        this.btnText = btnText;
        this.screenClass = screen;
    }
    
    // Getters
    public String btnText() { return btnText; }
    public Class screenClass() { return screenClass; }
}
