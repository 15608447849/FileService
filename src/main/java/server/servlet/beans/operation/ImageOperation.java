package server.servlet.beans.operation;

import bottle.util.FileTool;
import bottle.util.Log4j;
import server.prop.WebProperties;
import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

import static server.servlet.beans.operation.OperationUtils.*;

/**
 * @Author: leeping
 * @Date: 2019/8/2 10:00
 * 图片处理
 */
public class ImageOperation{

    private  static final ConcurrentLinkedQueue<ImageOperation> queue = new ConcurrentLinkedQueue<>();


    private static final Runnable RUNNABLE = () -> {
        //循环读取队列中的任务
        while (true){
            ImageOperation op = queue.poll();
            if (op!=null) {
                op.execute();
            } else {
                synchronized (queue){
                    try {
                        queue.wait();
                    } catch (InterruptedException e) {
                        Log4j.error("文件服务错误",e);
                    }
                }
            };
        }
    };


    static {
        new Thread(RUNNABLE).start();
    }

    //源图片
    private File image;
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


    private ImageOperation(File image, int[] maxImageLimit, boolean isCompress, long spSize, boolean isLogo, boolean minScaleExist,String tailorStr) {
        this.image = image;
        this.maxImageLimit = maxImageLimit;
        this.isCompress = isCompress;
        this.spSize = spSize;
        this.isLogo = isLogo;
        this.minScaleExist = minScaleExist;
        this.tailorStr = tailorStr;
    }

    public static void add(File image, int[] maxImageLimit, boolean isCompress, long spSize, boolean isLogo, boolean minScaleExist,String tailorStr){
        if (image == null) return;
        queue.add(new ImageOperation(image,maxImageLimit,isCompress,spSize,isLogo,minScaleExist,tailorStr));
        synchronized (queue){
            queue.notify();
        }
    }

    /*图片处理执行执行*/
    private void execute(){
        try {
            Log4j.info(this);
            // 1. 图片最大限制处理
            maxImageLimitHandler();
            // 2. 是否添加水印
            logoHandler();
            // 3. 是否添加最小比例图
            minScaleExistHandler();
            // 4. 图片压缩处理
            imageCompressHandler();
            //  5. 裁剪处理
            tailorHandler();
        } catch (Exception e) {
            Log4j.error("文件服务错误",e);
        }
    }

    private void maxImageLimitHandler() {
        if (maxImageLimit!=null && maxImageLimit[0] > 0 && maxImageLimit[1] > 0){
            image = imageMaxSizeHandle(maxImageLimit,image);
        }
    }

    private void logoHandler() {
        if (isLogo){
            //文本水印
            image = markImageByText(WebProperties.logoText,image,WebProperties.logoTextRotate,parseToColor(WebProperties.logoTextColor),WebProperties.logoTextAlpha,WebProperties.logoTextPosition);
            //图片水印
            image = markImageByIcon(WebProperties.logoIcon,image,WebProperties.logoIconAlpha,WebProperties.logoIconRotate,WebProperties.logoIconPosition);

        }
    }

    private void minScaleExistHandler() {
        if (minScaleExist) {
            imageCompress_scale_min(image,new File(filePathAndStrToSuffix(image.getAbsolutePath(),"-min")));
        }
    }

    private void imageCompressHandler() {
        if (isCompress) {
            long time = System.currentTimeMillis();
            File temp = new File(filePathAndStrToSuffix(image.getAbsolutePath(),"-ing"));
            boolean flag = imageCompress(image,temp,spSize);
            Log4j.info("图片("+ image + ")压缩"+  ( flag ? "成功":"失败") +",耗时:" + (System.currentTimeMillis() - time )+" 毫秒");
            if (flag) {
                //重命名
                FileTool.rename(image,new File(filePathAndStrToSuffix(image.getAbsolutePath(),"-org"))) ; // 原文件 添加-org
                if (image.exists()) image.delete();
                FileTool.rename(temp,image) ; // 原文件 添加-org
            }

        }
    }

    private void tailorHandler() {
        if (tailorStr==null) return;
            String[] tailorArr = tailorStr.split(",");
            for (String sizeStr : tailorArr) {
                File dest = new File(filePathAndStrToSuffix(image.getAbsolutePath(),"-"+sizeStr));
                String[] sizeArr = sizeStr.split("x");
                int w = Integer.parseInt(sizeArr[0]);
                int h = Integer.parseInt(sizeArr[1]);
                imageResizeByGoogle(image,dest,w,h);
            }
    }


}
