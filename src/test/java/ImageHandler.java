import bottle.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import static server.servlet.beans.operation.OperationUtils.*;

/**
 * @Author: leeping
 * @Date: 2019/8/1 11:30
 */
public class ImageHandler {




    public static void main(String[] args) throws IOException {

        String dic = "C:\\Users\\user\\Desktop\\card\\tt";

        File file = new File(dic,"10.jpg");
        System.out.println(filePathAndStrToSuffix(file.getAbsolutePath(),"-min"));
//        File min = new File(file.getAbsolutePath()+"-min");
//        min.createNewFile();
//        imageCompress_scale_min(file,min);


//        String destImageRelPath = it.relativePath.replace(
//                it.currentFileName,
//                it.currentFileName.replace(it.suffix,"")+"-"+sizeStr+it.suffix) ;


//        System.out.println("原图图片大小: "+ fileSizeFormat(file.length()));
        //第一次裁剪
//        File file_rsize = new File(dic,"src_rsize.png");
//        imageResizeByGoogle(file,file_rsize,200,200);
//        System.out.println("未压缩,裁剪至  200 x 200 , 大小  : "+ fileSizeFormat(file_rsize.getAbsoluteFile().length()));

//        File src_compress_1 = new File(dic,"src_compress_1.png");
//        if (src_compress_1.exists()) src_compress_1.delete();
//        FileUtils.copyFile(file,src_compress_1);
//        src_compress_1 = imageCompress_scale_min(src_compress_1.getAbsoluteFile());
//        File file_compress2 = new File(dic,"src_compress_2.png");
//        FileUtils.copyFile(file,file_compress2);
//        file_compress2 = imageCompress(file_compress2.getAbsoluteFile(),1024*1024*5);
//        file_compress2 = imageCompress(file_compress2.getAbsoluteFile());
//        File file_rsize2 = new File(dic,"src_rsize2.png");
//        imageResizeByGoogle(file_compress2,file_rsize2,200,200);
//        System.out.println("压缩后,裁剪至  200 x 200 , 大小  : "+ fileSizeFormat(file_rsize.getAbsoluteFile().length()));
    }
}
