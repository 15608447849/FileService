package server.servlet.imps;

import bottle.util.FileTool;
import bottle.util.Log4j;
import bottle.util.StringUtil;
import server.prop.WebServer;
import server.servlet.beans.operation.FileErgodicOperation;
import server.servlet.beans.result.Result;
import server.servlet.iface.Mservlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static server.servlet.beans.result.Result.RESULT_CODE.EXCEPTION;
import static server.servlet.beans.result.Result.RESULT_CODE.SUCCESS;

// 文件遍历 查询是否存在文件
public class FileErgodic extends Mservlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-type", "text/html;charset=UTF-8");
        Result result = new Result();
        try {
            String path = req.getHeader("specify-path");
            String sub = req.getHeader("ergodic-sub");
            String filter = req.getHeader("filter-array");

            boolean isSub = true;
            if (!StringUtil.isEmpty(sub)){
                isSub = Boolean.parseBoolean(sub);
            }

            String[] filterArrays = null;
            if (!StringUtil.isEmpty(filter)){
                filterArrays = filter.split(",");
            }

            if (StringUtil.isEmpty(path)) throw new IllegalAccessException("请设置需要遍历的目录路径");

            path = path.replace(WebServer.domain,"");
            path = WebServer.rootFolderStr + checkDirPath(path);

            //判断是否是一个文件
            File file = new File(path);
            if (file.isFile()){
                result.data = true;
            }else if (file.isDirectory()){

                FileErgodicOperation op = new FileErgodicOperation(path,isSub);
                if (filterArrays!=null && filterArrays.length>0){
                    String[] finalFilterArrays = filterArrays;
                    op.setCallback(new FileErgodicOperation.Callback() {
                        @Override
                        public boolean filterFile(File file) {
                            String fileName = file.getName();
                            for (String str: finalFilterArrays){
                                if (fileName.contains(str)) return true;
                            }
                            return false;
                        }
                    });
                }

                result.data = op.start();
            }else{
                result.data = false;
            }
            result.value(SUCCESS);
        } catch (Exception e) {
            Log4j.error("文件服务错误",e);
            result.data = new ArrayList<>();
            result.value(EXCEPTION,e.toString());
        }
        writeJson(resp,result);
    }

}
