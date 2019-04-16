package server.servlet.iface;

import bottle.util.FileUtils;
import bottle.util.Log4j;
import bottle.util.StringUtils;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;



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
 * Created by Administrator on 2017/5/31.
 */
public class Mservlet extends javax.servlet.http.HttpServlet {


    private final String PARAM_SEPARATOR = ";";

    //跨域
    protected void filter(HttpServletRequest req,HttpServletResponse resp) {
//        http://www.ruanyifeng.com/blog/2016/04/cors.html
        try {
            resp.setCharacterEncoding("UTF-8");
            req.setCharacterEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Access-Control-Allow-Methods","GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS");
        resp.addHeader("Access-Control-Allow-Headers","x-requested-with"); // 允许x-requested-with请求头
        resp.addHeader("Access-Control-Allow-Headers",
                        "specify-path,specify-filename,save-md5,is-sync,tailor-list," +
                                "path-list,excel-path,ergodic-sub,"+
                                "delete-list"
        );
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        filter(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        filter(req,resp);

    }

    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        filter(req,resp);
        super.doOptions(req, resp);
    }

    protected ArrayList<String> filterData(String data) {
        ArrayList<String> dataList = new ArrayList<>();
        try {
            if (!StringUtils.isEmpty(data)) { // 不为空
                data = URLDecoder.decode(data, "UTF-8"); // url解码
                if (data.contains(PARAM_SEPARATOR)) {
                    String[] pathArray = data.split(PARAM_SEPARATOR);
                    Collections.addAll(dataList, pathArray);
                } else {
                    dataList.add(data);// 如果只有一个
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return dataList;
    }


    protected ArrayList<String> filterJsonData(String json){
        try {
            if (!StringUtils.isEmpty(json)) { // 不为空
                json = URLDecoder.decode(json, "UTF-8"); // url解码
                return new Gson().fromJson(json,new TypeToken<ArrayList<String>>(){}.getType());
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
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
            path = path.replace("\\\\", FileUtils.SEPARATOR);
            if (!path.startsWith(FileUtils.SEPARATOR)) path = FileUtils.SEPARATOR + path;//保证前面有 '/'
            if (!path.endsWith(FileUtils.SEPARATOR)) path += FileUtils.SEPARATOR; //后面保证 '/'
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
            e.printStackTrace();
        }
    }

    protected void writeJson(HttpServletResponse resp, Object o) {
        writeString(resp, new Gson().toJson(o), true);
    }

}
