package server.servlet.imps;

import bottle.threadpool.IOThreadPool;
import bottle.util.Log4j;
import server.prop.WebServer;
import server.servlet.beans.operation.FFMPRG_CMD;
import server.servlet.beans.operation.FileErgodicOperation;
import server.servlet.beans.result.Result;
import server.servlet.iface.Mservlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static server.servlet.beans.operation.OperationUtils.*;
import static server.servlet.beans.result.Result.RESULT_CODE.EXCEPTION;
import static server.servlet.beans.result.Result.RESULT_CODE.SUCCESS;

/**
 * @Author: leeping
 * @Date: 2019/7/29 10:58
 */
public class FileInsideOperation extends Mservlet{

    public interface InsideOperationI{
        String execute(HttpServletRequest resp);
    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-type", "text/html;charset=UTF-8");
        try {
            String cmd = req.getParameter("cmd");
            InsideOperationI exe = (InsideOperationI) Class.forName("server.servlet.imps."+cmd).newInstance();
            String resStr = exe.execute(req);
            writeString(resp,resStr,true);
        } catch (Exception e) {
            writeString(resp,"execute fail , error by " + e,true);
        }
    }

}
