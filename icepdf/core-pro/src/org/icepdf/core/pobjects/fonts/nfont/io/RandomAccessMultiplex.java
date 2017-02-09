package org.icepdf.core.pobjects.fonts.nfont.io;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Multiplex a limited number of file descriptors.
 * That is, for applications that require numerous file descriptors, such as fonts,
 * share a set of file descriptors, closing a file only when necessary in order to give it to another.
 * Data is keyed by URL so that if it needs to be closed it can be reopened.
 *
 * @version $Revision: 1.1 $ $Date: 2005/07/12 21:33:19 $
 */
public class RandomAccessMultiplex {

    private static final Logger logger =
            Logger.getLogger(RandomAccessMultiplex.class.toString());

    class Rec {
        RandomAccess ra = null;
        Object owner;
        URL source;

        public String toString() {
            return owner != null ? owner.toString() : "null";
        }
    }

    private final int max_;
    private final int bufsiz_;
    /**
     * LRU list of RAs in use (last is youngest, so cheap to switch lists).
     */
    private final List<Rec> inuse_;
    /**
     * LRU list of RAs created but not in present use (last is youngest).
     */
    private final List<Rec> avail_;
    /**
     * Cache of RandomAccess read from URLs.
     */
    private final Map<URL, SoftReference<RandomAccess>> expensive_ = new HashMap<URL, SoftReference<RandomAccess>>(13);    // don't care about owner because these are RAByteArray and seek for free
    /**
     * Statistic: fast reclaims of already-open.
     */
    private int s_fast = 0;
    /**
     * Statistic: closes of open.
     */
    private int s_force = 0;
    /**
     * Statistic: reuse of byte[] from URL.
     */
    private int s_ex = 0;
    /**
     * Creates a new instance, sharing at most <var>max</var> file descriptors among all clients.
     */
    public RandomAccessMultiplex(int max) {
        this(max, Files.BUFSIZ/*, 0L*/);
    }


    public RandomAccessMultiplex(int max, long bufsize/*, long maxmem*/) {
        max_ = Math.max(1, max);
        bufsiz_ = Math.max(1, (int) bufsize);

        inuse_ = new ArrayList<Rec>(max_);    // LinkedList faster to move from one list to another, but slower for scanning (though not O(n))
        avail_ = new ArrayList<Rec>(max_);
    }

    /**
     * Obtains a {@link RandomAccess} object (shared pool of file descriptors).
     * Only a limited number of RandomAccess objects are available, so
     * clients should only briefly hold one before {@link #releaseRA(RandomAccess) releasing} it.
     *
     * @param source can be to file, http over network, systemresource to JAR
     */
    public synchronized RandomAccess getRA(Object owner, URL source) throws IOException, InterruptedException {
//        assert source!=null;
        if (source == null)
            throw new IllegalArgumentException();

        // 1. reuse Rec
        // owner already has available open file descriptor? (memory cache put in LRU rotation for hard ref)
        for (int i = avail_.size() - 1; i >= 0; i--) {
            Rec r = avail_.get(i);
            if (r.owner == owner && r.source.equals(source)) {    // an object can own multiple RAs
                avail_.remove(i);
                inuse_.add(r);
                s_fast++;
                return r.ra;
            }
        }

        // 2. Rec slot
        Rec rec = null;
        // a. Unused capacity?
        if (avail_.size() + inuse_.size() < max_) rec = new Rec();

        // b. Same URL?  Reuse RA.
        // RA's buffer probably no good, but cheaper than close then open on empty buffer.
        // Different owner or would have hit above, and perhaps original owner would have reclaimed and used buffer.
        // Can't share RA among owners because don't want to force sync on owner reads (and easier bookkeeping here when ra.close()).
        // In the case of fonts, dfonts can lose as roman/ital/bold/bi variants share same file and will require more seeks, but different sizes win twice: no seek and leave open slots for other fonts.
        if (rec == null)
            for (int i = avail_.size() - 1; i >= 0; i--) {
                Rec r = avail_.get(i);
                if (source.equals(r.source)) {
                    avail_.remove(i);
                    rec = r;
                    break;
                }
            }

        // c. Close an old file
        while (rec == null) {
            if (avail_.size() > 0) {
                rec = avail_.remove(0);    // LRU: oldest is first
//                assert rec.ra != null;
                if (rec.ra == null) {
                    throw new IllegalStateException();
                }
                rec.ra.close();
                rec.ra = null;
                s_force++;
            } else {
                // wait() until available slot -- fonts release after use, so contention must be among >= max_ different threads, which is rare
                // wait(100);    // blocks thread -- bad if used single-threaded
                /*getLogger.severe(*/
                logger.finer("*** OUT OF SLOTS ***");
//                    System.exit(1);	// for now indicates a bug
            }
        }


        // 3. new RA (RAF if source file, or byte[] if JAR or network)
        if (rec.ra == null) {
            RandomAccess ra = null;
            SoftReference ref = (SoftReference) expensive_.get(source);    // give it a hard ref in the LRU rotation.  Kicks out a file desc, but non-file likely more expensive.
            if (ref != null && (ra = (RandomAccess) ref.get()) != null)
                s_ex++;
            else if ("file".equals(source.getProtocol())) {    // if big and from file then can read incrementally
                try {
                    File file = new File(source.toURI().getPath());    // if source.getPath()/source.getFile() then /Library/Fonts/#HeadlineA.dfont parsed as ref!  You'd think that file.toURL() would escape '#'.
                    ra = new RandomAccessFileBuffered(file, "r", bufsiz_);
                } catch (URISyntaxException e) {
                    logger.log(Level.WARNING, "Error loading system font file.", e);
                }
                /*if (file.length() < bufsiz_ * 2 or maxmem_ or 100*1024) ra = new RandomAccessByteArray(source.openStream(), "r");	// go through LRU, but not memory cache
                else*/
            } else {    // JAR or network, read fully
                ra = new RandomAccessByteArray(source.openStream(), "r");
                expensive_.put(source, new SoftReference<RandomAccess>(ra));
            }
            rec.ra = ra;
        }

        rec.owner = owner;
        rec.source = source;
        inuse_.add(rec);

        return rec.ra;
    }

    public synchronized void releaseRA(RandomAccess ra) {
        // translate RandomAccess to Rec
        for (int i = inuse_.size() - 1; i >= 0; i--) {
            Rec rec = inuse_.get(i);
            if (rec.ra == ra) {
                inuse_.remove(i);
                avail_.add(rec);
                break;
            }
        }

        notify();
//System.out.println(" / releasing slot "+slot+" "+ra+" ("+cnt+" free)");
    }

    public synchronized void close() {
//        assert inuse_.size() == 0: inuse_.size() + " file descriptors still in use";
        if (inuse_.size() != 0)
            throw new IllegalStateException(inuse_.size() + " file descriptors still in use");

        try {
            for (int i = 0, imax = avail_.size(); i < imax; i++) {
                avail_.get(i).ra.close();
            }
        } catch (IOException ioe) {
        }
    }
}
