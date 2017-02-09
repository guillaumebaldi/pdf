
package org.icepdf.core.pobjects.fonts.nfont.instructions;

public class Cvt {


    public short unscaledCvt[];
    public int cvt_[];
    public double scale;

    public Cvt () {


    }

    public void scale(double d) {

        scale = d;
        for(int i = 0; i < unscaledCvt.length; i++)
            cvt_[i] = (int)(d * (double)unscaledCvt[i] + 0.5D);
    }

    public void writePixels(int loc, int val) {
        if(loc < cvt_.length)
            cvt_[loc] = val;
    }

    public void writeFUnits(int loc, int val)  {
        val = (int)((double)val * scale + 0.5D);
        if(loc < cvt_.length)
            cvt_[loc] = val;
    }

    public int get(int loc) {
        if(loc < cvt_.length)
            return cvt_[loc];
        else
            return 0;
    }
}
