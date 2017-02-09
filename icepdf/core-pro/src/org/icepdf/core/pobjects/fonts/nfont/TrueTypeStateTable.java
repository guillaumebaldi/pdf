package org.icepdf.core.pobjects.fonts.nfont;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * => only used in AAT, which is not fully documented and not supported
 */
/*package-private*/
class TrueTypeStateTable {

    private static final Logger log =
            Logger.getLogger(TrueTypeStateTable.class.toString());

    public static int HEADER_LENGTH = 2 * 4;
    public static int STATE_START_OF_TEXT = 0, STATE_START_OF_LINE = 1;

    private int firstGlyph_;
    private byte[] classArray_;    // 0=end of text, 1=out of bounds, 2=deleted glyph, 3=end of line, 4-up user defined


    public TrueTypeStateTable(NFontSfnt s/*ource*/, int offset0) {
        int off = offset0;//0 + HEADER_LENGTH;
        int stateSize = s.readUint16();// & 0xff;	// top byte for alignment
        int classTable = s.readUint16() + off, stateArray = s.readUint16() + off, entryTable = s.readUint16() + off;
        if (log.isLoggable(Level.FINER)) {
            log.finer("state table " + stateSize + ", " + classTable + " " + stateArray + " " + entryTable + " @ " + offset0);
        }

        if (false)
            for (int i = s.data_.length - 4 - 1; i > 0; i--) {
                s.setOffset(i);
                int f = s.readUint16(), n = s.readUint16();
                if (Math.abs(f + n - s.data_.length) < 4000 && n < 700) {//System.out.print("*** ");
                    if (log.isLoggable(Level.FINER)) {
                        log.finer("@ " + i + " " + f + "..+" + n + " = " + (f + n) + "  vs  " + s.data_.length);
                    }
                }
            }

        s.setOffset(classTable - 10);
        int lookAhead;
        for (int i = 0; i < 10; i++) {
            lookAhead = s.readUint8();
            if (log.isLoggable(Level.FINER)) {
                log.finer(" " + lookAhead);
            }
        }
        log.finer(" ... ");

        for (int i = 0; i < 100; i++) {
            lookAhead = s.readUint8();
            if (log.isLoggable(Level.FINER)) {
                log.finer(" " + lookAhead);
            }
        }
        log.finer("");
        s.setOffset(classTable);
        firstGlyph_ = s.readUint16();
        int nGlyphs = s.readUint16();
        if (log.isLoggable(Level.FINER)) {
            log.finer("classes, " + firstGlyph_ + "/" + Integer.toHexString(firstGlyph_) + "..+" + nGlyphs);
        }
        classArray_ = new byte[nGlyphs];
        System.arraycopy(s.data_, s.getOffset(), classArray_, 0, nGlyphs); //s.offset_ += nGlyphs;

        if (true) return;


        s.setOffset(stateArray);
        if (log.isLoggable(Level.FINER)) {
            log.finer("state, stateSize=" + stateSize + ", length=" + (entryTable - stateArray));
        }
        for (int i = 0, imax = entryTable - stateArray; i < imax; i++) {
            if (log.isLoggable(Level.FINER)) {
                log.finer(" " + lookAhead);
            }
        }
        log.finer("");
        // byte[] stateSize *


        s.setOffset(entryTable);
    }

}
