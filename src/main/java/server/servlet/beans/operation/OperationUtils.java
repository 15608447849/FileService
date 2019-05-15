package server.servlet.beans.operation;

import bottle.util.StringUtils;

import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/4/1 14:54
 */
public class OperationUtils {



    public static <T> T getIndexValue(List<String> dataList, int index, T def){
        try {
            if (dataList!=null) {
                String val = null;

                if (dataList.size()>index) {
                    val = dataList.get(index);
                }
                if (!StringUtils.isEmpty(val)){
                    if (def instanceof String){
                        return (T) val;
                    }
                    Class cls = def.getClass();
                    Method method = cls.getMethod("parse"+cls.getSimpleName(), String.class);
                    return (T) method.invoke(null,val);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return flag;
    }*/
    /**
     * 图片 压缩/放大
     *
     * @param w int 新宽度
     * @param h int 新高度
     */
    public static boolean imageResizeByGoogle(File srcImage, File distImage, int w, int h) {
        boolean flag = false;
        try {
            Thumbnails.of(srcImage).size(w,h).keepAspectRatio(false).outputQuality(1.0f).toFile(distImage);
            flag = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return flag;
    }

    //判断文件是否为图片
    public static boolean isImage(File file){
        try
        {
            BufferedImage bufreader = ImageIO.read(file);
            int width = bufreader.getWidth();
            int height = bufreader.getHeight();
            if(!(width==0 || height==0)){
                return true;
            }
        }catch (Exception ignored) {
        }
        return false;
    }

    public enum LogoPlace{
        LEFT_TOP,RIGHT_BOTTOM,CENTER;
    }
    public static File markImageByText(String logoText, File image, int degree, Color color, float alpha) {
        return markImageByText(logoText,image,degree,color,alpha,LogoPlace.CENTER);
    }
    //添加水印
    public static File markImageByText(String logoText, File image, int degree, Color color, float alpha, LogoPlace place) {
        OutputStream os = null;
        try {
            if (logoText == null) return image;
            // 1、源图片
            java.awt.Image srcImg = ImageIO.read(image);
            BufferedImage buffImg = new BufferedImage(srcImg.getWidth(null),srcImg.getHeight(null), BufferedImage.TYPE_INT_RGB);
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("simhei.ttf");
            Font font = null;
            int size = Math.max(buffImg.getWidth(),buffImg.getHeight()) /15;
            if (in!=null){
                font =  Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(Font.ITALIC, size);//字体
            }else{
                font = new Font("default",Font.ITALIC, size);
            }
            FontMetrics fm = sun.font.FontDesignMetrics.getMetrics(font);
            int fontW = fm.stringWidth(logoText);
            int fontH = fm.getHeight();
            //默认居中
            float x = buffImg.getWidth()/2.0f - fontW/2.0f;
            float y = buffImg.getHeight()/2.0f - fontH/2.0f;

            if (place == LogoPlace.LEFT_TOP){
                x = 0;
                y = fontH;
            }else if (place ==LogoPlace.RIGHT_BOTTOM){
                x = buffImg.getWidth() - fontW;
                y = buffImg.getHeight() - fontH ;
            }
            // 2、得到画笔对象
            Graphics2D g = buffImg.createGraphics();
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
            ImageIO.write(buffImg, "PNG", os);
            return image.getAbsoluteFile();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != os) os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return image;
    }

}
