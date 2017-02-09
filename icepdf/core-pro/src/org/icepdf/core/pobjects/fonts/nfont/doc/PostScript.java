package org.icepdf.core.pobjects.fonts.nfont.doc;

import org.icepdf.core.pobjects.fonts.nfont.lang.Arrayss;
import org.icepdf.core.pobjects.fonts.nfont.lang.Integers;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * PostScript parser (tokenizer) and writer.
 * Applications: PostScript interpreter, Type 1 nfont parser, CMap parser.
 * Applications can <code>import static</code>.
 * <p/>
 * <ul>
 * <li>reading: {@link #readObject(PushbackInputStream)} (supporting: {@link #eatSpace(PushbackInputStream)}, {@link #getInteger(int)}, {@link #getReal(double)}).
 * <li>writing: {@link #writeObject(Object, DataOutputStream)}
 * </ul>
 *
 * @version $Revision: 1.1 $ $Date: 2005/07/12 21:33:19 $
 */

public class PostScript {
//    public static final Class CLASS_DICTIONARY = Dict.class;
    /**
     * Different than PDF.
     */
    public static final Class CLASS_ARRAY = ArrayList.class;
    public static final Class CLASS_NAME = String.class;
    public static final Class CLASS_STRING = StringBuilder.class;
    public static final Class CLASS_REAL = Double.class;
    public static final Class CLASS_INTEGER = Integer.class;
    public static final Class CLASS_BOOLEAN = Boolean.class;
    public static final Class CLASS_ARRAY_EX = Object[].class;
//    public static final Class CLASS_STREAM_EX = PushbackInputStream.class;
    //public static final Class CLASS_NULL = OBJECT_NULL.getClass();

    public static final Class CLASS_BUILTIN = Short.class;
    public static final Class CLASS_COMMENT = char[].class;
    public static final Class CLASS_DATA = byte[].class;

    public static final Object OBJECT_NULL = new Object() {
        public String toString() {
            return "NULL";
        }
    };
    // Different object type than valid objects.  new String("NULL");=> String so prints, new String() so doesn't match any other String (including literals)
    //private static final char[] OBJECT_COMMENT = new char[0];


    public static final char[] ESCAPE = new char[0x100];
    /**
     * Array position at character index (0-255) is true iff character is PostScript whitespace.
     */
    public static final boolean[] WHITESPACE = new boolean[0x100];
    /**
     * Array position at character index (0-255) is true iff character is PostScript whitespace or delimiter.
     */
    public static final boolean[] WSDL = new boolean[0x100];


    private boolean init;
    static {
        Arrayss.fillIdentity(ESCAPE);
        ESCAPE['n'] = '\n';
        ESCAPE['r'] = '\r';
        ESCAPE['t'] = '\t';
        ESCAPE['b'] = '\b';
        ESCAPE['f'] = '\f';
        //ESCAPE['(']='('; ESCAPE[')']=')'; ESCAPE['\\']='\\'; -- same as themselves
        // also octal character codes (handled separately)

        //for (int i=0,imax=WHITESPACE.length; i<imax; i++) WHITESPACE[i] = WSDL[i] = OP[i] = false;	// redundant
        String ws = "\0\t\r\f\n ";
        for (int i = 0, imax = ws.length(); i < imax; i++)
            WHITESPACE[ws.charAt(i)] = WSDL[ws.charAt(i)] = true;
        String dl = "()<>[]{}/%";
        for (int i = 0, imax = dl.length(); i < imax; i++)
            WSDL[dl.charAt(i)] = true;
    }

    private static final Double /*REALn1=new Double(-1.0),*/ REAL0 = new Double(0.0), REAL1 = new Double(1.0)/*, REAL2=new Double(2.0)*/;
    private static final double[] DIVISORS = {1.0, 0.1, 0.01, 0.001, 1e-4, 1e-5, 1e-6, 1e-7, 1e-8, 1e-9, 1e-10, 1e-11, 1e-12, 1e-13, 1e-14, 1e-15};    // Acrobat has only 5 significant digits in fraction


    /**
     * Read a object/token from a content stream.
     * Has to be recursive so can read array and dictionary.
     * Slightly different syntax than PDF's content stream.
     * Also used in parsing Type 1 fonts.
     */
    public static Object readObject(PushbackInputStream pis) throws IOException {
//	assert pis!=null;
        if (pis == null) {
            throw new IllegalArgumentException("pis can not be null");
        }

        Object obj;    // set by each branch
        StringBuilder sb;

        int c = pis.read();
        switch (c) {

            case '(':    // string
                sb = new StringBuilder(32/*small because return this*/);    // running out of Eden space vs doubling

                for (int nest = 1; nest > 0 && (c = pis.read()) != -1; ) {
                    if (c == '\\') {
                        c = pis.read();
                        if ('0' <= c && c <= '7') {    // 1, 2, or 3 digits of octal
                            int octval = (c - '0');
                            if ('0' <= (c = pis.read()) && c <= '7') {
                                octval = octval * 8 + (c - '0');
                                if ('0' <= (c = pis.read()) && c <= '7')
                                    octval = octval * 8 + (c - '0');
                                else pis.unread(c);
                            } else
                                pis.unread(c);
//System.out.println("octal => "+(char)octval+"/"+Integer.toOctalString(octval)+"(decimal="+octval+")");
                            sb.append((char) octval);
                        } else if (c == '\n' || c == '\r') {    // continuation -- don't add to string
                            while (c == '\r' || c == '\n') c = pis.read();
                            pis.unread(c);
                        } else
                            sb.append(ESCAPE[c]);
                    } else if (c == '\n' || c == '\r') {
                        sb.append('\n');    // normalized
                        while (c == '\r' || c == '\n') c = pis.read();
                        pis.unread(c);
                    } else if (c == '(') {
                        nest++;
                        sb.append('(');
                    } else if (c == ')') {
                        nest--;
                        if (nest > 0) sb.append(')');
                    } else
                        sb.append((char) c);    // [different from File]
                }
                //c=pis.read(); -- not invariant anymore
                obj = sb;    // usually further process vis-a-vis encoding
//System.out.println("string: |"+obj+"|); //next char="+(char)c*/);
                break;

/*	case '[':	// array - executed
		eatSpace(pis);
		List<Object> al = new ArrayList<Object>(100);	// copy at end anyhow so avoid doubling
		while ((obj = readObject(pis)) != "]") { al.add(obj); /*System.out.println((char)c+" => "+al.get(al.size()-1));* / }
		//pis.read(); // ']' -- already consumed
System.out.println("proc = "+al);
		obj = al.toArray();
		break;*/

            case '[':    // array - executed
                obj = "[";
                break;
            case ']':    // end array
                obj = "]";
                break;

            case '{':    // procedure - scanned not executed
                eatSpace(pis);
                List al = new ArrayList(100);    // copy at end anyhow so avoid doubling
                while ((obj = readObject(pis)) != "}" && obj != null) {
                    al.add(obj); /*System.out.println((char)c+" => "+al.get(al.size()-1));*/
                }
                //pis.read(); // ']' -- already consumed
//System.out.println("proc = "+al);
                obj = al.toArray();
                break;

            case '}':
                obj = "}";
                break;

            case '<':    // dictionary or hex
                if ((c = pis.read()) == '<') {    // second '<' => dictionary - executed
                    obj = "<<";
                    // don't create dict here -- clients want control

                } else if (c == '~') {    // ASCII85
                    sb = new StringBuilder(100);
                    InputStream a85 = new org.icepdf.core.pobjects.fonts.nfont.io.InputStreamASCII85(pis);
                    while ((c = a85.read()) != -1) sb.append((char) c);
                    a85.close();
                    obj = sb;

                } else if (c != -1) {    // hex, which are actually strings, which are Java StringBuffer
                    pis.unread(c);
                    eatSpace(pis);
                    sb = new StringBuilder(100);
                    int hval, hval2;
                    while ((c = pis.read()) != -1 && c != '>') {
                        hval = Integers.parseInt(c);
                        if (hval == -1) continue;
                        hval <<= 4;

                        c = pis.read();
                        if (c == -1 || c == '>') {
                            sb.append((char) hval);
                            break;
                        } else if ((hval2 = Integers.parseInt(c)) != -1)
                            sb.append((char) (hval + hval2));
                        else
                            sb.append((char) hval);
                    }
//System.out.println("HEX = "+sb+", len="+sb.length()+", "+Integer.toHexString(sb.charAt(0))+" "+Integer.toHexString(sb.charAt(1)));
                    obj = sb;
                    //c=pis.read();	// invariant
                } else
                    obj = null;
                break;

            case '>':
                c = pis.read();
                //assert c == '>': c + " / " + (char) c;
                if (c != '>') {
                    throw new IllegalStateException(c + " / " + (char) c);
                }
                obj = ">>";
                break;

            case '%':    // comment: "%..<eol>", treated as single space, no semantics
                sb = new StringBuilder(40);
                while ((c = pis.read()) != -1 && c != '\n' && c != '\r')
                    sb.append((char) c);
                obj = new char[sb.length()];
                sb.getChars(0, sb.length(), (char[]) obj, 0);
//System.out.println("comment: |"+sb.toString()+"|");
                break;

            case '\0':
            case '\t':
            case '\r':
            case '\f':
            case '\n':
            case ' ':
//                assert false;	// comments processed in caller, and should see no whitespace
                throw new IllegalStateException();
            case 0xff:    // PushbackInputStream returns pushed -1 as 0xff
            case -1:
                obj = null;
                break;

            case '/':    // deferred name
                sb = new StringBuilder(20);
                sb.append('/');    // keep executable indication
                while ((c = pis.read()) != -1) {
                    if (c == '#')
                        sb.append((char) Integers.parseHex(pis.read(), pis.read()));
                    else if (!WSDL[c])
                        sb.append((char) c);
                    else {
                        pis.unread(c);
                        break;
                    }
                }
                obj = sb.toString();    // objects in stream thrown away quickly as commands are processed, so emphasize speed
//System.out.println("name: "+obj);
                break;

            case '+':
            case '-':
            case '.':    // int or float, actually return int or double objects.
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                boolean fisnum = '0' <= c && c <= '9';
                if (!fisnum) {
                    int c2 = pis.read();
                    pis.unread(c2);
                    fisnum = '0' <= c2 && c2 <= '9';
                }
                if (fisnum) {
                    int/*never long in content stream*/ val = 0, whole = 0, div = -100;
                    boolean fneg = false;
                    if (c == '+') fneg = false;
                    else if (c == '-') fneg = true;
                    else if (c == '.') div = 0;
                    else val = c - '0';
                    while ((c = pis.read()) != -1) {
                        if ('0' <= c && c <= '9') {
                            val = val * 10 + c - '0';
                            div++;
                        } else if (c == '.' && div < 0) {
                            div = 0;
                            whole = val;
                            val = 0;
                        } else if (c == 'e' || c == 'E') {
                            div -= ((Number) readObject(pis)).intValue();
                            break;
                        } else if (c == '#') {
                            int radix = val > 0 ? val : 10;
                            val = 0;
                            for (int r; (c = pis.read()) != -1 && (r = Integers.parseInt(c)) != -1 && r < radix; )
                                val = val * radix + r;
//System.out.println("radix "+radix+", in="+(char)c+", num="+r+" = "+val);
                            pis.unread(c);
//System.out.println("radix "+radix+" = "+val);
                        } else {
                            pis.unread(c);
                            break;
                        }
                    }
                    if (fneg) val = -val;
// LATER: if not a number, it was a name!  like '23A' is a name

                    // we make lots of new objects in order to put into dictionaries and heterogeneous arrays
                    if (div < 0/*<=0*/) {    // "1." not considered an int
                        obj = getInteger(val);
                    } else
                        obj = getReal(whole, val, div);
                    break;

                } // else fall through to executable name (e.g.,  "-|")
//Systemout.println("number: "+val+"/"+div);

            default:    // executable name
                sb = new StringBuilder(20);
                sb.append((char) c);
                while ((c = pis.read()) != -1) {
                    if (c == '#')
                        sb.append((char) Integers.parseHex(pis.read(), pis.read()));
                    else if (!WSDL[c])
                        sb.append((char) c);
                    else {
                        pis.unread(c);
                        break;
                    }
                }
                obj = sb.toString();
                if ("true".equals(obj))
                    obj = Boolean.TRUE;
                else if ("false".equals(obj))
                    obj = Boolean.FALSE;
                else if ("null".equals(obj)) obj = OBJECT_NULL;
                //RESTORE: else { Number canonical = (Number)BUILTIN.get(obj); if (canonical!=null) obj = CANONICAL[canonical.intValue()]; }
        }

        eatSpace(pis);

        //assert obj!=null; => EOF, comment
        return obj;
    }

    public static Integer getInteger(int val) {
        return Integers.getInteger(val);
    }

    public static Double getReal(double val) {
        return val == 0.0 ? REAL0 : val == 1.0 ? REAL1 : new Double(val);
    }

    public static Double getReal(int whole, int fract, int pow) {
        return whole == 0 && fract == 0 ? REAL0 :
                whole == 1 && fract == 0 ? REAL1 :
                        0 <= pow && pow < DIVISORS.length ? getReal(whole + fract * DIVISORS[pow]) : // faster to multiply by reciprocal
                                getReal(whole + fract * Math.pow(10.0, -pow));
    }


    public static void eatSpace(PushbackInputStream pis) throws IOException {
//        assert pis!=null;
        if (pis == null) {
            throw new IllegalArgumentException();
        }
        int c;
        while ((c = pis.read()) != -1 && WHITESPACE[c]) {
        }
        pis.unread(c);
        //if (c=='%') { readObject(pis); eatSpace(pis); }	// ok to lose comments because generated PDF is just cached; original PDF remains official source
    }


    public static void writeObject(Object obj, DataOutputStream out) throws IOException {
        Class cl = obj.getClass();
        if (OBJECT_NULL == obj)
            out.writeBytes(" null");
        else if (CLASS_NAME == cl) {
            out.writeBytes("/" + obj);

        } else if (CLASS_STRING == cl) {
            out.write('(');
            StringBuilder sb = (StringBuilder) obj;
            for (int i = 0, imax = sb.length(); i < imax; i++) {
                char ch = sb.charAt(i);
                if (ch == '(')
                    out.writeBytes("\\(");
                else if (ch == ')')
                    out.writeBytes("\\)");
                else if (ch < ' ' || ch >= 127) {
                    out.write('\\');
                    int val = ch;
                    out.write('0' + val / 64);
                    val = val % 64;
                    out.write('0' + val / 8);
                    val = val % 8;
                    out.write('0' + val);
                } else
                    out.write(ch);
            }
            out.write(')');

        } else if (CLASS_ARRAY == cl) {
            out.write('[');
            ArrayList l = (ArrayList) obj;
            for (int i = 0, imax = l.size(); i < imax; i++)
                writeObject(l.get(i), out);
            out.writeBytes("]");

            //} else if (CLASS_DICTIONARY == cl) {
        } else if (obj instanceof Map) {
            out.writeBytes("<<");
            //Dict dict = (Dict)obj;
            Map dict = (Map) obj;
            for (Iterator i = dict.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry e = (Map.Entry) i.next();
                writeObject(e.getKey(), out);
                out.write(' ');
                writeObject(e.getValue(), out);
                out.write('\n');
            }
            out.writeBytes(">>");

        } else if (CLASS_ARRAY_EX == cl) {
            out.write('{');
            Object[] oa = (Object[]) obj;
            for (int i = 0, imax = oa.length; i < imax; i++)
                writeObject(oa[i], out);
            out.writeBytes("}");

        } else if (CLASS_COMMENT == cl) {
            out.writeBytes("% " + obj + "\n");

        } else { //assert CLASS_REAL == cl || CLASS_INTEGER == cl || CLASS_BUILTIN==cl;
            out.write(' ');
            out.writeBytes(obj.toString());
        }
    }
}
