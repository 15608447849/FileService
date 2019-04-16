package server.servlet.imps;

import bottle.util.FileUtils;
import bottle.util.Log4j;
import bottle.util.StringUtils;
import server.prop.WebProperties;
import server.servlet.beans.operation.FileErgodicOperation;
import server.servlet.beans.result.Result;
import server.servlet.iface.Mservlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

import static server.servlet.beans.result.Result.RESULT_CODE.EXCEPTION;
import static server.servlet.beans.result.Result.RESULT_CODE.SUCCESS;

public class FileErgodic extends Mservlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
        Result result = new Result();
        String path = req.getHeader("specify-path");
        String sub = req.getHeader("ergodic-sub");
        boolean isSub = true;
        if (!StringUtils.isEmpty(sub)){
            try{
                isSub = Boolean.parseBoolean(sub);
            }catch (Exception ignored){}
        }
        if (StringUtils.isEmpty(path)) path = FileUtils.SEPARATOR;
        try {
            path = WebProperties.get().rootPath + checkDirPath(path);
            result.data = new FileErgodicOperation(path,isSub).start();
            Log4j.info("遍历目录:"+ path +" list: "+ result.data);
            result.value(SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
            result.data = new ArrayList<>();
            result.value(EXCEPTION,e.toString());
        }
        writeJson(resp,result);
    }
}
