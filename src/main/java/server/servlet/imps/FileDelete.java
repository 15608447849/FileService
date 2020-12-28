package server.servlet.imps;

import bottle.util.FileTool;
import bottle.util.Log4j;
import server.HuaWeiOBS.HWOBSServer;
import server.prop.WebServer;
import server.servlet.beans.result.Result;
import server.servlet.iface.Mservlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

import static server.servlet.beans.result.Result.RESULT_CODE.SUCCESS;

/**
 * @Author: leeping
 * @Date: 2019/4/1 17:25
 */
public class FileDelete extends Mservlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-type", "text/html;charset=UTF-8");
        //文件删除
        ArrayList<String> list = filterJsonData(req.getHeader("delete-list"));
        Log4j.info("删除文件:\n\t"+list);
        if (list!=null){
            list.forEach(path -> FileTool.deleteFileOrDir(  WebServer.rootFolderStr + path));
            HWOBSServer.deleteFile(list);
        }
        writeJson(resp,new Result().value(SUCCESS));
    }
}
