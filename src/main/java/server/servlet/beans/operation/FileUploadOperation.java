package server.servlet.beans.operation;
;
import bottle.util.EncryptUtil;
import bottle.util.FileTool;
import bottle.util.Log4j;
import org.apache.commons.fileupload.FileItem;
import server.prop.WebServer;
import server.servlet.beans.result.UploadResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static server.servlet.beans.operation.OperationUtils.getIndexValue;

/**
 * Created by user on 2017/12/14.
 */
public class FileUploadOperation {

    private final ArrayList<String> specifyPaths; //指定的文件保存相对路径
    private final ArrayList<String> specifyNames;//指定的文件名
    private final List<FileItem> fileItems;

    public FileUploadOperation(ArrayList<String> specifyPaths, ArrayList<String> specifyNames, List<FileItem> fileItems) {
        this.specifyPaths = specifyPaths;
        this.specifyNames = specifyNames;
        this.fileItems = fileItems;
    }

    public  List<UploadResult>  execute() throws Exception{
        List<UploadResult> resultList = new ArrayList<>();

        fileItems.removeIf(FileItem::isFormField);

        if (fileItems.size() == 0) throw new NullPointerException("the file item list is null.");

        FileItem fileItem;
        String areaName;//域名
        String areaFileName;//域名中的文件名
        String specifyPath ;
        String specifyFileName ;

        UploadResult uploadResult;

        for (int i = 0 ;i< fileItems.size();i++) {
            uploadResult = new UploadResult();
            fileItem = fileItems.get(i);
            areaName = fileItem.getFieldName();
            areaFileName = fileItem.getName();
            specifyPath = getIndexValue(specifyPaths,i,"/defaults/"+areaName+"/");
            specifyFileName = getIndexValue(specifyNames,i,areaFileName);

            saveFile(fileItem,specifyPath,specifyFileName,uploadResult);
            Log4j.info("表单域名 :"+areaName+" , 表单名 :"+areaFileName+" , 上传文件: " + specifyPath+specifyFileName);
            resultList.add(uploadResult);//添加结果集合
        }
        return resultList;
    }

    private void saveFile(FileItem fileItem, String specifyPath, String specifyFileName,UploadResult uploadResult) {
        //本地跟目录路径
        final String rootPath = WebServer.rootFolderStr;
        //url访问 相对全路径
        final String localRelativePath = specifyPath + specifyFileName;
       //目录, 本地绝对全路径
        final String localAbsolutelyDictPath = rootPath + specifyPath;
        //文件 本地绝对全路径
        final String localAbsolutelyFilePath = rootPath + localRelativePath;

        //获取文件后缀
        String suffix = "";
        if (specifyFileName.contains(".")){
            suffix = specifyFileName.substring(specifyFileName.lastIndexOf(".")); //包含 '.'
        }


        //创建指定目录
        if (!FileTool.checkDir(localAbsolutelyDictPath)){
            uploadResult.error = "directory( "+localAbsolutelyDictPath+" ) does not exist or created fail";
            return;
        }

        try {
            File file = new File(localAbsolutelyFilePath);

            if (file.exists()) {
                if (file.delete()){
                    uploadResult.error = "file("+localAbsolutelyFilePath+") exists and cannot be deleted";
                    return;
                }
            }
            fileItem.write(file); //流写入文件
            fileItem.delete(); //删除临时文件

            uploadResult.localAbsolutelyPath = localAbsolutelyFilePath;
            uploadResult.httpUrl = localAbsolutelyFilePath.replace(rootPath,WebServer.domain);
            uploadResult.currentFileName = specifyFileName;
            uploadResult.suffix = suffix;
            uploadResult.fileSize = file.length();
            uploadResult.md5 = EncryptUtil.getFileMd5ByString(file);
            uploadResult.success = true;
        } catch (Exception e) {
            Log4j.error("文件服务错误",e);
            uploadResult.error = "save file error";
        }
    }



}
