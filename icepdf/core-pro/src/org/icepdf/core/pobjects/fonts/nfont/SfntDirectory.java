package org.icepdf.core.pobjects.fonts.nfont;

import org.icepdf.core.pobjects.fonts.nfont.lang.Mac;


/**
 * Directory entry for Sfnt-based fonts, such as TrueType.
 */
public/*for nfont.Info and core.Compress*/ class SfntDirectory {
    public int tag;
    public int checkSum;
    public/*Info*/ int offset;
    private int offset0;
    public/*Info*/ int length;
    private int length0;

    // for editing and writing
    /*package-private*/
    int padlen;
    /**
     * If <code>null</code>, read content from existing file; if non-null, use that data.
     */
    /*package-private*/
    byte[] data = null;


    /*package-private*/
    SfntDirectory(int tag, int checkSum, int offset, int length) {
        this.tag = tag;
        this.checkSum = checkSum;
        this.offset = offset0 = offset;
        this.length = length0 = length;
        data = null;
    }

    /*package-private*/
    SfntDirectory(int tag, byte[] data) {
        this.tag = tag;
        length0 = -1;
        setData(data);
    }

    /**
     * If <var>data</var> is <code>null</code>, data is reread from file.
     */
    public void setData(byte[] data) {
        //tag = (same)
        if (data != null) {
            offset = -1;
            length = data.length;
        } else {
            offset = offset0;
            length = length0;
        }
        //X int mod4 = length%4; padlen = length + (mod4==0? 0: 4-mod4); => computed at write
        //X checkSum => don't waste work if don't use, may modify data before writing
        this.data = data;
    }

    public String toString() {
        return Mac.strTag(tag) + " @ " + Integer.toHexString(offset) + " .. +" + length;
    }
}
