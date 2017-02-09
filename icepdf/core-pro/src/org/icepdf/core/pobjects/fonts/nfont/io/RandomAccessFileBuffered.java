package org.icepdf.core.pobjects.fonts.nfont.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;


/**
 * A {@link java.io.RandomAccessFile} subclass that implements {@link org.icepdf.core.pobjects.fonts.nfont.io.RandomAccess}.
 */
public class RandomAccessFileBuffered extends RandomAccessFile implements RandomAccess {
    public RandomAccessFileBuffered(String name, String mode) throws FileNotFoundException {
        super(name, mode);
    }

    public RandomAccessFileBuffered(File file, String mode) throws FileNotFoundException {
        super(file, mode);
    }

    public RandomAccessFileBuffered(File file, String mode, int bufsize) throws FileNotFoundException {
        super(file, mode);
    }

    public void slice(long start, long length) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void writeString8(String s) throws IOException {
        for (int i = 0, imax = s.length(); i < imax; i++) write(s.charAt(i));
    }

    public void writeString16(String s) throws IOException {
        super.writeChars(s);
    }
}
