package org.icepdf.core.pobjects.fonts.nfont;

import org.icepdf.core.pobjects.Name;
import org.icepdf.core.pobjects.fonts.nfont.doc.PostScript;
import org.icepdf.core.pobjects.fonts.nfont.lang.Arrayss;
import org.icepdf.core.pobjects.fonts.nfont.lang.Strings;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

//import static org.icepdf.core.pobjects.fonts.nfont.NFont.NOTDEF_CHAR;


/**
 * CMap as used in PDF and TrueType, with extensions for PDF ToUnicode.
 *
 * @author Copyright (c) 2003 - 2005  Thomas A. Phelps
 * @version $Revision: 1.5 $ $Date: 2008/02/26 17:48:36 $
 */
public class CMap implements org.icepdf.core.pobjects.fonts.CMap {
//    static final boolean DEBUG = !false;// && com.pt.Meta.DEVEL;

    private static final Logger logger =
            Logger.getLogger(CMap.class.toString());

    // cmap names
    public enum CMapNames {
        identity("Identity"),
        identityH("Identity-H"),
        identityV("Identity-V"),
        identityUTF16BE("Identity-UTF16-BE"),
        reverse("Reverse");

        private String name;

        CMapNames(String name) {
            this.name = name;
        }

        public boolean equals(String name) {
            return this.name.equals(name);
        }

        public boolean equals(Name type) {
            return name.equals(type.getName());
        }
    }

    /**
     * Identity predefined CMap, which maps a character to itself.
     */
    public static final CMap IDENTITY = new CMapIdentity();
    /**
     * Identity_H predefined CMap, which reads two bytes to make one character.
     */
    public static final CMap IDENTITY_H = new CMapIdentityH();
    /**
     * Identity_V predefined CMap, which reads two bytes to make one character.
     */
    public static final CMap IDENTITY_V = new CMapIdentityH();    // same function as IdentityH, but different instance
    /**
     * UNIMPLEMENTED.  Identity_UTF16BE predefined CMap.
     */
    public static final CMap IDENTITY_UTF16BE = new CMapIdentityUTF16BE();


//    private static final int[] FIRST_ZERO = {0};

    private static final Map<String, SoftReference> cache_ = new HashMap<String, SoftReference>(20);// Map<String,SoftReference<CMap>> cache_ = new HashMap<String,SoftReference<CMap>>(20);

    // ligature map to improve text extraction and search for these character codes.
    public static final int LIGATURE_MAP_SIZE = 64263;
    private static final String[] LIGATURE_MAP = new String[LIGATURE_MAP_SIZE];

    static {
        LIGATURE_MAP[7531] = "ue";
        LIGATURE_MAP[6425] = "ff";
        LIGATURE_MAP[64257] = "fi";
        LIGATURE_MAP[64258] = "fl";
        LIGATURE_MAP[64259] = "ffi";
        LIGATURE_MAP[64260] = "ffl";
        LIGATURE_MAP[64261] = "?t";
        LIGATURE_MAP[64262] = "st";
        LIGATURE_MAP[306] = "IJ";
        LIGATURE_MAP[307] = "ij";
        // actually a single character
//        LIGATURE_MAP[198] = "AE";
//        LIGATURE_MAP[230] = "ae";
        LIGATURE_MAP[152] = "OE";
        LIGATURE_MAP[153] = "oe";
    }

    /*
private String registry_;
private String ordering_;
private int supplement_;
*/
    //String name_;
    private char[][] toSel_;
    /**
     * Starting value of parallel fromSel_.
     */
    private int[] first_;
    private char[][] fromSel_;
    // fromSel_ can't handle char->String mappings, new placeholder to do so.
    private StringBuilder[] fromSelStr;
    private int[] firstfrom_;
    // sub map, some of the Uni maps actually point to a common CMap.
    private CMap usecmap_;
    private boolean[] onebyte_;
    // general test so see if one or two bytes are in the begincodespacerange
    // code space range can be used to determine if a code falls into a one or
    // two byte character length.
    private int[][] codeSpaceRange;
    private boolean oneByte;
    private boolean twoByte;
    private boolean mixedByte;

    /**
     * Create a CMap from an array such that <var>toSel</var>[<var>from</var>] = <var>to</var>.
     */
    public CMap(/*String registry, String ordering, int supplement,*/ char[] toSel) {
        this(new char[][]{toSel});
        oneByte = true;
        //assert toSel!=null;
        if (toSel == null) {
            throw new IllegalArgumentException("Char[] can not be null");
        }
    }

    public CMap(char[][] segs) {
        //assert segs != null;
        if (segs == null) {
            throw new IllegalArgumentException("Segs can not be null");
        }
        compact(segs);
    }

    /**
     * Constructor for special case subclasses.
     */
    /*package-private*/
    CMap() {
        //name_ = name;
    }

    /**
     * Create a CMap by parsing <var>in</var>.
     */
    public CMap(/*String registry, String ordering, int supplement,*/ InputStream in) throws IOException {
        this(null, in);
    }

    /**
     * Gets the ligature map we use to map single character codes to multiple charcters, for example
     * 'fi' becomes 'f' and 'i'.  The maps is has a length of LIGATURE_MAP_SIZE and once the list
     * is returned any value in the mapping can be altered.  Values can be nulled if the mapping
     * is not required.
     *
     * LIGATURE_MAP[7531] = "ue";
     * LIGATURE_MAP[6425] = "ff";
     * LIGATURE_MAP[64257] = "fi";
     * LIGATURE_MAP[64258] = "fl";
     * LIGATURE_MAP[64259] = "ffi";
     * LIGATURE_MAP[64260] = "ffl";
     * LIGATURE_MAP[64261] = "?t";
     * LIGATURE_MAP[64262] = "st";
     * LIGATURE_MAP[306] = "IJ";
     * LIGATURE_MAP[307] = "ij";
     * LIGATURE_MAP[152] = "OE";
     * LIGATURE_MAP[153] = "oe";
     *
     * @return list of ligature mappings, index key is the original character code.   For example
     * 64257 = fi.
     */
    public static String[] getLigatureMap() {
        return LIGATURE_MAP;
    }

    public boolean isOneByte() {
        return oneByte;
    }

    public boolean isTwoByte() {
        return twoByte;
    }

    public boolean isMixedByte() {
        return mixedByte;
    }

    public void applyBytes(CMap cMap) {
        oneByte = cMap.oneByte;
        twoByte = cMap.twoByte;
        mixedByte = cMap.mixedByte;
    }

    /**
     * Create a CMap by parsing <var>in</var>, and fall back to resolve undefined mappings in <var>base</var>.
     */
    public CMap(CMap base, InputStream in) throws IOException {
        usecmap_ = base;    // PDF has UseCMap in dictionary (rather than in stream?)
        char[][] segs = new char[256][];
        onebyte_ = new boolean[256];
        Arrays.fill(onebyte_, true);    // assume one-byte unless marked otherwise

        PushbackInputStream pis = new PushbackInputStream(in, 5);
        // parser stack
        Object[] s = new Object[768];
        int si = 0;    // PostScript stack, not just PDF operands
        PostScript.eatSpace(pis);

        for (Object o; (o = PostScript.readObject(pis)) != null; ) {
            Class cl = o.getClass();
            if (PostScript.CLASS_COMMENT == cl) {
            }    // LATER: collect header
            else if (PostScript.CLASS_NAME != cl) {
                s[si++] = o;
            } else if (((String) o).startsWith("/")) {
                s[si++] = ((String) o).substring(1);
            } else {    // executable name
                String op = (String) o;
                int newsi = 0;
                if ("begincodespacerange".equals(op)) {    // if not present assume all 2-byte
                    // <src1> <srcn>
                    // # bytes in source by length of specification.  dest always 2-byte?
                    int numberOfRanges = ((Number) s[si - 1]).intValue();
                    codeSpaceRange = new int[numberOfRanges][2];
                    for (int i = 0; i < numberOfRanges; i++) {
                        StringBuilder s1 = (StringBuilder) PostScript.readObject(pis), sn = (StringBuilder) PostScript.readObject(pis);
                        int len = s1.length();
                        if (len == 1) {
                            oneByte = true;
                        }
                        if (len == 2) {
                            twoByte = true;
                        }
                        //assert len==sn.length(): s1 + " vs " + sn;
                        if (len != sn.length()) {
                            throw new IllegalStateException(s1 + " vs " + sn);
                        }
                        if (len == 1) {
                            int start = s1.charAt(0);
                            int end = sn.charAt(0);
                            codeSpaceRange[i][0] = start;
                            codeSpaceRange[i][1] = end;
                            for (int j = start; j <= end; j++) {
                                onebyte_[j] = true;
                            }
                        } else if (len == 2) {
                            int start = (s1.charAt(0) << 8) | s1.charAt(1);
                            int end = (sn.charAt(0) << 8) | sn.charAt(1);
                            codeSpaceRange[i][0] = start;
                            codeSpaceRange[i][1] = end;
                            for (int j = s1.charAt(0), jmax = Math.min(sn.charAt(0), 255); j <= jmax; j++) {
                                onebyte_[j] = false;    // assume 2-byte
                            }
                        } else {
                            //assert len==3 && false: len;
                            if (len != 3 && false) {
                                throw new IllegalStateException(len + "");
                            }
                        }    // alert on 3-byte
                    }
                    // setup byte lengths for string paring
                    if (oneByte && twoByte) {
                        mixedByte = true;
                    }
                    PostScript.readObject(pis);    // "endcodespacerange"

                } else if ("beginbfchar".equals(op)) {
                    // <src> <dest>
                    for (int i = 0, imax = ((Number) s[si - 1]).intValue(); i < imax; i++) {
                        StringBuilder src = (StringBuilder) PostScript.readObject(pis), dest = (StringBuilder) PostScript.readObject(pis);
                        int s1 = s2i(src)[0];
                        char[] ds = s2c(dest);
                        //assert 1<=s1.length<=3 && d1.length==1;
                        if (ds.length == 1) {
                            int page = s1 >> 8;
                            if (segs[page] == null) segs[page] = new char[256];
                            segs[page][s1 & 0xff] = ds[0];
                        } else {
                            if (fromSelStr == null)
                                fromSelStr = new StringBuilder[256];
                            fromSelStr[s1 & 0xff] = new StringBuilder().append(ds);
                        }
                    }
                    PostScript.readObject(pis);    // "endbfchar"

                } else if ("beginbfrange".equals(op)) {
                    // imax represents the number of entries to expect, this value can sometimes be incorrect.
                    // so we might want to break out of the loop if the pattern doesn't match.
                    for (int i = 0, imax = ((Number) s[si - 1]).intValue(); i < imax; i++) {
                        Object obj1 = PostScript.readObject(pis);
                        Object obj2 = PostScript.readObject(pis);

                        // double check that we have src1 and srcn token
                        if (!(obj1 instanceof StringBuilder &&
                                obj2 instanceof StringBuilder)) {
                            break;
                        }

                        StringBuilder src1 = (StringBuilder) obj1;
                        StringBuilder srcn = (StringBuilder) obj2;

                        Object dest = PostScript.readObject(pis);
                        int s1 = s2i(src1)[0], sn = s2i(srcn)[0];
                        if (PostScript.CLASS_STRING == dest.getClass()) {    // <src1> <srcn> <dest>
                            char[] ds = s2c((StringBuilder) dest);
                            char d1 = 0;
                            if (ds.length > 0) {
                                d1 = ds[0];
                            }
                            for (; s1 <= sn; s1++, d1++) {    // LATER: respect "rectangular" and save some work and maybe memory
                                int page = s1 >> 8;
                                if (segs[page] == null)
                                    segs[page] = new char[256];
                                segs[page][s1 & 0xff] = d1;
                            }
                        }
                        // vector cmap type case <src1> <srcn> [<dest1> <dest2> ...]
                        else if ("[".equals(dest)) {
                            // read the rest of the range values that make up the vector.
                            // sn-s1 also define how many characters are in the map.
                            while (!"]".equals(o)) {
                                dest = PostScript.readObject(pis);
                                if (!(dest instanceof StringBuilder)) {
                                    break;
                                }
                                // convert dest to a hex and build up the page value
                                char[] ds = s2c((StringBuilder) dest);
                                if (fromSelStr == null) {
                                    fromSelStr = new StringBuilder[256];
                                }
                                fromSelStr[s1 & 0xff] = new StringBuilder().append(ds);
                                s1++;
                            }
                        } else {
                            if (logger.isLoggable(Level.SEVERE)) {
                                logger.severe("Error parsing CMAP file ");
                            }
                        }
                    }
                    PostScript.readObject(pis);    // "endbfrange"

                } else if ("begincidrange".equals(op)) {
                    // <src1> <srcn> <dest-in-decimal>
                    for (int i = 0, imax = ((Number) s[si - 1]).intValue(); i < imax; i++) {
                        StringBuilder src1 = (StringBuilder) PostScript.readObject(pis),
                                srcn = (StringBuilder) PostScript.readObject(pis);
                        Object dest = PostScript.readObject(pis);
                        int s1 = s2i(src1)[0], sn = s2i(srcn)[0];
                        char d1 = (char) ((Number) dest).intValue();
                        for (; s1 <= sn; s1++, d1++) {
                            int page = s1 >> 8;
                            if (segs[page] == null) segs[page] = new char[256];
                            segs[page][s1 & 0xff] = d1;
                        }
                    }
                    PostScript.readObject(pis);    // "endcidrange"

                } else if ("beginnotdefrange".equals(op)) {
                    // <src1> <srcn> notdef
                    for (int i = 0, imax = ((Number) s[si - 1]).intValue(); i < imax; i++) {
                        StringBuilder src1 = (StringBuilder) PostScript.readObject(pis), srcn = (StringBuilder) PostScript.readObject(pis);
                        Object dest = PostScript.readObject(pis);
                        int s1 = s2i(src1)[0], sn = s2i(srcn)[0];
                        char d1 = (char) ((Number) dest).intValue();
                    }
                    PostScript.readObject(pis);    // "endnotdefrange"

                } else if ("begincmap".equals(op)) {
                } else if ("endmap".equals(op)) {
                    break;    // stop and throw away dict

                } else if ("usecmap".equals(op)) {
//                    assert usecmap_ == null;
                    if (usecmap_ != null) {
                        throw new IllegalStateException();
                    }
                    usecmap_ = CMap.getInstance((String) s[si - 1]);

                    // copy over maps immediate encoding info needed for
                    // proper string decode of a StringObject.
                    oneByte = usecmap_.oneByte;
                    twoByte = usecmap_.twoByte;
                    mixedByte = usecmap_.mixedByte;
                    codeSpaceRange = usecmap_.codeSpaceRange;

                    // can be a list of font names, usually just one, or a matrix
                    // definition which can have 6 values.  But regardless we don't
                    // use this information currently,
                } else if ("[".equals(op)) {
                    List<Object> l = new ArrayList<Object>(10);    // just numbers in PostScript
                    int nest = 1;
                    int count = 0;
                    while (nest > 0 && count < 10) {
                        o = PostScript.readObject(pis);
                        if ("]".equals(o)) {
                            nest--;
                            if (nest > 0) {
                                l.add(o);
                            }
                        } else {
                            l.add(o);
                        }
                        count++;
                    }
                    s[si++] = l;
                    newsi = si;
                    //} else if ("dup".equals(op)) { s[si] = s[si-1]; si++;
                    //} else if ("pop".equals(op)) si--;
                    //} else if ("".equals(op)) {
                    //else if ("readonly".equals(op)) {	// don't clear stack
                    //	newsi = si;
                } // else ignore everything else

                si = newsi;
            }
        }

        pis.close();

        compact(segs);
    }

    /**
     * Converts string with 1-, 2-, or 3-bytes per character into int[] with one entry per character.
     */
    private int[] s2i(CharSequence s) {
        int[] ints = new int[s.length()];
        int inti = 0;
        for (int i = 0, imax = s.length(); i < imax; ) {
            int v = s.charAt(i++);
            if (onebyte_[v])
                ints[inti++] = v;
            else if (i == imax) {
                ints[inti++] = v;
                onebyte_[v] = true;
            }    // Distiller 6.0 produces codespacerange <0000> <ffff> but bfchar <00>.  seems like a bug
            else /*if two-byte*/
                ints[inti++] = (v << 8) | s.charAt(i++);
            //else false: s;
        }
        return Arrayss.resize(ints, inti);
    }

    // destination always 2-byte?
    private char[] s2c(CharSequence s) {
        //assert s!=null && s.length() % 2 == 0;
        if (s == null && s.length() % 2 != 0) {
            throw new IllegalArgumentException();
        }
        int len = s.length();
        char[] dest = new char[len / 2];
        for (int i = 0, j = 0; i < len - 1; i += 2, j++)
            dest[j] = (char) ((s.charAt(i) << 8) | s.charAt(i + 1));
        return dest;
    }

    /**
     * Returns a predefined CMap by <var>name</var>, such as "GBT-EUC-V".
     * Non-Identity maps are taken from files of the same name in <code>/com/adobe/CMap</code> relative to the JAR.
     */
    public static CMap getInstance(String name) {
        CMap cmap = null;
        SoftReference ref; //SoftReference<CMap> ref;
        InputStream in;

        if ("Identity".equals(name))
            cmap = IDENTITY;    // special cases
        else if ("Identity-H".equals(name))
            cmap = IDENTITY_H;
        else if ("Identity-V".equals(name))
            cmap = IDENTITY_V;
        else if ((ref = (SoftReference) cache_.get(name)) != null && (cmap = (CMap) ref.get()) != null) {
        }    // cached?
        else if ((in = CMap.class.getResourceAsStream("/org/icepdf/core/pobjects/fonts/nfont/cmap/" + name)) != null) {    // bundled in JAR?
            try {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Loading CMAP file " + name);
                }
                cmap = new CMap(in);
                //assert cmap.toSel_ != null: name;
                if (cmap.toSel_ == null) {
                    throw new IllegalStateException(name);
                }
                in.close();
            } catch (IOException e) {
                logger.log(Level.FINE, "Error getting cmap instance", e);
                cmap = CMap.IDENTITY;
            } catch (Throwable e) {
                logger.log(Level.FINE, "error reading cmap file " + name, e);
            }

            cache_.put(name, new SoftReference(cmap));//cache_.put(name, new SoftReference<CMap>(cmap));

            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Successfully loaded CMAP file " + name);
            }
        } else {
            // if (!fat version) then suggest fat version
            cmap = null;
        }
        return cmap;
    }

    /**
     * Returns new CMap with functions of {@link #toSelector(String)} and {@link #fromSelector(String)} reversed.
     */
    public CMap reverse() {
        return new CMapReverse(this);
    }

    /**
     * Compare the segments segs[i] looking for other segs that have the ranges
     * very subtle optimization for cmaps with more then one sequence definition.
     *
     * @param segs cmap sequence definitions.
     */
    private void compact(char[][] segs) {
        int segcnt = 0;
        for (int i = 0, imax = segs.length; i < imax; i++)
            if (segs[i] != null) segcnt++;

        //int spcnt = 0;
        char[][] s = new char[segcnt][];
        int[] first = new int[segcnt];
        for (int i = 0, si = 0; si < segcnt; i++)
            if (segs[i] != null) {
                char[] g = s[si] = segs[i];
                int glen = g.length;
                //assert g.length>=256; -- just as long as needed in Type 1 CID
                if (glen <= 3) {    // already short
                } else if (g[0] == g[1]) {    // all cMap to same glyph
                    char g0 = g[0];
                    boolean fsame = true;
                    for (int j = 0 + 2; j < glen; j++)
                        if (g[j] != g0) {
                            fsame = false;
                            break;
                        }
                    if (fsame) {
                        s[si] = new char[]{g0};
//                        spcnt++;
                    }
                } else if ((g[0] & 0xffff00) == (g[1] & 0xffff00) && g[1] - g[0] == 1) {    // same high byte and low byte at same delta
                    boolean fsame = true;
                    int high = g[0] & 0xffff00, lowbase = g[0];
                    for (int j = 2; j < glen; j++)
                        if ((g[j] & 0xffff00) != high || g[j] - lowbase != j) {
                            fsame = false;
                            break;
                        }
                    if (fsame) { /*s[si] = new char[] { high, lowbase };--conflict with 2-element usual case*/
//                        spcnt++;
                    }    //  ... have to update inverse too
                }

                first[si] = i << 8;
                si++;
            }

        toSel_ = s;
        first_ = first;
    }


    // functionality

    /**
     * Converts string from source bytes to characters.
     */
    public String toSelector(String str) {
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0, imax = str.length(); i < imax; i++) {
            char ch = str.charAt(i);
            if (onebyte_ != null && !onebyte_[ch] && i + 1 < imax) {
                ch = (char) ((ch << 8) | str.charAt(++i));
            }
            sb.append(toSelector(ch));
        }
        return sb.toString();
    }

    public char toSelector(char ch) {
        char toch = translate(ch, toSel_, first_);
        return toch != NFont.NOTDEF_CHAR || usecmap_ == null ? toch : usecmap_.toSelector(ch);
    }

    public char toSelector(char ch, boolean isCFF) {
        char toch = translate(ch, toSel_, first_);
        return toch != NFont.NOTDEF_CHAR || usecmap_ == null ? toch : usecmap_.toSelector(ch, isCFF);
    }

    public String toUnicode(char ch) {
        // check normal toSel_ range for a hit
        char toch = translate(ch, toSel_, first_);
        // if now match look in the fromSelStr match
        if (toch == NFont.NOTDEF_CHAR && fromSelStr != null) {
            StringBuilder toUnicode = fromSelStr[ch & 0xff];
            if (toUnicode != null) {
                return toUnicode.toString();
            }
        }
        // if no unicode then use either the toch or cmap diff as a return.
        if (toch != NFont.NOTDEF_CHAR || usecmap_ == null) {
            // check against our ligature map for a possible match.
            if (toch < LIGATURE_MAP_SIZE &&
                    LIGATURE_MAP[toch] != null) {
                return LIGATURE_MAP[toch];
            }
            return String.valueOf(toch);
        } else {
            ch = usecmap_.toSelector(ch);
            return String.valueOf(ch);
        }
    }

    public boolean isEmptyMapping(){
        return toSel_ == null || toSel_.length == 0;
    }

    private char translate(int c, char[][] toSel, int[] first) {
        char toch = NFont.NOTDEF_CHAR;
        for (int j = 0, jmax = toSel.length; j < jmax; j++) {    // => binary search on c&0xff00
            //if (c <= first[j] + toSel[j].length) {	// don't search on first_ (overlaps?).  Arrays.binarySearch?
            char[] to = toSel[j];
            int len = to.length;
            if (c < first[j] + len) {    // don't search on first_ (overlaps?).  Arrays.binarySearch?
                if (len == 1)
                    toch = to[0];    // special case: all cMap to same glyph
                else if (len == 2)
                    toch = (char) (j + to[0]);    // special case: delta
                else
                    toch = to[c & 0xff];
                // else PAGE_IDENTITY not a special case, though compactly represented by PAGE_IDENTITY
                break;
            }
        }
        return toch;
    }


    /**
     * Converts string from characters to source bytes.
     */
    public String fromSelector(String str) {
        invert(true);
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0, imax = str.length(); i < imax; i++) {
            char ch = fromSelector(str.charAt(i));
            if (onebyte_ != null && ((ch & 0xff00) != 0 || !onebyte_[ch]))
                sb.append((char) (ch >> 8)).append((char) (ch & 0xff));
            else
                sb.append(ch);
        }
        return sb.toString();
    }

    public char fromSelector(char ch) {
        return fromSelector(ch, false);
    }

    /**
     * Gets the code space range as defined begincodespacerange in a CMAP.  The
     * entries can be iderated over to dermind if a character code is likely
     * valid.
     *
     * @return code range values for this cmap. Can be null.
     */
    public int[][] getCodeSpaceRange() {
        return codeSpaceRange;
    }

    /**
     * cid to gid selector
     *
     * @param ch    character
     * @param isCFF is a compact font file
     * @return character translation.
     */
    public char fromSelector(char ch, boolean isCFF) {
        invert(isCFF);
        char toch = translate(ch, fromSel_, firstfrom_);
        return toch != NFont.NOTDEF_CHAR || usecmap_ == null ? toch : usecmap_.fromSelector(ch);
    }

    private void invert(boolean isCFF) {
        if (fromSel_ != null) return;    // already computed

        // seems to be causing more problems then good, sticking with
        // long path for consistency.
//        // fast path: all codes<256
//        if (toSel_.length == 1 && first_[0] == 0 && toSel_[0].length <= 256) {
//            char[] toSel = toSel_[0];
//            char[] fromSel = new char[256];
//            boolean ffast = true;
//            for (int i = 0, imax = toSel.length; i < imax; i++) {
//                char ch = toSel[i];
//                if (NFont.NOTDEF_CHAR == ch) continue;
//                if (ch < 256)
////                    fromSel[ch] = (char) i;
//                    fromSel[i] = (char) ch;
//                else {
//                    ffast = false;
//                    break;
//                }
//            }
//            if (ffast) {
//                fromSel_ = new char[][]{fromSel};
//                firstfrom_ = first_;
//                return;
//            }    // both int[] { 0 }
//            // else fall through
//        }

        // need to sniff out none zero entries, like above but with a look
        // head features.
        char[][] segs = new char[256][];
        int segcnt = 0;
        if (isCFF) {
            for (int i = 0, imax = toSel_.length; i < imax; i++) {
                int high = first_[i];
                char[] toSel = toSel_[i];
                // special cases: all cMap to same glyph OK just not 1-to-1, delta...
                for (int j = 0, jmax = toSel.length; j < jmax; j++) {
                    char ch = toSel[j];
                    if (ch == NFont.NOTDEF_CHAR) continue;
                    int topage = ch >> 8;
                    if (segs[topage] == null) {
                        segs[topage] = new char[256];
                        segcnt++;
                    }
                    segs[topage][ch & 0xff] = (char) (high | j);    // from[based on char] = based on position
                }
            }
        } else {
            for (int i = 0, imax = toSel_.length; i < imax; i++) {
                int high = first_[i];
                char[] toSel = toSel_[i];
                // special cases: all cMap to same glyph OK just not 1-to-1, delta...
                for (int j = 0, jmax = toSel.length; j < jmax; j++) {
                    char ch = toSel[j];
                    if (ch == NFont.NOTDEF_CHAR) continue;
                    int topage = j >> 8;
                    if (segs[topage] == null) {
                        segs[topage] = new char[256];
                        segcnt++;
                    }
                    segs[topage][j & 0xff] = (char) (high | ch);    // from[based on char] = based on position
                }
            }
        }

        // compact -- compact() sets toSel_ and first_
        char[][] s = new char[segcnt][];
        int[] first = new int[segcnt];
        for (int i = 0, si = 0; si < segcnt; i++)
            if (segs[i] != null) {
                s[si] = segs[i];
                first[si] = i << 8;
                si++;
            }
        fromSel_ = s;
        firstfrom_ = first;
    }
}


class CMapIdentity extends CMap {
    public String toSelector(String str) {
        return str;
    }

    public char toSelector(char ech) {
        return ech;
    }

    public char toSelector(char ech, boolean isCFF) {
        return ech;
    }

    public String fromSelector(String str) {
        return str;
    }

    public char fromSelector(char ech) {
        return ech;
    }

    public String toUnicode(char ech) {
        return String.valueOf(ech);
    }
}


class CMapIdentityH extends CMapIdentity {
    public String toSelector(String str) {
        //assert str!=null && str.length() % 2 == 0: str;
        if (!(str != null && str.length() % 2 == 0)) {
            throw new IllegalArgumentException(str);
        }
        int len = str.length();
        StringBuilder sb = new StringBuilder(len / 2);
        for (int i = 0; i < len; i += 2)
            sb.append((char) ((str.charAt(i) << 8) | str.charAt(i + 1)));
//System.out.println("IdentityH "+sb+"/"+sb.length()+" => "+sbout+"/"+sbout.length());
        return Strings.valueOf(sb);
    }

    public String fromSelector(String str) {
        //assert str!=null;
        if (str == null) {
            throw new IllegalArgumentException();
        }
        int len = str.length();
        StringBuilder sb = new StringBuilder(len * 2);
        for (int i = 0; i < len; i++)
            sb.append((char) (str.charAt(i) >> 8)).append((char) (str.charAt(i) & 0xff));
        return Strings.valueOf(sb);
    }
}


class CMapIdentityUTF16BE extends CMapIdentity {
    public String toSelector(String str) {
        //assert false;
        return str;
    }

    public String fromSelector(String str) {
        //assert false;
        return str;
    }
}

class CMapReverse extends CMap {
    private CMap base_;

    public CMapReverse(CMap base) {
        base_ = base;
    }

    public boolean isOneByte() {
        return base_.isOneByte();
    }

    public boolean isTwoByte() {
        return base_.isTwoByte();
    }

    public boolean isMixedByte() {
        return base_.isMixedByte();
    }

    public String toSelector(String str) {
        return base_.fromSelector(str);
    }

    public char toSelector(char ech) {
        return toSelector(ech, false);
    }

    public char toSelector(char ech, boolean isCFF) {
        return base_.fromSelector(ech, isCFF);
    }

    public String fromSelector(String str) {
        return base_.toSelector(str);
    }

    public char fromSelector(char ech) {
        return base_.toSelector(ech);
    }

    public String toUnicode(char ech) {
        return String.valueOf(base_.toSelector(ech));
    }
}
