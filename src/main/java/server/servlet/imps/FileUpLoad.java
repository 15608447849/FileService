package server.servlet.imps;

import bottle.util.Log4j;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import server.servlet.beans.result.Result;
import server.servlet.beans.result.UploadResult;
import server.prop.WebProperties;
import server.servlet.beans.operation.FileUploadOperation;
import server.servlet.iface.Mservlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static server.servlet.beans.result.Result.RESULT_CODE.*;

/**
 * Created by lzp on 2017/5/13.
 * 文件上传接收
 */
public class FileUpLoad extends Mservlet {

    //内存缓冲区
    private final int CACHE_VAL = 4096;

    //设置单个文件的最大上传值
    private final long SINGE_FILE_MAX_SIZE =  1024 * 1024 * 1024 * 10L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req,resp);

        List<UploadResult> resultList = null;

        Result result = new Result();

        //指定对应下标的文件的保存路径
        ArrayList<String> pathList = checkDirPathByList(filterData(req.getHeader("specify-path")));

        //指定对应下标的文件的保存文件名
        ArrayList<String> fileNameList = filterData( req.getHeader("specify-filename"));

        //指定对应下标的文件的是否保存成md5文件名
        ArrayList<String> fileSaveMD5 = filterData(req.getHeader("save-md5"));

        try {

            if (!ServletFileUpload.isMultipartContent(req)){
                throw new IllegalArgumentException("content-type is not 'multipart/form-data'");
            }

            DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();
            diskFileItemFactory.setRepository(new File(WebProperties.get().tempPath));
            // 设定上传文件的值，如果上传文件大于缓冲区值，就可能在repository所代表的文件夹中产生临时文件，否则直接在内存中进行处理
            diskFileItemFactory.setSizeThreshold(CACHE_VAL);

            // 创建一个ServletFileUpload对象
            ServletFileUpload uploader = new ServletFileUpload(diskFileItemFactory);
            uploader.setFileSizeMax(SINGE_FILE_MAX_SIZE);
            uploader.setHeaderEncoding("utf-8");


            List<FileItem> listItems = uploader.parseRequest(req);
            resultList = new FileUploadOperation(pathList,fileNameList,fileSaveMD5,listItems).execute();
            subHook(req, resultList); //钩子
        } catch (Exception e) {
            Log4j.error("文件上传错误",e.fillInStackTrace());
            result.value(EXCEPTION);
        }finally {
          //向客户端返回结果
            if (resultList!=null){
                result.value(SUCCESS);
                result.data = resultList;
            }
          writeJson(resp,result);
        }
    }
    protected void subHook(HttpServletRequest req, List<UploadResult> resultList){
        //子类实现
    }
}
