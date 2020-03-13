package server.servlet.imps;

import bottle.util.FileTool;
import bottle.util.StringUtil;
import server.prop.WebProperties;
import server.servlet.beans.operation.FileErgodicOperation;
import server.servlet.beans.result.Result;
import server.servlet.iface.Mservlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
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
        if (!StringUtil.isEmpty(sub)){
            try{
                isSub = Boolean.parseBoolean(sub);
            }catch (Exception ignored){}
        }
        if (StringUtil.isEmpty(path)) path = FileTool.SEPARATOR;
        try {
            path = WebProperties.rootPath + checkDirPath(path);
            //判断是否是一个文件
            File file = new File(path);
            if (file.isFile()){
                result.data = true;
//                Log4j.info("存在文件:"+ path);
            }else if (file.isDirectory()){
                result.data = new FileErgodicOperation(path,isSub).start();
//                Log4j.info("遍历目录:"+ path +" list: "+ result.data);
            }else{
                result.data = false;
            }
            result.value(SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
            result.data = new ArrayList<>();
            result.value(EXCEPTION,e.toString());
        }
        writeJson(resp,result);
    }

}
