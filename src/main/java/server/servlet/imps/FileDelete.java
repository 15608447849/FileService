package server.servlet.imps;

import bottle.util.FileTool;
import bottle.util.Log4j;
import server.hwobs.HWOBSServer;
import server.undertow.ServletAnnotation;
import server.undertow.WebServer;
import server.undertow.ServletResult;
import server.undertow.CustomServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

import static server.undertow.ServletResult.RESULT_CODE.SUCCESS;

/**
 * @Author: leeping
 * @Date: 2019/4/1 17:25
 */

@ServletAnnotation(name = "删除文件",path = "/delete")
public class FileDelete extends CustomServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-type", "text/html;charset=UTF-8");
        //文件删除
        ArrayList<String> list = filterJsonData(req.getHeader("delete-list"));
        Log4j.info("删除文件:\n\t"+list);

        if (list!=null){
            list.removeIf(val -> val == null || val.length() == 0 || val.equals("null"));
            list.forEach(path -> FileTool.deleteFileOrDir(  WebServer.rootFolderStr + path));
            HWOBSServer.deleteFile(list);
        }
        writeJson(resp,new ServletResult().value(SUCCESS));
    }
}
