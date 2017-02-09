package org.icepdf.core.pobjects.fonts.nfont.instructions;

import org.icepdf.core.pobjects.fonts.nfont.TrueTypeGlyphData;
import org.icepdf.core.pobjects.fonts.nfont.lang.Stack;

/**
 * HintInstructions contain state as to which operand code is being executed.
 * Some instructions don't need to keep this information just the execute
 * command function.
 *
 * @since 4.5
 */
public abstract class HintInstruction implements Instruction {

    protected int operand;
    protected String name;

    protected HintInstruction(int operand, String name) {
        this.operand = operand;
        this.name = name;
    }

    @Override
    public String toString() {
             return   "opcode: " + Integer.toHexString(operand) +
                ", name='" + name + '\'';
    }

    public abstract int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset, Stack stack, GraphicsState graphicsState);
}
