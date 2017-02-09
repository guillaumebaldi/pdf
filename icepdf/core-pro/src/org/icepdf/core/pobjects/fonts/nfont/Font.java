package org.icepdf.core.pobjects.fonts.nfont;

import org.icepdf.core.io.SeekableInput;
import org.icepdf.core.pobjects.*;
import org.icepdf.core.pobjects.fonts.AFM;
import org.icepdf.core.pobjects.fonts.FontDescriptor;
import org.icepdf.core.pobjects.fonts.FontFile;
import org.icepdf.core.pobjects.fonts.FontManager;
import org.icepdf.core.pro.application.ProductInfo;
import org.icepdf.core.util.Defs;
import org.icepdf.core.util.FontUtil;
import org.icepdf.core.util.Library;
import org.icepdf.core.util.Utils;

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Font Resource for the NFont library.
 * <p/>
 * Font are grouped into the 'Resource' category and can be shared.  As a result we need to make sure
 * that the init method are synchronized as they can be accessed by different page loading threads.
 */
public class Font extends org.icepdf.core.pobjects.fonts.Font {

    private static final Logger logger =
            Logger.getLogger(Font.class.toString());

    public static final Name FONT_DESCRIPTOR_KEY = new Name("FontDescriptor");
    public static final Name TO_UNICODE_KEY = new Name("ToUnicode");
    public static final Name DESCENDANT_FONTS_KEY = new Name("DescendantFonts");
    public static final Name ENCODING_KEY = new Name("Encoding");
    public static final Name BASEENCODING_KEY = new Name("BaseEncoding");
    public static final Name CID_SYSTEM_INFO_KEY = new Name("CIDSystemInfo");
    public static final Name CID_TO_GID_MAP_KEY = new Name("CIDToGIDMap");
    public static final Name DW_KEY = new Name("DW");
    public static final Name W_KEY = new Name("W");
    public static final Name WIDTHS_KEY = new Name("Widths");
    public static final Name FIRST_CHAR_KEY = new Name("FirstChar");
    public static final Name DIFFERENCES_KEY = new Name("Differences");

    // ToUnicode CMap object stores any mapping information
    private CMap toUnicodeCMap;

    // encoding cmap for composite font type0
    private CMap cMap;

    // An array of (LastChar ? FirstChar + 1) widths, each element being the
    // glyph width for the character code that equals FirstChar plus the array index.
    // For character codes outside the range FirstChar to LastChar, the value
    // of MissingWidth from the FontDescriptor entry for this font is used.
    protected float[] widths;

    // A specification of the font's character encoding, if different from its
    // built-in encoding. The value of Encoding may be either the name of a predefined
    // encoding (MacRomanEncoding, MacExpertEncoding, or WinAnsi- Encoding, as
    // described in Appendix D) or an encoding dictionary that specifies
    // differences from the font's built-in encoding or from a specified predefined
    // encoding
    private Encoding encoding;
    private String ordering;

    private static List<String> hintingEnabledNames;
    private static boolean isGlobalHinting;
    private static boolean allFontsHinted;

    static {
        // turn of bitmap caching, temporary
        NFont.setUseBitmaps(false);

        // decide if large images will be scaled
        isGlobalHinting = Defs.sysPropertyBoolean("org.icepdf.core.nfont.truetype.hinting", false);

        String eraseExisting = Defs.property("org.icepdf.core.nfont.truetype.eraseHintingDefaults", "false");
        hintingEnabledNames = new ArrayList<String>();
        if (!Boolean.valueOf(eraseExisting)) {
            hintingEnabledNames.add("mingli");
            hintingEnabledNames.add("kai");
            hintingEnabledNames.add("tcid");
        }
        String hintEnabledFonts = Defs.property("org.icepdf.core.nfont.truetype.hintingNames");
        if (hintEnabledFonts != null) {
            StringTokenizer st = new StringTokenizer(hintEnabledFonts);
            while (st.hasMoreTokens()) {
                String name = st.nextToken();
                hintingEnabledNames.add(name.toLowerCase().trim());
            }
        }
        String allFonts = Defs.property("org.icepdf.core.nfont.truetype.hinting.alwayson", "false");
        allFontsHinted = Boolean.valueOf(allFonts);

        // Announce ICEpdf Core
        if (logger.isLoggable(Level.INFO)) {
            logger.info("ICEsoft ICEpdf Pro " + getLibraryVersion());
        }
    }

    public Font(Library l, HashMap h) {
        super(l, h);
    }

    /**
     * Initiate the font. Retrieve any needed attributes, basically set up the
     * font so it can be used by the content parser.
     */
    public synchronized void init() {
        // flag for initiated fonts
        if (inited) {
            return;
        }

        boolean isEmbedded = false;

        // Assign the font descriptor
        Object of = library.getObject(entries, FONT_DESCRIPTOR_KEY);
        if (of instanceof FontDescriptor) {
            fontDescriptor = (FontDescriptor) of;
        }
        // some font descriptors are missing the type entry so we
        // try build the fontDescriptor from retrieved hashtable.
        else if (of instanceof HashMap) {
            fontDescriptor = new FontDescriptor(library, (HashMap) of);
        }
        if (fontDescriptor != null) {
            fontDescriptor.init();
            // get most types of embedded fonts from here
            if (fontDescriptor.getEmbeddedFont() != null) {
                font = fontDescriptor.getEmbeddedFont();
                isEmbedded = true;
                // make sure we mark this font as having a font file
                isFontSubstitution = false;
            }
        }

        // If there is no FontDescriptor then we most likely have a core afm
        // font and we should get the matrix so that we can derive the correct
        // font.
        if (fontDescriptor == null && basefont != null) {
            // see if the baseFont name matches one of the AFM names
            Object afm = AFM.AFMs.get(basefont.toLowerCase());
            //System.out.println("Looking for afm " + basefont);
            if (afm != null && afm instanceof AFM) {
                AFM fontMetrix = (AFM) afm;
                // finally create a fontDescriptor based on AFM data.
                //System.out.println("Initiating core 14 AFM font DEscriptor");
                fontDescriptor = FontDescriptor.createDescriptor(library, fontMetrix);
                fontDescriptor.init();
            }
        }

        // Get flags data if it exists.
        int fontFlags = 0;
        if (fontDescriptor != null) {
            fontFlags = fontDescriptor.getFlags();
        }

        // ToUnicode indicates that we now have CMap stream that need to be parsed
        Object objectUnicode = library.getObject(entries, TO_UNICODE_KEY);
        if (objectUnicode != null && objectUnicode instanceof Stream) {
            Stream cMapStream = (Stream) objectUnicode;
            if (cMapStream != null) {
                InputStream cMapInputStream = cMapStream.getDecodedByteArrayInputStream();
                try {
                    if (logger.isLoggable(Level.FINER)) {
                        String content;
                        if (cMapInputStream instanceof SeekableInput) {
                            content = Utils.getContentFromSeekableInput((SeekableInput) cMapInputStream, false);
                        } else {
                            InputStream[] inArray = new InputStream[]{cMapInputStream};
                            content = Utils.getContentAndReplaceInputStream(inArray, false);
                            cMapInputStream = inArray[0];
                        }
                        logger.finer("ToUnicode CMAP = " + content);
                    }

                    // try and load the cmap stream
                    toUnicodeCMap = new CMap(cMapInputStream);
                } catch (Throwable e) {
                    logger.log(Level.SEVERE, "Error reading cmap file.", e);
                } finally {
                    try {
                        // close the stream
                        if (cMapInputStream != null)
                            cMapInputStream.close();
                    } catch (IOException e) {
                        logger.log(Level.FINE, "CMap Reading/Parsing Error.", e);
                    }
                }
            }
        }
        // handle toUnicode maps that are name based.
        if (objectUnicode != null && objectUnicode instanceof Name) {
            Name unicodeName = (Name) objectUnicode;
            if (CMap.CMapNames.identity.equals(unicodeName)) {
                toUnicodeCMap = CMap.IDENTITY;
            } else if (CMap.CMapNames.identityV.equals(unicodeName)) {
                toUnicodeCMap = CMap.IDENTITY_V;
            } else if (CMap.CMapNames.identityH.equals(unicodeName)) {
                toUnicodeCMap = CMap.IDENTITY_H;
            }
        }

        /**
         * Take care of Composite fonts (CIDFonts), CIDFontType0, CIDFontType2 and Type0
         */
//        System.out.println("---> Font " + basefont + " : " + subtype);
        if (subtype.equals("Type0")) {
            // first get descendant font and try and load embedded stream
            // should be a TrueType font, only one descendant us allowed
            if (entries.containsKey(DESCENDANT_FONTS_KEY)) {
                Object descendant = library.getObject(entries, DESCENDANT_FONTS_KEY);
                if (descendant != null && descendant instanceof List) {
                    List descendantFonts = (List) descendant;
                    // finally get the first element which should be the Font
                    // which holds the embedded font data
                    Font descendantFont = null;
                    Object descendantFontObject = descendantFonts.get(0);
                    if (descendantFontObject instanceof Reference) {
                        Reference descendantFontReference = (Reference) descendantFontObject;
                        descendantFont = (Font) library.getObject(descendantFontReference);
                    } else if (descendantFontObject instanceof Font) {
                        descendantFont = (Font) descendantFontObject;
                    }
                    if (descendantFont != null) {
                        if (toUnicodeCMap != null) {
                            descendantFont.toUnicodeCMap = toUnicodeCMap;
                        }
                        descendantFont.init();
                        font = descendantFont.font;
                        toUnicodeCMap = descendantFont.toUnicodeCMap;
//                        charset = descendantFont.charset;
                        // we have a type0 cid font  which we need to setup some
                        // special mapping
                        if (descendantFont.isFontSubstitution &&
                                toUnicodeCMap != null &&
                                font instanceof NFontTrueType) {
                            // get the encoding mapping
                            Object cmap = library.getObject(entries, ENCODING_KEY);
                            // try and load the cmap from the international jar.
                            if (cmap != null && cmap instanceof Name) {
                                CMap encodingCMap = CMap.getInstance(cmap.toString());
                                ((NFontTrueType) font).applyCidCMap(encodingCMap);
                            }
                        }
//                        new Demo((NFont)font, 0, 350);
                        // make sure do check the descendant for the presence
                        // of a font file
                        if (!descendantFont.isFontSubstitution) {
                            isEmbedded = true;
                        }
                    }
                }
            }
            if (entries.containsKey(ENCODING_KEY)) {
                // may also need the cmap data from Encoding attribute
                Object cmap = library.getObject(entries, ENCODING_KEY);
                // try and load the cmap from the international jar.
                if (cmap != null && cmap instanceof Name) {
                    cMap = CMap.getInstance(cmap.toString());
                    // mark the font for vertical writing
                    if (cMap != null && cMap.equals(CMap.IDENTITY_V)) {
                        isVerticalWriting = true;
                    }
                    if (logger.isLoggable(Level.WARNING) && cMap == null) {
                        logger.warning("CMAP resource error, could not find file: " +
                                cmap.toString());
                    }
                }
                // PDF has embedded cmap file which we parse.
                else if (cmap != null && cmap instanceof Stream) {
                    try {
                        InputStream cMapStream =
                                ((Stream) cmap).getDecodedByteArrayInputStream();

//                        if (logger.isLoggable(Level.FINER)) {
//                            InputStream[] inArray = new InputStream[]{cMapStream};
//                            String content = Utils.getContentAndReplaceInputStream(inArray, false);
//                            logger.finer("Encoding CMAP = " + content);
//                        }

                        cMap = new CMap(cMapStream);
                        cMapStream.close();
                    } catch (Throwable e) {
                        logger.log(Level.FINE, "Error parsing cmap.", e);
                    }
                }

                /**
                 * If the above failed then we try hard to find an appropriate
                 * match
                 */
                if (toUnicodeCMap == null && cmap instanceof Name) {
                    String cmapName = cmap.toString();
                    if (cmapName.equals("Identity-H") ||
                            cmapName.equals("Identity-V") ||
                            cmapName.equals("Identity")) {
                        toUnicodeCMap = cMap;
                    }
                    // UCS2 is already in unicode
                    else if (cmapName.indexOf("UCS2") > 0) {
                        toUnicodeCMap = CMap.IDENTITY_H;
                    }
                    // Mapt ot UTF16
                    else if (cmapName.indexOf("UTF16") > 0) {
                        toUnicodeCMap = CMap.IDENTITY_UTF16BE;
                    } else {
                        // see if there is match in one of adobe's definitions
                        for (String[] aTO_UNICODE : TO_UNICODE) {
                            for (String anATO_UNICODE : aTO_UNICODE) {
                                if (cmapName.equals(anATO_UNICODE)) {
                                    toUnicodeCMap = CMap.getInstance(cmapName);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (toUnicodeCMap == null) {
                toUnicodeCMap = cMap;
            }
            // we have a potential problem as a CID font needs to have a CID
            // font program which 99% of the time embedded in the document. If
            // we don't have the font but have the toUnicode cMap we can change
            // the font encoding so we get the write cid glyph.
            if (!isEmbedded && toUnicodeCMap != null) {
                if (toUnicodeCMap != null) {
                    cMap = toUnicodeCMap;
                } else {
                    cMap = toUnicodeCMap = CMap.IDENTITY_H;
                }
            }
            // Apply the descendant font cMapping and unicode cMap, to complete
            // the type0 construction.
            FontFile[] descendantFont = new FontFile[]{font};
            NFontType0 type0 = new NFontType0(basefont, descendantFont);
            // apply cmap but never do anything on the descendant
            font = type0.deriveFont(cMap, toUnicodeCMap);
        } else if (subtype.equals("Type3")) {
            font = new NFontType3(library, entries);
            ((NFontType3) font).setParentResource(parentResource);
        }
        /**
         * If the font is null at this point then we have a CID with no embedded
         * data and thus must map it against adobes predefined cmap files and a font
         * that has lots of glyphs.
         */
        if (font == null &&
                (subtype.equals("CIDFontType0") ||
                        subtype.equals("CIDFontType2"))) {
            // Get CIDSystemInfo dictionary so we can get ordering data
            Object obj = library.getObject(entries, CID_SYSTEM_INFO_KEY);
            if (obj instanceof HashMap) {
                StringObject orderingObject = (StringObject) ((HashMap) obj).get(new Name("Ordering"));
                StringObject registryObject = (StringObject) ((HashMap) obj).get(new Name("Registry"));
                if (orderingObject != null && registryObject != null) {
                    ordering = orderingObject.getDecryptedLiteralString(library.getSecurityManager());
                    String registry = registryObject.getDecryptedLiteralString(library.getSecurityManager());
                    FontManager fontManager = FontManager.getInstance().initialize();
                    isFontSubstitution = true;

                    // find a font and assign a charset.
                    // simplified Chinese
                    if (ordering.startsWith("GB1") || ordering.startsWith("'CNS1")) {
                        font = fontManager.getChineseSimplifiedInstance(basefont, fontFlags);
                    }
                    // Korean
                    else if (ordering.startsWith("Korea1")) {
                        font = fontManager.getKoreanInstance(basefont, fontFlags);
                    }
                    // Japanese
                    else if (ordering.startsWith("Japan1")) {
                        font = fontManager.getJapaneseInstance(basefont, fontFlags);
                    }
                    // might be a font loading error a we need check normal system fonts too
                    else if (ordering.startsWith("Identity")) {
                        font = fontManager.getInstance(basefont, fontFlags);
                    }
                    // fallback traditional Chinese.
                    else {
                        font = fontManager.getChineseTraditionalInstance(basefont, fontFlags);
                    }
                    if (font instanceof NFontTrueType) {
                        // Build a toUnicode table as defined in section 9.10.2.
                        //
                        // a)Map the character code to a character identifier (CID)
                        // according to the font’s CMap.
                        // this is the font encoding
                        // b)Obtain the registry and ordering of the character collection
                        // used by the font’s CMap (for example, Adobe and Japan1) from
                        // its CIDSystemInfo dictionary.
                        // c)Construct a second CMap name by concatenating the registry
                        // and ordering obtained in step (b) in the format
                        // registry–ordering–UCS2 (for example, Adobe–Japan1–UCS2).
                        String ucs2CMapName = registry + '-' + ordering + "-UCS2";
                        // d) Obtain the CMap with the name constructed in step (c)
                        // (available from the ASN Web site; see the Bibliography).
                        CMap ucs2CMap = CMap.getInstance(ucs2CMapName);
                        // e) Map the CID obtained in step (a) according to the CMap
                        // obtained in step (d), producing a Unicode value.
                        toUnicodeCMap = ucs2CMap;
                        font = ((NFontTrueType) font).deriveFont(toUnicodeCMap);
                    }
                }
            }
        }

        /**
         * Use the font manager to get a system font that is close the named
         * values.
         */
        if (font == null) {
            try {
                font = FontManager.getInstance().initialize().getInstance(basefont, fontFlags);
                isFontSubstitution = true;
            } catch (Exception e) {
                if (logger.isLoggable(Level.SEVERE)) {
                    logger.log(Level.SEVERE,
                            "Font loading failure, no font available to substitute: " + basefont);
                }
            }
        }

        /**
         * Finally apply encoding and widths
         */
        if (subtype.equals("Type1") || subtype.equals("MMType1") ||
                subtype.equals("Type3") || subtype.equals("TrueType")) {
            NFontSimple fs = (NFontSimple) font;
//            fs = fs.deriveFont(encoding, CMap.IDENTITY);
            fs = setEncoding(fs, toUnicodeCMap, isEmbedded);
            fs = setWidth(fs, fs.getEncoding());
            font = fs;
        } else if (subtype.equals("CIDFontType0") ||
                subtype.equals("CIDFontType2")) {
            Object gidMap = library.getObject(entries, CID_TO_GID_MAP_KEY);
            if (subtype.equals("CIDFontType2") &&
                    ((ordering != null && ordering.startsWith("Identity")) || gidMap != null || !isFontSubstitution)) {
                CMap subfontToUnicodeCMap = toUnicodeCMap != null ? toUnicodeCMap : CMap.IDENTITY;
                if (gidMap == null || gidMap instanceof Name) {
                    String mappingName = null;
                    if (gidMap != null) {
                        mappingName = gidMap.toString();
                    }
                    if (toUnicodeCMap instanceof CMapIdentityH) {
                        mappingName = toUnicodeCMap.toString();
                    }
                    // mapping name will be null only in a few corner cases, but
                    // identity will be applied otherwise.
                    if (mappingName == null || mappingName.equals("Identity")) {
                        font = ((NFontTrueType) font).deriveFont(CMap.IDENTITY, subfontToUnicodeCMap);
                    }
                } else if (gidMap instanceof Stream) {
                    try {
                        ByteArrayInputStream cidStream =
                                ((Stream) gidMap).getDecodedByteArrayInputStream();
                        int character = 0;
                        int i = 0;
                        int length = cidStream.available() / 2;
                        char[] cidToGid = new char[length];
                        // parse the cidToGid stream out, arranging the high bit,
                        // each character position that has a value > 0 is a valid
                        // entry in the CFF.
                        while (character != -1 && i < length) {
                            character = cidStream.read();
                            character = (char) ((character << 8) | cidStream.read());
                            cidToGid[i] = (char) character;
                            i++;
                        }
                        cidStream.close();
                        // apply the cidToGid mapping, but try figure out how many bytes are going to be in
                        // in each character, we use the toUnicode mapping if present.
                        CMap cidGidMap = new CMap(cidToGid);
                        if (toUnicodeCMap != null) {
                            cidGidMap.applyBytes(toUnicodeCMap);
                        }
                        font = ((NFontTrueType) font).deriveFont(
                                cidGidMap.reverse(), CMap.IDENTITY);
                    } catch (IOException e) {
                        logger.log(Level.FINE, "Error reading CIDToGIDMap Stream.", e);
                    }
                }
                // apply CIDFontType2 width info
                float defaultWidth = -1;
                ArrayList individualWidths = null;
                if (library.getObject(entries, W_KEY) != null) {
                    individualWidths = (ArrayList) library.getObject(entries, W_KEY);
                }
                if (library.getObject(entries, DW_KEY) != null) {
                    defaultWidth =
                            ((Number) library.getObject(entries, DW_KEY)).floatValue();
                }
                if (individualWidths != null || defaultWidth > -1) {
                    font = ((NFontTrueType) font).deriveFont(defaultWidth, individualWidths);
                } else {
                    // build out the default widths base on first and last.  The CID
                    // font likely has bad width so we default the widths.
                    // side note it's also possible that the width array will be way to
                    // short which may need further work.
                    font = ((NFontTrueType) font).deriveFont(1000, null);
                }
            }
        }
        // Check to see if the font has a valid name that can be use to
        // to compare against the know list of fonts to hind.
        if (isGlobalHinting && font instanceof NFontTrueType) {

            boolean found = false;
            if (allFontsHinted) {
                ((NFontTrueType) font).setHinting(true);
                found = true;
            }
            if (!found && font != null) {
                String fontName = font.getName().toLowerCase();
                fontName = FontUtil.removeBaseFontSubset(fontName);
                String fontFamily = font.getFamily().toLowerCase();
                for (String name : hintingEnabledNames) {
                    if (fontName.contains(name) ||
                            fontFamily.contains(name)) {
                        ((NFontTrueType) font).setHinting(true);
                        found = true;
                        break;
                    }
                }
            }
            if (!found && fontDescriptor != null) {
                String fontName = fontDescriptor.getFontName();
                fontName = FontUtil.removeBaseFontSubset(fontName);
                if (fontName != null) {
                    fontName = FontUtil.guessFamily(fontName);
                    fontName = fontName.toLowerCase();
                }
                String fontFamily = fontDescriptor.getFontFamily().toLowerCase();
                for (String name : hintingEnabledNames) {
                    if (fontName != null &&
                            (fontName.contains(name) ||
                                    fontFamily.contains(name))) {
                        ((NFontTrueType) font).setHinting(true);
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                String baseFontName = basefont;
                baseFontName = FontUtil.removeBaseFontSubset(baseFontName);
                if (baseFontName != null) {
                    baseFontName = baseFontName.toLowerCase();
                }
                for (String name : hintingEnabledNames) {
                    if (baseFontName != null) {
                        if (baseFontName.contains(name)) {
                            ((NFontTrueType) font).setHinting(true);
                            break;
                        }
                    }
                }
            }
        }

        inited = true;
    }

    /**
     * Utility method for setting the widths for a particular font given the
     * specified encoding.
     *
     * @param font     font to have its width set
     * @param encoding encoding used.
     * @return a new font with the specified width and encoding.
     */
    private NFontSimple setWidth(NFontSimple font, Encoding encoding) {
        int ascent = 100;
        int descent = 100;
        int missing = -1;
        Rectangle2D bbox = null;

        // An array of (LastChar ? FirstChar + 1) widths, each element being the
        // glyph width for the character code that equals FirstChar plus the array index.
        List tmp = (List) library.getObject(entries, WIDTHS_KEY);
        if (tmp != null) {
            widths = new float[tmp.size()];
            for (int i = 0, max = tmp.size(); i < max; i++) {
                widths[i] = ((Number) tmp.get(i)).floatValue();
            }
        }

        if (widths != null) {
            if (fontDescriptor != null) {
                ascent = (int) fontDescriptor.getAscent();
                descent = (int) fontDescriptor.getDescent();
                missing = (int) fontDescriptor.getMissingWidth();
                PRectangle tmpBBox = fontDescriptor.getFontBBox();
                // allocated the original two points that define the rectangle,
                // as they are needed by the NFont
                if (tmpBBox != null) {
                    bbox = tmpBBox.getOriginalPoints();
                    bbox.setRect(bbox.getX(), bbox.getY(), bbox.getWidth(), bbox.getHeight());
                }
            }
            // Assignes the First charater code defined in the font's Widths array
            Object o = library.getObject(entries, FIRST_CHAR_KEY);
            if (o != null) {
                firstchar = ((Number) o).intValue();
            }
        }
        // core 14 won't have a widths attributes, so get them from afm files
        else {
            firstchar = 32; // default for core 14
            String conicalName = getCanonicalName(basefont);
            // the conical name might be null in some corner cases, do to non
            // conforming PDFs.
            if (conicalName != null) {
                widths = AFM.AFMs.get(conicalName.toLowerCase()).getWidths();
            }
            if (widths != null) {
                isAFMFont = true;
            }
            // PDF syntax error
            if (widths == null) {
                // fail out
            }
            // no need to make changes to widths
            else if (encoding != null && encoding.equals(Encoding.ADOBE_STANDARD)) {
//                System.out.println("Found standard encoding" +" ");
            } else if (encoding != null) {
                // sort widths to match font encodings.
                float[] newWidths = new float[256 - firstchar];
                int afmLength = newWidths.length;
                String charName;
                for (int i = 0; i < afmLength; i++) {
                    charName = encoding.getName((char) (i + firstchar));
                    int standard = Encoding.ADOBE_STANDARD.getChar(charName) - firstchar;
                    if (standard >= 0 && standard < widths.length) {
                        newWidths[i] = widths[standard];
                    }
                }
                widths = newWidths;
                missing = AFM.AFMs.get(getCanonicalName(basefont).toLowerCase()).getAvgWidth();
            }
        }
        // widths are a bit strange,  generally seems to work well except for
        // OpenType fonts.
        if (widths != null && !isAFMFont
                && !(font instanceof NFontOpenType)) {
            font = font.deriveFont(widths, firstchar, missing, ascent,
                    descent,
                    bbox // bbox has first and second point that make up rectangle in PDF notation
            );
            return font;
        } else {
            return font;
        }
    }

    /**
     * Utility method for setting the encoding for a particular font.
     *
     * @param font          font to set encoding of
     * @param toUnicodeCMap cmap to Unicode characters
     * @param isEmbedded    true if font is embedded, false otherwise
     * @return new simiple font with the specified encoding.
     */
    private NFontSimple setEncoding(NFontSimple font, CMap toUnicodeCMap,
                                    boolean isEmbedded) {
        encoding = null;
        Object fontEncodingObject = null;
        if (entries.containsKey(ENCODING_KEY)) {
            // may also need the cmap data from Encoding attribute
            fontEncodingObject = library.getObject(entries, ENCODING_KEY);
        }
        if (fontEncodingObject == null) {
            if (isCore14(basefont)) {
                if (basefont.startsWith("ZapfD")) {
                    encoding = Encoding.ZAPF_DINGBATS;
                } else if (basefont.startsWith("Symbol")) {
                    encoding = Encoding.SYMBOL;
                } else {
                    encoding = Encoding.ADOBE_STANDARD;
                }
            }
        }
        // the font can have a named encoding or it can have an encoding dictionary
        else if (fontEncodingObject instanceof Name) {
            if (basefont.equals("ZapfDingbats")) {
                encoding = Encoding.ZAPF_DINGBATS;
            } else if (basefont.equals("Symbol")) {
                encoding = Encoding.SYMBOL;
            } else {
                encoding = Encoding.getInstance(fontEncodingObject.toString());
            }
        } else {//if (fontEncodingObject instanceof Hashtable) {
            /**
             * Encoding dictionaryies contain two entries
             * BaseEncoding -> name
             * Differences -> array
             */
            HashMap encodingDictinary = (HashMap) fontEncodingObject;
            String baseEncoding = null;
            if (encodingDictinary.containsKey(BASEENCODING_KEY)) {
                baseEncoding =
                        encodingDictinary.get(BASEENCODING_KEY).toString();
            }
//            System.out.println("--> BASE font " + basefont);
            if (baseEncoding != null) {
//                System.out.println("baseEncdoing, getting instance " + baseEncoding);
                encoding = Encoding.getInstance(baseEncoding);
            } else if (isEmbedded) {
//                System.out.println("--> Embedded");
                encoding = font.getEncoding();
            } else if (basefont == null) {
                encoding = Encoding.ADOBE_STANDARD;
            } else if (basefont.startsWith("ZapfD")) {
//                System.out.println("--> found ZapFD");
                encoding = Encoding.ZAPF_DINGBATS;
            } else if (basefont.startsWith("Symbol")) {
//                System.out.println("--> symbol ");
                encoding = Encoding.SYMBOL;
            } else {
//                System.out.println("--> default standard");
                encoding = Encoding.ADOBE_STANDARD;
            }

            // get differences array
            Object[] stringDifferences = null;
            if (encodingDictinary.containsKey(DIFFERENCES_KEY)) {
                Object differenceObject = encodingDictinary.get(DIFFERENCES_KEY);
                if (differenceObject instanceof Reference) {
                    differenceObject = library.getObject((Reference) differenceObject);
                }
                if (differenceObject instanceof List) {
                    List differences = (List) differenceObject;
                    final int sz = differences.size();
                    stringDifferences = new Object[sz];
                    Object tmp;
//                    for(int i= stringDifferences.length - 1; i >= 0; i--){
                    for (int i = 0; i < sz; i++) {
                        tmp = differences.get(i);
                        if (tmp instanceof Number) {
                            stringDifferences[i] = tmp;
                        } else if (tmp instanceof Name) {
                            stringDifferences[i] = tmp.toString();
                        }
                    }
                }
            }
            encoding = new Encoding(encoding, stringDifferences);
        }
        // PDF-722 it seems that if the font is derived (EFERR+fontName) then we
        // can't trust the encoding defined in the PDF file and use the font's
        // encoding directly.  This to also only apply to WinAnsi Encoding
        if (!((font instanceof NFontTrueType && !(font instanceof NFontOpenType)) &&
                fontDescriptor != null &&
                fontDescriptor.getFontName() != null &&
                fontDescriptor.getFontName().contains("+") &&
                encoding != null &&
                encoding.getName().startsWith("WinAnsi"))) {
            font = font.deriveFont(encoding, toUnicodeCMap);
        }else{
            // we can't use the encoding as it not correct for display but we need it for the toUnicode mapping.
            if (font instanceof  NFontTrueType) {
                if (toUnicodeCMap == null) {
                    font = ((NFontTrueType) font).deriveFont(encoding.guessToUnicode());
                }else{
                    font = font.deriveFont(encoding, toUnicodeCMap);
                }
            }
        }
        return font;
    }

    /**
     * Gets the widths of the given <code>character</code> and appends it to the
     * current <code>advance</code>
     *
     * @param character character to find width of
     * @param advance   current advance of the character
     */
    public float getWidth(int character, float advance) {
        //System.out.println("char " + character + " " + (character - firstchar));
        character -= firstchar;
        if (widths != null) {
//            System.out.println("Width " + (int)character + " " + fontDescriptor.getAverageWidth() + " " + widths.length);
            if (character >= 0 && character < widths.length) {
                if (!font.getFamily().equals("Type 3") && widths[character] >= 0) {
                    return (widths[character]) / 1000f;
                }
            } else {
                return fontDescriptor.getAverageWidth() / 1000f;
            }
        }
        // find any widths in the font descriptor
        else if (fontDescriptor != null) {
            //System.out.println("Missing width " + fontDescriptor.getMaxWidth());
            if (fontDescriptor.getAverageWidth() > 0) {
                return fontDescriptor.getAverageWidth() / 1000f;
            } else if (fontDescriptor.getMissingWidth() > 0) {
                return fontDescriptor.getMissingWidth() / 1000f;
            }
        }
        return advance;
    }

    /**
     * Gets the version number of ICEpdf rendering core.  This is not the version
     * number of the PDF format used to encode this document.
     *
     * @return version number of ICEpdf's rendering core.
     */
    public static String getLibraryVersion() {
        return ProductInfo.PRIMARY + "." + ProductInfo.SECONDARY + "." +
                ProductInfo.TERTIARY + " " + ProductInfo.RELEASE_TYPE;
    }


//    class Demo extends JFrame {
//
//        private NFont font;
//        private int firstChar;
//        private int lastChar;
//
//        public Demo(NFont font, int firstChar, int lastChar) {
//            super("ATJ demo");
//            this.addWindowListener(new WindowAdapter() {
//                public void windowClosing(WindowEvent e) {
//                    setVisible(false);
//                    dispose();
//                }
//            });
//            this.font = font;
//            this.firstChar = firstChar;
//            this.lastChar = lastChar;
//            setBounds(100, 100, 600, 500);
//            setVisible(true);
//        }
//
//
//        public void paint(Graphics g_old_api) {
//            Graphics2D g = (Graphics2D) g_old_api;
//            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
//
//            int w = getWidth(), h = getHeight();
//            g.setColor(Color.WHITE);
//            g.fillRect(0, 0, w, h);
//            g.setColor(Color.RED);
//            g.drawLine(0, 0, w, h);
//
//
//            font = font.deriveFont(10f);
//            float width = 10;
//            //for (int i= firstChar, max = lastChar; i <= lastChar; i++){
//            for (int i= 0, max = 300; i <= max; i++){
//                font.drawString(g, (char)i+"", width, 300f);
//                width += font.echarAdvance((char)i).getX();
//            }
//        }
//    }
}
