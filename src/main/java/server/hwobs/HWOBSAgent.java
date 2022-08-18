package server.hwobs;

import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.threadpool.IOSingerThreadPool;
import bottle.threadpool.IThreadPool;
import bottle.util.EncryptUtil;
import bottle.util.Log4j;
import bottle.util.TimeTool;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.URLResource;
import server.sqlites.tables.SQLiteFileTable;
import server.undertow.WebServer;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static server.comm.SuffixConst.isSystemDefaultFileSuffix;
import static server.hwobs.HWOBSServer.convertLocalFileToCDNUrl;
import static server.sqlites.tables.SQLiteListTable.*;

/**
 * @Author: leeping
 * @Date: 2020/10/22 21:42
 * 从SqlLite队列获取任务列表进行上传
 * 1. 查询是否存在, 查询MD5是否匹配
 * 2. 执行上传操作
 */
@PropertiesFilePath("/hwobs.properties")
public class HWOBSAgent {

    private HWOBSAgent(){ }

    @PropertiesName("hwobs.upload.max")
    public static int max_upload_size = 1000;

    @PropertiesName("hwobs.upload.interval")
    public static int  force_interval_sec = 0;

    private static final String TYPE = "OBS_FILE_UPLOAD_QUEUE";

    private static final IThreadPool pool
            = new IOSingerThreadPool(Runtime.getRuntime().availableProcessors() * 4);

    static {
        ApplicationPropertiesBase.initStaticFields(HWOBSAgent.class);
    }

    // 添加文件到obs上传队列
    static boolean addFileToQueue(String localFilePath){
        try {
            if (!HWOBSServer.isEnable) return false;

            File file = new File(localFilePath);

            // 文件不存在或者空文件
            if (!file.exists() || file.length()<=0) return false;

            //临时文件
            if (localFilePath.startsWith(WebServer.GET_TEMP_FILE_DIR())) return false;

            //过滤不上传的文件后缀
            if (isSystemDefaultFileSuffix(file.getName()))  return false;

            String md5 = EncryptUtil.getFileMd5ByString(file);
            String remotePath = localFilePath.replace(WebServer.rootFolderStr,"");

            // 判断队列是否存在
            if (checkQueueExist(md5)) return false;
            // 判断obs是否存在
            if (checkOBSExist(remotePath,md5)) return false;

            boolean isAdd = addListValue(TYPE,localFilePath,md5);

            Log4j.info( "加入OBS上传队列: "+ localFilePath +" >> "+ isAdd);
            synchronized (TYPE){
                TYPE.notifyAll();
            }
            return isAdd;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void addFileToQueue(List<String> localPathList){

        for (String localPath : localPathList){
            addFileToQueue(localPath);
        }

    }

    public static int getQueueSize(){
        return listCount(TYPE);
    }

    private static final Thread thread = new Thread(() -> {
        while (true){
            try{

                List<ListStorageItem> list = null;
                if (max_upload_size>0){
                    list = getListByType(TYPE,max_upload_size,1);
                }

                if (list!=null  && list.size() > 0) {
                    uploadFileToOBS(list);
                }else{
                    synchronized (TYPE){
                        TYPE.wait(600 * 1000);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    });

    private static void uploadFileToOBS(List<ListStorageItem> list) {
        try{

            int total = list.size();
            Log4j.info("OBS 本次上传文件数: "+ total );

            long _stime = System.currentTimeMillis();
            String _sTimeStr = TimeTool.date_yMd_Hms_2Str(new Date());

            List<Future<Boolean>> futures = new ArrayList<>();
            for (ListStorageItem it : list) {

                Callable<Boolean> callback = () -> {
                    // 上传执行
                    boolean removeQueue = executeUploadFileToOBS(it.value,it.attach);// value=文件本地路径, attach=文件MD5
                    if (removeQueue){
                        removeListValue(TYPE, it.value);// 从数据库列表删除
                    }
//                    boolean isDelRes = removeListValue(TYPE, it.value);// 从数据库列表删除
//                    if (!removeQueue || !isDelRes)  Log4j.info("OBS上传=" + removeQueue+" 移除队列=" + isDelRes);

                    return removeQueue;
                };

                futures.add(pool.submit(callback));
                // 强制间隔
                if (force_interval_sec > 0){
                    Thread.sleep(force_interval_sec * 1000L);
                }
            }
            int failIndex = 0;
            for (Future<Boolean> future : futures){
                Boolean res = future.get();
                if (res == null || !res){
                    failIndex++;
                }
            }
            long _etime = System.currentTimeMillis();

            Log4j.info(
                    "OBS 上传完成 " +
                            " 开始时间: "+ _sTimeStr +
                            " 成功/总数: "+ (total - failIndex)+"/"+ total +
                            " 用时: "+  TimeTool.formatDuring(_etime - _stime) );

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /*
    * True  OBS已存在或上传成功
    * False OBS不存在
    * */
    private static boolean executeUploadFileToOBS(String fileLocalPath, String localFileMD5 ){
        if (localFileMD5 == null){
            try {
                localFileMD5 = EncryptUtil.getFileMd5ByString(new File(fileLocalPath));
            } catch (Exception e) {
                return true;
            }
        }
        // 远程文件路径
        String remotePath = fileLocalPath.replace(WebServer.rootFolderStr, "");
        // 检查obs是否存在指定文件
        if (!checkOBSExist(remotePath, localFileMD5)){
            boolean isSuccess = HWOBSServer.uploadLocalFile(fileLocalPath, remotePath,localFileMD5);
            if (isSuccess) SQLiteFileTable.addFile_HWOBS(remotePath,  convertLocalFileToCDNUrl(remotePath));
            return isSuccess;
        }
        return true;
    }

    // 检查上传列表是否存在指定文件
    private static boolean checkQueueExist(String localFileMD5){
        //查询数据库是否存在, 存在则跳过
        return existIdentity(TYPE,localFileMD5);
    }

    public static boolean checkOBSExist(String remotePath,String localFileMD5) {

        String remoteFileMD5 = HWOBSServer.getFileMD5(remotePath);
        return remoteFileMD5!=null && remoteFileMD5.equals(localFileMD5);
    }

    public static String getFileURL(String path) {
        try{
            // 判断是否启用OBS,且文件存在,尝试通过OBS获取
            if (HWOBSServer.isEnable &&  HWOBSServer.existFile(path) ){
                String url = convertLocalFileToCDNUrl(path);
                //Log4j.info("获取OBS资源 " + url );
                url = url.replaceAll("\\s+","%20");// 空格转换
                return url;
            }
        }catch (Exception e){
            Log4j.error("获取OBS资源: "+ path,e);
        }
        return null;
    }


    public static Resource getResource(String path) {
        try{
            String url = getFileURL(path);
            if (url!=null){
                return new URLResource( URI.create(url).toURL() ,null);
            }
        }catch (Exception e){
            Log4j.error("获取OBS资源: "+ path,e);
        }
        return null;
    }





    public static void deleteRemoteFile(String path) {
        if (!HWOBSServer.isEnable) return;
        HWOBSServer.deleteFile(path);
    }

    public static boolean existRemoteFile(String remotePath) {
        if (!HWOBSServer.isEnable) return false;
        return HWOBSServer.existFile(remotePath);
    }

    public static List<String> ergodicDirectory(String remotePath, boolean isSub,boolean replaceRoot) {
        List<String> list = new ArrayList<>();
        if (HWOBSServer.isEnable){
            Set<String> set = HWOBSServer.ergodicDirectory(remotePath,isSub);
            for (String p: set){
                list.add( replaceRoot ? p.replace(remotePath,"/") : p);
            }
        }
        return list;
    }

    public static void start(){
        if (!HWOBSServer.isEnable) return;

        HWOBSAgent.thread.setDaemon(true);
        HWOBSAgent.thread.setName("hwobs-agent-"+ HWOBSAgent.thread.getId());
        HWOBSAgent.thread.start();

        HWOBSErgodic.thread.setDaemon(true);
        HWOBSErgodic.thread.setName("hwobs-ergodic-"+ HWOBSErgodic.thread);
        HWOBSErgodic.thread.start();
    }

}
