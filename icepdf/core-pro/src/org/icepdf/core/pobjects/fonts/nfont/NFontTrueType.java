package org.icepdf.core.pobjects.fonts.nfont;

import org.icepdf.core.pobjects.fonts.FontFile;
import org.icepdf.core.pobjects.fonts.nfont.instructions.Cvt;
import org.icepdf.core.pobjects.fonts.nfont.instructions.GraphicsState;
import org.icepdf.core.pobjects.fonts.nfont.instructions.Interpreter;
import org.icepdf.core.pobjects.fonts.nfont.instructions.Maxp;
import org.icepdf.core.pobjects.fonts.nfont.io.Rez;
import org.icepdf.core.pobjects.fonts.nfont.lang.Integers;
import org.icepdf.core.pobjects.fonts.nfont.lang.Mac;

import java.awt.*;
import java.awt.font.OpenType;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * TrueType outline fonts (<code>.ttf</code>, OS X <code>.dfont</code>, Microsoft <code>.ttc</code> TrueType Collection).
 * Does not execute the hinting instructions, which requires a license from Apple.
 * Any bitmap glyphs, which are largely obsolete, are not supported; if the nfont contains only bitmaps, as indicated by the subtype, the user should not try to use the nfont.
 * <p/>
 * <h2>Implementation Notes</h2>
 * <ul>
 * <li>Rendering/rasterization is done the same way as in <a href='NFontType1.html#imp'>Type 1</a>.
 * <li>For the common case of smaller nfont sizes, glyphs are cached to bitmaps,
 * and bitmaps are bitblit'ted (low-level high-performance copying) to the screen.
 * </ul>
 * <p/>
 * <p/>
 * <p>Table support
 * <!--
 * optional (6): 'cvt ', 'fpgm', 'hdmx', 'kern', 'OS/2', 'prep'
 * OpenType requires 'OS/2' also, but not 'glyf' or 'loca'
 * -->
 * <ul>
 * <li>implemented tables
 * <ul>
 * <li>all required: head, maxp, cmap, loca, hhea, hmtx, glyf, name, post
 * <li>optional: OS/2
 * </ul>
 * <li>ignored tables
 * <ul>
 * <li>related to instructions and grid-fitting: cvt, fpgm, prep, gasp
 * <li>precomputed caches: hdmx, vdmx, LTSH
 * <li>related to bitmaps: bdat, bhed, bloc, EBDT, EBLC, EBSC
 * <li>obsolete Multiple Master (OpenType 1.3 discontinued support): fvar, MMSD, MMFX
 * <!--li>superceded: mort (by morx) can't ignore because nfont may have only mort -->
 * <li><a href='http://www.microsoft.com/typography/tools/vtt.htm'>Visual TrueType</a> private tables: TSI0, TSI1, TSI2, TSI3, TSI4, TSI5
 * <li><a href='http://www.microsoft.com/typography/developers/volt/default.htm'>VOLT</a> private tables: TSIB, TSID, TSIJ, TSIP, TSIS, TSIV
 * <li>undocumented: umif, ...
 * <li>other: PCLT, ...
 * </ul>
 * </ul>
 * <p/>
 * <!--
 * Where code similar between NFontTrueType and NFontType1, comments found here, once rather than duplicated.
 * -->
 *
 * @author Copyright (c) 2003 - 2005  Thomas A. Phelps.  All rights reserved.
 * @version $Revision: 1.5 $ $Date: 2008/10/24 19:44:52 $
 */
public class NFontTrueType extends NFontSfnt implements /*java.awt.nfont.OpenType,*/ Cloneable {

    private static final Logger logger =
            Logger.getLogger(NFontTrueType.class.toString());

    public static final String FORMAT = "TrueType";
    /**
     * Macintosh OS X data-fork nfont (<code>.dfont</code> suffix).
     */
    public static final String SUBFORMAT_DFONT = "dfont";
    /**
     * TrueType Collection (<code>.ttc</code> suffix).
     */
    public static final String SUBFORMAT_TTC = "TTC";
    /**
     * Font does not have any outline glyphs, only bitmaps.  (Rendering not supported.)
     */
    public static final String SUBFORMAT_BITMAP = "bitmap";

    /**
     * Platform ID for Unicode.
     */
    public static final int PID_UNICODE = 0;
    /**
     * Platform ID for Macintosh.
     */
    public static final int PID_MACINTOSH = 1;
    //public static final int reserved=2; -- was ISO, but same as Unicode*/
    /**
     * Platform ID for Microsoft / Windows.
     */
    public static final int PID_MICROSOFT = 3;

    private static final GeneralPath GLYPH_ZERO_CONTOUR = new GeneralPath(GeneralPath.WIND_NON_ZERO, 1);

    private static final String[] MAP_MAC_STANDARD = {// not the same as Encoding.MAC_ROMAN
            ".notdef", ".null", "nonmarkingreturn", "space",
            "exclam", "quotedbl", "numbersign", "dollar", "percent", "ampersand", "quotesingle", "parenleft", "parenright", "asterisk", "plus", "comma", "hyphen", "period", "slash",
            "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
            "colon", "semicolon", "less", "equal", "greater", "question", "at",
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
            "bracketleft", "backslash", "bracketright", "asciicircum", "underscore", "grave",
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
            "braceleft", "bar", "braceright", "asciitilde", "Adieresis", "Aring", "Ccedilla", "Eacute", "Ntilde", "Odieresis", "Udieresis",
            "aacute", "agrave", "acircumflex", "adieresis", "atilde", "aring", "ccedilla", "eacute", "egrave", "ecircumflex", "edieresis", "iacute", "igrave", "icircumflex", "idieresis",
            "ntilde", "oacute", "ograve", "ocircumflex", "odieresis", "otilde", "uacute", "ugrave", "ucircumflex", "udieresis",
            "dagger", "degree", "cent", "sterling", "section", "bullet", "paragraph", "germandbls", "registered", "copyright", "trademark",
            "acute", "dieresis", "notequal", "AE", "Oslash", "infinity", "plusminus", "lessequal", "greaterequal", "yen",
            "mu", "partialdiff", "summation", "product", "pi", "integral", "ordfeminine", "ordmasculine", "Omega",
            "ae", "oslash", "questiondown", "exclamdown", "logicalnot", "radical", "florin", "approxequal", "Delta", "guillemotleft", "guillemotright", "ellipsis", "nonbreakingspace",
            "Agrave", "Atilde", "Otilde", "OE", "oe", "endash", "emdash", "quotedblleft", "quotedblright", "quoteleft", "quoteright", "divide", "lozenge", "ydieresis", "Ydieresis",
            "fraction", "currency", "guilsinglleft", "guilsinglright", "fi", "fl", "daggerdbl", "periodcentered", "quotesinglbase", "quotedblbase", "perthousand",
            "Acircumflex", "Ecircumflex", "Aacute", "Edieresis", "Egrave", "Iacute", "Icircumflex", "Idieresis", "Igrave", "Oacute", "Ocircumflex", "apple", "Ograve", "Uacute", "Ucircumflex", "Ugrave",
            "dotlessi", "circumflex", "tilde", "macron", "breve", "dotaccent", "ring", "cedilla", "hungarumlaut", "ogonek", "caron", "Lslash", "lslash", "Scaron", "scaron", "Zcaron", "zcaron",
            "brokenbar", "Eth", "eth", "Yacute", "yacute", "Thorn", "thorn", "minus", "multiply",
            "onesuperior", "twosuperior", "threesuperior", "onehalf", "onequarter", "threequarters", "franc", "Gbreve", "gbreve", "Idotaccent", "Scedilla", "scedilla", "Cacute", "cacute", "Ccaron", "ccaron", "dcroat"
    };
    private static final Encoding ENCODING_MAC_STANDARD = new Encoding("TrueType-1", MAP_MAC_STANDARD);


    /**
     * List of tags that {toByteArray()} rewrites.
     */
//    private static final int[] TAGS_KNOWN = {OpenType.TAG_LOCA};
    /**
     * List of tags that do not reference glyph numbers.
     */
//    private static final int[] TAGS_NO_GLYF = {OpenType.TAG_NAME, OpenType.TAG_DSIG};

    private static final int RGB_MASK = 0x00ffffff, ALPHA_MASK = 0xff000000;

    public static final Color COLOR_TRANSPARENT = new Color(0, 0, 0, 0);

    public boolean hinting;

    // multiple fonts in same file, from .dfont or TTC
    private NFontTrueType[] urs_;
    private long[] ttfoffset_;
    //private long[] ttflength_; -- not used, always read only as much as needed

    /*package-private*/
    NFontTrueType ur_;
    /**
     * Offset to data within RandomAccess.
     */
    private String subformat_;
    /**
     * If set, resolve different interpretations as Apple would.
     * private boolean fApple_;
     */
    private boolean fdfont_;

    private int ascent_, descent_;
    //    private int numGlyphs_;
//    private int maxPoints_;
//    private int maxContours_;
//    private int maxComponentPoints_;
//    private int maxComponentContours_;
//    private int maxZones_;
//    private int maxTwilightPoints_;
//    private int maxStorage_;
//    private int maxFunctionDefs_;
//    private int maxInstructionDefs_;
//    private int maxStackElements_;
    private int glyphcnt_;
    private int fontDirectionHint_;
    private int indexToLocFormat_;
    private int[] loca_;
    private String[] names_;
    private boolean[] subset_;
    private AffineTransform u_;    // concatenation of intrinsic matrix, applied matrix, and scale -- done once

    // X instruction execution => covered by Apple patent
    /* * Control Value Table, values referenced by instructions. */
//    private int[] cvt_;   Replaced with a class holding more information
    /**
     * Control value program ('prep'), executed whenever nfont, size, xform change.
     */
    private int[] cvp_;
    /**
     * Font Program, run when nfont is first used.
     */
    private int[] fpgm_;    // control value program ('prep') executed whenever nfont, size, xform change

    private Cvt mCvtTable;
    private Maxp mMaxpTable;

    // caches
    /**
     * In non-CID, mapping from all the way from encoded character through encoding to glyph.
     * In CID, mapping from CID to GID.
     */
    protected CMap c2g_;
    /*package-private*/
    float FUnit_;
    private int macStyle_;
    private int rights_ = RIGHT_UNKNOWN;
    private int hint_ = -1;
    private SoftReference/*<Shape>*/[] paths_;

    /**
     * Advance widths, indexed by glyph ID.
     */
    protected float[] widths_;
    /**
     * left side bearings, indexed by glyph ID.
     */
    protected float[] lsb_;
    private Shape notdef_ = null;//GLYPH_ZERO_CONTOUR;
    private int flags_;
    private int weight_;
    private int spacech_ = Integer.MIN_VALUE;
    private String name_, family_;

    // bitmap cache
    private Color color_ = Color.black;
    /**
     * Point size to bitmap-cache.  Persists across deriveFont(*) because we use {@link #clone()} to make new instances.
     */
    //private List<SoftReference<NFontTrueType>> instances_ = new ArrayList<SoftReference<NFontTrueType>>(10);
    private List instances_ = new ArrayList(10);
    /**
     * Glyph ID to bitmap.  Taken from size2map_, if cacheing turned on.
     */
//    private Map<Integer, BufferedImage> gid2bitmap_ = null;
    private Map gid2bitmap_ = null;
    //    private static final Map<Integer, BufferedImage> NONE = new HashMap<Integer, BufferedImage>(1);
    private static final Map NONE = new HashMap(1);
    // dimensions of maxbbox in pixels used for cached bitmaps since advance not always width.
    private double imgx_, imgy_;
    private int imgw_, imgh_;
    private int[] buf_;

    private final Object parseGlyphLock = new Object();

    /**
     * Composite Glyph Description flag constants.
     */
    // bit 0 0000000000000001
    protected static final int ARG_1_AND_2_ARE_WORDS_BIT_0 = 0x1;
    // bit 1 0000000000000010
    protected static final int ARGS_ARE_XY_VALUES_BIT_1 = 0x2;
    // bit 2 0000000000000100
    protected static final int ROUND_XY_TO_GRID_BIT_2 = 0x4;
    // bit 3 0000000000001000
    protected static final int WE_HAVE_A_SCALE_BIT_3 = 0x8;
    // bit 5 0000000000100000
    protected static final int MORE_COMPONENTS_BIT_5 = 0x20;
    // bit 6 0000000001000000
    protected static final int WE_HAVE_AN_X_AND_Y_SCALE_BIT_6 = 0x40;
    // bit 7 0000000010000000
    protected static final int WE_HAVE_A_TWO_BY_TWO_BIT_7 = 0x80;
    // bit 8 0000000100000000
    public static final int WE_HAVE_INSTRUCTIONS_BIT_8 = 0x100;
    // bit 9 0000001000000000
    public static final int USE_MY_METRICS_BIT_9 = 0x200;

    private TrueTypeGlyphData mGlyphData;
    private Interpreter mInterpreter;
    public static final boolean ultraVerbose = false;

    // handle Type0 fonts which don't match that of the PDF encoding
    private CMap type0Encoding;
    private boolean isType0CidSub;

    /**
     * Creates nfont from <var>source</var>, which can be file, JAR resource, or even network.
     * <!-- System shares file descriptors. -->
     */
    public NFontTrueType(URL source, String subFormatType) throws FontFormatException, IOException {
        super(source);
        //X assert paths_!=null: getName(); => bitmap only
    }

    /**
     * Creates nfont fron <code>byte[]</code>, for use on small fonts and fonts embedded in PDF.
     * The memory for <var>data</var> is not reclaimed until the entire nfont is garbage collected.
     */
    public NFontTrueType(byte[] data, String subFormatType) throws FontFormatException, IOException {    // still have IOException if read past length
        super(data);
    }

    /* => too many sharp edges.  Never have TrueType embedded, except PDF where
    it is compressed anyhow, and usually small so just read in full.
      this(new RandomAccessByteArray(data, "r"));
    }

      Creates nfont based on a file wrapped by {@link org.icepdf.core.pobjects.fonts.nfont.io.RandomAccessFileBuffered}
      or byte array wrapped by {@link RandomAccessByteArray}.
      RandomAccess should be pointing to the first byte of the nfont data.
      Client must not delete file during use, and must <code>close()</code> <var>ra</var> when done with the nfont.
    public NFontTrueType(RandomAccess ra) throws FontFormatException, IOException {
      source_ = null; ra_ = ra;
      parseFile();
    }  */

    //    public NFontTrueType deriveFont(float pointsize) {
    public NFont deriveFont(float pointsize) {
        //X if (pointsize == getSize()) return this; => other deriveFont want new instance for further modification
        NFontTrueType f = (NFontTrueType) super.deriveFont(pointsize);
        //f.FUnit_ = pointsize / unitsPerEm_;	// pixel = FUnit * pointSize * resolution / (72.0 * units-per-em), but we want same # pixels across screen rather than same size, so no "* resolution / 72.0"
        f.u_ = new AffineTransform(m_);
        f.u_.concatenate(f.at_);
        f.u_.scale(pointsize, pointsize);    // ~8% faster to combine here, rather than repeat for drawEstring
        /* if (pointsize != size_) -- apply PDF widths*/
        f.gid2bitmap_ = null;
        return f;
    }

    //    public NFontTrueType deriveFont(AffineTransform at) {
    public NFont deriveFont(AffineTransform at) {
        NFontTrueType f = (NFontTrueType) super.deriveFont(at);
        f.u_ = new AffineTransform(m_);
        f.u_.concatenate(at);
        f.u_.scale(f.size_, f.size_);
        return f;
    }

    public FontFile deriveFont(float defaultWidth, ArrayList widths) {
        NFontTrueType f = (NFontTrueType) super.deriveFont(size_);
        // iterate over widths apply in DW
        // only apply DW if widths are not defined, otherwise we use what the
        // font specified.
        if (widths == null && defaultWidth > 0) {
            f.pdfbad_ = new boolean[widths_.length];
            // convert the width to correct unit and then to glyph space
            // as we'll be converting it back to user space when calculating
            // advance.
            defaultWidth = (float) (defaultWidth * 0.001 / m_.getScaleX());
            for (int i = 0, max = widths_.length; i < max; i++) {
                if (widths_[i] > 0) {
                    f.widths_[i] = defaultWidth;
                }
            }
        }
        // apply width ranges overwriting any DW eateries.
        // W seems to have varying results when applied,  we have a few corner
        // cases where W breaks the layout of a page.
        if (widths != null) {
            try {
                int current;
                Object currentNext;
                int maxLength = Math.max(f.widths_.length, calculateWidthLength(widths));
                f.newwidths_ = new float[maxLength];
                // copy over the widths that we already have.
//                System.arraycopy(f.widths_, 0, f.newwidths_, 0, f.widths_.length);
                // udpate with new widths for /w entry in a cid font.
                for (int i = 0, max = widths.size() - 1; i < max; i++) {
                    current = ((Number) widths.get(i)).intValue();
                    currentNext = widths.get(i + 1);
                    if (currentNext instanceof ArrayList) {
                        ArrayList widths2 = (ArrayList) currentNext;
                        for (int j = 0, max2 = widths2.size();
                             j < max2;
                             j++) {
                            f.newwidths_[current + j] = (float) (((Number) widths2.get(j)).intValue());
                        }
                        i++;
                    } else if (currentNext instanceof Number) {
                        int currentEnd = ((Number) currentNext).intValue();
                        float width2 = (float) (((Number) widths.get(i + 2)).intValue());
                        for (; current <= currentEnd; current++) {
                            f.newwidths_[current] = width2;
                        }
                        i += 2;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return f;
    }

    /**
     * Calculates the acutal length of the width array,  which in theory could be much longer
     * then the font's width array.
     */
    private int calculateWidthLength(ArrayList widths) {
        int current;
        Object currentNext;
        int maxGlph = 0;
        for (int i = 0, max = widths.size() - 1; i < max; i++) {
            current = ((Number) widths.get(i)).intValue();
            currentNext = widths.get(i + 1);
            if (currentNext instanceof ArrayList) {
                ArrayList widths2 = (ArrayList) currentNext;
                maxGlph = current + widths2.size();
                i++;
            } else if (currentNext instanceof Number) {
                maxGlph = ((Number) currentNext).intValue();
                i += 2;
            }
        }
        return maxGlph + 1;
    }


    //    public NFontTrueType deriveFont(Encoding encoding, CMap toUnicode) {
    public NFontSimple deriveFont(Encoding encoding, CMap toUnicode) {
        NFontTrueType f = (NFontTrueType) super.deriveFont(encoding, toUnicode);

        CMap c2g = null;
        if (encoding == null) {
            c2g = getCmap(PID_MACINTOSH, -1);    // X c2g = ur_.c2g_; => ur_ Unicode, but if setting encoding then in 256 land so go with Mac
            if (c2g == null) c2g = ur_.c2g_;
        } else if (Encoding.MAC_ROMAN == encoding)
            c2g = getCmap(PID_MACINTOSH, 0);
        else if (Encoding.IDENTITY == encoding)
            c2g = CMap.IDENTITY;
        else if (Encoding.WIN_ANSI == encoding) {
        }    // match with 'post' more accurate than 3,1-Unicode
//        else if (Encoding.SYMBOL == encoding) c2g = getCmap(PID_MICROSOFT, 0);

        else if ((c2g = getCmap(PID_MICROSOFT, 0)) != null) {
        }
        //else if (Encoding.ZAPF_DINGBATS==encoding) c2g = getCmap(?, ?);

        if (c2g == null) {    // differences, or WinAnsi encoding on Mac with nfont with only Mac encoding
            readPostTable();
            if (intrinsic_ != null)//&& !intrinsic_.getName().equals("TrueType-2"))
                c2g = encoding.mapTo(intrinsic_);
            else if (ur_.c2g_ == null)
                c2g = CMap.IDENTITY;
            else {    // cMap to glyph via Unicode (sheesh)
                char[] toSel = new char[256];
                for (int i = 0; i < 256; i++) {
                    String cname = encoding.getName((char) i);
                    char uni = Encoding.UNICODE.getChar(cname);
                    toSel[i] = ur_.c2g_.toSelector(uni);
                }
                c2g = new CMap(toSel);
            }
        }
        f.setCID(c2g, f.touni_);

        return f;
    }

    public NFontTrueType deriveFont(CMap cid2gid, CMap toUnicode) {    // used by PDF Type 0
        NFontTrueType f = null;
        try {
            f = (NFontTrueType) clone();
        } catch (CloneNotSupportedException canthappen) {
        }
        f.setCID(cid2gid, toUnicode);
        return f;
    }

    public NFontTrueType deriveFont(CMap toUnicode) {    // used by PDF Type 0
        NFontTrueType f = null;
        try {
            f = (NFontTrueType) clone();
        } catch (CloneNotSupportedException canthappen) {
        }
        f.setCID(null, toUnicode);
        return f;
    }

    private void setCID(CMap cid, CMap uni) {
        c2g_ = cid != null ? cid : ur_.c2g_;
        touni_ = uni != null ? uni : /*c2g_==ur_.c2g_? CMap.IDENTITY:=>same as default*/ CMap.IDENTITY;
        spacech_ = Integer.MIN_VALUE;
    }

    //X public NFontTrueType deriveFont(String name) => FontManager maps from name to number within dfont or ttc


    // parse top: standard/dfont/TTC

    protected void parseFile() throws FontFormatException, IOException {
        subformat_ = SUBFORMAT_NONE;

        loca_ = null;
        boolean fsub = fdfont_ = checkDfont();
        raoff_ = ra_.getFilePointer();
        subset_ = null;

        super.parseFile();

        if (!fsub) fsub = checkTTC();

        if (!fsub) {
            ttfoffset_ = new long[]{0L}; /*ttflength_ = new long[] { ra_.length() };*/
        }
        urs_ = new NFontTrueType[ttfoffset_.length];
        urs_[0] = ur_ = this;

        raoff_ = ttfoffset_[0];

        //X readLocaTable(); => for .ttf, shared across .ttc, [0] for .dfont
    }

    /**
     * Checks for OS X "dfont" wrapper, and if so move file pointer to start of 'sfnt' data.
     * Returns <code>true</code> iff nfont is a dfont.
     */
    private boolean checkDfont() throws IOException {
        long headeroff = ra_.getFilePointer();
        data_ = readRaw(headeroff, 16);
        setOffset(0);
        long dataoff = readUint32();    // not so coincidentally big endian too
        if (dataoff != 0x100) {
            ra_.seek(headeroff);
            return false;
        }    // vs TrueType 0x00000100 or 'true' or 'typ1', or OpenType 0x00010000

        Rez rez = null;
        try {
            rez = new Rez(ra_, headeroff);
        } catch (IOException notvalid) {
            ra_.seek(headeroff);
            return false;
        }
        long[] offlen = rez.getResources(Mac.intTag("sfnt"));
        int cnt = offlen.length / 2;
        if (cnt == 0) return false;

        // rather than using TTC, OS X has multiple 'sfnt' resources
        ttfoffset_ = new long[cnt];    //ttflength_ = new long[cnt];
        for (int i = 0, j = 0; i < cnt; i++, j += 2) {
            ttfoffset_[i] = offlen[j];
            //ttflength_[i] = offlen[j+1];
        }

        subformat_ = SUBFORMAT_DFONT;
        ra_.seek(ttfoffset_[0]);
        return true;
    }

    private boolean checkTTC() throws IOException {
        if (TAG_TTCF != version_) return false;

        float version = readFixed();
        int numFonts = readUint32();
        data_ = readRaw(raoff_, 12 + numFonts * 4);
        setOffset(12);
        ttfoffset_ = new long[numFonts]; //ttflength_ = new long[numFonts]; -- not available
        for (int i = 0; i < numFonts; i++) ttfoffset_[i] = readUint32();
        // version 2.0 has DSIG here

        subformat_ = SUBFORMAT_TTC;
        return true;
    }


    /**
     * Reads essential tables for single nfont (out of possibly others in a dfont or TTC).  Other tables read on demand.
     */
    protected void parse() throws IOException {
        // parses out the subtable entries and builds a tabledir_ array
        // of the all the offset for font programs tables.
        super.parse();

        // do quick check for the loca table, if we don't have it we don't have a TrueType font and
        // as a result might have an OpenType font that was miss labeled.
        boolean foundLoca = false;
        for (int i = 0; i < tabledir_.length; i++) {
            if (tabledir_[i].tag == OpenType.TAG_LOCA) {
                foundLoca = true;
                break;
            }
        }
        // make sure we aren't parsing the OpenType part.
        if (!foundLoca && !(this instanceof NFontOpenType)) {
            // throw the state exception so we can try to load the font again as an OpenType font.
            throw new IllegalStateException("Missing LOCA table, TrueType font is likely OpenType.");
        }

        readNameTable();    // collect nfont name even if can't parse the rest

        if (getTable(OpenType.TAG_HEAD).length == 0/*< 54?*/) {
            //assert getTableDirectory(TAG_BDAT) != null /*&& getTableDirectory("glyf")==null && getTableDirectory("CFF ")==null*/: getName();
            if (getTableDirectory(OpenType.TAG_BDAT) == null) {
                throw new IllegalStateException();
            }
            subformat_ = SUBFORMAT_BITMAP;    // assume bitmap for now
            m_ = u_ = new AffineTransform();
            return;
        }
        float version = readFixed();
        //assert version < 2.0: version;
        if (version >= 2.0) {
            throw new IllegalStateException(version + "");
        }
        int revision = readUint32();    //really readFixed();	// nfont revision
        readUint32();    // checkSumAdjustment
        int magic = readUint32();
//        assert magic==0x5f0f3cf5: magic;
        if (magic != 0x5f0f3cf5) {
            throw new IllegalStateException(magic + "");
        }
        int flags = readUint16();
        int unitsPerEm = readUint16();
        FUnit_ = /*pointsize=>elsewhere*/1f / unitsPerEm;
        long created = readDateTime(), modified = readDateTime();
        //float xMin=readFWord(), yMin=readFWord(), xMax=readFWord(), yMax=readFWord();	// bounding box in em
        float xMin = readInt16(), yMin = readInt16(), xMax = readInt16(), yMax = readInt16();    // X FWord so bounding box in em => want raw since scale by intrinsic matrix which includes FUnit
        bbox_ = new Rectangle2D.Double(xMin, yMin, xMax, yMax);
        macStyle_ = readUint16();
        int lowestRecPPEM = readUint16();
        fontDirectionHint_ = readInt16();
        indexToLocFormat_ = readInt16();
        //assert indexToLocFormat_==0 || indexToLocFormat_ == 1: indexToLocFormat_;	// 0=short, 1=long
        if (!(indexToLocFormat_ == 0 || indexToLocFormat_ == 1)) {
            throw new IllegalStateException(indexToLocFormat_ + "");
        }
        int glyphDataFormat = readInt16();

        // process the maxp table
        readMaxpTable();

        // parse out the metrics table
        readMetricsTables();    // X on demand? => need for echarWidths and FLAG_FIXEDPITCH

        paths_ = new SoftReference/*<Shape>*/[mMaxpTable.numGlyphs_];    // do in constructor rather than on demand so deriveFont() clones all share
        // setup
        m_ = new AffineTransform(FUnit_, 0.0, 0.0, -FUnit_, 0.0, 0.0);
        u_ = m_;    // applied = identity, points = 1.  Shouldn't be drawing 1 point fonts, but...

        flags_ = computeFlags();    // after readNameTable() so have names_ set for guessing IP right

        // assume client will use Unicode
        // preferences: Unicode, symbol, Mac 256, whatever embedded, identity
        CMap c2g = null;
        if (c2g == null) {
            c2g = getCmap(PID_MICROSOFT, 0);
            if (c2g != null) encoding_ = Encoding.SYMBOL;
        }
        if (c2g == null) {
            c2g = getCmap(PID_MICROSOFT, 1);
            if (c2g != null) encoding_ = Encoding.WIN_ANSI;
        }    // Unicode, commonly available
        if (c2g == null) c2g = getCmap(PID_UNICODE, -1);
        if (c2g == null) {
            c2g = getCmap(PID_MACINTOSH, 0);
            if (c2g != null) encoding_ = Encoding.MAC_ROMAN;
        }    // only for first 256 chars
        // Zapf?
        if (c2g == null) c2g = getCmap(-1, -1);
        if (c2g == null)
            c2g = CMap.IDENTITY;    // no 'cmap' -- presumably supplied by PDF.  neither CMap nor CIDtoGID in invoice.core by FOP 0.20
        // encoding for PDF is not that in 'post', which can have partial differences that assumes, e.g., 'A' == 65

        setCID(c2g, CMap.IDENTITY);
    }


    /*package-private*/
    float readFWord() {
        return readInt16() * FUnit_;
    }

    /*package-private*/
    float readUFWord() {
        return readUint16() * FUnit_;
    }


    private int computeFlags() {
        int flags = FLAG_NONE;

        if (getTable(OpenType.TAG_POST, 0, 14).length >= 14) {    // seen null on CID
            setOffset(12);
            int isFixedPitch = readUint16();
            if (isFixedPitch == 1) flags |= FLAG_FIXEDPITCH;
        }

        if (getTable(OpenType.TAG_OS2).length >= 56) {    // OS2 table optional -- not used by Mac OS
            int version = readUint16();    // 0 = TrueType rev 1.5, 1 = TrueType rev 1.66, 2 = OpenType rev 1.2, 3 = OpenType 1.4
            int xAvgCharWidth = readInt16();
            int usWeightClass = readUint16();
            if (1 <= usWeightClass && usWeightClass <= 9)
                usWeightClass *= 100;    // buggy: Chancery, Charcoal, Hoefler*, Impact, Java Lucida, ...
            String name = getName();
            if (WEIGHT_NORMAL == usWeightClass && name != null && name.indexOf('-') > 0/*&& MS/ITC*/)
                usWeightClass = NFontManager.guessWeight(getName());    // buggy ITC on Windows XP: ErasITC CopperplateGothic, FranklinGothic, ...
            int usWidthClass = readUint16();
            rights_ = readUint16();
            skip(20);
            int sFamilyClass = readInt16();
            int bFamilyType = readUint8(), bSerifStyle = readUint8(), bWeight = readUint8(), bProportion = readUint8(), bContrast = readUint8(),
                    bStrokeVariation = readUint8(), bArmStyle = readUint8(), bLetterform = readUint8(), bMidline = readUint8(), bXHeight = readUint8();
            skip(16);    // Unicode blocks
            skip(4);    // nfont vendor
            int fsSelection = readUint16();
            int usFirstCharIndex = readUint16();    // => bit assignments pending in TrueType
            skip(2);
            // END of TrueType -- OpenType has more fields
            // ...
            // breakchar for spacech_ (spacegid_)

            weight_ = usWeightClass;

            // IBM
            int famcl = sFamilyClass >> 8;
            if (famcl == 10)
                flags |= FLAG_SCRIPT;
            else if (famcl == 12)
                flags |= FLAG_SYMBOLIC;
            else if (famcl != 8 && famcl != 0/*need positive ID for Apple*/)
                flags |= FLAG_SERIF;

            // PANOSE
            if (bFamilyType == 2 && bProportion == 9) flags |= FLAG_FIXEDPITCH;

            if (usWidthClass <= 4) flags |= FLAG_CONDENSED;

            if ((fsSelection & 1) != 0) flags |= FLAG_ITALIC;
            //if ((fsSelection&0x20)!=0) WEIGHT_BOLD; -- more precise above

            if (2 <= bFamilyType && bFamilyType <= 5 && 0 < usFirstCharIndex && usFirstCharIndex < 128)
                flags |= FLAG_NONSYMBOLIC;    // OS X non-Latin have 0

        } else {    // guess from copyright+trademark+manufacturer+license
            rights_ = names_ != null ? NFontManager.guessRight(names_[0] + " || " + names_[7] + " || " + names_[8] + " || " + names_[13]) : RIGHT_PREVIEW_PRINT;

            if ((macStyle_ & 2) != 0) flags |= FLAG_ITALIC;
            // fam.matches(italic|cursiva|oblique|inclined) FLAG_ITALIC
            // fam.matches(bold) FLAG_BOLD
            String name = getName();
            if (name.indexOf("Cond") > 0) flags |= FLAG_CONDENSED;
            weight_ = name.indexOf("Light") > 0 ? WEIGHT_LIGHT : (macStyle_ & 1) != 0 ? WEIGHT_BOLD : WEIGHT_NORMAL;

            String fam = getFamily();
//            if (!fam.matches("Helvetica.*|Arial.*|.+Sans.*")) flags |= FLAG_SERIF;
            if (fam.startsWith("Helvetica") ||
                    fam.startsWith("Arial") ||
                    fam.indexOf("Sans") > 0) {
                flags |= FLAG_SERIF;
            }
            //if (fam.matches(".+Script.*|.+Handwr.+")) flags |= FLAG_SCRIPT;	// Embassy, Zapfino have OS/2 table
            if (fam.indexOf("Script") > 0 || fam.indexOf("Handwr") > 0) {
                flags |= FLAG_SCRIPT;
            }
//            if (fam.matches("Symbol.*|ZapfDingbats|Dingbats|Wingdings|Webdings|.+Bats.*"))
            if (fam.startsWith("Symbol") ||
                    fam.startsWith("ZapfDingbats") ||
                    fam.startsWith("Dingbats") ||
                    fam.startsWith("Wingdings") ||
                    fam.startsWith("Webdings") ||
                    fam.indexOf("Bats") > 0) {
                flags |= FLAG_SYMBOLIC;    // else flags |= FLAG_NONSYMBOLIC;
            } else /*if (0 < usFirstCharIndex&&usFirstCharIndex < 128)*/
                flags |= FLAG_NONSYMBOLIC;
//System.out.println(getName()+": macStyle = "+Integer.toBinaryString(macStyle_)+" => weight="+weight_+", flags="+strFlags(flags));
        }

        return flags;
    }


    public String getName() {
        return name_;
    }

    public String getFamily() {
        return family_;
    }

    public int getStyle() {
        int style = PLAIN;
        if (name_ != null) {
            String name = name_.toLowerCase();
            if (name.contains("bold") ||
                    name.contains("medium") ||
                    name.contains("dark") ||
                    name.contains("black") ||
                    name.endsWith("bt") ||
                    name.contains("demi")) {
                style = BOLD;
            }
            if (name.contains("ital") ||
                    name.contains("slant") ||
                    name.contains("obli")) {
                style |= ITALIC;
            }
        }
        return style;
    }

    public String getDesigner() {
        return names_ != null ? names_[9] : null;
    }

    public String getCopyright() {
        return names_ != null ? names_[0] : null;
    }

    /**
     * Returns version string (id 5) in the 'name' table, not the first four bytes of the file, which is what MS Windows does.
     * If there is no such string defined, returns an assumed "1.0".
     * See {@link #getID()}.
     */
    public String getVersion() {
        return names_ != null && names_[5] != null ? names_[5] : super.getVersion();
    }

    public int getID() {
        return version_;
    }

    public int getRights() {
        return rights_;
    }

    /**
     * Returns subfont within OS X <code>.dfont</code>s or TrueType Collections,
     * numbered from <code>0</code> .. {@link #getCount()}.
     */
    public NFontTrueType getSubfont(int num) throws IOException, InterruptedException {
        if (num < 0 || num > getCount())
            return null;//throw new IOException("bad nfont number: "+num+" > "+getCount());
        if (urs_[num] != null) return urs_[num];

        NFontTrueType f = (NFontTrueType) deriveFont(/*1f*/size_);
        f.ur_ = f;
        f.raoff_ = ttfoffset_[num];
        f.getRA();
        try {
            f.parse(); //assert f.paths_!=null: f.getName();
            if (fdfont_/*SUBFORMAT_DFONT == getSubformat() -- NO, if created as OpenType*/) {
                f.loca_ = null;
                f.intrinsic_ = null;
            } //f.readLocaTable();
        } catch (Throwable e) {
            logger.log(Level.FINER, "Error parsing subfont ", e);
        } finally {
            f.releaseRA();
        }
        urs_[num] = f;
        return f;
    }

    public int getCount() {
        return urs_.length;
    }

    public String getFormat() {
        return FORMAT;
    }

    public String getSubformat() {
        return subformat_;
    }

    public int getFlags() {
        return flags_;
    }

    public int getWeight() {
        return weight_;
    }

    public boolean isHinted() {
        if (hint_ == -1) {
            hint_ = 0;
            if (SUBFORMAT_BITMAP == getSubformat()) {
            }    // bitmaps aren't hinted
            else {
                for (int i = 0 + 1, imax = getMaxGlyphNum(); i < imax && hint_ != 1; i++)
                    getGlyph(i);
                releaseRA();
            }
        }
        return hint_ == 1;
    }

    public int getMaxGlyphNum() {
        return mMaxpTable.numGlyphs_;
    }

    public int getNumGlyphs() {
        readLocaTable();
        return glyphcnt_;
    }

    public boolean canDisplayEchar(char ech) {
        if (type0Encoding != null) {
            // for type0 encodings we can use the code space range to help
            // determine if the first byte of a charcter code should be treated
            // as one or two byte encoded as we ch < 128 can still be the first
            // byte of a two byte characcter.
            if (type0Encoding != null && type0Encoding.getCodeSpaceRange() != null) {
                int[][] range = type0Encoding.getCodeSpaceRange();
                for (int i = 0; i < range.length; i++) {
                    if (ech >= range[i][0] && ech <= range[i][1]) {
                        // in an range then we assume we can display the ech.
                        return true;
                    }
                }
                return false;
            }
        }
        return canDisplayGID(getEchToGid(ech));
    }

    private boolean canDisplayGID(int gid) {
        return gid < getMaxGlyphNum() /*&& loca_[gid+1]-loca_[gid] > 0--space char 0-len but don't show box*/;
    }

    public char getSpaceEchar() {
        if (spacech_ == Integer.MIN_VALUE) {
            char ch;
            int gid;
            if (Encoding.SYMBOL == encoding_)
                spacech_ = NOTVALID_CHAR;    // Symbol has a space, but GhostScript buggy
            else if (encoding_ != null && "space".equals(encoding_.getName(' ')) && canDisplayEchar(' '))
                spacech_ = ' ';    // 0100ip0405.core has /Differences with additional /space at 160
            else if (touni_ != null)
                spacech_ = touni_.fromSelector(' ');
            else if (encoding_ != null && (ch = encoding_.getChar("space")) != NOTDEF_CHAR && canDisplayEchar(ch))
                spacech_ = ch;
            else if ((gid = getEchToGid(' ')) != NOTDEF_CHAR && canDisplayGID(gid)) {
                boolean fra = ra_ == null;
                spacech_ = ((GeneralPath) getGlyph(gid)).getPathIterator(new AffineTransform()).isDone() ? ' ' : NOTDEF_CHAR;
                if (fra) releaseRA();
            } else
                spacech_ = NOTVALID_CHAR;

            // check that space has no or short spline
            // ...
        }
        return (char) spacech_;
    }


    private void readLocaTable() {
        if (loca_ != null) return;    // (read on demand and) already read
        if (this != ur_) {
            ur_.readLocaTable();
            loca_ = ur_.loca_;
            glyphcnt_ = ur_.glyphcnt_;
            return;
        }

        getTable(OpenType.TAG_LOCA);
        glyphcnt_ = 0;
        if (data_.length == 0) {
            loca_ = new int[0];
            return;
        }    // may not exist if snft format but not TrueType

        loca_ = new int[mMaxpTable.numGlyphs_ + 1];
        if (indexToLocFormat_ == 0) {
            for (int i = 0, imax = mMaxpTable.numGlyphs_ + 1; i < imax; i++) {
                loca_[i] = readUint16() * 2;
            }
        } else {
            //assert indexToLocFormat_==1;
            if (indexToLocFormat_ != 1) {
                throw new IllegalStateException();
            }
            for (int i = 0, imax = mMaxpTable.numGlyphs_ + 1, jmax = data_.length;
                 i < imax && getOffset() + 2 < jmax; i++) {
                loca_[i] = readUint32();
            }
        }
        for (int i = 0, imax = mMaxpTable.numGlyphs_; i < imax; i++) {
            //assert loca_[i] <= loca_[i + 1]: loca_[i] + " > " + loca_[i + 1];
//            if (loca_[i] > loca_[i + 1]){
//                throw new IllegalStateException(loca_[i] + " > " + loca_[i + 1]);
//            }
            if (loca_[i] < loca_[i + 1])
                glyphcnt_++;    // set numGlyphs_ to smallest i for which this is true?
        }
    }

    // read on demand
    private void readPostTable() {
        if (intrinsic_ != null) return;
        if (this != ur_) {
            ur_.readPostTable();
            intrinsic_ = ur_.intrinsic_;
            return;
        }


        try {
            if (getTable(OpenType.TAG_POST).length < 10/*28*/) return;
        } catch (Exception e) {
            logger.fine("Error processing POST table.");
        }

        float format = readFixed();
        float italicAngle = readFixed();
        float underlinePosition = readFWord();
        float underlineThickness = readFWord();
        int isFixedPitch = readUint16();
        skip(18); //2 + 4 * 4

        Encoding en;
        if (format == 1f)
            en = ENCODING_MAC_STANDARD;

        else if (format == 2f) {
            int numberOfGlyphs = readUint16();
            int[] glyphNameIndex = new int[numberOfGlyphs];
            int maxinx = 0;
            for (int i = 0; i < numberOfGlyphs; i++) {
                int inx = glyphNameIndex[i] = readUint16();
                if (inx > maxinx) maxinx = inx;
            }
            int numberNewGlyphs = Math.max(0, maxinx - 257);
            String[] names = new String[numberNewGlyphs];
            for (int i = 0; i < numberNewGlyphs; i++)
                names[i] = readStringPascal();

            // PDF-513, when reviewing https://developer.apple.com/fonts/TTRefMan/RM06/Chap6post.html
            // it would appear the iteration should take place on the numberNewGlyphs
            // and not numberOfGlyphs.  As described in "'post' Format 2"
            String[] map = new String[numberOfGlyphs];
            for (int i = 0; i < numberNewGlyphs; i++) {
                int inx = glyphNameIndex[i];
                map[i] = inx <= 257 ? MAP_MAC_STANDARD[inx] : names[inx - 258];
            }
            en = new Encoding("TrueType-2", map);

        } else if (format == 2.5f) {    // deprecated as of Febuary 2000
            int numberOfGlyphs = readUint16();
            String[] map = new String[numberOfGlyphs];
            for (int i = 0; i < numberOfGlyphs; i++) {
                int delta = readInt8();    // can be negative
                map[i] = MAP_MAC_STANDARD[i + delta];
            }
            en = new Encoding("TrueType-2.5", map);

        } else if (format == 3f)
            en = null;    // no PostScript name information

        else {
            //  Format 4, Composite fonts on Japanese, Chinese or Korean printers
            // work only with character codes. QuickDraw GX printer,
            // https://developer.apple.com/fonts/TTRefMan/RM06/Chap6post.html.
            // ignore the unsupported format and try to render the font and document.
//            if (format != 4) {
//                throw new IllegalStateException();
//            }
            //  maps to composite nfont on printer -- shouldn't be accessed as simple nfont
            en = null;
        }

        intrinsic_ = en;
    }

    private void readNameTable() {
        int tlen = getTable(OpenType.TAG_NAME).length;
        if (tlen < 6) {
            name_ = "[no name]";
            family_ = "[no family]";
            return;
        }    // no name in some embedded
        int format = readUint16();    //assert format==0: format; //-- 0xffff in /System/Library/Monaco.dfont
        int count = readUint16();
        int stringOffset = readUint16();

        String[] best = new String[20/*all predefined strings*/];
        int[] bestpid = new int[best.length];
        Arrays.fill(bestpid, -1);
        for (int i = 0, imax = Math.min(count, (tlen - 6) / 12); i < imax; i++) {    // Math.min for WPO11_Font_Guide.core Emboss-Normal
            int pid = readUint16(), psid = readUint16(), langid = readUint16(), nameid = readUint16();
            int len = readUint16(), off = readUint16() + stringOffset;
            if (nameid >= best.length
                    || off + len > tlen    // several in WPO11_Font_Guide.core (subsetting chopped strings, but didn't touch offsets)
                    )
                continue;

            boolean funi = PID_UNICODE == pid || (PID_MICROSOFT == pid/*&& (psid==1 || psid==0) -- all MS?*/);
            if (bestpid[nameid] == -1 || langid == 0 || (PID_UNICODE == pid && bestpid[nameid] != PID_UNICODE)) {
                String s = (funi ? readString16(off, len) : readString(off, len)).trim();
                if (s.length() > 0) {
                    best[nameid] = s;
                    bestpid[nameid] = pid;
                }
            }
        }

        name_ = best[6] != null ? best[6] : best[4] != null ? name_ = best[4] : "[no name]";
        family_ = best[1] != null ? best[1] : NFontManager.guessFamily(name_);

        names_ = best;
    }

    private void readMaxpTable() {
        getTable(OpenType.TAG_MAXP);
        // make sure we have some data left to parse.
        if (data_.length < 4) {
            throw new IllegalStateException();
        }
        float version = readFixed();
        // 0.5 for CFF in OpenType
        if (version >= 2.0) {
            throw new IllegalStateException(version + "");
        }
        mMaxpTable = new Maxp();

        mMaxpTable.numGlyphs_ = readUint16();

        if (data_.length > 6) {
            mMaxpTable.maxPoints_ = readUint16();
            mMaxpTable.maxContours_ = readUint16();
            mMaxpTable.maxComponentPoints_ = readUint16();
            mMaxpTable.maxComponentContours_ = readUint16();
            mMaxpTable.maxZones_ = readUint16();
            mMaxpTable.maxTwilightPoints_ = readUint16();
            mMaxpTable.maxStorage_ = readUint16();
            mMaxpTable.maxFunctionDefs_ = readUint16();
            mMaxpTable.maxInstructionDefs_ = readUint16();
            mMaxpTable.maxStackElements_ = readUint16();
        }
    }

    private void readMetricsTables() {
        getTable(OpenType.TAG_HHEA);
//        assert data_.length >= 36;
        if (data_.length < 36) {
            throw new IllegalStateException();
        }
        float version = readFixed();
        //assert version < 2.0: version;
        if (version >= 2.0) {
            throw new IllegalStateException(version + "");
        }
        ascent_ = readInt16();    // FWord
        descent_ = readInt16();    // FWord
        float lineGap = readFWord();
        float advanceWidthMax = readUFWord();
        float minLeftSideBearing = readFWord();
        float minRightSideBearing = readFWord();
        float xMaxExtent = readFWord();
        int caretSlopeRise = readInt16();    // 1 for vertical caret
        int caretSlopeRun = readInt16();    // 0 for vertical
        float caretOffset = readFWord();    // 0 for non-slanted fonts
        readInt16();
        readInt16();
        readInt16();
        readInt16();
        int metricDataFormat = readInt16();
        int numOfLongHorMetrics = readUint16();    // number of advance widths in metrics table

        getTable(OpenType.TAG_HMTX);
//        assert data_.length >= numOfLongHorMetrics * 2;
//        if (data_.length < numOfLongHorMetrics * 2) {
//            throw new IllegalStateException();
//        }
        // advance widths
        widths_ = new float[numOfLongHorMetrics];
        lsb_ = new float[mMaxpTable.numGlyphs_];
        for (int i = 0; i < numOfLongHorMetrics && i < mMaxpTable.numGlyphs_; i++) {
            widths_[i] = readUint16();
            lsb_[i] = readInt16();
        }
        for (int i = numOfLongHorMetrics; i < mMaxpTable.numGlyphs_ && getOffset() + 2 <= data_.length/*CHLORINZ.TTF*/; i++) {
            lsb_[i] = readInt16();
        }
    }

    /**
     * Reproduce the logic for fetching the LSB value from the table.
     *
     * @param gid
     * @return
     */
    private short getLeftSideBearing(int gid) {
        if (gid < widths_.length)
            return (short) ((int) widths_[gid] & 0xffff);
        if (lsb_ == null)
            return 0;
        return (short) lsb_[gid - widths_.length];
    }

    /**
     * Returns {@link org.icepdf.core.pobjects.fonts.nfont.CMap} in use.
     */
    public CMap getCmap() {
        return c2g_;
    }

    /**
     * Returns cmap for <var>platformID</var> and <var>platformSpecificID</var>, or <code>null</code> if that table does not exist.
     * Pass <code>-1</code> to match any value.
     */
    public CMap getCmap(int platformID, int platformSpecificID) {
        SfntDirectory td = getTableDirectory(OpenType.TAG_CMAP);
        if (td == null)
            return CMap.IDENTITY;    // PDF CID fonts take external CMap
        getTable(td, 0, 4);
        int version = readUint16();    //assert version<10: version;	// usually 0, 1 in /Library/Fonts/DecoTypeNaskh.ttf, anything in bad data
        int numberSubtables = readUint16();    //assert numberSubtables>0: numberSubtables; -- bad data

        getTable(td, 0, 4 + numberSubtables * 8);
        setOffset(4);    // keep offset_
        boolean ffound = false;
        int pid = -1, psid = -1, offset = -1;
        if (version < 10/*VICTOR.TTF*/ && data_.length >= (8 + 4))
            for (int i = 0; i < numberSubtables; i++) {    // tables must be sorted
                pid = readUint16();    // read header
                psid = readUint16();
                offset = readUint32();

                if ((platformID == pid || platformID == -1) && (platformSpecificID == psid || platformSpecificID == -1)) {
                    byte[] tmpdata_ = data_;
                    int tmpoffset = getOffset();
                    if (getTable(td, offset, 12).length >= 6) {    // NERVOUS.TTF has empty body
                        int format = readUint16();
                        int length;
                        if (format <= 6)
                            length = readUint16();
                        else {
                            skip(2);
                            length = readUint32();
                        }
                        int lang = format <= 6 ? readUint16() : readUint32();
                        getTable(td, offset, /*length => mimic Acrobat?*/td.length - offset);    // read body
                        ffound = true;
                        break;
                    } else {
                        data_ = tmpdata_;
                        setOffset(tmpoffset);
                    }

                } else if (platformID < pid || (platformID == pid && platformSpecificID < psid))
                    break;
            }

        if (!ffound) return null;


        int format = readUint16();
        int length, language;
        char[][] segs;
        if (format <= 6) {    // 16-bit characters
            length = readUint16();
            language = readUint16();
            segs = new char[256][];    // language 0 on non-Mac
        } else {    // 32-bit character
            skip(-2);
            float vformat = readFixed();
            length = readUint32();
            language = readUint32();
            segs = new char[256 * 17][];    // Unicode Basic Multilingual Plane + 16 supplementary => 65536 max glyphs
        }
//if (DEBUG) System.out.println(getName()+", "+pid+"/"+psid+", CMap format = "+format+"  ");

        if (format == 0) {    // single byte
            char[] cm = new char[256];
            for (int i = 0, imax = cm.length; i < imax; i++)
                cm[i] = (char) readUint8();
//System.out.print(" "+(int)cm[i]);}
            segs[0] = cm;

        } else if (format == 2) {    // mix 8/16
            // read header keys and headers
            int[] subHeaderKeys = new int[256];
            int socnt = 0 + 1/*record for 8-bit*/;
            boolean[] souse = new boolean[256];
            for (int i = 0, imax = subHeaderKeys.length; i < imax; i++) {
                int sh = subHeaderKeys[i] = readUint16() / 8;
                if (sh > 0 && !souse[sh]) {
                    socnt++;
                    souse[sh] = true;
                }
//System.out.println("  high "+i+"="+subHeaderKeys[i]);}
            }
            int[] firstCode = new int[socnt], entryCount = new int[socnt], idDelta = new int[socnt], idRangeOffset = new int[socnt];
            for (int i = 0; i < socnt; i++) {
                firstCode[i] = readUint16();
                entryCount[i] = readUint16();
                idDelta[i] = readInt16();
                idRangeOffset[i] = readUint16();
//System.out.println("#"+i+" @ "+(offset_ - 8)+": "+firstCode[i]+"..+"+entryCount[i]+": +/- "+idDelta[i]+", off = "+idRangeOffset[i]);
            }
            int gIA = getOffset();
//System.out.println("socnt="+socnt+", glyphIndexArray = "+gIA+", length="+length+", data_.length="+data_.length+", gIA length = "+(length-gIA));
//            assert gIA == 3 * 2 + 256 * 2 + 8 * socnt;
            if (gIA != 3 * 2 + 256 * 2 + 8 * socnt) {
                throw new IllegalStateException();
            }
//            assert 6 + 512 + 6 + idRangeOffset[0] == gIA;	// format/len/lang + subHeaderKeys + first/entry/idDelta[0]
            if (6 + 512 + 6 + idRangeOffset[0] != gIA) {
                throw new IllegalStateException();
            }

            // make segs from tables and glyphIndexArray
            segs[0] = new char[256];    // mixed 8/16 so should always have 8
            for (int i = 0; i < 256; i++) {
                int k = subHeaderKeys[i];
                //assert k==0 || souse[k]: i + " => " + k;
                if (k != 0 || !souse[k]) {
                    throw new IllegalStateException(i + " => " + k);
                }
//                assert k < socnt: k;
                if (k >= socnt) {
                    throw new IllegalStateException(k + "");
                }
//if (k>=socnt) continue;
//System.out.println("W: k="+k+", "+firstCode[k]+"..+"+entryCount[k]+": +/- "+idDelta[k]+", off = "+idRangeOffset[k]);

                if (k == 0) {
//                    assert idDelta[0]==0;
                    if (idDelta[0] != 0) {
                        throw new IllegalStateException();
                    }
                    setOffset(gIA + i * 2);
                    segs[0][i] = (char) readUint16();
//System.out.println("1:"+Integer.toHexString(i)+" @ "+(offset_-2)+" => "+(int)segs[0][i]);

                } else {
                    setOffset(6 + 512 + k * 8 - 2 + idRangeOffset[k]);
//System.out.println("2:"+Integer.toHexString((i<<8) | firstCode[k])+" .. +"+entryCount[k]+" @ "+offset_);
                    for (int s = firstCode[k], e = s + entryCount[k]; s < e; s++) {
                        //offset_ = gIA-2 + idRangeOffset[k] - (6+512) - k*8;
                        int code = readUint16();
                        if (code != 0) code += idDelta[k];
                        code &= 0xffff;
//System.out.println("2:"+Integer.toHexString((i<<8) | s)+" @ "+(offset_-2)+" => "+Integer.toHexString(code));
                        if (segs[i] == null)
                            segs[i] = new char[256];    // i is high byte
                        segs[i][s] = (char) code;
                    }
                }

            }
//System.exit(0);

        } else if (format == 4) {    // 16, segmented.  popular Windows
            int segCount = readUint16() / 2;
            //int lenneed = segCount*6, lenmax = td.length-offset;	// MSungStd-Light-Acro.otf doesn't read enough -- apparently Acrobat just seeks in file, so don't need to have accurate length
            //if (lenneed>length && lenmax>=lenneed) { System.err.println("need "+lenneed); data_ = getTable(td,offset,lenmax); }
            skip(6);
//System.out.println("len = "+length+", lenneed="+lenneed+", lenmax="+lenmax+", #segs="+segCount/*+", lastchar="+Integer.toHexString(endCode[segCount-1])*/+", vs "+td.length);
            int[] endCode = new int[segCount];
            for (int i = 0; i < segCount; i++)
                endCode[i] = readUint16();    // sorted in order of increasing endCode values
//System.out.println("\tlastchar="+Integer.toHexString(endCode[i]));}
            int reservedPad = readUint16();    // "This value should be zero"
            int[] startCode = new int[segCount];
            for (int i = 0; i < segCount; i++) startCode[i] = readUint16();
            int[] idDelta = new int[segCount];
            for (int i = 0; i < segCount; i++) idDelta[i] = readInt16();

//int gIA = offset_ + segCount*2;
//System.out.println(getName()+", segCount="+segCount);
            for (int i = 0; i < segCount; i++) {
                int idRangeOffset = readUint16();    // Offset in bytes to glyph indexArray, or 0
//if (idRangeOffset!=0) System.out.println("      "+startCode[i]+"/0x"+Integer.toHexString(startCode[i])+".."+endCode[i]+" + "+idDelta[i]+" / "+idRangeOffset+", start of glyphIndexArray = "+gIA+", len="+(data_.length-gIA)+" vs formula "+(offset_+idRangeOffset));
                for (int s = startCode[i], s0 = s, e = endCode[i], d = idDelta[i]; s <= e; s++) {
                    if (s == 0xffff /*&& e==0xffff && code==0*/)
                        continue;    // break? -- 0xffff marks end, but we have another way, so don't burn up code page
                    int code;
                    if (idRangeOffset == 0) {
                        code = (s + d) & 0xffff;
                    } else {
                        int gi = (s - s0) * 2 + getOffset() - 2 + idRangeOffset;
                        if (gi + 1 < data_.length) {
                            code = ((data_[gi] & 0xff) << 8) | (data_[gi + 1] & 0xff);
//System.out.println("        "+s+"/"+Integer.toHexString(s)+" @ "+gi+" => "+code+" / "+idDelta[i]+" @ "+data_[gi-2]+" "+data_[gi-1]+" "+data_[gi]+" "+data_[gi+1]);
                        } else {
                            code = 0;
                        }
                        if (code != 0) code -= idDelta[i];
                    }
                    int page = s >> 8;
                    if (segs[page] == null) segs[page] = new char[256];
                    segs[page][s & 0xff] = (char) code;
//if (0x300 <= s&&s <= 0x400) System.out.print(getName()+" "+s+"=>"+code);
//System.out.println();
                }
            }
            //int[] glyphIndexArray[variable] = readUint16();	// Glyph index array

        } else if (format == 6    // 16, trimmed array/contiguous
                || format == 10) {    // 32, trimmed array (like format 6 but 32-bit)
            int firstCode = format == 6 ? readUint16() : readUint32();
            int entryCount = format == 6 ? readUint16() : readUint32();
//            assert firstCode + entryCount <= 0x100000;
            if (firstCode + entryCount > 0x100000) {
                throw new IllegalStateException();
            }
            for (int s = firstCode, j = 0; j < entryCount; s++, j++) {
                int code = readUint16();
                int page = s >> 8;
                if (segs[page] == null) segs[page] = new char[256];
                segs[page][s & 0xff] = (char) code;
            }

        } else if (format == 8) {    // mixed 16/32
//            assert false: "format 8 " + getName();	// not supported yet -- not seen in use
            throw new IllegalStateException("format 8 " + getName());
        } else if (format == 12) {    // 32, segmented.  example in /System/Library/LastResort.dfont and Doulos SIL Regular.ttf
            int nGroups = readUint32();
//System.out.println("cmap format 12: "+nGroups);
            for (int i = 0; i < nGroups; i++) {
                for (int s = readUint32(), e = readUint32(), gid = readUint32(); s <= e; s++, gid++) {
//if (true) { System.out.println("   "+Integer.toHexString(s)+".."+Integer.toHexString(e)+" => "+gid/*+" @ "+offset_*/); break; }
//System.out.println("   "+Integer.toHexString(s)+".."+Integer.toHexString(e)+" => "+gid);
                    int page = s >> 8;
                    //assert page <segs.length: Integer.toHexString(s) + " => " + page;
                    if (page >= segs.length) {
                        throw new IllegalStateException(Integer.toHexString(s) + " => " + page);
                    }
                    if (segs[page] == null)
                        segs[page] = new char[256];
                    segs[page][s & 0xff] = (char) gid;
                }
            }

        } else {
            //assert false: format;
            return CMap.IDENTITY;
        }


        // Windows 3 0 adds 0xf000 -- sometimes!
        // stop this before it get out of the lab
        if (PID_MICROSOFT == pid && psid == 0) {
            // check f000..ff00 and remap to 0..f00
//System.out.println("remap? "+segs[0xf0]+" => "+segs[0]+" ?");
            for (int i = 0xf0, j = 0; i <= 0xf0/*0xff?*/; i++, j++) {
                char[] cm = segs[i];
                if (cm != null && segs[j] == null) {
//if (DEBUG) System.out.println("  => remapping "+Integer.toHexString(i)+"00 to "+Integer.toHexString(j)+"00");
                    segs[j] = cm;
                    segs[i] = null;
                }
            }
        }

        return new CMap(segs);
    }


    /**
     * Caching and newwidths shaping on top of buildChar.
     */
    /*package-private*/
    Shape getGlyph(int gid) {
        //assert canDisplayGID(gid);
        if (!canDisplayGID(gid)) {
            if (notdef_ == null)
                notdef_ = canDisplayGID(NOTDEF_CHAR) ? getGlyph(NOTDEF_CHAR) : GLYPH_ZERO_CONTOUR;    // delay because may be CFF
            return notdef_;
        }

        SoftReference ref = paths_[gid];//SoftReference<Shape> ref = paths_[gid];
        Shape s = ref != null ? (Shape) ref.get() : null;

        if (s == null) {
            synchronized (parseGlyphLock) {

                // quick check to make sure we didn't already parse the glype
                // after intering the protected block.
                ref = paths_[gid];
                s = ref != null ? (Shape) ref.get() : null;
                if (s != null) {
                    return s;
                }

                //System.out.println("cache="+ref+", loading gid="+gid+" "+getName()+" "+getSize()+" "+paths_);//+" "+loca_[gid]+"..+"+(loca_[gid+1]-loca_[gid]));

                // At this stage, the glyph could be composite or simple, so we
                // don't know what kind of glyphData we might parse.
                mGlyphData = new TrueTypeGlyphData(mMaxpTable);
                parseGlyph(gid);
                short lsb = getLeftSideBearing(gid);
                mGlyphData.closeGlyphDefinition(lsb, d);

                if (mGlyphData.getGlyphCount() == 0) {
                    return GLYPH_ZERO_CONTOUR;
                }
                s = buildPathFromGlyphDefinition();
                paths_[gid] = new SoftReference(s);
            }
        }

        return s;
    }

    /**
     * Read the glyph header, and diverge based on the contour count
     */
    private void parseGlyph(int gid) {
        readLocaTable();
        if (gid < 0 || gid + 1 >= loca_.length) return;
        try {
            getRA();
            if (getTable(OpenType.TAG_GLYF, loca_[gid], loca_[gid + 1] - loca_[gid]).length < 10)
                return;
            // number of contours
            int nCon = readInt16();    // >=0 single; <0 compound
            mGlyphData.setNCon(nCon);

            // bounds on coordinate data, MUST parse to read glyph offsets
            float xMin = readFWord(), yMin = readFWord(), xMax = readFWord(), yMax = readFWord();

            if (nCon == 0) {
                //return;
            } else if (nCon > 0) {
                parseSimpleGlyph(gid, nCon);
            } else {
                if (nCon != -1) {
                    throw new IllegalStateException(nCon + "");
                }
                parseCompositeGlyph(gid);
            }
        } catch (Throwable e) {
            logger.log(Level.FINER, "Error parsing glyph", e);
        } finally {
            releaseRA();
        }
    }


    //    private final double d = 97.65625;
//    private final double d = 48.828125;
    private final double d = 1;           // Our icepdf-pro scale-free version

    /**
     * Parses the common section from a Glyph and appends the Glyph header information
     * to the current built in state. Gad, that's not right either. I should put those
     * member variables into a publicly available state object.
     *
     * @param nCon number of contours to read
     */
    private void parseSimpleGlyph(int gid, int nCon) {

        byte[] flagsArray;
        int[] endPointOfContourArray = new int[nCon];
        int xCoordArray[];
        int yCoordArray[];
        int nOn = 0;

        // endPtsOfContours[n], parses out an array of last points of each
        // contour; n is the number of contours; array entries are point indices
        int j;
        for (int i = 0; i < nCon; i++) {
            j = readUint16();
            endPointOfContourArray[i] = j;
        }

        // grab the instructions for later applications of hint operations
        // Array of instructions for this glyph
        int instructionLength = readUint16();

        if (instructionLength > 1) hint_ = 1;
        int[] instructions = new int[instructionLength];
        // Instruction
        for (int idx = 0; idx < instructionLength; idx++) {
            instructions[idx] = readUint8() & 0xFF;
        }

        int nPts = endPointOfContourArray[endPointOfContourArray.length - 1] + 1;
        // parse out the flags.
        byte b;
        flagsArray = new byte[nPts];
        for (int i = 0; i < nPts; i++) {

            b = data_[getOffset()];
            skip(1);
            flagsArray[i] = b;
            //assert (b & 0xc0)==0: getName()+" "+Integer.toBinaryString(b);	// seen in gs5man_e.core - Palatino.dfont - Palatino-Italic
            if ((b & 1) != 0) nOn++;
            if ((b & 8) != 0) {
                int cnt = readUint8();  // Handle flag repeat count
                for (int imax = i + cnt; i < imax; i++) {
                    flagsArray[i + 1] = b;
                }
                if ((b & 1) != 0) nOn += cnt;
            }
        }

        // parse out the x coords - Array of x-coordinates; the first is
        // relative to (0,0), others are relative to previous point
        int x = 0;
        xCoordArray = new int[nPts];
        for (int i = 0; i < nPts; i++) {
            b = flagsArray[i];
            int dx;
            if ((b & 2) != 0) {    //  x coord is 1 byte
                dx = readUint8();
                if ((b & 0x10) == 0) dx = -dx; // sign is negative.
            } else {
                // If set in 2 byte mode, this coord is same as last coord.
                if ((b & 0x10) != 0) {
                    dx = 0;
                } else {
                    dx = readInt16();  // x coord is 2 bytes
                }
            }
            x += dx;    // scaled when draw
            xCoordArray[i] = x;
        }
        // parse out the y coords -  Array of y-coordinates; the first is
        // relative to (0,0), others are relative to previous point
        int y = 0;
        yCoordArray = new int[nPts];
        for (int i = 0; i < nPts; i++) {
            b = flagsArray[i];
            int dy;
            if ((b & 4) != 0) {
                dy = readUint8();
                if ((b & 0x20) == 0) dy = -dy;
            } else {
                if ((b & 0x20) != 0) {
                    dy = 0;
                } else {
                    dy = readInt16();
                }
            }
            y += dy;
            yCoordArray[i] = y;
        }

        int xr[] = new int[nPts];
        int yr[] = new int[nPts];
        byte flagsr[] = new byte[nPts];
        int endpointsr[] = new int[nCon];

        int temp;
        int xT = mGlyphData.xTranslation[mGlyphData.fetcherId];
        int yT = mGlyphData.yTranslation[mGlyphData.fetcherId];

//        System.out.println("  SimpleGlyph: " + gid + " has: " + nPts + " points");
        if (logger.isLoggable(Level.FINEST)) {//ultraVerbose) {
            System.out.println("  SimpleGlyph has: " + nPts + " points - flags, X, and Y follow: ");
            System.out.print("     ");
            for (int idx = 0; idx < nPts; idx++) {
                System.out.print(" 0x" + Integer.toHexString(flagsArray[idx]));
            }
            System.out.println();
            System.out.print("    ");
            for (int idx = 0; idx < nPts; idx++) {
                System.out.print("  " + Integer.toString(xCoordArray[idx] + xT));
            }

            System.out.println();
            System.out.print("    ");
            for (int idx = 0; idx < nPts; idx++) {
                System.out.print("  " + Integer.toString(yCoordArray[idx] + yT));
            }

            System.out.println();
        }

        mGlyphData.fetcherId++;
        for (int idx = 0; idx < nPts; idx++) {

            temp = xCoordArray[idx] + xT;
            xr[idx] = (int) (d * (double) temp + 0.5D);
//            xr[idx] = (Integer) xCoordVector.get(idx);

            // Version with translation handled in the points
            temp = yCoordArray[idx] + yT;
            yr[idx] = (int) (d * (double) temp + 0.5D);

            // version with translation handled by AffineTransform
//            yr[idx] = (Integer)yCoordVector.get(idx) ;
            flagsr[idx] = flagsArray[idx];
        }


        for (int idx = 0; idx < nCon; idx++) {
            endpointsr[idx] = endPointOfContourArray[idx];
        }
        mGlyphData.appendGlyphDefinition(xr, yr, endpointsr, nOn, instructions, flagsr, gid);

    }

    /**
     * Parse a composite glyph. Read the composite header and then
     * loop over and parse each subglyph. This appends information into
     * the GlyphData object until parsing is done.
     *
     * @param gid the glyphId
     */
    private void parseCompositeGlyph(int gid) {

        int xTranslate, yTranslate;
        int existingxTranslate = 0, existingyTranslate = 0;

        SfntDirectory td = getTableDirectory(OpenType.TAG_GLYF);

        // parse through composite glyph description for this glyph id.
        int flags = 0;
//        TrueTypeGlyphData glyphData = null;

        for (int childGlyphDx = 0; getOffset() + 4 <= data_.length; childGlyphDx++) {    // in /Library/Fonts/Raanana.ttf sometimes don't set end flag and have one extra byte

            flags = readUint16();
            int gid2 = readUint16();

            // "gid too" -- glyph to add
            if (!canDisplayGID(gid2)) {
                throw new IllegalStateException(gid2 + "");
            }

//            float xTranslation, yTranslation;
            // ARGS_ARE_XY_VALUES_BIT_1
            // x and y offsets to be added to the glyph.
            if ((flags & ARGS_ARE_XY_VALUES_BIT_1) == ARGS_ARE_XY_VALUES_BIT_1) {
                // ARG_1_AND_2_ARE_WORDS_BIT_0
                if ((flags & ARG_1_AND_2_ARE_WORDS_BIT_0) == ARG_1_AND_2_ARE_WORDS_BIT_0) {
                    xTranslate = readInt16();
                    yTranslate = readInt16();
                } else {
                    xTranslate = readInt8();
                    yTranslate = readInt8();
                }

            } else {    // seen in /System/Library/Helvetica.dfont and /System/Library/Times.dfont

                // arg1 and arg 2 are a two point number,  In this case the
                // the first point number indicates the point that is to be matched
                // to the new glyph's "matched" point.  Once a glyph is added, its
                // point numbers begin directly after the last glyphs (endpoint of first glyph +1)

                if ((flags & ARG_1_AND_2_ARE_WORDS_BIT_0) == 0) {
                    mGlyphData.pointOne = readUint8();
                    mGlyphData.pointTwo = readUint8();
                } else {
                    mGlyphData.pointOne = readUint16();
                    mGlyphData.pointTwo = readUint16();
                }
                int p1x = mGlyphData.getXPtr(1, mGlyphData.pointOne);
                int p1y = mGlyphData.getYPtr(1, mGlyphData.pointOne);
                int p2x = mGlyphData.getXPtr(1, mGlyphData.pointTwo);
                int p2y = mGlyphData.getYPtr(1, mGlyphData.pointTwo);
                int dx = p1x - p2x;
                int dy = p1y - p2y;
                xTranslate = dx;
                yTranslate = dy;

//                xTranslate = yTranslate = 0;    // failsafe
            }

            if ((flags & ROUND_XY_TO_GRID_BIT_2) == ROUND_XY_TO_GRID_BIT_2) {
//                System.out.println("Round XY to Grid:");
            }

            float xscale = 1f, yscale = 1f, scale01 = 0f, scale10 = 0f;

            // WE_HAVE_A_SCALE
            if ((flags & WE_HAVE_AN_X_AND_Y_SCALE_BIT_6) == WE_HAVE_AN_X_AND_Y_SCALE_BIT_6) {
                xscale = readF2Dot14();
                yscale = readF2Dot14();
            }
            // WE_HAVE_AN_X_AND_Y_SCALE
            else if ((flags & WE_HAVE_A_SCALE_BIT_3) == WE_HAVE_A_SCALE_BIT_3) {
                xscale = yscale = readF2Dot14();
            }
            // WE_HAVE_A_TWO_BY_TWO
            else if ((flags & WE_HAVE_A_TWO_BY_TWO_BIT_7) == WE_HAVE_A_TWO_BY_TWO_BIT_7) {
                xscale = readF2Dot14();
                scale10 = readF2Dot14();
                scale01 = readF2Dot14();
                yscale = readF2Dot14();
            }


            if ((flags & USE_MY_METRICS_BIT_9) != 0) {
                if (gid < widths_.length && gid2 < widths_.length) {
                    widths_[gid] = widths_[gid2];    // only if not monospace, in which case already the same
                }
// removed this IllegalStateException, a little harsh, some Chrystal report
// doc where throughing this, doesn't seem to affect rendering otherwise.
                else if (gid2 < widths_.length) {
//                    assert gid2>=widths_.length;
//                    throw new IllegalStateException();
//                    widths_[gid] = widths_[gid2];
                }
                //? lsb_[gid]=lsb_[gid2];
            } //? else x += (lsb_[gid]  ); //-lsb -- apparently not

            //  Cache the current location
            byte[] btmp = data_;
            int offtmp = getOffset();

//            getTable(td, loca_[gid2], loca_[gid2 + 1] - loca_[gid2]);
//
//            int nCon = readInt16();
//            offset_ += 8;
//            if (nCon < 0) { // This is a sub - composite glyph
//                flags = flags & ~0x20;
//                break;
//            }    // Legendum_legacy.otf points to composite, which is apparently legal -- FIX

            // This is the header of the gid2 found in the composite glyph record.
            // That includes the x,y points for that simple glyph

            SfntDirectory td2 = getTableDirectory(OpenType.TAG_FPGM);
            getTable(td2, 0, 41250);

            int l = xTranslate;
            int i1 = yTranslate;
            xTranslate += existingxTranslate;
            yTranslate += existingyTranslate;

            // Some freaky opentype nonsense here.
            if ((flags & 0x800) != 0) {
                float sm = Math.max(xscale, yscale);
                xTranslate *= sm;
                yTranslate *= sm;
            }
            xscale /= d;
            yscale /= d;
            AffineTransform xform;
            // This is the case for typical instruction executing code. The instructions require
            // all the points to be already x,yTranslated before executing instructions.
            xform = new AffineTransform(xscale,
                    scale01,
                    scale10,
                    yscale,
                    xTranslate,
                    yTranslate);

            mGlyphData.appendCompositeTransform(xform);
            mGlyphData.xTranslation[childGlyphDx] = xTranslate;
            mGlyphData.yTranslation[childGlyphDx] = yTranslate;

            existingxTranslate = xTranslate;
            existingyTranslate = yTranslate;

//            System.out.println("Compound glyph: " + gid + " compound flags: " + flags + ", has subglyph,  gid: " + gid2);

            parseGlyph(gid2);
            existingxTranslate -= l;
            existingyTranslate -= i1;

            // Reset from temp
            data_ = btmp;
            setOffset(offtmp);

            // If no more components, read the instructions
            int[] instructions = null;
            if ((flags & MORE_COMPONENTS_BIT_5) == 0) {
                if ((flags & WE_HAVE_INSTRUCTIONS_BIT_8) == WE_HAVE_INSTRUCTIONS_BIT_8) {
                    // pop off the instructions.
                    int numberInstr = readUint16();
//                    System.out.println("Glyph: " + gid + " has: " + mGlyphData.getGlyphCount() +
//                            " subglyphs, AND has: " + numberInstr + " instructions");
                    instructions = new int[numberInstr + 5];
                    for (int i = 0; getOffset() < data_.length; i++) {
                        instructions[i] = readUint8() & 0xFF;
                    }
                    mGlyphData.appendCompositeInstructions(instructions);
                }
            }
        }

        // What to do with this?  This flag isn't defined in the document.

        /*
            byte[] btmp = data_;
            int offtmp = offset_;
            // (open type)if set +
            if ((flags & 0x800) != 0) {
                float sm = Math.max(scalex, scaley);
                xTranslation *= sm;
                yTranslation *= sm;
            }

         */
    }

    /**
     * Hmmm. The Composite Glyph needs to build a different path than the
     * subglyphs. This whole thing only works because
     */
    private GeneralPath buildPathFromGlyphDefinition() {

        GeneralPath retVal = null;
        int nCon = mGlyphData.getNCon();

        if (nCon > 0) {
            retVal = interpretSimpleGlyph();
        } else {
            retVal = interpretCompositeGlyph();
        }

        return retVal;
    }

    // It might be possible to merge the following two methods, but they
    // generate a slightly different GeneralPath structure.

    /**
     * Interpret the contents of a GlyphData to create the glyph geometry
     *
     * @return The GeneralPath generated by a simple Glyph
     */
    private GeneralPath interpretSimpleGlyph() {

        GeneralPath retVal;
        int[] instr = mGlyphData.getInstructions();
        if (instr != null && instr.length > 0 && isHinting()) {
            try {
                executeHintingSetup();
                if (isHinting()) {
                    mInterpreter.processGlyph(mGlyphData);
                }
            } catch (Throwable e) {
                logger.log(Level.WARNING, "Error applying glyph hints.", e);
            }
        }

        if (mGlyphData.getGlyphCount() == 0) {
            return GLYPH_ZERO_CONTOUR;
        }
        // Fetch these after the instructions are executed.
        retVal = buildPathFromGlyph(mGlyphData.getXByGlyphId(1, 0),
                mGlyphData.getYByGlyphId(1, 0),
                mGlyphData.xTranslation[0],
                mGlyphData.yTranslation[0],
                mGlyphData.getEndPtsOfContours(0),
                mGlyphData.getFlags(0),
                mGlyphData.getnOn(0));
        return retVal;
    }

    /**
     * Interpret the glyphData to create the geometry containing
     * all the geometries from all the sub glyphs
     *
     * @return GeneralPath object containing all subglyphs info
     */
    private GeneralPath interpretCompositeGlyph() {

        GeneralPath s = new GeneralPath(GeneralPath.WIND_NON_ZERO);

        int[] instr = mGlyphData.getInstructions();
        if (instr.length > 0 && isHinting()) {
            executeHintingSetup();
            try {
                if (isHinting()) {
                    mInterpreter.processGlyph(mGlyphData);
                }
            } catch (Throwable e) {
                logger.log(Level.WARNING, "Error applying glyph hints.", e);
            }
        }

        GeneralPath add;
        for (int compDx = 0; compDx < mGlyphData.getGlyphCount(); compDx++) {

            AffineTransform xform = mGlyphData.getGlyphTransform(compDx);
            int gid = mGlyphData.getGIDByIndex(compDx);
            SoftReference ref = paths_[gid]; //SoftReference<Shape> ref = paths_[gid];
            if (ref != null) {
                add = ref != null ? (GeneralPath) ref.get() : null;
            } else {

                add = buildPathFromGlyph(
                        mGlyphData.getXByGlyphId(1, compDx),
                        mGlyphData.getYByGlyphId(1, compDx),
                        mGlyphData.xTranslation[compDx],
                        mGlyphData.yTranslation[compDx],
                        mGlyphData.getEndPtsOfContours(compDx),
                        mGlyphData.getFlags(compDx),
                        mGlyphData.getnOn(compDx)
                );
            }

            if (xform.isIdentity()) {
                s.append(add.getPathIterator(null), false);    // base glyph usually has identity xform and slightly faster if avoid transforming points
            } else {
                s.append(add.getPathIterator(xform), false);
            }
        }

        return s;
    }


    /**
     * Generate the geometry for a single simple glyph
     *
     * @param xCoordinates
     * @param yCoordinates
     * @param endPtsOfContours
     * @param flags
     * @param nOn
     * @return
     */
    private GeneralPath buildPathFromGlyph(int[] xCoordinates,
                                           int[] yCoordinates,
                                           int xTranslation,
                                           int yTranslation,
                                           int[] endPtsOfContours,
                                           byte[] flags,
                                           int nOn) {
        // run instructions to move points around at point size
        // intruct(xCoordinates, yCoordinates, hints); -- we don't
        final int ON_CURVE_FLAG = 0x01;

        // build the GeneralPath from points
        GeneralPath s = new GeneralPath(GeneralPath.WIND_NON_ZERO, nOn * 2 + 5);
        //s.moveTo(lsb_[gid], 0); => done in drawing
        boolean fonlast = false, fclose = false, fnewpath = true;
        int nPts = flags.length;
        int x, y;
        for (int i = 0, con0 = 0, coni = 0; i < nPts; ) {
            int inx;
            if (fclose) {    // first because pending business
                fclose = false;
                inx = con0;
                coni++;
                fnewpath = true;
            } else if (fnewpath) {
                fnewpath = false;
                fonlast = false;
                con0 = i;
                x = xCoordinates[i] - xTranslation;
                y = yCoordinates[i] - yTranslation;
                // set first point of path for invariant
                if ((flags[i] & ON_CURVE_FLAG) != 0)
                    s.moveTo(x, y);
                else {    // path starts off-curve
                    i++;    // catch again on fclose when have a current point
                    if (i == nPts)
                        break;    // free floating off-curve -- ignore
                    float xn = xCoordinates[i] - xTranslation, yn = yCoordinates[i] - yTranslation;
                    if ((flags[i] & ON_CURVE_FLAG) == 0)
                        s.moveTo((x + xn) / 2f, (y + yn) / 2f);
                    else
                        s.moveTo(xn, yn);    // if start off-off take midpoint, else start off-on let on have it
                }
                inx = i;
                if (i == endPtsOfContours[coni])
                    fclose = true;    // single point path.  has happened on point 0
                    //assert i!=endPtsOfContours[coni];	// 1-point contour -- happens in Altona_Docu_1v1.core, obj 84
                else
                    i++;
            } else {
                inx = i;
                if (i == endPtsOfContours[coni])
                    fclose = true;    // i++ => postpone so don't end loop before wrap around to start
                else
                    i++;/*else fclose=false;--already*/
            }

            boolean fon = (flags[inx] & ON_CURVE_FLAG) != 0;
            x = xCoordinates[inx] - xTranslation;
            y = yCoordinates[inx] - yTranslation;

            if (fon) {
                if (fonlast)
                    s.lineTo(x, y); /*fonlast=true;--already*/    // on - on = line
                else
                    fonlast = true;    // off - on = nothing.  used as endpoint of previous point and maybe something by next
            } else {
                inx = fclose || inx + 1 >= nPts/*error in MS PMingLiU*/ ? con0 : inx + 1;    // if close after this point, next point wrapsaround to start of contour
                float xn = xCoordinates[inx] - xTranslation, yn = yCoordinates[inx] - yTranslation;
                fon = (flags[inx] & ON_CURVE_FLAG) != 0;
                if (!fon) {
                    xn = (x + xn) / 2f;
                    yn = (y + yn) / 2f;
                }    // off-on takes next point as endpoint as is, if off-off then midpoint
                s.quadTo(x, y, xn, yn);
                fonlast = false;
            }
            if (fnewpath) {
                //s.closePath(); -- already closed (and maybe not with straight line)
                i++;    // postponed eating point so loop wouldn't terminate before processed first on contour again
            }
        }
        return s;
    }

    private boolean isBitmap() {
        if (gid2bitmap_ != null) {    // usual case
        } else if (NFont.getUseBitmaps()
                && at_.getShearY() == 0.0 && at_.getShearX() == 0.0    // no rotation -- intrinsic affine can have slope for fabricated italics, which is allowed
                && size_ * at_.getScaleX() <= 18f && size_ * at_.getScaleY() <= 18f    // small/medium
            //X && newwidths_==null
                ) {

            Rectangle2D bbox = getMaxCharBounds();
            imgx_ = bbox.getX();
            imgy_ = bbox.getY();
            imgw_ = (int) Math.ceil(bbox.getWidth());
            imgh_ = (int) Math.ceil(bbox.getHeight());

            // match existing instance? if so, share cache
            for (int i = 0, imax = instances_.size(); i < imax; i++) {
                NFontTrueType f = (NFontTrueType) ((SoftReference) instances_.get(i)).get();
                if (f != null && this != f && f.gid2bitmap_ != null && f.gid2bitmap_ != NONE
                        && getName().equals(f.getName())    // dfont/TTC
                        && Math.abs(size_ - f.size_) < 0.01 && at_.equals(f.at_) && Arrays.equals(newwidths_, f.newwidths_)) {
                    gid2bitmap_ = f.gid2bitmap_;
                    buf_ = f.buf_;
                    //System.out.println("  REUSE "+f+", cMap="+System.identityHashCode(f.gid2bitmap_)+" "+f.gid2bitmap_.size());
                    break;
                } //else if (f!=null && this!=f && f.gid2bitmap_!=null && f.gid2bitmap_!=NONE && size_==f.size_) System.out.println("\tmiss: "+getName()+" "+size_+" != "+System.identityHashCode(f)+" "+at_.equals(f.at_)+" "+Arrays.equals(newwidths_, f.newwidths_));
            }

            // create new cache
            if (gid2bitmap_ == null) {
                //gid2bitmap_ = new HashMap<Integer, BufferedImage>(100);
                gid2bitmap_ = new HashMap(100);
                buf_ = new int[imgw_ * imgh_];

                // maybe share this cache with future instances
                boolean freplace = false;
                for (int i = 0, imax = instances_.size(); i < imax; i++) {
                    if (((SoftReference) instances_.get(i)).get() == null) {
//                        instances_.set(i, new SoftReference<NFontTrueType>(this));
                        instances_.set(i, new SoftReference(this));
                        freplace = true;
                    }
                }
                if (!freplace)
                    instances_.add(new SoftReference(this));//new SoftReference<NFontTrueType>(this));
            }

        } else
            gid2bitmap_ = NONE;

        return gid2bitmap_ != NONE;
    }


    /**
     * Bitmap cache of glyph, per color.
     */
    private Image getBitmap(Shape glyph, int gid, /*double width,--advance not enough*/ double sx) {
        Integer key = Integers.getInteger(gid);    // no object creation for gid < 1000
        // MT: sychronize on cache...
//        Map<Integer, BufferedImage> cache = gid2bitmap_;
        Map cache = gid2bitmap_;
        BufferedImage img = (BufferedImage) cache.get(key);
        if (img == null) {
            //X img = (BufferedImage)GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(imgw_, imgh_); => may not have transparency, may be headless, slower
            img = new BufferedImage(imgw_, imgh_, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) img.getGraphics();    // expensive on OS X -- adds 0.10 ms/glyph, amortized!
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setColor(COLOR_TRANSPARENT);
            g.fillRect(0, 0, imgw_, imgh_);
            g.setColor(color_);
            g.fillRect(0, 0, 1, 1);    // mark as bitmap
            g.translate(-imgx_, -imgy_);
            g.transform(u_);
            g.scale(sx, 1.0);
            g.setColor(color_);
            g.fill(glyph);
            //img.setRGB(gid%imgw_, gid%imgh_, Color.RED.getRGB());

            if (img.getRGB(0, 0) == 0)
                img.setRGB(0, 0, color_.getRGB() & RGB_MASK);    // pixel (0,0) has individual glyph's color, as injected transparent if necessary -- then recolor on demand (not all glyphs or new cache for change of color)

            g.dispose();

            cache.put(key, img);

        } else /*if (false)*/ {    // correct color?
            int rgb = color_.getRGB() & RGB_MASK;
            if (rgb != (img.getRGB(0, 0) & RGB_MASK)) {
                int[] buf = buf_;
                img.getRGB(0, 0, imgw_, imgh_, buf, 0, imgw_);
                for (int i = 0, imax = buf.length; i < imax; i++)
                    if (buf[i] != 0)
                        buf[i] = (buf[i] & ALPHA_MASK) | rgb;    // no additional alpha with antialiasing
                img.setRGB(0, 0, imgw_, imgh_, buf, 0, imgw_);    // still accelerated? if not, make new bitmap and replace in cache
                if (img.getRGB(0, 0) == 0) img.setRGB(0, 0, rgb);
                //System.out.print("c");
            }
        }

        return img;
    }


    public Point2D echarAdvance(char ech) {
        int gid = getEchToGid(ech);
        double adv = 0;
        int glyphId = ech - firstch_;
        if (!canDisplayGID(gid)) {
            adv = 0.0;
        } else if (newwidths_ != null && glyphId >= 0 && glyphId < newwidths_.length && newwidths_[glyphId] > 1) {
            adv = newwidths_[glyphId] * AFM_SCALE;
        } else if (newwidths_ != null && glyphId >= 0 && glyphId < newwidths_.length && newwidths_[glyphId] <= 1) {
            adv = newwidths_[glyphId];
        }
        if (adv == 0) {
            adv = widths_[Math.min(gid, widths_.length - 1)] * m_.getScaleX();
        }

        // bad newwidths_ applies only to glyph scaling, not advance
        double x = adv * size_ * at_.getScaleX(), y = adv * size_ * at_.getShearY();
        return new Point2D.Double(x, y);
    }

    public Rectangle2D getEstringBounds(String estr, int beginIndex, int limit) {
        Rectangle2D bounds = new Rectangle2D.Double();
        double x = 0f;
        for (int i = beginIndex; i < limit; i++) {
            int gid = estr.charAt(i);
            if (gid < 0 || gid >= getMaxGlyphNum()) continue;
            Rectangle2D r = getGlyph(gid).getBounds2D();
            r.setRect(x + r.getX(), r.getY(), r.getWidth(), r.getHeight());
            bounds = bounds.createUnion(r);
            x += widths_[Math.min(gid, widths_.length - 1)];
        }
        releaseRA();

        AffineTransform at = new AffineTransform(m_);
        at.concatenate(at_);
        at.scale(size_, -size_);
        Point2D pt1 = new Point2D.Double(bounds.getX(), bounds.getY()), pt2 = new Point2D.Double(bounds.getWidth(), bounds.getHeight());
        at.transform(pt1, pt1);
        at.transform(pt2, pt2);
        bounds.setRect(pt1.getX(), pt1.getY(), pt2.getX(), pt2.getY());

        return bounds;
    }


    public void applyCidCMap(CMap encoding) {
        // mark we have a cid font without the correct/intended font.
        isType0CidSub = true;
        type0Encoding = encoding;
    }

    public String toUnicode(char displayChar) {

        // encoding mapping test.
        if (isType0CidSub) {
            // apply the typ0 encoding
            char tmp = type0Encoding.toSelector(displayChar);
            // apply the UCS2 encoding
            return touni_.toUnicode(tmp);
        } else {
            return touni_ != null ? touni_.toUnicode(displayChar) : String.valueOf(displayChar);
        }
    }

    private int getEchToGid(char ech) {
        if (isType0CidSub) {
            // apply the typ0 encoding
            ech = type0Encoding.toSelector(ech);
            // apply the UCS2 encoding
            ech = touni_.toUnicode(ech).charAt(0);
            // finally we can get a usable glyph;
            return c2g_.toSelector(ech);
        } else {
            return c2g_.toSelector(ech);
        }
    }

    public void drawEstring(Graphics2D g, String estr, float x, float y, long layout, int mode, Color strokecolor) {

//        assert estr!=null;
        if (estr == null) {
            throw new IllegalArgumentException();
        }
        if (SUBFORMAT_BITMAP == getSubformat()) return;

        //Encoding en0 = getEncoding(); if (Encoding.WIN_ANSI!=en0 && Encoding.MAC_ROMAN!=en0) en0 = en0.getBase();
        boolean fsc = (LAYOUT_SMALLCAPS & layout) != 0;

        AffineTransform at = g.getTransform();
        boolean fbitmap =
                MODE_FILL == mode/*too restrictive but almost always true*/
                        && isBitmap()
                        && at.getScaleX() == 1.0 && at.getShearY() == 0.0 && at.getShearX() == 0.0 && at.getScaleY() == 1.0;
        Color color = g.getColor();
        BasicStroke bs = null;
        if (fbitmap) {
            color_ = color;
        } else {
            g.translate(x, y);
            g.transform(u_);

            if (MODE_STROKE == mode || MODE_FILL_STROKE == mode || MODE_STROKE_ADD == mode || MODE_FILL_STROKE_ADD == mode) {
                bs = (BasicStroke) g.getStroke();    // not copied?
                g.setStroke(new BasicStroke((float) (bs.getLineWidth() / Math.sqrt(Math.abs(u_.getDeterminant()))), bs.getEndCap(), bs.getLineJoin(), bs.getMiterLimit(), bs.getDashArray()/*adjust?*/, bs.getDashPhase()));
            }
        }

        double xx = x + imgx_;
        int yy = (int) Math.round(y + imgy_);
        for (int i = 0, imax = estr.length(); i < imax; i++) {
            char ech = estr.charAt(i);
            // encoding mapping test.
            int gid = getEchToGid(ech);
            // looking for specific /uniHEx encoding or name mapping.
            // using the document's encoding.
            if (NOTDEF_CHAR == gid) {
                ech = encoding_.getDiffChar(ech);
                gid = c2g_.toSelector(ech);
            }
            // maybe bad encoding, as in pdflr/nfont.core's Wingdings and Webdings
            // falling back on the fonts econding
            if (NOTDEF_CHAR == gid) {
                gid = ur_.c2g_.toSelector(ech);
            }
            // fallback to font's mapping width original ech as we wiped it.
            // this shouldn't happen very often.
            if (NOTDEF_CHAR == gid) {
                ech = estr.charAt(i);
                gid = ur_.c2g_.toSelector(ech);
            }


            Shape glyph = getGlyph(gid);
            if (glyph == notdef_) { /*ech=NOTDEF_CHAR; -- keep same char for widths shaping*/
                gid = 0;
            }

            double w = widths_[Math.min(gid, widths_.length - 1)];
            int inx = ech - firstch_;
            double nw = newwidths_ != null && 0 <= inx && inx < newwidths_.length && newwidths_[inx] > 1 ? newwidths_[inx] * AFM_SCALE / m_.getScaleX() : 0.0;    // not echarAdvance() because working in character space
            // Windows widths on curly quotes (single/double * left/right) way too wide, so ignore on scaling -- but keep for advance (ugh!)
            double sx = nw <= 1.0 || Math.abs(w - nw) <= 2.0  /*thousandths*/
                    || nonAsciiSet
                    || (pdfbad_ != null && pdfbad_[inx]) ?
                    //|| (Encoding.WIN_ANSI==en0 && 0221<= ech&&ech <=0224)?
                    1.0 :
                    nw / w;
            if (fbitmap) {    // faster because: (1) image over filled spline, (2) doesn't kill rendering pipeline
                Image img = getBitmap(glyph, gid, sx);
                if (img != null) g.drawImage(img, (int) (xx + 0.5), yy, null);
                xx += (nw != 0.0 ? nw : w) * u_.getScaleX();
            } else {
                if (sx != 1.0) g.scale(sx, 1.0);
                if (MODE_FILL == mode || MODE_FILL_STROKE == mode || MODE_FILL_ADD == mode || MODE_FILL_STROKE_ADD == mode)
                    g.fill(glyph);
                /*NO else*/
                if (MODE_STROKE == mode || MODE_FILL_STROKE == mode || MODE_STROKE_ADD == mode || MODE_FILL_STROKE_ADD == mode) {
                    if (strokecolor == null || strokecolor.equals(color))
                        g.draw(glyph);    // outline after fill
                    else {
                        g.setColor(strokecolor);
                        g.draw(glyph);
                        g.setColor(color);
                    }    // set color for next glyph.  faster than draw one full string, change color, draw next full string?
                }
                if (sx != 1.0) g.scale(1.0 / sx, 1.0);

                g.translate((nw != 0.0 ? nw : w) /*- lsb*/, 0.0);
            }

        }
        if (!fbitmap) g.setTransform(at);
        if (bs != null) g.setStroke(bs);
        releaseRA();
    }

    public Shape getEstringOutline(String estr, float x, float y) {
        char ech = estr.charAt(0);
        int gid = getEchToGid(ech);
        if (NOTDEF_CHAR == gid)
            gid = ur_.c2g_.toSelector(ech);

        Shape glyph = getGlyph(gid);
        Area outline = new Area(glyph);
        AffineTransform transform = new AffineTransform();
        transform.translate(x, y);
        transform.concatenate(u_);
        outline = outline.createTransformedArea(transform);
        // apply transform
        return outline;
    }

    public Rectangle2D getCharBounds(char displayChar) {
        return getEstringBounds(String.valueOf(displayChar), 0, 1);
    }

    public ByteEncoding getByteEncoding() {
        if (type0Encoding == null) {
            return null;
        }
        if (type0Encoding.isMixedByte()) {
            return ByteEncoding.MIXED_BYTE;
        } else if (type0Encoding.isTwoByte()) {
            return ByteEncoding.TWO_BYTE;
        } else {
            return ByteEncoding.ONE_BYTE;
        }
    }

    void readCVT_(Cvt table) {
        getTable(OpenType.TAG_CVT);
        int j = data_.length / 2;
        table.unscaledCvt = new short[j];
        table.cvt_ = new int[table.unscaledCvt.length];
        table.cvt_ = new int[table.unscaledCvt.length];
        for (int k = 0; k < j; k++) {
            table.unscaledCvt[k] = (short) readInt16();
        }
    }

    public void debugPosteriors(TrueTypeGlyphData data) {

        int overallPointIdx = 0;
        System.out.println("After hinting");
        System.out.println("\t\tp.n.\tNew X\t\tNew Y\t\tOrg X\t\tOrg Y\t\tx tch\t\ty tch");
        for (int gdx = 0; gdx < data.getGlyphCount(); gdx++) {
            int[] xCoords = data.getXByGlyphId(1, gdx);
            int[] yCoords = data.getYByGlyphId(1, gdx);
            int[] oxCoords = data.getXByGlyphId(3, gdx);
            int[] oyCoords = data.getYByGlyphId(3, gdx);
            boolean[] xtouched = data.getXTouchedByGlyphId(1, gdx);
            boolean[] ytouched = data.getYTouchedByGlyphId(1, gdx);

            byte[] flags = data.getFlagsByGlyphId(gdx);
            for (int idx = 0; idx < xCoords.length; idx++) {
                System.out.println("\t\t" + (overallPointIdx++) + "\t\t" + xCoords[idx] + "\t\t" + yCoords[idx] +
                        "\t\t" + oxCoords[idx] + "\t\t" + oyCoords[idx] + "\t\t" +
                        xtouched[idx] + "\t\t" + ytouched[idx]);
            }
        }
    }

    public boolean isHinting() {
        return hinting;
    }

    public void setHinting(boolean hinting) {
        this.hinting = hinting;
    }

    /**
     * Execute the code necessary if the code has hinting. fpgm_ is executed only
     * once per font, while the cvp_ is executed when the scale of the document
     * changes.
     */
    private void executeHintingSetup() {

        if (mInterpreter == null) {

            getTable(OpenType.TAG_FPGM);
            byte[] rawFpgm = data_;
            fpgm_ = new int[rawFpgm.length];

            for (int idx = 0; idx < rawFpgm.length; idx++) {
                fpgm_[idx] = rawFpgm[idx] & 0xFF;
            }

            getTable(OpenType.TAG_CVT);
            mCvtTable = new Cvt();
            readCVT_(mCvtTable);

            mInterpreter = new Interpreter(mMaxpTable, this);

            mInterpreter.getGraphicsState().setCvtTable(mCvtTable);
            mInterpreter.setCvt(mCvtTable);
            GraphicsState setupState = new GraphicsState();
            setupState.setCvtTable(mCvtTable);

            TrueTypeGlyphData tempGlyph = new TrueTypeGlyphData(mMaxpTable);
            org.icepdf.core.pobjects.fonts.nfont.lang.Stack stack
                    = new org.icepdf.core.pobjects.fonts.nfont.lang.Stack();

            Interpreter.execute(tempGlyph, fpgm_, stack, setupState);

            // Executing the 'prep' table should only be done on a scale change
            // but we don't as yet support the notion of getting the scale out
            if (d != mCvtTable.scale) {
                // Scale the unscaled values in the table into scaled Values
                mCvtTable.scale = d;
                mCvtTable.scale(d);

                // Read and execute the CVT program when scale changes.
                getTable(OpenType.TAG_PREP);
                byte[] rawCvp_ = data_;
                cvp_ = new int[rawCvp_.length];
                for (int idx = 0; idx < rawCvp_.length; idx++) {
                    cvp_[idx] = rawCvp_[idx] & 0xFF;
                }

                mInterpreter.execute(tempGlyph, cvp_, stack, setupState);
                mInterpreter.saveState(setupState);
            }
        }
    }
}
