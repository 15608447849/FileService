package server.servlet.imps;

import bottle.util.Log4j;
import bottle.util.StringUtil;
import server.hwobs.HWOBSServer;
import server.undertow.ServletAnnotation;
import server.undertow.WebServer;
import server.comm.FileErgodicExecute;
import server.undertow.ServletResult;
import server.undertow.CustomServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static server.undertow.ServletResult.RESULT_CODE.EXCEPTION;
import static server.undertow.ServletResult.RESULT_CODE.SUCCESS;

// 文件遍历 查询是否存在文件
@ServletAnnotation(name = "遍历文件列表",path = "/ergodic")
public class FileErgodic extends CustomServlet {


    private static boolean isFilter(String[] strArray, String fn){
        if (strArray!=null && strArray.length>0){
            for (String str: strArray){
                if (fn.contains(str)) return true;
            }
        }
        return false;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-type", "text/html;charset=UTF-8");
        ServletResult result = new ServletResult();

        try {
            String path = req.getHeader("specify-path");// 指定目录
            String sub = req.getHeader("ergodic-sub");// true-需要遍历子目录
            String filter = req.getHeader("filter-array");// 过滤后缀

            boolean isSub = true;
            if (!StringUtil.isEmpty(sub)){
                isSub = Boolean.parseBoolean(sub);
            }

            String[] filterArray = null;
            if (!StringUtil.isEmpty(filter)){
                filterArray = filter.split(",");
            }

            if (StringUtil.isEmpty(path)) throw new IllegalAccessException("请设置需要遍历的目录路径");

            path = path.replace(WebServer.domain,"");

            boolean checkDict= path.endsWith("/");

            String remotePath = checkDirPath(path);
            String localPath = WebServer.rootFolderStr + remotePath;


            if (checkDict){
                //检查目录 返回 子文件列表
                List<String> list = new ArrayList<>();
                result.setData(list);

                File dict = new File(localPath);
                if (dict.exists() && dict.isDirectory()){
                    // 遍历本地目录
                    list.addAll( new FileErgodicExecute( path ,isSub ).start(true ) );
                }
                if (HWOBSServer.enable){
                    // 遍历远程目录
                   Set<String> set = HWOBSServer.ergodicDirectory(remotePath,isSub);
                    list.addAll(new ArrayList<>(set));
                }

                Set<String> duplicate = new HashSet<>();
                // 尝试过滤和去重
                ListIterator<String> iterator = list.listIterator();
                while (iterator.hasNext()){
                    String str = iterator.next();
                    // 去重
                    if (!duplicate.add(str)){
                        iterator.remove();
                    }
                    // 过滤
                    int index = str.indexOf("/");
                    if (index>0){
                        str = str.substring(index+1);
                    }
                    if (isFilter(filterArray,str)){
                        iterator.remove();
                    }
                }


            }else {
                //检查文件 返回 true or false

                //判断本地文件
                File file = new File(localPath);
                if (file.exists() && file.isFile()){
                    result.setData(true);
                }else {
                    // 判断obs对象
                    if (HWOBSServer.enable){
                        boolean isExist = HWOBSServer.existFile(remotePath);
                        result.setData(isExist);
                    }
                }

            }

        } catch (Exception e) {
            Log4j.error("文件服务错误",e);
            result.setError(e.getMessage());
        }
        writeJson(resp,result);
    }

}
