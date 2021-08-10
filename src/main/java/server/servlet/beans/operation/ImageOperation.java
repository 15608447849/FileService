package server.servlet.beans.operation;

import bottle.threadpool.IOThreadPool;
import bottle.util.FileTool;
import bottle.util.GoogleGsonUtil;
import bottle.util.Log4j;
import server.HuaWeiOBS.HWOBSServer;
import server.HuaWeiOBS.OBSUploadPoolUtil;
import server.prop.WebServer;
import server.sqlites.SQLiteUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static server.servlet.beans.operation.OperationUtils.*;
import static server.sqlites.SQLiteUtils.*;

/**
 * @Author: leeping
 * @Date: 2019/8/2 10:00
 * 图片处理
 */
public class ImageOperation{

    private static final String TYPE = "IMAGE_FILE_HANDLE_QUEUE";

    private static final LinkedBlockingQueue<String[]> queue = new LinkedBlockingQueue<>();

    public static void add(String imagePath, int[] maxImageLimit, boolean isCompress, long spSize, boolean isLogo, boolean minScaleExist, String tailorStr){
        String json = GoogleGsonUtil.javaBeanToJson(new ImageOperation(imagePath,maxImageLimit,isCompress,spSize,isLogo,minScaleExist,tailorStr));
        String[] args = new String[]{json,imagePath};
        try {
            queue.put(args);
        } catch (Exception e) {
           Log4j.info("文件加入队列失败", Arrays.toString(args));
        }
    }

    private static final Runnable RUNNABLE_WRITE_DB =() ->{
        while (true){
            try {
                String[] args = queue.take();
                String json = args[0];
                String imagePath = args[1];

                boolean isAdd = addListValue(TYPE,json,imagePath,null);
                //        Log4j.info("添加文件处理: "+ imagePath + "  "+ isAdd);
                if (isAdd){
                    synchronized (TYPE){
                        TYPE.notifyAll();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    private static final Runnable RUNNABLE_READ_DB = () -> {
        //循环读取队列中的任务
        while (true){
            try{
                List<SQLiteUtils.StorageItem> list = getListByType(TYPE,100);
                if (list.size() != 0) {
                    executeHandler(list);
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

    private static void executeHandler(List<SQLiteUtils.StorageItem> list) {

        for (SQLiteUtils.StorageItem it : list){
            ImageOperation imageOperation = GoogleGsonUtil.jsonToJavaBean(it.value,ImageOperation.class);
            boolean isDelete;
            if (imageOperation==null){
                isDelete = true;
            }else {
                isDelete = imageOperation.execute();
            }
            if (isDelete)  removeListValue(TYPE,it.value,it.identity);
        }
    }

    //源图片路径
    private String imagePath;
    //图片最大限制 ,过大将裁剪,过小将设置白底
    private int[] maxImageLimit = null;
    //是否压缩
    private boolean isCompress = false;
    //压缩到指定范围内
    private long spSize = 0L;
    //是否添加水印
    private boolean isLogo = false;
    //是否存在最小比例图
    private boolean minScaleExist = false;
    //裁剪信息 例如  200x200,600x600,1200x1200
    private String tailorStr = null;

    private ImageOperation(String imagePath, int[] maxImageLimit, boolean isCompress, long spSize, boolean isLogo, boolean minScaleExist,String tailorStr) {
        this.imagePath = imagePath;
        this.maxImageLimit = maxImageLimit;
        this.isCompress = isCompress;
        this.spSize = spSize;
        this.isLogo = isLogo;
        this.minScaleExist = minScaleExist;
        this.tailorStr = tailorStr;
    }

    private File image;
    private List<String> filePathAll = new ArrayList<>();

    /*图片处理执行执行*/
    private boolean execute(){
        try {
            image = new File(imagePath);
            if (!image.exists()) {
                Log4j.info("文件不存在 "+image);
                return true;
            }
            filePathAll.add(imagePath);
            // 检查是否是图片
            if (isImageSuffix(image)) {
                //1. 图片最大限制处理
                maxImageLimitHandler();
                //2. 是否添加水印
                logoHandler();
                //3. 是否添加最小比例图
                minScaleExistHandler();
                //4. 图片压缩处理
                imageCompressHandler();
                //5. 裁剪处理
                tailorHandler();
            }
           OBSUploadPoolUtil.addFileToQueue(filePathAll);
           return true;
        } catch (Exception e) {
            Log4j.error("文件服务错误 处理图片: "+image,e);
        }
        return false;
    }

    // 1 图片最大限制
    private void maxImageLimitHandler() {
        if (maxImageLimit!=null && maxImageLimit[0] > 0 && maxImageLimit[1] > 0){
            image = imageMaxSizeHandle(maxImageLimit,image);
        }
    }

    //2 水印
    private void logoHandler() {
        if (isLogo){
            //文本水印
            image = markImageByText(WebServer.logoText,image, WebServer.logoTextRotate,parseToColor(WebServer.logoTextColor), WebServer.logoTextAlpha, WebServer.logoTextPosition);
            //图片水印
            image = markImageByIcon(WebServer.logoIcon,image, WebServer.logoIconAlpha, WebServer.logoIconRotate, WebServer.logoIconPosition);
        }
    }

    //3 最小比例
    private void minScaleExistHandler() {
        if (minScaleExist) {
            String image_min = filePathAndStrToSuffix(imagePath,"-min");
            boolean isSuccess = imageCompress_scale_min(image,new File(image_min));
            if (isSuccess) filePathAll.add(image_min);
        }
    }

    //4 压缩
    private void imageCompressHandler() {
        if (isCompress) {
            String image_ing = filePathAndStrToSuffix(imagePath,"-ing");
            String image_org = filePathAndStrToSuffix(imagePath,"-org");
            long time = System.currentTimeMillis();
            File temp = new File(image_ing);// 压缩中的临时文件
            boolean isSuccess = imageCompress(image,temp,spSize);
            Log4j.info("图片("+ image + ")压缩"+  ( isSuccess ? "成功":"失败") +",耗时:" + (System.currentTimeMillis() - time )+" 毫秒");
            if (isSuccess) {
                //重命名
                FileTool.rename(image,new File(image_org)) ; // 当前文件->添加-org
                if (image.exists()) {
                    if (!image.delete())  return;
                };
                FileTool.rename(temp,image) ; // 临时文件->当前文件
                filePathAll.add(image_org);
            }
        }
    }

    //5 指定大小裁剪
    private void tailorHandler() {
        if (tailorStr==null) return;
            String[] tailorArr = tailorStr.split(",");
            for (String sizeStr : tailorArr) {
                String image_tailor = filePathAndStrToSuffix(image.getAbsolutePath(),"-"+sizeStr);
                File dest = new File(image_tailor);
                String[] sizeArr = sizeStr.split("x");
                int w = Integer.parseInt(sizeArr[0]);
                int h = Integer.parseInt(sizeArr[1]);
                boolean isSuccess = imageResizeByGoogle(image,dest,w,h);
                if (isSuccess){
                    filePathAll.add(image_tailor);
                }
            }
    }



    public static void start(){
        Thread t_write = new Thread(RUNNABLE_WRITE_DB);
        t_write.setDaemon(true);
        t_write.setName("图片处理-写入DB-"+t_write.getId());
        t_write.start();

        Thread t_read = new Thread(RUNNABLE_READ_DB);
        t_read.setDaemon(true);
        t_read.setName("图片处理-读取DB-"+t_read.getId());
        t_read.start();
        Log4j.info("启动图片处理");
    }

}
