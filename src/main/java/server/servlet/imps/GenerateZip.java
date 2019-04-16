package server.servlet.imps;

import bottle.util.EncryptUtils;
import bottle.util.FileUtils;
import bottle.util.Log4j;
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
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static server.servlet.beans.result.Result.RESULT_CODE.*;

/**
 * Created by user on 2018/12/17.
 * 传递 需要打包下载的文件的相对路径
 * 生成zip包
 * 请求重定向
 */
public class GenerateZip extends Mservlet {

    private final String ZIP_TEMP_DIR_PREV = "zip_dir_temp_";

    private final int TIME_DEL = 1000 * 60 * 10; //10分钟

    private final Timer timer = new Timer();
    /**
     * 文件夹压缩zip
     * @param dir 指定文件夹
     * @return 压缩包相对路径
     */
    private String compressZip(File dir) throws Exception{
            String zipPath = File.separator + dir.getName()+".zip";
            File zipFile = new File(WebProperties.get().rootPath + zipPath);
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
            org.apache.commons.io.FileUtils.deleteDirectory(dir);
            return zipPath;
    }

    /**
     *
     * @param paths 需要打包的文件的相对路径 例如: /Music/jcs.msv
     * @return 存放了需要打包文件的临时目录全路径
     */
    private File cpFileListToDir(List<String> paths) throws  Exception{

            String homePath = WebProperties.get().rootPath;

            String dirPath = homePath + FileUtils.SEPARATOR + EncryptUtils.byteToHexString( (ZIP_TEMP_DIR_PREV +System.currentTimeMillis()).getBytes());

            File dir = new File(dirPath);

            if (!dir.exists()) dir.mkdirs();//创建目录

            int size = paths.size();
            File temp ;
            File out;
            File outDir;
            String path;

            for (int i = 0;i<size;i++){
                path = paths.get(i);

                if (!path.startsWith(FileUtils.SEPARATOR)) path = FileUtils.SEPARATOR+ path;//保证前面有 '/'

                temp = new File(homePath  + path);
                out = new File(dirPath + path);

                outDir = new File(out.getParent());

                if (!outDir.exists()) outDir.mkdirs();

                if (temp.exists()){
                    FileUtils.copyFile(temp,out);
                }
                else throw new FileNotFoundException();
            }

            return dir;
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
        Result result = new Result().value(UNKNOWN);
        List<String> pathList = filterData(req.getHeader("path-list"));
//        Log.i("zip 文件列表: "+ pathList);

        try {
            if(pathList.size() > 0) {

                String zipPath  = compressZip(cpFileListToDir(pathList));

                //返回ZIP包URL
                String url = String.format(Locale.CANADA,"http://%s:%d%s",
                        WebProperties.get().webIp,
                        WebProperties.get().webPort,
                        zipPath
                );

                // 注册定时器 - 10分钟后自动删除
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            File file = new File(WebProperties.get().rootPath + zipPath);
//                            Log.i("删除临时文件: "+ file);
                            file.delete();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },TIME_DEL);

                Log4j.info("zip URL : " + url.replace("\\", "/"));

                resp.sendRedirect(url);
            }
            result.value(PARAM_ERROR);

        } catch (Exception e) {
            e.printStackTrace();
            result.value(EXCEPTION);
        }

        writeJson(resp,result);
    }
}
