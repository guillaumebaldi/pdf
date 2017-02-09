package org.icepdf.core.pobjects.fonts.nfont;

import org.icepdf.core.pobjects.fonts.FontFile;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.net.URL;


/**
 * Type 0 composite nfont, to the extent needed by PDF.
 * PDF has exactly one descendant nfont.
 * The methods that in other NFont's operate on encoded characters here instead operate on CIDs;
 * Type 0 strings should first be normalized to CIDs with {@link #toCID(String)} before invoking other methods.
 *
 * @version $Revision: 1.2 $ $Date: 2007/04/09 20:44:01 $
 */
public class NFontType0 extends NFont implements Cloneable {
    public static final String FORMAT = "Type0";

    private FontFile[] df_;
    private CMap e2cid_ = CMap.IDENTITY;
    private String base_;
    private int spacech_ = Integer.MIN_VALUE;


    public NFontType0(String base, FontFile[] descendants) {
        super(null);
        base_ = base != null ? base : "(Type0)";
        df_ = descendants;
    }

    //    public NFontType0 deriveFont(float pointsize) {
    public NFont deriveFont(float pointsize) {
        NFontType0 f = (NFontType0) super.deriveFont(pointsize);
        f.df_ = df_.clone();
        for (int i = 0, imax = df_.length; i < imax; i++)
            f.df_[i] = df_[i].deriveFont(pointsize);
        //assert f.df_[0].getSize() == pointsize;
        if (f.df_[0].getSize() != pointsize) {
            throw new IllegalStateException();
        }
        return f;
    }

    //    public NFontType0 deriveFont(AffineTransform at) {
    public NFont deriveFont(AffineTransform at) {
        NFontType0 f = (NFontType0) super.deriveFont(at);
        for (int i = 0, imax = df_.length; i < imax; i++)
            f.df_[i] = df_[i].deriveFont(at);
        return f;
    }

    public NFontType0 deriveFont(CMap e2cid, CMap toUnicode) {
        NFontType0 f = (NFontType0) deriveFont(size_);
        f.e2cid_ = e2cid != null ? e2cid : CMap.IDENTITY/*_H?*/;
        f.touni_ = toUnicode != null ? toUnicode : f.e2cid_;
        return f;
    }

    public ByteEncoding getByteEncoding() {
        if (df_[0] instanceof NFontOpenType || df_[0] instanceof NFontType1) {
            return e2cid_.isOneByte() ? ByteEncoding.ONE_BYTE : ByteEncoding.TWO_BYTE;
        } else {
            // TrueType fonts
            ByteEncoding byteEncoding = df_[0].getByteEncoding();
            if (byteEncoding == null){
                // generally always two_byte as far as we've seen
                if (e2cid_ instanceof CMapIdentityH ||
                        e2cid_.isTwoByte()) {
                    byteEncoding = ByteEncoding.TWO_BYTE;
                }else{
                    // cmap files generally only one byte.
                    byteEncoding = ByteEncoding.ONE_BYTE;
                }
            }
            return byteEncoding;
        }
    }

    public String toCID(String estr) {
        return e2cid_.toSelector(estr);
    }


    public String getName() {
        return base_;
    }

    public String getFamily() {
        return "(Type0)";
    }

    public int getStyle() {
        return df_[0].getStyle();
    }

    public String getFormat() {
        return df_[0].getFormat()/*FORMAT ?*/;
    }

    public int getRights() {
        return df_[0].getRights();
    }

    public boolean isHinted() {
        return df_[0].isHinted();
    }

    @Override
    public URL getSource() {
        return df_[0].getSource();
    }

    public int getNumGlyphs() {
        int cnt = 0;
//        for (NFont f : df_)
        for (int i = df_.length - 1; i >= 0; i--)
            cnt += df_[i].getNumGlyphs();
        return cnt;
    }

    //public NFont getDescendant(int id) { return df_[id]; }
    //public int getNumDescendants() { return df_.length; }	// always 1 -- just support PDF Type 0, not general PostScript Type 0


    public Point2D echarAdvance(char ech) {
        return df_[0].echarAdvance(ech);
    }

    public Rectangle2D getMaxCharBounds() {
        return df_[0].getMaxCharBounds();
    }

    public boolean canDisplayEchar(char ech) {
        return df_[0].canDisplayEchar(ech);
    }

    public String toUnicode(char displayChar) {
        // make sure to check this instance and the child font for valid unicode mapping.
        return df_[0].getToUnicode() != null && !df_[0].getToUnicode().isEmptyMapping() ?
                df_[0].toUnicode(displayChar) :
                touni_ != null && !touni_.isEmptyMapping()? touni_.toUnicode(displayChar): String.valueOf(displayChar);
    }

    public char getSpaceEchar() {
        if (spacech_ == Integer.MIN_VALUE) {
            if (touni_ != null)
                spacech_ = touni_.fromSelector(' ');
            else
                spacech_ = df_[0].getSpaceEchar();
        }
        return (char) spacech_;
    }

    public Rectangle2D getEstringBounds(String estr, int beginIndex, int limit) {
        return df_[0].getEstringBounds(estr, beginIndex, limit);
    }

    public void drawEstring(Graphics2D g, String estr, float x, float y, long layout, int mode, Color strokecolor) {
        df_[0].drawEstring(g, estr, x, y, layout, mode, strokecolor);
    }

    public Rectangle2D getCharBounds(char displayChar) {
        return getEstringBounds(String.valueOf(displayChar), 0, 1);
    }

    public Shape getEstringOutline(String estr, float x, float y) {
        return df_[0].getEstringOutline(estr, x, y);
    }
}
