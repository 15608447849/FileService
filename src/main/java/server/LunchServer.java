package server;


import bottle.util.TimeTool;
import server.clear.FileClear;
import server.comm.FileErgodicExecute;
import server.comm.NetWorkUtil;
import server.hwobs.HWOBSAgent;
import server.servlet.beans.ImageOperation;
import server.undertow.WebServer;

import java.util.Date;


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
                if (args[i].startsWith("--net")){
                    NetWorkUtil.main(args);
                    return;
                }
                if (args[i].startsWith("--del:")){
                    String str = args[i].replace("--del:","");
                    String[] arr = str.split(",");

                    new FileErgodicExecute(arr[0], true).setCallback(file -> {
                        boolean flag = file.getName().endsWith(arr[1]);

                        if (flag){
                            System.out.println("删除文件: "+ file +" , 最后修改时间: " + TimeTool.date_yMd_Hms_2Str(new Date(file.lastModified())) +" 删除结果: "+ file.delete());
                        }

                        return true;
                    }).start();

                    return;
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




