package server.servlet.imps;


import bottle.util.GoogleGsonUtil;
import bottle.util.Log4j;
import server.undertow.ServletAnnotation;
import server.undertow.ServletResult;
import server.undertow.CustomServlet;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/**
 * @Author: leeping
 * @Date: 2019/8/8 11:10
 * 获取图片指定点的像素颜色
 */

@ServletAnnotation(name = "图片像素颜色",path = "/pixColor")
public class ImagePixColor extends CustomServlet {

    private static class Param{
        String image;
        int x;
        int y;
    }

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

    public static String Color2String(Color color) {
        String R = Integer.toHexString(color.getRed());
        R = R.length() < 2 ? ('0' + R) : R;
        String B = Integer.toHexString(color.getBlue());
        B = B.length() < 2 ? ('0' + B) : B;
        String G = Integer.toHexString(color.getGreen());
        G = G.length() < 2 ? ('0' + G) : G;
        return '#' + R + B + G;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-type", "text/html;charset=UTF-8");
        ServletResult result = new ServletResult();
        try {
            String json = req.getHeader("image-pix-color");
            if (json != null){
                Param param = GoogleGsonUtil.jsonToJavaBean(json,Param.class);
                if (param!=null){
                    BufferedImage image = ImageIO.read(new URL(param.image));
                    int [] rgb = getRGB(image,param.x,param.y);
                    result.setData(rgb);
                }
            }
        } catch (IOException e) {
            Log4j.error("文件服务错误",e);
        }
        writeJson(resp,result);
    }


}
