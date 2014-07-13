package FileSieve.gui.util;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Contains methods used by many classes to convert operational 
 * formats to readable strings
 * @author olgakaraseva
 */
public class Utilities {
    
    /** 
     * Converts file size to readable format, i.e. B, Kb, Mb, Gb, Tb
     * @param size          file size as long type
     * @return              readable file size as String
     */
    public static String readableFileSize(long size) {
        if(size <= 0) return "0";
        final String[] units = new String[] { "B", "Kb", "Mb", "Gb", "Tb" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
    
    /**
     * Converts timestamp of type long to readable date string
     * @param dateToModify      timestamp that should be converted
     * @return                  readable date string
     */
    public static String readableDate(long dateToModify){
        Date date=new Date(dateToModify);
        SimpleDateFormat df2 = new SimpleDateFormat("MM/dd/yy H:mm");
        String dateText = df2.format(date);
        return dateText;
    }
}
