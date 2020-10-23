package server;

import server.HuaWeiOBS.OBSUploadPoolUtil;
import server.prop.WebServer;
import server.servlet.beans.operation.FileClearThread;
import server.servlet.beans.operation.ImageOperation;
import server.servlet.imps.ImageHandle;


import static io.undertow.servlet.Servlets.servlet;


/**
 * Created by lzp on 2017/5/13.
 * 容器入口
 */
public class LunchServer {



    public static void main(String[] args) throws Exception {
        ImageOperation.start();
        OBSUploadPoolUtil.start();
        //文件清理线程
        FileClearThread.get().start();
        //开启web文件服务器
        WebServer.startWebServer();

    }




}




