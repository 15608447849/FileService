package server.HuaWeiOBS;

import bottle.util.EncryptUtil;
import bottle.util.Log4j;
import server.prop.WebServer;
import server.sqlites.SQLiteUtils;

import java.io.File;
import java.util.List;

import static server.sqlites.SQLiteUtils.*;

/**
 * @Author: leeping
 * @Date: 2020/10/22 21:42
 * 从SqlLite队列获取任务列表进行上传
 * 1. 查询是否存在, 查询MD5是否匹配
 * 2. 执行上传操作
 */
public class OBSUploadPoolUtil {
    private OBSUploadPoolUtil(){};

    private static final String TYPE = "OBS_FILE_UPLOAD_QUEUE";

    public static boolean addFileToQueue(String localFilePath){
        try {
            File file = new File(localFilePath);
            if (file.exists()){
                String md5 = EncryptUtil.getFileMd5ByString(file);
                boolean isAdd = addListValue(TYPE,localFilePath,md5,null);
                if (isAdd){
                    synchronized (TYPE){
                        TYPE.notifyAll();
                    }
                }
                return isAdd;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void addFileToQueue(List<String> localPathList){
        for (String localPath : localPathList){
            boolean isAdd = addFileToQueue(localPath);
            if (!isAdd){
                Log4j.info("文件加入OBS队列失败: "+ localPath );
            }
        }
    }

    private static final Runnable LOOP_QUEUE_RUNNABLE = () -> {
        while (true){
            try{
                tryUploadFileToOBS();
                synchronized (TYPE){
                    TYPE.wait(30 * 60 * 1000L);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    private static void tryUploadFileToOBS() {
        List<SQLiteUtils.StorageItem> list = getListByType(TYPE);
        for (SQLiteUtils.StorageItem it : list){
            String relativePath = it.value.replace(WebServer.rootFolderStr,"");
//
            boolean isExist = checkOBSFileExist(relativePath,it.identity);
            boolean removeQueue = !isExist;
            if (!isExist){
                removeQueue = HWOBSServer.uploadLocalFile(it.value,relativePath);
            }
            if (removeQueue) removeListValue(TYPE,it.value,it.identity);
        }
    }

    public static boolean checkOBSFileExist(String relativePath,String localFileMD5) {
        String remoteFileMD5 = HWOBSServer.existFile(relativePath);
        return remoteFileMD5!=null && remoteFileMD5.equals(localFileMD5);
    }

   static {
       Thread t = new Thread(LOOP_QUEUE_RUNNABLE);
       t.setDaemon(true);
       t.start();
   }

    public static void start(){
        Log4j.info("启动OBS上传线程");
    }

}
