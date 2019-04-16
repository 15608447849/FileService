package server.servlet.imps;

import bottle.util.FileUtils;
import server.prop.WebProperties;
import server.servlet.beans.result.Result;
import server.servlet.iface.Mservlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * @Author: leeping
 * @Date: 2019/4/1 17:25
 */
public class FileDelete extends Mservlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req,resp);
        //文件删除
        ArrayList<String> list = filterJsonData(req.getHeader("delete-list"));
        ArrayList<String> delList = new ArrayList<>();
        if (list!=null){
            String rootPath = WebProperties.get().rootPath;

            list.forEach(path -> {
                        try{
                            if (FileUtils.deleteFile( rootPath + path)){
                                delList.add(path);
                            }
                        }catch (Exception ignored){ }
                    }
            );
        }
        Result result = new Result();
        if (delList.size()>0){
            result.data = delList;
        }
        writeJson(resp,result);
    }
}
