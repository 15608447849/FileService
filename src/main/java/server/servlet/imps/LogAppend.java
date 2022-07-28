package server.servlet.imps;


import bottle.util.FileTool;
import bottle.util.StringUtil;
import bottle.util.TimeTool;
import server.undertow.ServletAnnotation;
import server.undertow.WebServer;
import server.undertow.CustomServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

// 任意日志记录生成文件

//@ServletAnnotation(name = "日志记录",path = "/logAppend")
public class LogAppend extends CustomServlet {
    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
    private static final LinkedBlockingQueue<String[]> queue = new LinkedBlockingQueue<>();

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
        FileTool.writeStringToFile(TimeTool.date_yMd_Hms_2Str(new Date()) +"\t" +content+"\n",
                WebServer.rootFolderStr + "/recodeLogs/"+clientType+"/"+simpleDateFormat.format(new Date()), logName, true);
    }
}
