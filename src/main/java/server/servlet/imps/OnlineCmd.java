package server.servlet.imps;

import bottle.util.TimeTool;
import server.clear.FileClear;
import server.comm.RuntimeUtil;
import server.hwobs.HWOBSAgent;
import server.hwobs.HWOBSErgodic;
import server.hwobs.HWOBSServer;
import server.undertow.AccessControlAllowOriginFilter;
import server.undertow.CustomResourceManager;
import server.undertow.CustomServlet;
import server.undertow.ServletAnnotation;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@ServletAnnotation(name = "服务在线命令行",path = "/cmd")
public class OnlineCmd extends CustomServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-type", "text/html;charset=UTF-8");
        String hwobs = req.getParameter("hwobs");
        String sys = req.getParameter("sys");

        String respStr = "OK";

        try {
            if (hwobs!=null){
                // 开始遍历
                if(hwobs.equals("start")){
                    HWOBSErgodic.isEnable = true;
                    new Thread(HWOBSErgodic::localErgodic).start();
                }

                // 停止遍历
                if (hwobs.equals("stop")){
                    HWOBSErgodic.isEnable = false;
                }

                // 0 标识暂停上传
                if(hwobs.startsWith("limit:")){
                    HWOBSAgent.max_upload_size = Integer.parseInt(hwobs.replace("limit:",""));
                }

                // 间隔提交时间
                if(hwobs.startsWith("sleep:")){
                    HWOBSAgent.force_interval_sec = Integer.parseInt( hwobs.replace("sleep:",""));
                }

                // 上传线程数
                if(hwobs.startsWith("threads:")){
                    HWOBSServer.uploadSegThreads = Integer.parseInt( hwobs.replace("threads:",""));
                }

                // 分段大小
                if(hwobs.startsWith("seg:")){
                    HWOBSServer.uploadSegSize = Long.parseLong( hwobs.replace("seg:",""));
                }
            }

            if (sys!=null){
                // 接入信息日志输出
                if (sys.startsWith("access:")){
                    try {
                        AccessControlAllowOriginFilter.isPrintAccess =  Boolean.parseBoolean( sys.replace("access:",""));
                    } catch (Exception ignored) { }
                }
                // 重置缓存
                if (sys.startsWith("cache:")){
                    try {
                        String[] args = sys.replace("cache:","").split(",");
                        long len = Long.parseLong(args[0]);
                        int sec = Integer.parseInt(args[1]);
                        CustomResourceManager.resetCache(len,sec);
                    } catch (Exception ignored) { }
                }

            }

        } catch (Exception e) {
            respStr = String.valueOf(e);
        }

        writeString(resp, respStr,true);
    }


}
