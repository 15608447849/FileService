package server;


import server.clear.FileClear;
import server.hwobs.HWOBSAgent;
import server.servlet.beans.ImageOperation;
import server.undertow.WebServer;


/**
 * Created by lzp on 2017/5/13.
 * 容器入口
 */

public class LunchServer {



    public static void main(String[] args) throws Exception {


        if (args!=null){
            for (int i = 0 ; i<args.length;i+=2){
                if (args[i].startsWith("--web.port")){
                    WebServer.initWebServer( Integer.parseInt(args[i+1]));
                }
            }
        }

        //文件清理线程
        FileClear.start();
        //图片处理
        ImageOperation.start();
        //华为OBS
        HWOBSAgent.start();
        //开启web文件服务器
        WebServer.startWebServer();
    }
}




