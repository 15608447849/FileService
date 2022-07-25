package server.hwobs;

import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.threadpool.IOSingerThreadPool;
import bottle.threadpool.IThreadPool;
import bottle.util.EncryptUtil;
import bottle.util.Log4j;
import bottle.util.TimeTool;
import server.comm.FileErgodicExecute;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.URLResource;
import server.sqlites.tables.SQLiteFileTable;
import server.undertow.WebServer;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static server.sqlites.tables.SQLiteListTable.*;

/**
 * @Author: leeping
 * @Date: 2020/10/22 21:42
 * 从SqlLite队列获取任务列表进行上传
 * 1. 查询是否存在, 查询MD5是否匹配
 * 2. 执行上传操作
 */
@PropertiesFilePath("/hwobs.properties")
public class HWOBSUpload {

    private HWOBSUpload(){}

    @PropertiesName("hwobs.ergodic.enable")
    private static boolean isRunning;

    @PropertiesName("hwobs.ergodic.delete")
    private static boolean isDelLoc;

    @PropertiesName("hwobs.ergodic.interval")
    private static long loopTime;

    @PropertiesName("hwobs.ergodic.file.retained")
    private static long retainedTime;

    @PropertiesName("hwobs.filter.suffix")
    private static String filterSuffixStr;

    private static List<String> filterSuffixList = new ArrayList<>();

    private static final int limit = 1000;

    private static final IThreadPool pool = new IOSingerThreadPool(Runtime.getRuntime().availableProcessors());

    private static final String TYPE = "OBS_FILE_UPLOAD_QUEUE";

    private static final Thread thread = new Thread(){
        @Override
        public void run() {
            while (isRunning ){
                try {
                    Thread.sleep(loopTime * 1000L);
                    localErgodic();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    static {
        ApplicationPropertiesBase.initStaticFields(HWOBSUpload.class);
        if (filterSuffixStr!=null){
            String[] arr = filterSuffixStr.split(",");
            filterSuffixList.addAll(Arrays.asList(arr));
        }
        thread.setDaemon(true);
        thread.start();
    }


    public static void localErgodic() {
        FileErgodicExecute execute = new FileErgodicExecute(WebServer.rootFolderStr, true);

        execute.setCallback(file -> {
            try {
                if (!(checkFileFilter(file.getName()))
                        && System.currentTimeMillis() - file.lastModified() > (retainedTime*1000L)){

                    String localFilePath = file.getCanonicalPath();
                    String localFileMD5 = EncryptUtil.getFileMd5ByString(file);

                    String remotePath = localFilePath.replace(WebServer.rootFolderStr,"");
                    remotePath = remotePath.replace("\\","/");

                    // 记录本地文件
                    SQLiteFileTable.addFile_LOCAL(remotePath,WebServer.domain+remotePath);

                    String remoteFileMD5 = HWOBSServer.getFileMD5(remotePath);

                    boolean isExist = remoteFileMD5!=null && remoteFileMD5.equals(localFileMD5);


                    if (!isExist){
                        HWOBSUpload.addFileToQueue(localFilePath);
                    }else if (isDelLoc){
                        Log4j.info(Thread.currentThread()+" OBS已存在 文件删除 MD5 = " + localFileMD5 + " 路径: "+ file + " 尝试删除: "+ file.delete() );
                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        });

        execute.start();
    }

    public static boolean addFileToQueue(String localFilePath){
        try {
            File file = new File(localFilePath);
            if (file.exists()){
                //过滤不上传的文件后缀
                if (checkFileFilter(file.getName())) return true;

                String md5 = EncryptUtil.getFileMd5ByString(file);
                String remotePath = localFilePath.replace(WebServer.rootFolderStr,"");
                //判断队列是否存在
                if (checkOBSFileExist(remotePath,md5,true)) return true;

                boolean isAdd = addListValue(TYPE,localFilePath,md5);
                Log4j.info(Thread.currentThread() + " 加入OBS上传队列: "+ localFilePath +" >> "+ isAdd);
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
            addFileToQueue(localPath);
        }
    }

    private static final Runnable LOOP_QUEUE_RUNNABLE = () -> {
        while (true){
            try{
                List<ListStorageItem> list = getListByType(TYPE,100);
                if (list.size() != 0) {
                    uploadFileToOBS(list);
                }else{
                    synchronized (TYPE){
                        TYPE.wait(300 * 1000);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    private static void uploadFileToOBS(List<ListStorageItem> list) {
        try{

            int total = list.size();
            long _stime = System.currentTimeMillis();
            String _sTimeStr = TimeTool.date_yMd_Hms_2Str(new Date());

            List<Future<Boolean>> futures = new ArrayList<>();
            for (ListStorageItem it : list) {
                Callable<Boolean> callback = () -> {
                    // obj上传执行
                    boolean removeQueue = uploadFileToOBSExecute(it.value,it.identity);// value=文件本地路径, identity=文件MD5
                    if (removeQueue) {
                        boolean isDelRes = removeListValue(TYPE, it.value);// 从数据库列表删除
                        if (!isDelRes)  Log4j.info(Thread.currentThread()+" OBS上传成功,移除队列失败: " + it.value);
                    }
                    return removeQueue;

                };

                futures.add(pool.submit(callback));
            }
            int failIndex = 0;
            for (Future<Boolean> future : futures){
                Boolean res = future.get();
                if (res == null || !res){
                    failIndex++;
                }
            }
            long _etime = System.currentTimeMillis();

            Log4j.info(Thread.currentThread() +
                    " 开始时间: "+ _sTimeStr +
                    " 成功/总数: "+ (total - failIndex)+"/"+ total +
                    " 用时: "+  TimeTool.formatDuring(_etime - _stime) );

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /*
    * True OBS已存在或上传成功
    * False OBS不存在
    * */
    private static boolean uploadFileToOBSExecute(String fileLocalPath,String localFileMD5 ){
        boolean flag = false;
        String remotePath = fileLocalPath.replace(WebServer.rootFolderStr, "");
        if (localFileMD5!=null){
            flag = checkOBSFileExist(remotePath, localFileMD5, false);
        }
        if (!flag){
            flag = HWOBSServer.uploadLocalFile(fileLocalPath, remotePath);
        }
        return flag;
    }

    public static boolean uploadTempFileToOBSExecute(String fileLocalPath){
        if (HWOBSServer.enable){
            String md5;
            try {
                md5 =EncryptUtil.getFileMd5ByString(new File(fileLocalPath));
            } catch (Exception e) {
                md5 = null;
            }
            return uploadFileToOBSExecute(fileLocalPath,md5);
        }
        return false;
    }


    private static boolean checkFileFilter(String fileName){
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);

        for (String filterSuffix : filterSuffixList){
            if (suffix.equals(filterSuffix)) return true;
        }
        return false;
    }

    private static boolean checkOBSFileExist(String remotePath,String localFileMD5,boolean selectQueue) {
        if (selectQueue){
            //查询数据库是否存在, 存在则跳过
            boolean queueExist = existIdentity(TYPE,localFileMD5);
            if(queueExist){
                return true;
            }
        }

        String remoteFileMD5 = HWOBSServer.getFileMD5(remotePath);
        return remoteFileMD5!=null && remoteFileMD5.equals(localFileMD5);
    }

    public static String getFileURL(String path) {
        try{
            // 判断是否启用OBS,且文件存在,尝试通过OBS获取
            if (HWOBSServer.enable &&  HWOBSServer.existFile(path) ){
                String url = HWOBSServer.convertLocalFileToCDNUrl(path);
                Log4j.info("获取OBS资源 " + url );
                return url;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static Resource getResource(String path) {
        try{
            String url = getFileURL(path);
            return new URLResource( URI.create(url).toURL(),path);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }



    public static void start(){
        if (!HWOBSServer.enable) return;
        Thread t = new Thread(LOOP_QUEUE_RUNNABLE);
        t.setDaemon(true);
        t.setName("hw-obs-upload-"+t.getId());
        t.start();
    }

}
