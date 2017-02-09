package org.icepdf.core.pobjects.fonts.nfont;

/**
 *
 */
public class CallContext {


    public static  String indent_ = "                                                                             ";
    public static  int mIndent;

    public static final boolean enabled = false;

    public static void down() {
        mIndent += 2;
    }

    public static void up() {
        mIndent -= 2;
        if (mIndent < 0) {
            throw new IllegalStateException("Negative step depth" );
        }
    }

    /**
     * Log a message with indent on a single line
     * @param message
     */
    public static void log( String message ) {
        if (enabled)
        System.out.print( indent_.substring(0, mIndent) + message);
    }

     /**
     * Log a message with indent on a single line
     * @param message
     */
    public static void logEndln( String message ) {
        if (enabled)
        System.out.println( indent_.substring(0, mIndent) + message);
    }

    /**
     * Log the start of a message with indent, but leave for continuation
     * @param message
     */
    public static void logContinuationIndent(String message) {
        if (enabled)
        System.out.print( indent_.substring(0, mIndent) + message);
    }

    /**
     * Log a continuation message without indent, leaving the line available
     * @param message
     */
    public static void logContinuation(String message) {
        if (enabled)
        System.out.print(message);
    }

    /**
     * Log the last portion of a message, without indent, and adding <cr>
     * @param message
     */
    public static void logContinuationEndln(String message) {
        if (enabled) {
            System.out.println(message);
        }
    }

    public static void logEndln() {
        if (enabled) {
            System.out.println();
        }
    }
}
