package org.icepdf.core.pobjects.fonts.nfont.lang;


/**
 * Extensions to {@link java.lang.StringBuffer}, which is <code>final</code>.
 * <p/>
 * <ul>
 * <li>translate/convert/format:
 * {@link #getBytes8(CharSequence)} without character set encoding,
 * {@link #valueOf(byte[])}
 * </ul>
 *
 * @version $Revision: 1.1 $ $Date: 2005/07/12 21:33:19 $
 */
public class StringBuffers {
    private StringBuffers() {
    }

    /**
     * Converts low bytes from StringBuffer to byte[].  Java's String.getDecodedStreamByteArray() changes bytes vis-a-vis an encoding.
     */
    public static byte[] getBytes8(CharSequence sb) {
//        assert sb!=null;
        if (sb == null)
            throw new IllegalArgumentException();
        int len = sb.length();
        byte[] b = new byte[len];
        for (int i = 0; i < len; i++)
            b[i] = (byte) sb.charAt(i);
        return b;
    }

    /**
     * Converts from byte[] to StringBuffer.  Java's String.getDecodedStreamByteArray() changes bytes vis-a-vis an encoding.
     */
    public static StringBuilder valueOf(byte[] b) {
//        assert b!=null;
        if (b == null)
            throw new IllegalArgumentException();
        StringBuilder sb = new StringBuilder(b.length);
        for (int i = 0, imax = b.length; i < imax; i++)
            sb.append((char) (b[i] & 0xff));
        return sb;
    }
}
