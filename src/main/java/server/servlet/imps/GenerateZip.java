package server.servlet.imps;


import bottle.util.EncryptUtil;
import bottle.util.FileTool;
import bottle.util.Log4j;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;
import server.undertow.ServletAnnotation;
import server.undertow.ServletResult;
import server.undertow.WebServer;
import server.undertow.CustomServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

import static server.comm.ZipUtil.checkPaths;
import static server.undertow.ServletResult.RESULT_CODE.*;

/**
 * Created by user on 2018/12/17.
 * 传递 需要打包下载的文件的相对路径
 * 生成zip包 发送zip地址
 */
@ServletAnnotation(name = "指定文件列表生成ZIP",path = "/zip")
public class GenerateZip extends CustomServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-type", "text/html;charset=UTF-8");
        ServletResult result = new ServletResult().value(UNKNOWN);
        List<String> pathList = filterData(req.getHeader("path-list"));

        Log4j.info("[ZIP] 指定文件列表\t"+ pathList);

        try {
            long time = System.currentTimeMillis();

            if(pathList.size() == 0) {
                throw new FileNotFoundException("没有指定需要打包的资源列表");
            }

            File compressDirt = cpFileListToDir(pathList);

            if (compressDirt==null || !compressDirt.exists()){
                throw new FileNotFoundException("没有存在一个可批量打包的文件");
            }

            String zipLocalPath  = compressZip(compressDirt);

            String url = zipLocalPath.replace(WebServer.rootFolderStr,WebServer.domain);

            Log4j.info("生成ZIP文件: " + zipLocalPath +" ,耗时: "+ (System.currentTimeMillis() - time)+" 毫秒 URL = " + url);

            //返回ZIP包URL
            result.setData(url);

        } catch (Exception e) {
            result.setError(e.getMessage());
        }
        writeJson(resp,result);
    }

    /**
     *
     * @param paths 需要打包的文件的相对路径 例如: /Music/jcs.msv
     * @return 存放了需要打包文件的临时目录全路径
     */
    private File cpFileListToDir(List<String> paths) throws  Exception{

        String storageDirPath = WebServer.GET_TEMP_FILE_DIR()+ "/zip/";

        List<File> fileList = checkPaths(paths,storageDirPath);
        if (fileList.size() > 0){

            StringBuilder cacheKey = new StringBuilder();
            for (File f : fileList) {
                cacheKey.append(f.getAbsolutePath()).append(f.length());
            }

            //存放目录
            String dirPath = storageDirPath + EncryptUtil.encryption(cacheKey.toString());

            File dir = new File(dirPath);

            if (!dir.exists()) if (!dir.mkdirs()) throw new IllegalArgumentException("无法创建目录: "+ dirPath);//创建目录

            int index =0;
            for (File file : fileList) {
                //目标临时存放位置
                File out = new File(dirPath, ++index +"-"+file.getName());
                if (!out.exists()){
                    //复制文件
                    FileUtils.copyFile(file, out);
                }
            }
            return dir;
        }
        return null;
    }

    /**
     * 文件夹压缩zip
     * @param compressFolder 指定文件夹
     * @return 压缩包相对路径
     */
    private String compressZip(File compressFolder) throws Exception{

        //打包文件
        File zipFile = new File(WebServer.GET_TEMP_FILE_DIR() ,  compressFolder.getName() +".zip");

        if (!zipFile.exists()){
            Project project = new Project();
            FileSet fileSet = new FileSet();
            fileSet.setProject(project);
            fileSet.setDir(compressFolder);
            Zip zip = new Zip();
            zip.setProject(project);
            zip.setDestFile(zipFile);
            zip.addFileset(fileSet);
            zip.execute();
        }

        //返回压缩包路径
        return zipFile.getCanonicalPath();
    }

}
