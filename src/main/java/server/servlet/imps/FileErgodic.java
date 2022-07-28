package server.servlet.imps;

import bottle.util.GoogleGsonUtil;
import bottle.util.Log4j;
import bottle.util.StringUtil;
import server.hwobs.HWOBSAgent;
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

import static server.comm.FilePathUtil.checkDirPath;
import static server.comm.FilePathUtil.checkFilePath;
import static server.comm.SuffixConst.*;

// 文件遍历 查询是否存在文件
@ServletAnnotation(name = "遍历文件列表",path = "/ergodic")
public class FileErgodic extends CustomServlet {


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-type", "text/html;charset=UTF-8");
        ServletResult result = new ServletResult();

        try {
            String filePath = req.getHeader("specify-file");// 指定文件
            String dictPath = req.getHeader("specify-path");// 指定目录
            String sub = req.getHeader("ergodic-sub");// true-需要遍历子目录
            String filterSuffix = req.getHeader("filter-array");// 过滤后缀

            boolean isSub = false;
            if (!StringUtil.isEmpty(sub)){
                isSub = Boolean.parseBoolean(sub);
            }

            if (filePath!=null && filePath.length()>0){
                result.setData(ergodicFile(filePath));
            } else if (dictPath!=null && dictPath.length()>0){
                result.setData(ergodicFolder(dictPath,isSub,filterSuffix));
            }else {
                throw new IllegalAccessException("请指定需要遍历的目录路径或文件");
            }

        } catch (Exception e) {
            Log4j.error("文件服务错误",e);
            result.setError(e.getMessage());
        }
        writeJson(resp,result);
    }

    private Object ergodicFile(String path) {
        String remotePath = checkFilePath(path);
        String localPath = WebServer.rootFolderStr + remotePath;
        boolean isExist = false;

        try {
            //判断本地文件
            File file = new File(localPath);
            isExist = file.exists() && file.isFile();
//            Log4j.info("遍历 检查LOC文件: " + localPath +" file.exists() && file.isFile() = "+ isExist);

            if (!isExist){
                // 判断obs对象
                isExist = HWOBSAgent.existRemoteFile(remotePath);
//                Log4j.info("遍历 检查OBS对象: " + remotePath +" isExist = "+ isExist);
            }

            Log4j.info("遍历 指定文件: " + remotePath +" 是否存在: " + isExist );

        } catch (Exception e) {
            Log4j.error("文件是否存在 "+ path,e);
        }

        return isExist;
    }

    private List<String> ergodicFolder(String path, boolean isSub, String filterSuffix) {
        String remotePath = checkDirPath(path);
        String localPath = WebServer.rootFolderStr + remotePath;

        int localFileSize = 0;
        int remoteFileSize = 0;

        //检查目录 返回 子文件列表
        List<String> respList = new ArrayList<>();

        try {
            File dict = new File(localPath);
            if (dict.exists() && dict.isDirectory()){
                // 遍历本地目录
                List<String> list =  new FileErgodicExecute( localPath ,isSub ).start( true );
                localFileSize = list.size();
                respList.addAll(list );
            }

            // 遍历远程目录
            List<String> list_remote = HWOBSAgent.ergodicDirectory(remotePath,isSub,true);
            remoteFileSize = list_remote.size();
            respList.addAll(list_remote);

            Set<String> duplicate = new HashSet<>();
            // 去重及过滤
            ListIterator<String> iterator = respList.listIterator();
            while (iterator.hasNext()){
                String str = iterator.next();
                if (!duplicate.add(str)){ // 去重
                    iterator.remove();
                }else if (isErgodicNeedFilterSuffix(str,filterSuffix)){ // 过滤
                    iterator.remove();
                }
            }

            if (respList.size()>0){
                Log4j.info("遍历 "+ remotePath +" OBS 文件个数: " + remoteFileSize +
                        " , LOC 文件个数: " + localFileSize +
                        " , 返回文件列表: " + GoogleGsonUtil.javaBeanToJson(respList) );
            }

        } catch (Exception e) {
            Log4j.error("文件夹遍历 "+ path,e);
        }

        return respList;

    }

}
