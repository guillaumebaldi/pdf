package org.icepdf.core.pobjects.fonts.nfont.instructions;

import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.Name;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.pobjects.Resources;
import org.icepdf.core.pobjects.fonts.FontFile;
import org.icepdf.core.pobjects.graphics.TextState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/**
 * Test/debug tool for hinting instructions.
 * <p/>
 * test 1: HA_20120316A.PDF
 * FONT NAME: /TT3
 * GLYPH ID: \u0081\u0082\u0083
 * <p/>
 * test 2: ch03.pdf
 * FONT NAME: /C2_0
 * GLYPH ID: \0927\3349\3203\3349\3213\2368
 * <p/>
 * test 3: HA_20120316c.pdf
 * FONT NAME: /F9
 * GLYPH ID: \u0081\u0082\u0083
 */
public class HintingTest {

    public static void main(String[] args) {
        try {
            // test 1:
//            new Demo(args[0].concat("HA_20120316A.pdf"),"TT3","\u0081\u0082\u0083");
//            new Demo(args[0].concat("HA_20120316A.pdf"), "TT3", "\u0082"); // 21 ops

            // test 2:
            new Demo(args[0].concat("ch03.pdf"), "C2_0", String.valueOf((char) 927)); // 28 ops
//            new Demo(args[0].concat("ch03.pdf"), "C2_0", String.valueOf((char)3349)); // 90 ops

            // test 3:
//            new Demo(args[0].concat("HA_20120316c.pdf"),"F9","\u0081\u0082\u0083");
//            new Demo(args[0].concat("HA_20120316c.pdf"),"F9","\u0082");   // 33 ops

        } catch (PDFException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (PDFSecurityException e) {
            e.printStackTrace();
        }
    }

}

class Demo extends JFrame {

    private FontFile font;
    private String gids;

    public Demo(String filePath, String fontName, String gids) throws PDFException, IOException, PDFSecurityException {
        super("Hinting Sample");
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                setVisible(false);
                dispose();
            }
        });
        // load the document
        Document document = new Document();
        document.setFile(filePath);
        // get the page we know the font is one
        Page page = document.getPageTree().getPage(0);
        try {
            page.init();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // retrieve the font by name
        Resources resources = page.getResources();
        org.icepdf.core.pobjects.fonts.Font font = resources.getFont(new Name(fontName));
        font.init();
        FontFile fontFile = font.getFont();
        this.gids = gids;
        this.font = fontFile;
        setBounds(100, 100, 800, 600);
        setVisible(true);
    }


    public void paint(Graphics g_old_api) {
        Graphics2D g = (Graphics2D) g_old_api;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        int w = getWidth(), h = getHeight();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.BLACK);


        font = font.deriveFont(250f);
        font.drawEstring(g, gids, 250, 250, FontFile.LAYOUT_NONE, TextState.MODE_FILL, Color.black);
    }
}

