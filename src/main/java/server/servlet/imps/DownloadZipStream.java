package server.servlet.imps;


import bottle.util.EncryptUtil;
import bottle.util.FileTool;
import bottle.util.Log4j;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;
import server.HuaWeiOBS.HWOBSServer;
import server.HuaWeiOBS.OBSUploadPoolUtil;
import server.prop.WebServer;
import server.servlet.beans.result.Result;
import server.servlet.iface.Mservlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static server.servlet.beans.operation.ZipUtils.checkPaths;
import static server.servlet.beans.result.Result.RESULT_CODE.*;
import static server.servlet.imps.GenerateZip.ZIP_TEMP_DIR_NAME;

/**
 * Created by user on 2018/12/17.
 * 传递 需要打包下载的文件的相对路径 or URL
 * 生成zip流输出
 * GET  > http://127.0.0.1:8080/downloadZip?zip-name=下载的zip名字.zip&path-list=/defaults/1/ERP.vsdx(需要重命名则添加小括号.vsdx);下一个路径
 * POST >  http://127.0.0.1:8080/downloadZip  header = { "path-list" :  "/defaults/1/ERP.vsdx; /defaults/2.png"  , "zip-name":"可以没有,默认时间戳命名"}
 *
 */
public class DownloadZipStream extends Mservlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-type", "text/html;charset=UTF-8");
        String pathListStr = req.getParameter("path-list");
        String zipName = req.getParameter("zip-name");
        execute(zipName,pathListStr,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-type", "text/html;charset=UTF-8");
        String pathListStr = req.getHeader("path-list");
        String zipName = req.getHeader("zip-name");
        execute(zipName,pathListStr,resp);
    }

    private void execute(String zipName,String pathListStr,HttpServletResponse resp) {

        List<String> pathList = filterData(pathListStr);

        Log4j.info("[ZIP流文件下载]指定文件列表\n\t"+ pathList);

        long time = System.currentTimeMillis();

        try(ZipOutputStream zos = new ZipOutputStream(resp.getOutputStream())) {

            if(pathList.size() == 0) {
                throw new FileNotFoundException("没有指定需要打包的资源列表");
            }

            if (zipName == null){
                zipName = System.nanoTime()+".zip";
            }

            resp.setContentType("application/x-msdownload");
            resp.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(zipName, "UTF-8"));

            for (String path : pathList){

                int index =  path.lastIndexOf("/")+1;

                String fileName = path.substring(index);

                // 处理重命名 例: ERP.vsdx(1234.vsdx)
                String regex = "(?<=\\()[^\\)]+";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(fileName);
                if (matcher.find()) {
                    fileName = matcher.group();

                    regex = "\\(([^)])*\\)";
                    path = path.replaceAll(regex,"");
                }

                zos.putNextEntry(new ZipEntry(fileName));

                try (InputStream in =
                             path.startsWith("http") || path.startsWith("https") ?
                                     (new URL(path).openConnection()).getInputStream():
                                     new FileInputStream(new File(WebServer.rootFolder,  path))){

                    byte[] bytes = new byte[4096];
                    int len;
                    while ((len = in.read(bytes)) > 0) {
                        zos.write(bytes, 0, len);
                    }

                }catch (Exception e){
                    Log4j.info("[ZIP流文件下载] 无效的路径:"+ path+" ,原因:"+ e);
                }
            }
            zos.flush();

        } catch (Exception e) {
            Log4j.error("ZIP流文件下载失败",e);
        }finally {
            Log4j.info("[ZIP流文件下载] 耗时: "+ (System.currentTimeMillis() - time)+" 毫秒");
        }
    }
}
