package server.servlet.imps;

import server.prop.WebProperties;
import server.servlet.beans.operation.OperationUtils;
import server.servlet.beans.result.UploadResult;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


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

        String rootPath = WebProperties.get().rootPath;

        for (int i = 0 ; i < tailorList.size() ;i++){
            try {
                UploadResult it = resultList.get(i);
                if (!it.success)  continue;
                File srcImage = new File(rootPath + it.relativePath);
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
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        //处理文件裁剪值


    }
}
