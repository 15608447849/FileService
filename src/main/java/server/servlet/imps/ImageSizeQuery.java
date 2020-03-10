package server.servlet.imps;

import bottle.threadpool.IOThreadPool;
import bottle.util.Log4j;
import server.prop.WebProperties;
import server.servlet.beans.operation.FFMPRG_CMD;
import server.servlet.beans.operation.FileErgodicOperation;
import server.servlet.beans.result.Result;
import server.servlet.iface.Mservlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static server.servlet.beans.operation.OperationUtils.*;
import static server.servlet.beans.result.Result.RESULT_CODE.EXCEPTION;
import static server.servlet.beans.result.Result.RESULT_CODE.SUCCESS;

/**
 * @Author: leeping
 * @Date: 2019/7/29 10:58
 */
public class ImageSizeQuery extends Mservlet{

    private static boolean isisCompressIng = false;

    private static IOThreadPool pool = new IOThreadPool();

    private static class ImageSize{
        String url;
        int width;
        int height;

        public ImageSize(String url, int width, int height) {
            this.url = url;
            this.width = width;
            this.height = height;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
        pool.post(()->{
            orgFileToFile();
        });
        writeJson(resp,"OK");
    }

    private void orgFileToFile() {

        new FileErgodicOperation(WebProperties.rootPath,
                true)
                .setCallback(file -> {
                    String _suffix = file.getName().substring(file.getName().lastIndexOf(".")+1);
                    if (_suffix.equals("jpg") && file.getName().contains("-org")){
                        Log4j.info("图片还原压缩处理: "+ file);
                        File compress = new File(file.getAbsolutePath().replace("-org",""));
                        FFMPRG_CMD.imageCompress_(file,compress);
                    }
                    return true;
                }).start();


    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
        Result result = new Result();
        int[] maxImageLimit = new int[] {0,0};
        String maxSize = req.getHeader("image-size-limit"); //图片大小
        boolean isCompress = req.getHeader("image-compress") != null ;
        long compressLimit = 0L;
        try{
            compressLimit = Long.parseLong(req.getHeader("image-compress"));
        }catch (Exception ignored){

        }
        final long _compressLimit = compressLimit;
        boolean minScaleExist = req.getHeader("image-min-exist") != null; //是否存在最小比例图
        if (maxSize!=null){
            try {
                String[] maxSizeArr = maxSize.split("x");
                int maxWidth = Integer.parseInt(maxSizeArr[0]);
                int maxHeight = Integer.parseInt(maxSizeArr[1]);
                maxImageLimit[0] = maxWidth;
                maxImageLimit[1] = maxHeight;

            } catch (Exception ignored) { }
        }
        String suffix = req.getHeader("image-spec-suffix-limit"); //图片后缀
        List<ImageSize> list = new ArrayList<>();
        List<File> imageList = new ArrayList<>();
        try {
            long time = System.currentTimeMillis();
            Log4j.info("查询图片大小,开始遍历文件");
            //遍历查询所有文件
            new FileErgodicOperation(WebProperties.rootPath,true)
                    .setCallback(file -> {
//                        Log4j.info("文件: "+ file);
                        if (suffix!=null){
                            String _suffix = file.getName().substring(file.getName().lastIndexOf(".")+1);
                            if (!(_suffix.equals(suffix))){
                                return true;
                            }
                        }
                        if (isImage(file)){
                            int[] sizeArr = getImageSize(file);
                            if (sizeArr[0] != maxImageLimit[0] || sizeArr[1] != maxImageLimit[1]){
                                list.add(new ImageSize(
                                        file.getPath().replace("\\","/")
                                        .replace(WebProperties.rootPath, WebProperties.domain),
                                        sizeArr[0], sizeArr[1]));
                            }
                            if (isCompress){
                                if (_compressLimit >0 ){
                                    if (file.length() > _compressLimit){
                                        imageList.add(file);
                                    }
                                }else{
                                    imageList.add(file);
                                }

                            }
                            if (minScaleExist){
                                String fileName = file.getName();
                                boolean flag = fileName.contains("-org") || fileName.contains("-min") || fileName.contains("-200x200") || fileName.contains("-400x400") || fileName.contains("-600x600");
                                if (!flag){
                                    File min = new File(filePathAndStrToSuffix(file.getAbsolutePath(),"-min"));
                                    if (!min.exists()) {
                                        imageCompress_scale_min(file,min);
                                    }
                                }

                            }
                        }
                        return true;
                    }).start();
            Log4j.info("查询图片大小,遍历文件结束,耗时: "+ (System.currentTimeMillis() - time));
            result.data = list;
            result.value(SUCCESS);
            pool.post(() -> {
                if (isisCompressIng) return;
                isisCompressIng = true;
                for (File image : imageList){
                    imageCompress(image,new File(filePathAndStrToSuffix(image.getAbsolutePath(),"-org")),_compressLimit);
                }
                isisCompressIng = false;
            });
        } catch (Exception e) {
            e.printStackTrace();
            result.data = new ArrayList<>();
            result.value(EXCEPTION,e.toString());
        }
        writeJson(resp,result);
    }
}
