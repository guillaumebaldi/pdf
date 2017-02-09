package org.icepdf.core.pobjects.fonts.nfont;

import org.icepdf.core.pobjects.fonts.nfont.lang.Arrayss;
import org.icepdf.core.pobjects.fonts.nfont.lang.Mac;

import java.awt.*;
import java.awt.font.OpenType;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OpenType and Apple Advanced Typography (aka TrueType GX) fonts.
 * Supports glyph outlines in TrueType and CFF formats, and layout tables in OpenType and TrueType GX.
 * Supports advanced layout tables, such as kerning, ligatures, and Indic rearrangement.
 * <p/>
 * <p>Advanced layout for both OpenType and TrueType is done through this class.
 * OpenType is a superset of TrueType, and TrueType fonts should be created by this class.
 * <p/>
 * <ul>
 * <li>implemented: all tables in {@link NFontTrueType}, plus CFF <!-- GSUB, GPOS, GDEF -->
 * </ul>
 *
 * @author Copyright (c) 2003 - 2004  Thomas A. Phelps
 * @version $Revision: 1.2 $ $Date: 2006/11/24 17:43:53 $
 */
public class NFontOpenType extends NFontTrueType implements Cloneable {

    private static final Logger logger =
            Logger.getLogger(NFontOpenType.class.toString());

//    public static final String COPYRIGHT = "Copyright (c) 2003 - 2005  Thomas A. Phelps.  All rights reserved.";

    public static final String FORMAT = "OpenType";

    /**
     * OpenType nfont with glyphs in CFF format.
     * Otherwise, subformat is one of {@link NFontTrueType}'s.
     */
    public static final String SUBFORMAT_CFF = "CFF";


    private static final int TAG_OTTO = Mac.intTag("OTTO");
    private static final int TAG_MORX = Mac.intTag("morx");


    private NFontType1 cff_;
    private int glyphcnt_ = -1;
    private boolean flayout_;

    private OTkern kern_;


    public NFontOpenType(URL source, String subFormatType) throws FontFormatException, IOException {
        super(source, subFormatType);
    }

    public NFontOpenType(byte[] data, String subFormatType) throws FontFormatException, IOException {
        super(data, subFormatType);
        // too big to hold entirely in memory in general, but still useful
    }

    /*
public NFontOpenType(RandomAccess ra) throws FontFormatException, IOException {
    super(ra);
    parseFile();
}
*/

    protected void parse() throws IOException {
        flayout_ = false;
        kern_ = null;
        super.parse();

        if (TAG_OTTO == getID()) {
            //assert getTableDirectory(TAG_GLYF) == null;
            if (getTableDirectory(OpenType.TAG_GLYF) != null) {
                throw new IllegalStateException();
            }
            /*
            byte[] b = getTable(TAG_CFF); assert b != null;
            cff_ = new NFontType1(b);
            */
            SfntDirectory td = getTableDirectory(OpenType.TAG_CFF);
            try {
                cff_ = new NFontType1(ra_, td.offset/*NOT getTableOffset(TAG_CFF)*/, td.length);
                cff_.source_ = source_;
            } catch (FontFormatException ffe) {
            }
            cff_.widths_ = widths_;
            cff_ = (NFontType1) cff_.deriveFont(Encoding.IDENTITY, CMap.IDENTITY);    // use my sfnt cmap

        } else {
            cff_ = null;
        }
    }

    //    public NFontOpenType deriveFont(float pointsize) {
    public NFont deriveFont(float pointsize) {
        //NFontOpenType f = null; try { f = (NFontOpenType)clone(); } catch (CloneNotSupportedException canthappen) {}
        NFontOpenType f = (NFontOpenType) super.deriveFont(pointsize);
        if (SUBFORMAT_CFF == getSubformat())
            f.cff_ = (NFontType1) cff_.deriveFont(pointsize);
        return f;
    }

    //    public NFontOpenType deriveFont(AffineTransform at) {
    public NFont deriveFont(AffineTransform at) {
        NFontOpenType f = (NFontOpenType) super.deriveFont(at);
        if (SUBFORMAT_CFF == getSubformat())
            f.cff_ = (NFontType1) cff_.deriveFont(at);
        return f;
    }

    //    public NFontOpenType deriveFont(int[] widths, int firstch, int missing, int ascent, int descent, Rectangle2D bbox) {
    public NFontSimple deriveFont(float[] widths, int firstch, int missing, int ascent, int descent, Rectangle2D bbox) {
        NFontOpenType f = (NFontOpenType) super.deriveFont(widths, firstch, missing, ascent, descent, bbox);
        if (SUBFORMAT_CFF == getSubformat())
            f.cff_ = (NFontType1) cff_.deriveFont(widths, firstch, missing, ascent, descent, bbox);
        return f;
    }


    // don't need getName(), getRight(), ... because these always from TrueType tables, even in CFF charstrings
    public String getFormat() {
        return FORMAT;
    }

    public String getSubformat() {
        return cff_ != null ? SUBFORMAT_CFF : super.getSubformat();
    }

    public boolean isHinted() {
        return SUBFORMAT_CFF == getSubformat() ? cff_.isHinted() : super.isHinted();
    }

    public int getNumGlyphs() {
        if (glyphcnt_ == -1)
            glyphcnt_ = SUBFORMAT_CFF == getSubformat() ? cff_.getNumGlyphs() : super.getNumGlyphs();
        return glyphcnt_;
    }


    private void readLayoutTables() {
        if (flayout_) return;
        flayout_ = true;    // first so if crash don't try again
        if (this != ur_) {
            NFontOpenType ur = (NFontOpenType) ur_;
            ur.readLayoutTables();
            // share tables, which were built after clone()
            kern_ = ur.kern_;

        } else {
            // OpenType (GSUB, GPOS, GDEF, JUST) ...
            /*if (no-kerning-in-gsub)*/
            kern_ = new OTkern(this);

            // ... OR Apple Advanced Typography (AAT)
            //if (getTableDirectory(TAG_MORX)!=null) readMorxTable(); else readMortTable();
        }
    }


    // OPENTYPE
    private void readGSUBTable(/*int flags => in use*/) {
        if (getTable(OpenType.TAG_GSUB).length < 10) return;

        float version = readFixed();
        int ScriptList = readUint16(), FeatureList = readUint16(), LookupList = readUint16();

        // augment flags with language-required
        setOffset(ScriptList);
        for (int i = 0, imax = readUint16(); i < imax; i++) {
            int tag = readUint32();
            int off = readUint16();
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("script: " + Mac.strTag(tag) + " " + Integer.toHexString(tag) + " @ " + off);
            }

            // LangSys...
        }

        setOffset(FeatureList);
        for (int i = 0, imax = readUint16(); i < imax; i++) {
            int tag = readUint32();
            int off = readUint16();
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("feature: " + Mac.strTag(tag) + " " + Integer.toHexString(tag) + " @ " + off);
            }
        }
    }

    // APPLE ADVANCED TYPOGRAPHY
    private void readFeatTable() {
        if (getTable(OpenType.TAG_FEAT).length < 10) return;

        float version = readFixed();
        int featureNameCount = readUint16();
        skip(6);    // reserved

        for (int i = 0; i < featureNameCount; i++) {
            int off0 = getOffset();
            int feature = readUint16();
            int nSettings = readUint16();
            int settingTable = readUint32();
            int featureFlags = readUint16();
            int nameIndex = readInt16();


        }
    }

    private void readMortTable() {
        if (getTable(OpenType.TAG_MORT).length < 6) return;

        float version = readFixed();
        int nChains = readUint32();

        for (int i = 0; i < nChains; i++) {
            int off0 = getOffset();
            // chain header
            int defaultFlags = readUint32();
            int chainLength = readUint32();
            int nFeatureEntries = readUint16();
            int nSubtables = readUint16();

            int flags = defaultFlags;
            for (int j = 0; j < nFeatureEntries; j++) {
                int featureType = readUint16();
                int featureSetting = readUint16();
                int enableFlags = readUint32();
                int disableFlags = readUint32();

                //if (featureType is requesed) flags = (flags&disableFlags) | enableFlags;

                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("mort feature type = " + featureType + ", setting = " + featureSetting);
                }
            }

            setOffset(off0 + chainLength);
        }
    }

    private void readMorxTable() {
        if (getTable(TAG_MORX).length < 6) return;
    }


    /*package-private*/
    Shape getGlyph(int gid) {
        return SUBFORMAT_CFF == getSubformat() ? cff_.getGlyph(gid) : super.getGlyph(gid);
    }


    /**
     * char => CMAP to glyphs => GSUB
     */
    private int[] estr2glyphs(String estr, long layout) {
        readLayoutTables();

        // char => glyph
        int elen = estr.length();
        int[] glyphs = new int[elen * 3];
        int glen = 0;
        for (int i = 0; i < elen; i++) {
            char ech = estr.charAt(i);

            int gid = c2g_.toSelector(ech);
            //if (NOTDEF_CHAR==gid) gid = ur_.c2g_.toSelector(ech);
            glyphs[glen++] = gid;
        }

        // GSUB: language-specific required, and optionally ligature, smallcaps, swash
        // Locate GSUB ScriptList, search for script in LangSys, access feature list, assemble lookups.
        if ((LAYOUT_LIGATURE & layout) != 0) {
        }

        return Arrayss.resize(glyphs, glen);
    }

    private int[] gsub() {
        return null;
    }

    private int[] gpos() {
        return null;
    }

    /**
     * Returns the kerning adjustment between glyph pair, or <code>0</code> if no adjustment.
     */
    public int getKern(int gid1, int gid2) {
        return kern_ != null ? kern_.getKern(gid1, gid2) : 0;
    }

    public int getStyle() {
        if (SUBFORMAT_CFF == getSubformat()) {
            cff_.getStyle();
        } else {
            // no example of the alternative.
        }
        return PLAIN;
    }

    /**
     * Draw using OpenType layout tables <!--and Unicode surrogate pairs-->.
     * <p/>
     * <!--
     * Can't be in NFontOpenType because Apple 'morx'
     * -->
     */
    public void drawEstring(Graphics2D g, String estr, float x, float y, long layout, int mode, Color strokecolor) {
//System.out.println(getName()+" drawE "+layout+" "+estr);
//if (!flayout_) System.out.println(getName()+" drawE "+flayout_+" "+estr);
        // Special mode for PDF that respects widths shaping.
        // Can't do widths shaping here because GSUB loses synchronization between char for newwidths_ and glyph.
        if (true || LAYOUT_NONE == layout) {
            if (SUBFORMAT_CFF == getSubformat())
                cff_.drawEstring(g, c2g_.toSelector(estr), x, y, layout, mode, strokecolor);    // apparently don't need to drop to intrinsic encoding if current is bogus
            else
                super.drawEstring(g, estr, x, y, layout, mode, strokecolor);
            return;
        }
        //assert newwidths_==null;


        int[] glyphs = estr2glyphs(estr, layout);
        // positions with GPOS and BASE => line breaks => JSTF => rasterize

        // GPOS, kern, BASE: kerning
//if (kernpair_!=null) System.out.println("maybe kern "+estr+" "+(LAYOUT_KERN&layout)+" "+Integer.toBinaryString(layout));

//        assert estr!=null;
        AffineTransform at = g.getTransform();
        g.translate(x, y);
        g.transform(m_);
        g.transform(at_);
        g.scale(size_, size_);

        int gid = 0;
        for (int i = 0, imax = glyphs.length, lastgid = 0; i < imax; i++, lastgid = gid) {
            gid = glyphs[i];
            Shape glyph = getGlyph(gid);    // could be from CFF data

            if ((LAYOUT_KERN & layout) != 0) {
                int kern = getKern(lastgid, gid);
                if (kern != 0) g.translate(kern, 0.0);
            }

            if (MODE_FILL == mode || MODE_FILL_STROKE == mode || MODE_FILL_ADD == mode || MODE_FILL_STROKE_ADD == mode)
                g.fill(glyph);
            /*NO else*/
            if (MODE_STROKE == mode || MODE_FILL_STROKE == mode || MODE_STROKE_ADD == mode || MODE_FILL_STROKE_ADD == mode)
                g.draw(glyph);


            double w = widths_[Math.min(gid, widths_.length - 1)];
            g.translate(w, 0.0);
        }
        g.setTransform(at);

        if (SUBFORMAT_CFF == getSubformat()) cff_.releaseRA();
        else super.releaseRA();
        //if (fra && cff_.ra_!=null) { NFontManager.releaseRA(cff_.ra_); cff_.ra_=null; }
        //releaseRA();
    }
}
