package server.servlet.beans.operation;


import bottle.util.FileTool;
import bottle.util.HttpUtil;
import bottle.util.Log4j;
import bottle.util.StringUtil;
import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/4/1 14:54
 */
public class OperationUtils {

    public static String fileSuffix(File file){
        String fileName = file.getName();

        String suffix =  fileName.substring(fileName.lastIndexOf(".") + 1);

        return suffix;
    }

    public static String filePathAndStrToSuffix(String path,String var){
        int i = path.lastIndexOf(".");
        String startStr = path.substring(0,i);
        String endStr = path.substring(i);
        String str = startStr+var+endStr;
        return str;
    }


    public static String getIndexValue(List<String> dataList, int index, String def){
        try {
            if (dataList!=null) {
                String val = null;

                if (dataList.size()>index) {
                    val = dataList.get(index);
                }

                return val;
            }
        } catch (Exception e) {
            Log4j.error("文件服务错误",e);
        }
        return def;
    }

    /**
     * 图片 压缩/放大
     *
     * @param w int 新宽度
     * @param h int 新高度
     */
   /* public static boolean imageResizeByJdk(File srcImage, File distImage, int w, int h) {
        boolean flag = false;
        try {
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            // SCALE_SMOOTH 的缩略算法 生成缩略图片的平滑度的 优先级比速度高 生成的图片质量比较好 但速度慢
//            BufferedImage image = new BufferedImage(w, h, BufferedImage.SCALE_SMOOTH);
            image.getGraphics().drawImage(ImageIO.read(srcImage), 0, 0, w, h, null); // 绘制缩小后的图
            FileOutputStream out = new FileOutputStream(distImage); // 输出到文件流
            // 可以正常实现bmp、png、gif转jpg
            JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
            JPEGEncodeParam jep = JPEGCodec.getDefaultJPEGEncodeParam(image);
            jep.setQuality(1.0f, true);
            encoder.encode(image,jep); // JPEG编码
            out.close();
            flag = true;
        } catch (IOException e) {
            Log4j.error("文件服务错误",e);
        }
        return flag;
    }*/

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
            Thumbnails.of(image).size(w,h)
                    .keepAspectRatio(false)
                    .outputQuality(1.0f)
                    .toOutputStream(fos);
            return true;
        }catch (IOException e){
            Log4j.error("文件服务错误",e);
        }
        return false;
    }

    public static boolean imageCompress_scale_min(File image,File dist){
        try(FileOutputStream fos = new FileOutputStream(dist)){
            Thumbnails.of(image)
                    .scale(0.1)
                    .outputQuality(0.1f)
                    .toOutputStream(fos);
            return true;
        } catch (IOException e) {
            Log4j.error("文件服务错误",e);
        }
        return false;
    }


    //原图,压缩后图片存储
    public static boolean imageCompress(File image,File compress,long spSize){
        try {
            if (!image.exists() || image.length() == 0) throw new FileNotFoundException(image.getCanonicalPath()) ;

            if (!isImage(image)) throw  new IllegalArgumentException(image+" 不是标准图片类型") ;

            if (spSize < 512*102L) spSize = 512*1024L;

            if (spSize<image.length()){
                return false; //不需要压缩文件
            }

            boolean isSuccess = false;
//            Log4j.info(image + " 压缩前大小: "+ fileSizeFormat(image.length()));
            if ( image.length() > spSize ){
//                Log4j.info("本地压缩: " + image);
                isSuccess = FFMPRG_CMD.imageCompress_(image,compress);//使用ffmpeg
            }
            if (!isSuccess){
//                Log4j.info("网络压缩: " + image);
                FileTool.copyFile(image,compress);// 复制图片到临时图片
                imageCompress(compress,spSize,0);// 对临时图片进行处理
            }
//            Log4j.info(compress + " 压缩后大小: "+ fileSizeFormat(compress.length()) );
            return compress.length() > 0;
        } catch (Exception e) {
            Log4j.error("文件服务错误",e);
        }
        return false;
    }

    public static void main(String[] args) {
        File file = new File("C:\\Users\\user\\Desktop\\1.jpg");
        long srcLen = file.length();

        File f = new File("C:\\Users\\user\\Desktop\\11.jpg");
        FFMPRG_CMD.imageCompress_(file,f);//使用ffmpeg

//       File f = imageCompress(file,1024*1024); //使用
        long  distLen = f.length();
       System.out.println("减小大小: " + (srcLen - distLen));
    }

    private static File imageCompress(final File image,long spSize,int executeCount){
        try {

            HashMap<String, String> map = new HashMap<>();
            map.put("user-agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.142 Safari/537.36");

            final HttpUtil.CallbackAbs callback = new HttpUtil.CallbackAbs() {
                public void onResult(HttpUtil.Response response) {
                    if (image.exists() ) {
                        if (image.delete()){
//                                     Log4j.info("图片压缩.删除文件 = "+ image);
                        }
                    }
                    String url = response.getConnection().getHeaderField("location");
                    new HttpUtil.Request(url).setDownloadFileLoc(image).download().setLocalCacheByteMax(1024*1024).execute();
                }
            };

             new HttpUtil.Request("https://tinypng.com/web/shrink")
                     .setType(HttpUtil.Request.POST)
                     .setBinaryStreamFile(image)
                     .setBinaryStreamUpload()
                     .setLocalCacheByteMax(1460)
                     .setParams(map)
                     .setReadTimeout(3 * 60 * 1000)
                     .setCallback(callback)
                     .execute();

        } catch (Exception ignored) { }

        File temp = image.getAbsoluteFile();
        if (executeCount<10 && temp.length() > spSize) {
            return imageCompress(temp,spSize,++executeCount);
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



    private static String getFormatName(Object o) {
        String type = "NODE" ;
        try ( ImageInputStream iis = ImageIO.createImageInputStream(o);){
            Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
            if (!iter.hasNext()) {
                return null;
            }
            type = iter.next().getFormatName();
        } catch (IOException e) {
            //
        }
        return type.toLowerCase();
    }

    public  static boolean isImageSuffix(File file){
        return file.getName().endsWith("jpg") || file.getName().endsWith("png") || file.getName().endsWith("jpeg");
    }

    //判断文件是否为图片
    public static boolean isImage(File file){
        String type = getFormatName(file);
        if (type == null) return false;
        return type.equals("jpeg") || type.equals("png");
    }


    //获取图片大小
    public static int[] getImageSize(File image){
        try {
            // 1、源图片
            BufferedImage srcImg = ImageIO.read(image);
            return new int[]{srcImg.getWidth(), srcImg.getHeight()};
        } catch (IOException e) {
            Log4j.error("文件服务错误",e);
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
    };


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

    //添加文字水印
    @SuppressWarnings("unchecked")
    public static File markImageByText(String logoText, File image, int degree, Color color, float alpha, int place) {
        OutputStream os = null;
        try {
            if (logoText == null || logoText.length() == 0) return image;
            // 1、源图片
            java.awt.Image srcImg = ImageIO.read(image);
            BufferedImage buffImg = new BufferedImage(srcImg.getWidth(null),srcImg.getHeight(null), BufferedImage.TYPE_INT_RGB);
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("simhei.ttf");
            Font font;
            int size = Math.max(buffImg.getWidth(),buffImg.getHeight()) /15; //字体大小
            if (in!=null){
                font =  Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(Font.ITALIC, size);//默认的字体类型
            }else{
                font = new Font("default",Font.ITALIC, size);
            }
            FontMetrics fm = sun.font.FontDesignMetrics.getMetrics(font);
            int fontW = fm.stringWidth(logoText); //字体-宽
            int fontH = fm.getHeight();//字体-高
            float[] posArr = getPositionByHeight(buffImg.getWidth(),buffImg.getHeight(),fontW,fontH,place);
            float x = posArr[0];
            float y = posArr[1] + size;
            // 2、得到画笔对象
            Graphics2D g = buffImg.createGraphics();

            if (fileSuffix(image).equals("png")){
                buffImg = g.getDeviceConfiguration().createCompatibleImage(srcImg.getWidth(null),srcImg.getHeight(null), Transparency.TRANSLUCENT);
                g.dispose();
                g = buffImg.createGraphics();
            }

            // 3、设置对线段的锯齿状边缘处理
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(srcImg.getScaledInstance(srcImg.getWidth(null), srcImg.getHeight(null), java.awt.Image.SCALE_SMOOTH), 0, 0, null);
            // 4、设置水印旋转
            if (degree> 0) g.rotate(Math.toRadians(degree),  x,y);
            // 5、设置水印文字颜色
            g.setColor(color);
            // 6、设置水印文字Font
            g.setFont(font);
            // 7、设置水印文字透明度
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
            // 8、第一参数->设置的内容，后面两个参数->文字在图片上的坐标位置(x,y)
            g.drawString(logoText,  x , y);
            // 9、释放资源
            g.dispose();
            // 10、生成图片
            os = new FileOutputStream(image);
            ImageIO.write(buffImg, "jpeg", os);
            return image.getAbsoluteFile();
        } catch (Exception e) {
            Log4j.error("文件服务错误",e);
        } finally {
            try {
                if (null != os) os.close();
            } catch (Exception e) {
                Log4j.error("文件服务错误",e);
            }
        }
        return image;
    }


    /**
     * 获取远程网络图片信息
     * @param imageURL
     * @return
     */
    public static BufferedImage getRemoteBufferedImage(String imageURL) {
        URL url = null;
        InputStream is = null;
        BufferedImage bufferedImage = null;
        try {
            url = new URL(imageURL);
            is = url.openStream();
            bufferedImage = ImageIO.read(is);
        } catch (IOException e) {
            Log4j.error("文件服务错误",e);
        } finally {
            try { if(is!=null) is.close(); } catch (IOException e) { Log4j.error("文件服务错误",e); }
        }
        return bufferedImage;
    }


    //添加图片水印
    public static File markImageByIcon(String iconUrl, File image,float alpha,int degree,int place) {
        OutputStream os = null;
        try{
            // 获取icon图片
            if (iconUrl == null || iconUrl.length() == 0) return image;
            Image icon;
            if (iconUrl.startsWith("http")||iconUrl.startsWith("https")){
                //远程获取
                icon = getRemoteBufferedImage(iconUrl);
            }else{
                //本地获取
                icon = new ImageIcon(iconUrl).getImage();
            }
            if (icon == null) return image;
            int iw = icon.getWidth(null);
            int ih = icon.getHeight(null);
            //根据原图创建画板
            Image srcImg = ImageIO.read(image);
            int width = srcImg.getWidth(null);
            int height = srcImg.getHeight(null);
            BufferedImage buffImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            //画笔对象
            Graphics2D g = buffImg.createGraphics();
            if (fileSuffix(image).equals("png")){
                buffImg = g.getDeviceConfiguration().createCompatibleImage(width,height, Transparency.TRANSLUCENT);
                g.dispose();
                g = buffImg.createGraphics();
            }
            // 设置对线段的锯齿状边缘处理
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(srcImg.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);

            //设置透明度
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
            //设置位置
            float[] posArr = getPositionByHeight(width,height,iw,ih,place);
            int x = (int) posArr[0];
            int y = (int) posArr[1];
            g.rotate(Math.toRadians(degree), x, y);//水印旋转角度
            g.drawImage(icon, x, y, null);//水印的位置
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
            g.dispose();
            os = new FileOutputStream(image);
            // 生成图片
            ImageIO.write(buffImg, fileSuffix(image), os);
            return image.getAbsoluteFile();
        }catch (Exception e){
            Log4j.error("文件服务错误",e);
        }finally {
            try {
                if (null != os) os.close();
            } catch (Exception e) {
                Log4j.error("文件服务错误",e);
            }
        }
        return image;
    }

    //处理图片大小-背景留白
    public static File imageMaxSizeHandle(int[] maxSizeArr,File imageSrc){
        try {
            int width = maxSizeArr[0];
            int height = maxSizeArr[1];

            BufferedImage image = ImageIO.read(imageSrc);
            if (image.getWidth() != width || height != image.getHeight()){
                BufferedImage bgImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = bgImage.createGraphics();
                g.setColor(Color.white);
                g.fillRect(0, 0, width, height);
                int x = ( bgImage.getWidth() - image.getWidth() ) / 2;
                int y = ( bgImage.getHeight() - image.getHeight() ) / 2;
                g.drawImage(image, x, y, image.getWidth(), image.getHeight(), null);
                g.dispose();
                ImageIO.write(bgImage, fileSuffix(imageSrc), imageSrc);
                return imageSrc.getAbsoluteFile();
            }
        } catch (Exception e) {
            Log4j.error("文件服务错误",e);
        }
        return imageSrc;
    }



}
