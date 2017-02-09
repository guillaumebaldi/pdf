package org.icepdf.core.pobjects.fonts.nfont;

import org.icepdf.core.pobjects.fonts.nfont.lang.Integers;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;


/**
 * Cache of glyph splines cached as bitmaps.
 * Faster because: (1) image over filled spline and (2) doesn't kill rendering pipeline vs <code>g.transform(); g.draw(Shape)</code>.
 *
 * @author Copyright (c) 2005  Thomas A. Phelps.  All rights reserved.
 * @version $Revision: 1.1 $ $Date: 2005/07/12 21:33:19 $
 */
public class CacheBitmap {
    /**
     * No bitmap caching is available at that size.
     */
    public static final CacheBitmap NONE = new CacheBitmap();

    private static final Color COLOR_TRANSPARENT = new Color(0, 0, 0, 0);    // no dependency on Colors.TRANSPARENT
    private static final int RGB_MASK = 0x00ffffff, ALPHA_MASK = 0xff000000;


    private static final int BIG = 500;
    /**
     * Glyph ID to bitmap, for smaller glyph counts.
     */
    private BufferedImage[] g2b_;
    /**
     * Glyph ID to bitmap, for larger glyph counts.
     */
    private Map<Integer, BufferedImage> g2big_; ///*SoftReference*/Map<Integer,BufferedImage> g2big_;

    private AffineTransform u_;
    // dimensions of maxbbox in pixels used for cached bitmaps since advance not always width.
    private double imgx_, imgy_;
    private int imgw_, imgh_;

    /**
     * Shared buffer for color conversions.
     */
    private int[] buf_;

    private Color color_ = Color.black;


    private CacheBitmap() {
        imgx_ = imgy_ = 0.0;
    }

    private CacheBitmap(NFont font, AffineTransform m) {
        float size = font.getSize();
        u_ = new AffineTransform(m);
        u_.concatenate(font.getTransform());
        u_.scale(size, size);

        Rectangle2D bbox = font.getMaxCharBounds();
        imgx_ = Math.floor/*negative*/(bbox.getX());
        imgy_ = Math.floor(bbox.getY());    // floor so do not participate in rounding
        imgw_ = (int) Math.ceil(bbox.getWidth()) + 1/*extra from floor(getX())*/;
        imgh_ = (int) Math.ceil(bbox.getHeight()) + 1;

        buf_ = new int[imgw_ * imgh_];

        int gidmax = font.getMaxGlyphNum();
        if (gidmax < BIG) {    // give up 2K for faster, 4 bytes per vs 20+overhead for hash so smaller if use > 100 glyphs
            g2b_ = new BufferedImage[gidmax + 1/*guard against off by 1 error in nfont data*/];
            g2big_ = null;
        } else {
            g2b_ = null;
            g2big_ = new HashMap<Integer, BufferedImage>(100);// new HashMap<Integer,BufferedImage>(100);
        }
    }

    /**
     * Returns cache for nfont at particular size, transform, ...., or {@link #NONE} if caching available for those dimensions.
     */
    /*package-private*/
    static CacheBitmap getInstance(NFont font, AffineTransform m) {    // extract attributes from NFont to simplify use by fonts
        AffineTransform at = font.getTransform();
        float size = font.getSize();
        return //NFont.isUseBitmaps() => nfont use of bitmaps dynamic on this flag
                at.getShearY() == 0.0 && at.getShearX() == 0.0    // no rotation -- intrinsic affine can have slope for fabricated italics, which is allowed
                        //&& at.getScaleX() == at.getScaleY()	// transform possibly outside of maxbbox
                        && size * at.getScaleX() <= 24f && size * at.getScaleY() <= 24f    // small/medium... leave to client to decide?
                        //X && newwidths_==null
                        ? new CacheBitmap(font, m)
                        : NONE;
    }


    public double dx() {
        return imgx_;
    }

    public double dy() {
        return imgy_;
    }

    public void setColor(Color color) {
        color_ = color;
    }


    /**
     * Bitmap cache of glyph, per color.
     */
    public Image getBitmap(int gid, Shape glyph, /*double width,--advance not enough*/ double sx) {
        // MT: sychronize on cache... or not since at worst both create same glyph
        BufferedImage img =
                g2b_ != null ? g2b_[gid] :
                        g2big_.get(Integers.getInteger(gid));    // no object creation for gid < 1000

        if (img == null) {
            img = new BufferedImage(imgw_, imgh_, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) img.getGraphics();    // expensive on OS X -- adds 0.10 ms/glyph, amortized!
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.translate(-imgx_, -imgy_);
            g.transform(u_);
            g.scale(sx, 1.0);
            g.setColor(color_);
            g.fill(glyph);
            g.dispose();

            // pixel (0,0) has individual glyph's color, as injected transparent if necessary -- then recolor on demand (not all glyphs or new cache for change of color)
            if (img.getRGB(0, 0) == 0)
                img.setRGB(0, 0, color_.getRGB() & RGB_MASK);

            if (g2b_ != null) g2b_[gid] = img;
            else g2big_.put(Integers.getInteger(gid), img);

        } else {    // correct color?
            int rgb = color_.getRGB() & RGB_MASK;
            if (rgb != (img.getRGB(0, 0) & RGB_MASK)) {
                int[] buf = buf_;
                img.getRGB(0, 0, imgw_, imgh_, buf, 0, imgw_);    // should still be accelerated, but not in OS X (as of 10.4.1)
                for (int i = 0, imax = buf.length; i < imax; i++)
                    if (buf[i] != 0)
                        buf[i] = (buf[i] & ALPHA_MASK) | rgb;    // no additional alpha with antialiasing
                img.setRGB(0, 0, imgw_, imgh_, buf, 0, imgw_);

                if (img.getRGB(0, 0) == 0)
                    img.setRGB(0, 0, /*(0x0<<24 -- transparent) |*/ rgb);    // mark
            }
        }

        return img;
    }
}
