package server.servlet.imps;

import bottle.util.Log4j;
import bottle.util.StringUtils;
import server.prop.WebProperties;
import server.servlet.beans.operation.OperationUtils;
import server.servlet.beans.result.UploadResult;

import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import static server.servlet.beans.operation.OperationUtils.isImage;
import static server.servlet.beans.operation.OperationUtils.markImageByText;


/**
 * @Author: leeping
 * @Date: 2019/4/1 13:48
 */
public class ImageTailor extends FileUpLoad{
    // 对应表单下标 tab1;tab2;tab3 ...
    // tailor-list =  width x height ; 1080x1920,5400x8630 ; 2540x1330,770x240 ; 44x44 ;


    @Override
    protected void subHook(HttpServletRequest req, List<UploadResult> resultList) {
        super.subHook(req, resultList);
        List<String> tailorList = filterData(req.getHeader("tailor-list"));
        String logo = WebProperties.get().imageLogoText;
        try {
            String logoText = req.getHeader("image-logo");
            System.out.println(logoText);
            //添加水印
            if (!StringUtils.isEmpty(logoText)){
                logo = URLDecoder.decode(URLDecoder.decode(logoText,"UTF-8"),"UTF-8");
                Log4j.info("添加客户端自定义水印:" + logo);
            }
        } catch (UnsupportedEncodingException ignored) {
        }

        String rootPath = WebProperties.get().rootPath;

        for (int i = 0; i < resultList.size() ; i++){
            UploadResult it = resultList.get(i);
            if (!it.success)  continue;
            File file = new File(rootPath + it.relativePath);
            if (!isImage(file)) continue;
            //水印
            File srcImage = markImageByText(logo,file,30,new Color(0 ,0 ,0),0.2f);
            //裁剪处理
            if (tailorList==null || tailorList.size()<i) continue;
            try {
                String tailorStr =tailorList.get(i);
                String[] tailorArr = tailorStr.split(",");
                List<String> tailorFileList = new ArrayList<>();
                for (String sizeStr : tailorArr) {
                    String destImageRelPath = it.relativePath.replace(it.currentFileName,
                            it.currentFileName.replace(it.suffix,"")+"-"+sizeStr+it.suffix) ;
                    File destImage = new File(rootPath + destImageRelPath);
                    String[] sizeArr = sizeStr.split("x");
                    int w = Integer.parseInt(sizeArr[0]);
                    int h = Integer.parseInt(sizeArr[1]);
                    boolean flag = OperationUtils.imageResizeByGoogle(srcImage,destImage,w,h);
                    if (flag) tailorFileList.add(destImageRelPath);
                }
                if (tailorFileList.size() > 0) it.tailorPathList = tailorFileList;
            } catch (Exception ignored) { }
        }

    }
}
