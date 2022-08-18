package server.clear;

import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.util.EncryptUtil;
import bottle.util.Log4j;
import bottle.util.TimeTool;
import server.comm.FileErgodicExecute;
import server.comm.RuntimeUtil;
import server.hwobs.HWOBSAgent;
import server.undertow.WebServer;

import java.io.File;
import java.util.*;

import static server.comm.SuffixConst.isClearForbidSuffix;

/**
 * @Author: leeping
 * @Date: 2020/9/4 14:34
 */
@PropertiesFilePath("/web.properties")
public class FileClear{

    private static void logRecode(String log){
        Log4j.info("[文件清理]\t"+log);
    }


    // 存储文件 检查执行间隔 秒
    @PropertiesName("file.clear.interval.time")
    public static int storageFileCheckIntervalTime = 24 * 60 * 60;

    // 存储文件 失效时间 秒, 0永久存储
    @PropertiesName("file.clear.segment.max.time")
    public static int storageFileExpireTimeout = 12 * 30 * 24 * 60 * 60;// 1年

    // 临时文件 失效时间 秒, 0永久存储
    @PropertiesName("file.clear.temp.exist.time")
    public static int tempFileExpireTimeout =   10 * 60; // 10分钟

    // 删除存储 开始时间
    public static long delFileStartTime;
    // 删除存储 遍历时长
    public static long delFileUseTime;
    // 删除存储 文件总大小
    public static long delFileLenTotal;
    // 删除存储 文件总数量
    public static long delFileCountTotal;


    static {
        ApplicationPropertiesBase.initStaticFields(FileClear.class);

        logRecode("存储目录: "+ WebServer.rootFolderStr + " , 最大存储时间: "+  (storageFileExpireTimeout <=0? "永久" :TimeTool.formatDuring(storageFileExpireTimeout * 1000L)));
        logRecode("临时目录: "+ WebServer.GET_TEMP_FILE_DIR() +" , 最大存储时间:"+ (tempFileExpireTimeout <=0? "永久" : TimeTool.formatDuring(tempFileExpireTimeout * 1000L)));
    }

    private static final Runnable RUNNABLE = () -> {
        while (true){
            try {
                if (storageFileCheckIntervalTime <= 0) break;
                Thread.sleep(storageFileCheckIntervalTime * 1000L);
                // 存储文件清理
                executeClear();
            } catch (Exception e) {
                Log4j.error("文件清理错误",e);
            }
        }
    };

    private static final Runnable RUNNABLE_TMP = () -> {
        while (true){
            try {
                Thread.sleep( Math.max(tempFileExpireTimeout,60) * 1000L);
                // 空白目录清理
                emptyDirectoryErgodic( WebServer.rootFolder );
                //临时文件清理
                temporaryErgodic();
            } catch (Exception e) {
                Log4j.error("文件清理错误",e);
            }
        }
    };



    private static void executeClear() {
        if (storageFileExpireTimeout <=0) return;
//        delFileLenTotal = 0;
//        delFileCountTotal = 0;
        delFileUseTime = 0;
        delFileStartTime = System.currentTimeMillis();
        //遍历文件
        new FileErgodicExecute(WebServer.rootFolderStr, true).setCallback(file -> {
                if (file.length() == 0 && (System.currentTimeMillis() - file.lastModified() >  300*1000L )){
                    logRecode("(存储)无效文件: " + file
                            + " 最后修改时间: " + TimeTool.date_yMd_Hms_2Str(new Date(file.lastModified()))
                            + " 删除结果: " + file.delete()
                    );
                }
                else if (System.currentTimeMillis() - file.lastModified() > storageFileExpireTimeout *1000L) {
                    //文件超过过期时间
                    if (!isClearForbidSuffix(file.getName())){
                        try {

                            String localFilePath = file.getCanonicalPath();
                            String remoteFilePath = localFilePath.replace(WebServer.rootFolderStr,"");
                            String localFileMD5 = EncryptUtil.getFileMd5ByString(file);
                            long length = file.length();
                            long lastTime = file.lastModified();

                            if(HWOBSAgent.checkOBSExist(remoteFilePath,localFileMD5)){

                                boolean isDelete  = file.delete();
                                if (isDelete){
                                    delFileLenTotal += length;
                                    delFileCountTotal++;
                                }
                                // 允许被删除
                                logRecode("(存储)(OBS)过期文件: " + localFilePath
                                        + " 最后修改时间: " + TimeTool.date_yMd_Hms_2Str(new Date(lastTime))
                                        + " 留存时间: "+ TimeTool.formatDuring(System.currentTimeMillis() - lastTime)
                                        + " MD5: "+ localFileMD5
                                        + " LEN: "+ RuntimeUtil.byteLength2StringShow(length)
                                        + " 删除结果: " + isDelete
                                );
                            }

                        } catch (Exception ignored) {

                        }
                    }

                }
            return true;
        }).start();
        delFileUseTime = System.currentTimeMillis()-delFileStartTime;
        logRecode("开始时间: "+TimeTool.date_yMd_Hms_2Str(new Date(delFileStartTime))+
                " ,总用时长: " + (TimeTool.formatDuring(delFileUseTime)) +
                " ,清理数量: "+ delFileCountTotal +
                " ,合计大小: "+ RuntimeUtil.byteLength2StringShow(delFileLenTotal));
    }

    private static void emptyDirectoryErgodic(File dict) {
        if (dict.isFile()) return;
        File[] subList = dict.listFiles();
        if (subList==null || subList.length==0 ){

            if(System.currentTimeMillis() - dict.lastModified() > 86400*1000L){
                // 删除空目录
                logRecode("空文件夹: " + dict +
                        " 最后修改时间: " + TimeTool.date_yMd_Hms_2Str(new Date(dict.lastModified())) +
                        " 删除结果: " + dict.delete()
                );
            }

        }else {
            // 遍历子目录
            for (File sub : subList){
                emptyDirectoryErgodic(sub);
            }
        }
    }

    private static void temporaryErgodic(){
        if (tempFileExpireTimeout < 0) return;
        new FileErgodicExecute(WebServer.GET_TEMP_FILE_DIR(), true).setCallback(file -> {
            if (System.currentTimeMillis() - file.lastModified() > (tempFileExpireTimeout * 1000L)) {
                //临时文件过期
                String log = "(临时)过期文件: " + file +
                        " 最后修改时间: " + TimeTool.date_yMd_Hms_2Str(new Date(file.lastModified()))
                        + " 删除结果: " +  file.delete();

                logRecode(log);
            }
            return true;
        }).start();
    }



    public static void start(){
        Thread thread = new Thread(RUNNABLE);
        thread.setDaemon(true);
        thread.setName("file-clear-"+thread.getId());
        thread.start();

        Thread thread_tmp = new Thread(RUNNABLE_TMP);
        thread_tmp.setDaemon(true);
        thread_tmp.setName("file-clear-tmp-"+thread_tmp.getId());
        thread_tmp.start();

    }
}
