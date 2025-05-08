package server.servlet.imps;


import bottle.tuples.Tuple2;
import bottle.util.TimeTool;
import server.clear.FileClear;
import server.comm.RuntimeUtil;
import server.comm.SysUtil;
import server.hwobs.HWOBSAgent;
import server.hwobs.HWOBSErgodic;
import server.hwobs.HWOBSServer;
import server.undertow.CustomResourceManager;
import server.undertow.CustomServlet;
import server.undertow.ServletAnnotation;
import server.undertow.WebServer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.util.*;

import static server.comm.RuntimeUtil.*;
import static server.undertow.AccessControlAllowOriginFilter.*;

// 返回目标外网IP

//@ServletAnnotation(name = "首页",path = "/")
public class Index extends CustomServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-type", "text/html;charset=UTF-8");

        //服务器域名
        String Host = req.getHeader("Host");
        //WEB应用IP（127.0.0.1）
        String getRemoteAddr = req.getRemoteAddr();

        //nginx 来访者公网IP
        String Xrealip = req.getHeader("X-Real-IP");
        //nginx 来访者公网IP
        String XForwardedFor = req.getHeader("X-Forwarded-For");


        writeString(resp,Host+"\t"+Xrealip+"\t"+XForwardedFor+"\t"+getRemoteAddr+"\n",true);
    }


}
