/**
 * Copyright (C) 2005, ICEsoft Technologies Inc.
 */
package org.icepdf.core.pobjects.fonts.nfont;

import org.icepdf.core.pobjects.Name;
import org.icepdf.core.pobjects.PRectangle;
import org.icepdf.core.pobjects.Resources;
import org.icepdf.core.pobjects.Stream;
import org.icepdf.core.pobjects.graphics.GraphicsState;
import org.icepdf.core.pobjects.graphics.Shapes;
import org.icepdf.core.pobjects.graphics.TextState;
import org.icepdf.core.util.Library;
import org.icepdf.core.util.content.ContentParser;
import org.icepdf.core.util.content.ContentParserFactory;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>The class represents a PDF document's Type 3 Font.
 * Type 3 fonts differ from other PDF fonts.  The dictionary defines the font;
 * font dictionaries for other fonts simply contain information about the font and
 * refer to a separate font program for the actual glyph descriptions.</p>
 * <p/>
 * <p>In Type 3 fonts, glyphs are defined by streams of PDF graphics operators.
 * These streams are associated with character names.  A separate encoding entry
 * maps character codes to appropriate character names for the glyphs.</p>
 * <p/>
 * <p>Type 3 fonts are more flexible than Type 1 fonts because the glyph
 * descriptions may contain arbitrary PDF graphics operators. However, Type 3
 * fonts have no hinting mechanism for improving output at small sizes or low
 * resolutions. A Type 3 font dictionary contains the entries listed below:</p>
 * <p/>
 * <table border="1" cellpadding="1" >
 * <tr>
 * <td><b>Key</b></td>
 * <td><b>Type</b></td>
 * <td><b>Value</b></td>
 * </tr>
 * <tr>
 * <td>Type</td>
 * <td>name</td>
 * <td>(Required) The type of PDF object that this dictionary describes;
 * must be Font for a font dictionary.</td>
 * </tr>
 * <tr>
 * <td>Subtype</td>
 * <td>name</td>
 * <td>(Required) The type of font; must be Type3 for a Type 3 font.</td>
 * </tr>
 * <tr>
 * <td>Name</td>
 * <td>name</td>
 * <td>(Required in PDF 1.0; optional otherwise)</td>
 * </tr>
 * <tr>
 * <td>FontBBox</td>
 * <td>rectangle</td>
 * <td><p>(Required) A rectangle expressed in the glyph coordinate system,
 * specifying the font bounding box. This is the smallest rectangle
 * enclosing the shape that would result if all of the glyphs of the
 * font were placed with their origins coincident and then filled.</p></td>
 * </tr>
 * <tr>
 * <td>FontMatrix</td>
 * <td>rectangle</td>
 * <td>(Required) An array of six numbers specifying the font matrix, mapping
 * glyph space to text space. A common practice is to define glyphs in
 * terms of a 1000-unit glyph coordinate system, in which case the font
 * matrix is [0.001 0 0 0.001 0 0].</td>
 * </tr>
 * <tr>
 * <td>CharProcs</td>
 * <td>dictionary</td>
 * <td>(Required) A dictionary in which each key is a character name and the
 * value associated with that key is a content stream that constructs
 * and paints the glyph for that character. The stream must include as
 * its first operator either d0 or d1, followed by operators describing
 * one or more graphics objects, which may include path, text, or image
 * objects.</td>
 * </tr>
 * <tr>
 * <td>Encoding</td>
 * <td>name</td>
 * <td>(Required) An encoding dictionary whose Differences array specifies
 * the complete character encoding for this font.</td>
 * </tr>
 * <tr>
 * <td>FirstChar</td>
 * <td>integer</td>
 * <td>(Required) The first character code defined in the font's Widths array.</td>
 * </tr>
 * <tr>
 * <td>LastChar</td>
 * <td>integer</td>
 * <td>(Required) The last character code defined in the font's Widths array.</td>
 * </tr>
 * <tr>
 * <td>Widths</td>
 * <td>array</td>
 * <td>(Required; indirect reference preferred) An array of (LastChar ? FirstChar + 1)
 * widths, each element being the glyph width for the character code that
 * equals FirstChar plus the array index. For character codes outside the
 * range FirstChar to LastChar, the width is 0. These widths are interpreted
 * in glyph space as specified by FontMatrix (unlike the widths of a Type 1
 * font, which are in thousandths of a unit of text space).</td>
 * </tr>
 * <tr>
 * <td>FontDescriptor</td>
 * <td>dictionary</td>
 * <td>(Required in Tagged PDF documents; must be an indirect reference) A
 * font descriptor describing the font's default metrics other than its
 * glyph widths.</td>
 * </tr>
 * <tr>
 * <td>Resources</td>
 * <td>dictionary</td>
 * <td>(Optional, but strongly recommended; PDF 1.2) A list of the named resources,
 * such as fonts and images, required by the glyph descriptions in this font.
 * If any glyph descriptions refer to named resources but this dictionary is
 * absent, the names are looked up in the resource dictionary of the page on
 * which the font is used.</td>
 * </tr>
 * <tr>
 * <td>ToUnicode</td>
 * <td>stream</td>
 * <td>(Optional; PDF 1.2) A stream containing a CMap file that maps
 * character codes to Unicode values</td>
 * </tr>
 * </table>
 *
 * @since 2.0
 */
public class NFontType3 extends NFontSimple implements Cloneable {

    private static final Logger logger =
            Logger.getLogger(NFontType3.class.toString());

    public static final Name FONT_BBOX_KEY = new Name("FontBBox");
    public static final Name FONT_MATRIX_KEY = new Name("FontMatrix");
    public static final Name CHAR_PROCS_KEY = new Name("CharProcs");
    public static final Name RESOURCES_KEY = new Name("Resources");

    private Library library;
    private HashMap charProcedures;
    private HashMap<Name, SoftReference<Shapes>> charShapesCache;
    private HashMap<Name, PRectangle> charBBoxes;
    private HashMap<Name, Point2D.Float> charWidths;
    private Resources resources;
    protected HashMap entries;
    private AffineTransform glyph2user;
    private PRectangle bBox;

    private Resources parentResource;

    /**
     * <p>Creates a new Type3 Font program.</p>
     *
     * @param library    PDF document's object library
     * @param properties dictionary value associated with this object
     */
    public NFontType3(Library library, HashMap properties) {
        super(null);
        this.library = library;
        // default transformation
        glyph2user = new AffineTransform(1.0f, 0.0f, 0.0f, 1f, 0.0f, 0.0f);

        // reference to properties dictionary for later use.
        entries = properties;

        // build the BBox
        Object o = library.getObject(properties, FONT_BBOX_KEY);
        if (o instanceof List) {
            List rectangle = (List) o;
            // allocated the original two points that define the rectangle,
            // as they are needed by the NFont
            bBox = new PRectangle(rectangle);
            bbox_.setRect(bBox.getX(), bBox.getY(), bBox.getWidth(), bBox.getHeight());
            // couple corner cases of [0 0 0 0] /FontBBox, zero height will not intersect the clip.
            // width is taken care by the /Width entry so zero is fine.
            if (bBox.getHeight() == 0) {
                bBox.height = 1;
            }
        }

        // build font matrix
        o = library.getObject(properties, FONT_MATRIX_KEY);
        if (o instanceof List) {
            List oFontMatrix = (List) o;
            m_ = new AffineTransform(((Number) oFontMatrix.get(0)).floatValue(),
                    ((Number) oFontMatrix.get(1)).floatValue(),
                    ((Number) oFontMatrix.get(2)).floatValue(),
                    ((Number) oFontMatrix.get(3)).floatValue(),
                    ((Number) oFontMatrix.get(4)).floatValue(),
                    ((Number) oFontMatrix.get(5)).floatValue());
        } else {
            m_ = new AffineTransform(0.001f, 0.0f, 0.0f, 0.001f, 0.0f, 0.0f);
        }

        // set default mapping
        touni_ = CMap.IDENTITY;

        // get the glyphs, via loading the CharProcs resources, which should
        // be name stream pairs.
        o = library.getObject(properties, CHAR_PROCS_KEY);
        if (o instanceof HashMap) {
            charProcedures = (HashMap) o;
            int length = charProcedures.size();
            charShapesCache = new HashMap<Name, SoftReference<Shapes>>(length);
            charBBoxes = new HashMap<Name, PRectangle>(length);
            charWidths = new HashMap<Name, Point2D.Float>(length);
        }
    }

    public int getStyle() {
        return PLAIN;
    }

    /**
     * <p>Derive a new Type3 font program using the following font size.</p>
     *
     * @param size in device space of new font.
     * @return new font with a modified size data.
     */
    public NFont deriveFont(float size) {
        NFontType3 nfonttype3 = null;
        try {
            nfonttype3 = (NFontType3) clone();
        } catch (CloneNotSupportedException e) {
            logger.log(Level.FINE, "Could not derive Type3 font ", e);
        }
        if (nfonttype3 != null) {
            nfonttype3.size_ = size;
            nfonttype3.max_ = null;
            nfonttype3.setGlyph2User(at_);
        }
        return nfonttype3;
    }

    /**
     * <p>Derive a new Type3 font program using the specified encoding and Cmap data</p>
     *
     * @param encoding encoding to be used for this font.
     * @param cmap     character map object associated with this font.
     * @return new font with specified encoding and Cmap data.
     */
    public NFontSimple deriveFont(Encoding encoding, CMap cmap) {
        return super.deriveFont(encoding, cmap == null ? CMap.IDENTITY : cmap);
    }

    /**
     * <p>Derives a new Type 3 font program using the specified transformation.</p>
     *
     * @param affinetransform transform to be applied to all glyphs
     * @return a new font where all glyphs are transformed by the specified
     * transform.
     */
    public NFont deriveFont(AffineTransform affinetransform) {
        NFontType3 nfonttype3 = (NFontType3) super.deriveFont(affinetransform);
        nfonttype3.setGlyph2User(affinetransform);
        return nfonttype3;
    }

    /**
     * <p>Gets Font matrics associated with this font.</p>
     *
     * @return affine transform for the mapping of glyph space to text space.
     */
    public AffineTransform getTransform() {
        return new AffineTransform(m_);
    }

    /**
     * <p>Draw the specified string data to a graphics context.</p>
     *
     * @param g2d    graphics context in which the string will be drawn to.
     * @param string string to draw
     * @param x      x-coordinate of the string rendering location
     * @param y      y-coordinate of the string rendering location
     * @param layout layout mode of this font, not value for type3 font
     * @param mode   rendering mode, not applicable for type3 fonts.
     */
    public void drawEstring(Graphics2D g2d, String string,
                            float x, float y,
                            long layout, int mode, Color color) {
        Shapes shape;
        AffineTransform oldTransform = g2d.getTransform();
        AffineTransform currentCTM = g2d.getTransform();
        currentCTM.concatenate(getTransform());
        g2d.setTransform(currentCTM);

        g2d.translate(x / m_.getScaleX(), y / m_.getScaleY());
        g2d.scale(size_, -size_);
        char displayChar;
        try {
            for (int i = 0, length = string.length(); i < length; i++) {
                displayChar = string.charAt(i);
                shape = getGlyph(displayChar, color);
                if (shape != null) {
                    shape.paint(g2d);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.fine("Type3 font painting interrupted.");
        }
        g2d.setTransform(oldTransform);
    }

    /**
     * <p>Calculates the  character advance of the given character.</p>
     *
     * @param displayChar character to get advance for
     * @return advance of the specified character
     */
    public Point2D echarAdvance(char displayChar) {
        String charName = encoding_.getName(displayChar);
        float width = 0f;
        if (charName != null && displayChar >= firstch_ && displayChar <= lastch_) {
            width = (float) (newwidths_[displayChar - firstch_] * m_.getScaleX());
        }

        if (width == 0.0f && charWidths.size() > 0) {
            Object tmp = charWidths.get(charName);
            if (tmp != null) {
                width = (float) (((Point2D.Float) tmp).x * m_.getScaleX());
            }
        }

//        if (logger.isLoggable(Level.FINER)) {
//            logger.finer("echar advance:" + width +
//                    " scale: " + m_.getScaleX() +
//                    " scale: " + at_.getScaleX());
//        }

        return new Point2D.Float((float) (width * size_ * at_.getScaleX()),
                (float) (width * size_ * at_.getShearY()));

    }

    /**
     * <p>Sets the BBox of this Type3 font for the specified name.  This is important
     * for Type3 fonts as it defines the clip.  The trick though, is that the BBox
     * data is stored inside of the glyph's content stream and thus must be set
     * by the content parser.  The PDF operator is "d1".</p>
     *
     * @param name name of the glyph being parsed (Type3 specific)
     * @param bBox bounding box for glyph.
     */
    public void setBBox(Name name, PRectangle bBox) {
        charBBoxes.put(name, bBox);
    }

    public Rectangle2D getMaxCharBounds() {
        AffineTransform af = new AffineTransform();
        af.scale(size_, -size_);
        af.concatenate(m_);
        af.concatenate(at_);
        return af.createTransformedShape(bBox.toJava2dCoordinates()).getBounds2D();
    }

    public Rectangle2D getCharBounds(char displayChar) {
        Rectangle2D r = getMaxCharBounds();

        String charName = encoding_.getName(displayChar);
        float width = 0f;
        if (charName != null && displayChar >= firstch_ && displayChar <= lastch_) {
            width = newwidths_[displayChar - firstch_];
        }

        if (width == 0.0f) {
            width = charWidths.get(charName).x;
        }

        PRectangle charRect = charBBoxes.get(charName);

//        if (logger.isLoggable(Level.FINER)) {
//            logger.finer("width : " + charRect.getWidth() + " " + bBox.getWidth());
//            logger.finer("height : " + charRect.getHeight() + " " + bBox.getHeight());
//        }

        r.setRect(0.0, r.getY(),
                width * size_,
                charRect.getHeight() * size_);
        return r;
    }

    /**
     * <p>Sets the horizontal displacement of this Type3 font for the specified
     * name. Like BBox this value must be set from inside the content parser as
     * the data is stored in the glyph's content stream.  The PDF operator is
     * "d0". </p>
     *
     * @param name         name of the glph being parsed (Type3 specific)
     * @param displacement horizontal and vertical displacement
     */
    public void setHorDisplacement(Name name, Point2D.Float displacement) {
        charWidths.put(name, displacement);
    }

    /**
     * <p>The font's Format.</p>
     *
     * @return font format "Type3"
     */
    public String getFormat() {
        return "Type3";
    }

    /**
     * <p>The font's Name.</p>
     *
     * @return font format "Type 3"
     */
    public String getName() {
        return "Type 3";
    }

    /**
     * <p>The font's Family.</p>
     *
     * @return font format "Type 3"
     */
    public String getFamily() {
        return "Type 3";
    }

    /**
     * <p>Gets the number of glyphs in this Type 3 font family.</p>
     *
     * @return number of glyphs descibed in font family.
     */
    public int getNumGlyphs() {
        return charProcedures.size();
    }

    /**
     * <p>Gets the width of the space character.</p>
     *
     * @return space character, always 32.
     */
    public char getSpaceEchar() {
        return 32;
    }

    public ByteEncoding getByteEncoding() {
        return ByteEncoding.ONE_BYTE;
    }

    /**
     * <p>Can the specified character be displayed by this font program.</p>
     *
     * @param c character to check if displayable.
     * @return true, if character can be displayed; false, otherwise.
     */
    public boolean canDisplay(char c) {
        return canDisplayEchar(c);
    }

    /**
     * <p>Can the specified character be displayed by this font program.</p>
     *
     * @param c character to check if displayable.
     * @return true, if character can be displayed; false, otherwise.
     */
    public boolean canDisplayEchar(char c) {
        return (getGlyph(c, Color.black) != null);
    }

    /**
     * Does the font program use hinted glyphs. Always false for Type3 fonts
     *
     * @return false.
     */
    public boolean isHinted() {
        return false;
    }

    /**
     * Uitility method for mapping glyph space to user space.
     *
     * @param affinetransform transforms all glphs by this transform which is
     *                        adjusted for font size.
     */
    private void setGlyph2User(AffineTransform affinetransform) {
        float fontSize = getSize();
        glyph2user = new AffineTransform(m_);
        AffineTransform affinetransform1 =
                new AffineTransform(affinetransform.getScaleX() * (double) fontSize,
                        affinetransform.getShearY(),
                        affinetransform.getShearX(),
                        -affinetransform.getScaleY() * (double) fontSize,
                        0.0f, 0.0f);
        glyph2user.concatenate(affinetransform1);
    }

    /**
     * Utility method for dynamically loading the needed glyph data for the specified
     * character number
     *
     * @param characterIndex character number of the glyph to display
     * @return a new Shapes object containing paintable data.
     */
    private Shapes getGlyph(int characterIndex, Color fillColor) {
        // Gets the name of the type3 character
        Name charName = new Name(encoding_.getName((char) characterIndex));
        // the same glyph name can have a different fills so we need to store the color in the key
        Name charKey = new Name(encoding_.getName((char) characterIndex) + fillColor.getRGB());
        SoftReference<Shapes> softShapes = charShapesCache.get(charKey);
        if (softShapes == null || softShapes.get() == null) {
            Object o = library.getObject(charProcedures.get(charName));
            if (o instanceof Stream) {
                Stream stream = (Stream) o;

                // get resources if any for char processing content streams.
                if (resources == null) {
                    resources = library.getResources(entries, RESOURCES_KEY);
                }
                if (resources == null) {
                    resources = parentResource;
                }
                ContentParser cp = ContentParserFactory.getInstance()
                        .getContentParser(library, resources);
                // Read the type 3 content stream
                try {
                    GraphicsState gs = new GraphicsState(new Shapes());
                    gs.setFillColor(fillColor);
                    cp.setGraphicsState(gs);
                    cp.setGlyph2UserSpaceScale((float) glyph2user.getScaleX());
                    Shapes charShapes = cp.parse(new byte[][]{stream.getDecodedStreamBytes()}, null).getShapes();
                    TextState textState = cp.getGraphicsState().getTextState();
                    setBBox(charName, textState.getType3BBox());
                    setHorDisplacement(charName, textState.getType3HorizontalDisplacement());
                    charShapesCache.put(charKey, new SoftReference<Shapes>(charShapes));
                    // one could add a bbox clip here, but no example where it is needed
                    // not adding it should speed things up a bit
                    return charShapes;
                } catch (IOException e) {
                    logger.log(Level.FINE, "Error loading Type3 stream data.", e);
                } catch (InterruptedException e) {
                    logger.log(Level.FINE, "Thread Interrupted while parsing Type3 stream data.", e);
                }
            }
        } else {
            return softShapes.get();
        }
        return null;
    }

    /**
     * Returns an empty Area object as type 3 fonts can not be used for
     * clipping paths.
     *
     * @param estr text to calculate glyph outline shape
     * @param x    x coordinate to translate outline shape.
     * @param y    y coordinate to translate outline shape.
     * @return empty Area object.
     */
    public Shape getEstringOutline(String estr, float x, float y) {
        return new Area();
    }

    public Resources getParentResource() {
        return parentResource;
    }

    public void setParentResource(Resources parentResource) {
        this.parentResource = parentResource;
    }
}
