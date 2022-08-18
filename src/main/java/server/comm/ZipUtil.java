package server.comm;

import bottle.util.FileTool;
import bottle.util.Log4j;
import server.hwobs.HWOBSAgent;
import server.undertow.WebServer;

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

public class ZipUtil {


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
            conn.setConnectTimeout(60*1000);

            String fileName =  System.currentTimeMillis() + "-" +urlStr.substring(urlStr.lastIndexOf("/")+1);

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

    /* 检查路径是否有效
    * 本地或远程获取文件(下载)
    * */
    public static List<File> checkPaths(List<String> paths,String storageDir) {
        List<File> list = new ArrayList<>();

        for (String path : paths){
            Log4j.info("[ZIP] 目标路径 : "+ path);
            File file = null;
            // 远程url
            if(path.startsWith("http") || path.startsWith("https")){
                if (storageDir!=null){
                    file =  httpUrlToLocalFile(path,storageDir);
                }
            }else{
                // 本地文件
                if (!path.startsWith(FileTool.SEPARATOR)) path = FileTool.SEPARATOR + path;// 保证前面有 '/'
                file = new File(WebServer.rootFolder,  path);

                if (!file.exists()){
                    Log4j.info("[ZIP] 文件不存在 : "+ file);
                    // 尝试通过OBS获取
                    String remoteUrl = HWOBSAgent.getFileURL(path);
                    if (remoteUrl!=null){
                        file =  httpUrlToLocalFile(remoteUrl,storageDir);
                    }
                }

            }

            if (file!=null && file.exists() && file.isFile() && file.length()>0){
                // 加入打包列表
                addFile(list,file);
            }

        }

        return list;
    }

}
