package server.HuaWeiOBS;

import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.util.EncryptUtil;
import bottle.util.Log4j;
import bottle.util.TimeTool;
import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.model.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @Author: leeping
 * @Date: 2020/10/20 15:51
 */
@PropertiesFilePath("/hwobs.properties")
public class HWOBSServer {

    private static final String MAIN_URL = "obs.myhuaweicloud.com";
    private static final String MAIN_URL_FORMAT = "obs.%s.myhuaweicloud.com";
    private static final String MAIN_URL_RESOURCE_PREV = "https://%s.obs.%s.myhuaweicloud.com";

    @PropertiesName("userName")
    public static String userName;

    @PropertiesName("areau.endpoint.flag")
    private static String areaEndpointPrev;

    @PropertiesName("access.key.id")
    public static String accessKeyId;

    @PropertiesName("secret.access.key")
    public static String secretAccessKey;

    @PropertiesName("bucket.name")
    public static String bucketName;

    @PropertiesName("cdn.prev")
    public static String cdnURL;

    private static final ObsClient obsClient_bucket;
    private static final ObsClient obsClient_info;
    private static final ObsClient obsClient_upload;
    static {
        ApplicationPropertiesBase.initStaticFields(HWOBSServer.class);
        String endpoint = String.format(MAIN_URL_FORMAT,areaEndpointPrev);
        Log4j.info(String.format("连接OBS: %s , %s , %s , %s",accessKeyId,secretAccessKey,MAIN_URL,endpoint));
        obsClient_bucket = new ObsClient(accessKeyId,secretAccessKey,MAIN_URL);
        obsClient_info = new ObsClient(accessKeyId,secretAccessKey,endpoint);
        obsClient_upload = new ObsClient(accessKeyId,secretAccessKey,endpoint);
        noExistOnCreateBucket();
        printBucketList();
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

                Log4j.info("创建桶("+bucketName+") 区域("+areaEndpointPrev+") 成功, 结果:\n\t"+ response);
            }

        }catch (ObsException e){

            recodeException("创建桶("+bucketName+")失败",e);
        }
    }

    //桶列表
    private static void printBucketList(){
        try {
            ListBucketsRequest request = new ListBucketsRequest();
            request.setQueryLocation(true);
            List<ObsBucket> buckets = obsClient_bucket.listBuckets(request);
            StringBuilder sb = new StringBuilder("桶列表:");
            for(ObsBucket bucket : buckets){
                sb.append("\n\tCreationDate:" + TimeTool.date_yMd_Hms_2Str(bucket.getCreationDate()) +" , BucketName:" +  bucket.getBucketName() +" , "+ "Location:" + bucket.getLocation());
                showCors(sb,bucket.getBucketName());
                if (bucket.getBucketName().equals(bucketName)){
                    if (!bucket.getLocation().equals(areaEndpointPrev)){
                        Log4j.info("桶: "+ bucket.getBucketName()+" 实际位置: "+ bucket.getLocation() +" ,配置地址: "+ areaEndpointPrev+", 已转换");
                        areaEndpointPrev = bucket.getLocation();
                    }
                }
            }
            Log4j.info(sb);
        } catch (ObsException e) {
            recodeException("打印桶列表错误",e);

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

    //文件查询 , 根目录, 是否查询子目录,
    public static Set<String> ergodicDirectory(String dirPath, boolean isErgodicSubCatalog){
        Set<String> list = new HashSet<>();

        try {
            ListObjectsRequest request = new ListObjectsRequest(bucketName);
            request.setDelimiter("/");//设置文件分隔符

            if (dirPath.startsWith("/")){
                if (dirPath.length()>1){
                    dirPath = dirPath.substring(1);
                    if (!dirPath.endsWith("/")){
                        dirPath+="/";
                    }
                }
            }
            listObjectsByPrefix(dirPath,request,list,isErgodicSubCatalog);

        } catch (ObsException e) {
            recodeException("列举 区域("+areaEndpointPrev+") 桶("+bucketName+") 错误",e);

            list.clear();
        }
        return list;
    }

    private static void listObjectsByPrefix(String path,ListObjectsRequest request,Set<String> set,boolean isErgodicSubCatalog) throws ObsException {
        if (request==null || set==null) return;
        if (path.equals("/")){
            path = "";
        }
        request.setPrefix(path);
        ObjectListing result  = obsClient_info.listObjects(request);

        List<ObsObject> objects = result.getObjects();
        List<String> commonPrefixes = result.getCommonPrefixes();

        if (objects.size()>0){
            for(ObsObject obsObject : result.getObjects()){
                String str = obsObject.getObjectKey();
                if (str.equals(path)){
                    continue;
                }
                if (str.endsWith("/")){
                    if (isErgodicSubCatalog){
                        //System.out.println("遍历 objects >> 目录: " + str);
                        //目录
                        for(String prefix : result.getCommonPrefixes()){
                            listObjectsByPrefix(prefix,request,set,isErgodicSubCatalog);
                        }
                    }

                }else{
                    //System.out.println("文件: " + str);
                    //文件
                    set.add("/"+str);
                }
            }

        }
        if (commonPrefixes.size()>0){

            if (isErgodicSubCatalog){
                for(String prefix : result.getCommonPrefixes()){
                    if (prefix.equals(path)){
                        continue;
                    }
                    //System.out.println("遍历 commonPrefixes >> 目录: " + prefix);
                    listObjectsByPrefix(prefix,request,set,isErgodicSubCatalog);
                }
            }

        }
    }


    //文件删除
    public static boolean deleteFile(List<String> fileList){

        for (String path: fileList){
            try{
                if (path.startsWith("/")){
                    if (path.length()>1){
                        path = path.substring(1);
                    }else{
                        path = "";
                    }
                }
                DeleteObjectResult deleteResult = obsClient_info.deleteObject(bucketName, path);
                //System.out.println("删除: "+ path + " --> "+ deleteResult.isDeleteMarker()+" , "+ deleteResult.getObjectKey());
            }catch (ObsException e){
                recodeException("删除目标("+path+") 区域("+areaEndpointPrev+") 桶("+bucketName+") 错误",e);
            }
        }
        return true;
    }

    //结束客户端
    public static void stop(){
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

    //文件上传
    static boolean uploadLocalFile(String localPath, String remotePath){

        try {
            File file = new File(localPath);

            if (!file.exists() || file.length() > 5 * 1024 * 1024 * 1024L) {
                return true;
            }

            if (remotePath.startsWith("/")){
                if (remotePath.length()>1){
                    remotePath = remotePath.substring(1);
                }
            }

            UploadFileRequest  request = new UploadFileRequest(bucketName, remotePath);
            request.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ);
            request.setUploadFile(localPath);

            request.setTaskNum(5);
            request.setPartSize( file.length() / 10);
            request.setEnableCheckpoint(true);


            request.setProgressInterval(10 * 1024 * 1024L);
            request.setProgressListener(status -> {
                // 获取上传平均速率
//                System.out.println("上传平均速率 :" + status.getAverageSpeed());
                // 获取上传进度百分比
//                System.out.println(localPath+ " 上传进度百分比:" + status.getTransferPercentage());
            });


            long time = System.currentTimeMillis();
            CompleteMultipartUploadResult response = obsClient_upload.uploadFile(request);

//            Log4j.info(
//                    "上传文件("+localPath+") 耗时:"
//                    + TimeTool.formatDuring(System.currentTimeMillis() - time)
//                            +"\n\t访问路径: "
//                    + response.getObjectUrl()
//            );

            String md5 = null;
            try {
                md5 = EncryptUtil.getFileMd5ByString(new File(localPath));
            } catch (Exception e) {
                System.err.println("无法产生MD5 , file: "+ localPath);
            }

            try {
                if (md5!=null){
                    SetObjectMetadataRequest requestMeta = new SetObjectMetadataRequest(bucketName, remotePath);
                    requestMeta.getMetadata().put("md5", md5 );
                    ObjectMetadata metadata = obsClient_upload.setObjectMetadata(requestMeta);
                }
            } catch (ObsException e) {
                recodeException("上传文件("+remotePath+") 设置MD5失败 区域("+areaEndpointPrev+") 桶("+bucketName+") 错误", (ObsException) e);
            }
            return true;
        }catch (ObsException e){
            recodeException("上传文件("+localPath+" -> "+remotePath+") 区域("+areaEndpointPrev+") 桶("+bucketName+") 错误",e);
        }
        return false;
    }

    //存在返回MD5
    static String getObsFileMD5(String remotePath){
        if (remotePath.startsWith("/")){
            if (remotePath.length()>1){
                remotePath = remotePath.substring(1);
                if (remotePath.endsWith("/")){
                    return null;
                }
            }
        }
        try {
            ObjectMetadata metadata = obsClient_info.getObjectMetadata(bucketName,remotePath );
            Object md5 = metadata.getUserMetadata("md5");
            return md5 == null? null: String.valueOf(md5);
        } catch (ObsException e) {
            //recodeException("获取文件MD5失败 ("+remotePath+") 区域("+areaEndpointPrev+") 桶("+bucketName+") 错误",e);
        }
        return null;
    }

    //获取文件
    public static String convertLocalFileToOBSUrl(String remotePath){
        return String.format(MAIN_URL_RESOURCE_PREV,bucketName,areaEndpointPrev)+remotePath;
    }

    public static String convertLocalFileToCDNUrl(String remotePath){
        if (cdnURL == null) return convertLocalFileToOBSUrl("");
        return cdnURL+remotePath;
    }

    //文件下载

}
