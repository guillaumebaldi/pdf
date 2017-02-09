package org.icepdf.core.pobjects.fonts.nfont.instructions;

import org.icepdf.core.pobjects.fonts.nfont.CallContext;
import org.icepdf.core.pobjects.fonts.nfont.NFontTrueType;
import org.icepdf.core.pobjects.fonts.nfont.TrueTypeGlyphData;
import org.icepdf.core.pobjects.fonts.nfont.lang.Stack;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The instructions interpreter is responsible for iterating over and executing
 * the TrueType font instructions.  Instructions are executed on the
 * given TrueTypeGlyphData object and the modified TrueTypeGlyphData is returned
 * for rendering by the NFontTrueType class.
 *
 * @since 4.5
 */
public class Interpreter {

    private static final Logger logger =
            Logger.getLogger(Interpreter.class.toString());

    private GraphicsState mGraphicsState;
    private GraphicsState mPostPrepState;
    private Stack mStack;
    public static int STORAGE[];
    public static int ppem = 1563;
    public static double ptSize = 1171.875;
    private static NFontTrueType mInvoker;
    public static boolean sUseCleanGS;
    private static Cvt sCvt;

    public static int pcCtr;

    public Interpreter(Maxp maxpTable, NFontTrueType invoker) {
        mGraphicsState = new GraphicsState();
        mStack = new Stack();
        STORAGE = new int[maxpTable.maxStorage_];
        mInvoker = invoker;
    }

    public void processGlyph(TrueTypeGlyphData glyphDef) {

        // iterate over instruction set.
        int[] instr = glyphDef.getInstructions();

        // Check instrctrl state. A true value turns off execution of instructions
        if (mGraphicsState.isInstructControl()) {
            return;
        }
        if (sUseCleanGS) {
            mGraphicsState = new GraphicsState();
        } else {
            try {
                mGraphicsState = (GraphicsState) mPostPrepState.clone();
                mGraphicsState.resetForGlyph();

            } catch (CloneNotSupportedException cnse) {
                logger.warning("GraphicsState clone not supported");
                mGraphicsState = new GraphicsState();
            }
        }
        mGraphicsState.setCvtTable(sCvt);
        execute( glyphDef, instr, mStack, mGraphicsState );
        return;
    }

    public static void execute(TrueTypeGlyphData glyphDef, int[] instr,
                               Stack stack, GraphicsState graphicsState) {
        try {
            int opCode;
            Instruction instruction;
            int insOffr = 0;
            for (int offset = 0, max = instr.length; offset < max; offset++) {
                opCode = instr[offset];
                instruction = Instructions.getInstruction(opCode);
                if (instruction != null) {
//                    CallContext.logContinuation(Integer.toString( pcCtr++ ) + "  (" + Integer.toString(++insOffr) + ") ");
                    offset = instruction.execute(glyphDef, instr, offset, stack, graphicsState);
//                    CallContext.logContinuationEndln(stack.getBriefDebug());

                }
            }
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Error executing instruction: ", e);
            mInvoker.setHinting(false); // No more hinting
        }
    }

    public GraphicsState getGraphicsState() {
        return mGraphicsState;
    }

    public void setCvt(Cvt cvtTable) {
        sCvt = cvtTable;
    }

    public static void setsUseDefaultGS (boolean useDefaultGS) {
        sUseCleanGS = useDefaultGS;
    }

    /**
     * Define the GraphicsState object used during the run of 'fpgm' and 'prep'
     * tables. This can, depending on runtime settings, be used as the source
     * GraphicsState for instruction execution for a Glyph
     * @param postPrepState Reference to the GraphicsState object used during fpgm and prep
     */
    public void saveState(GraphicsState postPrepState) {
        mPostPrepState = postPrepState;
    }
}
