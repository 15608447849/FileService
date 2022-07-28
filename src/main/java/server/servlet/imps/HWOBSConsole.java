package server.servlet.imps;



import server.hwobs.HWOBSAgent;
import server.hwobs.HWOBSErgodic;
import server.hwobs.HWOBSServer;
import server.undertow.CustomServlet;
import server.undertow.ServletAnnotation;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@ServletAnnotation(name = "华为对象存储",path = "/hwobs")
public class HWOBSConsole extends CustomServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-type", "text/html;charset=UTF-8");
        String cmd = req.getParameter("cmd");

        String respStr = "OK";

        try {
            // 开始遍历
            if(cmd.equals("start")){
                HWOBSErgodic.localErgodic();
                HWOBSErgodic.isEnable = true;
            }

            // 停止遍历
            if (cmd.equals("stop")){
                HWOBSErgodic.isEnable = false;
            }

            // 0 标识暂停上传
            if(cmd.startsWith("limit:")){
                HWOBSAgent.max_upload_size = Integer.parseInt(cmd.replace("limit:",""));
            }

            // 间隔提交时间
            if(cmd.startsWith("sleep:")){
                HWOBSAgent.force_interval_sec = Integer.parseInt( cmd.replace("sleep:",""));
            }

            if (cmd.equals("info")){
                respStr= "已扫描文件数: "+ HWOBSErgodic.current_ergodic +" 添加OBS队列数: "+ HWOBSErgodic.current_ergodic_add_upload_queue + " 待上传队列大小: "+ HWOBSAgent.getQueueSize();
            }
        } catch (Exception e) {
            respStr = String.valueOf(e);
        }

        writeString(resp, respStr,true);
    }

}
