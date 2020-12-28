package server.servlet.imps;


import bottle.util.FileTool;
import bottle.util.StringUtil;
import bottle.util.TimeTool;
import server.HuaWeiOBS.HWOBSServer;
import server.prop.WebServer;
import server.servlet.beans.operation.SysUtils;
import server.servlet.iface.Mservlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import static server.servlet.beans.operation.RuntimeUtils.*;

// 任意客户端日志记录功能
public class LogAppend extends Mservlet {

    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

    private static LinkedBlockingQueue<String[]> queue = new LinkedBlockingQueue<>();

    private static Thread thread = new Thread(() -> {
        while (true){
            try {
                execute(queue.take());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    });

    static {
        thread.setDaemon(true);
        thread.start();
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req,resp);
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-type", "text/html;charset=UTF-8");
        String clientType = req.getParameter("clientType");
        String logName = req.getParameter("logName");
        String content = req.getParameter("content");
        if (!StringUtil.isEmpty(clientType,logName,content)){
            queue.offer(new String[]{clientType,logName,content});
        }
        writeString(resp, "OK",true);
    }

    private static void execute(String[] arr){
        String clientType = arr[0];
        String logName = arr[1];
        String content = arr[2];
        FileTool.writeStringToFile(
                TimeTool.date_yMd_Hms_2Str(new Date()) +"\t" +content+"\n",
                WebServer.rootFolderStr + "/ClientLogs/"+clientType+"/"+simpleDateFormat.format(new Date()),
                logName, true);
    }
}
