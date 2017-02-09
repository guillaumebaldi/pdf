package org.icepdf.core.pobjects.fonts.nfont.lang;


/**
 * Extensions to {@link java.lang.Character}.
 * <p/>
 * <ul>
 * <li>{@link #isHexDigit(int)}
 * </ul>
 *
 * @version $Revision: 1.1 $ $Date: 2005/07/12 21:33:19 $
 */
public class Characters {
    private static Character[] SHARED = new Character[0x100];

    static {
        for (int i = 0, imax = SHARED.length; i < imax; i++)
            SHARED[i] = new Character((char) i);
    }


    private Characters() {
    }


    /**
     * Returns <code>true</code> iff <var>ch</var> is in [0-9a-fA-F].
     */
    public static boolean isHexDigit(int ch) {
        return ('0' <= ch && ch <= '9') || ('a' <= ch && ch <= 'f') || ('A' <= ch && ch <= 'F');
    }

    public static Character valueOf(char ch) {
        return ch < SHARED.length ? SHARED[ch] : new Character(ch);
    }
}
