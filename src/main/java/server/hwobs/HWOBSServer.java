package server.hwobs;

import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.util.*;
import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.model.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static server.comm.OperationUtil.getNetFileSizeDescription;

/**
 * @Author: leeping
 * @Date: 2020/10/20 15:51
 */
@PropertiesFilePath("/hwobs.properties")
public class HWOBSServer {

    private static final String MAIN_URL = "obs.myhuaweicloud.com";
    private static final String MAIN_URL_FORMAT = "obs.%s.myhuaweicloud.com";
    private static final String MAIN_URL_RESOURCE_PREV = "https://%s.obs.%s.myhuaweicloud.com";

    @PropertiesName("hwobs.enable")
    protected static boolean isEnable = false;

    @PropertiesName("hwobs.userName")
    public static String userName;

    @PropertiesName("hwobs.areau.endpoint.flag")
    private static String areaEndpointPrev;

    @PropertiesName("hwobs.access.key.id")
    public static String accessKeyId;

    @PropertiesName("hwobs.secret.access.key")
    public static String secretAccessKey;

    @PropertiesName("hwobs.bucket.name")
    public static String bucketName;

    @PropertiesName("hwobs.cdn")
    public static String cdnURL;

    private static ObsClient obsClient_bucket;
    private static ObsClient obsClient_info;
    private static ObsClient obsClient_upload;

    static {
        ApplicationPropertiesBase.initStaticFields(HWOBSServer.class);
        if (isEnable) {
            String endpoint = String.format(MAIN_URL_FORMAT,areaEndpointPrev);
            Log4j.info(String.format("[OBS]连接: %s , %s , %s , %s",accessKeyId,secretAccessKey,MAIN_URL,endpoint));
            obsClient_bucket = new ObsClient(accessKeyId,secretAccessKey,MAIN_URL);
            obsClient_info = new ObsClient(accessKeyId,secretAccessKey,endpoint);
            obsClient_upload = new ObsClient(accessKeyId,secretAccessKey,endpoint);
            noExistOnCreateBucket();
            printBucketList();
        }

    }

    private static void recodeException(String flag , ObsException e){
        Log4j.info(flag +
                "\n\tHTTP Code: " +e.getResponseCode() +
                "\n\tError Code: " +e.getErrorCode() +
                "\n\tError Message: " +e.getErrorMessage() +
                "\n\tRequest ID: " +e.getErrorRequestId() +
                "\n\tHost ID: " +e.getErrorHostId());
    }

    // 不存在则创建桶
    private static void noExistOnCreateBucket(){
        try{
            boolean exists = obsClient_bucket.headBucket(bucketName);
            if (!exists){
                ObsBucket obsBucket = new ObsBucket();
                obsBucket.setBucketName(bucketName);
                // 设置桶访问权限为公共读写
                obsBucket.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ_WRITE);
                // 设置桶的存储类型为标准存储
                obsBucket.setBucketStorageClass(StorageClassEnum.STANDARD);
                // 设置桶区域位置
                obsBucket.setLocation(areaEndpointPrev);

                HeaderResponse response = obsClient_bucket.createBucket(obsBucket);

                Log4j.info("[OBS]创建桶("+bucketName+") 区域("+areaEndpointPrev+") 成功, 结果:\n\t"+ response);
            }

        }catch (ObsException e){
            recodeException("[OBS]创建桶("+bucketName+")失败",e);
        }
    }

    //桶列表
    private static void printBucketList(){
        try {
            ListBucketsRequest request = new ListBucketsRequest();
            request.setQueryLocation(true);
            List<ObsBucket> buckets = obsClient_bucket.listBuckets(request);
            StringBuilder sb = new StringBuilder("[OBS]桶列表:");
            for(ObsBucket bucket : buckets){
                //sb.append("\n\tCreationDate:" + TimeTool.date_yMd_Hms_2Str(bucket.getCreationDate()) +" , BucketName:" +  bucket.getBucketName() +" , "+ "Location:" + bucket.getLocation());
                showCors(sb,bucket.getBucketName());
                if (bucket.getBucketName().equals(bucketName)){
                    sb.append("\tCreationDate:" + TimeTool.date_yMd_Hms_2Str(bucket.getCreationDate()) +" , BucketName:" +  bucket.getBucketName() +" , "+ "Location:" + bucket.getLocation());
                    if (!bucket.getLocation().equals(areaEndpointPrev)){
                        Log4j.info("桶: "+ bucket.getBucketName()+" 实际位置: "+ bucket.getLocation() +" ,配置地址: "+ areaEndpointPrev+", 已转换");
                        areaEndpointPrev = bucket.getLocation();
                    }
                }
            }
            Log4j.info(sb);
        } catch (ObsException e) {
            recodeException("[OBS]打印桶列表错误",e);

        }
    }

    //跨域规则
    private static void showCors(StringBuilder sb,String bucketName) {

        try {
            BucketCors cors = obsClient_bucket.getBucketCors(bucketName);
            for(BucketCorsRule rule : cors.getRules()){
                sb.append("\t\t\n" + rule.getId());
                sb.append("\t\t\n" + rule.getMaxAgeSecond());
                sb.append("\t\t\n" + rule.getAllowedHeader());
                sb.append("\t\t\n" + rule.getAllowedOrigin());
                sb.append("\t\t\n" + rule.getAllowedMethod());
                sb.append("\t\t\n" + rule.getExposeHeader());
            }
        } catch (ObsException e) {
            //recodeException("打印桶跨域规则",e);
        }
    }


    /* 指定目录查询文件列表, isErgodicSubCatalog = 是否查询子目录 */
    static Set<String> ergodicDirectory(String dirPath, boolean isErgodicSubCatalog){
//        Log4j.info("[OBS] 遍历指定文件夹: "+ dirPath+" 是否遍历子目录: "+ isErgodicSubCatalog);

        dirPath = dirPath.replace("\\", FileTool.SEPARATOR);
        Set<String> list = new HashSet<>();
        try {
            if (isEnable) {
                ListObjectsRequest request = new ListObjectsRequest(bucketName);
                request.setDelimiter("/");//设置文件分隔符
                if (dirPath.startsWith("/")) {
                    if (dirPath.length() > 1) {
                        dirPath = dirPath.substring(1);
                        if (!dirPath.endsWith("/")) {
                            dirPath += "/";
                        }
                    }
                }
                listObjectsByPrefix(dirPath, request, list, isErgodicSubCatalog, null);
            }
        } catch (ObsException e) {
            recodeException("[OBS]列举 ("+dirPath+") 区域("+areaEndpointPrev+") 桶("+bucketName+") 错误",e);
            list.clear();
        }
        return list;
    }


    // 遍历
    private static void listObjectsByPrefix(String path, ListObjectsRequest request,Set<String> set,boolean isErgodicSubCatalog, String marker) throws ObsException {
        if (request==null || set==null) return;
        if (path.equals("/")){
            path = "";
        }
        request.setPrefix(path);
        request.setMarker(marker);

        ObjectListing result  = obsClient_info.listObjects(request);
        List<ObsObject> objects = result.getObjects();
        List<String> commonPrefixes = result.getCommonPrefixes();

        if (objects.size()>0){
            for(ObsObject obsObject : objects){
                String str = obsObject.getObjectKey();
                if (str.equals(path)){
                    continue;
                }
                if (str.endsWith("/")){
                    if (isErgodicSubCatalog){
                        //目录
                        for(String prefix : result.getCommonPrefixes()){
                            listObjectsByPrefix(prefix,request,set, true, null);
                        }
                    }
                }else{
                    //文件
                    set.add("/"+str);
                }
            }

            if(result.isTruncated()){
                // 包含的对象数量大于1000执行下次遍历的位置
                listObjectsByPrefix(path,request,set, isErgodicSubCatalog, result.getNextMarker());
            }

        }
        if (commonPrefixes.size()>0){
            if (isErgodicSubCatalog){
                for(String prefix : result.getCommonPrefixes()){
                    if (prefix.equals(path)){
                        continue;
                    }
                    listObjectsByPrefix(prefix,request,set, true,null);
                }
            }

        }
    }

    //文件存在则返回MD5
    static String getFileMD5(String remotePath){
//        Log4j.info("[OBS] 查询指定文件MD5: "+ remotePath);
        remotePath = remotePath.replace("\\",FileTool.SEPARATOR);

        if (remotePath.startsWith("/")){
            if (remotePath.length()>1){
                remotePath = remotePath.substring(1);
                if (remotePath.endsWith("/")){
                    return null;
                }
            }
        }

        try {
            if (isEnable) {
                ObjectMetadata metadata = obsClient_info.getObjectMetadata(bucketName,remotePath );
                Object md5 = metadata.getUserMetadata("md5");
                return md5 == null? null: String.valueOf(md5);
            }

        } catch (ObsException e) {
            //recodeException("[OBS]获取文件MD5失败 ("+remotePath+") 区域("+areaEndpointPrev+") 桶("+bucketName+") 错误",e);
        }catch (Exception ee){
            Log4j.info("[OBS] 查询指定文件MD5 "+ remotePath+"  错误 "+ ee);
        }
        return null;
    }

    // 查询指定文件路径
    static boolean existFile(String remotePath){
//        Log4j.info("[OBS] 查询指定文件是否存在: "+ remotePath);
        remotePath = remotePath.replace("\\",FileTool.SEPARATOR);

        if (remotePath.startsWith("/")){
            if (remotePath.length()>1){
                remotePath = remotePath.substring(1);
                if (remotePath.endsWith("/")){
                    return false;
                }
            }
        }

        try {
            if (isEnable) {
                ObjectMetadata metadata = obsClient_info.getObjectMetadata(bucketName, remotePath);
                return true;
            }
        } catch (ObsException e) {
            //recodeException("[OBS]列举 区域("+areaEndpointPrev+") 桶("+bucketName+") 错误",e);
        }catch (Exception ee){
            Log4j.info("[OBS] 查询指定文件是否存在 "+ remotePath+"  错误 "+ ee);
        }
        return false;
    }


    //文件删除
    static void deleteFileList(List<String> fileList){
        for (String path: fileList){
           if (path==null || path.length()==0) continue;
            deleteFile(path);
        }

    }

    static boolean deleteFile(String path){
        path = path.replace("\\",FileTool.SEPARATOR);
        if (path.startsWith("/")){
            if (path.length()>1){
                path = path.substring(1);
            }else{
                path = "";
            }
        }
        try{
            if (isEnable) {
                DeleteObjectResult deleteResult = obsClient_info.deleteObject(bucketName, path);
                Log4j.info("[OBS]删除文件: "+ path + " --> "+ deleteResult.isDeleteMarker()+" , "+ deleteResult.getObjectKey());
                return deleteResult.isDeleteMarker();
            }
        }catch (ObsException e){
            recodeException("[OBS]删除文件("+path+") 区域("+areaEndpointPrev+") 桶("+bucketName+") 错误",e);
        }
        return false;
    }


    //文件上传
    static boolean uploadLocalFile(String localPath, String remotePath,String localFileMD5){
        try {
            if (!isEnable) return false;

            if (StringUtil.isEmpty(localPath,remotePath,localFileMD5)) return false;

            File file = new File(localPath);
            if (!file.exists() || file.length()<=0 || file.length() >= 5 * 1024 * 1024 * 1024L) {
                return false;
            }

            remotePath =   remotePath.replace("\\",FileTool.SEPARATOR);

            if (remotePath.startsWith("/")){
                if (remotePath.length()>1){
                    remotePath = remotePath.substring(1);
                }
            }

            UploadFileRequest  request = new UploadFileRequest(bucketName, remotePath);
            request.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ);
            request.setUploadFile(localPath);

            request.setTaskNum(4);
            request.setPartSize( file.length() / 4);
            request.setEnableCheckpoint(true);

            request.setProgressInterval(1024 * 1024L);

            long time = System.currentTimeMillis();

            request.setProgressListener(status -> {

                Log4j.debug("[OBS]上传文件("+localPath+")"
                        + " ,上传进度百分比:" + status.getTransferPercentage()
                        + " ,上传平均速率: " + getNetFileSizeDescription((long)(Math.ceil(status.getAverageSpeed())))
                        + " ,已用时长: " +  TimeTool.formatDuring(System.currentTimeMillis() - time)
                );
            });

            try {
                CompleteMultipartUploadResult response = obsClient_upload.uploadFile(request);
                Log4j.info("[OBS]上传成功 用时: "+ TimeTool.formatDuring(System.currentTimeMillis() - time)+"\n"+ response);
            } catch (ObsException e) {
                recodeException("[OBS]上传文件("+remotePath+") 失败 区域("+areaEndpointPrev+") 桶("+bucketName+") 错误", e);
                return false;
            }

            try {
                SetObjectMetadataRequest requestMeta = new SetObjectMetadataRequest(bucketName, remotePath);
                requestMeta.getMetadata().put("md5", localFileMD5 );
                ObjectMetadata metadata = obsClient_upload.setObjectMetadata(requestMeta);
            } catch (ObsException e) {
                recodeException("[OBS]上传文件("+remotePath+") 设置MD5失败 区域("+areaEndpointPrev+") 桶("+bucketName+") 错误", e);
            }

            return true;
        }catch (ObsException e){
            recodeException("[OBS]上传文件("+localPath+" -> "+remotePath+") 区域("+areaEndpointPrev+") 桶("+bucketName+") 错误",e);
        }
        return false;
    }

    //获取文件访问OBS URL
    public static String convertLocalFileToOBSUrl(String remotePath){
        return String.format(MAIN_URL_RESOURCE_PREV,bucketName,areaEndpointPrev)+remotePath;
    }

    //获取文件访问CDN URL
    public static String convertLocalFileToCDNUrl(String remotePath){
        if (cdnURL == null) return convertLocalFileToOBSUrl(remotePath);
        return cdnURL+remotePath;
    }

    //结束客户端
    static void stop(){
        try {
            obsClient_bucket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            obsClient_info.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            obsClient_upload.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }







}
