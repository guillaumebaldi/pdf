package org.icepdf.core.pobjects.fonts.nfont.io;

import org.icepdf.core.pobjects.fonts.nfont.lang.Arrayss;

import java.io.IOException;


/**
 * Parse Macintosh Resource files.
 * Mac resource forks are not readable by Java (or UNIX), but OS X <code>.dfont</code>s have a resource format in the data fork.
 *
 * @version $Revision: 1.1 $ $Date: 2005/07/12 21:33:19 $
 */
public class Rez {
    private RandomAccess ra_;
    private long raoffset_;

    private long dataoff_;
    private long nameoff_;
    private int[] type_;
    private int[] typecnt_;
    private long[] refsoff_;

    private byte[] data_;
    private int offset_;


    public Rez(RandomAccess ra, long offset) throws IOException {
        ra_ = ra;
        raoffset_ = offset;
        long ralen = ra_.length();

        // header
        readRaw(0, 16);
        dataoff_ = read32();
        long mapoff = read32();
        int datalen = read32();
        int maplen = read32();
        if (mapoff < 0 || mapoff + maplen > ralen || dataoff_ < 0 || dataoff_ + datalen > ralen)
            throw new IOException("not valid Resource file (in data fork)");

        // type list
        readRaw(mapoff, 30);
        offset_ = 24;    // reserved + attributes
        long typesoff = read16() + mapoff;
        nameoff_ = read16() + mapoff;
        int typescnt = read16() + 1;
        if (!(mapoff + 30 + typescnt * 8 < ralen && typesoff < ralen && nameoff_ < ralen))
            throw new IOException("not valid Resource file (in data fork)");

        type_ = new int[typescnt];
        typecnt_ = new int[typescnt];
        refsoff_ = new long[typescnt];

        // assert typesoff +2 == mapoff + 30;
        if (typesoff + 2 != mapoff + 30)
            throw new IllegalStateException();

        readRaw(typesoff + 2/*empirical*/, typescnt * 8);
        for (int i = 0; i < typescnt; i++) {
            type_[i] = read32();
            typecnt_[i] = read16() + 1;
            refsoff_[i] = read16() + typesoff;
        }
    }

    public int[] getTypes() {
        return (int[]) type_.clone();
    }


    /**
     * Returns array of names for resource of <var>type</var>, or 0-length array if no such type.
     */
    public String[] getNames(int type) throws IOException {
        int inx = Arrayss.indexOf(type_, type);
        if (inx == -1) return new String[0];

        int typecnt = typecnt_[inx];
        String[] name = new String[typecnt];

        readRaw(refsoff_[inx], 12 * typecnt);
        for (int j = 0; j < typecnt; j++) {
            offset_ = j * 12 + 2;
            long nameoff = read16();
            name[j] = nameoff != 0xff ? readString(nameoff + nameoff_) : "";
        }
        return name;
    }

    /**
     * Returns array of offsets and lengths of data for all resources of <var>type</var>,
     * in the order <var>offset1</var> <var>length1</var> <var>offset2</var> <var>length2</var> ....
     */
    public long[] getResources(int type) throws IOException {
        int inx = Arrayss.indexOf(type_, type);
        if (inx == -1) return new long[0];

        int typecnt = typecnt_[inx];
        long[] offlen = new long[typecnt * 2];

        readRaw(refsoff_[inx], 12 * typecnt);
        byte[] reflist = data_;
        for (int j = 0; j < typecnt; j++) {
            data_ = reflist;
            offset_ = j * 12 + 5;
            long resstart = read24() + dataoff_;
            offlen[j * 2] = resstart + 4 + raoffset_;
            readRaw(resstart, 4);
            offset_ = 0;
            offlen[j * 2 + 1] = read32() & 0xffffffff;
        }

        return offlen;
    }

    /**
     * Return array of offset and length for data of <var>type</var> and within that <var>name</var>.
     * Note that not all resources have names, in which case {@link #getResources(int)} can return all reources, from which one can be selected by position.
     */
    public long[] getResource(int type, String name) throws IOException {
        int inx = Arrayss.indexOf(type_, type);
        if (inx == -1) return new long[0];

        int typecnt = typecnt_[inx];
        readRaw(refsoff_[inx], 12 * typecnt);
        for (int j = 0; j < typecnt; j++) {
            offset_ = j * 12 + 2;
            long nameoff = read16();
            String n = nameoff != 0xff ? readString(nameoff + nameoff_) : "";
            if (name.equals(n)) {
                offset_++;
                long offset = read16() + dataoff_;
                readRaw(offset, 4);
                long length = read32();
                return new long[]{offset + 4, length};
            }
        }
        return new long[0];
    }


    private void readRaw(long offset, int length) throws IOException {
        ra_.seek(offset + raoffset_);
        data_ = new byte[length];
        ra_.readFully(data_);
        offset_ = 0;
    }

    private int read32() {
        int val = ((data_[offset_++] & 0xff) << 24) | ((data_[offset_++] & 0xff) << 16) | ((data_[offset_++] & 0xff) << 8) | (data_[offset_++] & 0xff);
//        assert val>=0;
        if (val < 0)
            throw new IllegalStateException();
        return val;
    }

    private int read24() {
        return ((data_[offset_++] & 0xff) << 16) | ((data_[offset_++] & 0xff) << 8) | (data_[offset_++] & 0xff);
    }

    private int read16() {
        return ((data_[offset_++] & 0xff) << 8) | (data_[offset_++] & 0xff);
    }

    private int read8() {
        return data_[offset_++] & 0xff;
    }

    private String readString(long offset) throws IOException {
        ra_.seek(offset + raoffset_);
        int len = ra_.read();
        byte[] buf = new byte[len];
        ra_.readFully(buf);
        return new String(buf, 0, len, "US-ASCII");
    }
}
