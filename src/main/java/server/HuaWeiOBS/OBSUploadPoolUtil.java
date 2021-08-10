package server.HuaWeiOBS;

import bottle.threadpool.IOThreadPool;
import bottle.util.EncryptUtil;
import bottle.util.Log4j;
import bottle.util.TimeTool;
import com.obs.services.IFSClient;
import server.prop.WebServer;
import server.sqlites.SQLiteUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static server.sqlites.SQLiteUtils.*;

/**
 * @Author: leeping
 * @Date: 2020/10/22 21:42
 * 从SqlLite队列获取任务列表进行上传
 * 1. 查询是否存在, 查询MD5是否匹配
 * 2. 执行上传操作
 */
public class OBSUploadPoolUtil {
    private OBSUploadPoolUtil(){}

    private static final int limit = 1000;
    private static IOThreadPool pool = new IOThreadPool();

    private static final int nThreads = Runtime.getRuntime().availableProcessors();

    private static final String TYPE = "OBS_FILE_UPLOAD_QUEUE";

    public static boolean addFileToQueue(String localFilePath){
        try {
            File file = new File(localFilePath);
            if (file.exists()){
                //过滤不上传的文件后缀
                if (checkFileFilter(file.getName())) return true;

                String md5 = EncryptUtil.getFileMd5ByString(file);
                String remotePath = localFilePath.replace(WebServer.rootFolderStr,"");
                //判断队列是否存在,判断OBS是否存在
                if (checkOBSFileExist(remotePath,md5,true)) return true;

                boolean isAdd = addListValue(TYPE,localFilePath,md5,null);
                if (isAdd){
                    //Log4j.info(Thread.currentThread() + " 加入队列: "+ localFilePath);
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
                List<SQLiteUtils.StorageItem> list = getListByType(TYPE,nThreads / 2 * limit);
                if (list.size() != 0) {
                    uploadFileToOBSDistribution(list);
                }else{
                    synchronized (TYPE){
                        TYPE.wait();
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };


    private static void uploadFileToOBSDistribution(List<StorageItem> list) {
        if (pool == null){
            int size = list.size();
            uploadFileToOBS("("+size+")单线程上传",list);
        }else{
           multiUploadFileToOBS(list);
        }
    }

    private static void multiUploadFileToOBS(List<StorageItem> list) {
        int size = list.size();
        int threadSize = size / limit;
        final List<List<StorageItem>> group = new ArrayList<>();
        for (int i = 0; i<threadSize; i++){
            group.add(list.subList(i*limit,i*limit+limit));
        }
        int hav = size % limit;
        if (hav>0){
            group.add(list.subList(threadSize*limit, size));
        }
        try{

            final CountDownLatch countDownLatch = new CountDownLatch(group.size());

            int index = 0;
            for (final List<StorageItem> _list : group) {
                final int _index = index;
                pool.post(() -> {
                    uploadFileToOBS("多线程上传 "+group.size()+"-"+_index, _list);
                    countDownLatch.countDown();
                });
                index++;
            }
            countDownLatch.await();

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
        if (WebServer.hwObsIsOpen > 0){
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


    private static void uploadFileToOBS(String flag,List<SQLiteUtils.StorageItem> list) {
        String startTime = TimeTool.date_yMd_Hms_2Str(new Date());
        long stime = System.currentTimeMillis();
        for (StorageItem it : list) {

            boolean removeQueue = uploadFileToOBSExecute(it.value,it.identity);

//            String remotePath = it.value.replace(WebServer.rootFolderStr, "");
//            boolean isExist = checkOBSFileExist(remotePath, it.identity, false);
//
//            boolean removeQueue = isExist;
//            if (!isExist) {
//                removeQueue = HWOBSServer.uploadLocalFile(it.value, remotePath);
//            }

            if (removeQueue) {
                boolean isDelRes = removeListValue(TYPE, it.value, it.identity);
                if (!isDelRes)  Log4j.info("移除队列失败: " + it);
            }
        }

        long etime = System.currentTimeMillis();

        Log4j.info(Thread.currentThread()+ " >> "+ flag +
                " 开始时间: "+startTime+" " +
                " 总用时长: " +TimeTool.formatDuring(System.currentTimeMillis() - stime) +
                " 平均时长: "+TimeTool.formatDuring(( etime - stime)/list.size()));

    }

    private static boolean checkFileFilter(String fileName){
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
        final String[] filterArr = new String[]{"log","info","dev"};
        for (String filterSuffix : filterArr){
            if (suffix.equals(filterSuffix)) return true;
        }
        return false;
    }

    private static boolean checkOBSFileExist(String remotePath,String localFileMD5,boolean selectQueue) {
        if (selectQueue){
            //查询数据库是否存在, 存在则跳过
            boolean queueExist = SQLiteUtils.existIdentity(TYPE,localFileMD5);
            if(queueExist){
                return true;
            }
        }

        String remoteFileMD5 = HWOBSServer.getObsFileMD5(remotePath);
        return remoteFileMD5!=null && remoteFileMD5.equals(localFileMD5);
    }

    public static void start(){
        Thread t = new Thread(LOOP_QUEUE_RUNNABLE);
        t.setDaemon(true);
        t.setName("OBS上传-"+t.getId());
        t.start();
        Log4j.info("启动OBS文件上传");
    }

}
