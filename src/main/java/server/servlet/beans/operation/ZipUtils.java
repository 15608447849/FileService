package server.servlet.beans.operation;

import bottle.util.FileTool;
import bottle.util.Log4j;
import server.prop.WebServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ZipUtils {

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    /* 添加文件到指定列表 */
    private static void addFile(List<File> list, File file) {
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

    /* 下载指定url的文件到本地具体位置 */
    private static File httpUrlToLocalFile(String urlStr,String storageDir){
        //流转文件
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(30*1000);
            String suffix = urlStr.substring(urlStr.lastIndexOf("."));
            String fileName =  simpleDateFormat.format(new Date()) + suffix;

            File localFile = new File(storageDir,fileName);
            try(InputStream in = conn.getInputStream()){
                try(OutputStream out=new FileOutputStream(localFile)){
                    byte[] buf = new byte[1024];
                    int len;
                    while((len=in.read(buf))>0){
                        out.write(buf,0,len);
                    }
                    return localFile;
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

    /* 检查路径是否有效 */
    public static List<File> checkPaths(List<String> paths,String storageDir) {
        List<File> list = new ArrayList<>();

        for (String path : paths){
            File file = null;
            if(path.startsWith("http") || path.startsWith("https")){
                if (storageDir!=null){
                    file =  httpUrlToLocalFile(path,storageDir);
                }
            }else{
                if (!path.startsWith(FileTool.SEPARATOR)) path = FileTool.SEPARATOR + path;// 保证前面有 '/'
                file = new File(WebServer.rootFolder,  path);
            }
            if (file==null || !file.exists()){
                //文件不存在
                Log4j.info("无效的路径: " + path);
                continue;
            }
            addFile(list,file);
        }

        return list;
    }

}
