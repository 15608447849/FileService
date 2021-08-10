package server.xdoc;

import bottle.util.Log4j;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import server.OS;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class ConvertUtil_jacob {
    static {
        try {
            OS.dynamicDllLoad();
            ComThread.InitMTA();
        } catch (Exception e) {
            Log4j.error("jabob初始化失败",e);
        }
    }

    /**
     * 添加水印
     */
    public static void add_watermark(InputStream in, OutputStream out, String watermark) throws IOException {
        try {
            PdfReader reader = new PdfReader(in);
            PdfStamper stamper = new PdfStamper(reader, out);
            BaseFont font = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H",BaseFont.NOT_EMBEDDED, true);
            Rectangle pageSize = null;
            PdfGState gs = new PdfGState();
            gs.setFillOpacity(0.1f);
            gs.setStrokeOpacity(0.4f);
            int pageCount = reader.getNumberOfPages() + 1;
            Graphics2D g = (Graphics2D) new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB).getGraphics();
            int textWidth = (int) new Font("宋体", Font.PLAIN, 24).getStringBounds(watermark, g.getFontRenderContext()).getWidth();;
            PdfContentByte pcb;
            for (int i = 1; i < pageCount; i++) {
                pageSize = reader.getPageSizeWithRotation(i);
                int wn = (int) Math.ceil(pageSize.getWidth() / textWidth);
                int hn = (int) Math.ceil(pageSize.getHeight() / textWidth);
                pcb = stamper.getOverContent(i);
                pcb.saveState();
                pcb.setGState(gs);
                pcb.beginText();
                pcb.setFontAndSize(font, 24);
                for (int m = 0; m < hn; m++) {
                    for (int n = 0; n < wn; n++) {
                        pcb.showTextAligned(Element.ALIGN_LEFT , watermark, 24 + n * textWidth, m * textWidth, 45);
                    }
                }
                pcb.endText();
            }
            stamper.close();
            reader.close();
        } catch (DocumentException e) {
            Log4j.error("添加水印",e);
        }
    }

    public static void excel_to_pdf(String src, String tar) throws Exception {
        ActiveXComponent app = null;
        Dispatch doc = null;
        try {
            app = new ActiveXComponent("Excel.Application");
            app.setProperty("Visible", new Variant(false));
            app.setProperty("AutomationSecurity", new Variant(3));
            Dispatch docs = app.getProperty("Workbooks").toDispatch();
            doc = Dispatch.call(docs, "Open",src, Boolean.FALSE, Boolean.TRUE)
                    .toDispatch();
            Dispatch.call(doc, "ExportAsFixedFormat", 0, tar);
        } finally {
            if (doc != null) {
                try {
                    Dispatch.call(doc, "Close", Boolean.FALSE);
                } catch (Exception e) {
                    Log4j.error("excel转pdf",e);
                }
            }
            if (app != null) {
                try {
                    app.invoke("excel转pdf");
                    app.safeRelease();
                } catch (Exception e) {
                    Log4j.error("excel转pdf",e);
                }
            }
        }
    }

    public static void ppt_to_pdf(String src, String tar)  throws Exception{
        ActiveXComponent app = null;
        Dispatch doc = null;
        try {
            app = new ActiveXComponent("PowerPoint.Application");
            app.setProperty("AutomationSecurity", new Variant(3));
            Dispatch docs = app.getProperty("Presentations").toDispatch();
            doc = Dispatch.call(
                    docs,
                    "Open",  src, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE ).toDispatch();
            Dispatch.call(doc, "SaveAs", tar, 32);
        } finally {
            if (doc != null) {
                try {
                    Dispatch.call(doc, "Close");
                } catch (Exception e) {
                    Log4j.error("ppt转pdf",e);
                }
            }
            if (app != null) {
                try {
                    app.invoke("Quit");
                    app.safeRelease();
                } catch (Exception e) {
                    Log4j.error("ppt转pdf",e);
                }
            }
        }
    }

    public static void word_to_pdf(String src, String tar) {
        ActiveXComponent app = null;
        Dispatch doc = null;
        try {
            app = new ActiveXComponent("Word.Application");
            app.setProperty("Visible", new Variant(false));
            app.setProperty("AutomationSecurity", new Variant(3));
            Dispatch docs = app.getProperty("Documents").toDispatch();
            doc = Dispatch.call(docs, "Open", src, Boolean.FALSE, Boolean.TRUE )
                    .toDispatch();
            Dispatch.call(doc, "SaveAs", tar, 17);
        } catch (Exception e) {
            Log4j.error("word转pdf",e);
        } finally {
            if (doc != null) {
                try {
                    Dispatch.call(doc, "Close",  Boolean.FALSE );
                } catch (Exception e) {
                    Log4j.error("word转pdf",e);
                }
            }
            if (app != null) {
                try {
                    app.invoke("Quit", new Variant(false));
                    app.safeRelease();
                } catch (Exception e) {
                    Log4j.error("word转pdf",e);
                }
            }

        }
    }

}
