package server.servlet.imps;

import bottle.util.Log4j;
import server.servlet.iface.Mservlet;
import server.xdoc.ConvertUtil_jacob;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

public class XOfficeServlet extends Mservlet {

    private static String nvl(String str, String def) {
        return str != null ? str : def;
    }

    private static void pipe(InputStream in, OutputStream out)
            throws IOException {
        int len;
        byte[] buf = new byte[4096];
        while (true) {
            len = in.read(buf);
            if (len > 0) {
                out.write(buf, 0, len);
            } else {
                break;
            }
        }
        in.close();
        out.flush();
        out.close();
    }



    private static void doAct(HttpServletRequest request, HttpServletResponse response, boolean post) throws IOException {

        String format = request.getHeader("_xformat");
        if (format == null) {
            format = request.getParameter("_xformat");
        }
        if (format == null) {
            Log4j.info("No Paramater '_xformat'!");
            return;
        }
        String urlFile = null;
        if (!post) {
            urlFile = request.getParameter("_file");
            if (urlFile == null) {
                Log4j.info("No Paramater '_file'!");
                return;
            }
        }
        long s = System.currentTimeMillis();
        Log4j.info("Convert start...");
        String fileName = System.getProperty("java.io.tmpdir");
        if (!fileName.endsWith("/") && !fileName.endsWith("\\")) {
            fileName += "/";
        }
        InputStream in = null;
        OutputStream out = null;
        try {
            File src, tar;
            if (post) {
                fileName += UUID.randomUUID().toString();
                src = new File(fileName + "." + format);
                tar = new File(fileName + ".pdf");
                in = request.getInputStream();
                out = new FileOutputStream(src);
                XOfficeServlet.pipe(in, out);
            } else {
                MessageDigest m = MessageDigest.getInstance("MD5");
                m.update(urlFile.getBytes(StandardCharsets.UTF_8));
                fileName += new BigInteger(1, m.digest()).toString(16);
                src = new File(fileName + "." + format);
                tar = new File(fileName + ".pdf");
                if (!src.exists()) {
                    Log4j.info("Read:" + urlFile + "...");
                    in = new URL(urlFile).openStream();
                    out = new FileOutputStream(src);
                    XOfficeServlet.pipe(in, out);
                } else {
                    Log4j.info("Cache:" + src.getAbsolutePath());
                }
            }
            if (!tar.exists()) {
                Log4j.info(src.getAbsolutePath() + " >>> " + tar.getAbsolutePath());
                if (format.startsWith("doc")) {
                    ConvertUtil_jacob.word_to_pdf(src.getAbsolutePath(), tar.getAbsolutePath());
                } else if (format.startsWith("xls")) {
                    ConvertUtil_jacob.excel_to_pdf(src.getAbsolutePath(), tar.getAbsolutePath());
                } else if (format.startsWith("ppt")) {
                    ConvertUtil_jacob.ppt_to_pdf(src.getAbsolutePath(), tar.getAbsolutePath());
                }
            } else {
                Log4j.info("Cache:" + tar.getAbsolutePath());
            }
            response.setHeader("Content-Disposition", "filename=\"" + tar.getName() + "\"");
            response.setContentType("application/pdf");
            out = response.getOutputStream();
            in = new FileInputStream(tar);
            String watermark = request.getParameter("_watermark");
            if (watermark != null && watermark.trim().length() > 0) {
                ConvertUtil_jacob.add_watermark(in, out, watermark);
            } else {
                XOfficeServlet.pipe(in, out);
            }
        } catch (Exception e) {
            response.setStatus(500);
            Log4j.error("xdoc服务错误",e);
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                }
            }
        }
        Log4j.info("Convert stop,Use " + (System.currentTimeMillis() - s) + " ms!");
    }
















    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        doAct(request, response, false);
    }
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        doAct(request, response, true);
    }
}
