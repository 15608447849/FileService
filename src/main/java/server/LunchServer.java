package server;

import server.HuaWeiOBS.OBSUploadPoolUtil;
import server.prop.WebServer;
import server.servlet.beans.operation.FileClear;
import server.servlet.beans.operation.ImageOperation;


import static io.undertow.servlet.Servlets.servlet;


/**
 * Created by lzp on 2017/5/13.
 * 容器入口
 */
public class LunchServer {
    public static void main(String[] args) throws Exception {
        //开启web文件服务器
        WebServer.startWebServer();
    }
}




