package org.icepdf.core.pobjects.fonts.nfont.io;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;


/**
 * Utilities for {@link java.io.InputStream}s.
 */
public class InputStreams {
    /**
     * Reads exactly <var>len</var> bytes from this file into the byte array.
     *
     * @throws EOFException if this file reaches the end before reading all the bytes.
     */
    public static void readFully(InputStream in, byte[] b, int off, int len) throws IOException {
        for (int hunk = 0; len > 0; off += hunk, len -= hunk) {
            hunk = in.read(b, off, len);    // read([]) can return less than full length
            if (hunk == -1) throw new EOFException();
        }
    }

    public static void readFully(InputStream in, byte[] b) throws IOException {
        readFully(in, b, 0, b.length);
    }

    /**
     * Reads the rest of <var>in</var> and returns contents.
     */
    public static byte[] toByteArray(InputStream in) throws IOException {
        return toByteArray(in, 10 * 1024);
    }

    public static byte[] toByteArray(InputStream in, long estlength) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream((int) estlength);
        byte[] buf = new byte[8 * 1024];
        for (int hunk; (hunk = in.read(buf)) != -1; ) bout.write(buf, 0, hunk);
        in.close();
        return bout.toByteArray();
    }

}
