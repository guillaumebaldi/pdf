package org.icepdf.core.pobjects.fonts.nfont;

import org.icepdf.core.pobjects.fonts.nfont.doc.Dict;
import org.icepdf.core.pobjects.fonts.nfont.doc.PostScript;
import org.icepdf.core.pobjects.fonts.nfont.io.InputStreams;
import org.icepdf.core.pobjects.fonts.nfont.io.RandomAccess;
import org.icepdf.core.pobjects.fonts.nfont.io.RandomAccessByteArray;
import org.icepdf.core.pobjects.fonts.nfont.lang.*;

import java.awt.*;
import java.awt.geom.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Adobe PostScript Type 1 fonts (PFA, PFB, PFB/IBM)
 * and CFF aka Type 2 aka Type 1C (including CID).
 * Note that already created Multiple Master <em>instances</em> are in PFB format.
 * <p/>
 * <p>Features over {@link NFont}:
 * <ul>
 * <li>glyph rendering
 * <li>dynamic instantiation, from file or stream
 * <li>{@link #deriveFont(Encoding, CMap) re-encoding}
 * </ul>
 * <p/>
 * <p id='imp'>Implementation notes:
 * <ul>
 * <li>Renderering should be done on a {@link java.awt.Graphics2D} with the {@link java.awt.RenderingHints#KEY_ANTIALIASING antialiasing hint} set.
 * Embedded nfont hints are not used, as antialiasing largely obsoletes this.
 * <li>PFA/PFB fonts are read into memory in their entirety.
 * Most fonts with Western characters are small (&lt;100K), but fonts with Asian characters can be a megabyte or more.
 * CFF glyph data is read as needed.
 * <li>CFF can contain more than one nfont, however it rarely does and as used in PDF never does.
 * Only the first nfont is supported.
 * </ul>
 *
 * @author Copyright (c) 2003-2005  Thomas A. Phelps.  All rights reserved.
 * @version $Revision: 1.5 $ $Date: 2008/10/01 12:57:05 $
 * @see "Adobe Type 1 Font Format"
 */
public class NFontType1 extends NFontSimple implements Cloneable {
    private static final boolean DEBUG = false;

    private static final Logger logger =
            Logger.getLogger(NFontType1.class.toString());

//    public static final String COPYRIGHT = "Copyright (c) 2003 - 2005  Thomas A. Phelps.  All rights reserved.";

    /**
     * Type 1 format (non Compact).
     */
    public static final String FORMAT = "Type1";
    /**
     * Compact Font Format aka Type 2 aka Type 1C.
     */
    public static final String FORMAT_CFF = "CFF";

    /**
     * CFF/CID font format for type 0 .
     */
    public static final String FORMAT_CFF_CID_TYPE_0 = "CIDFontType0";

    /**
     * Encrypted portion as ASCII hex character pairs, as found in Mac OS 9 and earlier.
     */
    public static final String SUBFORMAT_PFA = "PFA";
    /**
     * Encrypted and segmented, as found in Windows.
     */
    public static final String SUBFORMAT_PFB_IBM = "PFB/IBM";
    /**
     * Encrypted and no segments, as found in PDF files.
     */
    public static final String SUBFORMAT_PFB = "PFB";
    /**
     * Decrypted and no segments and no trialing 512 "0"s.
     */
    public static final String SUBFORMAT_DECRYPTED = "decrypted";
    /**
     * CID-keyed CFF.
     */
    public static final String SUBFORMAT_CFF_CID = "CFF/CID";

    // Compact font format
    private boolean isCFF;

    /**
     * Length of <code>0000\ncleartomark\n</code> segment at end of exported PFB/PFA/PFA_IBM.
     */
    public static final int PFB_00_LENGTH = 8 * 65/*64 0's + \n*/ + "cleartomark\n".length();


    private static final int PFB_ASCII = 1, PFB_BINARY = 2, PFB_EOF = 3;
    private static final AffineTransform MATRIX_DEFAULT = new AffineTransform(0.001, 0.0, 0.0, -0.001, 0.0, 0.0);    // flip Y for Adobe coordinates => Java
    private static final GeneralPath GLYPH_ZERO_CONTOUR = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 0 + 1);

    // charstring operators
    private static final int CS_HSTEM = 1, CS_VSTEM = 3, CS_VMOVETO = 4, CS_RLINETO = 5, CS_HLINETO = 6, CS_VLINETO = 7,
            CS_RRCURVETO = 8, CS_CLOSEPATH = 9, CS_CALLSUBR = 10, CS_RETURN = 11, CS_ESCAPE = 12, CS_HSBW = 13, CS_ENDCHAR = 14, /*CS_BLEND=16--obsolete MM*/
            CS_HSTEMHM = 18, CS_HINTMASK = 19, CS_CNTRMASK = 20,
            CS_RMOVETO = 21, CS_HMOVETO = 22, CS_VSTEMHM = 23, CS_RCURVELINE = 24, CS_RLINECURVE = 25, CS_VVCURVETO = 26, CS_HHCURVETO = 27, CS_SHORTINT = 28, CS_CALLGSUBR = 29,
            CS_VHCURVETO = 30, CS_HVCURVETO = 31,
    // 2-byte escape codes
    CS_ESC_DX = CS_HVCURVETO + 1,
            CS_DIV = 12 + CS_ESC_DX,
    // Type 1 only (reserved in Type 2 charstring)
    CS_DOTSECTION = 0 + CS_ESC_DX, CS_VSTEM3 = 1 + CS_ESC_DX, CS_HSTEM3 = 2 + CS_ESC_DX, CS_SEAC = 6 + CS_ESC_DX, CS_SBW = 7 + CS_ESC_DX, CS_CALLOTHERSUBR = 16 + CS_ESC_DX, CS_POP = 17 + CS_ESC_DX, CS_SETCURRENTPOINT = 33 + CS_ESC_DX,
    // new in Type 2 charstring
    CS_AND = 3 + CS_ESC_DX, CS_OR = 4 + CS_ESC_DX, CS_NOT = 5 + CS_ESC_DX, CS_ABS = 9 + CS_ESC_DX, CS_ADD = 10 + CS_ESC_DX, CS_SUB = 11 + CS_ESC_DX, CS_NEG = 14 + CS_ESC_DX, CS_EQ = 15 + CS_ESC_DX, CS_DROP = 18 + CS_ESC_DX,
            CS_PUT = 20 + CS_ESC_DX, CS_GET = 21 + CS_ESC_DX, CS_IFELSE = 22 + CS_ESC_DX, CS_RANDOM = 23 + CS_ESC_DX, CS_MUL = 24 + CS_ESC_DX, CS_SQRT = 26 + CS_ESC_DX, CS_DUP = 27 + CS_ESC_DX, CS_EXCH = 28 + CS_ESC_DX, CS_INDEX = 29 + CS_ESC_DX, CS_ROLL = 30 + CS_ESC_DX,
            CS_HFLEX = 34 + CS_ESC_DX, CS_FLEX = 35 + CS_ESC_DX, CS_HFLEX1 = 36 + CS_ESC_DX, CS_FLEX1 = 37 + CS_ESC_DX;
    private static final String[] CS_NAME = {// for debugging (don't do disassembly)
            "-Reserved-", "hstem", "-Reserved-", "vstem", "vmoveto", "rlineto", "hlineto", "vlineto",
            "rrcurveto", "closepath", "callsubr", "return", "escape", "hsbw", "endchar",
            "-Reserved-", "-Reserved-", "-Reserved-",
            "hstemhm", "hintmask", "cntrmask",
            "rmoveto", "hmoveto", "vstemhm", "rcurveline", "rlinecurve", "vvcurveto", "hhcurveto", "shortint", "callgsubr",
            "vhcurveto", "hvcurveto",
            // 2-byte escape codes
            "dotsection", "vstem3", "hstem3", "and", "or", "not", "seac", "sbw", "-Reserved-",
            "abs", "add", "sub", "div", "-Reserved-", "neg", "eq",
            "callothersubr", "pop", "drop", "-Reserved-",
            "put", "get", "ifelse", "random", "mul", "-Reserved-", "sqrt", "dup", "exch", "index", "roll",
            "-Reserved-", "-Reserved-", "setcurrentpoint", "hflex", "flex", "hflex1", "flex1"
    };


    // *** Type 1 only ***
    // encryption keys
    private static final int R_EEXEC = 55665, R_CHARSTRING = 4330;
    private static final int C1 = 52845, C2 = 22719;

    // *** CFF only ***
    // standard strings
    private static final String[] CFF_SID = {
            ".notdef", "space",
            "exclam", "quotedbl", "numbersign", "dollar", "percent", "ampersand", "quoteright", "parenleft", "parenright", "asterisk", "plus", "comma", "hyphen", "period", "slash",
            "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
            "colon", "semicolon", "less", "equal", "greater", "question", "at",
            /*34*/"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
            "bracketleft", "backslash", "bracketright", "asciicircum", "underscore", "quoteleft",
            /*66*/"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
            /*92*/"braceleft", "bar", "braceright", "asciitilde", "exclamdown", "cent", "sterling", "fraction", "yen", "florin", "section", "currency",
            /*104*/"quotesingle", "quotedblleft", "guillemotleft", "guilsinglleft", "guilsinglright", "fi", "fl", "endash", "dagger", "daggerdbl", "periodcentered",
            /*115*/"paragraph", "bullet", "quotesinglbase", "quotedblbase", "quotedblright", "guillemotright", "ellipsis", "perthousand", "questiondown",
            "grave", "acute", "circumflex", "tilde", "macron", "breve", "dotaccent", "dieresis", "ring", "cedilla", "hungarumlaut", "ogonek", "caron",
            "emdash", "AE", "ordfeminine", "Lslash", "Oslash", "OE", "ordmasculine", "ae", "dotlessi", "lslash", "oslash", "oe", "germandbls",
            "onesuperior", "logicalnot", "mu", "trademark", "Eth", "onehalf", "plusminus", "Thorn", "onequarter", "divide", "brokenbar", "degree", "thorn",
            "threequarters", "twosuperior", "registered", "minus", "eth", "multiply", "threesuperior", "copyright",
            "Aacute", "Acircumflex", "Adieresis", "Agrave", "Aring", "Atilde", "Ccedilla", "Eacute", "Ecircumflex", "Edieresis", "Egrave", "Iacute", "Icircumflex", "Idieresis", "Igrave",
            "Ntilde", "Oacute", "Ocircumflex", "Odieresis", "Ograve", "Otilde", "Scaron", "Uacute", "Ucircumflex", "Udieresis", "Ugrave", "Yacute", "Ydieresis", "Zcaron",
            "aacute", "acircumflex", "adieresis", "agrave", "aring", "atilde", "ccedilla", "eacute", "ecircumflex", "edieresis", "egrave", "iacute", "icircumflex", "idieresis", "igrave",
            "ntilde", "oacute", "ocircumflex", "odieresis", "ograve", "otilde", "scaron", "uacute", "ucircumflex", "udieresis", "ugrave", "yacute", "ydieresis", "zcaron",

            "exclamsmall", "Hungarumlautsmall", "dollaroldstyle", "dollarsuperior", "ampersandsmall", "Acutesmall", "parenleftsuperior", "parenrightsuperior", "twodotenleader", "onedotenleader",
            "zerooldstyle", "oneoldstyle", "twooldstyle", "threeoldstyle", "fouroldstyle", "fiveoldstyle", "sixoldstyle", "sevenoldstyle", "eightoldstyle", "nineoldstyle",
            "commasuperior", "threequartersemdash", "periodsuperior", "questionsmall",
            "asuperior", "bsuperior", "centsuperior", "dsuperior", "esuperior", "isuperior", "lsuperior", "msuperior", "nsuperior", "osuperior", "rsuperior", "ssuperior", "tsuperior",
            "ff", "ffi", "ffl", "parenleftinferior", "parenrightinferior", "Circumflexsmall", "hyphensuperior", "Gravesmall",
            "Asmall", "Bsmall", "Csmall", "Dsmall", "Esmall", "Fsmall", "Gsmall", "Hsmall", "Ismall", "Jsmall", "Ksmall", "Lsmall", "Msmall", "Nsmall", "Osmall", "Psmall", "Qsmall", "Rsmall", "Ssmall", "Tsmall", "Usmall", "Vsmall", "Wsmall", "Xsmall", "Ysmall", "Zsmall",
            "colonmonetary", "onefitted", "rupiah", "Tildesmall", "exclamdownsmall", "centoldstyle", "Lslashsmall", "Scaronsmall", "Zcaronsmall", "Dieresissmall", "Brevesmall",
            "Caronsmall", "Dotaccentsmall", "Macronsmall", "figuredash", "hypheninferior", "Ogoneksmall", "Ringsmall", "Cedillasmall", "questiondownsmall",
            "oneeighth", "threeeighths", "fiveeighths", "seveneighths", "onethird", "twothirds",
            "zerosuperior", "foursuperior", "fivesuperior", "sixsuperior", "sevensuperior", "eightsuperior", "ninesuperior",
            "zeroinferior", "oneinferior", "twoinferior", "threeinferior", "fourinferior", "fiveinferior", "sixinferior", "seveninferior", "eightinferior", "nineinferior",
            "centinferior", "dollarinferior", "periodinferior", "commainferior",
            "Agravesmall", "Aacutesmall", "Acircumflexsmall", "Atildesmall", "Adieresissmall", "Aringsmall", "AEsmall", "Ccedillasmall", "Egravesmall", "Eacutesmall", "Ecircumflexsmall", "Edieresissmall", "Igravesmall", "Iacutesmall",
            "Icircumflexsmall", "Idieresissmall", "Ethsmall", "Ntildesmall", "Ogravesmall", "Oacutesmall", "Ocircumflexsmall", "Otildesmall", "Odieresissmall", "OEsmall", "Oslashsmall", "Ugravesmall", "Uacutesmall", "Ucircumflexsmall", "Udieresissmall", "Yacutesmall", "Thornsmall", "Ydieresissmall",
            "001.000", "001.001", "001.002", "001.003",
            "Black", "Bold", "Book", "Light", "Medium", "Regular", "Roman", "Semibold"
    };

    //private static final String[] CHARSET_ISOADOBE = new int[229]; => identity special case
    private static final int[] CHARSET_EXPERT = {// array of SIDs
            0, /*!*/ 1, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 13, 14, 15, 99, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 27, 28, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 109, 110,
            267, 268, 269, 270, 271, 272, 273, 274, 275, 276, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 318,
            158, 155, 163, 319, 320, 321, 322, 323, 324, 325, 326, 150, 164, 169, 327, 328, 329, 330, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357, 358, 359,
            360, 361, 362, 363, 364, 365, 366, 367, 368, 369, 370, 371, 372, 373, 374, 375, 376, 377, 378
    };
    private static final int[] CHARSET_EXPERT_SUBSET = {
            0, 1, 231, 232, 235, 236, 237, 238, 13, 14, 15, 99, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 27, 28, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266,
            109, 110, 267, 268, 269, 270, 272, 300, 301, 302, 305, 314, 315, 158, 155, 163, 320, 321, 322, 323, 324, 325, 326, 150, 164, 169,
            327, 328, 329, 330, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346
    };

    private static final String[] CFF_EXPERT_MAP = {
            ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef",
            ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef",
            "space", "exclamsmall", "Hungarumlautsmall", ".notdef", "dollaroldstyle", "dollarsuperior", "ampersandsmall", "Acutesmall", "parenleftsuperior", "parenrightsuperior", "twodotenleader", "onedotenleader", "comma",
            "hyphen", "period", "fraction", "zerooldstyle", "oneoldstyle", "twooldstyle", "threeoldstyle", "fouroldstyle", "fiveoldstyle", "sixoldstyle", "sevenoldstyle", "eightoldstyle", "nineoldstyle", "colon", "semicolon",
            "commasuperior", "threequartersemdash", "periodsuperior", "questionsmall", ".notdef", "asuperior", "bsuperior", "centsuperior", "dsuperior", "esuperior", ".notdef", ".notdef", ".notdef",
            "isuperior", ".notdef", ".notdef", "lsuperior", "msuperior", "nsuperior", "osuperior", ".notdef", ".notdef", "rsuperior", "ssuperior", "tsuperior", ".notdef",
            "ff", "fi", "fl", "ffi", "ffl", "parenleftinferior", ".notdef", "parenrightinferior", "Circumflexsmall", "hyphensuperior", "Gravesmall",
            "Asmall", "Bsmall", "Csmall", "Dsmall", "Esmall", "Fsmall", "Gsmall", "Hsmall", "Ismall", "Jsmall", "Ksmall", "Lsmall", "Msmall",
            "Nsmall", "Osmall", "Psmall", "Qsmall", "Rsmall", "Ssmall", "Tsmall", "Usmall", "Vsmall", "Wsmall", "Xsmall", "Ysmall", "Zsmall",
            "colonmonetary", "onefitted", "rupiah", "Tildesmall",
            ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef",
            ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef", ".notdef",
            "exclamdownsmall", "centoldstyle", "Lslashsmall", ".notdef", ".notdef", "Scaronsmall", "Zcaronsmall", "Dieresissmall", "Brevesmall", "Caronsmall", ".notdef", "Dotaccentsmall", ".notdef",
            ".notdef", "Macronsmall", ".notdef", ".notdef", "figuredash", "hypheninferior", ".notdef", ".notdef", "Ogoneksmall", "Ringsmall", "Cedillasmall", ".notdef", ".notdef", ".notdef",
            "onequarter", "onehalf", "threequarters", "questiondownsmall", "oneeighth", "threeeighths", "fiveeighths", "seveneighths", "onethird", "twothirds", ".notdef", ".notdef",
            "zerosuperior", "onesuperior", "twosuperior", "threesuperior", "foursuperior", "fivesuperior", "sixsuperior", "sevensuperior", "eightsuperior", "ninesuperior", "zeroinferior",
            "oneinferior", "twoinferior", "threeinferior", "fourinferior", "fiveinferior", "sixinferior", "seveninferior", "eightinferior", "nineinferior", "centinferior", "dollarinferior", "periodinferior", "commainferior",
            "Agravesmall", "Aacutesmall", "Acircumflexsmall", "Atildesmall", "Adieresissmall", "Aringsmall", "AEsmall", "Ccedillasmall", "Egravesmall", "Eacutesmall", "Ecircumflexsmall", "Edieresissmall", "Igravesmall", "Iacutesmall",
            "Icircumflexsmall", "Idieresissmall", "Ethsmall", "Ntildesmall", "Ogravesmall", "Oacutesmall", "Ocircumflexsmall", "Otildesmall", "Odieresissmall", "OEsmall", "Oslashsmall", "Ugravesmall", "Uacutesmall", "Ucircumflexsmall", "Udieresissmall", "Yacutesmall", "Thornsmall", "Ydieresissmall"
    };

    private static final Encoding CFF_ENCODING_STANDARD = new Encoding("CFF_Standard", Arrayss.subset(CFF_SID, 0, 229));    // different than Encoding.ADOBE_STANDARD as name[1]=space;
    private static final Encoding CFF_ENCODING_EXPERT = new Encoding("CFF_Expert", CFF_EXPERT_MAP);    // different than Encoding.MAC_EXPERT

    private static final String[] CFF_FLOAT = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "E", "E-", null, "-"};

    private static final String[] CFF_KEYS = {"version", "Notice", "FullName", "FamilyName",
            "Weight", "FontBBox", "BlueValues", "OtherBlues", "FamilyBlues", "FamilyOtherBlues", "StdHW", "StdVW",
            "escape", "UniqueID", "XUID", "charset", "Encoding", "CharStrings", "Private", "Subrs", "defaultWidthX", "nominalWidthX",
            // ESC
            "Copyright",
            "isFixedPitch", "ItalicAngle", "UnderlinePosition", "UnderlineThickness", "PaintType", "CharstringType", "FontMatrix", "StrokeWidth",
            "BlueScale", "BlueShift", "BlueFuzz", "StemSnapH", "StemSnapV", "ForceBold", null, null, "LanguageGroup", "ExpansionFactor",
            "initialRandomSeed", "SyntheticBase", "PostScript", "BaseFontName", "BaseFontBlend", null, null, null, null, null, null,
            "ROS", "CIDFontVersion", "CIDFontRevision", "CIDFontType", "CIDCount", "UIDBase", "FDArray", "FDSelect", "FontName"    // CID
    };
    private static final String[] FONTINFO_KEYS = {
            "version", "Notice", "Copyright", "FullName", "FamilyName", "Weight",
            "isFixedPitch", "ItalicAngle", "UnderlinePosition", "UnderlineThickness",
            "BaseFontName"
    };

    // parallel to CFF_TOP_KEYS.  position has type, or default (type implied).  null = n/a for dictionary type
    private static final Object TYPE_DELTA = new Object();    // TYPE_SID => ""
    private static final Object[] CFF_TOP_NORM = {"", "", "", "",
            "", new Object[]{Integers.ZERO, Integers.ZERO, Integers.ZERO, Integers.ZERO}, null, null, null, null, null, null,
            null, null, null, Integers.ZERO, Integers.ZERO, null, null, null, null, null,
            null,
            Integers.ZERO, Integers.ZERO, Integers.getInteger(100), Integers.getInteger(50), Integers.ZERO, Integers.TWO,
            new Object[]{0.001f, Floats.ZERO, Floats.ZERO, 0.001f, Floats.ZERO, Floats.ZERO}, Integers.ONE,
            null, null, null, null, null, null, null, null, null, null,
            null, null, "", "", TYPE_DELTA, null, null, null, null, null, null,
            null, Integers.ZERO, Integers.ZERO, Integers.ZERO, Integers.getInteger(8720), null, null, null, ""
    };
    private static final Object[] CFF_PRIVATE_NORM = {null, null, null, null,
            null, null, TYPE_DELTA, TYPE_DELTA, TYPE_DELTA, TYPE_DELTA, null, null,
            null, null, null, null, null, null, null, null, Integers.ZERO, Integers.ZERO,
            null,
            null, null, null, null, null, null, null, null,
            0.039625f, Integers.getInteger(7), Integers.ONE, TYPE_DELTA, TYPE_DELTA, Integers.ZERO/*false*/, null, null, Integers.ZERO, new Float(0.06),
            Integers.ZERO
    };

    static {    // checks on tables
//        assert CFF_SID[229]=="exclamsmall" && CFF_SID[274] == "Asmall" && CFF_SID[378] == "Ydieresissmall";
//        if (!("exclamsmall".equals(CFF_SID[229]) && "Asmall".equals(CFF_SID[274]) && "Ydieresissmall".equals(CFF_SID[378])))
//            throw new IllegalComponentStateException("Illegal Table State");
        //assert CHARSET_EXPERT.length == 166;
//        if (CHARSET_EXPERT.length != 166)
//            throw new IllegalComponentStateException("Illegal Table State");
        //assert CHARSET_EXPERT_SUBSET.length == 88: CHARSET_EXPERT_SUBSET;
//        if (CHARSET_EXPERT_SUBSET.length != 88)
//            throw new IllegalComponentStateException("Illegal Table State " + CHARSET_EXPERT_SUBSET);
        //assert CFF_EXPERT_MAP[53]=="fiveoldstyle";
//        if (!"fiveoldstyle".equals(CFF_EXPERT_MAP[53]))
//            throw new IllegalComponentStateException("Illegal Table State");
//        assert CFF_EXPERT_MAP[126]=="Tildesmall";
//        if ( "Tildesmall".equals(CFF_EXPERT_MAP[126]))
//            throw new IllegalComponentStateException("Illegal Table State");
//        assert CFF_EXPERT_MAP[219]=="nineinferior";
//        if ("nineinferior".equals(CFF_EXPERT_MAP[219]))
//            throw new IllegalComponentStateException("Illegal Table State");
//        assert CFF_EXPERT_MAP[254]=="Thornsmall";
//        if ("Thornsmall".equals(CFF_EXPERT_MAP[254]))
//            throw new IllegalComponentStateException("Illegal Table State");
    }


    private static final Rectangle2D BBOX_MISSING = new Rectangle2D.Double(-100.0, -250.0, 1000.0 - 100.0, 1000.0 - 250.0);    // buggy PageMaker 7.0 / Distiller 5.0.2 (tpadova)

    private static final float WIDTH_INVALID = Float.MIN_VALUE;

    private static final Random rand_ = new Random();


    /**
     * Primordial ur-instance.
     */
    private NFontType1 ur_;
    private RandomAccess ra_;
    private long raoff_;

    // definition of nfont
    //private Map<Object,Object> afm_ = null;	// AFM files rare and don't need for PDF
    private Dict dict_;
    /**
     * Character code => name.
     */
    private byte[][][] subrs_ = null;
    /**
     * CFF shared subrs
     */
    private byte[][] gsubrs_ = null;
    private int gbias_ = 0;
    /**
     * gid => charstring procedure.
     */
    private byte[][] charstrings_ = null;
    /**
     * Set of all CFF charstrings can be large so just keep offsets.
     */
    private int[] charstringsoff_ = null;

    private String format_, subformat_, subformatType_;
    private int rights_;
    private int hint_ = -1;    // -1=not computed, 0=no, 1=yes
    private int charstringType_;
    private int lenIV_ = 4;    // 4 assumed by version 23.0 of the PostScript interpreter, found in the original LaserWriter (Type 1 only)
    private int glyphcnt_;

    // caches and settings
    /**
     * In non-CID, mapping all the way from encoded character through encoding to glyph.
     * In CID, mapping from CID to GID.
     */
    private CMap c2g_;
    /**
     * Cache paths at given point size for performance.  Indexed by glyph so independent of encoding.
     * LATER: maybe antialias cache as bitmaps so can control scaling.
     */
    private SoftReference/*<Shape>*/[] paths_;    // share paths across point sizes
    /**
     * Widths, in glyph space, indexed by glyph (already did a mapping through glyph names).
     */
    /*package-private -- for OpenType*/
    float[] widths_ = null;
    /**
     * Per-glyph nfont dictionaries (shared).
     */
    private Dict[] fd_;
    /**
     * Glyph to nfont dictionary mapping: fds_[gid] = fd_ index.
     */
    private int[] fds_ = null;
    private Shape notdef_;
    private int spacech_ = Integer.MIN_VALUE;
    private int flags_;
    private int weight_;
    private AffineTransform u_;


    public NFontType1(URL source, String subFormatType) throws FontFormatException, IOException {
        super(source);
        parse(InputStreams.toByteArray(source.openStream(), 100 * 1024), subFormatType);
    }

    /**
     * Constructor used by OpenType.
     */
    /*package-private*/
    NFontType1(RandomAccess source, int offset, int length) throws FontFormatException, IOException {
        super(null);
        ra_ = source;
        ur_ = this;
        raoff_ = offset;
        //assert offset>=0;// && offset<ra_.length();
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0");
        }

        format_ = NFontOpenType.FORMAT;
        subformat_ = SUBFORMAT_NONE;

        try {
            getRA();

//        assert ra_.getFilePointer() == 0L;
//        if (ra_.getFilePointer() != 0L) {
//            throw new IllegalStateException();
//        }
//System.out.println("  cff +"+offset+".."+length+" "+ra_);
            if (length < 100 * 1024L) {
                // if small, read entirely and throw away ra_
                ra_.seek(raoff_);
                byte[] data = new byte[length];
                ra_.readFully(data);
                ra_.close();
                releaseRA();
                ra_ = new RandomAccessByteArray(data, "r");
                raoff_ = 0L;
            }
            parseCFF();
        } catch (Throwable e) {
            logger.log(Level.FINER, "Error parsing Type1 font.", e);
        }
        finally {
            if (ra_ instanceof RandomAccessByteArray) {
                ra_ = null;
            } else {
                releaseRA();
            }
        }

        // Type 1 encrypted and small so always read in entirety, CFF in PDF compressed -- only CFF in OpenType big enough and random access
        //assert SUBFORMAT_CFF_CID == getSubformat(); => not necessarily
    }

    /**
     * Creates instance of Type 1 nfont given <var>data</var>, automatically determining subformat (PFA, PFB, PFB/IBM, CFF, or CID).
     * <p/>
     * <p>Note: Data from non-<code>byte[]</code> sources can be easily converted,
     * from a {@link java.io.File} with {toByteArray(File)}
     * or from a {@link java.io.InputStream} with {toByteArray(InputStream, int)}.
     * <var>data</var> may be mutated.
     * <p/>
     * <p>PDF records the lengths of the clear text and encrypted portions of Type 1 (non-CFF) fonts.
     * This information is not used; instead the data are quickly scanned to derive these lengths.
     * <p/>
     * <!--
     * <p>By default the nfont uses its embedded encoding.
     * If a Unicode encoding is desired make new instance with <code><var>nfont</var> = <var>nfont</var>.deriveFont(Encoding.UNICODE, CMap.IDENTITY)</code>.
     * Return to the embedded encoding with <code><var>nfont</var> = <var>nfont</var>.deriveFont(null, null)</code>.
     * -->
     */
    public NFontType1(byte[] data, String subFormatType) throws FontFormatException, IOException {
        super(null);
        parse(data, subFormatType);
    }

    private void parse(byte[] data, String subFormatType) throws FontFormatException, IOException {
//        assert data!=null && data.length >= 10;
        if (data == null || data.length < 10) {
            throw new IllegalArgumentException("Data can not be null or have length < 10");
        }
        ur_ = this;
        subformatType_ = subFormatType;

        // determine format
        format_ = FORMAT;
        int major = data[0] & 0xff, offSize = data[3] & 0xff;
        if (0 <= major && major <= 10 && 0 <= offSize && offSize <= 4) {
            format_ = FORMAT_CFF;
            subformat_ = SUBFORMAT_NONE;
        } else if (data[0] == (byte) 0x80)
            subformat_ = SUBFORMAT_PFB_IBM;
        else { //assert data[0]=='!'; -- below
            int clen = getClen(data);    // use passed clen? override clen?
            if (clen == -1)
                throw new FontFormatException("not genuine PFB or PFA (maybe Type 3)");
            if (data[clen] == ' ')
                subformat_ = SUBFORMAT_DECRYPTED;
            else {
                subformat_ = SUBFORMAT_PFA;
                for (int i = clen, imax = clen + 50; i < imax; i++)
                    if (!Characters.isHexDigit((char) data[i] & 0xff)) {
                        subformat_ = SUBFORMAT_PFB;
                        break;
                    }
            }
        }

        if (FORMAT_CFF.equals(format_)) {
            ra_ = new RandomAccessByteArray(data, "r");
            raoff_ = 0L;
            parseCFF();
            isCFF = true;
            ra_ = null;    // read in full
        } else {
            data = normalize(data);
            if (data[0] == '%' && data[1] == '!')
                try {
                    parsePFB(data);
                } catch (IOException bogus) {
                }    // may convert byte[] into InputStream
            else
                throw new FontFormatException("no valid Type 1 nfont header (%! or 0x80)");
        }
    }

    //    public NFontType1 deriveFont(float pointsize) {
    public NFont deriveFont(float pointsize) {
        NFontType1 f = (NFontType1) super.deriveFont(pointsize);
        f.u_ = new AffineTransform(m_);
        f.u_.concatenate(f.at_);
        f.u_.scale(pointsize, pointsize);
        return f;
    }

    //    public NFontType1 deriveFont(AffineTransform at) {
    public NFont deriveFont(AffineTransform at) {
        NFontType1 f = (NFontType1) super.deriveFont(at);
        f.u_ = new AffineTransform(m_);
        f.u_.concatenate(at);
        f.u_.scale(f.size_, f.size_);
        return f;
    }

    //    public NFontType1 deriveFont(Encoding encoding, CMap toUnicode) {
    public NFontSimple deriveFont(Encoding encoding, CMap toUnicode) {
        if (encoding == null) encoding = ur_.encoding_;
        NFontType1 f = (NFontType1) super.deriveFont(encoding, toUnicode);
        f.setEncoding(encoding);
//System.out.println("encoding = "+encoding);
//System.out.println("widths arranged by encoding"); for (int i=0; i<256; i++) { int gid=c2g_.toSelector((char)i); System.out.print(" "+i+"/"+gid+"/"+widths_[gid]); } System.out.println();
        return f;
    }

    /**
     * Optimize encoded char to glyph translation.
     */
    private void setEncoding(Encoding encoding) {
//        assert encoding!=null;
        if (encoding == null) {
            throw new IllegalArgumentException("Encoding can not be null");
        }
        //if (encoding.equals(Encoding.ADOBE_STANDARD)) encoding = Encoding.ADOBE_STANDARD;
        encoding_ = encoding;
        CMap c2g;
        if (Encoding.IDENTITY == encoding) {
            c2g = CMap.IDENTITY;
        } else if (subformatType_ != null &&
                format_.equals(FORMAT_CFF) &&
                subformatType_.startsWith(FORMAT_CFF_CID_TYPE_0)) {
            // direct mapping, we can ignore encoding TYPE_0 OR TYPE_0c.
            c2g = CMap.IDENTITY;
            encoding_ = Encoding.IDENTITY;
        } else {
            c2g = encoding_.mapTo(intrinsic_);
        }

        setCID(c2g);
    }

    private void setCID(CMap cid) {
        c2g_ = cid;
        spacech_ = Integer.MIN_VALUE;
    }

    private void getRA() throws IOException, InterruptedException {
        if (ra_ == null) {
//            assert getSource()!=null;
            URL source = getSource();
            if (source == null) {
                throw new IllegalArgumentException("null URL source");
            }
            ra_ = getMultiplex().getRA(this/*ur_*/, source);
        }
    }

    /*package-private for OpenType*/
    void releaseRA() {
        if (ra_ != null /*&& !(ra_ instanceof RandomAccessByteArray)--if so fully read*/) {
            getMultiplex().releaseRA(ra_);
            ra_ = null;
        }
    }


    /**
     * Computes the length of the clear text.
     */
    public static int getClen(byte[] data) {
        int i = Arrayss.indexOf(data, Strings.getBytes8("eexec"));
        if (i == -1) return -1;    // not PFA/PFB
        i += 5;
        for (int c; ; i++)
            if ((c = data[i]) != '\r' && c != '\n' /*&& c!=' ' && c!='\t' -- correct but conflict with Compact v1*/)
                break;
//if (DEBUG) System.out.println("scanned lengths: "+clen+", "+elen+" (data length = "+data.length+")");
        // assert i>0 && data.length - i > 0: i + ", " + data.length;
        if (!(i > 0 && data.length - i > 0)) {
            throw new IllegalStateException(i + ", " + data.length);
        }
        return i;
    }

    /**
     * Parses unencrypted data into nfont dictionary.
     * Rather than implementing a full PostScript interpreter, relies on Adobe Type Manager Compatibility rules.
     * <p/>
     * <pre>
     * dict
     * /FontInfo -> dict
     * /Private -> dict
     * /CharStrings -> dict
     * /Metrics -> dict
     * /Encoding array
     * other entries
     * </pre>
     */
    private void parsePFB(byte[] data) throws FontFormatException, IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        PushbackInputStream pis = new PushbackInputStream(bis, 5);

        Object[] s = new Object[100];
        int si = 0;    // PostScript stack, not just PDF operands
        Dict dict = null;
        Dict[] d = new Dict[20];
        int di = 0;
        PostScript.eatSpace(pis);
        subrs_ = new byte[1][][];    // might not have /Subrs

        // for bad data
        Encoding en = Encoding.IDENTITY;
        intrinsic_ = Encoding.IDENTITY;
        boolean fhead = true;

        for (Object o; (o = PostScript.readObject(pis)) != null; ) {
            Class cl = o.getClass();

            if (PostScript.CLASS_COMMENT == cl) {
                // n/a
            } else if (PostScript.CLASS_NAME != cl)
                s[si++] = o;
            else if (((String) o).startsWith("/")) {
                // ATM special cases
                if ("/Encoding".equals(o)) {
                    o = PostScript.readObject(pis);
                    if ("StandardEncoding".equals(o)) {
                        PostScript.readObject(pis);    // "def"
                        en = Encoding.ADOBE_STANDARD;
                        /*} else if ("ISOLatin1Encoding".equals(o)) {
                            PostScript.readObject(pis);	// "def"
                            en = ENCODING_ISOLATIN1;*/
                    } else {
                        String[] map = new String[256];
                        Arrays.fill(map, NOTDEF);
                        while ((o = PostScript.readObject(pis)) != null) {
                            if ("def".equals(o) || "readonly".equals(o))
                                break;
                            else if ("dup".equals(o)) {    // ATM pattern: "dup <index> <charactername> put"
                                o = PostScript.readObject(pis);
                                if (PostScript.CLASS_INTEGER == o.getClass()) {    // Bakoma euex7.pfb has "dup dup  161 10 getinterval..."
                                    int inx = ((Number) o).intValue();
                                    map[inx] = Strings.valueOf(((String) PostScript.readObject(pis)).substring(1));    // "/A" => "A"
                                    //PostScript.readObject(pis);	// eat "put"
//System.out.println("en "+inx+" = "+cMap[inx]);
                                }
                            }
                        }
                        en = new Encoding("embedded", map);
                        if (Encoding.ADOBE_STANDARD.equals(en))
                            en = Encoding.ADOBE_STANDARD;
//if (DEBUG) System.out.println("normalized to ADOBE_STANDARD"); }
                    }
                    si = 0;

                } else if ("/Subrs".equals(o)) {
                    int size = ((Number) PostScript.readObject(pis)).intValue();
                    subrs_[0] = new byte[size][];
                    for (int i = 0; i < size; ) {    // "The integer immediately following /Subrs must be exactly the number of entries in the Subrs array." -- but TeX fonts violate this
                        o = PostScript.readObject(pis);
                        if ("dup".equals(o)) {    // ATM pattern: "dup <index> <nbytes> RD <~n~binary~bytes~> NP"
                            int inx = ((Number) PostScript.readObject(pis)).intValue();
                            subrs_[0][inx] = RD(pis);
                            i++;
                        } else if ("ND".equals(o) || "|-".equals(o) || "def".equals(o)/*THT not ATM: 'noaccess def' not ND*/)
                            break;
                    }
                    si = 0;
                    fhead = false;

                } else if ("/CharStrings".equals(o)) {
                    // create implicit mapping between names and glyphs by order in which read in
                    //assert charstrings_==null;	// assume only one /CharStrings block
                    if (charstrings_ != null) {
                        throw new IllegalStateException();
                    }
                    int size = ((Number) PostScript.readObject(pis)).intValue();
                    charstrings_ = new byte[size + 1][]; //assert private_==dict: dict;
                    String[] cname = new String[size + 1];
                    Arrays.fill(cname, ".notdef");
                    glyphcnt_ = size;
                    for (int i = 1; i <= size; ) {
                        o = PostScript.readObject(pis);
                        if (o == null)
                            throw new FontFormatException("insufficient data -- probably corrupt");
                        else if (PostScript.CLASS_NAME == o.getClass() && ((String) o).startsWith("/")) {    // ATM pattern: "<charactername> <nbytes> RD <~n~binary~bytes~> ND"
                            String cn = ((String) o).substring(1);    // shared with CFF_SID or Strings.getString()?
                            //int inx = i; if (".notdef".equals(cn)) inx=0;
                            cname[i] = cn;
                            charstrings_[i] = RD(pis);    // doesn't rely on specific definition, such as "RD" or "-|"
                            i++;
                        } else if ("end".equals(o)) {
                            glyphcnt_ = i - 1;
                            break;
                        }    // have seen fewer entries than size
                    }
                    intrinsic_ = new Encoding("induced", cname);
//System.out.println("CharStrings "+size+" used, "+charstrings_.length+" allocated");
                    si = 0;
                    fhead = false;

                    //} else if ("/Private".equals(o)) { => normal dictionary reading
                    //	si = 0;

                } else
                    s[si++] = ((String) o).substring(1);

            } else {    // executable name
                String op = (String) o;
                int newsi = 0;

                if ("definefont".equals(op))
                    break;
                else if ("readonly".equals(op)) {    // don't clear stack
                    newsi = si;

                    // parsing
                } else if ("[".equals(op)) {
                    List l = new ArrayList(10);    // just numbers in PostScript
                    int nest = 1;
                    while (nest > 0) {
                        o = PostScript.readObject(pis);
                        if (o == null || "]".equals(o)) {
                            nest--;
                            if (nest > 0) l.add(o);
                        } else
                            l.add(o);
                    }
                    s[si++] = l;
                    newsi = si;

                } else if (!fhead) {
                    // some Adobe software buggy: duplicate nfont dict within eexec with bogus data!
                }

                // dictionary
                else if ("dict".equals(op)) {    // size - dict
                    int size = ((Number) s[si - 1]).intValue();
                    Dict newdict = new Dict(size);
                    if (dict_ == null)
                        dict_ = newdict;
                    else if (si >= 2) dict.put(s[si - 2], newdict);
                    dict = newdict;
                } else if ("end".equals(op)) {    // (remove dictionary from dictionaries stack)
                    di--;
                    dict = di > 0 ? d[di - 1] : dict_;
                } else if ("def".equals(op)) {    // key value def - (defined in current dictionary)
                    if (si >= 2) {
                        dict.put(s[si - 2], s[si - 1]);
                        if ("lenIV".equals(s[si - 2]))
                            lenIV_ = ((Number) s[si - 1]).intValue();    // not 4 in texmf/fonts/type1/public/cm-super/*
                    }

                } // else ignore everything else

                si = newsi;
            }
        }
        pis.close();

        charstringType_ = 1;
        if (charstrings_ == null) charstrings_ = new byte[0][0];    // bad data
        setEncoding(en);
        touni_ = en.guessToUnicode();
        setup();
    }

    /**
     * Eats ATM pattern "... <nbytes> RD <~n~binary~bytes~> NP" and returns <code>byte[]</code> with charstring bytecode.
     */
    private byte[] RD(PushbackInputStream pis) throws FontFormatException, IOException {
        int c = pis.read();
        if (!('0' <= c && c <= '9'))
            throw new FontFormatException("damaged nfont: expected number (0x30..0x39) in /CharStrings RD but saw 0x" + Integer.toHexString(c));
        pis.unread(c);
        int len = ((Number) PostScript.readObject(pis)).intValue() - lenIV_;
        //assert len <= 0xffff: len;	// T1_SPEC: "Individual tokens and charstrings may not exceed 65,535 characters in length."
        if (len > 0xffff) {
            throw new IllegalStateException(len + "");
        }

        // "exactly one blank character" after so that encrypted string can start with whitespace
        //X Object o = PostScript.readObject(pis);	//assert "RD".equals(o) || "-|".equals(o): o; => can read too much following whitespace
        for (c = pis.read(); c != ' ' && c != '\t' && c != '\n' && c != '\r'; c = pis.read()) {
        }    // doesn't matter what it is (RD or -| or whatever)
        // just read exactly one blank, which just did above

        for (int i = 0; i < lenIV_; i++) pis.read();    // random bytes
        byte[] b = new byte[len];
        InputStreams.readFully(pis, b);
        //decrypt(R_CHARSTRING, b,0,len); -- already done in normalization
        PostScript.eatSpace(pis);
        return b;
    }

    /**
     * Fully decrypts Type 1 data, both full file and additional charstrings.
     * If encrypted portion is PFA, transformed to binary.
     */
    private static void decrypt(byte[] data, int clen, int elen) {
        //assert data!=null && data.length > 0 && clen > 0 && elen > 0: data + " " + clen + " " + elen;
        if (data == null || data.length <= 0 || clen <= 0 || elen <= 0) {
            throw new IllegalArgumentException(data + " " + clen + " " + elen);
        }
        byte[] b = data;

        // already decrypted, as from Compact PDF v1?
        boolean falready = true;
        for (int i = 0; i < 4/*NOT lenIV*/; i++)
            if (b[i + clen] != ' ') {
                falready = false;
                break;
            }
        if (falready) return;


        // LATER: verify "eexec"
        decrypt(R_EEXEC, b, clen, elen);

        // must scan for lenIV: "/lenIV 0 def"
        int lenIV = 4;
        int x = Arrayss.indexOf(b, Strings.getBytes8("lenIV"));
        if (x >= 0) {
            x += "lenIV".length();
            while (Character.isWhitespace((char) b[x])) x++;
            lenIV = 0;
            while (Character.isDigit((char) b[x])) {
                lenIV = lenIV * 10 + b[x] - '0';
                x++;
            }
        }

        for (int j = 0; j < 4/*NOT lenIV*/; j++)
            b[clen + j] = (byte) ' ';    // wipe random bytes

        // look for "RD" or "-|" preceded by a number
        for (int i = clen, imax = i + elen - 1, c = 0; i < imax; i++) {
            if (((b[i] == 'R' && b[i + 1] == 'D') || (b[i] == '-' && b[i + 1] == '|')) && b[i - 1] == ' ' && (c = b[i - 2]) >= '0' && c <= '9') {
                int cnt = c - '0';
                for (int j = i - 3, mul = 10; ; j--, mul *= 10)
                    if ((c = b[j]) >= '0' && c <= '9') cnt += (c - '0') * mul;
                    else break;
                i += 3;    // "RD " or "-| " or ...
                decrypt(R_CHARSTRING, b, i, cnt);
                for (int j = 0; j < lenIV; j++)
                    b[i + j] = (byte) 'X';    // normalize random bytes for compression, but don't make whitespace because data could start with space!
                i += cnt;
            }
        }
    }

    /**
     * Encrypts <var>data</var>.
     */
    private static void encrypt(byte[] data, int clen, int elen) {
        //assert data!=null && data.length >= clen + elen && clen > 0 && elen > 0: data.length;
        if (data == null || data.length < clen + elen || clen <= 0 || elen <= 0) {
            throw new IllegalArgumentException(data.length + "");
        }
        byte[] b = data;
        // independent of lenIV since initial random bytes of eexec is fixed at 4, and RD gives length of charstring including lenIV
        for (int i = clen, imax = i + elen - 1, c = 0; i < imax; i++) {
            if (((b[i] == 'R' && b[i + 1] == 'D') || (b[i] == '-' && b[i + 1] == '|')) && b[i - 1] == ' ' && (c = b[i - 2]) >= '0' && c <= '9') {
                int cnt = c - '0';
                for (int j = i - 3, mul = 10; ; j--, mul *= 10)
                    if ((c = b[j]) >= '0' && c <= '9') cnt += (c - '0') * mul;
                    else break;
                i += 3;
                encrypt(R_CHARSTRING, b, i, cnt);
                i += cnt;
            }
        }

        while (true) {
            encrypt(R_EEXEC, b, clen, elen);
            int c = b[clen] & 0xff;
            if (c != ' ' || c != '\t' || c != '\n' || c != '\r')
                break;    // no whitespace allowed at start of encrypted portion
            encrypt(R_EEXEC, b, clen, elen);
            b[clen]++;    // try again: unencrypt and twiddle first random byte
        }
    }

    private static void decrypt(int r, byte[] b, int off, int len) {
        for (int i = off, imax = off + len; i < imax; i++) {
            int cipher = b[i] & 0xff;
            b[i] = (byte) (cipher ^ (r >> 8));
            r = ((cipher + r) * C1 + C2) & 0xffff;
        }
    }

    private static void encrypt(int r, byte[] b, int off, int len) {
        for (int i = off, imax = off + len; i < imax; i++) {
            int plain = b[i] & 0xff;
            int cipher = (plain ^ (r >> 8)) & 0xff;
            b[i] = (byte) cipher;
            r = ((cipher + r) * C1 + C2) & 0xffff;
        }
    }


    /**
     * @see "Adobe Technical Note #5176, The Compact Font Format Specification"
     */
    private void parseCFF() throws IOException {
        RandomAccess ra = ra_;
        ra.seek(raoff_);

        // 1. fixed positions for first 5 data structured in fixed order -- must be read to know sizes
        // a. Header
        int major = ra.read(), minor = ra.read();
        int hdrSize = ra.read(), offSize = ra.read();
        ra.seek(raoff_ + hdrSize);

        // b. Name INDEX -- list of fonts
        int[] offs = readINDEX(ra);
        int cnt = offs.length - 1;
        byte[] data;
        ra.readFully(data = new byte[offs[cnt] - offs[0]]);
        String[] names = new String[cnt];
        for (int i = 0, off0 = offs[0]; i < cnt; i++) {
            if (offs[i] - off0 <= 0/*0-length, as in Klatchen*/ || data[offs[i] - off0] == 0/*name deleted*/)
                names[i] = "";
            else
                try {
                    names[i] = new String(data, offs[i] - off0, offs[i + 1] - offs[i], "US-ASCII");
                } catch (java.io.UnsupportedEncodingException canthappen) {
                }
        }
//        assert cnt==1: cnt;	// report unusual cases.  always 1 in PDF
        if (cnt != 1) {
            throw new IllegalStateException(cnt + "");
        }

        // c. Top DICT INDEX
        offs = readINDEX(ra);
//        assert offs.length == 1 + 1;
        if (offs.length != 2) {
            throw new IllegalStateException();
        }
        Dict top = dict_ = readDICT(ra, offs[0], offs[1] - offs[0], new Dict(cnt * 4 / 3));
        Object o = top.get("CharstringType");
        charstringType_ = o != null ? ((Number) o).intValue() : 2;

        // d. String INDEX
        offs = readINDEX(ra);
        cnt = offs.length - 1;
        ra.readFully(data = new byte[offs[cnt] - offs[0]]);
        String[] SI = new String[cnt];
        for (int i = 0, off0 = offs[0]; i < cnt; i++) {
            try {
                SI[i] = new String(data, offs[i] - off0, offs[i + 1] - offs[i], "US-ASCII");
            } catch (java.io.UnsupportedEncodingException canthappen) {
            }
        }

        // e. Global Subr INDEX (none in PDF)
        offs = readINDEX(ra);
        cnt = offs.length - 1;
        gsubrs_ = new byte[cnt][];
        gbias_ = charstringType_ == 1 ? 0 : cnt < 1240 ? 107 : cnt < 33900 ? 1131 : 32768;
        for (int i = 0; i < cnt; i++) {
            ra.readFully(gsubrs_[i] = new byte[offs[i + 1] - offs[i]]);
        }

        // 2. read rest of tables, referenced by offset
        // a. CharStrings
        int off = ((Number) top.get("CharStrings")).intValue();
        ra.seek(off + raoff_);
        offs = readINDEX(ra);
        cnt = offs.length - 1;
        int maxGlyph = cnt;
        if (ra instanceof RandomAccessByteArray) {    // => offs[cnt](==length) < 100K? or < cnt*4 * 2
            charstrings_ = new byte[cnt][];
            charstringsoff_ = null;
            for (int i = 0; i < maxGlyph; i++)
                ra.readFully(charstrings_[i] = new byte[offs[i + 1] - offs[i]]);
        } else {
//System.out.println("incremental charstrings, length = "+offs[cnt]);
            charstrings_ = null;
            charstringsoff_ = offs;
            ra.seek(offs[cnt] + raoff_);
        }
        glyphcnt_ = 0;
        for (int i = 0; i < maxGlyph; i++)
            if (offs[i] < offs[i + 1]) glyphcnt_++;

        // b. charset: GID => name
        o = top.get("charset");
        int[] chs = new int[maxGlyph];    // glyph => SID or CID
        if (o == null || (off = ((Number) o).intValue()) == 0)
            chs = null;    // ISOAdobe -- identity
        else if (off == 1)
            chs = CHARSET_EXPERT;
        else if (off == 2)
            chs = CHARSET_EXPERT_SUBSET;
        else {
            ra.seek(off + raoff_);
            int format = ra.read();
            if (format == 0) {
                for (int i = 1; i < maxGlyph; i++, off += 2)
                    chs[i] = (ra.read() << 8) | ra.read();
                //chs[0] = 0;
            } else {
//                assert format==1 || format == 2: format;
                if (!(format == 1 || format == 2)) {
                    throw new IllegalStateException(format + "");
                }
                for (int i = 1; i < maxGlyph; ) {
                    int first = (ra.read() << 8) | ra.read();
                    int nLeft = format == 1 ? ra.read() : (ra.read() << 8) | ra.read();
                    for (int j = 0; j <= nLeft && i < chs.length; j++) {
                        chs[i++] = first + j;
                    }
                }
            }
        }

        // CFF CID-keyed fonts have a CIDFontName in the name index and a
        // corresponding Top DICT.  The Top Dict begins with ROS operator which
        // specifies teh Registry-Ordering-supplement for the the font.  This
        // will indicate to a CFF parser that special CID processing should be
        // applied to tis font.
        // encoding + /Private
        if (top.get("ROS") != null) {
            subformat_ = SUBFORMAT_CFF_CID;
            parseCFF_CID(ra, top, chs, SI);
        }
        // The “CFF” font program has a Top DICT that does not use CIDFont operators:
        // The CIDs shall be used directly as GID values, and the glyph procedure
        // shall be retrieved using the CharStrings INDEX. Detection happens
        // in the setEncoding method.
        else {
            parseCFF_CFF(ra, top, chs, SI);
        }


        // 2. (continued)
        // e. Subrs
        if (!SUBFORMAT_CFF_CID.equals(subformat_)) {
            //assert fd_==null;
            if (fd_ != null) {
                throw new IllegalStateException();
            }
            fd_ = new Dict[1];
            fd_[0] = dict_;
        }
        int fdcnt = fd_.length;
        subrs_ = new byte[fdcnt][][];
        for (int i = 0, imax = fdcnt; i < fdcnt; i++) {
            Dict priv = (Dict) fd_[i].get("Private");
            o = priv.get("Subrs");
            if (o != null) {
                ra.seek(/*privoff: normalized (self)=>(0)+*/ ((Number) o).intValue() + raoff_);
                offs = readINDEX(ra);
                cnt = offs.length - 1;
                subrs_[i] = new byte[cnt][];
                for (int j = 0; j < cnt; j++)
                    ra.readFully(subrs_[i][j] = new byte[offs[j + 1] - offs[j]]);
            }
        }


        // 3. normalize to Type 1 format
        // a. default, SID/delta
        normalizeDict(top, CFF_KEYS, CFF_TOP_NORM, SI);
        // b. Top + private => Type 1 dict -> (FontInfo, private)
        Dict fi = new Dict(13);
//        for (String key : FONTINFO_KEYS) {
        for (int i = FONTINFO_KEYS.length - 1; i >= 0; i--) {
            Object val = top.remove(FONTINFO_KEYS[i]);
            if (val != null) fi.put(FONTINFO_KEYS[i], val);
        }
        if (fi.size() > 0) top.put("FontInfo", fi);
        if (SUBFORMAT_CFF_CID.equals(subformat_))
            for (int i = 0, imax = fd_.length; i < imax; i++) {
                if (fd_[i].get("FontName") == null) {
                    // some names are names[0]= "" names.length = 1
                    if (i < names.length) {
                        fd_[i].put("FontName", names[i]);
                    }
                }
            }
        else {
            //assert dict_.get("FontName") == null;
            if (dict_.get("FontName") != null) {
                throw new IllegalStateException();
            }

            dict_.put("FontName", names[0]);
        }    // first one only -- no /FontName operator


        setup();
    }

    private void parseCFF_CFF(RandomAccess ra, Dict top, int[] chs, String[] SI) throws IOException {
        // 2. rest of tables
        // b. charset => cname_

        if (chs == null)
            intrinsic_ = CFF_ENCODING_STANDARD;
            //else if (CHARSET_EXPERT==chs) CFF_ENCODING_EXPERT;
        else {
            //*assert?*/ chs[0]=NOTDEF_CHAR;	// for char 0: take whatever, set to NOTDEF, assert==NOTDEF?
            String[] cname = new String[chs.length];
            for (int i = 0, imax = chs.length; i < imax; i++)
                cname[i] = getSID(chs[i], SI);
            intrinsic_ = new Encoding("intrinsic", cname);
        }

        // c. Encoding: incoming number => GID ... => name.  In PDF, external to nfont data.
        Object o = top.get("Encoding");
        Encoding en;
        if (o == null) {
            en = Encoding.ADOBE_STANDARD;//{ en = null; assert false: "CID encoding"; }// => CID
        } else {
            int off = ((Number) o).intValue();
            if (off == 0)
                en = Encoding.ADOBE_STANDARD;
            else if (off == 1)
                en = CFF_ENCODING_EXPERT;
            else {
                ra.seek(off + raoff_);
                int format = ra.read();
                boolean fsupp = ((format & 0x80) != 0);
                format &= 0x7f;
                String[] map = new String[256];
                int maxch = 0;
                if (format == 0) {
                    int nCodes = ra.read();
                    for (int i = 1; i <= nCodes; i++)
                        map[ra.read()] = chs != null ? getSID(chs[i], SI) : CFF_SID[i];

                } else {
//                    assert format==1;
                    if (format != 1) {
                        throw new IllegalStateException();
                    }
                    // ranges
                    int nRanges = ra.read();
                    for (int i = 0, inx = 1; i < nRanges; i++) {
                        int first = ra.read(), nLeft = ra.read();
                        for (int j = 0; j <= nLeft; j++, inx++) {
                            map[first + j] = chs != null ? getSID(chs[inx], SI) : CFF_SID[inx];
                        }
                    }
                }
                if (fsupp) {
                    int nSups = ra.read();
                    for (int i = 0; i < nSups; i++, off += 3)
                        map[ra.read()] = getSID((ra.read() << 8) | ra.read(), SI);
                }

                map[0] = NOTDEF;
                en = new Encoding("custom", map);
            }
        }

        // d. Private
        Object[] oa = (Object[]) top.get("Private");
        int privoff = ((Number) oa[1]).intValue();    // Private has some (self) offsets
        Dict priv = readDICT(ra, privoff, ((Number) oa[0]).intValue(), new Dict(13));
        normalizeDict(priv, CFF_KEYS, CFF_PRIVATE_NORM, SI);
        o = priv.get("Subrs");
        if (o != null)
            priv.put("Subrs", Integers.getInteger(((Number) o).intValue() + privoff));
        top.put("Private", priv);

        setEncoding(en);
        touni_ = en.guessToUnicode();
    }

    private void parseCFF_CID(RandomAccess ra, Dict top, int[] chs, String[] SI) throws IOException {
        intrinsic_ = Encoding.IDENTITY;

        int off = ((Number) top.get("FDArray")).intValue();
        ra.seek(off + raoff_);
        int[] offs = readINDEX(ra);
        int cnt = offs.length - 1;
        //cnt>1 seen in isaacs_reliablepdf1103.pdf
        fd_ = new Dict[cnt];
        for (int i = 0; i < cnt; i++) {
            Dict d = readDICT(ra, offs[i], offs[i + 1] - offs[i], new Dict(13));
            normalizeDict(d, CFF_KEYS, CFF_TOP_NORM, SI);

            // /Private apparently undocumented but seems to be (length, offset(0)) pair
            Object[] oa = (Object[]) d.get("Private");
            off = ((Number) oa[1]).intValue();
            int len = ((Number) oa[0]).intValue(), privoff = off;
            Dict priv = readDICT(ra, off, len, new Dict(13));
            normalizeDict(priv, CFF_KEYS, CFF_PRIVATE_NORM, SI);
            Object o = priv.get("Subrs");
            if (o != null)
                priv.put("Subrs", Integers.getInteger(((Number) o).intValue() + privoff));
            d.put("Private", priv);
            fd_[i] = d;
        }

        // CID
        int maxGlyph = chs.length;
        char[] cid = new char[maxGlyph];
        int cmin = Integer.MAX_VALUE, cmax = Integer.MIN_VALUE;
        for (int i = 0; i < maxGlyph; i++) {
            int c = chs[i];
            cid[i] = (char) c;
            if (c < cmin) cmin = c;
            else if (c > cmax) cmax = c;
        }
        firstch_ = cmin;
        lastch_ = cmax;

        // FDSelect
        off = ((Number) top.get("FDSelect")).intValue();
        ra.seek(off + raoff_);
        int format = ra.read();
        int[] fds = new int[maxGlyph];    // apparently Adobe assumes init to 0, as seen in 17157-yvl7-10r.pdf's object 556
        if (format == 0) {
            for (int i = 0; i < maxGlyph; i++) fds[i] = ra.read();

        } else {
//            assert format==3: format;
            if (format != 3) {
                throw new IllegalStateException(format + "");
            }
            int nRanges = (ra.read() << 8) | ra.read();
            int first = (ra.read() << 8) | ra.read();
            for (int i = 0, next; i < nRanges; i++, first = next) {
                int fd = ra.read();
                next = (ra.read() << 8) | ra.read();
                for (int j = first; j < next; j++) fds[j] = fd;
            }
        }
        fds_ = fds;

        setCID(new CMap(cid).reverse()); //touni_ = CMap.IDENTITY; ?
    }

    private Dict readDICT(RandomAccess ra, int offset, int len, Dict dict) throws IOException {
        Object[] s = new Object[256];
        int si = 0;
        ra.seek(offset + raoff_);
        for (int i = 0; i < len; i++) {
            int v = ra.read();
            if (v <= 27) {    // operator
//                assert si>=1 && v <= 21;
                if (!(si >= 1 && v <= 21)) {
                    throw new IllegalStateException();
                }
                Object val = si == 1 ? s[0] : Arrayss.subset(s, 0, si);    // array
                String key;
                if (v != 12) {
                    key = CFF_KEYS[v];
                } else {
                    key = CFF_KEYS[22 + ra.read()];
                    i++;
                }
                if (key != null/*seen in O'Really Myriad-CnSemiBold*/)
                    dict.put(key, val);    // client can convert SID and offsets
                si = 0;
            } else if (v == 28) {
                s[si++] = Integers.getInteger(((byte) ra.read() << 8) | ra.read());
                i += 2;
            } else if (v == 29) {
                s[si++] = Integers.getInteger(((byte) ra.read() << 24) | (ra.read() << 16) | (ra.read() << 8) | ra.read());
                i += 4;
            } else if (v == 30) {    // floating point in non-optimal format
                StringBuilder sb = new StringBuilder(10);
                while (true) {
                    v = ra.read();
                    i++;
                    int nyb = v >> 4;
//                    assert 0 <= nyb && nyb <= 0xf: Integer.toHexString(v) + " / " + nyb;
                    if (!(0 <= nyb && nyb <= 0xf)) {
                        throw new IllegalStateException(Integer.toHexString(v) + " / " + nyb);
                    }
                    if (nyb == 0xf) break;
                    else sb.append(CFF_FLOAT[nyb]);
                    nyb = v & 0xf;
                    if (nyb == 0xf) break;
                    else sb.append(CFF_FLOAT[nyb]);
                }
                float f = sb.length() > 0 ? Float.parseFloat(sb.toString()) : 0f;
                s[si++] = f;
            } else if (v == 31) {
//                assert false: v;
                throw new IllegalStateException(v + "");
            } else if (v <= 246)
                s[si++] = Integers.getInteger(v - 139);
            else if (v <= 250) {
                s[si++] = Integers.getInteger(((v - 247) << 8) + ra.read() + 108);
                i++;
            } else if (v <= 254) {
                s[si++] = Integers.getInteger(-((v - 251) << 8) - ra.read() - 108);
                i++;
            } else {
                //assert false: v;
                throw new IllegalStateException(v + "");
            }
        }
        return dict;
    }

    /**
     * Returns offsets, letting caller interpret data.
     * Offsets are relative to initial passed offset <var>i</var> (non-existent still 0, not 0+<var>i</var>),
     * <b>not</b> "byte before data" as found in raw data.
     * Last offset gives end of data.
     * File pointer is moved to start of data.
     */
    private int[] readINDEX(RandomAccess ra) throws IOException {
        int offset = (int) (ra.getFilePointer() - raoff_);
        int count = (ra.read() << 8) | ra.read();
        int[] offs = new int[count + 1];
        if (count == 0) return offs;
        int offSize = ra.read();
        for (int i = 0, imax = offs.length, dx = offset + 3 + (count + 1) * offSize - 1/*relative to byte preceding data*/; i < imax; i++) {
            int x = 0;
            for (int j = 0; j < offSize; j++) x = (x << 8) | ra.read();
            offs[i] = x + dx;
//            assert i==0 || offs[i] >= offs[i - 1];	// monotonically increasing
            if (!(i == 0 || offs[i] >= offs[i - 1])) {
                throw new IllegalStateException();
            }
        }
        ra.seek(offs[0] + raoff_);    // usually +0?
        return offs;
    }

    private String getSID(int n, String[] SI) {
        return n < CFF_SID.length ? CFF_SID[n] : SI[n - CFF_SID.length];
    }

    /**
     * Replaces SID with String, delta with absolute, absent value with default.
     */
    private void normalizeDict(Dict dict, String[] keys, Object[] norms, String[] SI) {
        for (int i = 0, imax = norms.length; i < imax; i++) {
            String key = keys[i];
            Object val, norm = norms[i];
            if (key == null || null == norm) {
                // n/a
            } else if ((val = dict.get(key)) != null) {    // possible conversion
                if ("" == norm) {
                    dict.put(key, getSID(((Number) val).intValue(), SI));
                } else if (TYPE_DELTA == norm) {    // delta
                    Object[] oa = val instanceof Object[] ? (Object[]) val : new Object[]{val};    // 1-element array not parsed as array
                    float last = 0f;
                    for (int j = 0, jmax = oa.length; j < jmax; j++) {
                        //assert oa[j] instanceof Integer: oa[j];
                        last += ((Number) oa[j]).floatValue();    // assume all float's for now
                        oa[j] = Floats.getFloat(last);
                    }
                }
            } else {    // val==null, possible default
                // copy over the default value.
                if ("" != norm && TYPE_DELTA != null) dict.put(key, norm);
            }
        }
    }


    /**
     * Set operational values from dictionary, shared by Type 1 and CFF.
     */
    private void setup() {
        // need to check for CFF_CID as it changes what dictionary we look at.
        Dict cffDic = (SUBFORMAT_CFF_CID.equals(subformat_) ? fd_[0] : dict_);

        // normally we go for the font matrix from the cffDic but for some reason we have a couple
        // SUBFORMAT_CFF_CID where the fontmatrix is 1 0 0 1 0 0 which is incorrect for render.
        // probably something else going on but this seems to be good enough for now.
        float[] m = getFloats(cffDic.get("FontMatrix"));
        if (m[0] == 1.0f && m[1] == 0.0f) {
            m = getFloats(dict_.get("FontMatrix"));
        }
        m_ = m != null ? new AffineTransform(m[0], m[1], m[2], -m[3], m[4], m[5]) : MATRIX_DEFAULT;
        u_ = m_;

        Object o = cffDic.get("FontBBox");
        if (o != null) {
            float[] f = getFloats(o);
            bbox_ = new Rectangle2D.Double(f[0], f[1], f[2], f[3]);
        }
        if (o == null || (bbox_.getWidth() < 10.0 && bbox_.getHeight() > 2000.0) || (bbox_.getWidth() == 0.0 && bbox_.getHeight() == 0.0))
            bbox_ = BBOX_MISSING;

        int maxGlyph = getMaxGlyphNum();    // can have more than 256 glyphs, if only encode and display a faction
        //int len = Math.min(names.length, 256);
        //names[0] = NOTDEF;
        paths_ = new SoftReference[maxGlyph];    // indexed by glyph (not character)

        // widths arranged by char # (not glyph #, which CFF has but Type 1 doesn't)
        // set up widths table so know advance
        if (!NFontOpenType.FORMAT.equals(getFormat())) {    // not OpenType, which supplies widths and lsb from separate table
            widths_ = new float[maxGlyph];
            float[] dw = null, nw = null;
            boolean fcff = FORMAT_CFF.equals(getFormat());
            if (fcff) {
                int fdcnt = fd_.length;
                dw = new float[fdcnt];
                nw = new float[fdcnt];
                for (int i = 0; i < fdcnt; i++) {
                    Dict priv = (Dict) (SUBFORMAT_CFF_CID.equals(subformat_) ? fd_[i] : dict_).get("Private");
                    //assert priv!=null;
                    if (priv == null) {
                        throw new IllegalStateException();
                    }
                    o = priv.get("defaultWidthX");
                    dw[i] = o != null ? ((Number) o).floatValue() : 0f;
                    o = priv.get("nominalWidthX");
                    nw[i] = o != null ? ((Number) o).floatValue() : 0f;
                    //dw==0f && nw==0f is OK
                }
            } //else lsb_ = new float[maxGlyph];	// all 0 in CFF or separate table in OpenType

            for (int i = 0; i < maxGlyph; i++) {
                byte[] cs = charstrings_[i];
                if (cs == null) continue;
                float w = buildChar(i, null);
                int fd = fds_ == null ? 0 : fds_[i];
                if (fcff)
                    w = WIDTH_INVALID == w ? dw[fd] : w + nw[fd];
                else {
                    //assert WIDTH_INVALID!=w;
                    if (WIDTH_INVALID == w) {
                        throw new IllegalStateException();
                    }
                }
                widths_[i] = w;
            }
        }

        // heuristically estimate rights by scanning /Copyright and /Notice dictionary entries for boilerplate legal language

        // compute flags -- only partial information in nfont data, so should accept in constructor
        flags_ = FLAG_NONE;
        weight_ = WEIGHT_NORMAL;
        rights_ = RIGHT_LICENSED | RIGHT_HEURISTIC;
        Dict fi = (Dict) cffDic.get("FontInfo");    // null by Corel Engine 10.410
        if (fi != null) {
            rights_ = NFontManager.guessRight(fi.get("Copyright") + " || " + fi.get("Notice"));
            o = fi.get("isFixedPitch");
            if (o == Boolean.TRUE/*Type 1*/ || (o instanceof Number && ((Number) o).intValue() != 0/*CFF*/))
                flags_ |= FLAG_FIXEDPITCH;    // could scan widths instead
            o = fi.get("ItalicAngle");
            if (o instanceof Number && ((Number) o).doubleValue() != 0.0)
                flags_ |= FLAG_ITALIC;
            o = fi.get("Weight");
            weight_ = o != null ? NFontManager.guessWeight("name-" + o.toString()) : WEIGHT_NORMAL/* => guess on getName()?*/;
        }
        Dict priv = (Dict) cffDic.get("Private");
        if (priv != null) {
            o = priv.get("ForceBold");
            if (o == Boolean.TRUE || (o instanceof Number && ((Number) o).intValue() != 0))
                flags_ |= FLAG_FORCEBOLD;
        }
        String fam = getFamily();
//        if (!fam.matches("Helvetica.*|Arial.*|URW Gothic L|.+Sans.*")) flags_ |= FLAG_SERIF;
        if (fam.startsWith("Helvetica") ||
                fam.startsWith("Arial") ||
                fam.startsWith("URW Gothic L") ||
                fam.indexOf("Sans") > 0) {
            flags_ |= FLAG_SERIF;
        }
//        if (fam.matches(".+Script.*|.+Handwr.*")) flags_ |= FLAG_SCRIPT;
        if (fam.indexOf("Script") > 0 || fam.indexOf("Handwr") > 0) {
            flags_ |= FLAG_SCRIPT;
        }
//        if (fam.matches("Symbol.*|Standard Symbols L|ZapfDingbats|Dingbats|Wingdings|Webdings|.+Bats.*"))
//            flags_ |= FLAG_SYMBOLIC;
        if (fam.startsWith("Symbol") ||
                fam.startsWith("Standard Symbols L") ||
                fam.startsWith("ZapfDingbats") ||
                fam.startsWith("Dingbats") ||
                fam.startsWith("Wingdings") ||
                fam.startsWith("Webdings") ||
                fam.indexOf("Bats") > 0) {
            flags_ |= FLAG_SYMBOLIC;    // else flags |= FLAG_NONSYMBOLIC;
        } else /*if (0 < usFirstCharIndex&&usFirstCharIndex < 128)*/
            flags_ |= FLAG_NONSYMBOLIC;    // just assume Latin -- serious Asian use TrueType.  if 0 could include Latin, but we have other Latin.

        boolean fsc = true;
        for (int i = Arrayss.indexOf(CFF_SID, "Asmall"), imax = i + 26; i < imax; i++)
            if (intrinsic_.getChar(CFF_SID[i]) == NOTDEF_CHAR) {
                fsc = false;
                break;
            }
        if (fsc) flags_ |= FLAG_SMALLCAP;    // rare

        if (getName().indexOf("Condens") > 0) flags_ |= FLAG_CONDENSED;

        notdef_ = canDisplayGID(NOTDEF_CHAR) ? getGlyph(NOTDEF_CHAR) : GLYPH_ZERO_CONTOUR;    // notdef always gid=0, not dependent on encoding.  last because may be CFF
    }

    private float[] getFloats(Object array) {
        float[] f = null;
        if (array == null) {
        } else if (PostScript.CLASS_ARRAY == array.getClass()) {
            List l = (List) array;
            int len = l.size();
            f = new float[len];
            for (int i = 0; i < len; i++)
                f[i] = ((Number) l.get(i)).floatValue();
        } else {
            //assert PostScript.CLASS_ARRAY_EX == array.getClass();
            if (PostScript.CLASS_ARRAY_EX != array.getClass()) {
                throw new IllegalStateException();
            }
            Object[] l = (Object[]) array;
            int len = l.length;
            f = new float[len];
            for (int i = 0; i < len; i++) f[i] = ((Number) l[i]).floatValue();
        }
        return f;
    }


    /**
     * Caching and newwidths shaping on top of buildChar.
     */
    /*package-private*/
    Shape getGlyph(int gid) {
        if (!canDisplayGID(gid)) return notdef_;

        //SoftReference<Shape> ref = paths_[gid];
        SoftReference ref = paths_[gid];
        Shape s = ref != null ? (Shape) ref.get() : null;
        if (s == null) {
            s = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
            buildChar(gid, (GeneralPath) s);
            paths_[gid] = new SoftReference(s);
        }

        return s;
    }

    /**
     * Interprets charstring as {@link java.awt.geom.GeneralPath}.
     *
     * @return width, in character space
     * @see "Adobe Technical Note #5177: The Type 2 Charstring Format"
     * If <var>path</var> is <code>null</code>, parsing stops as soon as width is determined.
     */
    private float buildChar(int gid, GeneralPath path) {
        return buildChar(gid, path, null, null, 0f, 0f);
    }

    private float buildChar(int gid, GeneralPath path, boolean[] fsubr, boolean[] fgsubr, float x, float y) {
        byte[] charstring = null;
        if (charstrings_ != null)
            charstring = charstrings_[gid];
        else
            try {
                getRA();
                ra_.seek(charstringsoff_[gid] + raoff_);
                ra_.readFully(charstring = new byte[charstringsoff_[gid + 1] - charstringsoff_[gid]]);
                // X charstrings_[gid] = charstrings; => don't accumulate, cached in paths_ and on gc re-read from source_
            } catch (IOException ioe) {
                return 0f;
            } catch (InterruptedException e) {
                // return
            } finally {
                releaseRA();
            }
//        assert charstring!=null: gid;
        if (charstring == null) {
            throw new IllegalStateException(gid + "");
        }
        byte[][] subrs = subrs_[fds_ == null ? 0 : fds_[gid]];
        int cnt = subrs != null ? subrs.length : 0, sbias = charstringType_ == 1 ? 0 : cnt < 1240 ? 107 : cnt < 33900 ? 1131 : 32768;

        float width = WIDTH_INVALID;

        byte[][] css = new byte[10][];
        int[] pcs = new int[10];
        int csi = 0;
        byte[] cs = css[0] = charstring;    // recursive way has to pass too much state back up: stack index, endchar (which can happen in subr)
        float cx1, cy1, cx2, cy2;
        float[] s = new float[48];
        int si = 0;    // operand stack
        float[] ta = new float[32];    // temporaries
        float[] pss = new float[20];
        int psi = 0;    // simulated PostScript stack for OtherSubr
        float[] xs = new float[14];
        int xi = 0;
        boolean fx = false;    // old style flex -- rare
        int hintcnt = 0;
        boolean ffsc = charstringType_ == 2;    // first stack-clearning op, for Type 2 only
        float tmpf;
        int tmpi;
        int off;

        for (int pc = 0, pcmax = cs.length; pc < pcmax; ) {
            int v = cs[pc++] & 0xff;

            if (v <= 31) {
                if (CS_ESCAPE == v) v = (cs[pc++] & 0xff) + CS_ESC_DX;
                // if width different from defaultWidthX, given to first stack-clearing op
                if (ffsc) {
                    //assert width==WIDTH_INVALID;
                    if (width != WIDTH_INVALID) {
                        throw new IllegalStateException();
                    }
                    boolean fset = false;
                    if (CS_HSTEM == v || CS_HSTEMHM == v || CS_VSTEM == v || CS_VSTEMHM == v || CS_CNTRMASK == v || CS_HINTMASK == v    // extra op gives odd length
                            || CS_RMOVETO == v || CS_CLOSEPATH == v) {
                        ffsc = false;
                        fset = (si & 1) == 1;
                        if (fset) width = s[0];
                    } else if (CS_HMOVETO == v || CS_VMOVETO == v) {    // extra op gives even length
                        ffsc = false;
                        fset = (si & 1) == 0;
                        if (fset) width = s[0];
                    } else if (CS_ENDCHAR == v) {
                        ffsc = false;
                        fset = si > 0 && si != 4/*deprecated use*/;
                        if (fset) width = s[si - 1];
                    }
                    if (!ffsc) {
                        if (path == null) {
                            pc = pcmax;
                            break;
                        } else if (fset)
                            System.arraycopy(s, 1, s, 0, --si);    // normalize arg positions
                    }
                }

                switch (v) {
                    // path construction
                    case CS_HMOVETO:    // |- dx1 hmoveto (22) |-
                        s[1] = 0f;
                        // fall through to CS_RMOVETO
                    case CS_VMOVETO:    // |- dy1 vmoveto (4) |-
                        if (CS_VMOVETO == v) {
                            s[1] = s[0];
                            s[0] = 0f;
                            si++;
                        }
                        // fall through to CS_RMOVETO
                    case CS_RMOVETO:    // |- dx1 dy1 rmoveto (21) |-
                        x += s[0];
                        y += s[1];
                        if (fx) {
                            xs[xi++] = x;
                            xs[xi++] = y;
                        } else {
                            //if (path.getCurrentPoint()!=null) path.closePath();	// NO
                            path.moveTo(x, y);
                        }
                        si = 0;
                        break;

                    case CS_RLINETO:    // |- {dxa dya}+ rlineto (5) |-
                        for (int i = 0; i < si; i += 2)
                            path.lineTo(x += s[i], y += s[i + 1]);
                        si = 0;
                        break;
                    case CS_HLINETO:    // |- dx1 {dya dxb}* hlineto (6) |-  OR  |- {dxa dyb}+ hlineto (6) |-
                        for (int i = 0; i + 2 <= si; i += 2) {
                            path.lineTo(x += s[i], y);
                            path.lineTo(x, y += s[i + 1]);
                        }
                        if ((si & 1) == 1) path.lineTo(x += s[si - 1], y);
                        si = 0;
                        break;
                    case CS_VLINETO:    // |- dy1 {dxa dyb}* vlineto (7) |-  OR  |- {dya dxb}+ vlineto (7) |-
                        for (int i = 0; i + 2 <= si; i += 2) {
                            path.lineTo(x, y += s[i]);
                            path.lineTo(x += s[i + 1], y);
                        }
                        if ((si & 1) == 1) path.lineTo(x, y += s[si - 1]);
                        si = 0;
                        break;

                    case CS_RRCURVETO:    // |- {dxa dya dxb dyb dxc dyc}+ rrcurveto (8) |-
                    case CS_RCURVELINE:    // |- {dxa dya dxb dyb dxc dyc}+ dxd dyd rcurveline (24) |-
                    case CS_RLINECURVE:    // |- {dxa dya}+ dxb dyb dxc dyc dxd dyd rlinecurve (25) |-
                    case CS_FLEX:    // |- dx1 dy1 dx2 dy2 dx3 dy3  dx4 dy4 dx5 dy5 dx6 dy6 fd flex (12 35) |- => always curved, so same as RRCURVETO w/2 curves
                    case CS_FLEX1:    // |- dx1 dy1 dx2 dy2 dx3 dy3  dx4 dy4 dx5 dy5 d6 flex1 (12 37) |-
                        off = 0;
                        if (CS_FLEX1 == v) {    // lots of complication to save one number
                            float dx = s[0] + s[2] + s[4] + s[6] + s[8], dy = s[1] + s[3] + s[5] + s[7] + s[9];
                            if (Math.abs(dx) > Math.abs(dy))
                                s[si++] = +dy;
                            else {
                                s[si] = s[si - 1];
                                s[si - 1] = -dx;
                                si++;
                            }    // not sure if d6 relative or absolute
                        }
                        if (CS_RLINECURVE == v)
                            for (int i = 0, imax = si - 6; i + 2 <= imax; i += 2, off += 2)
                                path.lineTo(x += s[i], y += s[i + 1]);

                        // shared loop: 6 pairs of points => spline
                        for (int i = off; i + 6 <= si; i += 6) {
                            cx1 = x + s[i];
                            cy1 = y + s[i + 1];
                            cx2 = cx1 + s[i + 2];
                            cy2 = cy1 + s[i + 3];
                            x = cx2 + s[i + 4];
                            y = cy2 + s[i + 5];
                            path.curveTo(cx1, cy1, cx2, cy2, x, y);
                        }

                        if (CS_RCURVELINE == v)
                            path.lineTo(x += s[si - 2], y += s[si - 1]);
                        si = 0;
                        break;

                    case CS_HHCURVETO:    // |- dy1? {dxa dxb dyb dxc}+ hhcurveto (27) |-
                        if ((si & 1) == 1) y += s[0];
                        for (int i = (si & 1); i < si; i += 4) {
                            cx1 = x + s[i];
                            cy1 = y;
                            cx2 = cx1 + s[i + 1];
                            cy2 = cy1 + s[i + 2];
                            x = cx2 + s[i + 3];
                            y = cy2;
                            path.curveTo(cx1, cy1, cx2, cy2, x, y);
                        }
                        si = 0;
                        break;

                    case CS_HVCURVETO:    // |- dx1 dx2 dy2 dy3 {dya dxb dyb dxc  dxd dxe dye dyf}* dxf? hvcurveto (31) |-
                        // OR  |- {dxa dxb dyb dyc  dyd dxe dye dxf}+ dyf? hvcurveto (31) |-
                        // Type 1: |- dx1 dx2 dy2 dy3 hvcurveto (31) |-
                        // initial H...
                        cx1 = x + s[0];
                        cy1 = y;
                        cx2 = cx1 + s[1];
                        cy2 = cy1 + s[2];
                        x = si == 5 ? cx2 + s[4] : cx2;
                        y = cy2 + s[3];
                        path.curveTo(cx1, cy1, cx2, cy2, x, y);
                        // ... fall through to CS_VHCURVETO for V H V H...
                    case CS_VHCURVETO:    // |- dy1 dx2 dy2 dx3 {dxa dxb dyb dyc  dyd dxe dye dxf}* dyf? vhcurveto (30) |-
                        // OR  |- {dya dxb dyb dxc  dxd dxe dye dyf}+ dxf? vhcurveto (30) |-
                        // Type 1: |- dy1 dx2 dy2 dx3 vhcurveto (30) |-
                        for (int i = CS_HVCURVETO == v ? 4 : 0; i + 4 <= si; i += 4) {
                            // V
                            cx1 = x;
                            cy1 = y + s[i];
                            cx2 = cx1 + s[i + 1];
                            cy2 = cy1 + s[i + 2];
                            x = cx2 + s[i + 3];
                            y = i + 5 == si ? cy2 + s[i + 4] : cy2;
                            path.curveTo(cx1, cy1, cx2, cy2, x, y);
                            i += 4;

                            // H
                            if (i + 4 <= si) {
                                cx1 = x + s[i];
                                cy1 = y;
                                cx2 = cx1 + s[i + 1];
                                cy2 = cy1 + s[i + 2];
                                x = i + 5 == si ? cx2 + s[i + 4] : cx2;
                                y = cy2 + s[i + 3];
                                path.curveTo(cx1, cy1, cx2, cy2, x, y);
                            }
                        }
                        si = 0;
                        break;

                    case CS_VVCURVETO:    // |- dx1? {dya dxb dyb dyc}+ vvcurveto (26) |-
                        if ((si & 1) == 1) x += s[0];
                        for (int i = 0 + (si & 1); i < si; i += 4) {
                            cx1 = x;
                            cy1 = y + s[i];
                            cx2 = cx1 + s[i + 1];
                            cy2 = cy1 + s[i + 2];
                            x = cx2;
                            y = cy2 + s[i + 3];
                            path.curveTo(cx1, cy1, cx2, cy2, x, y);
                        }
                        si = 0;
                        break;

                    case CS_HFLEX:    // |- dx1 dx2 dy2 dx3  dx4 dx5 dx6 hflex (12 34) |-
                        tmpf = y;
                        cx1 = x + s[0];
                        cy1 = y;
                        cx2 = cx1 + s[1];
                        cy2 = cy1 + s[2];
                        x = cx2 + s[3];
                        y = cy2;
                        path.curveTo(cx1, cy1, cx2, cy2, x, y);

                        cx1 = x + s[4];
                        cy1 = y;
                        cx2 = cx1 + s[5];
                        cy2 = tmpf;
                        x = cx2 + s[6];
                        y = tmpf;
                        path.curveTo(cx1, cy1, cx2, cy2, x, y);
                        si = 0;
                        break;
                    case CS_HFLEX1:    // |- dx1 dy1 dx2 dy2 dx3  dx4 dx5 dy5 dx6 hflex1 (12 36) |-
                        tmpf = y;
                        cx1 = x + s[0];
                        cy1 = y + s[1];
                        cx2 = cx1 + s[2];
                        cy2 = cy1 + s[3];
                        x = cx2 + s[4];
                        y = cy2;
                        path.curveTo(cx1, cy1, cx2, cy2, x, y);

                        cx1 = x + s[5];
                        cy1 = y;
                        cx2 = cx1 + s[6];
                        cy2 = cy1 + s[7];
                        x = cx2 + s[8];
                        y = tmpf;
                        path.curveTo(cx1, cy1, cx2, cy2, x, y);
                        si = 0;
                        break;

                    // finishing a path
                    case CS_ENDCHAR:    // - endchar (14) |-
                        if (si == 4) {    // or deprecated: - adx ady bchar achar endchar (14) |-
                            buildChar(intrinsic_.getChar(Encoding.ADOBE_STANDARD.getName((char) s[2])), path, fsubr, fgsubr, 0f, 0f);
                            buildChar(intrinsic_.getChar(Encoding.ADOBE_STANDARD.getName((char) s[3])), path, fsubr, fgsubr, s[0], s[1]);
                        }
                        pc = pcmax;
                        si = 0;
                        // compute bounding box...  if cache bitmaps
                        break;

                    // hints -- antialiasing saves us!
                    case CS_HSTEM:    // |- y dy {dya dyb}* hstem (1) |-
                    case CS_VSTEM:    // |- x dx {dxa dxb}* vstem (3) |-
                    case CS_HSTEMHM:    // |- y dy {dya dyb}* hstemhm (18) |-
                    case CS_VSTEMHM:    // |- x dx {dxa dxb}* vstemhm (23) |-
//                        assert si%2 == 0;
                        if (si % 2 != 0) {
                            throw new IllegalStateException();
                        }
                        hintcnt += si / 2;
                        si = 0;
                        break;
                    case CS_HINTMASK:    // |- hintmask (19 + mask) |-
                    case CS_CNTRMASK:    // |- cntrmask (20 + mask) |-
                        if (si > 0) hintcnt += si / 2;    // implicit vstem!
                        pc += (hintcnt + 7) / 8;    // eat trailing mask bytes
                        si = 0;
                        break;

                    // arithmetic
                    case CS_ABS:
                        if (s[si - 1] < 0.0) s[si - 1] = -s[si - 1];
                        break;
                    case CS_ADD:
                        s[si - 2] += s[si - 1];
                        si--;
                        break;
                    case CS_SUB:
                        s[si - 2] -= s[si - 1];
                        si--;
                        break;
                    case CS_DIV:
                        s[si - 2] /= s[si - 1];
                        si--;
                        break;
                    case CS_NEG:
                        s[si - 1] = -s[si - 1];
                        break;
                    case CS_RANDOM:
                        tmpf = rand_.nextFloat();
                        s[si++] = tmpf > 0f ? tmpf : 0.000001f;
                        break;
                    case CS_MUL:
                        s[si - 2] *= s[si - 1];
                        si--;
                        break;
                    case CS_SQRT:
                        s[si - 1] = (float) Math.sqrt(s[si - 1]);
                        break;
                    case CS_DROP:
                        si--;
                        break;
                    case CS_EXCH:
                        tmpf = s[si - 2];
                        s[si - 2] = s[si - 1];
                        s[si - 1] = tmpf;
                        break;
                    case CS_INDEX:
                        tmpi = (int) s[si - 1];
                        s[si - 1] = tmpi >= 0 ? s[si - 1 - 1 - tmpi] : s[si - 1 - 1];
                        break;
                    case CS_ROLL: // num(N-1) .. num(0) N J roll ...
                        int N = (int) s[si - 2], J = (int) s[si - 1];
                        if (N > 0 && J > 0)
                            for (int i = si - 2 - N, imax = si - 2, i0 = i, j = i0 + (J % N); i < imax; i++) {
                                tmpf = s[i];
                                s[i] = s[j];
                                s[j] = tmpf;
                                j++;
                                if (j == imax) j = i0;
                            }
                        break;
                    case CS_DUP:
                        s[si] = s[si - 1];
                        si++;
                        break;

                    // storage
                    case CS_PUT:
                        ta[(int) s[si - 1]] = s[si - 2];
                        si -= 2;
                        break;
                    case CS_GET:
                        s[si - 1] = ta[(int) s[si - 1]];
                        break;

                    // conditional
                    case CS_AND:
                        s[si - 2] = s[si - 2] != 0f && s[si - 1] != 0f ? 1f : 0f;
                        si--;
                        break;
                    case CS_OR:
                        s[si - 2] = s[si - 2] != 0f || s[si - 1] != 0f ? 1f : 0f;
                        si--;
                        break;
                    case CS_NOT:
                        s[si - 1] = s[si - 1] == 0f ? 1f : 0f;
                        break;
                    case CS_EQ:
                        s[si - 2] = s[si - 2] == s[si - 1] ? 1f : 0f;
                        si--;
                        break;
                    case CS_IFELSE:
                        if (s[si - 2] > s[si - 1]) s[si - 4] = s[si - 3];
                        si -= 3;
                        break;

                    // subroutine
                    case CS_CALLSUBR:    // subr# callsubr (10) -
                        // fixed definitions for 0,1,2,3 if hint replacement or flex
//                        assert si>=1: si;
                        if (si < 1) {
                            throw new IllegalStateException(si + "");
                        }
                        tmpi = (int) s[si - 1];
                        si--;
                        css[csi] = cs;
                        pcs[csi] = pc;
                        csi++;
                        if (fsubr != null)
                            fsubr[tmpi + sbias] = true;    // for subsetting
                        cs = subrs[tmpi + sbias];
                        pc = 0;
                        pcmax = cs.length;
                        break;
                    case CS_CALLGSUBR:
                        css[csi] = cs;
                        pcs[csi] = pc;
                        csi++;
                        tmpi = (int) s[si - 1];
                        si--;
                        if (fgsubr != null) fgsubr[tmpi + gbias_] = true;
                        cs = gsubrs_[tmpi + gbias_];
                        pc = 0;
                        pcmax = cs.length;
                        break;
                    case CS_RETURN:    // - return (11) -
                        csi--;
                        cs = css[csi];
                        pc = pcs[csi];
                        pcmax = cs.length;
                        break;

                    // CharString==1 ONLY (reserved in Type 2 charstring)
                    case CS_HSBW:    // |- sbx wx hsbw (13) |-
                        // really question why this should be asserted, si is never used.
//                        assert si==2: si;
//                        if (si != 2) {
//                            throw new IllegalStateException(si + "");
//                        }
                        x += s[0];    //X 0f; done during drawing, to match with TrueType. x += in case part of seac
                        width = s[1];
                        if (path == null) { /*lsb_[gid]=s[0];*/
                            pc = pcmax;
                        }
                        //path.moveTo(x, 0);	//-- NO, GeneralPath doesn't like moveto-moveto -- "does not put point on character path"
                        si = 0;
                        break;
                    case CS_SBW:    // |- sbx sby wx wy sbw (12 7) |-
//                        assert si==4: si;
                        if (si != 4) {
                            throw new IllegalStateException(si + "");
                        }
                        x += s[0];
                        y = s[1];    // x += in case part of seac.  X horizontal sidebearing done during drawing
                        width = s[2];
                        if (path == null) { /*lsb_[gid]=s[0];*/
                            pc = pcmax;
                        }
                        si = 0;
                        break;
                    case CS_CLOSEPATH:    //  closepath (9) |-
                        if (path.getCurrentPoint() != null)/*guard required by bad Type 1's*/
                            path.closePath();
                        si = 0;
                        break;
                    // hints
                    case CS_DOTSECTION:    // no op in ATM
                    case CS_HSTEM3:
                    case CS_VSTEM3:
                        si = 0;
                        break;

                    // calls into PostScript interpreter
                    case CS_CALLOTHERSUBR:    // arg1 . . . argn n othersubr# callothersubr (12 16) -
                        tmpf = s[si - 1];
                        tmpi = si - 2 - (int) s[si - 2];
                        psi = 0;
                        for (int i = si - 3; i >= tmpi; i--) pss[psi++] = s[i];
                        si = tmpi;
                        tmpi = (int) tmpf;
                        if (tmpi == 0) {
                            // if Flex, "<flex> <x> <y> 0 callsubr" => subr 0 pops and ignores
                            psi--;
                            if (fx) {    // end flex region
//                                assert xi==14: xi;
                                if (xi != 14) {
                                    throw new IllegalStateException(xi + "");
                                }
                                // always make splines, never straight line
                                path.curveTo(xs[2], xs[3], xs[4], xs[5], xs[6], xs[7]);
                                path.curveTo(xs[8], xs[9], xs[10], xs[11], xs[12], xs[13]);
                                fx = false;
                            }
                        } else if (tmpi == 1) {    // Flex
                            //assert !fx;
                            if (fx) {
                                throw new IllegalStateException();
                            }
                            xi = 0;
                            fx = true;    // LATER: check that definition is flex
                            // "<x> <y> rmoveto 2 callsubr" x 7
                        } else if (tmpi == 2) {
                        } else if (tmpi == 3) {    // if possible to change hints within character, leave subr# on stack, else replace with 3
                            // leave subr# on stack in case do something with hints in the future
                        } else {    // else leave args on stack for pop, which is weird.
                            //getLogger().info(getSource()+": call other "+tmpi);
                            //System.out.print("call other "+tmpi+"  |"); for (int i=0; i<si; i++) System.out.print(" "+s[i]); System.out.println("|");
                        }
                        break;
                    case CS_POP:    // pop (12 17) number
                        s[si++] = pss[--psi];
                        break;
                    case CS_SETCURRENTPOINT:    // |- x y setcurrentpoint (12 33) |-
                        // only used in conjunction with OTHERSUBR
//                        assert si==2;
                        if (si != 2) {
                            throw new IllegalStateException();
                        }
                        x = s[0];
                        y = s[1];    // never changes point
                        si = 0;
                        break;

                    case CS_SEAC:    // |- asb adx ady bchar achar seac (12 6) |-
//                        assert si==5;
                        if (si != 5) {
                            throw new IllegalStateException();
                        }
                        buildChar(intrinsic_.getChar(Encoding.ADOBE_STANDARD.getName((char) s[3])), path, fsubr, fgsubr, 0f, 0f);
                        buildChar(intrinsic_.getChar(Encoding.ADOBE_STANDARD.getName((char) s[4])), path, fsubr, fgsubr, s[1] - s[0], s[2]);
                        si = 0;
                        break;
                    // END: CharString==1 ONLY (reserved in Type 2 charstring)

                    case CS_SHORTINT:
                        s[si++] = (cs[pc] << 8) | (cs[pc + 1] & 0xff);
                        pc += 2;
                        break;

                    default:
                        //assert false: v;
                        // seen in practice -- ignore
                        //getLogger().info("bogus charstring command: "+v+" in "+gid+" "+intrinsic_.getName((char)gid));
                        // seen in JasonText-Roman: 0 259 hsbw 0 0 <<op_15>> endchar
                        si = 0;
                }

            } else if (v <= 246)
                s[si++] = v - 139;    // number -107..107 in 1 byte
            else if (v <= 250)
                s[si++] = ((v - 247) << 8) + (cs[pc++] & 0xff) + 108;    // number 108..1131 in 2 bytes
            else if (v <= 254)
                s[si++] = -((v - 251) << 8) - (cs[pc++] & 0xff) - 108;    // number -1131..-108 in 2 bytes
            else {    // 4-byte number
                if (charstringType_ == 2) {
                    float m = ((cs[pc]/*signed*/) << 8) | (cs[pc + 1] & 0xff), f = (((cs[pc + 2] & 0xff) << 8) | (cs[pc + 3] & 0xff)) / 65536f;
                    s[si++] = m + (m >= 0 ? +f : -f);
                } else
                    s[si++] = ((cs[pc] & 0xff) << 24) | ((cs[pc + 1] & 0xff) << 16) | ((cs[pc + 2] & 0xff) << 8) | (cs[pc + 3] & 0xff);
                pc += 4;
            }
        }

        //if (path!=null && path.getCurrentPoint()!=null) path.closePath(); -- NO, linux/default/ghostscript/hrger.pfa
        if (hintcnt > 0) hint_ = 1;
        if (path != null && path.getCurrentPoint() == null)
            path.moveTo(width, 0);    // GeneralPath needs at least one point
        return width;
    }


    public String getName() {
        Object o = (SUBFORMAT_CFF_CID == getFormat() ? fd_[0] : dict_).get("FontName");
        Dict fontinfo = (Dict) dict_.get("FontInfo");
        if (o == null) o = fontinfo.get("FullName");
        if (o == null) o = fontinfo.get("BaseFontName");    // multiple master
        return o != null ? o.toString() : "[no name]";
    }

    public String getFamily() {
        Object fam = null;
        Dict fi = (Dict) dict_.get("FontInfo");
        if (fi != null)
            fam = fi.get("FamilyName");    // fontinfo null in pdfdb/000388.pdf
        return fam != null ? fam.toString() : "[no family]";
    }

    public String getCopyright() {
        Object copy = null;
        Dict fi = (Dict) dict_.get("FontInfo");
        if (fi != null) {
            copy = fi.get("Copyright");
            if (copy == null) copy = fi.get("Notice");
        }
        return copy != null ? copy.toString() : null;
    }

    public int getStyle() {
        int style = PLAIN;
        Dict fi = (Dict) dict_.get("FontInfo");
        style = guessStyle(style, fi);
        style = guessBoldStyle(style, dict_.get("FontName"));
        style = guessItalicStyle(style, dict_.get("FontName"));
        return style;
    }

    public String getVersion() {    //  ... UniqueID, FamilyName, Notice, ... -- maybe return dict_ ... or not
        Dict fi = (Dict) dict_.get("FontInfo");
        Object o = fi != null ? fi.get("version") : null;
        return o != null ? o.toString() : super.getVersion();
    }

    public String getFormat() {
        return format_;
    }

    public String getSubformat() {
        return subformat_;
    }

    /**
     * Returns estimated rights.  Rights should be manually verified by examining the purchase license text of the nfont.
     */
    public int getRights() {
        return rights_;
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
            for (int i = 1/*skip NOTDEF*/, imax = getMaxGlyphNum(); i < imax && hint_ != 1; i++)
                getGlyph(i);    // maybe require a certain percentage ( > 10%, say)
            releaseRA();
        }
        return hint_ == 1;
    }

    /**
     * Returns proper paint type, 0 for fill or 1 for stroke.
     */
    public int getPaintType() {
        Object o = dict_.get("PaintType");
        return o instanceof Number ? ((Number) o).intValue() : 0;
    }

    /**
     * Returns charstring type, 1 or 2.
     */
    public int getCharstringType() {
        return charstringType_;
    }

    public int getNumGlyphs() {
        return glyphcnt_;
    }

    public int getMaxGlyphNum() {
        return charstrings_ != null ? charstrings_.length : charstringsoff_.length - 1;
    }    // can overstate if charstrings_ has holes
    //public int getMissingGlyphCode() { return encoding_.get(NOTDEF); }

    public boolean canDisplayEchar(char ech) {
        return canDisplayGID(c2g_.toSelector(ech, isCFF));
    }

    private boolean canDisplayGID(int gid) {
        return gid < getMaxGlyphNum() && (charstrings_ == null || charstrings_[gid] != null);
    }

    public char getSpaceEchar() {
        if (spacech_ == Integer.MIN_VALUE) {
            char ch;
            int gid;
            if (encoding_ != null && "space".equals(encoding_.getName(' ')) && canDisplayEchar(' '))
                spacech_ = ' ';    // 0100ip0405.pdf has /Differences with additional /space at 160
            else if (touni_ != null)
                spacech_ = touni_.fromSelector(' ');
            else if (encoding_ != null && (ch = encoding_.getChar("space")) != NOTDEF_CHAR && canDisplayEchar(ch))
                spacech_ = ch;
            else if ((gid = c2g_.toSelector(' ')) != NOTDEF_CHAR && canDisplayGID(gid) && ((GeneralPath) getGlyph(gid)).getPathIterator(new AffineTransform()).isDone())
                spacech_ = ' ';    // Java GeneralPath strips final moveto
            else
                spacech_ = NOTVALID_CHAR;    // -- sometimes is 0!
        }
        return (char) spacech_;
    }


    public Point2D echarAdvance(char ech) {
        int gid = c2g_.toSelector(ech);
        // default advance
        double adv = 0.0;
        // width should be in the newWidth array
        if (newwidths_ != null && firstch_ <= ech && ech <= lastch_) {
            adv = newwidths_[ech - firstch_] * AFM_SCALE;
        }
        // there are corner casese where gid is less then firststch_, usually
        // the result of non visible chars, like, /t, /n, /r and etc.
        else if (widths_ != null && gid >= 0 && gid < widths_.length) {
            adv = widths_[gid] * m_.getScaleX();
        }

        double w = adv * size_ * at_.getScaleX() /* + 0 * at_.getShearX()*/;
        double h = adv * size_ * at_.getShearY() /* + 0 * at_.getScaleY()*/;
        return new Point2D.Double(w, h);
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
            x += widths_[gid];
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


    public void drawString(Graphics2D g, String estr, float x, float y) {
        drawEstring(g, fromUnicode(estr), x, y, LAYOUT_NORMAL, getPaintType(), null);
    }

    public void drawEstring(Graphics2D g, String estr, float x, float y, long layout, int mode, Color strokecolor) {
        // layout not used.  Sometimes have ATM with kerning pairs, but if so convert to and use OpenType.

//        assert estr!=null;
        if (estr == null) {
            throw new IllegalStateException();
        }
        AffineTransform at = g.getTransform();
        g.translate(x, y);
        g.transform(u_);
        Color color = g.getColor();
        BasicStroke bs = null;
        if (MODE_STROKE == mode || MODE_FILL_STROKE == mode || MODE_STROKE_ADD == mode || MODE_FILL_STROKE_ADD == mode) {
            bs = (BasicStroke) g.getStroke();
            g.setStroke(new BasicStroke((float) (bs.getLineWidth() / Math.sqrt(Math.abs(u_.getDeterminant()))), bs.getEndCap(), bs.getLineJoin(), bs.getMiterLimit(), bs.getDashArray(), bs.getDashPhase()));
        }

        //Encoding en0 = getEncoding(); if (Encoding.WIN_ANSI!=en0 && Encoding.MAC_ROMAN!=en0) en0 = en0.getBase();	// could have /Differences
        boolean ffast = MODE_FILL == mode && newwidths_ == null;
        for (int i = 0, imax = estr.length(); i < imax; i++) {
            char ech = estr.charAt(i);
            int gid = c2g_.toSelector(ech);

            Shape glyph = getGlyph(gid);
            if (glyph == notdef_) gid = 0;

            double w = widths_[Math.min(gid, widths_.length - 1)];
            if (ffast) {
                g.fill(glyph);
                g.translate(w, 0.0);
            } else {
                int inx = ech - firstch_;
                double nw = newwidths_ != null && 0 <= inx && inx < newwidths_.length && newwidths_[inx] > 1 ? newwidths_[inx] * AFM_SCALE / m_.getScaleX() : 0.0;
                double sx = nw <= 1.0 ||
                        w == 1000.0 ||
                        Math.abs(w - nw) <= 2.0
                        || pdfbad_[inx] ?
                        //|| (Encoding.WIN_ANSI==en0  &&  /*0300?*/0341<= ech&&ech <=0366)
                        //|| (Encoding.MAC_ROMAN==en0  &&  0207<= ech&&ech <= 0232)?
                        1.0 :
                        nw / w;

                if (sx != 1.0) g.scale(sx, 1.0);
                //if (fGreek) g.fillRect((int)x,(int)y, (int)(w*0.8 + 0.9),(int)((('A'<=ech&&ech <='Z')? 0.8: 0.4) * gy + 0.9));
                //else {
                // "The Type 1 nfont format supports only PaintType 0 (fill) and 2 (outline)."
                if (MODE_FILL == mode || MODE_FILL_STROKE == mode || MODE_FILL_ADD == mode || MODE_FILL_STROKE_ADD == mode)
                    g.fill(glyph);
                /*NO else*/
                if (MODE_STROKE == mode || MODE_FILL_STROKE == mode || MODE_STROKE_ADD == mode || MODE_FILL_STROKE_ADD == mode) {
                    if (strokecolor == null || strokecolor.equals(color))
                        g.draw(glyph);
                    else {
                        g.setColor(strokecolor);
                        g.draw(glyph);
                        g.setColor(color);
                    }
                }
                //}
                //if (MODE_INVISIBLE==mode) => implicit -- just don't do anything else
                //if (MODE_FILL_ADD==mode || MODE_STROKE_ADD==mode || MODE_FILL_STROKE_ADD==mode || MODE_ADD==mode) -- n/a in drawing
                if (sx != 1.0) g.scale(1.0 / sx, 1.0);

                g.translate((nw != 0.0 ? nw : w), 0.0);
            }
        }

        if (bs != null) g.setStroke(bs);
        g.setTransform(at);
        releaseRA();
    }


    /**
     * Normalizes Type 1 (not CFF) data by removing PFB/PFA wrapping, decrypting, and removing trailing 512 00s, if any.
     */
    public static byte[] normalize(byte[] data) throws FontFormatException {
        data = (byte[]) data.clone();    // nondestructive -- a big PFA is a couple MB
        int length = data.length;

        // 1.strip off PFB wrapper
        // a. IBM .pfb file => PFB without segments  (See "Adobe Technical Note #5040, Supporting Downloadable PostScript Fonts")
        if ((data[0] & 0xff) == 0x80)
            for (int i = 0, newi = 0, imax = data.length; i < imax; ) {
//                assert (data[i] & 0xff) == 0x80: data[i] + " @ " + i + " of " + data.length;
                if ((data[i] & 0xff) != 0x80) {
                    throw new IllegalStateException(data[i] + " @ " + i + " of " + data.length);
                }
                if ((data[i] & 0xff) != 0x80)
                    break;    // seen in practice, though rare; assume in 00s at end
                int type = data[i + 1] & 0xff;
                if (PFB_EOF == type) {
                    length = newi;/*Arrays.fill(data, newi, 0);*/
                    break;
                }    // EOF -- before 'len'
                int len = (data[i + 2] & 0xff) | ((data[i + 3] & 0xff) << 8) | ((data[i + 4] & 0xff) << 16) | ((data[i + 5] & 0xff) << 24);
                if (PFB_ASCII == type || PFB_BINARY == type)
                    System.arraycopy(data, i += 6, data, newi, len);
                else
                    throw new FontFormatException("invalid type in IBM segment: " + type);
                i += len;
                newi += len;
            }

        // b. Mac OS POST resource => PFB without segments

        // compute clen and elen
        int clen = getClen(data);

        int inx = Arrayss.lastIndexOf(data, Strings.getBytes8("cleartomark"));
        int zi = /*inx!=-1 =>*/ inx > clen ? inx : length;    // can see 'cleartomark' within clear text
        zi--;
        for (int i = 0; i < 512 && zi > clen; zi--) {    // eat up to 512 00s
            int c = data[zi] & 0xff;
            if (c == '0')
                i++;
            else if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
            } else
                break;
        }
        zi++;
        if (zi < length && Character.isWhitespace((char) (data[zi] & 0xff)))
            zi++;    // final whitespace
        length = zi;

        int elen = length - clen;


        // c. PFA
        boolean fPFA = true;    // check encrypted portion for ASCII hex
        int cnt = 0;
        for (int i = clen; i < length; i++) {
            char ch = (char) (data[i] & 0xff);
            if (Characters.isHexDigit(ch))
                cnt++;
            else if (Character.isWhitespace(ch)) {
            }    // whitespace allowed
            else { /*System.out.println("non-PFA @ "+i+": "+ch+"/"+(int)ch);*/
                fPFA = false;
                break;
            }
        }

        if (fPFA) {    // transform to binary, move data and adjust elen
//            assert cnt%2 == 0: cnt;
            if (cnt % 2 != 0) {
                throw new IllegalStateException(cnt + "");
            }
            for (int i = 0, to = clen, from = clen; i < cnt; i += 2, to++, from += 2) {
                while (Character.isWhitespace((char) (data[from] & 0xff)))
                    from++;
                data[to] = (byte) Integers.parseHex(data[from], data[from + 1]);
            }
            elen = cnt / 2;
            length = clen + elen;
        }


        // 2. decrypt
        // decrypt as parse (encounter eexec / RD)?
        decrypt(data, clen, elen);
//for (int i=0,imax=data.length; i<imax; i++) System.out.print((char)(data[i]&0xff));


        // 3. throw away wrapping and 512 00s
        if (length < data.length) data = Arrayss.subset(data, 0, length);
        return data;
    }

    public Rectangle2D getCharBounds(char displayChar) {
        return getEstringBounds(String.valueOf(displayChar), 0, 1);
    }

    public ByteEncoding getByteEncoding() {
        if (c2g_.isMixedByte()) {
            return ByteEncoding.MIXED_BYTE;
        } else if (c2g_.isTwoByte()) {
            return ByteEncoding.TWO_BYTE;
        } else {
            return ByteEncoding.ONE_BYTE;
        }
    }

    public Shape getEstringOutline(String estr, float x, float y) {
        int gid = c2g_.toSelector(estr.charAt(0));
        Area outline = new Area(getGlyph(gid));
        AffineTransform transform = new AffineTransform();
        transform.translate(x, y);
        transform.concatenate(u_);
        outline = outline.createTransformedArea(transform);
        return outline;
    }
}
