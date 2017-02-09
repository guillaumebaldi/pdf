package org.icepdf.core.pobjects.fonts.nfont.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * ASCII base-85 filter: 5 chars decoded to 4 bytes.
 * Used by PostScript, Type 1 fonts, PDF (thumbnails, indexed color spaces, embedded fonts, but not often for content streams).
 *
 * @version $Revision: 1.1 $ $Date: 2005/07/12 21:33:19 $
 */
public class InputStreamASCII85 extends FilterInputStream {
    private boolean eof_ = false;
    private long val_ = 0;    // 4 bytes--unsigned!
    private int vali_ = -1;

    public InputStreamASCII85(InputStream in) {
        super(in);
    }

    public int read(byte[] b, int off, int len) throws IOException {
//	      assert b!=null && off>=0 && len>=0 && len+off <= b.length;
        if (!(b != null && off >= 0 && len >= 0 && len + off <= b.length)) {
            throw new IllegalArgumentException();
        }
        if (eof_ && vali_ < 0/*drains*/)
            return -1;

        len = Math.min(len, b.length - off);
        for (int i = off, imax = off + len, c; i < imax; i++) {
            if (vali_ == 16 && i + 3 < imax) {    // 5 chars=>4 bytes but already processed one byte.  2910ms=>2740 on fractalmap.core
                b[i] = (byte) (val_ >> 16);
                b[i + 1] = (byte) (val_ >> 8);
                b[i + 2] = (byte) val_;
                vali_ = -8;
                i += 3 - 1;
            } else if ((c = read()) != -1)
                b[i] = (byte) c;
            else
                return i - off;
        }

        return len;
    }


    public int read() throws IOException {
        int b;

        if (vali_ >= 0) {    // 16, 8, 0, -8 -- before eof check because may be draining
            b = (int) (val_ >> vali_) & 0xff;
            vali_ -= 8;

        } else if (eof_)
            b = -1;

        else {
            int c;
            // can see EOF/-1 before '~>', as in IPwA.core object 1240
            while ((c = in.read()) != -1 && (c < '!' || c > '~')) {
//                assert c < 0x80;	// 33 (!) to 117 (u) + special cases (z, ~).  "while" to skip interstitial whitespace.  c==-1 is error
                if (c >= 0x80)
                    throw new IllegalStateException();
            }

            if (c == -1 || c == '~') {
                eof_ = true;
                if (c == '~') {
                    c = in.read();
//                    assert c=='>': c;
                    if (c != '>')
                        throw new IllegalStateException(c + "");
                }
                b = -1;

            } else if (c == 'z') {    // special case: 'z' = four 00s
                // could recurse, but handle first byte
                val_ = 0L;
                vali_ = 24 - 8;
                b = 0;

            } else {    // fill up rest of 5 chars
                val_ = c - '!';
                vali_ = -8;
                for (int i = 1; i < 5; i++) {
                    while ((c = in.read()) != -1 && (c < '!' || c > '~')) {
                    }    // skip whitespace
                    if (c != '~') {
                        val_ = val_ * 85 + (c - '!');
                        vali_ += 8;
                    } else {
                        c = in.read();
//                        assert c=='>';
                        if (c != '>')
                            throw new IllegalStateException(c + "");
                        for (int j = i; j < 5; j++) val_ = val_ * 85 + 84;
                        val_ >>= (5 - i) * 8;    // i == 2..4 => 1-3 valid bytes => shifts of 3..1
                        eof_ = true;
                        break;
                    }
                }

                // could recurse, but handle first byte
//                assert vali_ >= 0;	// single valid char before '~>' EOF, which doesn't give 8 bits
                if (vali_ < 0)
                    throw new IllegalStateException(c + "");
                b = (int) (val_ >> vali_) & 0xff;
                vali_ -= 8;
            }
        }

//        assert vali_==16 || vali_ == 8 || vali_ == 0 || vali_ == -8;
        if (!(vali_ == 16 || vali_ == 8 || vali_ == 0 || vali_ == -8))
            throw new IllegalStateException();
//        assert b>=-1 && b <= 255;
        if ((b >= -1 && b <= 255))
            throw new IllegalStateException();
        return b;
    }


    public boolean markSupported() {
        return false;
    }
}
