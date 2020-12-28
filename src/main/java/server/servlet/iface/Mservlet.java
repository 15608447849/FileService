package server.servlet.iface;

import bottle.util.FileTool;
import bottle.util.Log4j;
import bottle.util.StringUtil;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import server.prop.WebServer;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by lzp on 2017/5/31.
 */
public class Mservlet extends javax.servlet.http.HttpServlet {



    private final static String PARAM_SEPARATOR = ";";

    protected ArrayList<String> filterData(String data) {
        ArrayList<String> dataList = new ArrayList<>();
        try {
            if (!StringUtil.isEmpty(data)) { // 不为空
                data = URLDecoder.decode(data, "UTF-8"); // url解码
                if (data.contains(PARAM_SEPARATOR)) {
                    String[] pathArray = data.split(PARAM_SEPARATOR);
                    Collections.addAll(dataList, pathArray);
                } else {
                    dataList.add(data);// 如果只有一个
                }
            }
        } catch (UnsupportedEncodingException e) {
            Log4j.error("文件服务错误",e);
        }
        return dataList;
    }


    protected ArrayList<String> filterJsonData(String json){
        try {
            if (!StringUtil.isEmpty(json)) { // 不为空
                json = URLDecoder.decode(json, "UTF-8"); // url解码
                return new Gson().fromJson(json,new TypeToken<ArrayList<String>>(){}.getType());
            }
        } catch (UnsupportedEncodingException e) {
            Log4j.error("文件服务错误",e);
        }
        return null;
    }

    protected ArrayList<String> checkDirPathByList(ArrayList<String> pathList){
        if (pathList.size()>0){
            for(int i=0;i<pathList.size();i++){
                pathList.set(i,checkDirPath(pathList.get(i)));
            }
        }
        return pathList;
    }

    //检测目录路径是否正确
    protected String checkDirPath(String path) {
            if (path.startsWith("..")) path = path.replace("..","");
            if (path.startsWith(".")) path = path.replace(".","");

            path = path.replace("\\\\", FileTool.SEPARATOR);
            if (!path.startsWith(FileTool.SEPARATOR)) path = FileTool.SEPARATOR + path;//保证前面有 '/'
            if (!path.endsWith(FileTool.SEPARATOR)) path += FileTool.SEPARATOR; //后面保证 '/'
            return path;
    }

    protected <T> T getJsonObject(HttpServletRequest req, String headerKey, Class<T> clazzType) throws JsonSyntaxException {
        final String json = req.getHeader(headerKey);
        if (json != null) {
            return new Gson().fromJson(json, clazzType);
        }
        return null;
    }

    protected void writeString(HttpServletResponse resp, String str, boolean isClose) {
        try {
            PrintWriter out = resp.getWriter();
            out.write(str);
            out.flush();
            if (isClose) out.close();
        } catch (IOException e) {
            Log4j.error("文件服务错误",e);
        }
    }

    protected void writeJson(HttpServletResponse resp, Object o) {
        writeString(resp, new Gson().toJson(o), true);
    }

}
