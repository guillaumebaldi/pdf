package org.icepdf.core.pobjects.fonts.nfont;

import org.icepdf.core.pobjects.fonts.nfont.instructions.Maxp;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The TrueTypeGlyphData information derived from parsing the 'glypf' table. The
 * data allows for the hinting of the glyph as well as storing needed hinting
 * data instructions such as zones and touched coordinates.
 * <p/>
 * For composite glyphs, the implementation of points is a 2 dimensional array
 * the primary index being the subglyph index, the array portion of the structure
 * being (for example) the x coordinates associated with that glyph. If
 * the glyph is a simple glyph, only the first array row is defined.
 * <p/>
 * This is necessary to implement some of the instructions that may operate in
 * a compound glyph.
 * <p/>
 * Parse the simple glyphs definition defined in the 'glypf' table.  The
 * parsed data is returned in a n element array where each element is
 * defined as follows:
 * <ul>
 * <li>[0] - array xCoordinates
 * <li>[1] - array yCoordinates
 * <li>[2] - array of last points of each contour; entries are glyph point indices
 * <li>[3] - array of flags describing the path made up of points.
 * <li>[4] - nOn n points.
 * <li>[5] - array of instructions for this glyph, aka hinting.
 * </ul>
 * {xCoordinates, yCoordinates, endPtsOfContours, flags, nOn, instructions};
 */
public class TrueTypeGlyphData {

    private static final Logger logger =
            Logger.getLogger(TrueTypeGlyphData.class.toString());

    // coordinates by [zone][glyph][pointId]
    public int[][][] xZ;
    public int[][][] yZ;

    // touched flags by [zone][glyph][pointId]
    public boolean[][][] touchedXZ;
    public boolean[][][] touchedYZ;

    private byte[][] mFlags;  // Array of byte flags [glyph][point]
    private int[][] mEndPointArrays; // array of last points in contour per [glyph][pointId]
    private int[] mNon;       // Number of points on the curve, per glyph
    private int[] mGIDs;     // array of the original GIDs associated with each subglyph
    private ArrayList<int[]> mXCoordAccumulator;
    private ArrayList<int[]> mYCoordAccumulator;
    private ArrayList<int[]> mEndPointAccumulator;
    private ArrayList<byte[]> mFlagsAccumulator;
    private ArrayList<Integer> mGIDAccumulator;
    private ArrayList<AffineTransform> mTransformAccumulator; // one AffineTransform per subglyph

    public int xTranslation[]; // compound glyph translation
    public int yTranslation[];
    public int fetcherId;

    private ArrayList mNOnAccumulator;
    private int[] mInstructions;
    /**
     * This value represents the number of contours that are actually processed by the
     * parsing code. Used to consolidate arrays
     */
    private int mProcessedGlyphCount;
    /**
     * The number of contours parsed from the font file. This may differ from above due
     * to using -1 as a flag indicating composite glyphs when only 1 physical contour exists
     */
    private int mParsedContourCount;

    public int pointOne;
    public int pointTwo;
    private Maxp mMaxpTable;

    /**
     * Construct an empty object.
     */
    public TrueTypeGlyphData(Maxp maxTable) {
        mXCoordAccumulator = new ArrayList(25);
        mYCoordAccumulator = new ArrayList(25);
        mEndPointAccumulator = new ArrayList(25);
        mFlagsAccumulator = new ArrayList(25);
        mNOnAccumulator = new ArrayList(10);
        mGIDAccumulator = new ArrayList(10);
        mTransformAccumulator = new ArrayList<AffineTransform>(5);
        xTranslation = new int[50];
        yTranslation = new int[50];
        mMaxpTable = maxTable;

        // Setup the twilight points with the max size in place.
        xZ = new int[4][][];
        yZ = new int[4][][];
        xZ[0] = new int[1][];
        xZ[0][0] = new int[mMaxpTable.maxTwilightPoints_];
        yZ[0] = new int[1][];
        yZ[0][0] = new int[mMaxpTable.maxTwilightPoints_];

        xZ[1] = new int[1][];
        xZ[1][0] = new int[mMaxpTable.maxPoints_];
        yZ[1] = new int[1][];
        yZ[1][0] = new int[mMaxpTable.maxPoints_];

        xZ[2] = new int[1][];
        xZ[2][0] = new int[mMaxpTable.maxPoints_];
        yZ[2] = new int[1][];
        yZ[2][0] = new int[mMaxpTable.maxPoints_];

        xZ[3] = new int[1][];
        xZ[3][0] = new int[mMaxpTable.maxPoints_];
        yZ[3] = new int[1][];
        yZ[3][0] = new int[mMaxpTable.maxPoints_];


        touchedXZ = new boolean[4][][];
        touchedYZ = new boolean[4][][];

        touchedXZ[0] = new boolean[1][];
        touchedYZ[0] = new boolean[1][];
        touchedXZ[0][0] = new boolean[mMaxpTable.maxTwilightPoints_];
        touchedYZ[0][0] = new boolean[mMaxpTable.maxTwilightPoints_];

        // temporarily set this up until the glyphs are defined.
//        xZ[1] = new int[1][mMaxpTable.maxPoints_];
//        yZ[1] = new int[1][mMaxpTable.maxPoints_];
    }


    public void appendGlyphDefinition(int[] xZ1, int[] yZ1, int[] endPtsOfContours,
                                      int nOn, int[] instructions, byte[] flags, int gid) {

        mXCoordAccumulator.add(xZ1);
        mYCoordAccumulator.add(yZ1);
        mEndPointAccumulator.add(endPtsOfContours);
        mFlagsAccumulator.add(flags);
        mNOnAccumulator.add(new Integer(nOn));
        mGIDAccumulator.add(new Integer(gid));

        mProcessedGlyphCount++;

        if (logger.isLoggable(Level.FINEST)) {
            if (mInstructions != null && mInstructions.length > 0) {
                logger.finest("Multiple sources of instructions!!!); ");
            }
        }
        mInstructions = instructions;

    }

    /**
     * At the end of the composite glyph definitions may be a set of
     * instructions that can operate on all the previously defined glyphs
     * and their points. It's not a problem if this overwrites a subglyphs
     * instructions, but it's something I'd like to know.
     */
    public void appendCompositeInstructions(int[] instructions) {

        if (logger.isLoggable(Level.FINEST)) {
            if (mInstructions != null && mInstructions.length > 0) {
                logger.finest("Previous Simple Glyph instructions being overwritten!!!! ");
            }
        }
        mInstructions = instructions;
    }

    /**
     * From a CompoundGlyph, we will append an AffineTransform for each subglyph
     *
     * @param compAt
     */
    public void appendCompositeTransform(AffineTransform compAt) {
        mTransformAccumulator.add(compAt);
    }

    /**
     * Call this after any parent compound glyph is done parsing subglyphs to convert
     * the ArrayList structures to []. This method also appends two
     * extra integers onto the x,y coords according to the spec.
     * x[end-1],y[end-1] = origin.
     * x[end] = left side bearing
     */
    public void closeGlyphDefinition(int leftSideBearing, double scale) {

        this.xZ[1] = new int[mProcessedGlyphCount][];
        this.yZ[1] = new int[mProcessedGlyphCount][];

        this.touchedXZ[1] = new boolean[mProcessedGlyphCount][];
        this.touchedYZ[1] = new boolean[mProcessedGlyphCount][];


        this.mFlags = new byte[mProcessedGlyphCount][];
        this.mEndPointArrays = new int[mProcessedGlyphCount][];
        this.mNon = new int[mProcessedGlyphCount];

        // Shadow zone is already sized
        this.touchedXZ[0][0] = new boolean[this.xZ[0][0].length];
        this.touchedYZ[0][0] = new boolean[this.yZ[0][0].length];

        xZ[1] = new int[mProcessedGlyphCount][];
        yZ[1] = new int[mProcessedGlyphCount][];
        xZ[3] = new int[mProcessedGlyphCount][];
        yZ[3] = new int[mProcessedGlyphCount][];

        mGIDs = new int[mGIDAccumulator.size()];

        for (int glyphDx = 0; glyphDx < mProcessedGlyphCount; glyphDx++) {
            int[] xVals = mXCoordAccumulator.get(glyphDx);
            if (glyphDx == mProcessedGlyphCount - 1) {
                // last glyph make room for extra values
                int[] xV = new int[xVals.length + 2];
                System.arraycopy(xVals, 0, xV, 0, xVals.length);
                xV[xV.length - 2] = 0;
                xV[xV.length - 1] = (int) (scale * (double) leftSideBearing + 0.5D);
                this.xZ[1][glyphDx] = xV;
                this.xZ[3][glyphDx] = xV.clone();
                this.touchedXZ[1][glyphDx] = new boolean[this.xZ[1][glyphDx].length - 2];
            } else {
                this.xZ[1][glyphDx] = xVals;
                this.xZ[3][glyphDx] = xVals.clone();
                this.touchedXZ[1][glyphDx] = new boolean[this.xZ[1][glyphDx].length];
            }

            int[] yVals = mYCoordAccumulator.get(glyphDx);
            if (glyphDx == mProcessedGlyphCount - 1) {
                // last glyph make room for extra values
                int[] yV = new int[yVals.length + 2];
                System.arraycopy(yVals, 0, yV, 0, yVals.length);
                yV[yV.length - 2] = 0;
                yV[yV.length - 1] = 0;
                this.yZ[1][glyphDx] = yV;
                this.yZ[3][glyphDx] = yV.clone();
                this.touchedYZ[1][glyphDx] = new boolean[this.yZ[1][glyphDx].length - 2];
            } else {
                this.yZ[1][glyphDx] = yVals;
                this.yZ[3][glyphDx] = yVals.clone();
                this.touchedYZ[1][glyphDx] = new boolean[this.yZ[1][glyphDx].length];
            }


            this.mFlags[glyphDx] = (byte[]) mFlagsAccumulator.get(glyphDx);
            this.mNon[glyphDx] = ((Integer) mNOnAccumulator.get(glyphDx)).intValue();
            this.mEndPointArrays[glyphDx] = (int[]) mEndPointAccumulator.get(glyphDx);
            this.mGIDs[glyphDx] = ((Integer) mGIDAccumulator.get(glyphDx)).intValue();
        }
    }

    public int getGlyphCount() {
        return mProcessedGlyphCount;
    }

    public int getNCon() {
        return mParsedContourCount;
    }

    public void setNCon(int nCon) {
        if (mParsedContourCount == 0) {
            mParsedContourCount = nCon;
        }
    }

    public int[] getGlyphEndPoints(int glyphIdx) {
        return mEndPointArrays[glyphIdx];
    }

    public AffineTransform getGlyphTransform(int idx) {
        return mTransformAccumulator.get(idx);
    }

    public byte[] getFlagsByGlyphId(int glyphId) {
        return mFlags[glyphId];
    }

    /**
     * Fetch a flag byte for a point, numbered from the start
     *
     * @param pointId
     * @return
     */
    public byte getFlagsPtr(int pointId) {
        return extendedFlagGet(pointId);
    }

    public void setFlagsPtr(int pointIdx, byte flag) {
        extendedFlagSet(pointIdx, flag);
    }


    /**
     * Fetch the xCoordinate value for the point from a zone. The point number
     * may fit within the entire point range of points defined by all the child
     * glyphs. Hinting instructions define point offsets from 0 - max number of points
     * defined by all the glyphs in a composite glyph so this is required.
     *
     * @param zone     index into the zone array, normally given by one of the zp vars, but
     *                 may be hardcoded.
     * @param pointIdx Point index
     * @return X Coord value
     */
    public int getXPtr(int zone, int pointIdx) {
        return extendedIntegerLookup(xZ[zone], pointIdx, "getXptr");
    }

    public void setXPtr(int zone, int pointIdx, int val) {
        extendedIntegerSet(xZ[zone], pointIdx, val, "getXptr");
    }

    public void setXPtr(int zone, int pointIdx, float val) {
        extendedIntegerSet(xZ[zone], pointIdx, Math.round(val), "getXptr");
    }

    public int[] getXByGlyphId(int zone, int glyphId) {
        return xZ[zone][glyphId];
    }

    public void setXByGlyphId(int zone, int glyphId, int[] val) {
        xZ[zone][glyphId] = (int[]) val.clone();
    }

    /**
     * Fetch the yCoordinate value for the point from a zone. The point number
     * may fit within the entire point range of points defined by all the child
     * glyphs. Hinting instructions define point offsets from 0 - max number of points
     * defined by all the glyphs in a composite glyph so this is required.
     *
     * @param zone     index into the zone array, normally given by one of the zp vars, but
     *                 may be hardcoded.
     * @param pointIdx Point index
     * @return Y Coord value
     */
    public int getYPtr(int zone, int pointIdx) {
        return extendedIntegerLookup(yZ[zone], pointIdx, "getYPtr");
    }

    public void setYPtr(int zone, int pointIdx, int val) {
        extendedIntegerSet(yZ[zone], pointIdx, val, "setYptr");
    }

    public void setYPtr(int zone, int pointIdx, float val) {
        extendedIntegerSet(yZ[zone], pointIdx, Math.round(val), "setYptr");
    }

    public int[] getYByGlyphId(int zone, int glyphId) {
        return yZ[zone][glyphId];
    }

    public void setYByGlyphId(int zone, int glyphId, int[] val) {
        yZ[zone][glyphId] = (int[]) val.clone();
    }

    // x touched methods
    public boolean getTouchedXPtr(int zone, int pointIdx) {
        return extendedBooleanLookup(touchedXZ[zone], pointIdx, "getTouchedXPtr");
    }

    public void setTouchedXPtr(int zone, int pointIdx, boolean touched) {
        extendedBooleanSet(touchedXZ[zone], pointIdx, touched, "setTouchedXZPtr");
    }

    public boolean[] getXTouchedByGlyphId(int zone, int glyphId) {
        return touchedXZ[zone][glyphId];
    }

    public void setXTouchedByGlyphId(int zone, int glyphId, boolean[] data) {
        touchedXZ[zone][glyphId] = data;
    }

    // yZtouched series
    public boolean getTouchedYPtr(int zone, int pointIdx) {
        return extendedBooleanLookup(touchedYZ[zone], pointIdx, "getTouchedYPtr");
    }

    public void setTouchedYPtr(int zone, int pointIdx, boolean touched) {
        extendedBooleanSet(touchedYZ[zone], pointIdx, touched, "setTouchedYPtr");
    }

    public boolean[] getYTouchedByGlyphId(int zone, int glyphId) {
        return touchedYZ[zone][glyphId];
    }

    public void setYTouchedByGlyphId(int zone, int glyphId, boolean[] data) {
        touchedYZ[zone][glyphId] = data;
    }

    public int getGIDByIndex(int glyphIndex) {
        return mGIDs[glyphIndex];
    }


    /**
     * Extended method support. Accept a point number in the range 0-max and
     * find the contour that contains it and fetch it
     *
     * @param table
     * @param pointIdx
     * @param loc
     * @return
     */
    private int extendedIntegerLookup(int[][] table, int pointIdx, String loc) {
        int pdx = pointIdx;
        int max = mProcessedGlyphCount;
        // can happen in CVP execution that program is operating in shadow point zone
        if (max == 0 && table.length > 0) {
            max = table.length;
        }
        for (int glyphDx = 0; glyphDx < max; glyphDx++) {
            if (pdx < table[glyphDx].length) {
                return table[glyphDx][pdx];
            }
            pdx -= table[glyphDx].length;
        }
        throw new IllegalArgumentException("Point index out of range in " + loc + ": " + pointIdx);
    }

    private void extendedIntegerSet(int[][] table, int pointIdx, int value, String loc) {
        int pdx = pointIdx;
        int max = mProcessedGlyphCount;
        // can happen in CVP execution that program is operating in shadow point zone
        if (max == 0 && table.length > 0) {
            max = table.length;
        }
        for (int glyphDx = 0; glyphDx < max; glyphDx++) {
            if (pdx < table[glyphDx].length) {
                table[glyphDx][pdx] = value;
                return;
            }
            pdx -= table[glyphDx].length;
        }
        throw new IllegalArgumentException("Point index out of range in " + loc + ": " + pointIdx);
    }

    private boolean extendedBooleanLookup(boolean[][] table, int pointIdx, String loc) {
        int pdx = pointIdx;
        int max = mProcessedGlyphCount;
        // can happen in CVP execution that program is operating in shadow point zone
        if (max == 0 && table.length > 0) {
            max = table.length;
        }
        for (int glyphDx = 0; glyphDx < max; glyphDx++) {
            if (pdx < table[glyphDx].length) {
                return table[glyphDx][pdx];
            }
            pdx -= table[glyphDx].length;
        }
        // todo: Need to fix the algorithm for the two extra points that get appended
        // to the x,y coord arrays. Do they support the touched flags?
        return false;
    }

    private void extendedBooleanSet(boolean[][] table, int pointIdx, boolean value, String loc) {
        int pdx = pointIdx;
        int max = mProcessedGlyphCount;
        // can happen in CVP execution that program is operating in shadow point zone
        if (max == 0 && table.length > 0) {
            max = table.length;
        }
        for (int glyphDx = 0; glyphDx < max; glyphDx++) {
            if (pdx < table[glyphDx].length) {
                table[glyphDx][pdx] = value;
                return;
            }
            pdx -= table[glyphDx].length;
        }
        // todo: Need to fix the algorithm for the two extra points that get appended
        // to the x,y coord arrays. Just ignore setting them for now
//        throw new IllegalArgumentException("Point index out of range in " + loc + ": " + pointIdx);
    }

    private byte extendedFlagGet(int pointIdx) {
        int pdx = pointIdx;
        for (int glyphDx = 0; glyphDx < mProcessedGlyphCount; glyphDx++) {
            if (pdx < mFlags[glyphDx].length) {
                return mFlags[glyphDx][pdx];
            }
            pdx -= mFlags[glyphDx].length;
        }
        throw new IllegalArgumentException("Point index out of range in getFlag: " + pointIdx);
    }

    private void extendedFlagSet(int pointIdx, byte flag) {
        int pdx = pointIdx;
        for (int glyphDx = 0; glyphDx < mProcessedGlyphCount; glyphDx++) {
            if (pdx < mFlags[glyphDx].length) {
                mFlags[glyphDx][pdx] = flag;
            }
            pdx -= mFlags[glyphDx].length;
        }
//        throw new IllegalArgumentException("Point index out of range in setFlag: " + pointIdx);
    }


    /**
     * Fetch the end points of the Contours, for a particular glyph. Glyphs may
     * have more than one contour, so this wont always be trivial.
     *
     * @param glyphDx Index of the glyph
     * @return Array of point ids representing end points of contours.
     */
    public int[] getEndPtsOfContours(int glyphDx) {
        return (int[]) mEndPointAccumulator.get(glyphDx);
    }

    public int getnOn(int glyphDx) {
        return mNon[glyphDx];
    }

    public int[] getInstructions() {
        return mInstructions;
    }

    public byte[] getFlags(int glyphDx) {
        return mFlags[glyphDx];
    }

    /**
     * Fetch a Glyphs entire 'end of contour' point number array. This should mkae
     * it easier for algorithms trying to find contour 'n' to do so.
     *
     * @return
     */
    public int[] getGlyphEndArray() {

        int[] accumulator = new int[10];
        int sdx = 0;  // index into returnVal;
        int markDx = 0; // starting offset of each glyphs slice
        for (int gdx = 0; gdx < mProcessedGlyphCount; gdx++) {

            int[] endPts = getEndPtsOfContours(gdx);
            int[] points = getXByGlyphId(1, gdx);

            if (sdx + endPts.length > accumulator.length) {
                int[] temp = new int[accumulator.length * 2];
                System.arraycopy(accumulator, 0, temp, 0, accumulator.length);
                accumulator = temp;
            }

            for (int pdx = 0; pdx < endPts.length; pdx++) {
                accumulator[sdx++] = endPts[pdx] + markDx;
            }
            markDx += points.length;
        }
        int[] returnVal = new int[sdx];
        System.arraycopy(accumulator, 0, returnVal, 0, sdx);
        return returnVal;
    }
}
