//import com.sun.image.codec.jpeg.JPEGCodec;
//import com.sun.image.codec.jpeg.JPEGImageEncoder;
import net.coobird.thumbnailator.Thumbnails;



import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.Iterator;



/**
 * @Author: leeping
 * @Date: 2019/4/1 11:33
 */
public class ImageDataUtils {
    //判断文件是否为图片
    public static void getImageType(String filename) throws IOException {
        File file = new File(filename);
        ImageInputStream image = ImageIO.createImageInputStream(new FileInputStream(file));
        Iterator<ImageReader> readers = ImageIO.getImageReaders(image);
        String formatName = readers.next().getFormatName();
        System.out.println(formatName);
    }


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
//            JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
//            encoder.encode(image); // JPEG编码
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
            FontMetrics fm = null;
//            FontMetrics fm = sun.font.FontDesignMetrics.getMetrics(font);
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
            e.printStackTrace();
            return null;
        } finally {
            try { if(is!=null) is.close(); } catch (IOException ignored) { }
        }
        return bufferedImage;
    }

    // 图片添加图片水印
    public static String markImgMark(String watermarkUrl, String source, String output) throws IOException {
        String result = "添加图片水印出错";
        File file = new File(source);
        Image img = ImageIO.read(file);
        int width = img.getWidth(null);
        int height = img.getHeight(null);
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
        ImageIcon imgIcon = new ImageIcon(watermarkUrl);
        Image con = imgIcon.getImage();

//        String imageURL = "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1558690099853&di=ae99776f06d83feabc1b11b24694cbb1&imgtype=0&src=http%3A%2F%2Fhbimg.b0.upaiyun.com%2Fec8e7c6b8de280f10459901a43fd21917b1b784a3a74-6NUzGH_fw658";
//        con = getRemoteBufferedImage(imageURL);

        float clarity = 0.8f;//透明度
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, clarity));
//        g.drawImage(con, 10, 10, null);//水印的位置
        System.out.println(con.getWidth(null) +" - "+ con.getHeight(null));
        int w = (width - con.getWidth(null))/2;
        int h = (height - con.getHeight(null)) /2;
        System.out.println(w + " "+ h);
        g.drawImage(con, w, h, null);//水印的位置
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        g.dispose();
        File sf = new File(output);
        ImageIO.write(bi, "jpg", sf); // 保存图片
        System.out.println("添加图片水印成功");
        return result;
    }

    private static Color parseToColor(final String c) {
        Color convertedColor = Color.BLACK;
        try {
            convertedColor = new Color(Integer.parseInt(c, 16));
        } catch(NumberFormatException ignored) {
        }
        return convertedColor;
    }

    public static void overlapImage(String qrcodePath) {
        try {
            BufferedImage big = new BufferedImage(800, 800, BufferedImage.TYPE_INT_RGB);
            Graphics2D gd = big.createGraphics();
//            big = gd.getDeviceConfiguration().createCompatibleImage(800, 800, Transparency.OPAQUE);
//            gd.setBackground(Color.BLUE);
            gd.setColor(Color.white);
            gd.fillRect(0, 0, 800, 800);
//            gd.dispose();
            //BufferedImage big = ImageIO.read(new File(screenPath));
            //BufferedImage small = ImageIO.read(new File(qrcodePath));
            BufferedImage small = ImageIO.read(new File("C:\\Users\\user\\Desktop\\11017020100202.jpg"));
//            Graphics2D g = big.createGraphics();
            int x = ( big.getWidth() - small.getWidth() ) / 2;
            int y = ( big.getHeight() - small.getHeight() ) / 2;
            gd.drawImage(small, x, y, small.getWidth(), small.getHeight(), null);
            gd.dispose();
            ImageIO.write(big, "jpg", new File(qrcodePath));
        } catch (Exception e) {
            e.printStackTrace();
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

//        markImageByText("www.onek11.com",new File("E:/迅雷下载/IMG_2501 - 副本.JPG"),0,new Color(0, 0, 255),0.2f,LogoPlace.RIGHT_BOTTOM);
//        markImageByText("欢迎使用一块医药采集平台",new File("E:/迅雷下载/IMG_2501 - 副本.JPG"),30,new Color(255 ,0 ,255),0.2f,LogoPlace.CENTER);
//        markImageByText("www.onek11.com",new File("E:/迅雷下载/IMG_2501 - 副本.JPG"),0,new Color(0, 0, 255),0.2f,LogoPlace.LEFT_TOP);

//
//        try {
//
//            String dirc =  "C:\\Users\\user\\Desktop\\GOODS\\";
////            System.out.println(markImgMark(dirc+"logo2.png",dirc+"1.jpg",dirc+"_1.jpg"));
//            System.out.println(markImgMark(dirc+"logo2.png",dirc+"1.jpg",dirc+"_1.jpg"));
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

//        try {
//            //得到当前的系统属性
//            System.getProperties().list(System.out);
//            String encoding = System.getProperty("file.encoding");
//            System.out.println("\n当前编码:" + encoding);
//        } catch (NumberFormatException e) {
//            e.printStackTrace();
//        }


//        overlapImage("C:\\Users\\user\\Desktop\\test.jpg");

        /*try {
            File file = new File("C:\\Users\\user\\Desktop\\","launch.jpg");
            File file2 = new File("C:\\Users\\user\\Desktop\\","launch2.png");
            Thumbnails.of(file)
                    .scale(1f)
                    .outputQuality(0.1f)
                    .toFile(file2);
            Thumbnails.of(file2)
                    .scale(1f)
                    .outputQuality(0.1f)
                    .toFile(file2);
            Thumbnails.of(file)
                    .scale(1f)
                    .outputQuality(0.1f)
                    .toFile(file2);
            Thumbnails.of(file)
                    .scale(1f)
                    .outputQuality(0.1f)
                    .toFile(file2);
        } catch (IOException e) {
            e.printStackTrace();
        }*/


        try {
            getImageType("C:\\Users\\Administrator\\Pictures\\2.jpg");
            markImageByText("onekdrug",new File("C:\\Users\\Administrator\\Pictures\\2.jpg"),0,new Color(0, 0, 255),0.2f,LogoPlace.RIGHT_BOTTOM);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }






}
