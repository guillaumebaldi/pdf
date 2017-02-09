package org.icepdf.core.pobjects.fonts.nfont.instructions;

import org.icepdf.core.pobjects.fonts.nfont.CallContext;
import org.icepdf.core.pobjects.fonts.nfont.TrueTypeGlyphData;
import org.icepdf.core.pobjects.fonts.nfont.lang.Stack;

import java.awt.geom.Point2D;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The instruction class contains an array of all TrueType font operations
 * indexed by the operands code range.
 * <p/>
 * todo complete implementations / fix bugs.  *
 *
 * @since 4.5
 */
public class Instructions {

    private static final Logger logger =
            Logger.getLogger(Instructions.class.toString());


    protected static Instruction getInstruction(int instOffset) {
        instOffset = instOffset & 0xff;
        return INSTRUCTIONS[instOffset];
    }

    protected static final Instruction[] INSTRUCTIONS = new Instruction[256];
    /**
     * Store the font instructions in an array indexed by the operands hex code,
     * keep the look up as fast as possible.
     */
    static {

        // AA[] Adjust Angle
        INSTRUCTIONS[InstructionNames.AA] =
                new HintInstruction(InstructionNames.AA, "AA") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        stack.pop();
                        CallContext.log("AA - popped stack");
                        return instOffset;
                    }
                };
        // ABS[] ABSolute value
        INSTRUCTIONS[InstructionNames.ABS] =
                new HintInstruction(InstructionNames.ABS, "ABS") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int top = stack.pop();
                        stack.push(top < 0 ? -top : top);
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("ABS - stack neutral");
                        }
                        return instOffset;
                    }
                };
        // ADD[] ADD
        INSTRUCTIONS[InstructionNames.ADD] =
                new HintInstruction(InstructionNames.ADD, "ADD") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("ADD - stack down 1");
                        }
                        int arg1 = stack.pop();
                        int arg2 = stack.pop();
                        stack.push(arg1 + arg2);
                        return instOffset;
                    }
                };
        // ALIGNPTS[] ALIGN Points
        INSTRUCTIONS[InstructionNames.ALIGNPTS] =
                new HintInstruction(InstructionNames.ALIGNPTS, "ALIGNPTS") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {

                        int pt = stack.pop();
                        int pt1 = stack.pop();

                        float xPt = glyphDef.getXPtr(gs.zp1, pt);
                        float yPt = glyphDef.getYPtr(gs.zp1, pt);

                        float xPt1 = glyphDef.getXPtr(gs.zp0, pt1);
                        float yPt1 = glyphDef.getYPtr(gs.zp0, pt1);

                        int mark1 = getProjectionOnVector(gs.getProjectionVector(), xPt, yPt);
                        int mark2 = getProjectionOnVector(gs.getProjectionVector(), xPt1, yPt1);

                        int ave = (mark1 + mark2) / 2;

                        Point2D.Float deltaPt = getFreedomDistanceForProjectionMove(ave - mark1, gs);

                        int xp2 = Math.round( xPt + deltaPt.x);
                        int yp2 = Math.round( yPt + deltaPt.y);
                        int xpa = Math.round( xPt1 - deltaPt.x);
                        int ypa = Math.round( yPt1 - deltaPt.y);

                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("ALIGNPTS - Aligned: " + pt + " and " + pt1 + " to [" +
                                    xp2 + "," + yp2 + "] and [" + xpa + "," + ypa + "]");
                        }

                        if (gs.getFreedomVector().x != 0) {
                            glyphDef.setTouchedXPtr(gs.zp1, pt, true);
                            glyphDef.setTouchedXPtr(gs.zp0, pt1, true);
                            glyphDef.setXPtr(gs.zp1, pt, xp2);
                            glyphDef.setXPtr(gs.zp0, pt1, xpa);

                        }
                        if (gs.getFreedomVector().y != 0) {
                            glyphDef.setTouchedYPtr(gs.zp1, pt, true);
                            glyphDef.setTouchedYPtr(gs.zp1, pt1, true);
                            glyphDef.setXPtr(gs.zp1, pt, yp2);
                            glyphDef.setYPtr(gs.zp0, pt1, ypa);
                        }
                        return instOffset;
                    }
                };
        // ALIGNRP[] ALIGN to Reference Point
        INSTRUCTIONS[InstructionNames.ALIGNRP] =
                new HintInstruction(InstructionNames.ALIGNRP, "ALIGNRP") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int loop = graphicsState.getLoop();
                        for (int idx = 0; idx < loop; idx++) {

                            int pt = stack.pop();
                            int xRp = glyphDef.getXPtr(graphicsState.zp0, graphicsState.rp0);
                            int yRp = glyphDef.getYPtr(graphicsState.zp0, graphicsState.rp0);

                            int l38 = getProjectionOnVector(graphicsState.getProjectionVector(),
                                    xRp, yRp);
                            float xPt = glyphDef.getXPtr(graphicsState.zp1, pt);
                            float yPt = glyphDef.getYPtr(graphicsState.zp1, pt);

                            int i46 = l38 - getProjectionOnVector(graphicsState.getProjectionVector(),
                                    xPt, yPt);

                            Point2D.Float deltaPt = getFreedomDistanceForProjectionMove(i46, graphicsState);
                            xPt += deltaPt.x;
                            yPt += deltaPt.y;
                            if (logger.isLoggable(Level.FINEST)) {
                                CallContext.log("ALIGNRP - Aligning point: " + pt + ", to: [x,y] = [" + Math.round(xPt) +
                                          "," + Math.round(yPt) +"]");
                            }
                            glyphDef.setXPtr(graphicsState.zp1, pt, xPt);
                            glyphDef.setYPtr(graphicsState.zp1, pt, yPt);

                            if (graphicsState.getFreedomVector().x != 0) {
                                glyphDef.setTouchedXPtr(graphicsState.zp1, pt, true);
                            }
                            if (graphicsState.getFreedomVector().y != 0) {
                                glyphDef.setTouchedYPtr(graphicsState.zp1, pt, true);
                            }
                        }
                        graphicsState.setLoop(1);
                        return instOffset;
                    }
                };
        // AND[] logical AND
        INSTRUCTIONS[InstructionNames.AND] =
                new HintInstruction(InstructionNames.AND, "AND") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        boolean e2 = stack.pop() != 0; // e2: stack element
                        boolean e1 = stack.pop() != 0; // e1: stack element
                        if (e1 && e2) {
                            if (logger.isLoggable(Level.FINEST)) {
                                CallContext.log("AND - Pushing 1 (true)");
                            }
                            stack.push(1);
                        } else {
                            if (logger.isLoggable(Level.FINEST)) {
                                CallContext.log("AND - Pushing 0 (false)");
                            }
                            stack.push(0);
                        }
                        return instOffset;
                    }
                };
        // CALL[] CALL function
        INSTRUCTIONS[InstructionNames.CALL] =
                new HintInstruction(InstructionNames.CALL, "CALL") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int functionNumber = stack.pop();
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.logContinuationEndln("CALL function[" + functionNumber + "]");
                        }
                        int[] instructions = graphicsState.getFunction(functionNumber);
                        if (instructions != null) {
                            CallContext.down();
                            Interpreter.execute(glyphDef, instructions, stack, graphicsState);
                            if (logger.isLoggable(Level.FINEST)) {
                                CallContext.log("After CALL, stack depth: " + stack.offset);
                            }
                            CallContext.up();
                        }
                        return instOffset;
                    }
                };

        // CEILING[] CEILING
        INSTRUCTIONS[InstructionNames.CEILING] =
                new HintInstruction(InstructionNames.CEILING, "CEILING") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        stack.push(setF26Dot6((float) Math.ceil(getF26Dot6AsFloat(stack.pop()))));
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("CEILING - stack neutral");
                        }
                        return instOffset;
                    }
                };
        // CINDEX[] Copy the INDEXed element to the top of the stack
        INSTRUCTIONS[InstructionNames.CINDEX] =
                new HintInstruction(InstructionNames.CINDEX, "CINDEX") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int index = stack.pop();
                        int val = stack.elementAt(index);
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("CINDEX - Copy: " + val + " at pos: " + index + " to stack");
                        }
                        stack.push(stack.elementAt(index));
                        return instOffset;
                    }
                };
        // CLEAR[] CLEAR the stack
        INSTRUCTIONS[InstructionNames.CLEAR] =
                new HintInstruction(InstructionNames.CLEAR, "CLEAR") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        stack.removeAllElements();
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("CLEAR - cleared stack");
                        }
                        return instOffset;
                    }
                };
        // DEBUG[] DEBUG call
        INSTRUCTIONS[InstructionNames.DEBUG] =
                new HintInstruction(InstructionNames.DEBUG, "DEBUG") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        stack.pop();
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("DEBUG - stack popped");
                        }
                        return instOffset;
                    }
                };
        // DELTAC1[] DELTA exception C
        INSTRUCTIONS[InstructionNames.DELTAC1] =
                new HintInstruction(InstructionNames.DELTAC1, "DELTAC1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {
                        int n = stack.pop();
                        int idx = 0;
                        do {
                            if (idx >= n)
                                break;
                            int entry = stack.pop();
                            int exceptionVal = stack.pop();
                            int workingVal = gs.getDeltaBase() + (exceptionVal >> 4);

                            if ((double) workingVal == Interpreter.ppem) {
                                int l55 = (exceptionVal & 0xf) - 7;
                                if (l55 <= 0)
                                    l55--;

                                int l58 = storeDoubleAsF26Dot6((double) l55 *
                                        (1.0D / Math.pow(2D, gs.getDeltaShift())));
                                int i61 = (entry);
                                i61 += l58;
                                gs.getCvtTable().writePixels(entry, i61);
                            }
                            idx++;
                        } while (true);
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("DELTAC1 - stack popped:" + (1 + n * 2));
                        }
                        return instOffset;
                    }
                };
        // DELTAC2[] DELTA exception C2
        INSTRUCTIONS[InstructionNames.DELTAC2] =
                new HintInstruction(InstructionNames.DELTAC2, "DELTAC2") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {
                        int n = stack.pop();
                        int idx = 0;
                        do {
                            if (idx >= n)
                                break;
                            int cvtVal = stack.pop();
                            int exceptionVal = stack.pop();
                            int i52 = gs.getDeltaBase() + 16 + (exceptionVal >> 4);
                            if ((double) i52 == Interpreter.ppem) {
                                int i56 = (exceptionVal & 0xf) - 7;
                                if (i56 <= 0)
                                    i56--;
                                int i59 = storeDoubleAsF26Dot6((double) i56 * (1.0D / Math.pow(2D, gs.getDeltaShift())));
                                int j61 = gs.getCvtTable().get(cvtVal);
                                j61 += i59;
                                gs.getCvtTable().writePixels(cvtVal, j61);
                            }
                            idx++;
                        } while (true);
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("DELTAC2 - stack popped: " + (1 + n * 2));
                        }
                        return instOffset;
                    }
                };
        // DELTAC3[] DELTA exception C3
        INSTRUCTIONS[InstructionNames.DELTAC3] =
                new HintInstruction(InstructionNames.DELTAC3, "DELTAC3") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {
                        int n = stack.pop();
                        int idx = 0;
                        do {
                            if (idx >= n)
                                break;
                            int cvtVal = stack.pop();
                            int exceptionVal = stack.pop();
                            int j52 = gs.getDeltaBase() + 32 + (exceptionVal >> 4);
                            if ((double) j52 == Interpreter.ppem) {
                                int j56 = (exceptionVal & 0xf) - 7;
                                if (j56 <= 0)
                                    j56--;
                                int j59 = storeDoubleAsF26Dot6((double) j56 * (1.0D / Math.pow(2D, gs.getDeltaShift())));
                                int k61 = gs.getCvtTable().get(cvtVal);
                                k61 += j59;
                                gs.getCvtTable().writePixels(cvtVal, k61);
                            }
                            idx++;
                        } while (true);
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("DELTAC3 - stack popped: " + (1 + n * 2));
                        }
                        return instOffset;
                    }
                };
        // DELTAP1[] DELTA exception P1
        INSTRUCTIONS[InstructionNames.DELTAP1] =
                new HintInstruction(InstructionNames.DELTAP1, "DELTAP1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {

                        int n = stack.pop();
                        int idx = 0;
                        do {

                            if (idx >= n) {
                                break;
                            }
                            int pointId = stack.pop();
                            int exceptionVal = stack.pop();
                            int i51 = gs.getDeltaBase() + (exceptionVal >> 4);
                            if ((double) i51 == Interpreter.ppem) {
                                int i55 = (exceptionVal & 0xf) - 7;
                                if (i55 <= 0)
                                    i55--;
                                int i58 = storeDoubleAsF26Dot6((double) i55 * (1.0D / Math.pow(2D, gs.getDeltaShift())));
                                Point2D.Float deltaPt = getFreedomDistanceForProjectionMove(i58, gs);
                                float xPt = glyphDef.getXPtr(gs.zp0, pointId);
                                float yPt = glyphDef.getYPtr(gs.zp0, pointId);

                                xPt += deltaPt.x;
                                yPt += deltaPt.y;
                                Point2D.Float fV = gs.getFreedomVector();

                                if (fV.x != 0) {
                                    glyphDef.setXPtr(gs.zp0, pointId, xPt);
                                    glyphDef.setTouchedXPtr(gs.zp0, pointId, true);
                                }
                                if (fV.y != 0) {
                                    glyphDef.setYPtr(gs.zp0, pointId, yPt);
                                    glyphDef.setTouchedYPtr(gs.zp0, pointId, true);
                                }
                            }
                            idx++;
                        } while (true);
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("DELTAP1 - stack popped: " + (1 + n * 2));
                        }
                        return instOffset;
                    }
                };
        // DELTAP2[] DELTA exception P2
        INSTRUCTIONS[InstructionNames.DELTAP2] =
                new HintInstruction(InstructionNames.DELTAP2, "DELTAP2") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {
                        int n = stack.pop();
                        int idx = 0;
                        do {
                            if (idx >= n)
                                break;
                            int pointId = stack.pop();
                            int exceptionVal = stack.pop();
                            int j51 = gs.getDeltaBase() + 16 + (exceptionVal >> 4);
                            if ((double) j51 == Interpreter.ppem) {
                                int j55 = (exceptionVal & 0xf) - 7;
                                if (j55 <= 0)
                                    j55--;
                                int j58 = storeDoubleAsF26Dot6((double) j55 * (1.0D / Math.pow(2D,
                                        gs.getDeltaShift())));
                                Point2D.Float deltaPt = getFreedomDistanceForProjectionMove(j58, gs);

                                float xPt = glyphDef.getXPtr(gs.zp0, pointId);
                                float yPt = glyphDef.getYPtr(gs.zp0, pointId);

                                xPt += deltaPt.x;
                                yPt += deltaPt.y;
                                Point2D.Float fV = gs.getFreedomVector();

                                if (fV.x != 0) {
                                    glyphDef.setXPtr(gs.zp0, pointId, xPt);
                                    glyphDef.setTouchedXPtr(gs.zp0, pointId, true);
                                }
                                if (fV.y != 0) {
                                    glyphDef.setYPtr(gs.zp0, pointId, yPt);
                                    glyphDef.setTouchedYPtr(gs.zp0, pointId, true);
                                }
                            }
                            idx++;
                        } while (true);
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("DELTAP2 - stack popped: " + (1 + n * 2));
                        }
                        return instOffset;
                    }
                };
        // DELTAP3[] DELTA exception P3
        INSTRUCTIONS[InstructionNames.DELTAP3] =
                new HintInstruction(InstructionNames.DELTAP3, "DELTAP3") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {
                        int n = stack.pop();
                        int idx = 0;
                        do {
                            if (idx >= n)
                                break;
                            int pointId = stack.pop();
                            int exceptionVal = stack.pop();
                            int k51 = gs.getDeltaBase() + 32 + (exceptionVal >> 4);
                            if ((double) k51 == Interpreter.ppem) {
                                int k55 = (exceptionVal & 0xf) - 7;
                                if (k55 <= 0)
                                    k55--;
                                int k58 = storeDoubleAsF26Dot6((double) k55 * (1.0D / Math.pow(2D, gs.getDeltaShift())));
                                Point2D.Float deltaPt = getFreedomDistanceForProjectionMove(k58, gs);

                                float xPt = glyphDef.getXPtr(gs.zp0, pointId);
                                float yPt = glyphDef.getYPtr(gs.zp0, pointId);

                                xPt += deltaPt.x;
                                yPt += deltaPt.y;
                                Point2D.Float fV = gs.getFreedomVector();

                                if (fV.x != 0) {
                                    glyphDef.setXPtr(gs.zp0, pointId, xPt);
                                    glyphDef.setTouchedXPtr(gs.zp0, pointId, true);
                                }
                                if (fV.y != 0) {
                                    glyphDef.setYPtr(gs.zp0, pointId, yPt);
                                    glyphDef.setTouchedYPtr(gs.zp0, pointId, true);
                                }
                            }
                            idx++;
                        } while (true);
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("DELTAP3 - stack popped: " + (1 + n * 2));
                        }
                        return instOffset;
                    }
                };
        // DEPTH[] DEPTH of the stack
        INSTRUCTIONS[InstructionNames.DEPTH] =
                new HintInstruction(InstructionNames.DEPTH, "DEPTH") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        stack.push(stack.size());
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("DEPTH - pushed: " + (stack.size() - 1));
                        }
                        return instOffset;
                    }
                };
        // DIV[] DIVide
        INSTRUCTIONS[InstructionNames.DIV] =
                new HintInstruction(InstructionNames.DIV, "DIV") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int divisor = stack.pop();
                        int dividend = stack.pop();
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("DIV - dividing: " + dividend + " by " + divisor);
                        }
                        stack.push((dividend * 64) / divisor);
                        return instOffset;
                    }
                };
        // DUP[] DUPlicate top stack element
        INSTRUCTIONS[InstructionNames.DUP] =
                new HintInstruction(InstructionNames.DUP, "DUP") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int dup = stack.pop();
                        stack.push(dup);
                        stack.push(dup);
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("DUP - duplicating: " + dup + " on stack");
                        }
                        return instOffset;
                    }
                };
        // EIF[] End IF
        INSTRUCTIONS[InstructionNames.EIF] =
                new HintInstruction(InstructionNames.EIF, "EIF") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("ENDIF - Does nothing");
                        }
                        return instOffset;
                    }
                };
        // ELSE[] ELSE clause
        INSTRUCTIONS[InstructionNames.ELSE] =
                new HintInstruction(InstructionNames.ELSE, "ELSE") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int opcode = 0;
                        int nestingDepth = 0;
                        int pc = instOffset;
                        do {
                            if (opcode == InstructionNames.EIF && nestingDepth != 0) {
                                nestingDepth--;
                            }
                            pc++;
                            opcode = instr[pc] & 0xFF;
                            if (opcode == InstructionNames.IF) {
                                nestingDepth++;
                            }
                            if (opcode == InstructionNames.NPUSHB) {
                                pc = ++pc + instr[pc];
                            } else if (opcode == InstructionNames.NPUSHW) {
                                pc = ++pc + instr[pc] * 2;
                            } else if (opcode >= InstructionNames.PUSHB_0 && opcode <= InstructionNames.PUSHB_7) {
                                pc += (opcode + 1) - 176;
                            } else if (opcode >= InstructionNames.PUSHW_0 && opcode <= InstructionNames.PUSHW_7) {
                                pc += ((opcode + 1) - 184) * 2;
                            }
                        }
                        while (opcode != InstructionNames.EIF || nestingDepth != 0);
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("ELSE - does nothing");
                        }
                        return pc;
                    }
                };

        // ENDF[] END Function definition
        INSTRUCTIONS[InstructionNames.ENDF] =
                new HintInstruction(InstructionNames.ENDF, "ENDF") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        // nothing to to do, FDEF should have eaten this op code
                        // for us.
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("ENDF - does nothing");
                        }
                        return instOffset;
                    }
                };
        // EQ[] EQual
        INSTRUCTIONS[InstructionNames.EQ] =
                new HintInstruction(InstructionNames.EQ, "EQ") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        if (stack.pop() == stack.pop()) {
                            if (logger.isLoggable(Level.FINEST)) {
                                CallContext.log("EQ - top stack values ARE equal, pushing 1");
                            }
                            stack.push(1);
                        } else {
                            if (logger.isLoggable(Level.FINEST)) {
                                CallContext.log("EQ - top stack values NOT equal, pushing 0");
                            }
                            stack.push(0);
                        }
                        return instOffset;
                    }
                };
        // EVEN[] EVEN
        INSTRUCTIONS[InstructionNames.EVEN] =
                new HintInstruction(InstructionNames.EVEN, "EVEN") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int val = stack.pop();
                        val = storeDoubleAsF26Dot6(graphicsState.round(getF26Dot6AsDouble(val)));

                        val = ((val >> 6) + 1) % 2;
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("EVEN - Pushing: " + val);
                        }
                        stack.push(val);
                        return instOffset;
                    }
                };
        // FDEF[] Function DEFinition
        INSTRUCTIONS[InstructionNames.FDEF] =
                new HintInstruction(InstructionNames.FDEF, "FDEF") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
//                        int functionNumber = stack.pop();

                        int fNum = stack.pop();
                        int iptr = instOffset;
                        int opcode;
                        do {
                            instOffset++;
                            opcode = instr[instOffset] & 0xFF;
                            if (opcode == 64) {
                                instOffset = ++instOffset + instr[instOffset];
                            } else if (opcode == 65) {
                                instOffset = ++instOffset + instr[instOffset] * 2;
                            } else if (opcode >= 176 && opcode <= 183) {
                                instOffset += (opcode + 1) - 176;
                            } else if (opcode >= 184 && opcode <= 191) {
                                instOffset += ((opcode + 1) - 184) * 2;
                            }
                        } while (opcode != 45); // While ! eofunction

                        int subLength = instOffset - iptr - 1;
                        instOffset = iptr;
                        int fnc[] = new int[subLength];
                        for (int i = 0; i < subLength; i++) {
                            instOffset++;
                            fnc[i] = (instr[instOffset] & 0xFF);
                        }
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("FDEF - defining function: " + fNum);
                        }
                        graphicsState.addFunction(fNum, fnc);
                        // offset should be ready for the next instruction skipping the end.
                        instOffset++;
                        return instOffset;
                    }
                };
        // FLIPOFF[] set the auto FLIP Boolean to OFF
        INSTRUCTIONS[InstructionNames.FLIPOFF] =
                new HintInstruction(InstructionNames.FLIPOFF, "FLIPOFF") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        graphicsState.setAutoFlip(false);
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("FLIPOFF - setting off");
                        }
                        return instOffset;
                    }
                };
        // FLIPON[] set the auto FLIP Boolean to ON
        INSTRUCTIONS[InstructionNames.FLIPON] =
                new HintInstruction(InstructionNames.FLIPON, "FLIPON") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        graphicsState.setAutoFlip(true);
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("FLIPON - setting on");
                        }
                        return instOffset;
                    }
                };
        // FLIPPT[] FLIP PoinT
        INSTRUCTIONS[InstructionNames.FLIPPT] =
                new HintInstruction(InstructionNames.FLIPPT, "FLIPPT") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        for (int i = 0, max = graphicsState.getLoop(); i < max; i++) {

                            int point = stack.pop();
                            byte flag = glyphDef.getFlagsPtr(point);
                            flag ^= 0x01;
                            if (logger.isLoggable(Level.FINEST)) {
                                CallContext.log("FLIPPT - setting point: " + point + " to " + ((flag & 0x01) != 0));
                            }
                            glyphDef.setFlagsPtr(point, flag);

                        }
                        graphicsState.setLoop(1);
                        return instOffset;
                    }
                };
        // FLIPRGOFF[] FLIP RanGe OFF
        INSTRUCTIONS[InstructionNames.FLIPRGOFF] =
                new HintInstruction(InstructionNames.FLIPRGOFF, "FLIPRGOFF") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int highPoint = stack.pop();
                        int lowPoint = stack.pop();

                        for (int i = lowPoint, max = highPoint; i <= max; i++) {
                            byte flag = glyphDef.getFlagsPtr(i);
                            flag &= 0xFE;
                            if (logger.isLoggable(Level.FINEST)) {
                                CallContext.log("FLIPRGOFF - setting point: " + i + " to " + (((flag & 0x01) != 0)));
                            }
                            glyphDef.setFlagsPtr(i, flag);
                        }
                        return instOffset;
                    }
                };
        // FLIPRGON[] FLIP RanGe ON
        INSTRUCTIONS[InstructionNames.FLIPRGON] =
                new HintInstruction(InstructionNames.FLIPRGON, "FLIPRGON") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int highPoint = stack.pop();
                        int lowPoint = stack.pop();
                        for (int i = lowPoint, max = highPoint; i <= max; i++) {
                            byte flag = glyphDef.getFlagsPtr(i);
                            flag |= 0x01;
                            if (logger.isLoggable(Level.FINEST)) {
                                CallContext.log("FLIPRGON - setting point: " + i + " to " + ((flag & 0x01) != 0));
                            }
                            glyphDef.setFlagsPtr(i, flag);
                        }
                        return instOffset;
                    }
                };
        // FLOOR[] FLOOR
        INSTRUCTIONS[InstructionNames.FLOOR] =
                new HintInstruction(InstructionNames.FLOOR, "FLOOR") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        stack.push(setF26Dot6((float) Math.floor(getF26Dot6AsFloat(stack.pop()))));
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("FLOOR - stack neutral");
                        }
                        return instOffset;
                    }
                };
        // GC[a] Get Coordinate projected onto the projection vector
        INSTRUCTIONS[InstructionNames.GC_0] =
                new HintInstruction(InstructionNames.GC_0, "GC_0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {

                        int pt = stack.pop();
                        int x = glyphDef.getXPtr(gs.zp2, pt);
                        int y = glyphDef.getYPtr(gs.zp2, pt);

                        int val = getProjectionOnVector(gs.getProjectionVector(), x, y);
                        stack.push(val);
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("GC_0 - Pushed: " + val);
                        }
                        return instOffset;
                    }
                };
        // GC[a] Get Coordinate projected onto the projection vector
        INSTRUCTIONS[InstructionNames.GC_1] =
                new HintInstruction(InstructionNames.GC_1, "GC_1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {
                        int pt = stack.pop();
                        int x = glyphDef.getXPtr(2+gs.zp2, pt);
                        int y = glyphDef.getYPtr(2+gs.zp2, pt);

                        int val = getProjectionOnVector(gs.getProjectionVector(), x, y);
                        stack.push(val);
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("GC_1 - Pushed: " + val);
                        }
                        return instOffset;
                    }
                };
        // GETINFO[] GET INFOrmation
        INSTRUCTIONS[InstructionNames.GETINFO] =
                new HintInstruction(InstructionNames.GETINFO, "GETINFO") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int selector = stack.pop();
                        int result = 0;
                        if ((selector & 1) == 1) {
                            result += 3;
                        }
                        stack.push(result);
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("GETINFO - yawn");
                        }
                        return instOffset;
                    }
                };
        // GFV[] Get Freedom Vector
        INSTRUCTIONS[InstructionNames.GFV] =
                new HintInstruction(InstructionNames.GFV, "GFV") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        Point2D.Float freedomVector = graphicsState.getFreedomVector();
                        stack.push(setF2Dot14(freedomVector.x));
                        stack.push(setF2Dot14(freedomVector.y));
                        CallContext.log("GFV -- stack pushed 2 items");
                        return instOffset;
                    }
                };
        // GPV[] Get Projection Vector
        INSTRUCTIONS[InstructionNames.GPV] =
                new HintInstruction(InstructionNames.GPV, "GPV") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        Point2D.Float projectionVector = graphicsState.getProjectionVector();
                        stack.push(setF2Dot14(projectionVector.x));
                        stack.push(setF2Dot14(projectionVector.y));
                        CallContext.log("GPV -- stack pushed 2 items");
                        return instOffset;
                    }
                };
        // GT[] Greater Than
        INSTRUCTIONS[InstructionNames.GT] =
                new HintInstruction(InstructionNames.GT, "GT") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int e2 = stack.pop(); // e2: stack element
                        int e1 = stack.pop(); // e1: stack element
                        if (e1 > e2) {
                            CallContext.log("GT " + e1 + " is > " + e2 + " push(1) - DO subsequent IF clause");
                            stack.push(1);
                        } else {
                            CallContext.log("GT " + e1 + " is <= " + e2 + " push(0)  - SKIP subsequent IF clause");
                            stack.push(0);
                        }
                        return instOffset;
                    }
                };
        // GTEQ[] Greater Than or EQual
        INSTRUCTIONS[InstructionNames.GTEQ] =
                new HintInstruction(InstructionNames.GTEQ, "GTEQ") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int e2 = stack.pop(); // e2: stack element
                        int e1 = stack.pop(); // e1: stack element
                        if (e1 >= e2) {
                            CallContext.log("GTEQ " + e1 + " is >= " + e2 + " push(1) - DO subsequent IF clause");
                            stack.push(1);
                        } else {
                            CallContext.log("GTEQ " + e1 + " is < " + e2 + " push(0) - SKIP subsequent IF clause");
                            stack.push(0);
                        }
                        return instOffset;
                    }
                };
        // IDEF[] Instruction DEFinition
        INSTRUCTIONS[InstructionNames.IDEF] =
                new HintInstruction(InstructionNames.IDEF, "IDEF") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int instructionDefinition = stack.pop();
                        instOffset++;// skip paste FDEF
                        int fdefStart = instOffset;
                        // scan ahead to find the InstructionNames.ENDF
                        while (instr[instOffset] != InstructionNames.ENDF) {
                            instOffset++;
                        }
                        // copy over the instructions into a new instructions array
                        byte[] fInstr = new byte[instOffset - fdefStart - 1];
                        System.arraycopy(instr, fdefStart, fInstr, 0, fInstr.length);

                        graphicsState.addInstruction(instructionDefinition, fInstr);
                        // offset should be ready for the next instruction
                        CallContext.log("IDEF - ");
                        return instOffset;
                    }
                };
        // IF[] IF test
        INSTRUCTIONS[InstructionNames.IF] =
                new HintInstruction(InstructionNames.IF, "IF") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        boolean execIf = stack.pop() != 0;
                        if (execIf) {
                            CallContext.log("IF - Executing clause");
                            return instOffset;
                        }

                        int opcode = 0;
                        int nestingDepth = 0;
                        int pc = instOffset;
                        do {
                            if (opcode == InstructionNames.EIF && nestingDepth != 0) {
                                nestingDepth--;
                            }
                            pc++;
                            opcode = instr[pc] & 0xFF;
                            if (opcode == InstructionNames.IF) {
                                nestingDepth++;
                            }
                            if (opcode == InstructionNames.NPUSHB) {
                                pc = ++pc + instr[pc];
                            } else if (opcode == InstructionNames.NPUSHW) {
                                pc = ++pc + instr[pc] * 2;
                            } else if (opcode >= InstructionNames.PUSHB_0 && opcode <= InstructionNames.PUSHB_7) {
                                pc += (opcode + 1) - 176;
                            } else if (opcode >= InstructionNames.PUSHW_0 && opcode <= InstructionNames.PUSHW_7) {
                                pc += ((opcode + 1) - 184) * 2;
                            }
                        }
                        while (opcode != InstructionNames.ELSE && opcode != InstructionNames.EIF || nestingDepth != 0);
                        CallContext.log("IF - skipping clause length: " + (pc - instOffset));
                        return pc;
                    }
                };
        // INSTCTRL INSTRuction execution ConTRoL
        INSTRUCTIONS[InstructionNames.INSTCTRL] =
                new HintInstruction(InstructionNames.INSTCTRL, "INSTRCTRL") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int selector = stack.pop();
                        int value = stack.pop();
                        if (selector == 1) {
                            graphicsState.setInstructControl( value == 1 );
                        } else if (selector == 2) {
                            Interpreter.setsUseDefaultGS (value == 2);
                        }
                        return instOffset;
                    }
                };
        // IP[] Interpolate Point
        INSTRUCTIONS[InstructionNames.IP] =
                new HintInstruction(InstructionNames.IP, "IP") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {

                        int loop = gs.getLoop();
                        for (int idx = 0; idx < loop; idx++) {

                            int pt = stack.pop();
                            float xPtr = glyphDef.getXPtr(gs.zp2, pt);
                            float yPtr = glyphDef.getYPtr(gs.zp2, pt);

                            // Check ranges? Really?
//                        if(j26 < 0 || j26 > x[ttgraphicsstate.zp2].length || ttgraphicsstate.rp1 > x[ttgraphicsstate.zp0].length || ttgraphicsstate.rp2 > x[ttgraphicsstate.zp1].length)
//                            break;
                            if (gs.rp1 == gs.rp2) {
                                return instOffset;
                            }

                            int xRp1 = glyphDef.getXPtr(2 + gs.zp0, gs.rp1);
                            int yRp1 = glyphDef.getYPtr(2 + gs.zp0, gs.rp1);

                            int xRp2 = glyphDef.getXPtr(2 + gs.zp1, gs.rp2);
                            int yRp2 = glyphDef.getYPtr(2 + gs.zp1, gs.rp2);

                            int k38 = getProjectionOnVector(gs.getDualProjectionVector(), xRp1, yRp1);
                            int l45 = getProjectionOnVector(gs.getDualProjectionVector(), xRp2, yRp2);

                            if (k38 == l45)
                                continue;

                            int xPt = glyphDef.getXPtr(2 + gs.zp2, pt);
                            int yPt = glyphDef.getYPtr(2 + gs.zp2, pt);

                            int j50 = getProjectionOnVector(gs.getDualProjectionVector(), xPt, yPt);
                            double d16 = (double) (j50 - k38) / (double) (l45 - k38);

                            int xRP1Z0 = glyphDef.getXPtr(gs.zp0, gs.rp1);
                            int yRP1Z0 = glyphDef.getYPtr(gs.zp0, gs.rp1);

                            int l60 = getProjectionOnVector(gs.getProjectionVector(), xRP1Z0, yRP1Z0);

                            int xRP2ZP1 = glyphDef.getXPtr(gs.zp1, gs.rp2);
                            int yRP2ZP1 = glyphDef.getYPtr(gs.zp1, gs.rp2);

                            int k62 = getProjectionOnVector(gs.getProjectionVector(), xRP2ZP1, yRP2ZP1);

                            int i64 = (int) (d16 * (double) (k62 - l60) + (double) l60 + 0.5D) - j50;
                            Point2D.Float deltaPt = getFreedomDistanceForProjectionMove(i64, gs);

                            xPtr += deltaPt.x;
                            yPtr += deltaPt.y;

                            if (logger.isLoggable(Level.FINEST)) {
                                if (loop > 1) {
                                    CallContext.logEndln("IP - move p: " + pt + " to [x,y] [" + Math.round(xPtr) +
                                        "," + Math.round(yPtr) + "]");
                                } else {
                                     CallContext.log("IP - move p: " + pt + " to [x,y] [" + Math.round(xPtr) +
                                        "," + Math.round(yPtr) + "]");
                                }
                            }
                            glyphDef.setXPtr(gs.zp0, pt, xPtr);
                            glyphDef.setYPtr(gs.zp0, pt, yPtr);
                            if (gs.getFreedomVector().x != 0) {
                                glyphDef.setTouchedXPtr(gs.zp2, pt, true);
                            }
                            if (gs.getFreedomVector().y != 0) {
                                glyphDef.setTouchedYPtr(gs.zp2, pt, true);
                            }
                        }
                        gs.setLoop(1);
                        return instOffset;
                    }
                };
        // ISECT[] moves point p to the InterSECTion of two lines
        INSTRUCTIONS[InstructionNames.ISECT] =
                new HintInstruction(InstructionNames.ISECT, "ISECT") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int a0 = stack.pop() & 0xff; // start point of line A (uint32)
                        int a1 = stack.pop() & 0xff; // end point of line A (uint32)
                        int b0 = stack.pop() & 0xff; // start point of line B (uint32)
                        int b1 = stack.pop() & 0xff; // end point of line B (uint32)
                        int p = stack.pop() & 0xff; // point to move (uint32)
                        // find the actual coordinates
                        Point2D.Float A1 = createPoint(glyphDef, graphicsState.getZp0(), a0);
                        Point2D.Float A2 = createPoint(glyphDef, graphicsState.getZp0(), a1);
                        Point2D.Float B3 = createPoint(glyphDef, graphicsState.getZp1(), b0);
                        Point2D.Float B4 = createPoint(glyphDef, graphicsState.getZp1(), b1);
                        // calculate the intersection
                        Float xIntersect =
                                (((A1.x * A2.y) - (A1.y * A2.x)) * (B3.x - B4.x)) - ((A1.x - A2.x) * ((B3.x * B4.y) - (B3.y * B4.x))) /
                                        (((A1.x - A2.x) * (B3.y - B4.y)) - ((A1.y - A2.y) * (B3.x - B4.x)));
                        Float yIntersect =
                                (((A1.x * A2.y) - (A1.y * A2.x)) * (B3.y - B4.y)) - ((A1.y - A2.y) * ((B3.x * B4.y) - (B3.y * B4.x))) /
                                        (((A1.x - A2.x) * (B3.y - B4.y)) - ((A1.y - A2.y) * (B3.x - B4.x)));
                        // update the specified point.
                        glyphDef.setXPtr(graphicsState.zp2, p, xIntersect.floatValue());
                        glyphDef.setYPtr(graphicsState.zp2, p, yIntersect.floatValue());
                        return instOffset;
                    }
                };
        // IUP_0 Interpolate Untouched Points through the outline in y axis
        INSTRUCTIONS[InstructionNames.IUP_0] =
                new HintInstruction(InstructionNames.IUP_0, "IUP_0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("IUPy - Interpolate in Y");
                        }
                        debugPriors(glyphDef);
                        interpolateUntouchedPoints(48, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // IUP_1 Interpolate Untouched Points through the outline in x axis
        INSTRUCTIONS[InstructionNames.IUP_1] =
                new HintInstruction(InstructionNames.IUP_1, "IUP_1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("IUPx - Interpolate in X");
                        }
                        interpolateUntouchedPoints(49, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // JMPR JuMP Relative
        INSTRUCTIONS[InstructionNames.JMPR] =
                new HintInstruction(InstructionNames.JMPR, "JMPR") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        // jump ahead to specified instruction offset.
                        return instOffset + stack.pop() - 1;
                    }
                };
        // JROF[] Jump Relative On False
        INSTRUCTIONS[InstructionNames.JROF] =
                new HintInstruction(InstructionNames.JROF, "JROF") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        boolean flag = stack.pop() == 0; // e: stack element
                        int offset = stack.pop(); // offset: number of bytes to move instruction pointer (int32)
                        if (flag) {
                            instOffset += offset - 1;
                            CallContext.log("JROF - jumped to: " + instOffset);
                        } else {
                            CallContext.log("JROF - didn't jump");
                        }

                        return instOffset;
                    }
                };
        // JROT[] Jump Relative On True
        INSTRUCTIONS[InstructionNames.JROT] =
                new HintInstruction(InstructionNames.JROT, "JROT") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        boolean flag = stack.pop() != 0; // e: stack element
                        int offset = stack.pop(); // offset: number of bytes to move instruction pointer (int32)
                        if (flag) {
                            instOffset += offset - 1;
                            CallContext.log("JROT - jumped to: " + instOffset);
                        } else {
                            CallContext.log("JROT - didn't jump");
                        }
                        return instOffset;
                    }
                };
        // LOOPCALL[] LOOP and CALL function
        INSTRUCTIONS[InstructionNames.LOOPCALL] =
                new HintInstruction(InstructionNames.LOOPCALL, "LOOPCALL") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int functionNumber = stack.pop();
                        int count = stack.pop();
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.logContinuationEndln("LOOPCALL - START");
                        }
                        int[] instructions = graphicsState.getFunction(functionNumber);
                        if (instructions != null) {
                            for (int i = 0; i < count; i++) {
                                Interpreter.execute(glyphDef, instructions, stack, graphicsState);
                            }
                        }
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("LOOPCALL - FIN. Called Instructions: " + count + " times");
                        }
                        return instOffset;
                    }
                };
        // LT[] Less Than
        INSTRUCTIONS[InstructionNames.LT] =
                new HintInstruction(InstructionNames.LT, "LT") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int e2 = stack.pop(); // e2: stack element
                        int e1 = stack.pop(); // e1: stack element
                        if (e1 < e2) {
                            if (logger.isLoggable(Level.FINEST)) {
                                CallContext.log("LT - pushing 1 (" + e1 + " < " + e2 + ")");
                            }
                            stack.push(1);
                        } else {
                            if (logger.isLoggable(Level.FINEST)) {
                                CallContext.log("LT - pushing 0 (" + e1 + " >= " + e2 + ")");
                            }
                            stack.push(0);
                        }
                        return instOffset;
                    }
                };
        // LTEQ[] Less Than or Equal
        INSTRUCTIONS[InstructionNames.LTEQ] =
                new HintInstruction(InstructionNames.LTEQ, "LTEQ") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int e2 = stack.pop(); // e2: stack element
                        int e1 = stack.pop(); // e1: stack element
                        if (e1 <= e2) {
                            if (logger.isLoggable(Level.FINEST)) {
                                CallContext.log("LTEQ - Pushing: " + 1);
                            }
                            stack.push(1);
                        } else {
                            if (logger.isLoggable(Level.FINEST)) {
                                CallContext.log("LTEQ - Pushing: " + 0);
                            }
                            stack.push(0);
                        }
                        return instOffset;
                    }
                };
        // MAX[] MAXimum of top two stack elements
        INSTRUCTIONS[InstructionNames.MAX] =
                new HintInstruction(InstructionNames.MAX, "MAX") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int e2 = stack.pop(); // e2: stack element
                        int e1 = stack.pop(); // e1: stack element
                        if (e1 > e2) {
                            if (logger.isLoggable(Level.FINEST)) {
                                CallContext.log("MAX - pushing " + e1);
                            }
                            stack.push(e1);
                        } else {
                            if (logger.isLoggable(Level.FINEST)) {
                                CallContext.log("MAX - pushing " + e2);
                            }
                            stack.push(e2);
                        }
                        return instOffset;
                    }
                };
        // MD[a] Measure Distance
        INSTRUCTIONS[InstructionNames.MD_0] =
                new HintInstruction(InstructionNames.MD_0, "MD_0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {

                        int p1 = stack.pop();
                        int p2 = stack.pop();

                        int x2 = glyphDef.getXPtr(gs.zp1, p2);
                        int y2 = glyphDef.getYPtr(gs.zp1, p2);
                        int x1 = glyphDef.getXPtr(gs.zp0, p1);
                        int y1 = glyphDef.getYPtr(gs.zp0, p1);

                        int d = getProjectionOnVector(gs.getProjectionVector(), x2, y2) -
                                getProjectionOnVector(gs.getProjectionVector(), x1, y1);
                        stack.push(d);
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("MD_0 - measured distance: " + d);
                        }
                        return instOffset;
                    }
                };
        // MD[a] Measure Distance
        INSTRUCTIONS[InstructionNames.MD_1] =
                new HintInstruction(InstructionNames.MD_1, "MD_1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {
                        int p1 = stack.pop();
                        int p2 = stack.pop();

                        int x2 = glyphDef.getXPtr(2 + gs.zp1, p2);
                        int y2 = glyphDef.getYPtr(2 + gs.zp1, p2);
                        int x1 = glyphDef.getXPtr(2 + gs.zp0, p1);
                        int y1 = glyphDef.getYPtr(2 + gs.zp0, p1);

                        int d = getProjectionOnVector(gs.getProjectionVector(), x2, y2) -
                                getProjectionOnVector(gs.getProjectionVector(), x1, y1);
                        stack.push(d);
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("MD_1 - measured distance: " + d);
                        }
                        return instOffset;
                    }
                };
        // MDAP[a] Set point as touched
        INSTRUCTIONS[InstructionNames.MDAP_0] =
                new HintInstruction(InstructionNames.MDAP_0, "MDAP_0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int pointNumber = stack.pop();
                        graphicsState.setRp0(pointNumber);
                        graphicsState.setRp1(pointNumber);
                        Point2D.Float fV = graphicsState.getFreedomVector();
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.logContinuationIndent("MDAP_0 - Setting point: " + pointNumber);
                        }
                        if (fV.x != 0) {
                            glyphDef.setTouchedXPtr(graphicsState.zp0, pointNumber, true);
                            if (logger.isLoggable(Level.FINEST)) {
                                CallContext.logContinuation(" to X-TOUCHED");
                            }
                        }
                        if (fV.y != 0) {
                            if (logger.isLoggable(Level.FINEST)) {
                                CallContext.logContinuation(" to Y-TOUCHED");
                            }
                            glyphDef.setTouchedYPtr(graphicsState.zp0, pointNumber, true);
                        }
                        return instOffset;
                    }
                };
        // MDAP[a] Rounds a point along the pv and marks as touched
        INSTRUCTIONS[InstructionNames.MDAP_1] =
                new HintInstruction(InstructionNames.MDAP_1, "MDAP_1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int pointNumber = stack.pop();
                        Point2D.Float fV = graphicsState.getFreedomVector();
                        graphicsState.setRp0(pointNumber);
                        graphicsState.setRp1(pointNumber);

                        float xPtr = glyphDef.getXPtr(graphicsState.zp0, pointNumber);
                        float yPtr = glyphDef.getYPtr(graphicsState.zp0, pointNumber);

                        int i25 = getProjectionOnVector(graphicsState.getProjectionVector(),
                                xPtr, yPtr);

                        i25 = storeDoubleAsF26Dot6(graphicsState.round(getF26Dot6AsDouble(i25))) - i25;
                        Point2D.Float delta = getFreedomDistanceForProjectionMove(i25, graphicsState);

                        xPtr += delta.x;
                        yPtr += delta.y;

                        glyphDef.setXPtr(graphicsState.zp0, pointNumber, xPtr );
                        glyphDef.setYPtr(graphicsState.zp0, pointNumber, yPtr );

                        if (fV.x != 0) {
                            glyphDef.setTouchedXPtr(graphicsState.zp0, pointNumber, true);
                        }
                        if (fV.y != 0) {
                            glyphDef.setTouchedYPtr(graphicsState.zp0, pointNumber, true);
                        }
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("MDAP_1 setting point: " + pointNumber + " to [x,y] " +
                                    Math.round(xPtr) + "," + Math.round(yPtr) );
                        }
                        return instOffset;
                    }
                };

        // MDRP_0[abcde] Move Direct Relative Point
        INSTRUCTIONS[InstructionNames.MDRP_0] =
                new HintInstruction(InstructionNames.MDRP_0, "MDRP_0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xC0 = 1100 0000
                        doMDRP(p, false, false, false, 0, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_1.
        INSTRUCTIONS[InstructionNames.MDRP_1] =
                new HintInstruction(InstructionNames.MDRP_1, "MDRP_1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xC1 = 1100 0001
                        doMDRP(p, false, false, false, 1, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_2.
        INSTRUCTIONS[InstructionNames.MDRP_2] =
                new HintInstruction(InstructionNames.MDRP_2, "MDRP_2") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xC2 = 1100 0010
                        doMDRP(p, false, false, false, 2, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_3.
        INSTRUCTIONS[InstructionNames.MDRP_3] =
                new HintInstruction(InstructionNames.MDRP_3, "MDRP_3") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xC1 = 1100 0011
                        doMDRP(p, false, false, false, 3, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_4.
        INSTRUCTIONS[InstructionNames.MDRP_4] =
                new HintInstruction(InstructionNames.MDRP_4, "MDRP_4") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xC4 = 1100 0100
                        doMDRP(p, false, false, true, 0, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_5.
        INSTRUCTIONS[InstructionNames.MDRP_5] =
                new HintInstruction(InstructionNames.MDRP_5, "MDRP_5") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xC5 = 1100 0101
                        doMDRP(p, false, false, true, 1, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_6.
        INSTRUCTIONS[InstructionNames.MDRP_6] =
                new HintInstruction(InstructionNames.MDRP_6, "MDRP_6") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xC6 = 1100 0110
                        doMDRP(p, false, false, true, 2, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_7.
        INSTRUCTIONS[InstructionNames.MDRP_7] =
                new HintInstruction(InstructionNames.MDRP_7, "MDRP_7") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xC7 = 1100 0111
                        doMDRP(p, false, false, true, 3, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_8.
        INSTRUCTIONS[InstructionNames.MDRP_8] =
                new HintInstruction(InstructionNames.MDRP_8, "MDRP_8") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xC8 = 1100 1000
                        doMDRP(p, false, true, false, 0, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_9.
        INSTRUCTIONS[InstructionNames.MDRP_9] =
                new HintInstruction(InstructionNames.MDRP_9, "MDRP_8") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xC9 = 1100 1001
                        doMDRP(p, false, true, false, 1, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_10.
        INSTRUCTIONS[InstructionNames.MDRP_10] =
                new HintInstruction(InstructionNames.MDRP_10, "MDRP_10") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xCA = 1100 1010
                        doMDRP(p, false, true, false, 2, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_11.
        INSTRUCTIONS[InstructionNames.MDRP_11] =
                new HintInstruction(InstructionNames.MDRP_11, "MDRP_11") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xCB = 1100 1011
                        doMDRP(p, false, true, false, 3, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_12.
        INSTRUCTIONS[InstructionNames.MDRP_12] =
                new HintInstruction(InstructionNames.MDRP_12, "MDRP_12") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xCC = 1100 1100
                        doMDRP(p, false, true, true, 0, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_13.
        INSTRUCTIONS[InstructionNames.MDRP_13] =
                new HintInstruction(InstructionNames.MDRP_13, "MDRP_13") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xCD = 1100 1101
                        doMDRP(p, false, true, true, 1, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_14.
        INSTRUCTIONS[InstructionNames.MDRP_14] =
                new HintInstruction(InstructionNames.MDRP_14, "MDRP_14") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xCE = 1100 1110
                        doMDRP(p, false, true, true, 2, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_15.
        INSTRUCTIONS[InstructionNames.MDRP_15] =
                new HintInstruction(InstructionNames.MDRP_15, "MDRP_15") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xCF = 1100 1111
                        doMDRP(p, false, true, true, 3, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_16.
        INSTRUCTIONS[InstructionNames.MDRP_16] =
                new HintInstruction(InstructionNames.MDRP_16, "MDRP_16") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xD0 = 1101 0000
                        doMDRP(p, true, false, false, 0, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_17.
        INSTRUCTIONS[InstructionNames.MDRP_17] =
                new HintInstruction(InstructionNames.MDRP_17, "MDRP_17") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xD1 = 1101 0001
                        doMDRP(p, true, false, false, 1, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_18.
        INSTRUCTIONS[InstructionNames.MDRP_18] =
                new HintInstruction(InstructionNames.MDRP_18, "MDRP_18") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xD2 = 1101 0010
                        doMDRP(p, true, false, false, 2, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_19.
        INSTRUCTIONS[InstructionNames.MDRP_19] =
                new HintInstruction(InstructionNames.MDRP_19, "MDRP_19") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xD3 = 1101 0011
                        doMDRP(p, true, false, false, 3, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_20.
        INSTRUCTIONS[InstructionNames.MDRP_20] =
                new HintInstruction(InstructionNames.MDRP_20, "MDRP_20") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xD4 = 1101 0100
                        doMDRP(p, true, false, true, 0, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_21.
        INSTRUCTIONS[InstructionNames.MDRP_21] =
                new HintInstruction(InstructionNames.MDRP_21, "MDRP_21") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xD5 = 1101 0101
                        doMDRP(p, true, false, true, 1, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_22.
        INSTRUCTIONS[InstructionNames.MDRP_22] =
                new HintInstruction(InstructionNames.MDRP_22, "MDRP_22") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xD6 = 1101 0110
                        doMDRP(p, true, false, true, 2, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_23.
        INSTRUCTIONS[InstructionNames.MDRP_23] =
                new HintInstruction(InstructionNames.MDRP_23, "MDRP_23") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xD7 = 1101 0101
                        doMDRP(p, true, false, true, 3, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_24.
        INSTRUCTIONS[InstructionNames.MDRP_24] =
                new HintInstruction(InstructionNames.MDRP_24, "MDRP_24") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xD8 = 1101 1000
                        doMDRP(p, true, true, false, 0, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_25.
        INSTRUCTIONS[InstructionNames.MDRP_25] =
                new HintInstruction(InstructionNames.MDRP_25, "MDRP_25") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xD9 = 1101 1001
                        doMDRP(p, true, true, false, 1, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_26.
        INSTRUCTIONS[InstructionNames.MDRP_26] =
                new HintInstruction(InstructionNames.MDRP_26, "MDRP_26") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xDA = 1101 1010
                        doMDRP(p, true, true, false, 2, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_27.
        INSTRUCTIONS[InstructionNames.MDRP_27] =
                new HintInstruction(InstructionNames.MDRP_27, "MDRP_27") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xDB = 1101 1011
                        doMDRP(p, true, true, false, 3, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_28.
        INSTRUCTIONS[InstructionNames.MDRP_28] =
                new HintInstruction(InstructionNames.MDRP_28, "MDRP_28") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xDC = 1101 1100
                        doMDRP(p, true, true, true, 0, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_29.
        INSTRUCTIONS[InstructionNames.MDRP_29] =
                new HintInstruction(InstructionNames.MDRP_29, "MDRP_29") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xDD = 1101 1101
                        doMDRP(p, true, true, true, 1, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_30.
        INSTRUCTIONS[InstructionNames.MDRP_30] =
                new HintInstruction(InstructionNames.MDRP_30, "MDRP_30") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xDE = 1101 1110
                        doMDRP(p, true, true, true, 2, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        // MDRP_0[abcde] flag interrogation as for MDRP_31.
        INSTRUCTIONS[InstructionNames.MDRP_31] =
                new HintInstruction(InstructionNames.MDRP_31, "MDRP_31") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int p = stack.pop();// p: point number (uint32)
                        // 0xDD = 1101 1111
                        doMDRP(p, true, true, true, 3, glyphDef, graphicsState);
                        return instOffset;
                    }
                };


        // MIAP[a] Move Indirect Absolute Point
        INSTRUCTIONS[InstructionNames.MIAP_0] =
                new HintInstruction(InstructionNames.MIAP_0, "MIAP_0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {

                        // _0 means don't round, and don't use cut in value.
                        int n = stack.pop();// n: CVT entry number (F26Dot6)
                        int p = stack.pop();// p: point number (uint32)

                        int val = gs.getCvtTable().get(n);

                        float xPt = glyphDef.getXPtr(gs.zp0, p);
                        float yPt = glyphDef.getYPtr(gs.zp0, p);

                        int projectedD =
                                getProjectionOnVector(gs.getProjectionVector(), xPt, yPt);

                        int dToMove = val - projectedD;

                        Point2D.Float deltaPoint = getFreedomDistanceForProjectionMove(dToMove, gs);

                        xPt += deltaPoint.x;
                        yPt += deltaPoint.y;
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("MIAP_0 set point: " + p + ", CVT_n: " + n + ", CVT_val: " + val +
                                    ", zones [" + gs.zp0 + "," + gs.zp1 + "," + gs.zp2 + "] to [x,y] = [" + (int) xPt + "," + (int)yPt + "]");
                        }
                        glyphDef.setXPtr(gs.zp0, p, xPt);
                        glyphDef.setYPtr(gs.zp0, p, yPt);
                        if (gs.getFreedomVector().x != 0) {
                            glyphDef.setTouchedXPtr(gs.zp0, p, true);
                        }
                        if (gs.getFreedomVector().y != 0) {
                            glyphDef.setTouchedYPtr(gs.zp0, p, true);
                        }
                        gs.setRp0(p);
                        return instOffset;
                    }
                };
        // MIAP[a] Move Indirect Absolute Point
        INSTRUCTIONS[InstructionNames.MIAP_1] =
                new HintInstruction(InstructionNames.MIAP_1, "MIAP_1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {


                        int n = stack.pop();
                        int p = stack.pop();
                        int val = gs.getCvtTable().get(n);

                        float xPt = glyphDef.getXPtr(gs.zp0, p);
                        float yPt = glyphDef.getYPtr(gs.zp0, p);

                        int k46 = getProjectionOnVector(gs.getProjectionVector(),
                                xPt, yPt);
                        int l50 = val - k46;
                        if (Math.abs(l50) > gs.getControlValueCutIn()) {
                            val = k46;
                        }

                        val = storeDoubleAsF26Dot6(gs.round(getF26Dot6AsDouble(val)));
                        l50 = val - k46;
                        Point2D.Float deltaPt = getFreedomDistanceForProjectionMove(l50, gs);

                        xPt += deltaPt.x;
                        yPt += deltaPt.y;
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("MIAP_1 set point: " + p + ", CVT_n: " + n + ", CVT_val: " + val +
                                    ", zones [" + gs.zp0 + "," + gs.zp1 + "," + gs.zp2 + "] to [x,y] = [" + (int) xPt + "," + yPt + "]");
                        }
                        glyphDef.setXPtr(gs.zp0, p, xPt);
                        glyphDef.setYPtr(gs.zp0, p, yPt);
                        if (gs.getFreedomVector().x != 0) {
                            glyphDef.setTouchedXPtr(gs.zp0, p, true);
                        }
                        if (gs.getFreedomVector().y != 0) {
                            glyphDef.setTouchedYPtr(gs.zp0, p, true);
                        }
                        gs.rp0 = gs.rp1 = p;
                        return instOffset;
                    }
                };
        // MIN[] MINimum of top two stack elements
        INSTRUCTIONS[InstructionNames.MIN] =
                new HintInstruction(InstructionNames.MIN, "MIN") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int e2 = stack.pop(); // e2: stack element
                        int e1 = stack.pop(); // e1: stack element
                        if (e1 < e2) {
                            if (logger.isLoggable(Level.FINEST)) {
                                CallContext.log("MIN - pushing " + e1);
                            }
                            stack.push(e1);
                        } else {
                            if (logger.isLoggable(Level.FINEST)) {
                                CallContext.log("MIN - pushing " + e2);
                            }
                            stack.push(e2);
                        }
                        return instOffset;
                    }
                };
        // MINDEX[] Move the INDEXed element to the top of the stack
        INSTRUCTIONS[InstructionNames.MINDEX] =
                new HintInstruction(InstructionNames.MINDEX, "MINDEX") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int index = stack.pop();
                        stack.push(stack.remove(index));
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("MINDEX - moving element: " + index);
                        }
                        return instOffset;
                    }
                };

        // MIRP[abcde] Move Indirect Relative Point
        INSTRUCTIONS[InstructionNames.MIRP] =
                new HintInstruction(InstructionNames.MIRP, "MIRP_0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop();// n: CVT entry number (F26Dot6)
                        int p = stack.pop();// p: point number (uint32)
                        doMIRP(p, cvtNum, false, false, false, 0, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_1] =
                new HintInstruction(InstructionNames.MIRP_1, "MIRP_1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop();// n: CVT entry number (F26Dot6)
                        int p = stack.pop();// p: point number (uint32)
                        // E1 = 1110 0001
                        doMIRP(p, cvtNum, false, false, false, 1, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_2] =
                new HintInstruction(InstructionNames.MIRP_2, "MIRP_2") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // E2 = 1110 0010
                        doMIRP(p, cvtNum, false, false, false, 2, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_3] =
                new HintInstruction(InstructionNames.MIRP_3, "MIRP_3") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // E3 = 1110 0011
                        doMIRP(p, cvtNum, false, false, false, 3, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_4] =
                new HintInstruction(InstructionNames.MIRP_4, "MIRP_4") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // E4 = 1110 0100
                        doMIRP(p, cvtNum, false, false, true, 0, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_5] =
                new HintInstruction(InstructionNames.MIRP_5, "MIRP_5") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // E5 = 1110 0101
                        doMIRP(p, cvtNum, false, false, true, 1, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_6] =
                new HintInstruction(InstructionNames.MIRP_6, "MIRP_6") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // E6 = 1110 0110
                        doMIRP(p, cvtNum, false, false, true, 2, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_7] =
                new HintInstruction(InstructionNames.MIRP_7, "MIRP_7") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // E7 = 1110 0111
                        doMIRP(p, cvtNum, false, false, true, 3, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_8] =
                new HintInstruction(InstructionNames.MIRP_8, "MIRP_8") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // E8 = 1110 1000
                        doMIRP(p, cvtNum, false, true, false, 0, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_9] =
                new HintInstruction(InstructionNames.MIRP_9, "MIRP_9") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // E9 = 1110 1001
                        doMIRP(p, cvtNum, false, true, false, 1, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_10] =
                new HintInstruction(InstructionNames.MIRP_10, "MIRP_10") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // EA = 1110 1010
                        doMIRP(p, cvtNum, false, true, false, 2, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_11] =
                new HintInstruction(InstructionNames.MIRP_11, "MIRP_11") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // EB = 1110 1011
                        doMIRP(p, cvtNum, false, true, false, 3, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_12] =
                new HintInstruction(InstructionNames.MIRP_12, "MIRP_12") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // EC = 1110 1100
                        doMIRP(p, cvtNum, false, true, true, 0, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_13] =
                new HintInstruction(InstructionNames.MIRP_13, "MIRP_13") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // ED = 1110 1101
                        doMIRP(p, cvtNum, false, true, true, 1, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_14] =
                new HintInstruction(InstructionNames.MIRP_13, "MIRP_14") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // EE = 1110 1110
                        doMIRP(p, cvtNum, false, true, true, 2, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_15] =
                new HintInstruction(InstructionNames.MIRP_15, "MIRP_15") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // EF = 1110 1111
                        doMIRP(p, cvtNum, false, true, true, 1, glyphDef, graphicsState);
                        return instOffset;
                    }
                };

        INSTRUCTIONS[InstructionNames.MIRP_16] =
                new HintInstruction(InstructionNames.MIRP_1, "MIRP_16") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop();// n: CVT entry number (F26Dot6)
                        int p = stack.pop();// p: point number (uint32)
                        // F0 = 1111 0000
                        doMIRP(p, cvtNum, true, false, false, 0, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_17] =
                new HintInstruction(InstructionNames.MIRP_2, "MIRP_17") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop();// n: CVT entry number (F26Dot6)
                        int p = stack.pop();// p: point number (uint32)
                        // F1 = 1111 0001
                        doMIRP(p, cvtNum, true, false, false, 1, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_18] =
                new HintInstruction(InstructionNames.MIRP_3, "MIRP_18") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // F2 = 1110 0010
                        doMIRP(p, cvtNum, true, false, false, 2, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_19] =
                new HintInstruction(InstructionNames.MIRP_4, "MIRP_19") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // F3 = 1111 0011
                        doMIRP(p, cvtNum, true, false, true, 3, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_20] =
                new HintInstruction(InstructionNames.MIRP_5, "MIRP_20") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // F4 = 1111 0100
                        doMIRP(p, cvtNum, true, false, true, 0, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_21] =
                new HintInstruction(InstructionNames.MIRP_6, "MIRP_21") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // F5 = 1111 0101
                        doMIRP(p, cvtNum, true, false, true, 1, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_22] =
                new HintInstruction(InstructionNames.MIRP_7, "MIRP_22") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // F6 = 1111 0110
                        doMIRP(p, cvtNum, true, false, true, 2, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_23] =
                new HintInstruction(InstructionNames.MIRP_8, "MIRP_23") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // F7 = 1111 0111
                        doMIRP(p, cvtNum, true, false, true, 3, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_24] =
                new HintInstruction(InstructionNames.MIRP_24, "MIRP_24") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // F8 = 1111 1000
                        doMIRP(p, cvtNum, true, true, false, 0, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_25] =
                new HintInstruction(InstructionNames.MIRP_25, "MIRP_25") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // F9 = 1111 1001
                        doMIRP(p, cvtNum, true, true, false, 1, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_26] =
                new HintInstruction(InstructionNames.MIRP_26, "MIRP_26") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // FA = 1111 1010
                        doMIRP(p, cvtNum, true, true, false, 2, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_27] =
                new HintInstruction(InstructionNames.MIRP_27, "MIRP_27") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // FB = 1111 1011
                        doMIRP(p, cvtNum, true, true, false, 3, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_28] =
                new HintInstruction(InstructionNames.MIRP_28, "MIRP_28") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // FC = 1111 1100
                        doMIRP(p, cvtNum, true, true, true, 0, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_29] =
                new HintInstruction(InstructionNames.MIRP_29, "MIRP_29") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // FD = 1111 1101
                        doMIRP(p, cvtNum, true, true, true, 1, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_30] =
                new HintInstruction(InstructionNames.MIRP_15, "MIRP_30") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // FE = 1111 1110
                        doMIRP(p, cvtNum, true, true, true, 2, glyphDef, graphicsState);
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.MIRP_31] =
                new HintInstruction(InstructionNames.MIRP_31, "MIRP_31") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int cvtNum = stack.pop(); // n: CVT entry number (F26Dot6)
                        int p = stack.pop();      // p: point number (uint32)
                        // FF = 1111 1111
                        doMIRP(p, cvtNum, true, true, true, 3, glyphDef, graphicsState);
                        return instOffset;
                    }
                };

        // MPPEM[] Measure Pixels Per EM
        INSTRUCTIONS[InstructionNames.MPPEM] =
                new HintInstruction(InstructionNames.MPPEM, "MPPEM") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        stack.push(Interpreter.ppem);
                        if (logger.isLoggable(Level.FINEST)) {
                            CallContext.log("MPPEM - Pushed " + Interpreter.ppem + " to the stack");
                        }
                        return instOffset;
                    }
                };
        // MPS[] Measure Point Size
        INSTRUCTIONS[InstructionNames.MPS] =
                new HintInstruction(InstructionNames.MPS, "MPS") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        stack.push((int) (Interpreter.ptSize * 64D));
                        return instOffset;
                    }
                };
        // MSIRP[a] Move Stack Indirect Relative Point, do not change rp0
        INSTRUCTIONS[InstructionNames.MSIRP_0] =
                new HintInstruction(InstructionNames.MSIRP_0, "MSIRP_0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {

                        int d = stack.pop();// d: distance (F26Dot6)
                        int point = stack.pop();// p: point number (uint32)

                        float xPt = glyphDef.getXPtr(gs.zp1, point);
                        float yPt = glyphDef.getYPtr(gs.zp1, point);

                        int rp0 = gs.getRp0();
                        int xRp = glyphDef.getXPtr(gs.zp0, rp0);
                        int yRp = glyphDef.getYPtr(gs.zp0, rp0);

                        int pointOnVec = getProjectionOnVector(gs.getProjectionVector(), xPt, yPt);
                        int rpOnVec = getProjectionOnVector(gs.getProjectionVector(), xRp, yRp);

                        int dToMove = (d - (pointOnVec - rpOnVec));
                        Point2D.Float deltaPoint = getFreedomDistanceForProjectionMove(dToMove, gs);

                        if (logger.isLoggable(Level.FINEST))
                            CallContext.logContinuationIndent("MSIRP_0 moving point: " + point + ", d: " + d + " from: [x,y] [" + (int) xPt + "," + (int) yPt + "]");

                        xPt += deltaPoint.x;
                        yPt += deltaPoint.y;

                        if (logger.isLoggable(Level.FINEST))
                            CallContext.logContinuation(" to [" + (int) xPt + "," +  (int) yPt + "]");

                        if (gs.getFreedomVector().x != 0) {
                            glyphDef.setTouchedXPtr(gs.zp1, point, true);
                        }
                        if (gs.getFreedomVector().y != 0) {
                            glyphDef.setTouchedYPtr(gs.zp1, point, true);
                        }
                        glyphDef.setXPtr(gs.zp1, point, xPt);
                        glyphDef.setYPtr(gs.zp1, point, yPt);

                        gs.setRp1(gs.getRp0());
                        gs.setRp2(point);

                        return instOffset;
                    }
                };
        // MSIRP[a] Move Stack Indirect Relative Point, set rp0 to point number p
        INSTRUCTIONS[InstructionNames.MSIRP_1] =
                new HintInstruction(InstructionNames.MSIRP_1, "MSIRP_1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int d = stack.pop();// d: distance (F26Dot6)
                        int point = stack.pop();// p: point number (uint32)

                        float xpt = glyphDef.getXPtr(graphicsState.zp1, point);
                        float ypt = glyphDef.getYPtr(graphicsState.zp1, point);

                        int rp0 = graphicsState.getRp0();
                        int rpxZ1 = glyphDef.getXPtr(graphicsState.zp0, rp0);
                        int rpyZ1 = glyphDef.getYPtr(graphicsState.zp0, rp0);

                        Point2D.Float pV = graphicsState.getProjectionVector();

                        int projectedPD = getProjectionOnVector(pV, xpt, ypt);
                        int projectedRPD = getProjectionOnVector(pV, rpxZ1, rpyZ1);

                        int dToMove = (int) (d - (projectedPD - projectedRPD));
                        Point2D.Float deltaPoint = getFreedomDistanceForProjectionMove(dToMove, graphicsState);

                        if (logger.isLoggable(Level.FINEST))
                            CallContext.logContinuationIndent("MSIRP_1 - moving point: " + point + ", d: " + d + " from: [x,y] [" + (int) xpt + "," + (int) ypt + "]");

                        xpt += (int) deltaPoint.x;
                        ypt += (int) deltaPoint.y;

                        if (logger.isLoggable(Level.FINEST))
                            CallContext.logContinuation(" to [" + (int) xpt + "," + (int) ypt + "]");

                        if (graphicsState.getFreedomVector().x != 0) {
                            glyphDef.setTouchedXPtr(graphicsState.zp1, point, true);
                        }
                        if (graphicsState.getFreedomVector().y != 0) {
                            glyphDef.setTouchedYPtr(graphicsState.zp1, point, true);
                        }
                        glyphDef.setXPtr(graphicsState.zp1, point, xpt);
                        glyphDef.setYPtr(graphicsState.zp1, point, ypt);

                        graphicsState.setRp1(graphicsState.getRp0());
                        graphicsState.setRp2(point);
                        graphicsState.setRp0(point);

                        return instOffset;
                    }
                };
        // MUL[] MULtiply
        INSTRUCTIONS[InstructionNames.MUL] =
                new HintInstruction(InstructionNames.MUL, "MUL") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int a = stack.pop();
                        int b = stack.pop();
                        stack.push((a * b) / 64);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("MUL - Pushed: " + (a * b) / 64);
                        return instOffset;
                    }
                };
        // NEG[] NEGate
        INSTRUCTIONS[InstructionNames.NEG] =
                new HintInstruction(InstructionNames.NEG, "NEG") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int val = stack.pop();
                        stack.push(-val);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("NEG - Negate to: " + (-val));
                        return instOffset;
                    }
                };
        // NEQ[] Not EQual
        INSTRUCTIONS[InstructionNames.NEQ] =
                new HintInstruction(InstructionNames.NEQ, "NEQ") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int e2 = stack.pop(); // e2: stack element
                        int e1 = stack.pop(); // e1: stack element
                        if (e1 != e2) {
                            if (logger.isLoggable(Level.FINEST))
                                CallContext.log("NEQ - elements NEQ pushing 1");
                            stack.push(1);
                        } else {
                            if (logger.isLoggable(Level.FINEST))
                                CallContext.log("NEQ - elements EQ pushing 0");
                            stack.push(0);
                        }
                        return instOffset;
                    }
                };
        // NOT[] logical NOT
        INSTRUCTIONS[InstructionNames.NOT] =
                new HintInstruction(InstructionNames.NOT, "NOT") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        boolean e1 = stack.pop() != 0; // e1: stack element
                        if (e1) {
                            stack.push(0);
                        } else {
                            stack.push(1);
                        }
                        return instOffset;
                    }
                };
        // NPUSHB[] PUSH N Bytes
        INSTRUCTIONS[InstructionNames.NPUSHB] =
                new HintInstruction(InstructionNames.NPUSHB, "NPUSHB") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        instOffset++;
                        instOffset = pushBytes(instr[instOffset], instr, instOffset, stack);
                        return instOffset;
                    }
                };
        // NPUSHW[] PUSH N Words
        INSTRUCTIONS[InstructionNames.NPUSHW] =
                new HintInstruction(InstructionNames.NPUSHW, "NPUSHW") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        instOffset++;
                        instOffset = pushWords(instr[instOffset], instr, instOffset, stack);
                        return instOffset;
                    }
                };
        // NROUND[ab] No ROUNDing of value
        INSTRUCTIONS[InstructionNames.NROUND_0] =
                new HintInstruction(InstructionNames.NROUND_0, "NROUND_0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        //
                        int val = stack.pop();
                        double d = getF26Dot6AsDouble(val);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("NROUND_0 Rounding: " + val + " to: " + storeDoubleAsF26Dot6(graphicsState.round(d)));
                        stack.push(storeDoubleAsF26Dot6(graphicsState.round(d)));
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.NROUND_1] = INSTRUCTIONS[InstructionNames.NROUND_0];
        INSTRUCTIONS[InstructionNames.NROUND_2] = INSTRUCTIONS[InstructionNames.NROUND_0];
        INSTRUCTIONS[InstructionNames.NROUND_3] = INSTRUCTIONS[InstructionNames.NROUND_0];
        // ODD[] ODD
        INSTRUCTIONS[InstructionNames.ODD] =
                new HintInstruction(InstructionNames.ODD, "ODD") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int value = stack.pop();
                        int out = value;

                        value = storeDoubleAsF26Dot6(graphicsState.round(getF26Dot6AsFloat(value)));
                        value = (value >> 6) % 2; // truncate to integer and check for remainder
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("ODD - read: " + out + " pushing; " + value);
                        stack.push(value);
                        return instOffset;
                    }
                };
        // OR[] logical OR
        INSTRUCTIONS[InstructionNames.OR] =
                new HintInstruction(InstructionNames.OR, "OR") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        boolean e2 = stack.pop() != 0; // e2: stack element
                        boolean e1 = stack.pop() != 0; // e1: stack element
                        if (e1 || e2) {
                            if (logger.isLoggable(Level.FINEST))
                                CallContext.log("OR - Pushing: " + 1);
                            stack.push(1);
                        } else {
                            if (logger.isLoggable(Level.FINEST))
                                CallContext.log("OR - Pushing: " + 0);
                            stack.push(0);
                        }
                        return instOffset;
                    }
                };
        // POP[] POP top stack element
        INSTRUCTIONS[InstructionNames.POP] =
                new HintInstruction(InstructionNames.POP, "POP") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        stack.pop();
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("POP - removing stack element: " + stack.offset);
                        return instOffset;
                    }
                };
        // PUSHB[abc] PUSH Bytes
        INSTRUCTIONS[InstructionNames.PUSHB_0] =
                new HintInstruction(InstructionNames.PUSHB_0, "PUSHB_0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        return pushBytes(1, instr, instOffset, stack);
                    }
                };
        INSTRUCTIONS[InstructionNames.PUSHB_1] =
                new HintInstruction(InstructionNames.PUSHB_1, "PUSHB_1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        return pushBytes(2, instr, instOffset, stack);
                    }
                };
        INSTRUCTIONS[InstructionNames.PUSHB_2] =
                new HintInstruction(InstructionNames.PUSHB_2, "PUSHB_2") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        return pushBytes(3, instr, instOffset, stack);
                    }
                };
        INSTRUCTIONS[InstructionNames.PUSHB_3] =
                new HintInstruction(InstructionNames.PUSHB_3, "PUSHB_3") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        return pushBytes(4, instr, instOffset, stack);
                    }
                };
        INSTRUCTIONS[InstructionNames.PUSHB_4] =
                new HintInstruction(InstructionNames.PUSHB_4, "PUSHB_4") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        return pushBytes(5, instr, instOffset, stack);
                    }
                };
        INSTRUCTIONS[InstructionNames.PUSHB_5] =
                new HintInstruction(InstructionNames.PUSHB_5, "PUSHB_5") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        return pushBytes(6, instr, instOffset, stack);
                    }
                };
        INSTRUCTIONS[InstructionNames.PUSHB_6] =
                new HintInstruction(InstructionNames.PUSHB_6, "PUSHB_6") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        return pushBytes(7, instr, instOffset, stack);
                    }
                };
        INSTRUCTIONS[InstructionNames.PUSHB_7] =
                new HintInstruction(InstructionNames.PUSHB_7, "PUSHB_7") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        return pushBytes(8, instr, instOffset, stack);
                    }
                };
        // PUSHW[abc] PUSH Words
        INSTRUCTIONS[InstructionNames.PUSHW_0] =
                new HintInstruction(InstructionNames.PUSHW_0, "PUSHW_0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        return pushWords(1, instr, instOffset, stack);
                    }
                };
        INSTRUCTIONS[InstructionNames.PUSHW_1] =
                new HintInstruction(InstructionNames.PUSHW_1, "PUSHW_1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        return pushWords(2, instr, instOffset, stack);
                    }
                };
        INSTRUCTIONS[InstructionNames.PUSHW_2] =
                new HintInstruction(InstructionNames.PUSHW_2, "PUSHW_2") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        return pushWords(3, instr, instOffset, stack);
                    }
                };
        INSTRUCTIONS[InstructionNames.PUSHW_3] =
                new HintInstruction(InstructionNames.PUSHW_3, "PUSHW_3") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        return pushWords(4, instr, instOffset, stack);
                    }
                };
        INSTRUCTIONS[InstructionNames.PUSHW_4] =
                new HintInstruction(InstructionNames.PUSHW_4, "PUSHW_4") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        return pushWords(5, instr, instOffset, stack);
                    }
                };
        INSTRUCTIONS[InstructionNames.PUSHW_5] =
                new HintInstruction(InstructionNames.PUSHW_5, "PUSHW_5") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        return pushWords(6, instr, instOffset, stack);
                    }
                };
        INSTRUCTIONS[InstructionNames.PUSHW_6] =
                new HintInstruction(InstructionNames.PUSHW_6, "PUSHW_6") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        return pushWords(7, instr, instOffset, stack);
                    }
                };
        INSTRUCTIONS[InstructionNames.PUSHW_7] =
                new HintInstruction(InstructionNames.PUSHW_7, "PUSHW_7") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        return pushWords(8, instr, instOffset, stack);
                    }
                };
        // RCVT[] Read Control Value Table entry
        INSTRUCTIONS[InstructionNames.RCVT] =
                new HintInstruction(InstructionNames.RCVT, "RCVT") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {

                        int read = stack.pop();// CVT entry number (uint32)
                        Cvt cvt = graphicsState.getCvtTable();
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("RCVT reading location: " + read + ", value: " + cvt.cvt_[read]);
                        stack.push(cvt.cvt_[read]);
                        return instOffset;
                    }
                };
        // RDTG[] Round Down To Grid
        INSTRUCTIONS[InstructionNames.RDTG] =
                new HintInstruction(InstructionNames.RDTG, "RDTG") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        graphicsState.setRoundState(68);
                        return instOffset;
                    }
                };
        // ROFF[] Round OFF
        INSTRUCTIONS[InstructionNames.ROFF] =
                new HintInstruction(InstructionNames.ROFF, "ROFF") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        graphicsState.setRoundState(-1);
                        return instOffset;
                    }
                };
        // ROLL ROLL the top three stack elements
        INSTRUCTIONS[InstructionNames.ROLL] =
                new HintInstruction(InstructionNames.ROLL, "ROLL") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int a = stack.pop();
                        int b = stack.pop();
                        int c = stack.pop();
                        stack.push(b);
                        stack.push(a);
                        stack.push(c);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("ROLL read: " + a + ", " + b + ", " + c + ", pushed: " +
                                    b + ", " + a + ", " + c);
                        return instOffset;
                    }
                };
        // ROUND[ab] ROUND value
        INSTRUCTIONS[InstructionNames.ROUND_0] =
                new HintInstruction(InstructionNames.ROUND_0, "ROUND_0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int val = stack.pop();
                        double d = getF26Dot6AsFloat(val);
//                        System.out.println("ROUND_0 - rounding: " + val + " to: " + graphicsState.round(d));
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("ROUND_N Rounding: " + val + " (" + d + ") to: " +
                                    storeDoubleAsF26Dot6(graphicsState.round(d)) + " (" + graphicsState.round(d) + ")");
                        stack.push(storeDoubleAsF26Dot6(graphicsState.round(d)));
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.ROUND_1] = INSTRUCTIONS[InstructionNames.ROUND_0];
        INSTRUCTIONS[InstructionNames.ROUND_2] = INSTRUCTIONS[InstructionNames.ROUND_0];
        INSTRUCTIONS[InstructionNames.ROUND_3] = INSTRUCTIONS[InstructionNames.ROUND_0];
        // RS[] Read Store
        INSTRUCTIONS[InstructionNames.RS] =
                new HintInstruction(InstructionNames.RS, "RS") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int offset = stack.pop();
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("RS - offset: " + offset + " value= " + Interpreter.STORAGE[offset]);
                        stack.push(Interpreter.STORAGE[offset]);
                        return instOffset;
                    }
                };
        // RTDG[] Round To Double Grid
        INSTRUCTIONS[InstructionNames.RTDG] =
                new HintInstruction(InstructionNames.RTDG, "RTDG") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        graphicsState.setRoundState(8);
                        graphicsState.setGridPeriod(1.0D);
                        return instOffset;
                    }
                };
        // RTG[] Round To Grid
        INSTRUCTIONS[InstructionNames.RTG] =
                new HintInstruction(InstructionNames.RTG, "RTG") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        graphicsState.setRoundState(72);// 72 dpi
                        graphicsState.setGridPeriod(1.0D);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("RTG - Round to grid (72, 1)");
                        return instOffset;
                    }
                };
        // RTHG[] Round To Half Grid
        INSTRUCTIONS[InstructionNames.RTHG] =
                new HintInstruction(InstructionNames.RTHG, "RTHG") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        graphicsState.setRoundState(104);// 72*2 dpi
                        graphicsState.setGridPeriod(1D);// 72*2 dpi
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("RTHG - Set round state to 1/2 grid");
                        return instOffset;
                    }
                };
        // RUTG[] Round Up To Grid
        INSTRUCTIONS[InstructionNames.RUTG] =
                new HintInstruction(InstructionNames.RUTG, "RUTG") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        graphicsState.setRoundState(64);
                        return instOffset;
                    }
                };
        // S45ROUND[] Super ROUND 45 degrees
        INSTRUCTIONS[InstructionNames.S45ROUND] =
                new HintInstruction(InstructionNames.S45ROUND, "S45ROUND") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        graphicsState.setRoundState(stack.pop());
                        graphicsState.setGridPeriod(0.70710678118654757D);
                        return instOffset;
                    }
                };
        // SANGW[] Set Angle Weight
        INSTRUCTIONS[InstructionNames.SANGW] =
                new HintInstruction(InstructionNames.SANGW, "SANGW") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        stack.pop();
                        return instOffset;
                    }
                };
        // SCANCTRL[] SCAN conversion ConTRoLht
        INSTRUCTIONS[InstructionNames.SCANCTRL] =
                new HintInstruction(InstructionNames.SCANCTRL, "SCANCTRL") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        stack.pop();
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SCANCTRL - Does nothing");
                        return instOffset;
                    }
                };
        // SCANTYPE[] SCANTYPE
        INSTRUCTIONS[InstructionNames.SCANTYPE] =
                new HintInstruction(InstructionNames.SCANTYPE, "SCANTYPE") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        stack.pop();
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SCANTYPE - Does nothing");
                        return instOffset;
                    }
                };
        // SCFS[] Sets Coordinate From the Stack using projection vector and freedom vector
        INSTRUCTIONS[InstructionNames.SCFS] =
                new HintInstruction(InstructionNames.SCFS, "SCFS") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {

                        int dist = stack.pop();
                        int pnt = stack.pop();
                        float xPt = glyphDef.getXPtr(gs.zp2, pnt);
                        float yPt = glyphDef.getYPtr(gs.zp2, pnt);
                        int d2 = getProjectionOnVector(gs.getProjectionVector(), xPt, yPt);

                        int deltaD = dist - d2;
                        Point2D.Float deltaPt = getFreedomDistanceForProjectionMove(deltaD, gs);
                        xPt += deltaPt.x;
                        yPt += deltaPt.y;
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SCFS - Setting point: " + pnt + " to: [" + Math.round(xPt) + ","
                                    + Math.round(yPt) + "]");
                        if (gs.getFreedomVector().x != 0) {
                            glyphDef.setXPtr(gs.zp2, pnt, xPt);
                            glyphDef.setTouchedXPtr(gs.zp2, pnt, true);
                        }
                        if (gs.getFreedomVector().y != 0) {
                            glyphDef.setYPtr(gs.zp2, pnt, yPt);
                            glyphDef.setTouchedYPtr(gs.zp2, pnt, true);
                        }
                        return instOffset;
                    }
                };
        // SCVTCI[] Set Control Value Table Cut-In
        INSTRUCTIONS[InstructionNames.SCVTCI] =
                new HintInstruction(InstructionNames.SCVTCI, "SCVTCI") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int val = stack.pop();
                        graphicsState.setControlValueCutIn(val);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SCVTCI - set control value cutin: " + val);
                        return instOffset;
                    }
                };
        // SDB[] Set Delta Base in the graphics state
        INSTRUCTIONS[InstructionNames.SDB] =
                new HintInstruction(InstructionNames.SDB, "SDB") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int val = stack.pop();
                        graphicsState.setDeltaBase(val);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SDB - set deltabase to: " + val);
                        return instOffset;
                    }
                };
        // SDPVTL[a] Set Dual Projection Vector To Line
        INSTRUCTIONS[InstructionNames.SDPVTL_0] =
                new HintInstruction(InstructionNames.SDPVTL_0, "SDPVTL_0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {

                        int p2 = stack.pop();
                        int p1 = stack.pop();

                        int xp2 = glyphDef.getXPtr(gs.zp2, p2);
                        int yp2 = glyphDef.getYPtr(gs.zp2, p2);

                        int xp1 = glyphDef.getXPtr(2 + gs.zp1, p1);
                        int yp1 = glyphDef.getYPtr(2 + gs.zp1, p1);

                        int originalxP2 = glyphDef.getXPtr(2 + gs.zp2, p2);
                        int originalyP2 = glyphDef.getYPtr(2 + gs.zp2, p2);

                        int originalxP1 = glyphDef.getXPtr(2 + gs.zp1, p1);
                        int originalyp1 = glyphDef.getYPtr(2 + gs.zp1, p1);

                        double da = getF26Dot6AsDouble(xp2 - xp1);
                        double db = getF26Dot6AsDouble(yp2 - yp1);

                        double dc = getF26Dot6AsDouble(originalxP2 - originalxP1);
                        double dd = getF26Dot6AsDouble(originalyP2 - originalyp1);

                        double de = Math.sqrt(da * da + db * db);
                        double df = Math.sqrt(dc * dc + dd * dd);

                        da /= de;
                        db /= de;
                        dc /= df;
                        dd /= df;

                        Point2D.Float pvec = new Point2D.Float((float) da, (float) db);
                        gs.setProjectionVector(pvec);

                        Point2D.Float dpvec = new Point2D.Float((float) dc, (float) dd);
                        gs.setProjectionVector(dpvec);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SDPVTL_0 - set DPV: [x,y]  [" + storeDoubleAsF2Dot14( dpvec.x ) +
                                    "," +  storeDoubleAsF2Dot14( dpvec.y ) + "]");
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.SDPVTL_1] =
                new HintInstruction(InstructionNames.SDPVTL_1, "InstructionNames.SDPVTL_1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {
//                    int p2 = stack.pop();
//                    int p1 = stack.pop();
                        int p2 = stack.pop();
                        int p1 = stack.pop();

                        int xP2 = glyphDef.getXPtr(gs.zp2, p2);
                        int yP2 = glyphDef.getYPtr(gs.zp2, p2);

                        int xP1 = glyphDef.getXPtr(gs.zp1, p1);
                        int yP1 = glyphDef.getYPtr(gs.zp1, p1);

                        int xP2o = glyphDef.getXPtr(2 + gs.zp2, p2);
                        int yP2o = glyphDef.getYPtr(2 + gs.zp2, p2);

                        int xP1o = glyphDef.getXPtr(2 + gs.zp1, p1);
                        int yP1o = glyphDef.getYPtr(2 + gs.zp1, p1);

                        double d9 = getF26Dot6AsDouble(xP2 - xP1);
                        double d15 = getF26Dot6AsDouble(yP2 - yP1);

                        double d22 = getF26Dot6AsDouble(xP2o - xP1o);
                        double d24 = getF26Dot6AsDouble(yP2o - yP1o);
                        double d26 = Math.sqrt(d9 * d9 + d15 * d15);
                        double d28 = Math.sqrt(d22 * d22 + d24 * d24);
                        d9 /= d26;
                        d15 /= d26;
                        d22 /= d28;
                        d24 /= d28;
                        gs.setProjectionVector(new Point2D.Float((float) d15, (float) -d9));
                        gs.setDualProjectionVector(new Point2D.Float((float) d24, (float) -d22));
                        if (logger.isLoggable(Level.FINEST)) {

                            CallContext.logEndln("SDPVTL_1 - set DPV: [x,y]  [" +  storeDoubleAsF2Dot14(d15) +
                                    "," + storeDoubleAsF2Dot14( -d22) + "]");
                            CallContext.logEndln("SDPVTL_1 - set PV: [x,y]  [" + storeDoubleAsF2Dot14(d15) +
                                    "," +  storeDoubleAsF2Dot14(-d9) + "]");

                            CallContext.log("SDPVTL_1 - p1, p2, z1, z2 + "+ p1 + ", " + p2 + ", " + gs.zp1 + ", " + gs.zp2);
                        }
                        return instOffset;
                    }
                };
        // SDS[] Set Delta Shift in the graphics state
        INSTRUCTIONS[InstructionNames.SDS] =
                new HintInstruction(InstructionNames.SDS, "SDS") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int val = stack.pop();
                        graphicsState.setDeltaShift(val);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SDS - set delta shift to: " + val);
                        return instOffset;
                    }
                };
        // SFVFS[] Set Freedom Vector From Stack
        INSTRUCTIONS[InstructionNames.SFVFS] =
                new HintInstruction(InstructionNames.SFVFS, "SFVFS") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        // pop y, x components of the projection vector.
                        float y = getF2Dot14(stack.pop());
                        float x = getF2Dot14(stack.pop());
                        graphicsState.setFreedomVector(new Point2D.Float(x, y));
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SFVFS - set vF from stack (" + x + "," + y + ")");
                        return instOffset;
                    }
                };
        // SFVTCA[0] Set Freedom Vector To Coordinate Axis
        INSTRUCTIONS[InstructionNames.SFVTCA_0] =
                new HintInstruction(InstructionNames.SFVTCA_0, "SFVTCA_0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        graphicsState.setFreedomVector(GraphicsState.Y_AXIS);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SFVTCA - set vF to Y_AXIS");
                        return instOffset;
                    }
                };
        // SFVTCA[1] Set Freedom Vector To Coordinate Axis
        INSTRUCTIONS[InstructionNames.SFVTCA_1] =
                new HintInstruction(InstructionNames.SFVTCA_1, "SFVTCA_1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        graphicsState.setFreedomVector(GraphicsState.X_AXIS);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SFVTCA - set vF to X_AXIS");
                        return instOffset;
                    }
                };
        // SFVTL[0] Set Freedom Vector To Line
        INSTRUCTIONS[InstructionNames.SFVTL_0] =
                new HintInstruction(InstructionNames.SFVTL_0, "SFVTL_0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        // pop the (uint32) point numbers.
                        int pointNumber2 = stack.pop();
                        int pointNumber1 = stack.pop();
                        // get the point values for the zone
                        Point2D.Float p1 = createPoint(glyphDef, graphicsState.getZp1(), pointNumber1);
                        Point2D.Float p2 = createPoint(glyphDef, graphicsState.getZp2(), pointNumber2);
                        // finally create a unit vector based on the points.
                        Point2D.Float unitVector = createUnitVector(p2, p1);
                        graphicsState.setFreedomVector(unitVector);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SFVTL_0 - set vF to line from p1: " + pointNumber2 + " to: " +
                                    pointNumber1 + " (" + unitVector.x + "," + unitVector.y + ")");
                        return instOffset;
                    }
                };
        // SFVTL[1] Set Freedom Vector To Line
        INSTRUCTIONS[InstructionNames.SFVTL_1] =
                new HintInstruction(InstructionNames.SFVTL_1, "SFVTL_1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        // pop the (uint32) point numbers.
                        int pointNumber2 = stack.pop();
                        int pointNumber1 = stack.pop();
                        // get the point values for the zone
                        Point2D.Float p1 = createPoint(glyphDef, graphicsState.getZp1(), pointNumber1);
                        Point2D.Float p2 = createPoint(glyphDef, graphicsState.getZp2(), pointNumber2);
                        // finally create a unit vector based on the points.
                        Point2D.Float unitVector = createUnitVector(p2, p1);
                        // create a unit vector that is perpendicular to unitVector.
                        // so a' = cross[ax, ay] or a = [-y, x]
                        unitVector.x = -unitVector.y;
                        unitVector.y = unitVector.x;
                        graphicsState.setFreedomVector(unitVector);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SFVTL_1 - set vF to line from p1: " + pointNumber1 + " to: " +
                                    pointNumber2 + ", (" + unitVector.x + "," + unitVector.y + ")");
                        return instOffset;
                    }
                };
        // SFVTPV[] Set Freedom Vector To Projection Vector
        INSTRUCTIONS[InstructionNames.SFVTPV] =
                new HintInstruction(InstructionNames.SFVTPV, "SFVTPV") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        graphicsState.setFreedomVector(graphicsState.getProjectionVector());
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SFVTPV - set vF to vP");
                        return instOffset;
                    }
                };
        // SHC[a] SHift Contour using reference point
        INSTRUCTIONS[InstructionNames.SHC_0] =
                new HintInstruction(InstructionNames.SHC_0, "SHC_0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {
                        // use rp2 in zone pointed to by zp1
                        // Use rp1 in zone zp0
                        int contourNumber = stack.pop(); // Contour to be shifted.
                        int[] pointsInContour = getPointsInContour(contourNumber, glyphDef);

                        int xRp = glyphDef.getXPtr(gs.zp1, gs.rp2);
                        int yRp = glyphDef.getYPtr(gs.zp1, gs.rp2);

                        int xRpOrigin = glyphDef.getXPtr(2 + gs.zp1, gs.rp2);
                        int yRpOrigin = glyphDef.getYPtr(2 + gs.zp1, gs.rp2);

                        int rpProj = getProjectionOnVector(gs.getProjectionVector(), xRp, yRp);
                        int rpOrigProj = getProjectionOnVector(gs.getProjectionVector(), xRpOrigin, yRpOrigin);

                        int delta = rpProj - rpOrigProj;
                        Point2D.Float deltaPt = getFreedomDistanceForProjectionMove(delta, gs);

                        float xPt, yPt;
                        for (int pdx = 0; pdx < pointsInContour.length; pdx++) {

                            if (gs.zp1 != gs.zp2 && gs.rp2 != pointsInContour[pdx]) {

                                xPt = glyphDef.getXPtr(gs.zp2, pointsInContour[pdx]);
                                yPt = glyphDef.getYPtr(gs.zp2, pointsInContour[pdx]);
                                xPt += deltaPt.x;
                                yPt += deltaPt.y;
                                glyphDef.setXPtr(gs.zp2, pointsInContour[pdx], xPt);
                                glyphDef.setYPtr(gs.zp2, pointsInContour[pdx], yPt);
                            }
                        }
                        return instOffset;

                    }
                };
        INSTRUCTIONS[InstructionNames.SHC_1] =
                new HintInstruction(InstructionNames.SHC_1, "SHC_1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {

                        // Use rp1 in zone zp0
                        int contourNumber = stack.pop(); // Contour to be shifted.
                        int[] pointsInContour = getPointsInContour(contourNumber, glyphDef);

                        int xRp = glyphDef.getXPtr(gs.zp0, gs.rp1);
                        int yRp = glyphDef.getYPtr(gs.zp0, gs.rp1);

                        int xRpOrigin = glyphDef.getXPtr(2 + gs.zp0, gs.rp1);
                        int yRpOrigin = glyphDef.getYPtr(2 + gs.zp0, gs.rp1);

                        int rpProj = getProjectionOnVector(gs.getProjectionVector(), xRp, yRp);
                        int rpOrigProj = getProjectionOnVector(gs.getProjectionVector(), xRpOrigin, yRpOrigin);

                        int delta = rpProj - rpOrigProj;
                        Point2D.Float deltaPt = getFreedomDistanceForProjectionMove(delta, gs);

                        float xPt, yPt;
                        for (int pdx = 0; pdx < pointsInContour.length; pdx++) {
                            if (gs.zp0 != gs.zp2 && gs.rp1 != pointsInContour[pdx]) {
                                xPt = glyphDef.getXPtr(gs.zp2, pointsInContour[pdx]);
                                yPt = glyphDef.getYPtr(gs.zp2, pointsInContour[pdx]);

                                xPt += deltaPt.x;
                                yPt += deltaPt.y;

                                glyphDef.setXPtr(gs.zp2, pointsInContour[pdx], xPt);
                                glyphDef.setYPtr(gs.zp2, pointsInContour[pdx], yPt);
                            }
                        }
                        return instOffset;
                    }
                };
        // SHP[a] SHift Point using reference point
        INSTRUCTIONS[InstructionNames.SHP_0] =
                new HintInstruction(InstructionNames.SHP_0, "SHP_0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {
                        for (int idx = 0; idx < gs.getLoop(); idx++) {

                            int ptId = stack.pop();

//                    if(ptId > x[ttgraphicsstate.zp2].length || ttgraphicsstate.rp2 > x[ttgraphicsstate.zp1].length)
//                        break;
                            float xPt = glyphDef.getXPtr(gs.zp1, ptId);
                            float yPt = glyphDef.getYPtr(gs.zp1, ptId);

                            int xRp2 = glyphDef.getXPtr(gs.zp1, gs.rp2);
                            int yRp2 = glyphDef.getYPtr(gs.zp1, gs.rp2);

                            int d1 = getProjectionOnVector(gs.getProjectionVector(), xRp2, yRp2);
                            int oxRp2 = glyphDef.getXPtr(2 + gs.zp1, gs.rp2);
                            int oyRp2 = glyphDef.getYPtr(2 + gs.zp1, gs.rp2);
                            int d2 = getProjectionOnVector(gs.getProjectionVector(), oxRp2, oyRp2);
                            int delta = d1 - d2;
                            Point2D.Float deltaPt = getFreedomDistanceForProjectionMove(delta, gs);

                            xPt += deltaPt.x;
                            yPt += deltaPt.y;
                            if (logger.isLoggable(Level.FINEST))
                                CallContext.log("SHP_0 - moved pt: " + ptId + " to " +
                                        Math.round( xPt) + "," + Math.round(yPt) +
                                ", rp2: " + gs.rp2);

                            glyphDef.setXPtr(gs.zp2, ptId, xPt);
                            glyphDef.setYPtr(gs.zp2, ptId, yPt);
                            if (gs.getFreedomVector().x != 0) {
                                glyphDef.setTouchedXPtr(gs.zp2, ptId, true);
                            }
                            if (gs.getFreedomVector().y != 0) {
                                glyphDef.setTouchedYPtr(gs.zp2, ptId, true);
                            }
                        }

                        gs.setLoop(1);
                        return instOffset;
                    }
                };
        // SHP[a] SHift Point using reference point
        INSTRUCTIONS[InstructionNames.SHP_1] =
                new HintInstruction(InstructionNames.SHP_1, "SHP_1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {
                        for (int idx = 0; idx < gs.getLoop(); idx++) {
                            int ptId = stack.pop();

                            int xRp = glyphDef.getXPtr(gs.zp0, gs.rp1);
                            int yRp = glyphDef.getYPtr(gs.zp0, gs.rp1);
                            int disc = getProjectionOnVector(gs.getProjectionVector(), xRp, yRp);

                            int xRpo = glyphDef.getXPtr(2 + gs.zp1, gs.rp1);
                            int yRpo = glyphDef.getYPtr(2 + gs.zp1, gs.rp1);
                            float xPt = glyphDef.getXPtr(gs.zp2, ptId);
                            float yPt = glyphDef.getYPtr(gs.zp2, ptId);
                            int origDisc = getProjectionOnVector(gs.getProjectionVector(), xRpo, yRpo);

                            int delta = disc - origDisc;
                            Point2D.Float deltaPt = getFreedomDistanceForProjectionMove(delta, gs);

                            xPt += deltaPt.x;
                            yPt += deltaPt.y;
                            if (logger.isLoggable(Level.FINEST))
                                CallContext.log("SHP_1 - moved pt: " + ptId + " to " + Math.round( xPt) + "," + Math.round(yPt) +
                                ", rp1: " + gs.rp1);

                            glyphDef.setXPtr(gs.zp2, ptId, xPt);
                            glyphDef.setYPtr(gs.zp2, ptId, yPt);

                            if (gs.getFreedomVector().x != 0) {
                                glyphDef.setTouchedXPtr(gs.zp2, ptId, true);
                            }
                            if (gs.getFreedomVector().y != 0) {
                                glyphDef.setTouchedYPtr(gs.zp2, ptId, true);
                            }
                        }

                        gs.setLoop(1);
                        return instOffset;
                    }
                };
        // SHPIX[] SHift point by a PIXel amount
        INSTRUCTIONS[InstructionNames.SHPIX] =
                new HintInstruction(InstructionNames.SHPIX, "SHPIX") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {

                        int distance = stack.pop();
                        Point2D.Float vec = gs.getFreedomVector();
                        for (int idx = 0; idx < gs.getLoop(); idx++) {

                            int pointNum = stack.pop();
                            float xPtr = glyphDef.getXPtr(gs.zp2, pointNum);
                            float yPtr = glyphDef.getYPtr(gs.zp2, pointNum);

                            xPtr += ((double) distance * vec.x) / 64D;
                            yPtr += ((double) distance * vec.y) / 64D;

                            if (logger.isLoggable(Level.FINEST))
                                CallContext.log("SHPIX - P: " + pointNum + " moved to [x,y] [" + Math.round(xPtr) +
                                        "," + Math.round(yPtr) + "]");
                            if (vec.x != 0) {
                                glyphDef.setTouchedXPtr(gs.zp2, pointNum, true);
                                glyphDef.setXPtr(gs.zp2, pointNum, xPtr);
                            } else if (vec.y != 0) {
                                glyphDef.setTouchedYPtr(gs.zp2, pointNum, true);
                                glyphDef.setYPtr(gs.zp2, pointNum, yPtr);
                            }

                        }
                        gs.setLoop(1);
                        return instOffset;
                    }
                };
        // SHZ[a] SHift Zone using reference point
        INSTRUCTIONS[InstructionNames.SHZ_0] =
                new HintInstruction(InstructionNames.SHZ_0, "SHZ_0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {
                        int zone = stack.pop();
                        int rpProj = getProjectionOnVector(gs.getProjectionVector(), gs.zp1, gs.rp2);
                        int origiRPProj = getProjectionOnVector(gs.getProjectionVector(), 2 + gs.zp1, gs.rp2);

                        int d = rpProj - origiRPProj;
                        Point2D.Float deltaPt = getFreedomDistanceForProjectionMove(d, gs);
                        int glyphCount = glyphDef.getGlyphCount();

                        int pdx = 0;
                        for (int gdx = 0; gdx < glyphCount; gdx++) {

                            int x[] = glyphDef.getXByGlyphId(zone, gdx);
                            int y[] = glyphDef.getYByGlyphId(zone, gdx);
                            for (int idx = 0; idx < x.length; idx++) {

                                if (zone != gs.zp1 && (pdx + idx) != gs.rp2) {
                                    x[idx] += (deltaPt.x + 0.5f);
                                    y[idx] += (deltaPt.y + 0.5f);
                                }
                            }
                            pdx += x.length;
                            glyphDef.setXByGlyphId(zone, gdx, x);
                            glyphDef.setYByGlyphId(zone, gdx, y);
                        }
                        return instOffset;
                    }
                };
        INSTRUCTIONS[InstructionNames.SHZ_1] =
                new HintInstruction(InstructionNames.SHZ_1, "SHZ_1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {
                        int zone = stack.pop();
                        int rpProj = getProjectionOnVector(gs.getProjectionVector(), gs.zp0, gs.rp1);
                        int origiRPProj = getProjectionOnVector(gs.getProjectionVector(), 2 + gs.zp0, gs.rp1);

                        int d = rpProj - origiRPProj;
                        Point2D.Float deltaPt = getFreedomDistanceForProjectionMove(d, gs);
                        int glyphCount = glyphDef.getGlyphCount();

                        int pdx = 0;
                        for (int gdx = 0; gdx < glyphCount; gdx++) {

                            int x[] = glyphDef.getXByGlyphId(zone, gdx);
                            int y[] = glyphDef.getYByGlyphId(zone, gdx);
                            for (int idx = 0; idx < x.length; idx++) {

                                if (zone != gs.zp0 && (pdx + idx) != gs.rp1) {
                                    x[idx] += (deltaPt.x + 0.5f);
                                    y[idx] += (deltaPt.y + 0.5f);
                                }
                            }
                            pdx += x.length;
                            glyphDef.setXByGlyphId(zone, gdx, x);
                            glyphDef.setYByGlyphId(zone, gdx, y);
                        }
                        return instOffset;
                    }
                };
        // SLOOP[] Set LOOP variable
        INSTRUCTIONS[InstructionNames.SLOOP] =
                new HintInstruction(InstructionNames.SLOOP, "SLOOP") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        graphicsState.setLoop(stack.pop());
                        return instOffset;
                    }
                };
        // SMD[] Set Minimum Distance
        INSTRUCTIONS[InstructionNames.SMD] =
                new HintInstruction(InstructionNames.SMD, "SMD") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        graphicsState.setMinimumDistance(stack.pop());
                        return instOffset;
                    }
                };
        // SPVFS[] Set Projection Vector From Stack
        INSTRUCTIONS[InstructionNames.SPVFS] =
                new HintInstruction(InstructionNames.SPVFS, "SPVFS") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        // pop y, x components of the projection vector.
                        float y = getF2Dot14(stack.pop());
                        float x = getF2Dot14(stack.pop());
                        graphicsState.setProjectionVector(new Point2D.Float(x, y));
                        return instOffset;
                    }
                };
        // SPVTCA[a] Set Projection Vector To Coordinate Axis
        INSTRUCTIONS[InstructionNames.SPVTCA_0] =
                new HintInstruction(InstructionNames.SPVTCA_0, "SPVTCA_0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        graphicsState.setProjectionVector(GraphicsState.Y_AXIS);
                        graphicsState.setDualProjectionVector(GraphicsState.Y_AXIS);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SPVTCA_0 - set Projection Vector to y axis");
                        return instOffset;
                    }
                };
        // SPVTCA[a] Set Projection Vector To Coordinate Axis
        INSTRUCTIONS[InstructionNames.SPVTCA_1] =
                new HintInstruction(InstructionNames.SPVTCA_1, "SPVTCA_1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        graphicsState.setProjectionVector(GraphicsState.X_AXIS);
                        graphicsState.setDualProjectionVector(GraphicsState.X_AXIS);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SPVTCA_1 - set Projection Vector to x axis");
                        return instOffset;
                    }
                };
        // SPVTL[a] Set Projection Vector To Line
        INSTRUCTIONS[InstructionNames.SPVTL_0] =
                new HintInstruction(InstructionNames.SPVTL_0, "SPVTL_0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {
                        // pop the (uint32) point numbers.
                        int k = stack.pop();
                        int k21 = stack.pop();

                        int xk21 = glyphDef.getXPtr(gs.zp2, k21);
                        int yk21 = glyphDef.getYPtr(gs.zp2, k21);

                        int xk = glyphDef.getXPtr(gs.zp1, k);
                        int yk = glyphDef.getYPtr(gs.zp1, k);

                        double d4 = getF26Dot6AsDouble(xk21 - xk);
                        double d10 = getF26Dot6AsDouble(yk21 - yk);

                        double d17 = Math.sqrt(d4 * d4 + d10 * d10);
                        d4 /= d17;
                        d10 /= d17;

                        Point2D.Float vec = new Point2D.Float((float) d4, (float) d10);
                        gs.setProjectionVector(vec);
                        gs.setDualProjectionVector(vec);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SPVTL_0 - setProjectionVector to line: " + vec);
                        return instOffset;
                    }
                };
        // SPVTL[a] Set Projection Vector perpendicular To Line
        INSTRUCTIONS[InstructionNames.SPVTL_1] =
                new HintInstruction(InstructionNames.SPVTL_1, "SPVTL_1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState gs) {
                        // pop the (uint32) point numbers.
                        int l = stack.pop();
                        int l21 = stack.pop();

                        int xl21 = glyphDef.getXPtr(gs.zp2, l21);
                        int yl21 = glyphDef.getYPtr(gs.zp2, l21);

                        int xl = glyphDef.getXPtr(gs.zp1, l);
                        int yl = glyphDef.getYPtr(gs.zp1, l);

                        double d5 = getF26Dot6AsDouble(xl21 - xl);
                        double d11 = getF26Dot6AsDouble(yl21 - yl);
                        double d18 = Math.sqrt(d5 * d5 + d11 * d11);
                        d5 /= d18;
                        d11 /= d18;
                        Point2D.Float vec = new Point2D.Float((float) -d11, (float) d5);
                        gs.setProjectionVector(vec);
                        gs.setDualProjectionVector(vec);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SPVTL_1 - setProjectionVector perp to line: " + vec);
                        return instOffset;
                    }
                };
        // SROUND[] Super ROUND
        INSTRUCTIONS[InstructionNames.SROUND] =
                new HintInstruction(InstructionNames.SROUND, "SROUND") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int pop = stack.pop();
                        graphicsState.setRoundState(pop);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SROUND - " + pop);
                        return instOffset;
                    }
                };
        // SRP0[] Set Reference Point 0
        INSTRUCTIONS[InstructionNames.SRP0] =
                new HintInstruction(InstructionNames.SRP0, "SRP0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int val = stack.pop();
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SRP0 - set RP0: " + val);
                        graphicsState.setRp0(val);
                        return instOffset;
                    }
                };
        // SRP1[] Set Reference Point 1
        INSTRUCTIONS[InstructionNames.SRP1] =
                new HintInstruction(InstructionNames.SRP1, "SRP1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int val = stack.pop();
                        graphicsState.setRp1(val);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SRP1 - set RP1: " + val);
                        return instOffset;
                    }
                };
        // SRP2[] Set Reference Point 2
        INSTRUCTIONS[InstructionNames.SRP2] =
                new HintInstruction(InstructionNames.SRP2, "SRP2") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int kp = stack.pop();
                        graphicsState.setRp2(kp);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SRP2 - set RP2: " + kp);
                        return instOffset;
                    }
                };
        // SSW[] Set Single Width
        INSTRUCTIONS[InstructionNames.SSW] =
                new HintInstruction(InstructionNames.SSW, "SSW") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        graphicsState.setSingleWidthValue(stack.pop());
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SRP0 - set SSW");
                        return instOffset;
                    }
                };
        // SSWCI[] Set Single Width Cut-In
        INSTRUCTIONS[InstructionNames.SSWCI] =
                new HintInstruction(InstructionNames.SSWCI, "SSWCI") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        graphicsState.setSingleWidthCutIn(stack.pop());
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SSWCI - set single width cut in");
                        return instOffset;
                    }
                };
        // SUB[] SUBtract
        INSTRUCTIONS[InstructionNames.SUB] =
                new HintInstruction(InstructionNames.SUB, "SUB") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int n2 = stack.pop();
                        int n1 = stack.pop();
                        stack.push(n1 - n2);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SUB - pushed: " + (n1 - n2));
                        return instOffset;
                    }
                };
        // SVTCA[a] Set freedom and projection Vectors To Coordinate Axis
        INSTRUCTIONS[InstructionNames.SVTCA_0] =
                new HintInstruction(InstructionNames.SVTCA_0, "SVTCA_0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        graphicsState.setFreedomVector(GraphicsState.Y_AXIS);
                        graphicsState.setProjectionVector(GraphicsState.Y_AXIS);
                        graphicsState.setDualProjectionVector(GraphicsState.Y_AXIS);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SVTCA_0 - Set Vectors to Y_AXIS");
                        return instOffset;
                    }
                };
        // SVTCA[a] Set freedom and projection Vectors To Coordinate Axis
        INSTRUCTIONS[InstructionNames.SVTCA_1] =
                new HintInstruction(InstructionNames.SVTCA_1, "SVTCA_1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        graphicsState.setFreedomVector(GraphicsState.X_AXIS);
                        graphicsState.setProjectionVector(GraphicsState.X_AXIS);
                        graphicsState.setDualProjectionVector(GraphicsState.X_AXIS);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SVTCA_1 - Set Vectors to X_AXIS");
                        return instOffset;
                    }
                };
        // SWAP[] SWAP the top two elements on the stack
        INSTRUCTIONS[InstructionNames.SWAP] =
                new HintInstruction(InstructionNames.SWAP, "SWAP") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int instruct1 = stack.pop();
                        int instruct2 = stack.pop();
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SWAP - arg1 " + instruct1 + " arg2 " + instruct2);
                        stack.push(instruct1);
                        stack.push(instruct2);
                        return instOffset;
                    }
                };
        // SZP0[] Set Zone Pointer 0
        INSTRUCTIONS[InstructionNames.SZP0] =
                new HintInstruction(InstructionNames.SZP0, "SZP0") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int zone = stack.pop();
                        graphicsState.setZp0(zone);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SZP0 - set ZP0: " + zone);
                        return instOffset;
                    }
                };
        // SZP1[] Set Zone Pointer 1
        INSTRUCTIONS[InstructionNames.SZP1] =
                new HintInstruction(InstructionNames.SZP1, "SZP1") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int zone = stack.pop();
                        graphicsState.setZp1(zone);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SZP1 - set ZP1: " + zone);
                        return instOffset;
                    }
                };
        // SZP2[] Set Zone Pointer 2
        INSTRUCTIONS[InstructionNames.SZP2] =
                new HintInstruction(InstructionNames.SZP2, "SZP2") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int zone = stack.pop();
                        graphicsState.setZp2(zone);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SZP1 - set ZP2: " + zone);
                        return instOffset;
                    }
                };
        // SZPS[] Set Zone PointerS
        INSTRUCTIONS[InstructionNames.SZPS] =
                new HintInstruction(InstructionNames.SZPS, "SZPS") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int point = stack.pop();
                        graphicsState.setZp0(point);
                        graphicsState.setZp1(point);
                        graphicsState.setZp2(point);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("SZPS - setting all ZP to: " + point);
                        return instOffset;
                    }
                };
        // UTP[] UnTouch Point
        INSTRUCTIONS[InstructionNames.UTP] =
                new HintInstruction(InstructionNames.UTP, "UTP") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int pointNumber = stack.pop();
                        Point2D.Float fV = graphicsState.getFreedomVector();
                        if (fV.x != 0f) {
                            glyphDef.setTouchedXPtr(graphicsState.zp0, pointNumber, false);
                        }
                        if (fV.y != 0f) {
                            glyphDef.setTouchedYPtr(graphicsState.zp0, pointNumber, false);
                        }
                        return instOffset;
                    }
                };
        // WCVTF[] Write Control Value Table in Funits
        INSTRUCTIONS[InstructionNames.WCVTF] =
                new HintInstruction(InstructionNames.WCVTF, "WCVTF") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int val = stack.pop(); // n: number in FUnits (uint32)
                        int pos = stack.pop(); // l: control value table location (uint32)
                        Cvt cvt = graphicsState.getCvtTable();
                        cvt.writeFUnits(pos, val);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("WCVTF - writing: " + val + " to pos: " + pos);
                        return instOffset;
                    }
                };
        // WCVTP[] Write Control Value Table in Pixel units
        INSTRUCTIONS[InstructionNames.WCVTP] =
                new HintInstruction(InstructionNames.WCVTP, "WCVTP") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int val = stack.pop(); // n: number in FUnits (uint32)
                        int pos = stack.pop(); // l: control value table location (uint32)
                        graphicsState.getCvtTable().writePixels(pos, val);
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("WCVTP - write: " + val + " to pos: " + pos);
                        return instOffset;
                    }
                };
        // WS[] Write Store
        INSTRUCTIONS[InstructionNames.WS] =
                new HintInstruction(InstructionNames.WS, "WS") {
                    @Override
                    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                                       Stack stack, GraphicsState graphicsState) {
                        int val = stack.pop(); // v: storage area value (uint32)
                        int loc = stack.pop(); // l: storage area location (uint32)
                        if (logger.isLoggable(Level.FINEST))
                            CallContext.log("WS - writing: " + val + " to stor[" + loc + "]");
                        Interpreter.STORAGE[loc] = val;
                        return instOffset;
                    }
                };

    }


//    private static void touchPoint(TrueTypeGlyphData glyphDef,
//                                   GraphicsState graphicsState, int pointNumber, boolean touched) {
//        Point2D.Float freedomVector = graphicsState.getFreedomVector();
//
//        if (freedomVector.equals(GraphicsState.X_AXIS)) {
//            setXPointTouched(glyphDef, graphicsState.getZp0(), pointNumber, touched);
//        } else if (freedomVector.equals(GraphicsState.Y_AXIS)) {
//            setYPointTouched(glyphDef, graphicsState.getZp0(), pointNumber, touched);
//        } else {
//            setXPointTouched(glyphDef, graphicsState.getZp0(), pointNumber, touched);
//            setYPointTouched(glyphDef, graphicsState.getZp0(), pointNumber, touched);
//        }
//    }

    private static Point2D.Float createPoint(TrueTypeGlyphData glyphDef,
                                             int zone, int pointNumber) {
        return new Point2D.Float(
                glyphDef.getXPtr(zone, pointNumber),
                glyphDef.getYPtr(zone, pointNumber));
    }

    private static void setPoint(TrueTypeGlyphData glyphDef, int zone,
                                 int pointNumber, float x, float y) {
        // this doesn't even work! oh yes it does. Heh.
        glyphDef.setXPtr(zone, pointNumber, (int) x);
        glyphDef.setYPtr(zone, pointNumber, (int) y);
    }


    /**
     * @deprecated
     */
//    private static Point2D.Float getDisplacement(Point2D.Float fromUnitVector,
//                                                 Point2D.Float toUnitVector, float distance) {
//        // project the from(x,y) coords on the toUnitVctor
//        Point2D.Float p1 = project( fromUnitVector, toUnitVector );
//
//        float ratio = distance / vectorLength(p1);
//        return new Point2D.Float(fromUnitVector.x * ratio, fromUnitVector.y * ratio);
//    }

    // utility for calculating the distance bteween two points
    private static float distance(Point2D.Float p1, Point2D.Float p2) {
        float x = p2.x - p1.x;
        float y = p2.y - p1.y;
        return (float) Math.sqrt((x * x) + (y * y));
    }

    // length of a Vector defined by p2
    private static float vectorLength(Point2D.Float p2) {
        float x = p2.x;
        float y = p2.y;
        return (float) Math.sqrt((x * x) + (y * y));
    }


//    /**
//     * utility for finding projection of point p on the line defined by p1, p2
//     *
//     * @param p1 point1 on of the line, (0,0) for unit vectors
//     * @param p2 point2 on the line, or the unit vector
//     * @param p  point to project on the line.
//     * @return coordinates of projected point.
//     */
//    private static Point2D.Float project(Point2D.Float p1, Point2D.Float p2, Point2D.Float p) {
//        float m = (p2.y - p1.y) / (p2.x - p1.x);
//        float b = p1.y - (m * p1.x);
//        float x = (m * p.y + p.x - m * b) / (m * m + 1);
//        float y = (m * m * p.y + m * p.x + b) / (m * m + 1);
//        return new Point2D.Float(x, y);
//    }

    /**
     * utility for finding projection of point p on the unit vector p2
     *
     * @param p2 point2 on the line, or the unit vectory
     * @param p  point to project on the line.
     * @return coordinates of projected point.
     * @deprecated use getProjectionOnVector instead
     */
    private static Point2D.Float project(Point2D.Float p2, Point2D.Float p) {
        float m = (p2.y) / (p2.x);
        float x = (m * p.y + p.x) / (m * m + 1);
        float y = (m * m * p.y + m * p.x) / (m * m + 1);
        return new Point2D.Float(x, y);
    }

    // utility for creating a unit vector out of two points, u = u / ||U||
    private static Point2D.Float createUnitVector(Point2D.Float point1, Point2D.Float point2) {
        // create a unit vector out of p1, p2.
        float x = point2.x - point1.x;
        float y = point2.y - point1.y;
        float u = (float) Math.sqrt((x * x) + (y * y));
        return new Point2D.Float(x / u, y / u);
    }


    private static float getF2Dot14(int val) {
        return (val >> 14) + (val & 0x3fff) / 16384f;
    }

    private static int setF2Dot14(float val) {
        return (int) (val * 16384 + 0.5);
    }

    private static float getF26Dot6AsFloat(int val) {
        return ((float) val) / 64;
    }

    private static double getF26Dot6AsDouble(int val) {
        return val / 64D;
    }

    protected static int storeDoubleAsF2Dot14(double d) {
        return (int)(d * 16384D + 0.5D);
    }


    private static int setF26Dot6(float val) {
        return (int) (val * 64 + 0.5);
    }

    protected static int storeDoubleAsF26Dot6(double d) {
        return (int) (d * 64D + 0.5D);
    }

    private static int[] getX(Object[] glypDef) {
        return (int[]) glypDef[0];
    }

    private static int[] getY(Object[] glypDef) {
        return (int[]) glypDef[1];
    }

    private static int[] getEnd(Object[] glypDef) {
        return (int[]) glypDef[2];
    }

    private static byte[] getFlags(Object[] glypDef) {
        return (byte[]) glypDef[3];
    }

    private static int getnOn(Object[] glypDef) {
        return (Integer) glypDef[4];
    }

    /**
     * Retrieve packed vector components in a Point
     *
     * @param i
     * @return
     */
    private static Point2D.Float getVectorComponents(int i) {
        return new Point2D.Float(getF26Dot6AsFloat(i >> 16), getF26Dot6AsFloat((i << 16) >> 16));
    }

    /**
     * Create a packed vector from 2 2.14 numbers in an int
     *
     * @param point Object containing vector x,y coords
     * @return
     */
    private static int createVector(Point2D.Float point) {
        int x = setF26Dot6(point.x);
        int y = setF26Dot6(point.y);
        return ((x & 0xffff) << 16) + (y & 0xffff);
    }

    /**
     * Utility for pushing bytes off the instructions set and onto the stack.
     *
     * @param pushes     number of pushes to make.
     * @param instr      instruction set.
     * @param instOffset current offset/pointer on instruction set
     * @param stack      stack to push bytes onto.
     * @return updated instruction offset for number of pushes.
     */
    public static int pushBytes(int pushes, int[] instr, int instOffset,
                                Stack stack) {
        if (logger.isLoggable(Level.FINEST))
            CallContext.logContinuation("PUSHB " + pushes + " bytes:");
        for (int i = 0; i < pushes; i++) {
            if (logger.isLoggable(Level.FINEST))
                CallContext.logContinuation(" " + (instr[instOffset + i + 1] ));
            stack.push(instr[instOffset + i + 1] );
        }
        // move the offset for the respective push.
//        CallContext.logEndln();
        return instOffset + pushes;
    }

    private static int pushWords(int count, int ai[], int instOffset, Stack stack) {

        if (logger.isLoggable(Level.FINEST))
            CallContext.logContinuation("PUSHW " + count + " words:");
        for (int k = 0; k < count; k++) {

            instOffset++;
            int l;

            int b1 = ai[instOffset];
            instOffset++;
            int b2 = ai[instOffset];
            l = getIntFrom2Uint8(b1, b2);
            if (logger.isLoggable(Level.FINEST))
                CallContext.logContinuation(" " + l);
            stack.push(l);
        }

//        CallContext.logEndln();
        return instOffset;
    }

    private static int getIntFrom2Uint8(int b1, int b2) {
        return (b1 << 8) + b2 + (b1 >> 7 & 1) * 0xffff0000;
    }

    public static void interpolateUntouchedPoints(int axis, TrueTypeGlyphData glyphData, GraphicsState graphicsstate) {

//        return;
//    }
//    public void a(int axis, TrueTypeGlyphData glyphData, GraphicsState graphicsstate) {

        int zone = graphicsstate.getZp2(); // second zone pointer
        int newCoords[];
        int originalCoords[];
        boolean touched[];
        int[] touchedPtIds;
        int glyphCount = glyphData.getGlyphCount();
        int[] endPointsOfContours;

        // Debugging stuff
        int tdx = 0;
        boolean[] tc;

        int overallLoggingIndex = 0;

//        System.out.print("     touched: ");
//        for (int glyphDx = 0; glyphDx < glyphCount; glyphDx ++) {
//
//            if (axis == 49) {
//                tc = glyphData.getXTouchedByGlyphId(1, glyphDx);
//            } else {
//                tc = glyphData.getYTouchedByGlyphId(1, glyphDx);
//            }
//
//            for (int idx = 0; idx < tc.length; idx++) {
//                if (tc[idx]) {
//                    System.out.print(" " + tdx);
//                }
//                tdx++;
//            }
//        }
//        System.out.println();

        for (int glyphDx = 0; glyphDx < glyphCount; glyphDx++) {

            if (axis == 49) {
                newCoords = glyphData.getXByGlyphId(zone, glyphDx);
                originalCoords = glyphData.getXByGlyphId(3, glyphDx); // untouched originals.
                touched = glyphData.getXTouchedByGlyphId(zone, glyphDx);

            } else {
                newCoords = glyphData.getYByGlyphId(zone, glyphDx);
                originalCoords = glyphData.getYByGlyphId(3, glyphDx); // untouched originals.
                touched = glyphData.getYTouchedByGlyphId(zone, glyphDx);
            }

            // Here
            endPointsOfContours = glyphData.getEndPtsOfContours(glyphDx); // values can range from 0 - points in glyph

            int numPts = touched.length;


            if (endPointsOfContours.length == 0) {
                // go to the next glyph
                continue;
            }

            int touchedPointsInContour;
            int pointsInContour;
            int endPtIdx = 0;

            nextContour:
            for (int contourStartIndex = 0; contourStartIndex < touched.length; contourStartIndex += pointsInContour) {
                touchedPtIds = new int[numPts];
                touchedPointsInContour = 0;
                pointsInContour = 0;
                int endPoint = endPointsOfContours[endPtIdx++];

                do {

                    if (touched[contourStartIndex + pointsInContour]) {
                        touchedPtIds[touchedPointsInContour++] = contourStartIndex + pointsInContour;
                    }
                    pointsInContour++;

                } while (((contourStartIndex + pointsInContour) <= endPoint)
                        && (contourStartIndex + pointsInContour < touched.length));


                // Handle the interpretation of the points

                // Do nothing case
                if (touchedPointsInContour == 0) {
                    continue;
                }
                // one point touched case
                if (touchedPointsInContour == 1) {

                    int deltaD = newCoords[touchedPtIds[0]] - originalCoords[touchedPtIds[0]];
                    int idx = contourStartIndex;

                    do {
                        if (idx >= contourStartIndex + pointsInContour) {
                            continue nextContour;
                        }
                        if (!touched[idx]) {
                            newCoords[idx] += deltaD;
                        }
                        idx++;
                    } while (true);
                }

                int spos, epos;
                for (int idx = 0; idx < touchedPointsInContour; idx++) {
                    if (idx + 1 >= touchedPointsInContour) {  // if we're at the end

                        interpolateRange(touchedPtIds[idx] + 1,  // do after last touched point to end of contour
                                pointsInContour - 1,
                                touchedPtIds[idx],
                                touchedPtIds[0],
                                newCoords,
                                originalCoords);

                        interpolateRange(contourStartIndex, touchedPtIds[0] - 1, // do from start of contour to first touched point
                                touchedPtIds[idx],
                                touchedPtIds[0],
                                newCoords,
                                originalCoords);
                    } else {
                        spos = touchedPtIds[idx] + 1;
                        epos = touchedPtIds[idx + 1] - 1;
                        interpolateRange(touchedPtIds[idx] + 1,
                                touchedPtIds[idx + 1] - 1,
                                touchedPtIds[idx],
                                touchedPtIds[idx + 1],
                                newCoords, originalCoords);
                    }
                }
                // If we reached here, there have been changes
            }

            if (axis == 49) {
                glyphData.setXByGlyphId(zone, glyphDx, newCoords);
            } else {
                glyphData.setYByGlyphId(zone, glyphDx, newCoords);
            }
        }
    }

    /**
     * Interpolate a range of points. It helps to think of the range from left - right
     *
     * @param firstInclPt the index of the first point inside the interpolative range
     * @param lastInclPt  the index of the last point inside the iterpolative range
     * @param lowBoundPt  the index of the point acting as interpolative anchor on low side
     * @param highBoundPt index of the point acting as interpolative anchor on high side.
     * @param newCoords   array of point coordinates as they currently are (after any adjustments)
     *                    Can be x or y
     * @param oldCoords   array of point coords as they were read from font file
     *                    can be x or y
     */
    private static void interpolateRange(int firstInclPt, int lastInclPt,
                                         int lowBoundPt, int highBoundPt,
                                         int newCoords[], int oldCoords[]) {

//        CallContext.log("  Interpolating from: " + firstInclPt +
//                            " to " + lastInclPt + " using: " +
//                            lowBoundPt + " and " + highBoundPt);
        int leftLimitDx;
        int rightLimitDx;
        if (oldCoords[highBoundPt] < oldCoords[lowBoundPt]) {
            leftLimitDx = highBoundPt;
            rightLimitDx = lowBoundPt;
        } else {
            leftLimitDx = lowBoundPt;
            rightLimitDx = highBoundPt;
        }
        for (int pDx = firstInclPt; pDx <= lastInclPt; pDx++) {

            if (oldCoords[pDx] < oldCoords[leftLimitDx]) {     // Outside left interpolation point, do translation
                newCoords[pDx] += newCoords[leftLimitDx] - oldCoords[leftLimitDx];
//                System.out.println("     Adjusted " + pDx + " by: " + (newCoords[leftLimitDx] - oldCoords[leftLimitDx]) + " to " + newCoords[pDx]);
                continue;
            }
            if (oldCoords[pDx] > oldCoords[rightLimitDx]) {   // Outside right interpolation point, do translation
                newCoords[pDx] += newCoords[rightLimitDx] - oldCoords[rightLimitDx];
//                System.out.println("     Adjusted "+ pDx + " by: " + ( newCoords[rightLimitDx] - oldCoords[rightLimitDx] ) + " to " + newCoords[pDx]);
            } else {   // between left/right limits, do linear interpolation ratio nonsense.
                double d = (double) (oldCoords[pDx] - oldCoords[leftLimitDx]) / (double) (oldCoords[rightLimitDx] - oldCoords[leftLimitDx]);

                newCoords[pDx] = newCoords[leftLimitDx] + (int) (d * (double) (newCoords[rightLimitDx] - newCoords[leftLimitDx]));
//                System.out.println("     Interpolated "+ pDx + " by: " + (  (int)(d * (double)(newCoords[rightLimitDx] - newCoords[leftLimitDx]) )) + " to " +
//                                       newCoords[pDx]);
            }
        }
    }

    /**
     * Find the distance to move along the freedom vector that equates to a
     * move of 'd' along the Projection vector. This assumes the projection vector
     * is only along one axis or the other and the resulting point will have either
     * the x or y value filled in.
     *
     * @param d length to move along Pv in 26.6
     * @return A Point2D object with one field or the other filled in.
     */
    public static Point2D.Float getFreedomDistanceForProjectionMove(int d, GraphicsState gs) {

        Point2D.Float Vf = gs.getFreedomVector();
        Point2D.Float Vp = gs.getProjectionVector();

        int projection = getProjectionOnVector(Vp, Vf.x, Vf.y);
        Point2D.Float returnVal = new Point2D.Float();

        if (projection != 0) {
            returnVal.x = ((Vf.x * d) / projection);
            returnVal.y = ((Vf.y * d) / projection);
        }
        return returnVal;
    }

    /**
     * If vector is a unit vector, then this function returns
     * the projection of the point on the Vector, x = V.cos(theta), y = V.sin(theta)
     *
     * @param vector The Vector to project on
     * @param x      x coord of point (or another vector)
     * @param y      y coord of point (or another vector)
     * @return A new Point2D.Float containing projected values.
     */
    public static int getProjectionOnVector(Point2D.Float vector, float x, float y) {
        return Math.round ( (vector.x * x) + (vector.y * y));
    }


    /**
     * Adapter
     *
     * @param point
     * @param vec
     * @param p
     * @return
     * @deprecated use getProjectionOnVector instead
     */
    public static int project(Point2D.Float point, Point2D.Float vec, Point2D.Float p) {
        return getProjectionOnVector(vec, p.x, p.y);
    }

    public static void doMIRP(int pointNumber, int cvtNum, boolean setRP0, boolean keepMinDx, boolean roundAndCutin,
                              int engineComp, TrueTypeGlyphData glyphData, GraphicsState gs) {


        int xOrigin = glyphData.getXPtr(2 + gs.zp1, pointNumber);
        int yOrigin = glyphData.getYPtr(2 + gs.zp1, pointNumber);

        int pOnVec =
                getProjectionOnVector(gs.getDualProjectionVector(),
                        xOrigin, yOrigin);

        int xRp = glyphData.getXPtr(2 + gs.zp0, gs.rp0);
        int yRp = glyphData.getYPtr(2 + gs.zp0, gs.rp0);
        int rpOnVec =
                getProjectionOnVector(gs.getDualProjectionVector(), xRp, yRp);

        int projectedPointD =  pOnVec - rpOnVec;

        int i65 = getProjectionOnVector(gs.getDualProjectionVector(), (xOrigin-xRp), (yOrigin-yRp));
//        int i64 = getMagnitude(i65);


        if (logger.isLoggable(Level.FINEST))
            CallContext.logEndln("MIRP - GOING IN: point: " + pointNumber + " x,y = [" +
                    xOrigin + "," +
                    yOrigin + "]");
        if (logger.isLoggable(Level.FINEST))
            CallContext.logEndln("MIRP - GOING IN: rp0: " + gs.rp0 + " x,y = [" +
                    xRp + "," +
                    yRp + "]");


//        int projectedPointD = getMagnitude(pOnVec) - getMagnitude(rpOnVec);

        if (Math.abs(projectedPointD - gs.getSingleWidthValue()) < gs.getSingleWidthCutIn()) {
            projectedPointD = gs.getSingleWidthValue();
        }
        int cvtVal = gs.getCvtTable().get(cvtNum);
        if (logger.isLoggable(Level.FINEST))
            CallContext.logEndln("MIRP - GOING IN: CVT: " + cvtNum + " value: " + cvtVal);

        if (roundAndCutin) {
            if (gs.isAutoFlip() && (projectedPointD < 0 && cvtVal > 0 || projectedPointD > 0 && cvtVal < 0)) {
                cvtVal = -cvtVal;
            }
            if (Math.abs(projectedPointD - cvtVal) < gs.getControlValueCutIn()) {
                projectedPointD = cvtVal;
            }
        }
        projectedPointD = engineCompensation(projectedPointD, engineComp);

        if (roundAndCutin) {
            projectedPointD = round(projectedPointD, gs);
        }
        if (keepMinDx && Math.abs(projectedPointD) < gs.getMinimumDistance()) {
            if (projectedPointD > 0) {
                projectedPointD = gs.getMinimumDistance();
            } else {
                projectedPointD = -gs.getMinimumDistance();
            }
        }

        int i63 = getProjectionOnVector(gs.getProjectionVector(),
                glyphData.getXPtr(gs.zp0, gs.rp0),
                glyphData.getYPtr(gs.zp0, gs.rp0)) + projectedPointD;

        float xPt = glyphData.getXPtr(gs.zp1, pointNumber);
        float yPt = glyphData.getYPtr(gs.zp1, pointNumber);
        int j64 = i63 - getProjectionOnVector(
                gs.getProjectionVector(),
                glyphData.getXPtr(gs.zp1, pointNumber),
                glyphData.getYPtr(gs.zp1, pointNumber));

        Point2D.Float deltaVec = getFreedomDistanceForProjectionMove(j64, gs);
        if (logger.isLoggable(Level.FINEST))
            CallContext.log("MIRP - dX,dY = [" + (int) deltaVec.x + "," + (int) deltaVec.y + "]");

        xPt += deltaVec.x;
        yPt += deltaVec.y;

        glyphData.setXPtr(gs.zp1, pointNumber, xPt);
        glyphData.setYPtr(gs.zp1, pointNumber, yPt);

        if (gs.getFreedomVector().x != 0) {
            glyphData.setTouchedXPtr(gs.zp1, pointNumber, true);
        }
        if (gs.getFreedomVector().y != 0) {
            glyphData.setTouchedYPtr(gs.zp1, pointNumber, true);
        }

        gs.setRp1(gs.getRp0());
        gs.rp2 = pointNumber;
        if (setRP0) {
            gs.setRp0(pointNumber);
        }
    }

    /**
     * Implement the "Move Direct Relative Point"  algorithm passing in the varying
     * flags for all 31 implementations
     *
     * @param pointNumber
     * @param setRP0
     * @param keepMinDx
     * @param doRounding
     * @param engineComp
     * @param glyphData
     * @param gs
     */
    public static void doMDRP(int pointNumber, boolean setRP0, boolean keepMinDx, boolean doRounding,
                              int engineComp, TrueTypeGlyphData glyphData, GraphicsState gs) {


        int xOriginal = glyphData.getXPtr(2 + gs.zp1, pointNumber);
        int yOriginal = glyphData.getYPtr(2 + gs.zp1, pointNumber);

        float xPt = glyphData.getXPtr(gs.zp1, pointNumber);
        float yPt = glyphData.getYPtr(gs.zp1, pointNumber);

        int pOnVec =
                getProjectionOnVector(gs.getDualProjectionVector(),
                        xOriginal, yOriginal);

        int xRp = glyphData.getXPtr(2 + gs.zp0, gs.rp0);
        int yRp = glyphData.getYPtr(2 + gs.zp0, gs.rp0);
        if (logger.isLoggable(Level.FINEST))
            CallContext.logEndln("MDRP - GOING IN: point: " + pointNumber + " x,y = [" +
                    xOriginal + "," +
                    yOriginal + "]");
        if (logger.isLoggable(Level.FINEST))
            CallContext.logEndln("MDRP - GOING IN: rp0: " + gs.rp0 + " x,y = [" +
                    xRp + "," +
                    yRp + "]");


        int rpOnVec =
                getProjectionOnVector(gs.getDualProjectionVector(),
                        xRp, yRp);

        int projectedPointD = pOnVec - rpOnVec;  // k59

        if ( Math.abs(projectedPointD) < gs.getSingleWidthCutIn() ) {
            if (projectedPointD > 0) {
                projectedPointD = gs.getSingleWidthValue();
            } else {
                projectedPointD = -gs.getSingleWidthValue();
            }
        }

        projectedPointD = engineCompensation(projectedPointD, engineComp);

        if (doRounding) {
            projectedPointD = round(projectedPointD, gs);
        }

        if (keepMinDx && Math.abs(projectedPointD) < gs.getMinimumDistance()) {
            if (projectedPointD < 0) {
                projectedPointD = -gs.getMinimumDistance();
            } else {
                projectedPointD = gs.getMinimumDistance();
            }
        }

        int i63 = getProjectionOnVector(gs.getProjectionVector(),
                glyphData.getXPtr(gs.zp0, gs.rp0),
                glyphData.getYPtr(gs.zp0, gs.rp0)) + projectedPointD;


        int j64 = i63 - getProjectionOnVector(
                gs.getProjectionVector(),
                glyphData.getXPtr(gs.zp1, pointNumber),
                glyphData.getYPtr(gs.zp1, pointNumber));

        Point2D.Float deltaVec = getFreedomDistanceForProjectionMove(j64, gs);
        if (logger.isLoggable(Level.FINEST))
            CallContext.log("MDRP - dX,dY = [" + (int) deltaVec.x + "," + (int) deltaVec.y + "]");
        xPt += deltaVec.x;
        yPt += deltaVec.y;

        glyphData.setXPtr(gs.zp1, pointNumber, xPt);
        glyphData.setYPtr(gs.zp1, pointNumber, yPt);

        if (gs.getFreedomVector().x != 0) {
            glyphData.setTouchedXPtr(gs.zp1, pointNumber, true);
        }
        if (gs.getFreedomVector().y != 0) {
            glyphData.setTouchedYPtr(gs.zp1, pointNumber, true);
        }

        // after point p is moved, rp1 is set equal to rp0, rp2 is
        // set equal to point number p; if the a flag is set to
        // TRUE, rp0 is set equal to point number p

        gs.setRp1(gs.getRp0());
        gs.rp2 = pointNumber;
        if (setRP0) {
            gs.setRp0(pointNumber);
        }
    }

    private static int engineCompensation(int val, int j) {
        return val;
    }

    public static void debugPriors(TrueTypeGlyphData data) {
        return;
    }

    /**
     * Method to find the 'n'th contour defined in all the contours defined by
     * the possible glyphs in a simple or composite glyph.
     *
     * @param contour The contour number.
     * @return an array of the point numbers in that contour
     */
    private static int[] getPointsInContour(int contour, TrueTypeGlyphData glyphData) {

           int[] endPointArray = glyphData.getGlyphEndArray();
           int spos = 0;
           int epos;
           for (int cdx = 0; cdx < endPointArray.length; cdx ++) {
               epos = endPointArray[cdx];
               if (cdx == contour) {
                   int returnVal[] = new int[(epos - spos) + 1];
                   for (int idx = 0; idx <= (epos-spos); idx++) {
                       returnVal[idx] = spos + idx;
                   }
                   return returnVal;
               }
               spos = epos+1;
               if (cdx + 1 == endPointArray.length) {
                   break;
               }
               epos = endPointArray[cdx+1];
           }
           return new int[0];
       }

    // todo: move to graphicsState class
    private static int round(int i, GraphicsState gs) {
        double d = getF26Dot6AsDouble(i);
        d = gs.round(d);
        return storeDoubleAsF26Dot6(d);
    }


//         int overallPointIdx = 0;
//         System.out.println("Prior to hinting");
//        System.out.println("\t\tp.n.\tNew X\t\tNew Y\t\tx tch\t\ty tch");
//        for (int cdx = 0; cdx < data.getGlyphCount(); cdx ++) {
//            int[] oxCoords = data.getXByGlyphId(1, cdx);
//            int[] oyCoords = data.getYByGlyphId(1, cdx);
//            boolean[] xtouched = data.getXTouchedByGlyphId(1, cdx);
//            boolean[] ytouched = data.getYTouchedByGlyphId(1, cdx);
//
//            byte[] flags = data.getFlagsByGlyphId(cdx);
//            for (int idx = 0; idx < oxCoords.length; idx++) {
//                System.out.println("\t\t"+(overallPointIdx++)+"\t\t"+oxCoords[idx]+"\t\t"+oyCoords[idx]+
//                                "\t\t"+xtouched[idx]+"\t\t"+ytouched[idx]);
//            }
//        }
//    }
}