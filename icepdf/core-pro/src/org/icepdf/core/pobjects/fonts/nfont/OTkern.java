package org.icepdf.core.pobjects.fonts.nfont;

import java.awt.font.OpenType;
import java.util.Arrays;


/**
 * TrueType and OpenType 'kern' table: pairwise glyph kerning.
 * For TrueType outlines; if CFF, must use GPOS instead.
 *
 * @version $Revision: 1.1 $ $Date: 2005/07/12 21:33:19 $
 */
/*package-private*/
class OTkern {
    private boolean fae_;
    // array of tables, >1 table in Skia
    private int[] coverage_;
    private short[][] val_;
    // format 0
    private long[][] pair_;    // give us unsigned int!
    // format 1
    TrueTypeStateTable[] state_;
    // format 2
    private int[] cols_;
    private int[] left1_, right1_;    // first char in class
    private short[][] left_, right_;    // classes
    // format 3


    public OTkern(NFontSfnt s/*ource*/) {
        if (s.getTable(OpenType.TAG_KERN).length < 4) {
            coverage_ = new int[0];
            return;
        }

        // Apple different
        int version = s.readUint16(), nTables = s.readUint16();
        fae_ = nTables == 0;
        if (fae_) {    // Apple extensions
            version = (version << 16) | nTables;
            nTables = s.readUint32();
        }

//System.out.println("'kern', nTables = "+nTables+(fae_? ", Apple extensions": "")+", v="+Integer.toHexString(version));
        coverage_ = new int[nTables];
        val_ = new short[nTables][];
        pair_ = new long[nTables][];
        cols_ = new int[nTables];
        state_ = new TrueTypeStateTable[nTables];
        left1_ = new int[nTables];
        right1_ = new int[nTables];
        left_ = new short[nTables][];
        right_ = new short[nTables][];

        for (int i = 0; i < nTables; i++) {
            int offset0 = s.getOffset();
            int length, coverage, tupleIndex;
            if (fae_) {
                length = s.readUint32() - 8;
                coverage = s.readUint16();
                tupleIndex = s.readUint16();
            } else {
                s.readUint16();
                length = s.readUint16() - 6;
                coverage = s.readUint16();
                tupleIndex = -1;
            }
            int format = coverage_[i] = coverage & 0xff;

//System.out.println("kern "+i+"/"+nTables+", format = "+format+", "+s.offset_+"..+"+length+" < "+s.data_.length);
            if (format == 0) {
                int nPairs = s.readUint16();
                s.skip(6);
//System.out.println("nPairs = "+nPairs);
                long[] pair = pair_[i] = new long[nPairs];
                short[] val = val_[i] = new short[nPairs];
                for (int j = 0; j < nPairs; j++) {
                    pair[j] = s.readUint32();
                    val[j] = (short) s.readInt16();
                }

            } else if (format == 1) {
                /*
    for (int j=0; j<100; j++) System.out.print(" "+s.readUint8());  System.out.println(); s.offset_ = offset0;
                state_[i] = new TrueTypeStateTable(s, offset0);
                s.offset_ = offset0 + TrueTypeStateTable.HEADER_LENGTH;
                int valueTable = offset0 + s.readUint16();
    System.out.println(offset0+" "+valueTable);
                // not accurately specified in Apple's TrueType manual?  Not part of OpenType
                */
            } else if (format == 2) {
                int rowWidth = s.readUint16(), rows = 0, cols = cols_[i] = rowWidth / 2;
                int leftOffsetTable = s.readUint16(), rightOffsetTable = s.readUint16(), array = s.readUint16();

                s.setOffset(offset0 + leftOffsetTable);
                left1_[i] = s.readUint16();
                int nGlyphs = s.readUint16();
                short[] left = left_[i] = new short[nGlyphs];
                for (int j = 0; j < nGlyphs; j++) {
                    int val = (s.readUint16() - array) / rowWidth;    // undo premultiply
                    left[j] = (short) val;
                    if (val > rows) rows = val;
                }

                s.setOffset(offset0 + rightOffsetTable);
                right1_[i] = s.readUint16();
                nGlyphs = s.readUint16();
                short[] right = right_[i] = new short[nGlyphs];
                for (int j = 0; j < nGlyphs; j++)
                    right[j] = (short) (s.readUint16() / 2);
//System.out.println("rowW="+rowWidth+", cols="+cols+", first "+left1_[i]+" / "+right1_[i]+", offsets "+leftOffsetTable+" / "+rightOffsetTable+" / "+array+", data_.length="+s.data_.length+", @ "+offset0+" "+left_[i].length+"/"+left.length);

                s.setOffset(offset0 + array);
                rows++;
                cols++;    // numbered 0..val inclusive
                short[] val = val_[i] = new short[rows * cols];
                for (int j = 0, jmax = val.length; j < jmax; j++)
                    val[j] = (short) s.readUint16();

            } else if (format == 3) {


            }

            s.setOffset(offset0 + length);
        }
    }


    /**
     * Returns kerning pair value between two glyphs (not characters), in FUnits.
     */
    public int getKern(int glyph1, int glyph2) {
        int kern = 0;

        for (int i = 0, imax = coverage_.length; i < imax; i++) {
            int ksub = Integer.MIN_VALUE;
            int coverage = coverage_[i];
            //int xstream = coverage & ...
            int format = coverage & 0xff;

            if (format == 0) {    // ordered list of kerning pairs
                int inx = Arrays.binarySearch(pair_[i], (glyph1 << 16) | glyph2);
                if (inx >= 0) ksub = val_[i][inx];

            } else if (format == 1) {    // state table for contextual kerning
                // not well defined and not defined in OpenType

            } else if (format == 2) {    // simple n x m array of kerning values
                int l0 = left1_[i], c1 = l0 <= glyph1 && glyph1 < l0 + left_[i].length ? left_[i][glyph1 - l0] : 0;
                int r0 = right1_[i], c2 = r0 <= glyph2 && glyph2 < r0 + right_[i].length ? right_[i][glyph2 - r0] : 0;
//System.out.print("   "+glyph1+"="+c1+"*"+cols_[i]+" "+glyph2+"="+c2+" in "+val_[i].length+" "+left_[i].length+"/"+right_[i].length);
                kern = val_[i][c1 * cols_[i] + c2];

            } else if (format == 3) {    // simple n x m array of kerning indices
                // not used in practice and not defined in OpenType
            }


            if (ksub == Integer.MIN_VALUE) {
            }    // not set
            else if (fae_) {    // different bits definitions!
                // cross-stream
//if (format==2 && ksub!=0) System.out.println("kern "+glyph1+"||"+glyph2+" = "+ksub);
                kern += ksub;
            } else {
                if ((coverage & 0x10) != 0)
                    kern = ksub;    // override
                else if ((coverage & 0x2) != 0 && kern < ksub)
                    kern = ksub;    // minimum
                else
                    kern += ksub;
            }
        }

        return kern;
    }
}
