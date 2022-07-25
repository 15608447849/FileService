package server.clear;

import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.util.Log4j;
import bottle.util.TimeTool;
import server.comm.FileErgodicExecute;
import server.undertow.WebServer;

import java.io.File;
import java.util.*;

/**
 * @Author: leeping
 * @Date: 2020/9/4 14:34
 */
@PropertiesFilePath("/web.properties")
public class FileClear{

    @PropertiesName("file.clear.file.delete")
    private static boolean isEnableDelete = false;

    // 不可清理的文件后缀,逗号分隔
    @PropertiesName("file.clear.filter.file.suffix")
    private static String fileSuffixArrayStr = "";

    // 最大留存时间 秒, -1,永久存储
    @PropertiesName("file.clear.segment.max.time")
    private static int segmentMaxTime = 12 * 30 * 24 * 60 * 60;

    // 执行间隔 秒
    @PropertiesName("file.clear.interval.time")
    private static int intervalTime = 24 * 60 * 60;

    private static final Set<String> suffixSet = new HashSet<>();

    @PropertiesName("file.clear.temp.exist.time")
    private static int temp_file_timeout =   3 * 60; //3分钟

    static {
        ApplicationPropertiesBase.initStaticFields(FileClear.class);

        try {
            String[] suffixArr = fileSuffixArrayStr.split(",");
            suffixSet.addAll(Arrays.asList(suffixArr));
        } catch (Exception ignored) {
            //pass
        }
    }

    private static final Runnable RUNNABLE = () -> {
        while (true){
            try {
                Thread.sleep(intervalTime * 1000L);
                if (intervalTime <= 0 || segmentMaxTime < 0) break;
                executeClear(segmentMaxTime * 1000L,suffixSet);
            } catch (Exception e) {
                Log4j.error("文件服务错误",e);
            }
        }
    };

    private static void mrecode(String log){
//        Log4j.writeLogToSpecFile("./logs/clear",Log4j.sdfDict.format(new Date()),log+"\n");
        Log4j.info("./logs/clear",Log4j.sdfDict.format(new Date()),log+"\n");
    }

    private static void executeClear(long segmentMaxTime, Set<String> suffixSet) {

        mrecode("清理存储文件,"+ WebServer.rootFolderStr + " , 最大存储时间: "+ TimeTool.formatDuring(segmentMaxTime)+ " , 过滤后缀: "+ suffixSet);

        //遍历文件
        new FileErgodicExecute(WebServer.rootFolderStr, true).setCallback(file -> {
                if (System.currentTimeMillis() - file.lastModified() > segmentMaxTime) {
                    //文件超过过期时间
                    String fileName = file.getName();
                    String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);

                    if (!suffixSet.contains(suffix)){
                        // 后缀允许被删除
                        String log = "(存储)过期文件: " + file
                                + " 最后修改时间: " + TimeTool.date_yMd_Hms_2Str(new Date(file.lastModified()))
                                + " 删除结果: " + (  isEnableDelete ? file.delete() :" 禁止删除" );


                        mrecode(log);
                    }
                }
            return true;
        }).start();

        //移除空白目录
        emptyDirectoryErgodic( WebServer.rootFolder );
    }

    private static void emptyDirectoryErgodic(File dict,File... filterDirs) {
        if (dict.isFile()) return;
        File[] subList = dict.listFiles();

        if (subList==null || subList.length==0){
            for (File noDelDir : filterDirs){
                if (noDelDir.getAbsolutePath().equals(dict.getAbsolutePath())){
                    return;
                }
            }

            // 删除
            String log = "空文件夹: " + dict +
                            " 最后修改时间: " + TimeTool.date_yMd_Hms_2Str(new Date(dict.lastModified()))
                    + " 删除结果: " + (  isEnableDelete ? dict.delete() :" 禁止删除" );

            mrecode(log);
            return;
        }

        // 遍历子目录
        for (File _it : subList){
            emptyDirectoryErgodic(_it,filterDirs);
        }
    }


    private static final Runnable RUNNABLE_TEMP = () -> {
        while (true){
            try{
                Thread.sleep((temp_file_timeout * 1000L * 2));

                mrecode("清理临时文件,失效时间:"+ temp_file_timeout +" 秒");

                new FileErgodicExecute(WebServer.GET_TEMP_FILE_DIR(), true).setCallback(file -> {
                    if (System.currentTimeMillis() - file.lastModified() > (temp_file_timeout * 1000L)) {
                        //临时文件过期
                        String log = "(临时)过期文件: " + file +
                                        " 最后修改时间: " + TimeTool.date_yMd_Hms_2Str(new Date(file.lastModified()))
                                        + " 删除结果: " +  file.delete();

                        mrecode(log);

                    }
                    return true;
                }).start();

            }catch (Exception e){
                Log4j.error("文件服务错误",e);
            }
        }
    };


    public static void start(){
        Thread t_global = new Thread(RUNNABLE);
        t_global.setDaemon(true);
        t_global.setName("storage-file-clear-"+t_global.getId());
        t_global.start();

        Thread t_temp = new Thread(RUNNABLE_TEMP);
        t_temp.setDaemon(true);
        t_temp.setName("temp-file-clear-"+t_temp.getId());
        t_temp.start();

    }
}
