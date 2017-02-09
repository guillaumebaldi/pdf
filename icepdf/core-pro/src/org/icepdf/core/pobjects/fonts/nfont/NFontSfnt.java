package org.icepdf.core.pobjects.fonts.nfont;

import org.icepdf.core.pobjects.fonts.nfont.io.RandomAccess;
import org.icepdf.core.pobjects.fonts.nfont.io.RandomAccessByteArray;
import org.icepdf.core.pobjects.fonts.nfont.lang.Arrayss;
import org.icepdf.core.pobjects.fonts.nfont.lang.Mac;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * sfnt-table-based data, which includes TrueType and OpenType nfont formats.
 * <p/>
 * <ul>
 * <li>instantiation: from {@link #NFontSfnt(URL) URL} or {@link #NFontSfnt(byte[]) byte[]}
 * <li>table management: {@link #getTables() read}, {@link #setTable(int, byte[]) edit}, {@link #checksum(byte[]) checksum}
 * <li>{@link #readUint8() read table contents}
 * <li>{toByteArray() write new nfont} with added/removed/modified tables
 * </ul>
 *
 * @author Copyright (c) 2003 - 2004  Thomas A. Phelps.  All rights reserved.
 * @version $Revision: 1.1 $ $Date: 2005/07/12 21:33:19 $
 */
public abstract class NFontSfnt extends NFontSimple /*implements java.awt.nfont.OpenType*/ {
    //private static final boolean DEBUG = false && multivalent.Meta.DEVEL;

    private static final Logger logger =
            Logger.getLogger(NFontSfnt.class.toString());


    public static final int TAG_TTCF = Mac.intTag("ttcf");

    public static final String FORMAT = "sfnt";


    private static final byte[] TABLE_EMPTY = new byte[0];


    /*package-private*/
    RandomAccess ra_;

    /*package-private*/
    int version_;    // 'true' for Mac only, 0x00010000 for OpenType/Windows, 'typ1' old style, 'OTTO' CFF
    /*package-private*/
    long raoff_;
    protected SfntDirectory[] tabledir_;

    // data for current table
    /*package-private*/
    byte[] data_;
    /*package-private*/
    private ThreadLocal<Integer> offset_ = new ThreadLocal<Integer>();


    public NFontSfnt(URL source) throws FontFormatException, IOException {
        super(source);

        try {
            getRA(); //X assert ra_.getFilePointer() == 0L; => already in use then NFontManager.createFont();
            ra_.seek(0L);
            // if small read entirely?  no, because don't read all tables... maybe read data into backing byte[]
            parseFile();
            parse();
        } catch (IOException ioe) {
            throw ioe;
        } catch (InterruptedException e) {
            // return
        } finally {
            releaseRA();
        }
    }

    public NFontSfnt(byte[] data) throws FontFormatException, IOException {    // still have IOException if read past length
        super(null);
        ra_ = new RandomAccessByteArray(data, "r");
        parseFile();
        parse();
    }


    // set raoff_, version_

    /**
     * Parse header of file, which may contain several fonts.
     */
    protected void parseFile() throws FontFormatException, IOException {
        data_ = readRaw(raoff_, 12);
        offset_.set(0);
        version_ = readUint32();
    }

    /**
     * Reads essential tables for single nfont (out of possibly others in a dfont or ttc).  Other tables read on demand.
     */
    protected void parse() throws IOException {
        // 1. directory, enabling getTable(tag)
        data_ = readRaw(raoff_, 12);
        offset_.set(4);
        int numTables = readUint16();
        //assert numTables>=1: numTables;
        if (!(numTables >= 1)) {
            throw new IllegalStateException(numTables + "");
        }
        // ignore binary search values -- sequential plenty fast and usually few tables

        data_ = readRaw(raoff_ + 12, numTables * 4 * 4);
        offset_.set(0);
        tabledir_ = new SfntDirectory[numTables];
//        assert numTables>0 && numTables < 1000: numTables;
        if (numTables < 0 && numTables >= 1000) {
            throw new IllegalStateException(numTables + "");
        }
        int j = 0;
        for (int i = 0; i < numTables; i++) {
            int tag = readUint32();
            int checksum = readUint32(), offset = readUint32(), length = readUint32();
            // validate: seen 0-length 'kern' by Windows Type 1 Installer V1.0d, and 0-length TSI[0-5] in Mathematica
            if (length > 0 /*&& offset>0 && offset<filelen*/)
                tabledir_[j++] = new SfntDirectory(tag, checksum, offset, length);
        }
        tabledir_ = (SfntDirectory[]) Arrayss.resize(tabledir_, j);
    }

    /*package-private*/
    void getRA() throws IOException, InterruptedException {
        if (ra_ == null) {
//            assert getSource()!=null;
            if (getSource() == null) {
                throw new IllegalStateException();
            }
            ra_ = getMultiplex().getRA(this/*ur_*/, getSource());
        }
    }

    /*package-private for OpenType*/
    void releaseRA() {
        if (ra_ != null && !(ra_ instanceof RandomAccessByteArray)) {
            getMultiplex().releaseRA(ra_);
            ra_ = null;
        }
    }

    /**
     * Returns an array of the nfont table directories.
     */
    public SfntDirectory[] getTables() {
        return (SfntDirectory[]) tabledir_.clone();
    }

    public SfntDirectory getTableDirectory(String sfntTag) {
        return getTableDirectory(Mac.intTag(sfntTag));
    }

    public/*for tool.nfont.Info*/ SfntDirectory getTableDirectory(int sfntTag) {
        for (int i = 0, imax = tabledir_.length; i < imax; i++)
            if (sfntTag == tabledir_[i].tag)
                return tabledir_[i];    // don't binsearch because may have edited
        return null;
    }

    /**
     * Returns the table as an array of bytes for a specified tag.
     */
    public byte[] getTable(int sfntTag) {
        return getTable(sfntTag, 0, Integer.MAX_VALUE);
    }

    /**
     * Returns the table as an array of bytes for a specified tag.
     */
    public byte[] getTable(String sfntTag) {
        return getTable(Mac.intTag(sfntTag));
    }

    /**
     * Returns a subset of the table as an array of bytes for a specified tag.
     */
    public byte[] getTable(int sfntTag, int offset, int count) {
        return getTable(getTableDirectory(sfntTag), offset, count);
    }

    /**
     * Returns a subset of the table as an array of bytes for a specified tag.
     */
    public byte[] getTable(String sfntTag, int offset, int count) {
        return getTable(Mac.intTag(sfntTag), offset, count);
    }

    public byte[] getTable(SfntDirectory td, int offset, int count) {
        byte[] data = TABLE_EMPTY;
        if (td != null) {
            if (td.data != null) {
                data = td.data;    // modified (not cached)
            }else {
                boolean fra = ra_ == null;
                try {
                    getRA();
                    data = readRaw((TAG_TTCF == version_ ? 0L : raoff_) + td.offset + offset, Math.min(td.length, count));
                } catch (Throwable e) {
                    logger.log(Level.FINER, "Error reading table " + td.toString());
                }finally {
                    if (fra) {
                        releaseRA();    // don't release if available through constructor or drawEstring
                    }
                }
            }
        }
        data_ = data;
        offset_.set(0);
        return data_;
    }

    /*package-private*/
    byte[] readRaw(long offset, int length) throws IOException {
//        assert ra_!=null;
        if (ra_ == null) {
            throw new IllegalStateException();
        }
        if (length <= 0) return TABLE_EMPTY;
        byte[] b = new byte[length];
        ra_.seek(offset);
        ra_.readFully(b);
        return b;
    }

    /**
     * Returns the size of the table for a specified tag.
     */
    public int getTableSize(int sfntTag) {
        SfntDirectory td = getTableDirectory(sfntTag);
        return td != null ? td.length : -1;
    }

    /**
     * Returns the size of the table for a specified tag.
     */
    public int getTableSize(String sfntTag) {
        return getTableSize(Mac.intTag(sfntTag));
    }

    /**
     * Sets new data for existing table, or adds new table.
     * There can only be one table by a givn name in the nfont.
     */
    public void setTable(int tag, byte[] data) {
        //assert data!=null?
        SfntDirectory ttd = getTableDirectory(tag);
        /*if (data==null) deleteTable(tag);
        else*/
        if (ttd == null) {
            tabledir_ = (SfntDirectory[]) Arrayss.resize(tabledir_, tabledir_.length + 1);
            tabledir_[tabledir_.length - 1] = new SfntDirectory(tag, data);
        } else
            ttd.setData(data);
    }

    /**
     * Deletes nfont table.
     */
    public void deleteTable(int tag) {
        SfntDirectory ttd = getTableDirectory(tag);
        if (ttd != null) {
            SfntDirectory last = tabledir_[tabledir_.length - 1];
            if (ttd != last) tabledir_[Arrayss.indexOf(tabledir_, ttd)] = last;
            tabledir_ = (SfntDirectory[]) Arrayss.resize(tabledir_, tabledir_.length - 1);
        }
    }

    /**
     * Computes checksum for table <var>data</var>.
     */
    public static int checksum(byte[] data) {
        int sum = 0;

        int i = 0, imax = data.length;
        for (; i + 4 < imax; i += 4) {
            int v = ((data[i] & 0xff) << 24) | ((data[i + 1] & 0xff) << 16) | ((data[i + 2] & 0xff) << 8) | (data[i + 3] & 0xff);
            sum += v;
        }
        if (i < imax) {
            int cnt = 0;
            int v = 0;
            for (; i < imax; i++, cnt++) v = (v << 8) | (data[i] & 0xff);
            for (; cnt < 4; cnt++) v <<= 8;
            sum += v;
        }
        return sum;
    }


    public int readUint8() {
        int val = data_[offset_.get()] & 0xff;
        skip(1);
        return val;
    }

    public int readInt8() {
        int val = data_[offset_.get()];
        skip(1);
        return val;
    }

    public int readUint16() {
        if (data_.length >= 2) {
            int val = ((data_[offset_.get()] & 0xff) << 8) | (data_[offset_.get() + 1] & 0xff);
            skip(2);
            return val;
        } else {
            return 0;
        }
    }

    public int readInt16() {
        int val = (data_[offset_.get()] << 8) | (data_[offset_.get() + 1] & 0xff);
        skip(2);
        return val;
    }

    public int readUint32() {
        int val = ((data_[offset_.get()] & 0xff) << 24) | ((data_[offset_.get() + 1] & 0xff) << 16) | ((data_[offset_.get() + 2] & 0xff) << 8) | (data_[offset_.get() + 3] & 0xff);
        skip(4);
        return val;
    }

    public int readInt32() {
        int val = (data_[offset_.get()] << 24) | ((data_[offset_.get() + 1] & 0xff) << 16) | ((data_[offset_.get() + 2] & 0xff) << 8) | (data_[offset_.get() + 3] & 0xff);
        skip(4);
        return val;
    }

    public float readFixed() {
        int m = readInt16();
        float f = readUint16() / 65536f;
        return m + (m >= 0 ? +f : -f);
    }

    public float readF2Dot14() {
        int val = readInt16();
        return (val >> 14) + (val & 0x3fff) / 16384f;
    }

    public long readDateTime() {
        long val = (readUint32() << 32) | readUint32();
        // convert from 1904-based to Java 1900-based?
        return val;
    }

    public String readStringPascal() {
        // we have a corner case where the data_ comes up a little short during
        // the parse.  I can't tell if it's a parser error but I've added some
        // code to finish the read by returning null, PDF-442.
        if (offset_.get() < data_.length) {
            int length = readUint8();
            StringBuilder sb = new StringBuilder(length);
            int dataLength = data_.length - 1;
            for (int i = 0; i < length && offset_.get() < dataLength; i++) {
                sb.append((char) (data_[offset_.get()] & 0xff));
                skip(1);
            }
            return sb.toString();
        } else {
            return null;
        }
    }

    /*package-private*/
    String readString(int offset, int length) {
        StringBuilder sb = new StringBuilder(length);
        if (offset + length > data_.length)
            length = data_.length - offset;    // bad data seen in Courier New, embedded in IGEE_StudentInstructions.core by OS X 10.2.6
        for (int i = 0; i < length; i++)
            sb.append((char) (data_[i + offset] & 0xff));
        return sb.toString();
    }

    /*package-private*/
    String readString16(int offset, int length) {
        StringBuilder sb = new StringBuilder(length / 2);
        for (int i = 0; i < length; i += 2)
            sb.append((char) (((data_[i + offset] & 0xff) << 8) | (data_[i + offset + 1] & 0xff)));
        return sb.toString();
    }

    public int getOffset() {
        return offset_.get();
    }

    public void setOffset(int offset) {
        offset_.set(offset);
    }

    public void skip(int count) {
        offset_.set(offset_.get() + count);
    }
}
