package org.icepdf.core.pobjects.fonts.nfont.instructions;

import org.icepdf.core.pobjects.fonts.nfont.TrueTypeGlyphData;
import org.icepdf.core.pobjects.fonts.nfont.lang.Stack;


/**
 * TrueType Font instruction
 */
public interface Instruction {

    public int execute(TrueTypeGlyphData glyphDef, int[] instr, int instOffset,
                       Stack stack, GraphicsState graphicsState);

}
