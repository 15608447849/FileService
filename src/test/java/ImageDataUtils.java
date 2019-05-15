import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;

import static net.coobird.thumbnailator.util.exif.Orientation.BOTTOM_LEFT;
import static net.coobird.thumbnailator.util.exif.Orientation.LEFT_TOP;

/**
 * @Author: leeping
 * @Date: 2019/4/1 11:33
 */
public class ImageDataUtils {
    //判断文件是否为图片
    private static boolean isImage(File file){
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
    //获取图片文件实际类型,若不是图片则返回null
    private static String getImageFileType(File f){
        if (isImage(f))
        {
            ImageInputStream iis = null;
            try
            {
                iis = ImageIO.createImageInputStream(f);
                Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
                if (iter.hasNext())
                {
                     return iter.next().getFormatName();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }finally {
                try {
                    if (iis!=null)  iis.close();
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }


    /**
     * 图片 压缩/放大
     *
     * @param w int 新宽度
     * @param h int 新高度
     */
    public static void resize(File srcImage,File distImage,int w, int h) {
       long stime = System.currentTimeMillis();
        try {
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            // SCALE_SMOOTH 的缩略算法 生成缩略图片的平滑度的 优先级比速度高 生成的图片质量比较好 但速度慢
//            BufferedImage image = new BufferedImage(w, h, BufferedImage.SCALE_SMOOTH);
            image.getGraphics().drawImage(ImageIO.read(srcImage), 0, 0, w, h, null); // 绘制缩小后的图

            FileOutputStream out = new FileOutputStream(distImage); // 输出到文件流
            // 可以正常实现bmp、png、gif转jpg
            JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
            encoder.encode(image); // JPEG编码
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Time: "+ (System.currentTimeMillis() - stime ));
    }


    public enum LogoPlace{
        LEFT_TOP,RIGHT_BOTTOM,CENTER;
    }
    /**
     * 给图片添加水印文字、可设置水印文字的旋转角度
     */
    public static void markImageByText(String logoText, File imgPath,int degree,Color color,float alpha,LogoPlace place) {
        OutputStream os = null;
        try {
            // 1、源图片
            java.awt.Image srcImg = ImageIO.read(imgPath);
            BufferedImage buffImg = new BufferedImage(srcImg.getWidth(null),srcImg.getHeight(null), BufferedImage.TYPE_INT_RGB);
//            float x = buffImg.getWidth()/2.0f * 0.02f + 12 ;

            Font font = new java.awt.Font("Default", Font.ITALIC, Math.max(buffImg.getWidth(),buffImg.getHeight()) /15);//字体
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
            os = new FileOutputStream(imgPath);
            ImageIO.write(buffImg, "PNG", os);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != os) os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
//        File file = new File("E:\\迅雷下载","1.jpg");
//
//        File file1 = new File("E:\\迅雷下载",file.getName()+".tump");
//        resize(file,file1,200,200);
//
//        String format = getImageFileType(file1);
//        System.out.println(format);

        markImageByText("www.onek11.com",new File("E:/迅雷下载/IMG_2501 - 副本.JPG"),0,new Color(0, 0, 255),0.2f,LogoPlace.RIGHT_BOTTOM);
        markImageByText("欢迎使用一块医药采集平台",new File("E:/迅雷下载/IMG_2501 - 副本.JPG"),30,new Color(255 ,0 ,255),0.2f,LogoPlace.CENTER);
        markImageByText("www.onek11.com",new File("E:/迅雷下载/IMG_2501 - 副本.JPG"),0,new Color(0, 0, 255),0.2f,LogoPlace.LEFT_TOP);
    }

}
