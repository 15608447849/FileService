package server.servlet.imps;

import bottle.threadpool.IOThreadPool;
import bottle.util.EncryptUtil;
import bottle.util.Log4j;
import server.HuaWeiOBS.HWOBSServer;
import server.HuaWeiOBS.OBSUploadPoolUtil;
import server.prop.WebServer;
import server.servlet.beans.operation.FileErgodicOperation;
import server.servlet.iface.Mservlet;
import sun.security.provider.MD5;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class Online extends Mservlet {

    private static boolean isExecute;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        final String file_ergodic_path = req.getParameter("file_ergodic_path");
        if (file_ergodic_path!=null){
            new Thread(() -> {
                try {
                    executeErgodic(WebServer.rootFolderStr + file_ergodic_path);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

        writeString(resp,"online",true);
    }

    private static void executeErgodic(String path) {
        if (isExecute) return;
        isExecute = true;
        FileErgodicOperation op = new FileErgodicOperation(path, true);
        op.setCallback(new FileErgodicOperation.Callback() {
            @Override
            public boolean filterFile(File file) {

                try {
                    String relativePath = file.getCanonicalPath().replace(WebServer.rootFolderStr,"");
                    String md5 = EncryptUtil.getFileMd5ByString(file);
                    boolean isExist = OBSUploadPoolUtil.checkOBSFileExist(relativePath,md5);
                    if (isExist){
                        boolean isAdd = OBSUploadPoolUtil.addFileToQueue(file.getCanonicalPath());
                        if (!isAdd){
                            Log4j.info("添加OBS上传文件失败: "+ file);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });
        op.start();
        isExecute = false;
    }
}
