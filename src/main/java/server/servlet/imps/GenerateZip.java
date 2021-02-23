package server.servlet.imps;


import bottle.util.EncryptUtil;
import bottle.util.FileTool;
import bottle.util.Log4j;
import bottle.util.TimeTool;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;
import server.servlet.beans.result.Result;
import server.prop.WebServer;
import server.servlet.iface.Mservlet;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;

import static server.servlet.beans.result.Result.RESULT_CODE.*;

/**
 * Created by user on 2018/12/17.
 * 传递 需要打包下载的文件的相对路径
 * 生成zip包 发送zip地址
 */
public class GenerateZip extends Mservlet {

    private static final String ZIP_TEMP_DIR_PREV = "zip_";
    private static final String ZIP_URL_FLAG= "_network_";

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");


    private void addFile(List<File> list, File file) {
        if (file.isFile()){
            list.add(file);
        }else if (file.isDirectory()){
            File[] files = file.listFiles();
            if (files != null && files.length>0){
                for (File f : files){
                    addFile(list,f);
                }
            }
        }
    }


    private File httpUrlToLocalFile(String urlStr){
        //流转文件
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(30*1000);
            try(InputStream in = conn.getInputStream()){
                //写入本地文件
                String suffix = urlStr.substring(urlStr.lastIndexOf("."));

                File tempFile = new File(WebServer.GET_TEMP_FILE_DIR(),ZIP_TEMP_DIR_PREV + ZIP_URL_FLAG + simpleDateFormat.format(new Date()) + suffix);
                try(OutputStream out=new FileOutputStream(tempFile)){
                    byte[] buf = new byte[1024];
                    int len;
                    while((len=in.read(buf))>0){
                        out.write(buf,0,len);
                    }
                    return tempFile;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(conn!=null){
                conn.disconnect();
            }
        }
        return null;
    }

    private List<File> checkPaths(List<String> paths) {

       List<File> list = new ArrayList<>();
       for (String path : paths){
           File file;
           if(path.startsWith("http") || path.startsWith("https")){
               file =  httpUrlToLocalFile(path);
           }else{
               if (!path.startsWith(FileTool.SEPARATOR)) path = FileTool.SEPARATOR + path;// 保证前面有 '/'
               file = new File(WebServer.rootFolder,  path);
           }

           if (file==null || !file.exists()){
               //文件不存在
               continue;
           }

          addFile(list,file);
       }
       return list;
    }



    /**
     *
     * @param paths 需要打包的文件的相对路径 例如: /Music/jcs.msv
     * @return 存放了需要打包文件的临时目录全路径
     */
    private File cpFileListToDir(List<String> paths) throws  Exception{

        String dirPath = WebServer.GET_TEMP_FILE_DIR() + FileTool.SEPARATOR + ZIP_TEMP_DIR_PREV + simpleDateFormat.format(new Date());

        List<File> fileList = checkPaths(paths);

        File dir = new File(dirPath);

        if (fileList.size() > 0){
            if (!dir.exists()) if (!dir.mkdirs()) throw new IllegalArgumentException("无法创建目录: "+ dirPath);//创建目录

            for (File file : fileList) {

                //目标临时存放位置
                File out = new File(dirPath, EncryptUtil.encryption(file.getAbsolutePath())+"_"+file.getName());

                //复制文件
                FileUtils.copyFile(file, out);

                //删除网络下载资源
                if (file.getName().startsWith(ZIP_TEMP_DIR_PREV + ZIP_URL_FLAG)){
                    file.delete();
                }

            }
        }
        return dir;
    }

    /**
     * 文件夹压缩zip
     * @param dir 指定文件夹
     * @return 压缩包相对路径
     */
    private String compressZip(File dir) throws Exception{
            File zipFile = new File(WebServer.GET_TEMP_FILE_DIR() ,  dir.getName() +".zip");

            if (zipFile.exists()) if (!zipFile.delete()) throw new IllegalStateException("无法删除文件: "+ zipFile);

            Project prj = new Project();
            Zip zip = new Zip();
            zip.setProject(prj);
            zip.setDestFile(zipFile);
            FileSet fileSet = new FileSet();
            fileSet.setProject(prj);
            fileSet.setDir(dir);
            zip.addFileset(fileSet);
            zip.execute();

            //返回压缩包路径
            return zipFile.getCanonicalPath();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-type", "text/html;charset=UTF-8");
        Result result = new Result().value(UNKNOWN);
        List<String> pathList = filterData(req.getHeader("path-list"));
        Log4j.info("ZIP-文件列表: "+ pathList);
        try {
            if(pathList.size() == 0) {
                throw new FileNotFoundException("没有指定需要打包的资源列表");
            }
            File compressDirt = cpFileListToDir(pathList);

            if (!compressDirt.exists()){
                throw new FileNotFoundException("没有存在一个可批量打包的文件");
            }

            String zipLocalPath  = compressZip(compressDirt);

            //完成 删除目录
            boolean isDelete  = compressDirt.delete();
            Log4j.info("已生成ZIP文件: " + zipLocalPath +" ,删除打包目标目录: "+ compressDirt + (isDelete?" 成功":" 失败"));

            //返回ZIP包URL
            result.data = zipLocalPath.replace(WebServer.rootFolderStr,WebServer.domain);
            result.value(SUCCESS);
        } catch (Exception e) {
            result.data = e.getMessage();
            result.value(PARAM_ERROR);
        }
        writeJson(resp,result);
    }
}
