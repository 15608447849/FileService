package server.servlet.beans.operation;

import bottle.util.StringUtils;

import net.coobird.thumbnailator.Thumbnails;

import java.io.File;
import java.io.IOException;
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

}
