package server.servlet.beans.operation;

import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.util.Log4j;
import bottle.util.TimeTool;
import server.prop.WebServer;

import java.io.File;
import java.util.*;

/**
 * @Author: leeping
 * @Date: 2020/9/4 14:34
 */

@PropertiesFilePath("/clear.properties")
public class FileClearThread extends Thread{

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

    private FileClearThread(){
        this.setDaemon(true);
        this.setName("文件服务清理线程-"+getId());
    }

    @Override
    public void run() {
        while (true){
            try {
                ApplicationPropertiesBase.initStaticFields(FileClearThread.class);
                if (intervalTime < 0 || segmentMaxTime < 0) break;
                Set<String> suffixSet = new HashSet<>();
                try {
                    String[] suffixArr = fileSuffixArrayStr.split(",");
                    suffixSet.addAll(Arrays.asList(suffixArr));
                } catch (Exception e) {
                    Log4j.error("文件服务错误",e);
                }
                executeClear(segmentMaxTime * 1000L,suffixSet);
                Thread.sleep(intervalTime * 1000L);
            } catch (Exception e) {
                Log4j.error("文件服务错误",e);
            }
        }
    }

    private static void executeClear(long segmentMaxTime, Set<String> suffixSet) {
        //遍历文件

        Log4j.info( WebServer.rootFolderStr + " ,启动文件清理, 最大存储时间: "+ TimeTool.formatDuring(segmentMaxTime)+" , 过滤后缀: "+ suffixSet);
        new FileErgodicOperation(WebServer.rootFolderStr, true).setCallback(file -> {
            // 过滤'未超时',及后缀在'过滤后缀列表'内的文件
            String suffix = file.getName();
            suffix = suffix.substring(suffix.lastIndexOf(".") + 1);
                if (System.currentTimeMillis() - file.lastModified() > segmentMaxTime) {
                    if (!suffixSet.contains(suffix)){
                        String log = "过期文件: " + file +
                                " 最后修改时间: " + TimeTool.date_yMd_Hms_2Str(new Date(file.lastModified()))
                                //+ " ["+suffix+"] "+suffixSet +" " +suffixSet.contains(suffix)
                                + "删除结果: " + (  isEnableDelete ? file.delete() :" 禁止删除" );

                        Log4j.writeLogToSpecFile("./clear",Log4j.sdfDict.format(new Date()),log);
                    }
                }
            return true;
        }).start();


        //移除空白目录
        emptyDirectoryErgodic( WebServer.rootFolder , WebServer.temporaryFolder );//排除系统目录
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
                    + "删除结果: " + (  isEnableDelete ? dict.delete() :" 禁止删除" );

            Log4j.writeLogToSpecFile("./clear",Log4j.sdfDict.format(new Date()),log);
            return;
        }

        // 遍历子目录
        for (File _it : subList){
            emptyDirectoryErgodic(_it,filterDirs);
        }
    }

    private static final class H{
        private static final FileClearThread INSTANCE = new FileClearThread();
    }

    public static FileClearThread get(){
        return H.INSTANCE;
    }

}
