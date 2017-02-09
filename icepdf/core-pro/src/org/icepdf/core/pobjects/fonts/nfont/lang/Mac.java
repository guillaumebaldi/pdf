package org.icepdf.core.pobjects.fonts.nfont.lang;


/**
 * Macintosh system conventions.
 * <p/>
 * <ul>
 * <li>4-byte tags: {@link #strTag(int)}, {@link #intTag(String)}
 * </ul>
 *
 * @version $Revision: 1.1 $ $Date: 2005/07/12 21:33:19 $
 */
public class Mac {
    private Mac() {
    }


    /**
     * Convert 4-byte <code>'abcd'</code> into {@link java.lang.String}.
     */
    public static String strTag(int itag) {
        char[] tag = new char[4];
        tag[0] = (char) ((itag >> 24) & 0xff);
        tag[1] = (char) ((itag >> 16) & 0xff);
        tag[2] = (char) ((itag >> 8) & 0xff);
        tag[3] = (char) (itag & 0xff);
        return new String(tag);
    }

    /**
     * Convert 4-character {@link java.lang.String} into 4-byte int.
     */
    public static int intTag(String stag) {
        //assert stag!=null;
        if (stag == null)
            throw new IllegalArgumentException();
        int tag = 0;
        int len = stag != null ? stag.length() : 0;
        for (int i = 0; i < 4; i++)
            tag = (tag << 8) | (i < len ? stag.charAt(i) : ' ');
        return tag;
    }
}
