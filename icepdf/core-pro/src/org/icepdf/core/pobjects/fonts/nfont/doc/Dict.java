package org.icepdf.core.pobjects.fonts.nfont.doc;


/**
 * PostScript dictionary, which is different from PDF dictionary..
 * Keys are any object type (although (strings) are converted to /names), and values can be any PostScript object.
 *
 * @version $Revision: 1.1 $ $Date: 2005/07/12 21:33:19 $
 */
public class Dict extends java.util.HashMap {
    public Dict() {
        super();
    }

    public Dict(int initialCapacity) {
        super(initialCapacity);
    }

    //public Dict(int initialCapacity, float loadFactor) { super(initialCapacity, loadFactor); }
    public Dict(Dict m) {
        super(m);
    }

    // in PostScript, any object can be a key, but strings are canonicalized to names
    // in PDF, all keys are names
    public Object put(Object key, Object value) {
        return super.put(key.getClass() == PostScript.CLASS_STRING ? key.toString() : key, value);
    }

    public Object get(String key) {
        return super.get(key.getClass() == PostScript.CLASS_STRING ? key.toString() : key);
    }

    public Object remove(String key) {
        return super.remove(key.getClass() == PostScript.CLASS_STRING ? key.toString() : key);
    }

    public boolean containsKey(String key) {
        return super.containsKey(key.getClass() == PostScript.CLASS_STRING ? key.toString() : key);
    }
}
