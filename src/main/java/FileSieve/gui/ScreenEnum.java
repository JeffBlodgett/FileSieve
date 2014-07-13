package FileSieve.gui;

/**
 * Enumeration for 3 screens used by ScreenSwitcher and their related info:
 * which button calls the screen and the relevant screen class
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
    
    /**
     * Checks whether enum contains a given member
     * @param aName             button text associated with enum
     * @return                  true if is member, false otherwise
     */
    public static boolean isMember(String aName) {
       ScreenEnum[] screenEnums = ScreenEnum.values();
       for (ScreenEnum screen : screenEnums){
           if (screen.btnText.equals(aName)){
               return true;
           }
       }
       return false;
   }
}