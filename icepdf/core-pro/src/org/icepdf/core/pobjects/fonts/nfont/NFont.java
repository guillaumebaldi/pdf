package org.icepdf.core.pobjects.fonts.nfont;

//X import java.awt.Font; => legacy free!

import org.icepdf.core.pobjects.fonts.FontFile;
import org.icepdf.core.pobjects.fonts.nfont.io.RandomAccessMultiplex;
import org.icepdf.core.pobjects.fonts.nfont.lang.Strings;
import org.icepdf.core.util.Defs;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
//import java.util.logging.Level;
//import java.util.logging.Logger;


/**
 * Superclass for <em>new fonts</em>, which provide more control than {@link java.awt.Font}.
 * Usage is patterned after {@link java.awt.Font}, except that drawing is a method on the nfont rather than a special case of {@link java.awt.Graphics2D}.
 * <p/>
 * <ul>
 * <li>{@link #FLAG_NONE nfont property flags}.  (Flags are identical to Adobe's definitions, with additional as marked.)
 * <li>{@link #WEIGHT_THIN standard weight values}
 * <li>{@link #MODE_FILL display modes}
 * <li>{@link #RIGHT_INSTALL intellectual property nfont usage restriction settings} and {@link #getRights() querying}
 * <li>{@link #NOTDEF_CHAR special characters}
 * <li>{@link #getInstance(String, int, int, float) creation}
 * <li>derive by {@link #deriveFont(float) size}, {@link #deriveFont(AffineTransform) AffineTransform}
 * <li>query metadata: {@link #getName()}, {@link #getFamily()}, {@link #getDesigner()}, {@link #getCopyright()}, {@link #getFormat()}, {@link #getSubformat()},  {@link #getRights()}, {@link #getNumGlyphs()}, {@link #getMaxGlyphNum()}
 * <li>query: {@link #getSize()}, {@link #getFlags()}, {@link #getWeight()}, {@link #getTransform()}, {@link #isTransformed()}, {@link #strFlags(int)}
 * <li>character encoding: {@link #toUnicode(String)}, {@link #fromUnicode(String)}, {@link #getSpaceEchar()}
 * <li>measurement of advance: {@link #stringAdvance(String)}, {@link #stringAdvance(String)}, {@link #estringAdvance(String, int, int)}, {@link #charAdvance(char)}, {@link #echarAdvance(char)}
 * <li>meansurement of bounds: {@link #getStringBounds(String)}, {@link #getStringBounds(String, int, int)}, {@link #getEstringBounds(String, int, int)}, {@link #getMaxCharBounds()}
 * <li>display Unicode: {@link #canDisplay(char)}, {@link #drawString(Graphics2D, String, float, float)}, {@link #drawString(Graphics2D, String, float, float, long, int, Color)}
 * <li>display text in same encoding as nfont, which is necessary when nfont has characters that do not have Unicode mappings:
 * {@link #canDisplayEchar(char)}, {@link #drawEstring(Graphics2D, String, float, float, long, int, Color)}
 * <li>logged, for applications to debug their use of fonts
 * </ul>
 *
 * @version $Revision: 1.4 $ $Date: 2009/03/09 15:50:14 $
 */
public abstract class NFont implements FontFile {

    private static final Logger logger =
            Logger.getLogger(NFont.class.toString());

    public static final int FLAG_NONE = 1 << 31;    // not 0 because would conflict with FLAG_SANSSERIF
    public static final int FLAG_FIXEDPITCH = 1 << 0;
    public static final int FLAG_SERIF = 1 << 1;
    public static final int FLAG_SANSSERIF = 0 << 1;
    /**
     * Not exactly standard Latin; though could be close.  This follows the Adobe definition and does NOT mean dingbat glyphs as in Symbol or Zapf Dingbats.  And it's redundant with {@link #FLAG_NONSYMBOLIC}.
     */
    public static final int FLAG_SYMBOLIC = 1 << 2;
    public static final int FLAG_SCRIPT = 1 << 3;
    /**
     * Standard Latin character set.
     */
    public static final int FLAG_NONSYMBOLIC = 1 << 5;
    public static final int FLAG_ITALIC = 1 << 6;
    public static final int FLAG_ALLCAP = 1 << 16;
    public static final int FLAG_SMALLCAP = 1 << 17;
    /**
     * Force boldface even at small point sizes.
     */
    public static final int FLAG_FORCEBOLD = 1 << 18;
    /**
     * Condensed (narrow) (non-Adobe).
     */
    public static final int FLAG_CONDENSED = 1 << 28;
    /**
     * Expanded (wide) (non-Adobe).
     */
    public static final int FLAG_EXPANDED = 1 << 29;
    /**
     * Ornamental (non-Adobe).
     */
    public static final int FLAG_ORNAMENTAL = 1 << 30;
    public static final int FLAG_DEFAULT = FLAG_SERIF /*| FLAG_NONSYMBOLIC -- causes cache misses*/;
    /**
     * Thin
     */
    public static final int WEIGHT_THIN = 100;
    /**
     * Extra-light (aka Ultra-light)
     */
    public static final int WEIGHT_EXTRALIGHT = 200;
    /**
     * Light
     */
    public static final int WEIGHT_LIGHT = 300;
    /**
     * Normal (aka Regular)
     */
    public static final int WEIGHT_NORMAL = 400;
    /* Another name for {@WEIGHT_NORMAL}.
    public static final int WEIGHT_REGULAR = WEIGHT_NORMAL;*/
    /**
     * Medium
     */
    public static final int WEIGHT_MEDIUM = 500;
    /**
     * Semi-bold (aka Demi-bold)
     */
    public static final int WEIGHT_SEMIBOLD = 600;
    /**
     * Bold
     */
    public static final int WEIGHT_BOLD = 700;
    /**
     * Extra-Bold (aka Ultra-bold)
     */
    public static final int WEIGHT_EXTRABOLD = 800;
    /**
     * Black (aka Heavy)
     */
    public static final int WEIGHT_BLACK = 900;
    /**
     * Fill text [usual].
     */
    public static final int MODE_FILL = 0;
    /**
     * Stroke text [outline].
     */
    public static final int MODE_STROKE = 1;
    /**
     * Fill, then stroke, text.
     */
    public static final int MODE_FILL_STROKE = 2;
    /**
     * Neither fill nor stroke text (invisible) [used in scanned paper].
     */
    public static final int MODE_INVISIBLE = 3;
    /**
     * Fill text and add to path for clipping.
     */
    public static final int MODE_FILL_ADD = 4;
    /**
     * Stroke text and add to path for clipping.
     */
    public static final int MODE_STROKE_ADD = 5;
    /**
     * Fill, then stroke, text and add to path for clipping.
     */
    public static final int MODE_FILL_STROKE_ADD = 6;
    /**
     * Add text to path for clipping.
     */
    public static final int MODE_ADD = 7;
    public static final int MODE_INVALID = -1;

    // intellectual property rights, based on TrueType but not identical
    /**
     * Installable Embedding.
     */
    public static final int RIGHT_INSTALL = 0x0;
    /**
     * Fonts subject to a licensing agreement are legally protected or licensed.  Must check licensing manually.  (Apple only; reserved in Adobe/Microsoft.)
     */
    public static final int RIGHT_LICENSED = 0x1;
    /**
     * Font (if only this bit set) must not be modified, embedded or exchanged in any manner without first obtaining permission of the legal owner.
     */
    public static final int RIGHT_RESTRICTED = 0x2;
    /**
     * Print and preview embedding.
     */
    public static final int RIGHT_PREVIEW_PRINT = 0x4;
    /**
     * Editable embedding.
     */
    public static final int RIGHT_EDITABLE = 0x8;
    /**
     * No subsetting.
     */
    public static final int RIGHT_NOSUBSET = 0x100;
    /**
     * Bitmap embedding only.
     */
    public static final int RIGHT_BITMAP = 0x200;
    // extensions
    /**
     * Right to use for non-commercial purposes.
     */
    public static final int RIGHT_NONCOMMERCIAL = 0x10000;    // TrueType OS/2 fsType is 16-bit field so non conflicts
    /**
     * Rights not directly available to programmatically, and so were heuristically estimated, as by scanning text for legal language.
     */
    public static final int RIGHT_HEURISTIC = 0x20000;
    /* Type 0, Type 3
    public static final int RIGHT_NOTAPPLICABLE = 0x40000;*/
    /**
     * Rights could not be determined programmatically from nfont data.
     * This is because either nfont format does not support this or because the information was not reported.
     * Whether to interpret this as permitting embedding in PDF is left to the caller.
     */
    public static final int RIGHT_UNKNOWN = ~0;    // illegal pattern: all restrictions w/o separate check


    /**
     * Do no layout.  Used if glyph selection and positioning fully controlled by application, or precomputed as in PDF and DVI.
     */
    public static final long LAYOUT_NONE = 0;
    /**
     * Allow Unicode nfont fallback.  That is, if a nfont is missing a glyph, try to find it in another nfont.
     */
    public static final long LAYOUT_FALLBACK = 1 << 0;
    // GSUB
    public static final long LAYOUT_LIGATURE = 1 << 5;
    public static final long LAYOUT_SMALLCAPS = 1 << 6;
    public static final long LAYOUT_SWASH = 1 << 7;
    // GPOS / kern / BASE
    public static final long LAYOUT_KERN = 1 << 10;
    // convenience: no extra flag bits
    /**
     * Minimum formatting necessary for correctness: formatting (tab, softhyphen), (only) necessary ligatures (as in Arabic), nfont fallback.
     */
    public static final long LAYOUT_MINIMUM = LAYOUT_FALLBACK;
    /**
     * Usual text display (convenience for <code>LAYOUT_LIGATURE | LAYOUT_KERN</code>).
     */
    public static final long LAYOUT_NORMAL = /*LAYOUT_NONE;*/LAYOUT_LIGATURE | LAYOUT_KERN;
    public static final String SUBFORMAT_NONE = "none";
    // internal to fonts
    public static final String NOTDEF = ".notdef";
    public static final char NOTDEF_CHAR = '\0';
    public static final char NOTVALID_CHAR = (char) -2;
    public static final double AFM_SCALE = 0.001;

    /**
     * The plain style constant.
     */
    public static final int PLAIN = 0;

    /**
     * The bold style constant.  This can be combined with the other style
     * constants (except PLAIN) for mixed styles.
     */
    public static final int BOLD = 1;

    /**
     * The italicized style constant.  This can be combined with the other
     * style constants (except PLAIN) for mixed styles.
     */
    public static final int ITALIC = 2;


    private static final AffineTransform TRANSFORM_IDENTITY = new AffineTransform();

    private static boolean usebitmaps_ =
            "true".equals(Defs.property("nfont.usebitmaps"))
                    ||
                    (true    // default to false in a couple years when throwaway machine is 2GHz
                            && Defs.property("os.name").indexOf("OS X") == -1// OS X bug makes bitblt slower than filling splines on some but not all machines
                            //&& CPU < 2GHz
                    );

    private static RandomAccessMultiplex plex_ = new RandomAccessMultiplex(8, 32 * 1024);    // 32K buffer because often seeking about within Latin 1 glyphs

    /**
     * Given <var>family</var> and <var>flags</var>, returns best matching nfont, at point size <var>size</var>.
     * Uses system nfont manager.
     */
    public static NFont getInstance(String family, int weight, int flags, float size) {
        return NFontManager.getDefault().getInstance(family, weight, flags, size);
    }

    /**
     * To increase performance, some subclasses may cached glyphs as bitmaps, and in accelerated video memory in Java 5 and later.
     * This is not always desirable because it prevents subpixel positioning, is in fact slower on OS X due to a performance bug in bitmap bitblt'ting, and is not needed on 2GHz or faster machines.
     * The default setting is undefined.
     * Bitmap use can be turned on by the user at runtime, with the command line option <code>-Dfont.usebitmaps=true</code>.
     * <!-- Later, when not needed, just keep methods and return <code>false</code> in getUseBitmaps(). -->
     */
    public static void setUseBitmaps(boolean b) {
        usebitmaps_ = b;
    }

    public static boolean getUseBitmaps() {
        return usebitmaps_;
    }


    // INSTANCES
    protected CMap touni_ = null;
    protected float size_ = 1f;
    protected AffineTransform at_ = TRANSFORM_IDENTITY;
    protected Rectangle2D max_ = null;
    protected URL source_;

    /**
     * Constructor, with source of nfont data, which may be <code>null</code>.
     */
    protected NFont(URL source) {
        source_ = source;
    }

    // constructor-like

    /**
     * Creates nfont a new <var>pointsize</var>, assuming 72 ppi.
     * Note to subclassers: you must make a complete independent instance of the nfont here,
     * even if pointsize and everything else is the same, as other <code>deriveFont</code> methods use this to make a clone and might make subsequent changes.
     */
    public NFont deriveFont(float pointsize) {
        NFont f = null;
        try {
            f = (NFont) clone();
        } catch (CloneNotSupportedException canthappen) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, getClass().getName() + " must 'implements Cloneable'");
            }
        }
        f.size_ = pointsize;
        f.max_ = null;
        return f;
    }

    /**
     * Transform all glyphs by arbitrary affine transform.
     * Note that this sets the absolute transform -- new transforms replace previous ones and do not concatenate together.
     */
    public NFont deriveFont(AffineTransform at) {
        //if (at_.equals(at)) return this; => NO, always create new instance so subclasses can super.deriveFont() and tweak
        NFont f = deriveFont(size_);
        f.at_ = at.isIdentity() ? TRANSFORM_IDENTITY : new AffineTransform(at);
        //f.at_ = new AffineTransform(at_); f.at_.concatenate(at); => not cumulative along example of point size
        return f;
    }

    // metadata

    /**
     * Returns name of nfont, such as "Times-Roman", which is different than the filename.
     */
    public abstract String getName();

    /**
     * Returns name of nfont, such as "Times".
     */
    public abstract String getFamily();

    /**
     * Returns nfont's source file or URL, which is usually a file, or <code>null</code> if nfont was created from <code>byte[]</code> or other non-addressible source.
     */
    public URL getSource() {
        return source_;
    }

    /**
     * Returns version of nfont, or <code>null</code> if there is no version or it is not applicable.
     */
    public String getVersion() {
        return null/*"0.0"?*/;
    }

    /**
     * Returns name of nfont's designer, or <code>null</code> if not available.
     */
    public String getDesigner() {
        return null;
    }

    /**
     * Returns copyright, or <code>null</code> if not set.
     */
    public String getCopyright() {
        return null;
    }

    /**
     * Returns nfont usage rights bit mask.
     */
    public int getRights() {
        return RIGHT_UNKNOWN;
    }

    /**
     * Returns primary format, such as "Type1" or "OpenType".
     */
    public abstract String getFormat();

    /**
     * Returns secondary format, such as "CFF/CID" under primary format "CFF".
     */
    public String getSubformat() {
        return SUBFORMAT_NONE;
    }

    /**
     * Returns number of glyphs defined in nfont.
     */
    public abstract int getNumGlyphs();

    /**
     * Returns highest glyph index + 1, which may be larger than {@link #getNumGlyphs()} if some "slots" are empty.
     */
    public int getMaxGlyphNum() {
        return getNumGlyphs();
    }

    // query

    /**
     * Returns size in pixels.
     */
    public float getSize() {
        return size_;
    }

    /**
     * Returns weight, such as {@link #WEIGHT_BOLD}.
     */
    public int getWeight() {
        return WEIGHT_NORMAL;
    }

    /**
     * Returns additional flags bit mask, such as {@link #FLAG_ITALIC} and {@link #FLAG_FIXEDPITCH}.
     */
    public int getFlags() {
        return FLAG_NONE;
    }

    //public int getMissingGlyphCode() { return NOTDEF_CHAR; }

    /**
     * Returns a copy of the transform associated with this {@link NFont}.
     */
    public AffineTransform getTransform() {
        return new AffineTransform(at_);
    }

    /**
     * Returns whether the affine transform is the identity transform without creating a copy.
     */
    public boolean isTransformed() {
        return !at_.isIdentity();
    }

//    /**
//     * Returns <code>true</code> iff nfont has hinted outlines, which is Type 1 and TrueType is a sign of higher quality.
//     */
//    public abstract boolean isHinted();


    // encoding
    //public abstract String toGlyphs(String chars); => NO, glyphs private
    //public abstract String fromChars(String glyphs);

    /**
     * Translates characters in current encoding into Unicode.
     */
    public String toUnicode(String encoded) {
        return touni_ != null ? touni_.toSelector(encoded) : encoded;
    }

    public String toUnicode(char displayChar) {
        return touni_ != null ? touni_.toUnicode(displayChar) : String.valueOf(displayChar);
    }

    /**
     * Get the toUnicode CMap if associated with this font instance
     *
     * @return
     */
    public CMap getToUnicode() {
        return touni_;
    }

    /**
     * Translates Unicode characters into current encoding.
     * If there is no Unicode mapping for the character, the character {@link #NOTDEF_CHAR} is returned.
     */
    public String fromUnicode(String unicode) {
        return touni_ != null ? touni_.fromSelector(unicode) : unicode;
    }

    /**
     * Returns the character that seems to be used as a space in the current encoding, or {@link #NOTDEF_CHAR} if no such character.
     */
    public abstract char getSpaceEchar();

    // metrics, from FontMetrics
    public Point2D stringAdvance(String str) {
        return stringAdvance(str, 0, str != null ? str.length() : 0);
    }    // stringAdvance() better name than stringWidth() because of rotation and shearing

    public Point2D stringAdvance(String str, int beginIndex, int limit) {
        return estringAdvance(fromUnicode(str), beginIndex, limit);
    }

    public Point2D estringAdvance(String estr, int beginIndex, int limit) {
        double w = 0.0, h = 0.0;
        if (estr != null)
            for (int i = Math.max(beginIndex, 0), imax = Math.min(limit, estr.length()); i < imax; i++) {
                Point2D pt = echarAdvance(estr.charAt(i));
                w += pt.getX();
                h += pt.getY();
            }
        return new Point2D.Double(w, h);
    }

    public Point2D charAdvance(char ch) {
        return echarAdvance(fromUnicode(Strings.valueOf(ch)).charAt(0));
    }

    public abstract Point2D echarAdvance(char ech);

    /**
     * Logical origin, ascent, advance, and height (includes leading).
     */
    public Rectangle2D getStringBounds(String str) {
        return getStringBounds(str, 0, str != null ? str.length() : 0);
    }

    public Rectangle2D getStringBounds(String str, int beginIndex, int limit) {
        return getEstringBounds(fromUnicode(str.substring(beginIndex, limit)), beginIndex, limit);
    }

    public Rectangle2D getEstringBounds(String estr, int beginIndex, int limit) {
        Rectangle2D r = getMaxCharBounds();    // same uni/en/glyph
        r.setRect(0.0, r.getY(), estringAdvance(estr, beginIndex, limit).getX(), r.getHeight());
        return r;
    }

    /**
     * Returns left in rectangle's x, ascent in y, width in width, height in height.
     */
    public abstract Rectangle2D getMaxCharBounds();

    public abstract Rectangle2D getCharBounds(char character);

    /**
     * Returns height of glyphs.
     */
    public double getHeight() {    // convenience method
        if (max_ == null) max_ = getMaxCharBounds();
        return max_.getHeight();
    }

    /**
     * Returns maximum ascent glyphs above baseline.
     */
    public double getAscent() {    // convenience method
        if (max_ == null) {
            max_ = getMaxCharBounds();
        }
        return -max_.getY();
    }

    /**
     * Returns maximum descent of glyphs below baseline.
     */
    public double getDescent() {    // convenience method
        return getHeight() - getAscent();
    }
    //public abstract int getLeading()

    // render

    /**
     * Can the Unicode character <var>uni</var> be rendered?
     */
    public boolean canDisplay(char uni) {
        return canDisplayEchar(fromUnicode(Strings.valueOf(uni)).charAt(0));
    }

    /**
     * Can the character <var>ch</var> in the nfont's encoding be rendered?
     */
    public abstract boolean canDisplayEchar(char ech);

    /**
     * Convenience method for drawing string in {@link #MODE_FILL fill mode}.
     */
    public void drawString(Graphics2D g, String uni, float x, float y) {
        drawString(g, uni, x, y, LAYOUT_KERN, MODE_FILL, null);
    }

    /**
     * Translates Unicode string <var>str</var> to this nfont's encoding and invoke {@link #drawEstring(Graphics2D, String, float, float, long, int, Color)}.
     * Note that some fonts contain characters without Unicode mappings and therefore cannot be shown with this method;
     * in that case, use {@link #drawEstring(Graphics2D, String, float, float, long, int, Color)}.
     */
    public void drawString(Graphics2D g, String uni, float x, float y, long layout, int mode, Color strokecolor) {
        //if ((FLAG_ALLCAPS&layout)!=0) uni = uni.toUpperCase(); ... too late
        String etxt = fromUnicode(uni);
        drawEstring(g, etxt, x, y, layout, mode, strokecolor);
    }

    /**
     * Draw string <var>str</var> <em>in nfont's encoding</em> at (<var>x</var>,<var>y</var>) in drawing mode <var>mode</var>.
     * For good results, set the {@link java.awt.RenderingHints#KEY_ANTIALIASING} hint on the <code>Graphics2D</code>.
     */
    public abstract void drawEstring(Graphics2D g, String estr, float x, float y, long layout, int mode, Color strokecolor);


    /**
     * Decode flag bit positions into humand-readble string.
     */
    public static String strFlags(int flags) {
        StringBuilder sb = new StringBuilder(30);
        if ((FLAG_FIXEDPITCH & flags) != 0) sb.append("/fix");
        if ((FLAG_SERIF) == 0) sb.append("/sans");
        if ((FLAG_SYMBOLIC & flags) != 0) sb.append("/sym");
        if ((FLAG_SCRIPT & flags) != 0) sb.append("/script");
        if ((FLAG_NONSYMBOLIC & flags) != 0) sb.append("/nonsym");
        if ((FLAG_ITALIC & flags) != 0) sb.append("/ital");
        if ((FLAG_ALLCAP & flags) != 0) sb.append("/allcap");
        if ((FLAG_SMALLCAP & flags) != 0) sb.append("/smcap");
        if ((FLAG_FORCEBOLD & flags) != 0) sb.append("/force");
        if ((FLAG_CONDENSED & flags) != 0) sb.append("/cond");
        return sb.toString();
    }

    public String toString() {
        return getName() + "/" + getSize()/*+"/"+at_*/ + " " + getFormat() + "/" + getSubformat()/*+" "+phelps.lang.Classes.getTail(getClass())*/;
    }

    /**
     * Returns file descriptor multiplexer shared among all {@link NFont}s.
     * Applications can use many fonts, requiring many file descriptors.
     * Fonts should share file descriptors, taking as needed and releasing when possible, so as not to exhaust the system.
     */
    protected static RandomAccessMultiplex getMultiplex() {
        return plex_;
    }

    public abstract int getStyle();

    public FontFile deriveFont(org.icepdf.core.pobjects.fonts.Encoding encoding,
                               org.icepdf.core.pobjects.fonts.CMap toUnicode) {
        return null;
    }

    public FontFile deriveFont(float[] widths, int firstCh, float missingWidth,
                               float ascent, float descent, char[] diff) {
        return null;
    }

    public FontFile deriveFont(Map widths, int firstCh, float missingWidth,
                               float ascent, float descent, char[] diff) {
        return null;
    }

    /**
     * Derive a CID font that has a specified DW or W key.
     *
     * @param defaultWidth default width key value
     * @param widths       width key value.
     * @return update font file with specified widths applied.
     */
    public FontFile deriveFont(float defaultWidth, ArrayList widths) {
        return null;
    }

    protected int guessStyle(int style, HashMap fontInfo) {
        if (fontInfo != null) {
            style = guessBoldStyle(style, fontInfo.get("Weight"));
            style = guessItalicStyle(style, fontInfo.get("ItalicAngle"));
            style = guessBoldStyle(style, fontInfo.get("BaseFontName"));
            style = guessItalicStyle(style, fontInfo.get("BaseFontName"));
            style = guessBoldStyle(style, fontInfo.get("FullName"));
            style = guessItalicStyle(style, fontInfo.get("FullName"));
        }
        return style;
    }

    protected int guessBoldStyle(int style, Object value) {
        if (value != null && value instanceof CharSequence) {
            String weight = value.toString().toLowerCase();
            if (weight.contains("bold") ||
                    weight.contains("medium") ||
                    weight.contains("dark") ||
                    weight.contains("black") ||
                    weight.contains("medi") ||
                    weight.contains("demi")
                    ) {
                style |= BOLD;
            }
        }
        return style;
    }

    protected int guessItalicStyle(int style, Object value) {
        if (value != null && value instanceof CharSequence) {
            String weight = value.toString().toLowerCase();
            if (weight.contains("ital") ||
                    weight.contains("obli") ||
                    weight.contains("slant")
                    ) {
                style |= ITALIC;
            }
        }
        if (value != null && value instanceof Number) {
            Integer angle = (Integer) value;
            if (angle != 0) {
                style |= ITALIC;
            }
        }
        return style;
    }

    /**
     * Interesting events are logged to this logger <!-- <code>com.icesoft.pdf.pobjects.fonts.nfont.NFont</code> namespace-->,
     * at the following use of logging levels:
     * { Level#CONFIG} nfont path, fonts found;
     * { Level#FINE} nfont creation.
     * <!--These levels are set for use within this module and will likely need to be calibrated for larger systems;
     * for example, a logging level of {@link Level#FINE} in the system might translate to {@link Level#INFO} in this lower-level library.-->
     * <!--By default the logging level is set to {@link Level#WARNING}, with no handlers.-->
     */
//    public static Logger getLogger() {
//        return Logger.getLogger("org.icepdf.core.pobjects.fonts.nfont");
//    }
    //static { LOGGER.setLevel(Level.WARNING); }	// low-level library doesn't squawk unless problem or more verbose output requested

    /*public boolean equals(Object o) {
      if (o==null || !(o instanceof NFont)) return false;
      NFont f = (NFont)o;
      return getFormat() == f.getFormat()
          && getName().equals(f.getname()) && getFamily().equals(f.getFamily())
          && getSize()==f.getSize() && getWeight()==f.getWeight() && getTransform().equals(f.getTransform())
          && isHinted()==f.isHinted() & getNumGlyphs()==f.getNumGlyphs()
          ;
    }*/
}
