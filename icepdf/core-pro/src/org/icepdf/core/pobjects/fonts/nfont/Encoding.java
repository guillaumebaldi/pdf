package org.icepdf.core.pobjects.fonts.nfont;

import org.icepdf.core.pobjects.fonts.nfont.lang.Characters;
import org.icepdf.core.pobjects.fonts.nfont.lang.Strings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//import static org.icepdf.core.pobjects.fonts.nfont.NFont.NOTDEF;
//import static org.icepdf.core.pobjects.fonts.nfont.NFont.NOTDEF_CHAR;


/**
 * An encoding maps between character codes (numbers) and character names (strings).
 *
 * @author Copyright (c) 2003-2004  Thomas A. Phelps
 * @version $Revision: 1.2 $ $Date: 2005/09/09 03:17:36 $
 */
public class Encoding {

    protected static final Logger logger =
            Logger.getLogger(Encoding.class.toString());
    /**
     * Unicode predefined encoding.
     */
    public static final Encoding UNICODE;
    /**
     * Adobe Standard predefined encoding.
     */
    public static final Encoding ADOBE_STANDARD;
    //public static final Encoding ISO_LATIN1;
    /**
     * Mac Roman predefined encoding.
     */
    public static final Encoding MAC_ROMAN;
    /**
     * Mac Expert predefined encoding.
     */
    public static final Encoding MAC_EXPERT;
    /**
     * WinAnsi predefined encoding.
     */
    public static final Encoding WIN_ANSI;
    /**
     * PDF Doc predefined encoding.
     */
    public static final Encoding PDF_DOC;
    /**
     * Zapf Dingbats predefined encoding.
     */
    public static final Encoding ZAPF_DINGBATS;
    /**
     * Symbol predefined encoding.
     */
    public static final Encoding SYMBOL;
    /**
     * Identity predefined encoding, which maps characters to the glyph of the same number.
     */
    public static final Encoding IDENTITY;

    private static final String STANDARD_NAME = "Standard";
    private static final String MAC_ROMAN_NAME = "MacRoman";
    private static final String WIN_ANSI_NAME = "WinAnsi";
    private static final String PDF_DOC_NAME = "PDFDoc";

    static {
        UNICODE = new EncodingUnicode();
        UNICODE.toUni_ = CMap.IDENTITY;
        UNICODE.hasUni_ = true;    // should be done in EncodingUnicode, but private to this class

        // init static finals (=> on demand? but then not final)
        String[] stdmap = new String[256], macmap = new String[256], winmap = new String[256], pdfmap = new String[256];
        try {
            readEncodings("Latin", stdmap, macmap, winmap, pdfmap);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error reading encoding.", ioe);
        }
        // "In WinAnsiEncoding, all unused codes greater than 40 cMap to the bullet character.  However, only code 225 is specifically assigned to the bullet character; other codes are subject to future reassignment."
        winmap[127] = "bullet";    // for Distiller 4.0 for Windows
        ADOBE_STANDARD = new Encoding(STANDARD_NAME, stdmap);
        MAC_ROMAN = new Encoding(MAC_ROMAN_NAME, macmap);
        WIN_ANSI = new Encoding(WIN_ANSI_NAME, winmap);
        PDF_DOC = new Encoding(PDF_DOC_NAME, pdfmap);

        String[] exmap = new String[256];
        try {
            readEncodings("Expert", null, null, null, exmap);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error reading encoding.", ioe);
        }
        MAC_EXPERT = new Encoding("MacExpert", exmap);

        String[] symap = new String[256];
        try {
            readEncodings("Symbol", null, null, null, symap);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error reading encoding.", ioe);
        }
        SYMBOL = new Encoding("Symbol", symap);

        String[] zmap = new String[256];
        try {
            readEncodings("Zapf", null, null, null, zmap);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error reading encoding.", ioe);
        }
        ZAPF_DINGBATS = new Encoding("ZapfDingbats", zmap);

        IDENTITY = new Encoding("Identity", new String[256]/*256 for diffs*/);
        IDENTITY.toUni_ = CMap.IDENTITY;
        IDENTITY.hasUni_ = false;
    }

    private static void readEncodings(String path, String[] map1, String[] map2, String[] map3, String[] map4) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(Encoding.class.getResourceAsStream("/org/icepdf/core/pobjects/fonts/nfont/encoding/Encoding" + path + ".txt")));
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            if (line.startsWith("#")) continue;
            int i = 0, imax = line.length();
            while (line.charAt(i) != ' ') i++;
            String cname = Strings.valueOf(line.substring(0, i));
            i++;
            int num;
            char ch;
            if (map1 != null) {    // 4 or 1
                num = 0;
                while ((ch = line.charAt(i++)) != ' ')
                    if (ch != '-') num = num * 8 + ch - '0';
                map1[num] = cname;
                num = 0;
                while ((ch = line.charAt(i++)) != ' ')
                    if (ch != '-') num = num * 8 + ch - '0';
                map2[num] = cname;
                num = 0;
                while ((ch = line.charAt(i++)) != ' ')
                    if (ch != '-') num = num * 8 + ch - '0';
                map3[num] = cname;
            }
            num = 0;
            while (i < imax)
                if ((ch = line.charAt(i++)) != '-') num = num * 8 + ch - '0';
            map4[num] = cname;
        }
        r.close();

        if (map1 != null) map1[0] = map2[0] = map3[0] = NFont.NOTDEF;
        map4[0] = NFont.NOTDEF;
    }


    private String name_;
    private String[] map_;
    private CMap toUni_ = null;    // compute on demand -- maybe not used outside of PDF, or maybe PDF /ToUnicode
    private boolean hasUni_;
    private Encoding base_ = Encoding.IDENTITY;


    /**
     * Creates Encoding out of array of character names such that <code>cnames[ch] = cname</code>.
     */
    public Encoding(String name, String[] map) {
        //assert cMap!=null: name;
        if (map == null) {
            throw new IllegalArgumentException(name);
        }
        name_ = name;
        map_ = map;

        boolean hasUni = true;
        for (int i = 0, imax = map.length; i < imax; i++) {
            String cname = map[i];
            if (cname != null) {
                if (NFont.NOTDEF.equals(cname))
                    map[i] = NFont.NOTDEF;    // normalize while you're at it
                else if (hasUni && UNICODE.getChar(cname) == NFont.NOTDEF_CHAR)
                    hasUni = false; // X break; => collect all you can.  TeX has "suppress" in place of "space".
            }
        }
        // In WinAnsiEncoding, all unused codes greater than 40 map to the bullet character.
        // However, only code 225 shall be specifically assigned to the bullet character;
        // other codes are subject to future reassignment.
        if (WIN_ANSI_NAME.equals(name)){
            String bullet = "bullet";
            // 32 = 40 octal
            for (int i = 32, imax = map.length; i < imax; i++) {
                if (map[i] == null){
                    map[i] = bullet;
                }
            }
        }
        hasUni_ = hasUni;
    }

    /**
     * Constructs an encoding based on another with differences, as found in PDF.
     */
    public Encoding(Encoding base, Object[] diff) {
        if (base == null) base = IDENTITY;
        base_ = base;
        String[] map = base.map_;

        boolean hasUni = base.hasUni_;
        if (diff != null) {
            map = map.clone();

            for (int i = 0, imax = diff.length, j = 0; i < imax; i++) {
                Object o = diff[i];
                if (o instanceof Number)
                    j = ((Number) o).intValue();
                else {
//                    assert /*COS.CLASS_NAME*/String.class==o.getClass();
                    if (!(o instanceof String)) {
                        throw new IllegalStateException();
                    }
                    if (!NFont.NOTDEF.equals(o)) {
//                        map[j] = NFont.NOTDEF;
//                    }else {
                        String cname = (String) o;
                        map[j] = cname;
                        if (hasUni && UNICODE.getChar(cname) == NFont.NOTDEF_CHAR)
                            hasUni = false;
                    }
                    j++;
                }
            }
        }

        name_ = base.name_ + "+diffs";
        map_ = map;
        hasUni_ = hasUni;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0, max = map_.length; i < max; i++) {
            builder.append(i).append(" -> ").append(map_[i]).append("\n");
        }
        return builder.toString();
    }

    /**
     * Returns a predefined encoding by name, or <code>null</code> if no encoding by that name.
     * Valid names are: <code>Identity</code>, <code>MacRomanEncoding</code>, <code>MacExpertEncoding</code>, <code>WinAnsiEncoding</code>, <code>PDFDocEncoding</code>, <code>ZapfDingbatsEncoding</code>, <code>SymbolEncoding</code>.
     */
    public static Encoding getInstance(String name) {
        Encoding en;
        if (name == null || "Identity".equals(name))
            en = IDENTITY;
        else if (/*name.startsWith("Standard")?*/"StandardEncoding".equals(name))
            en = ADOBE_STANDARD;
            //else if ("ISOLatin1".equals(name)) en = ISO_LATIN1;
        else if ("MacRomanEncoding".equals(name))
            en = MAC_ROMAN;
        else if ("MacExpertEncoding".equals(name))
            en = MAC_EXPERT;
        else if ("WinAnsiEncoding".equals(name))
            en = WIN_ANSI;
        else if ("PDFDocEncoding".equals(name))
            en = PDF_DOC;
        else if ("ZapfDingbatsEncoding".equals(name))
            en = ZAPF_DINGBATS;
        else if ("SymbolEncoding".equals(name))
            en = SYMBOL;
        else
            en = null;

        return en;
    }

    /**
     * Returns name of encoding.
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns base encoding, if created as base plus diffs.
     */
    public Encoding getBase() {
        return base_;
    }


    /**
     * Returns character name in encoding.  For example, for many encodings <code>getName((char)65).equals("A")</code>.
     */
    public String getName(int c) {
        String cname = 0 <= c && c < map_.length ? map_[c] : null;
        return cname != null ? cname : NFont.NOTDEF;
    }

    /**
     * Returns character associated with <var>name</var>.
     */
    public char getChar(String name) {
        char ch = NFont.NOTDEF_CHAR;
        // simple check to see if we have a /Euro or /euro
        boolean isEuro = name.equalsIgnoreCase("euro");
        for (int i = 0, imax = map_.length; i < imax; i++) {
            if (name.equals(map_[i])) {
                ch = (char) i;
                break;
            }
            // if a euro then do the character name to ID mapping regardless
            // of case.
            else if (map_[i] != null
                    && isEuro
                    && name.equalsIgnoreCase(map_[i])) {
                ch = (char) i;
                break;
            }
        }
        return ch;
    }

    public char getDiffChar(char echar) {
        if (echar < map_.length) {
            String name = map_[echar];
            // check for /uniHEX, PDF-788.
            if (name != null && name.startsWith("uni")) {
                try {
                    return (char) Integer.parseInt(name.substring(3), 16);
                } catch (NumberFormatException e) {
                    // conder case for a few character names that start with "uni"
                    return UNICODE.getChar(name);
                }
            } else if (name != null) {
                // push the name back to unicode
                return UNICODE.getChar(name);
            }
        }
        return echar;
    }

    /**
     * Returns CMap that maps characters to another encoding's.
     */
    public CMap mapTo(Encoding to) {
        CMap cmap;
        if (to == this || Encoding.IDENTITY == this || Encoding.IDENTITY == to)
            cmap = CMap.IDENTITY;
        else {
            char[] c2c = new char[map_.length];
            for (int i = 0, imax = map_.length; i < imax; i++) {
                char ech = (char) i;
                String cname = 0 <= ech && ech <= map_.length ? map_[ech] : null;    // not getName(ech) because want to distinguish null and ".notdef"
                if (cname != null) {
                    if (cname.equals(to.getName(ech)))
                        c2c[i] = ech;    // fast path: both names same position
                    else
                        c2c[i] = to.getChar(cname);
                }
                // can have glyphs defined but no encoding to address it  and  encoding but no glyph
            }
            cmap = new CMap(c2c);
        }

        return cmap;
    }

    /**
     * Estimates the Unicode mapping based on matching glyph names with standard Unicode names.
     * The computation is an estimate because an encoding/nfont may use a Unicode name for a non-Unicode character
     * and because an encoding/nfont may use character names that do not have Unicode equivalents.
     * If there is no Unicode name for a character in the string, then that character maps to
     * {@link org.icepdf.core.pobjects.fonts.nfont.NFont#NOTDEF_CHAR}.
     * Java2D's string drawing interprets strings as Unicode.
     *
     * @see <a href='http://partners.adobe.com/asn/tech/type/unicodegn.jsp'>"Unicode and Glyph Names"</a>
     */
    public CMap guessToUnicode() {
        if (toUni_ == null) {
            char[] uni = new char[256];
            for (int i = 0; i < 256; i++)
                uni[i] = UNICODE.getChar(getName((char) i));

            boolean fid = true;
            for (int i = 0; i < 256; i++) {
                char ch = uni[i];
                if (ch != i && map_[i] != null && map_[i].equals(NFont.NOTDEF)) {
                    fid = false;
                    break;
                }
            }
            toUni_ = fid ? new CMap(uni) : CMap.IDENTITY;
        }
        return toUni_;
    }


    /**
     * Returns <code>true</code> iff encoding has full Unicode mappings, and so is invertible from this encoding to Unicode and back.
     */
    public boolean hasUnicode() {
        return hasUni_;
    }

    public boolean equals(Object o) {
        return o == this ||
                o instanceof Encoding &&
                        !(o == IDENTITY || o == UNICODE) &&
                        Arrays.equals(map_, ((Encoding) o).map_);
    }
}


class EncodingUnicode extends Encoding {
    static final Map<String, Character> CNAME2UNICODE = new HashMap<String, Character>(6000);//static final Map<String, /*String*/Character> CNAME2UNICODE = new HashMap<String, Character>(6000);
    static Map<Character, String> UNICODE2CNAME = null;//static Map<Character, String> UNICODE2CNAME = null;

    private static void readUnicode() throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(Encoding.class.getResourceAsStream("/org/icepdf/core/pobjects/fonts/nfont/encoding/glyphlist.txt")));
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            if (line.length() == 0 || line.startsWith("#")) continue;
            int inx = line.indexOf(';');
            //assert inx!=-1: line;
            if (inx == -1) {
                throw new IllegalStateException(line);
            }
            String cname = Strings.valueOf(line.substring(0, inx));
            String val = line.substring(inx + 1);
            inx = val.indexOf(' ');
            if (inx != -1) val = val.substring(0, inx);
            Character chobj = Characters.valueOf((char) Integer.parseInt(val, 16));
            CNAME2UNICODE.put(cname, chobj);
        }
        r.close();

        //assert CNAME2UNICODE.get("bullet").charValue() == '\u2022';
        if (CNAME2UNICODE.get("bullet") != '\u2022') {
            throw new IllegalStateException();
        }
    }

    /*package-private*/
    EncodingUnicode() {
        super("Unicode", new String[0]);

        try {
            readUnicode();
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error reading encoding.", ioe);
        }
    }

    /**
     * Returns Adobe name of codepoint, not Unicode Consortium name.
     */
    public String getName(char ch) {
        if (UNICODE2CNAME == null) {
            // UNICODE2CNAME = new HashMap<Character, String>(6000);
            UNICODE2CNAME = new HashMap<Character, String>(6000);
            for (Iterator i = CNAME2UNICODE.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry e = (Map.Entry) i.next();
                String cname = (String) e.getKey();
                Character chobj = (Character) e.getValue();
                if (UNICODE2CNAME.get(chobj) == null/*space vs spacehackarabic*/)
                    UNICODE2CNAME.put(chobj, cname);
            }
        }
        String o = UNICODE2CNAME.get(new Character(ch));
        return o != null ? o : NFont.NOTDEF;
    }

    public char getChar(String cname) {
        Character o = CNAME2UNICODE.get(cname);
        return o != null ? o : NFont.NOTDEF_CHAR;
    }

    public CMap mapTo(Encoding to) {
        return to.mapTo(this).reverse();
    }    // don't iterate over all of Unicode!

    public CMap guessToUnicode() {
        return CMap.IDENTITY;
    }
}
