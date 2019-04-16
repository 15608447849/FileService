import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

    public static void main(String[] args) {
        File file = new File("E:\\迅雷下载","1.jpg");

        File file1 = new File("E:\\迅雷下载",file.getName()+".tump");
        resize(file,file1,200,200);

        String format = getImageFileType(file1);
        System.out.println(format);

    }

}
