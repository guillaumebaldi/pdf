package org.icepdf.core.pobjects.fonts.nfont;


//import static org.icepdf.core.pobjects.fonts.nfont.NFont.*;


/**
 * Placeholder for possible nfont manager, which satisfies {@link #getInstance(String, int, int, float)}.
 */
public class NFontManager {
    private static NFontManager def_ = new NFontManager();

    public static NFontManager getDefault() {
        return def_;
    }

    public static String guessFamily(String name) {
        String fam = name;
        int inx;
        if ((inx = fam.indexOf(',')) > 0)
            fam = fam.substring(0, inx);    // "Arial,BoldItalic"
        if ((inx = fam.lastIndexOf('-')) > 0)
            fam = fam.substring(0, inx);    // "Times-Bold", but careful for corporate prefix
        // corporate ID...
        return fam;
    }

    public static int guessWeight(String name) {
        return name.indexOf("Bold") != -1 ? NFont.WEIGHT_BOLD : NFont.WEIGHT_NORMAL;
    }

    public static int guessRight(String txt) {
        return NFont.RIGHT_RESTRICTED | NFont.RIGHT_HEURISTIC;
    }

    /* possible functionality
public String[] getAvailableNames() {
    return new String[0];
}

public String[] getAvailableFamilies() {
    return new String[0];
}
*/

    public NFont getInstance(String family, int weight, int flags, float size) {
        return null;
    }
}
