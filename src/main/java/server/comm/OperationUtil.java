package server.comm;


import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.util.FileTool;
import bottle.util.HttpUtil;
import bottle.util.Log4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

/**
 * @Author: leeping
 * @Date: 2019/4/1 14:54
 */
@PropertiesFilePath("/web.properties")
public class OperationUtil {


    @PropertiesName("image.use.tinypng.url")
    private static  String tinypng_url;

    static {
        ApplicationPropertiesBase.initStaticFields(OperationUtil.class);
    }

    // 获取人性化文件大小
    public static String getNetFileSizeDescription(long size) {
        StringBuilder bytes = new StringBuilder();
        DecimalFormat format = new DecimalFormat("###.0");
        if (size >= 1024 * 1024 * 1024) {
            double i = (size / (1024.0 * 1024.0 * 1024.0));
            bytes.append(format.format(i)).append("GB");
        }
        else if (size >= 1024 * 1024) {
            double i = (size / (1024.0 * 1024.0));
            bytes.append(format.format(i)).append("MB");
        }
        else if (size >= 1024) {
            double i = (size / (1024.0));
            bytes.append(format.format(i)).append("KB");
        }
        else {
            if (size <= 0) {
                bytes.append("0B");
            }
            else {
                bytes.append((int) size).append("B");
            }
        }
        return bytes.toString();
    }

    public static String fileSuffix(File file){
        String fileName = file.getName();

        String suffix =  fileName.substring(fileName.lastIndexOf(".") + 1);

        return suffix;
    }

    // 文件后缀变更
    public static String filePathAndStrToSuffix(String path,String var){
        int i = path.lastIndexOf(".");
        String startStr = path.substring(0,i);
        String endStr = path.substring(i);
        return startStr + var + endStr;
    }


    public static String getIndexValue(List<String> dataList, int index, String def){
        try {
            if (dataList!=null) {
                String val = null;

                if (dataList.size()>index) {
                    val = dataList.get(index);
                }

                if (val!=null) return val;
            }
        } catch (Exception e) {
            Log4j.error("",e);
        }
        return def;
    }

   /**
    人性化显示文件大小
    */
    public static String fileSizeFormat(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "K";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "M";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + "G";
        }
        return fileSizeString;
    }

    /**
     * 图片裁剪
     *
     * @param w int 新宽度
     * @param h int 新高度
     */
    public static boolean imageResizeByGoogle(File image, File dist, int w, int h) {
        try(FileOutputStream fos = new FileOutputStream(dist)){
            Thumbnails.of(image).size(w,h).keepAspectRatio(false).outputQuality(1.0f).toOutputStream(fos);
            return true;
        }catch (Exception e){
            Log4j.error("图片大小裁剪异常 "+ image,e);
        }
        return false;
    }

    public static boolean imageCompress_scale_min(File image,File dist){
        try(FileOutputStream fos = new FileOutputStream(dist)){
            Thumbnails.of(image).scale(0.1).outputQuality(0.1f).toOutputStream(fos);
            return true;
        } catch (Exception e) {
            Log4j.error("图片缩略图生成异常 "+ image,e);
        }
       return false;
    }


    //原图,压缩后图片存储
    public static boolean imageCompressUseTinypng(File image, File compress, long spSize){
        try {
            if (!image.exists() || image.length() == 0) throw new FileNotFoundException(image.getCanonicalPath()) ;

            if (spSize < 512*102L) spSize = 512*1024L;

            if (spSize<image.length()){
                return false; //不需要压缩文件
            }
            boolean isSuccess = false;
            if ( image.length() > spSize ){
                isSuccess = FFMPEGTool.imageCompress_(image,compress);//使用ffmpeg
                Log4j.info("FFMPEG 压缩处理: "+ image);
            }
            if (!isSuccess){
                FileTool.copyFile(image,compress);// 复制图片到临时图片
                imageCompressUseTinypng(compress,spSize,0);// 对临时图片进行处理
                Log4j.info("tinypng 压缩处理: "+ image);
            }
            return compress.length() > 0;
        } catch (Exception e) {
            Log4j.error("图片压缩异常 "+ image,e);
        }
        return false;
    }


    private static File imageCompressUseTinypng(final File image, long spSize, int executeCount){
        try {
            if (tinypng_url == null) return image;

            HashMap<String, String> map = new HashMap<>();
            map.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36");
            map.put("origin", "https://tinypng.com");
            map.put("referer", "https://tinypng.com/");

            new HttpUtil.Request(tinypng_url)
                    .setType(HttpUtil.Request.POST)
                    .setBinaryStreamFile(image)
                    .setBinaryStreamUpload()
                    .setLocalCacheByteMax(1460)
                    .setParams(map)
                    .setReadTimeout(3 * 60 * 1000)
                    .setCallback(new HttpUtil.CallbackAbs() {
                        public void onResult(HttpUtil.Response response) {
                            if (image.exists() && !image.delete()) return;

                            String url = response.getConnection().getHeaderField("location");
                            new HttpUtil.Request(url).setDownloadFileLoc(image).download().setLocalCacheByteMax(1024*1024).execute();
                        }
                    })
                    .execute();

        } catch (Exception ignored) { }

        File temp = image.getAbsoluteFile();
        if (executeCount<3 && temp.length() > spSize) {
            Log4j.info("tinypng 第"+executeCount+"次循环压缩: temp文件大小: "+ temp.length());
            return imageCompressUseTinypng(temp,spSize,++executeCount);
        }
        return temp;
    }

    public static Color parseToColor(final String c) {
        Color convertedColor = Color.BLACK;
        try {
            convertedColor = new Color(Integer.parseInt(c, 16));
        } catch(NumberFormatException ignored) {
        }
        return convertedColor;
    }

    //判断文件是否为图片
    public static void getImageType(String filename) throws IOException {
        File file = new File(filename);
        ImageInputStream image = ImageIO.createImageInputStream(new FileInputStream(file));
        Iterator<ImageReader> readers = ImageIO.getImageReaders(image);
        String formatName = readers.next().getFormatName();
        System.out.println(formatName);
    }


    // 获取图片格式
    private static String getFormatName(File file) {
        String type = "NODE" ;
        try ( ImageInputStream iis = ImageIO.createImageInputStream(new FileInputStream(file))){
            Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
            if (iter.hasNext()) {
                type = iter.next().getFormatName();
            }

        } catch (Exception ignored) { }
        return type.toLowerCase();
    }

    // 判断是否图片后缀
    public  static boolean isImageType(File file){
        String type = getFormatName(file);
        return type.equalsIgnoreCase("jpeg") || type.equalsIgnoreCase("jpeg") || type.equalsIgnoreCase("png");
    }

    //获取图片大小
    public static int[] getImageSize(File image){
        try {
            // 1、源图片
            BufferedImage srcImg = ImageIO.read(image);
            return new int[]{srcImg.getWidth(), srcImg.getHeight()};
        } catch (Exception e) {
            Log4j.error("获取图片大小异常 "+ image,e);
        }
        return new int[]{-1,-1};
    }

    public enum LogoPlace{
        //居中 左上 左下 右上 右下
        CENTER(0),LEFT_TOP(1),LEFT_BOTTOM(2),RIGHT_TOP(3),RIGHT_BOTTOM(4),;
        private int value;
        LogoPlace(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }


    private static float[] getPositionByHeight(int sw,int sh,int lw,int lh,int posType){
        //判断大小
        if (sw < lw || sh < lh) return new float[]{0,0};
        //默认居中
        float x = ( sw - lw ) / 2.0f ;
        float y =(sh - lh) / 2.0f;

        if (posType == LogoPlace.LEFT_TOP.getValue()){ //左上
            x = 0;
            y = 0;
        }else if (posType == LogoPlace.LEFT_BOTTOM.getValue()){ // 左下
            x = 0;
            y = sh - lh;
        }else if (posType == LogoPlace.RIGHT_TOP.getValue()){ //右上
            x = sw - lw;
            y = 0;
        }else if (posType == LogoPlace.RIGHT_BOTTOM.getValue()){ //右下
            x = sw - lw;
            y = sh - lh;
        }
        return new float[]{x,y};
    }


    /**
     * 获取远程网络图片信息
     * @param imageURL
     * @return
     */
    public static BufferedImage getRemoteBufferedImage(String imageURL) {
        InputStream is = null;
        BufferedImage bufferedImage = null;
        try {
            URL url = new URL(imageURL);
            is = url.openStream();
            bufferedImage = ImageIO.read(is);
        } catch (IOException e) {
            Log4j.error("获取远端图片异常 "+ imageURL,e);
        } finally {
            try { if(is!=null) is.close(); } catch (IOException e) { Log4j.error("文件服务错误",e); }
        }
        return bufferedImage;
    }

    //添加文字水印
    public static boolean markImageByText(File image, File dest, String logoText,  int degree, Color color, float alpha, int place) {

        try {
            if (logoText == null || logoText.length() == 0) return false;

            // 源图片
            BufferedImage srcImg = ImageIO.read(image);
            int width = srcImg.getWidth();
            int height = srcImg.getHeight();

//            System.out.println(" src img : width= "+width+" , height = "+ height +" ,  type : "+  srcImg.getType() );
//            BufferedImage buffImg = new BufferedImage(srcImg.getWidth(),srcImg.getHeight(), TYPE_INT_RGB);

            // 字体信息
            int size = Math.max(width,height) /30; //字体大小
            Font font;
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("simhei.ttf");
            if (in != null){
                font =  Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(Font.ITALIC, size);//默认的字体类型
            }else{
                font = new Font("default",Font.ITALIC, size);
            }

            // 获取水印文字信息
//            FontMetrics fm = sun.font.FontDesignMetrics.getMetrics(font);
//            int w = fm.stringWidth(logoText); //字体-宽
//            int h = fm.getHeight();//字体-高

            Rectangle rec = font.getStringBounds(logoText,
                    new FontRenderContext(new AffineTransform(), true, true)).getBounds();
            int w = rec.width; //字体-宽
            int h = rec.height;//字体-高

            float[] posArr = getPositionByHeight(width,height,h,w,place);
            float x = posArr[0];
            float y = posArr[1] + size;
//            System.out.println("font : w = "+w+" , h = "+h+" x = "+ x +" y "+ y);

            // 画笔对象
            Graphics2D g =   srcImg.createGraphics();

            // 绘制原始图片
//            g.drawImage(srcImg, 0, 0, srcImg.getWidth(), srcImg.getHeight(),null);

            // 设置对线段的锯齿状边缘处理
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            // 设置水印文字颜色
            g.setColor(color);
            // 设置水印文字透明度
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
            // 设置水印文字Font
            g.setFont(font);
            // 设置内容,坐标位置(x,y)
            g.drawString(logoText,  x , y);
            // 设置水印旋转
            if (degree> 0) g.rotate(Math.toRadians(degree),  x,y);
            // 释放资源
            g.dispose();

            // 生成图片
            if (!dest.exists()) dest.createNewFile();
           return ImageIO.write(srcImg, fileSuffix(image), dest);

        } catch (Exception e) {
            Log4j.error("处理水印文字异常 "+ image,e);
        }
        return false;
    }


    //添加图片水印
    public static boolean markImageByIcon(File image, File dest, String iconUrl, float alpha,int degree,int place) {

        try{
            // 获取icon图片
            if (iconUrl == null || iconUrl.length() == 0) return false;

            Image icon;
            if (iconUrl.startsWith("http")||iconUrl.startsWith("https")){
                //远程获取
                icon = getRemoteBufferedImage(iconUrl);
            }else{
                //本地获取
                icon = new ImageIcon(iconUrl).getImage();
            }

            if (icon == null) return false;

            int iw = icon.getWidth(null);
            int ih = icon.getHeight(null);

            //根据原图创建画板
            BufferedImage srcImg = ImageIO.read(image);
            int width = srcImg.getWidth();
            int height = srcImg.getHeight();
//            BufferedImage buffImg = new BufferedImage(width, height, TYPE_INT_RGB);

            //画笔对象
            Graphics2D g = srcImg.createGraphics();

            // 绘制原始图
//            g.drawImage(srcImg.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);

            // 设置对线段的锯齿状边缘处理
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            //设置透明度
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
            //设置位置
            float[] posArr = getPositionByHeight(width,height,iw,ih,place);
            int x = (int) posArr[0];
            int y = (int) posArr[1];
            //水印旋转角度
            g.rotate(Math.toRadians(degree), x, y);
            //水印的位置
            g.drawImage(icon, x, y, null);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
            g.dispose();

            // 生成图片
            if (!dest.exists()) dest.createNewFile();
            return ImageIO.write(srcImg, fileSuffix(image), dest);
        }catch (Exception e){
            Log4j.error("处理水印图片异常 "+ image,e);
        }
        return false;
    }

    //处理图片大小-背景留白
    public static boolean imageMaxSizeHandle(int width , int height,File imageSrc, File imgDest){
        try {
            BufferedImage image = ImageIO.read(imageSrc);
            if (image.getWidth() != width || height != image.getHeight()){
                BufferedImage bgImage = new BufferedImage(width, height, TYPE_INT_RGB);
                Graphics2D g = bgImage.createGraphics();
                g.setColor(Color.white);
                g.fillRect(0, 0, width, height);
                int x = ( bgImage.getWidth() - image.getWidth() ) / 2;
                int y = ( bgImage.getHeight() - image.getHeight() ) / 2;
                g.drawImage(image, x, y, image.getWidth(), image.getHeight(), null);
                g.dispose();

                if (!imgDest.exists()) imgDest.createNewFile();
                return ImageIO.write(bgImage, fileSuffix(imageSrc), imgDest);
            }
        } catch (Exception e) {
            Log4j.error("处理背景留白异常 "+ imageSrc,e);
        }
        return false;
    }



    public static void main(String[] args) {
        try {
            /*
            markImageByText("onekdrug",
                    new File("C:\\Users\\Administrator\\Pictures\\2.jpg"),
                    0,
                    new Color(250, 1, 255),
                    0.2f,
                    LogoPlace.RIGHT_BOTTOM.value);
                    */
//            System.out.println(getFormatName(new File("C:\\Users\\Administrator\\Pictures\\3.jpg")));
//
//            imageCompress_scale_min(new File("C:\\Users\\Administrator\\Pictures\\3.jpg"),
//                    new File("C:\\Users\\Administrator\\Pictures\\3-min.jpg"));

            File f1 = new File("C:\\Users\\Administrator\\Desktop\\1.jpg");
            File f2 = new File("C:\\Users\\Administrator\\Desktop\\2.jpg");
            FileUtils.copyFile(f1,f2);
            System.out.println("开始压缩, 大小: "+ f2.length());
            imageCompressUseTinypng(f2, 46334, 0);
            System.out.println("完成压缩, 大小: "+ f2.length());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
