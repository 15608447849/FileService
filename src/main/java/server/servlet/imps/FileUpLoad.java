package server.servlet.imps;

import bottle.util.Log4j;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import server.sqlites.tables.SQLiteFileTable;
import server.sqlites.tables.SQLiteListTable;
import server.undertow.ServletAnnotation;
import server.undertow.ServletResult;
import server.servlet.beans.UploadFileItemResult;
import server.servlet.beans.FileUploadOperation;
import server.undertow.CustomServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static server.undertow.ServletResult.RESULT_CODE.*;

/**
 * Created by lzp on 2017/5/13.
 * 文件上传接收
 */

public class FileUpLoad extends CustomServlet {

    //内存缓冲区
    private static int MEMORY_CACHE_BYTE_MAX = 1024;

    //设置单个文件的最大上传值
    private static long SINGE_FILE_MAX_SIZE =  1024 * 1024 * 1024 * 5L;

    private static File TEMPORARY_FOLDER = new File("./temporaryFolder");

    private static final DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();

    public static void setTemporaryFolder(int memByteMax,long singerFileMaxSize,File file) {
        if ( memByteMax > 0) MEMORY_CACHE_BYTE_MAX = memByteMax;
        if (singerFileMaxSize > 0) SINGE_FILE_MAX_SIZE = singerFileMaxSize;
        if (file!=null) TEMPORARY_FOLDER = file;

        diskFileItemFactory.setRepository(TEMPORARY_FOLDER);
        // 设定上传文件的值，如果上传文件大于缓冲区值，就可能在repository所代表的文件夹中产生临时文件，否则直接在内存中进行处理
        diskFileItemFactory.setSizeThreshold(MEMORY_CACHE_BYTE_MAX);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-type", "text/html;charset=UTF-8");

        ServletResult result = new ServletResult();
        List<UploadFileItemResult> resultList = null;
        try {

            //指定对应下标的文件保存路径
            ArrayList<String> pathList = checkDirPathByList( filterData(req.getHeader("specify-path")));
            //指定对应下标的文件保存文件名
            ArrayList<String> fileNameList = filterData( req.getHeader("specify-filename"));

            if (!ServletFileUpload.isMultipartContent(req)){
                throw new IllegalArgumentException("content-type is not 'multipart/form-data'");
            }

            // 创建一个ServletFileUpload对象
            ServletFileUpload uploader = new ServletFileUpload(diskFileItemFactory);
            uploader.setFileSizeMax(SINGE_FILE_MAX_SIZE);
            uploader.setHeaderEncoding("utf-8");

            List<FileItem> listItems = uploader.parseRequest(req);
            resultList = new FileUploadOperation(pathList,fileNameList,listItems).execute();

            final List<UploadFileItemResult> _resultList = resultList;
            if (_resultList.size() == 0) throw new IllegalAccessException("表单文件解析错误");

            subHook(req, _resultList); //钩子

        } catch (Exception e) {
            Log4j.info(Thread.currentThread()+" 文件上传错误: "+ e.getMessage());
            result.value(EXCEPTION);
        }finally {
            //向客户端返回结果
            if (resultList!=null){
                result.setData(resultList);
            }
            writeJson(resp,result);
        }
    }

    protected void subHook(HttpServletRequest req, List<UploadFileItemResult> resultList){
        // 记录文件
        for (UploadFileItemResult it : resultList){
            if (it.success){
                SQLiteFileTable.addFile_LOCAL(it.relativePath,it.httpUrl);
            }
        }
    }
}
