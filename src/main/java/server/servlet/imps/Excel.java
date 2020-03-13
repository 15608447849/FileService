package server.servlet.imps;


import bottle.util.StringUtil;
import server.prop.WebProperties;
import server.servlet.beans.operation.ExcelReaderOperation;
import server.servlet.beans.result.Result;
import server.servlet.iface.Mservlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static server.servlet.beans.result.Result.RESULT_CODE.*;

public class Excel extends Mservlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
        Result result = new Result();
        String path = req.getHeader("excel-path");
        if (!StringUtil.isEmpty(path)){
            path = checkDirPath(path);
            path = WebProperties.rootPath + path;
            try {
                result.data = new ExcelReaderOperation(path).start();
                result.value(SUCCESS);
            } catch (Exception e) {
                e.printStackTrace();
                result.value(EXCEPTION);
            }
        }else{
            result.value(PARAM_ERROR);
        }
        writeJson(resp,result);
    }
}
