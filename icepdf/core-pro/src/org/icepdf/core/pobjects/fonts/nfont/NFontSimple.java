package org.icepdf.core.pobjects.fonts.nfont;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.net.URL;


/**
 * Superclass for "simple" fonts, which use a single byte for character codes with names, such as Type 1 (but not Type 0).
 * <p/>
 * <ul>
 * <li>glyph shaping to pre-set widths: for PDF
 * </ul>
 *
 * @version $Revision: 1.2 $ $Date: 2006/11/24 17:43:53 $
 */
public/*package-private => NFontType3*/ abstract class NFontSimple extends NFont {
    protected Encoding encoding_ = Encoding.IDENTITY;

    private static final Rectangle2D BBOX_DEFAULT = new Rectangle2D.Double(0.0, 0.0, 1.0, 1.0);

    /**
     * Character names indexed by glyph id.
     */
    protected Encoding intrinsic_ = null;
    protected float[] newwidths_ = null;
    protected boolean[] pdfbad_ = null;
    protected int firstch_ = 0, lastch_ = 255;    // refer to /Widths, no necessarily glyphs
    // flag to determine if we likely have a normal ascii range font or
    // if we have cid mapping which may help determine how we handle different widths.
    protected boolean nonAsciiSet;
    protected int missingwidth_ = -1;
    protected int newascent_ = 0, newdescent_ = 0;
    /**
     * Translation from glyph space to text space.
     */
    protected AffineTransform m_;
    /**
     * left, bottom, right, top.
     */
    protected Rectangle2D bbox_ = BBOX_DEFAULT;


    protected NFontSimple(URL source) {
        super(source);
    }

    /**
     * Creates nfont with glyphs shaped horizontally to match passed <var>widths</var> (in character space)
     * and shaped vertically to match <var>ascent</var> and <var>descent</var>.
     * Used by PDF on substituted fonts.
     */
    public NFontSimple deriveFont(float[] widths, int firstch, int missing, int ascent, int descent, Rectangle2D bbox) {
        NFontSimple f = (NFontSimple) deriveFont(size_);
        int lastch = firstch + widths.length - 1;
        f.newwidths_ = widths;
        f.firstch_ = firstch;
        f.lastch_ = lastch;    // null OK
        if (f.firstch_ < 32 && f.lastch_ < 100) {
            f.nonAsciiSet = true;
        }
        if (widths != null) {
            boolean[] pdfbad = new boolean[widths.length];
            Encoding en = getEncoding();    // client must set /Encoding before /Widths
            //X for (int i=0300; i<=0377; i++) in WIN_ANSI => seen adieresis encoded twice
            for (int i = firstch; i <= lastch; i++) {
                String name = en.getName(i);
                char ch = Encoding.WIN_ANSI.getChar(name);
                if ((0300 <= ch && ch <= 0377) || (0221 <= ch && ch <= 0224))
                    pdfbad[i - firstch] = true;
            }
            f.pdfbad_ = pdfbad;
        }
        f.missingwidth_ = missing;
        //f.newascent_ = ascent / (m_.getScaleY() * 1000f); f.newdescent_ = descent / (m_.getScaleY() * 1000f);
        f.newascent_ = ascent;
        f.newdescent_ = descent;
        if (bbox != null) {
            double scalex = AFM_SCALE / m_.getScaleX(), scaley = AFM_SCALE / m_.getScaleY();
            f.bbox_ = new Rectangle2D.Double(bbox.getX() * scalex, bbox.getY() * scaley, bbox.getWidth() * scalex, bbox.getHeight() * scaley);
        }
        return f;
    }

    /**
     * Encodings are for fonts byte-addressed glyphs -- 256 or fewer active glyphs.
     */
    public NFontSimple deriveFont(Encoding encoding, CMap toUnicode) {
        NFontSimple f = (NFontSimple) deriveFont(size_);
        if (encoding != null)
            f.encoding_ = encoding;    // if null, keep previous
        f.touni_ = toUnicode != null ? toUnicode : f.encoding_ != null ? f.encoding_.guessToUnicode() : CMap.IDENTITY;
        return f;
    }


    public Encoding getEncoding() {
        return encoding_;
    }


    public Rectangle2D getMaxCharBounds() {
        // left, bottom, right, top => left, ascent, width, height
        double[] pts = new double[]{bbox_.getX() * size_, bbox_.getY() * size_, bbox_.getWidth() * size_, bbox_.getHeight() * size_};
        m_.deltaTransform(pts, 0, pts, 0, 2);
        at_.deltaTransform(pts, 0, pts, 0, 2);
        if (pts[3] < 0.0) {
            pts[1] = -pts[1];
            pts[3] = -pts[3];
        }    // PostScript => Java coordinates
        return new Rectangle2D.Double(pts[0], -pts[3], pts[2] - pts[0], pts[3] - pts[1]);
    }
}
