package server.servlet.beans;

import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.util.*;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import server.hwobs.HWOBSAgent;
import server.servlet.imps.FileErgodic;
import server.sqlites.tables.SQLiteFileTable;
import server.undertow.CustomResourceManager;
import server.undertow.WebServer;

import javax.imageio.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static server.comm.OperationUtil.*;
import static server.comm.SuffixConst.IMG_SUFFIX_ARRAY;
import static server.sqlites.tables.SQLiteListTable.*;

/**
 * @Author: leeping
 * @Date: 2019/8/2 10:00
 * 图片处理
 */

@PropertiesFilePath("/web.properties")
public class ImageOperation{

    @PropertiesName("image.logo.text.position")
    public static int logoTextPosition = 0;
    @PropertiesName("image.logo.text")
    public static String logoText = null;
    @PropertiesName("image.logo.text.rotate")
    public static int logoTextRotate = 0;
    @PropertiesName("image.logo.text.color")
    public static String logoTextColor = "000000";
    @PropertiesName("image.logo.text.alpha")
    public static float logoTextAlpha = 0.15f;

    @PropertiesName("image.logo.icon")
    public static String logoIcon = null;
    @PropertiesName("image.logo.icon.position")
    public static int logoIconPosition = 0;
    @PropertiesName("image.logo.icon.alpha")
    public static float logoIconAlpha = 0.15f;
    @PropertiesName("image.logo.icon.rotate")
    public static int logoIconRotate = 0;

    static {
        ApplicationPropertiesBase.initStaticFields(ImageOperation.class);
    }

    private static final String TYPE = "IMAGE_FILE_HANDLE_QUEUE";

    private static final LinkedBlockingQueue<String[]> queue = new LinkedBlockingQueue<>();

    public static void add(String imagePath, int[] maxImageLimit, boolean isCompress, long spSize, boolean isLogo, boolean minScaleExist, String tailorStr){
        try {
            String json = GoogleGsonUtil.javaBeanToJson(new ImageOperation(imagePath,maxImageLimit,isCompress,spSize,isLogo,minScaleExist,tailorStr));
            String[] args = new String[]{json,imagePath};
            queue.put(args);
        } catch (Exception e) {
           Log4j.error("文件加入图片处理队列异常 "+imagePath,e);
        }
    }

    private static final Runnable RUNNABLE_WRITE_DB =() ->{
        while (true){
            try {
                String[] args = queue.take();
                String json = args[0];
                String imagePath = args[1];

                boolean isAdd = addListValue(TYPE,imagePath,json);
                if (isAdd){
                    synchronized (TYPE){
                        TYPE.notifyAll();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private static final Runnable RUNNABLE_READ_DB = () -> {
        //循环读取队列中的任务
        while (true){
            try{
                List<ListStorageItem> list = getListByType(TYPE,100,0);
                if (list.size() > 0) {
                    executeHandler(list);
                }else{
                    synchronized (TYPE){
                        TYPE.wait(300 * 1000L);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    private static void executeHandler(List<ListStorageItem> list) {

        for (ListStorageItem it : list){
            ImageOperation imageOperation = GoogleGsonUtil.jsonToJavaBean(it.attach,ImageOperation.class);
            List<String> filePathAll = imageOperation.execute();
            removeListValue(TYPE,it.value);
            if (filePathAll!=null && filePathAll.size()>0){
                addFileToSQLiteRecode(filePathAll);
                HWOBSAgent.addFileToQueue(filePathAll);
            }

        }
    }

    private static void addFileToSQLiteRecode(List<String> localFilePaths) {
        if (localFilePaths == null) return;

        for (String localPath : localFilePaths){
            String relativePath = localPath.replace(WebServer.rootFolderStr,"");
            String httpUrl = WebServer.domain + relativePath;
            SQLiteFileTable.addFile_LOCAL(relativePath,httpUrl);// 添加记录
            CustomResourceManager.removeCache(relativePath);// 移除缓存
            FileErgodic.removeCache(relativePath);//移除缓存
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
        Log4j.info("图片("+imagePath+")处理参数: " +
                " 最大限制:"+ Arrays.toString(maxImageLimit) +
                " 是否压缩:"+ isCompress +
                " 压缩阈值:"+ spSize +
                " 添加LOGO:"+ isLogo +
                " 缩略图:"+ minScaleExist +
                " 裁剪比例:"+ tailorStr
        );
    }

    private File image;
    private List<String> filePathAll = new ArrayList<>();

    /* 图片处理执行执行 */
    private List<String> execute(){
        try {
            image = new File(imagePath);
            if (image.exists()) {
                filePathAll.add(imagePath);
                // 检查是否是图片
                if (isImageType(image)) {
                    // 图片压缩
                    1();
                    // 图片最大限制处理
                    maxImageLimitHandler();
                    // 水印
                    logoHandler();
                    // 缩略图
                    minScaleExistHandler();
                    // 裁剪处理
                    tailorHandler();
                }
            }
        } catch (Exception e) {
            Log4j.error("处理图片异常 "+image,e);
        }
        return filePathAll;
    }

    // 图片最大限制
    private void maxImageLimitHandler() {
        try {
            if (maxImageLimit!=null && maxImageLimit[0] > 0 && maxImageLimit[1] > 0){
                int width = maxImageLimit[0];
                int height = maxImageLimit[1];
                String image_limit_path = filePathAndStrToSuffix(imagePath,IMG_SUFFIX_ARRAY[0]);
                boolean f = imageMaxSizeHandle(width,height,image,new File(image_limit_path));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //2 水印
    private void logoHandler() {
        if (!isLogo) return;

        try {
            //文本水印
            String image_mark_path = filePathAndStrToSuffix(imagePath,IMG_SUFFIX_ARRAY[1]);
            File mark = new File(image_mark_path);
            boolean f = markImageByText(image, mark,
                    logoText,logoTextRotate, parseToColor(logoTextColor), logoTextAlpha, logoTextPosition);
            if (f) image = mark;

            //图片水印
            f = markImageByIcon(image, mark, logoIcon, logoIconAlpha, logoIconRotate, logoIconPosition);
            if (f) image = mark;

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //最小比例(缩略图)
    private void minScaleExistHandler() {
        if (!minScaleExist) return;

        try {
            String min = filePathAndStrToSuffix(imagePath,IMG_SUFFIX_ARRAY[2]);
            boolean isSuccess = imageCompress_scale_min(image,new File(min));
            if (isSuccess) filePathAll.add(min);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //4 压缩
    private void imageCompressHandler() {
        if (!isCompress)  return;

        try {
            String image_ing = filePathAndStrToSuffix(imagePath,IMG_SUFFIX_ARRAY[3]);

            File ing = new File(image_ing);// 临时文件

            long time = System.currentTimeMillis();
            boolean isSuccess = imageCompressUseTinypng(image,ing,spSize);
            Log4j.info("图片 "+ image
                    + " 压缩"+  ( isSuccess ? "成功":"失败")
                    + " 耗时:" + TimeTool.formatDuring( System.currentTimeMillis() - time )
                    + " 变化: "+image.length()+" -> "+ing.length());

            if (isSuccess) {
                //重命名
                String image_org = filePathAndStrToSuffix(imagePath,IMG_SUFFIX_ARRAY[4]);
                FileTool.rename(image,new File(image_org)) ; // 当前文件->添加-org
                FileTool.rename(ing,image) ; // 临时文件->当前文件
                filePathAll.add(image_org);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // 制作指定大小裁剪图片
    private void tailorHandler() {
        if (tailorStr==null) return;

        try {
            String[] tailorArr = tailorStr.split(",");
            for (String sizeStr : tailorArr) {
                String image_tailor = filePathAndStrToSuffix(image.getAbsolutePath(),"-"+sizeStr);
                File dest = new File(image_tailor);
                String[] sizeArr = sizeStr.split("x");
                int w = Integer.parseInt(sizeArr[0]);
                int h = Integer.parseInt(sizeArr[1]);
                boolean isSuccess = imageResizeByGoogle(image,dest,w,h);
                if (isSuccess) filePathAll.add(image_tailor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void start(){
        Thread t_write = new Thread(RUNNABLE_WRITE_DB);
        t_write.setDaemon(true);
        t_write.setName("img-handle-write-"+t_write.getId());
        t_write.start();

        Thread t_read = new Thread(RUNNABLE_READ_DB);
        t_read.setDaemon(true);
        t_read.setName("img-handle-read-"+t_read.getId());
        t_read.start();

    }

    public static void main(String[] args) throws Exception {

         File image = new File("D:\\A_Java\\JavaProjects\\IDEAWORK\\FileService\\file\\media\\drug\\1653466937000001026\\1.jpg");
         File dest = new File("D:\\A_Java\\JavaProjects\\IDEAWORK\\FileService\\file\\media\\drug\\1653466937000001026\\out.jpg");
        //文本水印
        markImageByText(image,dest,logoText, logoTextRotate, parseToColor(logoTextColor), logoTextAlpha, logoTextPosition);
//        markImageByIcon("D:\\A_Java\\JavaProjects\\IDEAWORK\\FileService\\file\\media\\drug\\1653466937000001026\\1.jpg", image, logoIconAlpha, logoIconRotate, logoIconPosition);


     /*
        String watermarkText = "Watermark111111111111";
        File inputImageFile = new File("D:\\A_Java\\JavaProjects\\IDEAWORK\\FileService\\file\\media\\drug\\1653466937000001026\\8.jpg"); // 输入图片路径
        File outputImageFile = new File("D:\\A_Java\\JavaProjects\\IDEAWORK\\FileService\\file\\media\\drug\\1653466937000001026\\OUT_111.jpg"); // 输出图片路径

        BufferedImage  inputImage = ImageIO.read(inputImageFile);
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();

        Graphics2D g2d = (Graphics2D) inputImage.getGraphics();
        AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f); // 设置水印透明度
        g2d.setComposite(alphaChannel);
        g2d.setColor(Color.BLUE); // 设置水印颜色
        g2d.setFont(new Font("Arial", Font.BOLD, 30)); // 设置水印字体和大小
        g2d.drawString(watermarkText, 0, height - 500); // 在图片上绘制水印文字
        g2d.dispose();

        ImageIO.write(inputImage, "jpg", outputImageFile); // 输出图片
*/
        // 设置压缩参数
//        ImageWriter jpegWriter = ImageIO.getImageWritersByFormatName("jpg").next();
//        ImageWriteParam jpegWriteParam = jpegWriter.getDefaultWriteParam( );
//        jpegWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//        jpegWriteParam.setCompressionQuality(1.0f); // 设置压缩质量，1.0为最高质
//
//        File compressedImageFile = new File(outputImageFile.getPath());
//        IIOImage image = new IIOImage(inputImage, null, null);
//        jpegWriter.setOutput(ImageIO.createImageOutputStream(compressedImageFile));
//        jpegWriter.write(null, image, jpegWriteParam);
//        jpegWriter.dispose();

        /*
        File inputImageFile = new File("D:\\A_Java\\JavaProjects\\IDEAWORK\\FileService\\file\\media\\drug\\1653466937000001026\\9.jpg"); // 输入图片路径
        File outputImageFile = new File("D:\\A_Java\\JavaProjects\\IDEAWORK\\FileService\\file\\media\\drug\\1653466937000001026\\OUT_111.jpg"); // 输出图片路径
        BufferedImage  inputImage = ImageIO.read(inputImageFile);
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        Thumbnails.of(inputImageFile)
                .size(width,height)
//
                .watermark(Positions.BOTTOM_RIGHT,
                        ImageIO.read(new File("D:\\A_Java\\JavaProjects\\IDEAWORK\\FileService\\file\\media\\drug\\1653466937000001026\\111.jpg")), 0.5f) // 设置水印
                .outputQuality(1.0) // 输出质量
                .toFile(outputImageFile);
*/

    }


}
