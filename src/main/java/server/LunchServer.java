package server;


import bottle.util.Log4j;
import server.prop.WebServer;



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




