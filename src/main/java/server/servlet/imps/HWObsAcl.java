package server.servlet.imps;


import bottle.util.StringUtil;
import server.hwobs.HWOBSServer;
import server.undertow.CustomServlet;
import server.undertow.ServletAnnotation;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// 回复当前文件服务器的信息/状态

@ServletAnnotation(name = "华为OBS对象ACL控制",path = "/hwobsacl")
public class HWObsAcl extends CustomServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-type", "text/html;charset=UTF-8");

        String action = req.getParameter("action");
        String path = req.getParameter("path");
        System.out.println(action);
        System.out.println(path);
        try {
            if (StringUtil.isEmpty(action,path)) throw new IllegalArgumentException("非法请求");

            if (Integer.parseInt(action) == 0){
                // 私有化文件
                boolean flag = HWOBSServer.setFileAclPrivate(path);
                writeString(resp, flag ?"SUCCESS":"FAIL",true);

            }else if (Integer.parseInt(action) == 1){
                // 授权文件10秒内公开访问
                String url = HWOBSServer.setFileAclTempAccess(path);
                writeString(resp, url,true);

            }throw new IllegalArgumentException("无法识别请求");

        } catch (Exception e) {
            writeString(resp,e.getMessage(),true);
        }

    }


}
