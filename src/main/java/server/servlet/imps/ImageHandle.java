package server.servlet.imps;



import server.servlet.beans.ImageOperation;
import server.servlet.beans.UploadFileItemResult;
import server.undertow.ServletAnnotation;

import javax.servlet.http.HttpServletRequest;

import java.util.List;


/**
 * @Author: leeping
 * @Date: 2019/4/1 13:48
 * 图片处理
 */
@ServletAnnotation(name = "文件上传",path = "/upload")
public class ImageHandle extends FileUpLoad{
    // 对应表单下标 tab1;tab2;tab3 ...
    // tailor-list =  width x height ; 1080x1920,5400x8630 ; 2540x1330,770x240 ; 44x44 ;
    @Override
    protected void subHook(HttpServletRequest req, List<UploadFileItemResult> resultList) {
        super.subHook(req, resultList);

        List<String> tailorList = filterData(req.getHeader("tailor-list")); //裁剪信息
        boolean isCompress = req.getHeader("image-compress") != null ;//图片压缩
        long compressSize = 0L; //图片压缩指定大小
        try{
            compressSize =  Integer.parseInt(req.getHeader("image-compress-size"));
        }catch (Exception ignored){ }

        String maxSize = req.getHeader("image-size-limit"); //图片最大
        boolean minScaleExist = req.getHeader("image-min-exist") != null; //是否存在最小比例图
        boolean isLogo = req.getHeader("image-logo") != null; //是否添加水印 默认不添加水印

        int[] maxImageLimit = new int[] {0,0};//图片最大限制
        if (maxSize!=null){
            try {
               String[] maxSizeArr = maxSize.split("x");
               int maxWidth = Integer.parseInt(maxSizeArr[0]);
               int maxHeight = Integer.parseInt(maxSizeArr[1]);
                maxImageLimit[0] = maxWidth;
                maxImageLimit[1] = maxHeight;
            } catch (Exception ignored) { }
        }

        for (int i = 0; i < resultList.size() ; i++){
            UploadFileItemResult it = resultList.get(i);
            if (!it.success)  continue;
            String tailorStr = null;
            //裁剪处理值
            if (tailorList != null && tailorList.size() > i) {
                tailorStr = tailorList.get(i);
            }
            ImageOperation.add(it.localAbsolutelyPath,maxImageLimit,isCompress,compressSize,isLogo,minScaleExist,tailorStr);
        }
    }

}
