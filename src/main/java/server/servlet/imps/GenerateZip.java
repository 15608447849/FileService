package server.servlet.imps;


import bottle.util.EncryptUtil;
import bottle.util.FileTool;
import bottle.util.Log4j;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;
import server.servlet.beans.result.Result;
import server.prop.WebProperties;
import server.servlet.iface.Mservlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import static server.servlet.beans.result.Result.RESULT_CODE.*;

/**
 * Created by user on 2018/12/17.
 * 传递 需要打包下载的文件的相对路径
 * 生成zip包
 * 请求重定向
 */
public class GenerateZip extends Mservlet {

    private static final String ZIP_BATCH_DIR =  FileTool.SEPARATOR  + "ZIP_TEMP" +  FileTool.SEPARATOR ;

    private static final String ZIP_TEMP_DIR_PREV = "zip_batch_file_";

    private static final long TIME_DEL = 1000L * 60 * 60 * 3; //3小时

    private static final Timer timer = new Timer();

    static {
        // 注册定时器 - 10分钟后自动删除
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                File dict = new File(WebProperties.rootPath,ZIP_BATCH_DIR);
                if (dict.exists()){
                    File[] files = dict.listFiles();
                    for (File f : files){
                        if (System.currentTimeMillis() - f.lastModified() > TIME_DEL){
                            f.delete();
                        }
                    }
                }
            }
        },TIME_DEL);
    }

    private void addFile(List<File> list, File file) {
        if (file.isFile()){
            list.add(file);
        }else if (file.isDirectory()){
            File[] files = file.listFiles();
            if (files != null && files.length>0){
                for (File f : files){
                    addFile(list,f);
                }
            }
        }
    }

    private List<File> checkPaths(List<String> paths) {
       List<File> list = new ArrayList<>();
       for (String path : paths){
           if (!path.startsWith(FileTool.SEPARATOR)) path = FileTool.SEPARATOR + path;// 保证前面有 '/'
           File file = new File(WebProperties.rootPath  + path);
           if (!file.exists()){
               //文件不存在
               continue;
           }
          addFile(list,file);
       }
       return list;
    }



    /**
     *
     * @param paths 需要打包的文件的相对路径 例如: /Music/jcs.msv
     * @return 存放了需要打包文件的临时目录全路径
     */
    private File cpFileListToDir(List<String> paths) throws  Exception{

        String dirPath = WebProperties.rootPath  + ZIP_BATCH_DIR +
                EncryptUtil.encryption(ZIP_TEMP_DIR_PREV + System.currentTimeMillis());

        List<File> fileList = checkPaths(paths);

        File dir = new File(dirPath);
        if (fileList.size() > 0){
            if (!dir.exists()) dir.mkdirs();//创建目录
            for (File file : fileList) {
                File out = new File(dirPath,
                        EncryptUtil.encryption(file.getAbsolutePath())+"_"+file.getName()+
                                file.getName().substring(file.getName().lastIndexOf(".")));
                FileUtils.copyFile(file, out); //复制文件
            }
        }
        return dir;
    }


    /**
     * 文件夹压缩zip
     * @param dir 指定文件夹
     * @return 压缩包相对路径
     */
    private String compressZip(File dir){
            String zipPath =  ZIP_BATCH_DIR + dir.getName() +".zip";

            File zipFile = new File(WebProperties.rootPath + zipPath);
            if (zipFile.exists()) zipFile.delete();

            Project prj = new Project();
            Zip zip = new Zip();
            zip.setProject(prj);
            zip.setDestFile(zipFile);
            FileSet fileSet = new FileSet();
            fileSet.setProject(prj);
            fileSet.setDir(dir);
            zip.addFileset(fileSet);
            zip.execute();

            FileTool.deleteFileOrDir(dir.getAbsolutePath());

            return zipPath;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
        Result result = new Result().value(UNKNOWN);
        List<String> pathList = filterData(req.getHeader("path-list"));
        Log4j.info("ZIP-文件列表: "+ pathList);


        try {
            if(pathList.size() == 0) {
                throw new FileNotFoundException("没有指定需要打包的文件列表");
            }

            File dirt = cpFileListToDir(pathList);
            if (!dirt.exists()){
                throw new FileNotFoundException("没有存在一个可批量打包的文件");
            }

            String zipPath  = compressZip(dirt);
            //返回ZIP包URL
            String url = WebProperties.domain +  zipPath;
            Log4j.info("ZIP URL : " + url);
            result.data = url;
            result.value(SUCCESS);
        } catch (Exception e) {
            result.data = e.getMessage();
            result.value(PARAM_ERROR);
        }
        writeJson(resp,result);
    }
}
