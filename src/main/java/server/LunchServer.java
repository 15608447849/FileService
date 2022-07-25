package server;


import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.util.Log4j;
import server.clear.FileClear;
import server.hwobs.HWOBSUpload;
import server.servlet.beans.ImageOperation;
import server.servlet.imps.FileUpLoad;
import server.undertow.WebServer;

import java.io.File;


/**
 * Created by lzp on 2017/5/13.
 * 容器入口
 */

public class LunchServer {

    public static void main(String[] args) throws Exception {

        //文件清理线程
        FileClear.start();
        //图片处理
        ImageOperation.start();
        //华为OBS
        HWOBSUpload.start();
        //开启web文件服务器
        WebServer.startWebServer();

        if (args.length>=1){
            if (args[0].equals("--scan.file.upload.obs")){
                HWOBSUpload.localErgodic();
            }
        }
    }
}




