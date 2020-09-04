package server.servlet.imps;

import bottle.util.FileTool;
import server.prop.WebProperties;
import server.servlet.beans.result.Result;
import server.servlet.iface.Mservlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

import static server.servlet.beans.result.Result.RESULT_CODE.SUCCESS;

/**
 * @Author: leeping
 * @Date: 2019/4/1 17:25
 */
public class FileDelete extends Mservlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //文件删除
        ArrayList<String> list = filterJsonData(req.getHeader("delete-list"));
        if (list!=null){
            list.forEach(path -> FileTool.deleteFileOrDir(  WebProperties.rootPath + path));
        }
        writeJson(resp,new Result().value(SUCCESS));
    }
}
