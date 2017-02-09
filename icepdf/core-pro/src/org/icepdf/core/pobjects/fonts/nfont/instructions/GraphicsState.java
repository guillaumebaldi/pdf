package org.icepdf.core.pobjects.fonts.nfont.instructions;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

/**
 * The graphics state variable establish the context in which the TrueType
 * instructions are executed. The TrueType specification contain many markers
 * and constructs that are stored in the graphics state.  Each Glyph is
 * assumed to have its own separate graphics ate.
 *
 * @since 4.5
 */
public class GraphicsState implements Cloneable {

    public static final Point2D.Float X_AXIS = new Point2D.Float(1, 0);
    public static final Point2D.Float Y_AXIS = new Point2D.Float(0, 1);

    /**
     * Controls whether the sign of control value table entries will be changed
     * to match the sign of the actual distance measurement with which it is
     * compared. Setting auto flip to TRUE makes it possible to control distances
     * measured with or against the projection vector with a single control value
     * table entry. When auto flip is set to FALSE, distances must be measured
     * with the projection vector.
     * <p/>
     * Set with FLIPOFF[], FLIPON[]
     * Affects	MIRP[]
     */
    private boolean autoFlip = true;
    /**
     * Limits the regularizing effects of control value table entries to cases
     * where the difference between the table value and the measurement taken
     * from the original outline is sufficiently small.
     * Set with	LCVTCI[]
     * Affects	MIAP[], MIRP[]
     */
    private int controlValueCutIn = 68; // 17/16 pixels (F26Dot6)
    /**
     * Establishes the base value used to calculate the range of point sizes to
     * which a given DELTAC[] or DELTAP[] instruction will apply. The formulas
     * given below are used to calculate the range of the various DELTA
     * instructions.
     * <p/>
     * DELTAC1	  DELTAP1	  (delta_base) through (delta_base + 15) <br />
     * DELTAC2	  DELTAP2	  (delta_base + 16) through (delta_base + 31) <br />
     * DELTAC3	  DELTAP3	  (delta_base + 32) through (delta_base + 47) <br />
     * <p/>
     * Set with	SDB[]
     * Affects	DELTAP1[], DELTAP2[], DELTAP3[],
     * DELTAC1[], DELTAC2[], DELTAC3[]
     */
    private int deltaBase = 9;
    /**
     * Determines the range of movement and smallest magnitude of movement (the step)
     * in a DELTAC[] or DELTAP[] instruction. Changing the value of the delta
     * shift makes it possible to trade off fine control of point movement for
     * range of movement. A low delta shift favors range of movement over fine control.
     * A high delta shift favors fine control over range of movement. The step has
     * the value 1/2 to the power delta shift. The range of movement is calculated
     * by taking the number of steps allowed (16) and multiplying it by the step.
     * <p/>
     * The legal range for delta shift is zero through six. Negative values are illegal.
     * <p/>
     * Set with	SDPVTL[]
     * Affects	IP[], GC[], MD[], MDRP[], MIRP[]
     */
    private int deltaShift = 3;
    /**
     * A second projection vector set to a line defined by the original outline
     * location of two points. The dual projection vector is used when it is
     * necessary to measure distances from the scaled outline before any instructions
     * were executed.
     * <p/>
     * Set with	SDPVTL[]
     * Affects	IP[], GC[], MD[], MDRP[], MIRP[]
     */
    private Point2D.Float dualProjectionVector = X_AXIS;
    /**
     * A unit vector that establishes an axis along which points can move.
     * <p/>
     * Set with	SFVTCA[], SFVTL[], SFTPV[], SVTCA[], SFVFS[]
     */
    private Point2D.Float freedomVector = X_AXIS;
    /**
     * Makes it possible to turn off instructions under some circumstances.
     * When set to TRUE, no instructions will be executed.
     * <p/>
     * Set with		INSTCTRL[]
     * Affects all	instructions
     */
    private boolean instructControl = false;
    /**
     * Makes it possible to repeat certain instructions a designated number of
     * times. The default value of one assures that unless the value of loop is
     * altered, these instructions will execute one time.
     * <p/>
     * Set with	SLOOP[]
     * Affects	ALIGNRP[], FLIPPT[], IP[], SHP[], SHPIX[]
     */
    private int loop = 1;
    /**
     * Establishes the smallest possible value to which a distance will be rounded.
     * <p/>
     * Affects	MDRP[]
     * MIRP[]
     */
    private int minimumDistance = 1; // Default	1 pixel (F26Dot6)
    /**
     * A unit vector whose direction establishes an axis along which distances
     * are measured.
     * <p/>
     * Set with	SPVTCA[], SPVTL[], SVTCA[], SPVFS[]
     */
    private Point2D.Float projectionVector = X_AXIS;
    /**
     * Determines the manner in which values are rounded. Can be set to a number
     * of predefined states or to a customized state with the SROUND or S45ROUND
     * instructions.
     * <p/>
     * Set with	RDTG[], ROFF[], RTDG[], RTG[], RTHG[], RUTG[], SROUND[], S45ROUND[]
     */
    private int roundState = 72;  // 1 (grid)
    private double gridPeriod = 1.0D;


    /**
     * The first of three reference points. References a point number that
     * together with a zone designation specifies a point in either the glyph
     * zone or the twilight zone.
     * <p/>
     * Set with	SRP0[], MDRP[], MIAP[], MSIRP[]
     * Affects	ALIGNRP[], MDRP[], MIRP[], MSIRP[]
     */
    public int rp0 = 0;

    /**
     * The second of three reference points. References a point number that
     * together with a zone designation specifies a point in either the glyph
     * zone or the twilight zone.
     * <p/>
     * Set with	SRP1[], MDAP[], MDRP[], MIRP[]
     * Affects	IP[], SHC[], SHP[], SHZ[]
     */
    public int rp1 = 0;

    /**
     * The third of three reference points. References a point number that
     * together with a zone designation specifies a point in to either the glyph
     * zone or the twilight zone.
     * <p/>
     * Set with	SRP2[], MDRP[], MIRP[]
     * Affects	IP[], SHC[], SHP[], SHZ[]
     */
    public int rp2 = 0;

    /**
     * Determines whether the interpreter will activate dropout control for the
     * current glyph. Use of the dropout control mode can depend upon the currently
     * prevailing combination of the following three conditions:
     * <ol>
     * <li>Is the glyph rotated?</li>
     * <li>Is the glyph stretched?</li>
     * <li>Is the current pixel per em setting less than a specified threshold?</li>
     * </ol>
     * It is also possible to block dropout control if one of the above conditions is false.
     */
    private boolean scanControl;  // default false
    /**
     * The distance difference below which the interpreter will replace a CVT
     * distance or an actual distance in favor of the single width value.
     * <p/>
     * Set with	SSWCI[]
     * Affects	MDRP[], MIRP[]
     */
    private int singleWidthCutIn = 0;  // 0 pixels (F26Dot6)
    /**
     * The value used in place of the control value table distance or the actual
     * distance value when the difference between that distance and the single
     * width value is less than the single width cut-in.
     * <p/>
     * Set with	SSW[]
     * Affects	MDRP[], MIRP[]
     */
    private int singleWidthValue = 0;  // 0 pixels (F26Dot6)

    /**
     * The first of three zone pointers. Can be set to reference either the
     * glyph zone (Z0) or the twilight zone (Z1).
     * <p/>
     * Set with	SZP0[], SZPS[]
     */
    public int zp0 = 1;

    /**
     * The second of three zone pointers. Can be set to reference either the
     * twilight zone (Z0) or the glyph zone (Z1).
     */
    public int zp1 = 1;

    /**
     * The third of three zone pointers. Can be set to reference either the
     * twilight zone (Z0) or the glyph zone (Z1).
     */
    public int zp2 = 1;

    /**
     * CVTable
     */
    public Cvt mCvtTable;


    /**
     * Functions associated with this font program.
     */
    private Map functions = new HashMap();
    private Map instructions = new HashMap();


    public boolean isAutoFlip() {
        return autoFlip;
    }

    public void setAutoFlip(boolean autoFlip) {
        this.autoFlip = autoFlip;
    }

    public int getControlValueCutIn() {
        return controlValueCutIn;
    }

    public void setControlValueCutIn(int controlValueCutIn) {
        this.controlValueCutIn = controlValueCutIn;
    }

    public int getDeltaBase() {
        return deltaBase;
    }

    public void setDeltaBase(int deltaBase) {
        this.deltaBase = deltaBase;
    }

    public int getDeltaShift() {
        return deltaShift;
    }

    public void setDeltaShift(int deltaShift) {
        this.deltaShift = deltaShift;
    }

    public Point2D.Float getDualProjectionVector() {
        return dualProjectionVector;
    }

    public void setDualProjectionVector(Point2D.Float dualProjectionVector) {
        this.dualProjectionVector = dualProjectionVector;
    }

    public Point2D.Float getFreedomVector() {
        return freedomVector;
    }

    public void setFreedomVector(Point2D.Float freedomVector) {
        this.freedomVector = freedomVector;
    }

    public boolean isInstructControl() {
        return instructControl;
    }

    public void setInstructControl(boolean instructControl) {
        this.instructControl = instructControl;
    }

    public int getLoop() {
        return loop;
    }

    public void setLoop(int loop) {
        this.loop = loop;
    }

    public int getMinimumDistance() {
        return minimumDistance;
    }

    public void setMinimumDistance(int minimumDistance) {
        this.minimumDistance = minimumDistance;
    }

    public Point2D.Float getProjectionVector() {
        return projectionVector;
    }

    public void setProjectionVector(Point2D.Float projectionVector) {
        this.projectionVector = projectionVector;
    }

    public int getRoundState() {
        return roundState;
    }

    public void setRoundState(int roundState) {
        this.roundState = roundState;
    }

    public double getGridPeriod() {
        return gridPeriod;
    }

    public void setGridPeriod(double gridPeriod) {
        this.gridPeriod = gridPeriod;
    }

    public int getRp0() {
        return rp0;
    }

    public void setRp0(int rp0) {
        this.rp0 = rp0;
    }

    public int getRp1() {
        return rp1;
    }

    public void setRp1(int rp1) {
        this.rp1 = rp1;
    }

    public int getRp2() {
        return rp2;
    }

    public void setRp2(int rp2) {
        this.rp2 = rp2;
    }

    public boolean isScanControl() {
        return scanControl;
    }

    public void setScanControl(boolean scanControl) {
        this.scanControl = scanControl;
    }

    public int getSingleWidthCutIn() {
        return singleWidthCutIn;
    }

    public void setSingleWidthCutIn(int singleWidthCutIn) {
        this.singleWidthCutIn = singleWidthCutIn;
    }

    public int getSingleWidthValue() {
        return singleWidthValue;
    }

    public void setSingleWidthValue(int singleWidthValue) {
        this.singleWidthValue = singleWidthValue;
    }

    public int getZp0() {
        return zp0;
    }

    public void setZp0(int zp0) {
        this.zp0 = zp0;
    }

    public int getZp1() {
        return zp1;
    }

    public void setZp1(int zp1) {
        this.zp1 = zp1;
    }

    public int getZp2() {
        return zp2;
    }

    public void setZp2(int zp2) {
        this.zp2 = zp2;
    }

    public int[] getFunction(int functionNumber) {
        return (int[]) functions.get(functionNumber);
    }

    public void addFunction(int functionNumber, int[] instructions) {
        functions.put(functionNumber, instructions);
    }

    public void addInstruction(int instructionNumber, byte[] instructions) {
        this.instructions.put(instructionNumber, instructions);
    }

    public Cvt getCvtTable() {
        return mCvtTable;
    }

    public void setCvtTable(Cvt cvtTable) {
        this.mCvtTable = cvtTable;
    }

    public double round(double d)
    {
        if(roundState == -1)
            return d;
        boolean flag = d > 0.0D;
        int i = roundState >> 6 & 3;
        double d1;
        if(i == 0)
            d1 = gridPeriod / 2D;
        else
        if(i == 1)
            d1 = gridPeriod;
        else
            d1 = gridPeriod * 2D;
        i = roundState >> 4 & 3;
        double d2;
        if(i == 0)
            d2 = 0.0D;
        else
        if(i == 1)
            d2 = d1 / 4D;
        else
        if(i == 2)
            d2 = d1 / 2D;
        else
            d2 = (3D * d1) / 4D;
        i = roundState & 0xf;
        if(i == 0)
        {
            double d3;
            for(d3 = d2; d3 < d; d3 += d1);
            return d3;
        }
        double d4 = ((double)(i - 4) * d1) / 8D;
        d -= d2;
        double d5 = 0.0D;
        if(d > 0.0D)
            for(d += d4; d5 + d1 <= d; d5 += d1);
        else
            for(d -= d4; d5 - d1 >= d; d5 -= d1);
        d = d5;
        d += d2;
        if(flag && d < 0.0D)
            d = d2 % d1;
        if(!flag && d > 0.0D)
            d = (d2 - 10D * d1) % d1;
        return d;
    }

    /**
     * Reset the resettable fields back to
     */
    public void resetForGlyph() {

        zp0 = 1;
        zp1 = 1;
        zp2 = 1;
        projectionVector = X_AXIS;
        dualProjectionVector = X_AXIS;
        freedomVector = X_AXIS;
        roundState = 72;
        loop = 1;
        controlValueCutIn = 68;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
