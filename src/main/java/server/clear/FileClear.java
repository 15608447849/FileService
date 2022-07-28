package server.clear;

import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.util.Log4j;
import bottle.util.TimeTool;
import server.comm.FileErgodicExecute;
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

    // 最后清理时间点
    private static long lastTime = System.currentTimeMillis();

    // 执行间隔 秒
    @PropertiesName("file.clear.interval.time")
    private static int intervalTime = 24 * 60 * 60;

    // 最大留存时间 秒, 0永久存储
    @PropertiesName("file.clear.segment.max.time")
    private static int segmentMaxTime = 12 * 30 * 24 * 60 * 60;// 1年

    // 临时文件停留时间 秒, 0永久存储
    @PropertiesName("file.clear.temp.exist.time")
    private static int temp_file_timeout =   10 * 60; // 10分钟

    static {
        ApplicationPropertiesBase.initStaticFields(FileClear.class);

        logRecode("存储目录: "+ WebServer.rootFolderStr + " , 最大存储时间: "+  (segmentMaxTime<=0? "永久" :TimeTool.formatDuring(segmentMaxTime * 1000L)));
        logRecode("临时目录: "+ WebServer.GET_TEMP_FILE_DIR() +" , 最小时效时间:"+ (temp_file_timeout<=0? "永久" : TimeTool.formatDuring(temp_file_timeout * 1000L)));
    }

    private static final Runnable RUNNABLE = () -> {
        while (true){
            try {
                Thread.sleep(intervalTime * 1000L);
                if (intervalTime <= 0) break;
                // 临时文件清理
                temporaryErgodic();

                if (System.currentTimeMillis() - lastTime > (24*60*60*1000L)){
                    // 存储文件清理
                    executeClear();
                    lastTime = System.currentTimeMillis();
                    // 空白目录清理
                    emptyDirectoryErgodic( WebServer.rootFolder );
                }

            } catch (Exception e) {
                Log4j.error("文件服务错误",e);
            }
        }
    };



    private static void executeClear() {
        if (segmentMaxTime<=0) return;

        //遍历文件
        new FileErgodicExecute(WebServer.rootFolderStr, true).setCallback(file -> {
                if (file.length() == 0 && (System.currentTimeMillis() - file.lastModified() >  300*1000L )){
                    logRecode("(存储)无效文件: " + file
                            + " 最后修改时间: " + TimeTool.date_yMd_Hms_2Str(new Date(file.lastModified()))
                            + " 删除结果: " + file.delete()
                    );
                }
                else if (System.currentTimeMillis() - file.lastModified() > segmentMaxTime) {
                    //文件超过过期时间
                    if (!isClearForbidSuffix(file.getName())){
                        try {
                            String localFilePath = file.getCanonicalPath();
                            String remoteFilePath = localFilePath.replace(WebServer.rootFolderStr,"");
                            if (HWOBSAgent.existRemoteFile(remoteFilePath)){
                                // 允许被删除
                                logRecode("(存储)(OBS)过期文件: " + file
                                        + " 最后修改时间: " + TimeTool.date_yMd_Hms_2Str(new Date(file.lastModified()))
                                        + " 删除结果: " +file.delete()
                                );
                            }

                        } catch (Exception ignored) {

                        }
                    }

                }
            return true;
        }).start();
    }

    private static void emptyDirectoryErgodic(File dict) {
        if (dict.isFile()) return;
        File[] subList = dict.listFiles();
        if (subList==null || subList.length==0){
            // 删除空目录
            logRecode("空文件夹: " + dict +
                    " 最后修改时间: " + TimeTool.date_yMd_Hms_2Str(new Date(dict.lastModified())) +
                    " 删除结果: " + dict.delete()
            );
        }else {
            // 遍历子目录
            for (File sub : subList){
                emptyDirectoryErgodic(sub);
            }
        }
    }

    private static void temporaryErgodic(){
        if (temp_file_timeout<0) return;
        new FileErgodicExecute(WebServer.GET_TEMP_FILE_DIR(), true).setCallback(file -> {
            if (System.currentTimeMillis() - file.lastModified() > (temp_file_timeout * 1000L)) {
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
        Thread t_global = new Thread(RUNNABLE);
        t_global.setDaemon(true);
        t_global.setName("file-clear-"+t_global.getId());
        t_global.start();

    }
}
