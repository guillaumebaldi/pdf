package org.icepdf.core.pobjects.fonts.nfont.io;

import java.io.IOException;


/**
 * File-like objects that support random access.
 * <p/>
 * <p>Potentially conflicts with {@link RandomAccess} (which marks {@link java.util.List} as random access), but seldom in practice.
 *
 * @author Copyright (c) 2003-2005  Thomas A. Phelps.  All rights reserved.
 * @version $Revision: 1.1 $ $Date: 2005/07/12 21:33:19 $
 */
public interface RandomAccess {
    /**
     * Reads always return EOF, writes thrown away.
     */
    public static final RandomAccess DEVNULL = new RandomAccess() {
        public void seek(long pos) {
        }

        public long getFilePointer() {
            return 0L;
        }

        public long length() {
            return 0L;
        }

        public int skipBytes(int n) {
            return 0;
        }

        public int read() {
            return -1;
        }

        public int read(byte[] b, int off, int len) {
            return -1;
        }

        public int read(byte[] b) {
            return -1;
        }

        public void readFully(byte[] b) {
        }

        public void readFully(byte[] b, int off, int len) {
        }

        public void write(byte[] b) {
        }

        public void write(byte[] b, int off, int len) {
        }

        public void write(int b) {
        }

        public void writeString8(String s) {
        }

        public void writeString16(String s) {
        }

        public void close() {
        }

        public void slice(long start, long length) {
        }
    };


    void seek(long pos) throws IOException;    // and/or mark/reset

    long getFilePointer() throws IOException;

    long length() throws IOException;

    int skipBytes(int n) throws IOException;

    int read() throws IOException;

    int read(byte[] b, int off, int len) throws IOException;

    int read(byte[] b) throws IOException;

    void readFully(byte[] b) throws IOException;

    void readFully(byte[] b, int off, int len) throws IOException;

    void write(byte[] b) throws IOException;

    void write(byte[] b, int off, int len) throws IOException;

    void write(int b) throws IOException;

    /**
     * Writes low eight bits of each character in string -- high 8 bits are ignored.
     */
    void writeString8(String s) throws IOException;

    /**
     * Writes each character in string as 16-bits / 2-bytes.
     */
    void writeString16(String s) throws IOException;

    /**
     * Restricts reads to a slice of file, from <var>start</var> to <var>start</var> + <var>length</var> - 1.
     * While the slice is set, all file operations including {@link #seek(long)} and {@link #getFilePointer()} are relative to <var>start</var>,
     * and {@link #length()} is set to the passed <var>length</var>.
     * Subsequent slices are relative to the original file, not other slices.
     * Remove the slice by invoking with <code>slice(0, Long.MAX_VALUE)</code>.
     */
    void slice(long start, long length) throws IOException;

    void close() throws IOException;
}
