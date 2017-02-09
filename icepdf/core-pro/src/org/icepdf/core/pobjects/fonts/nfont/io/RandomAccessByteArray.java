package org.icepdf.core.pobjects.fonts.nfont.io;

import org.icepdf.core.pobjects.fonts.nfont.lang.Arrayss;
import org.icepdf.core.pobjects.fonts.nfont.lang.Maths;

import java.io.IOException;
import java.io.InputStream;


/**
 * A {@link RandomAccess} interface to a <code>byte[]</code>.
 * <p/>
 * <table width='99%' border='1'>
 * <tr><th>Similar to<th>Advantages<th>Disadvantages
 * <tr><td>{@link java.io.RandomAccessFile}<td>never writes to disk so faster and no IOExceptions.  Never needs disk access permission, so usable in security-restricted applets or Java Web Start.<td>entire data kept in memory
 * <tr><td><code>byte[]</code><td>extensible too and can be wrapped by {RandomAccessDataBE data accessors}<td>method access overhead
 * <tr><td>{@link java.io.ByteArrayOutputStream}<td>re-writable and readable<td>entire data kept in memory
 * <tr><td>{@link java.io.ByteArrayInputStream}<td>random access and writable<td>entire data kept in memory
 * </table>
 * <p/>
 * <!--
 * <p>Does not subclass {@link java.io.RandomAccessFile} because
 * we want to use this class in restricted situations where reading of local files is not permitted,
 * and {@link java.io.RandomAccessFile} always checks the readability of its file.
 * -->
 *
 * @author Copyright (c) 2003-2005  Thomas A. Phelps.  All rights reserved.
 * @version $Revision: 1.1 $ $Date: 2005/07/12 21:33:19 $
 * @see org.icepdf.core.pobjects.fonts.nfont.io.RandomAccessFileBuffered
 */
public class RandomAccessByteArray implements RandomAccess {
    /*  private static final File READABLE_FILE;
    static {
      // is any file readable from within an applet / Java Web Start ? NO
      File path = Classes.getRootDir("RandomAccessByteArray.class");
      READABLE_FILE = new File(phelps.net.URIs.decode(path));
    }*/

    private String mode_;
    private byte[] buf_;
    private long pos_ = 0;    // could be int since Java arrays can't be > 2^(32-1)
    private long length_;

    private long fp0_ = 0L;
    private long length0_ = Long.MAX_VALUE;


    /**
     * @param mode can be any mode accepted by {@link java.io.RandomAccessFile}: <code>"r"</code>, <code>"rw"</code>, <code>"rws"</code>, <code>"rwd"</code>.
     */
    public RandomAccessByteArray(byte[] buf, String mode) {
        //throw new IllegalArgumentException("Illegal mode \"" + mode + "\" must be one of \"r\", \"rw\", \"rws\", or \"rwd\"");
        //try { -- can't do this -- superclass constructor has to be absolute first
        //super(READABLE_FILE, "r");	// work even if no write permission to local file system
        //try { super.close(); } catch (IOException ignore) {}	// don't use that file -- just to satisfy constructor

        buf_ = buf;
        length_ = buf.length;
        mode_ = mode;
    }

    /**
     * Sets contents from (remainder of) <var>in</var> and closes <var>in</var>.
     */
    public RandomAccessByteArray(InputStream in, String mode) throws IOException {
        this(InputStreams.toByteArray(in, 32 * 1024), mode);
    }

    public RandomAccessByteArray(long length) {
        length_ = 0;
        buf_ = new byte[Math.max((int) length, 1024)];
        mode_ = "rw";    // makes no sense to have 0-length read-only
    }

    private void resize(long length) {
        if (length != buf_.length) {
            byte[] newbuf = new byte[(int) length];
            System.arraycopy(buf_, 0, newbuf, 0, (int) Math.min(buf_.length, length));
            buf_ = newbuf;
        }
    }


    public int read() {
        return getFilePointer() < length() ? buf_[(int) pos_++] & 0xff : -1;
    }

    public int read(byte[] b) {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) {
//        assert b!=null && off >= 0 && len >= 0 && len + off <= b.length;
        if (!(b != null && off >= 0 && len >= 0 && len + off <= b.length))
            throw new IllegalArgumentException();
        if (len == 0)
            return 0;
        else if (getFilePointer() >= length()) return -1;

        len = Math.min(len, (int) (length() - getFilePointer()));
        System.arraycopy(buf_, (int) pos_, b, off, len);
        pos_ += len;
        return len;
    }

    public void readFully(byte[] b) {
        read(b);
    }

    public void readFully(byte[] b, int off, int len) {
        read(b, off, len);
    }


    public void writeString8(String s) throws IOException {
        for (int i = 0, imax = s.length(); i < imax; i++) write(s.charAt(i));
    }

    public void writeString16(String s) throws IOException {
        for (int i = 0, imax = s.length(); i < imax; i++)
            writeChar(s.charAt(i));
    }

    public void writeChar(char ch) throws IOException {
        write(ch >> 8);
        write((int) ch);
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (mode_.indexOf("w") == -1)
            throw new IOException("File not opened for writing");    // throw unchecked instead?
//        assert b!=null && off >= 0 && len >= 0 && off + len <= b.length;
        if (!(b != null && off >= 0 && len >= 0 && off + len <= b.length))
            throw new IllegalArgumentException();
        long newlen = Math.max(getFilePointer() + len, length());
        if (newlen > buf_.length) {
            resize(Math.max(/*newlen + 100*1024 => maybe extensible array*/ length() * 2, 5 * 1024));
            length_ = newlen;
        }
        System.arraycopy(b, off, buf_, (int) pos_, len);
        pos_ += len;
    }

    public void write(int b) throws IOException {
        if (mode_.indexOf("w") == -1)
            throw new IOException("File not opened for writing");

        if (getFilePointer() + 1 > buf_.length) {
            resize(Math.max(length() + 100 * 1024, 5 * 1024));
            length_++;
        }    // extra space so can keep writing without reallocating after each byte
        buf_[(int) pos_++] = (byte) b;
    }

    public long getFilePointer() {
        return pos_ - fp0_;
    }

    public void setLength(long newLength) {
//        assert newLength >= 0;
        if (newLength < 0) {
            throw new IllegalArgumentException();
        }
        resize((int) newLength);
        length_ = newLength;
    }

    public long length() {
        return length0_ == Long.MAX_VALUE ? length_ : length0_;
    }

    public void seek(long pos) {
        pos_ = Maths.minmax(0, fp0_ + pos, fp0_ + length());
    }

    public int skipBytes(int n) throws IOException {
        long now = getFilePointer();
        seek(now + n);
        return (int) (getFilePointer() - now);
    }

    public void slice(long start, long length) throws IOException {
        start = Maths.minmax(0L, start, length_);
        if (length < 0) length = length_ - start;
        else if (start > 0 || length < Long.MAX_VALUE)
            length = Maths.minmax(0L, length, length_ - start);

        if (start != fp0_ || length != length0_) {
            fp0_ = start;
            length0_ = length;
            seek(0L);    // relative to fp0_
        }
    }

    public byte[] toByteArray() {
        int len = (int) length();
        byte[] buf = fp0_ == 0 && buf_.length == len ? buf_ : (byte[]) Arrayss.subset(buf_, (int) fp0_, len);
        close();
        return buf;
    }

    public void close() {
        // nothing to flush
        //X super.close(); => no superclass
        buf_ = null;
    }
}
