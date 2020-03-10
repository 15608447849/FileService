import bottle.util.FileUtils;
import server.servlet.beans.operation.ImageOperation;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

/**
 * @Author: leeping
 * @Date: 2019/8/7 21:59
 */
public class test2 {
    /**
     * 取得图像上指定位置像素的 rgb 颜色分量。
     * @param image 源图像。
     * @param x 图像上指定像素位置的 x 坐标。
     * @param y 图像上指定像素位置的 y 坐标。
     * @return 返回包含 rgb 颜色分量值的数组。元素 index 由小到大分别对应 r，g，b。
     */
    public static int[] getRGB(BufferedImage image, int x, int y){
        int[] rgb = new int [3];
        int pixel = image.getRGB(x, y);
        rgb[0] = (pixel & 0xff0000) >> 16;
        rgb[1] = (pixel & 0xff00) >> 8;
        rgb[2] = (pixel & 0xff);
        return  rgb;
    }

    /**
     * 读取一张图片的RGB值
     *
     * @throws Exception
     */
    public static void getImagePixel(String image) throws Exception {
        int[] rgb = new int[3];
        File file = new File(image);
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        int width = bi.getWidth();
        int height = bi.getHeight();
        int minx = bi.getMinX();
        int miny = bi.getMinY();
        System.out.println("width=" + width + ",height=" + height + ".");
        System.out.println("minx=" + minx + ",miniy=" + miny + ".");
        for (int i = minx; i < width; i++) {
            for (int j = miny; j < height; j++) {
                int pixel = bi.getRGB(i, j); // 下面三行代码将一个数字转换为RGB数字
                rgb[0] = (pixel & 0xff0000) >> 16;
                rgb[1] = (pixel & 0xff00) >> 8;
                rgb[2] = (pixel & 0xff);
                System.out.println("i=" + i + ",j=" + j + ":(" + rgb[0] + ","
                        + rgb[1] + "," + rgb[2] + ")");
                Color color =  new Color(rgb[0],rgb[1],rgb[2] );
                System.out.println(Color2String(color));
            }
        }
    }

    public static String Color2String(Color color) {
        String R = Integer.toHexString(color.getRed());
        R = R.length() < 2 ? ('0' + R) : R;
        String B = Integer.toHexString(color.getBlue());
        B = B.length() < 2 ? ('0' + B) : B;
        String G = Integer.toHexString(color.getGreen());
        G = G.length() < 2 ? ('0' + G) : G;
        return '#' + R + B + G;
    }

    public static void main(String[] args) throws Exception {
//        BufferedImage image = ImageIO.read(new URL("https://www.ykdrugs.com:9999/DC843A5259846C618F65BAC0F3614F5C/image/ad_1.png"));
//        int [] rgb = getRGB(image,10,10);
//        Color color =  new Color(rgb[0],rgb[1],rgb[2] );
//        System.out.println(Color2String(color));


//        ImageOperation.add(new File("C:\\Users\\user\\Desktop\\assets\\pay\\consell-pic - 副本.png"),null,true,0,false,false,null);
        ImageOperation.add(new File("C:\\Users\\user\\Desktop\\assets\\pay\\呵呵呵 呵呵呵.png"),null,true,0,false,false,null);


    }
}
