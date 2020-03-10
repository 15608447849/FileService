package server.servlet.beans.operation;

import bottle.util.EncryptUtils;
import bottle.util.FileUtils;
import bottle.util.Log4j;
import bottle.util.StringUtils;
import org.apache.commons.fileupload.FileItem;
import server.prop.WebProperties;
import server.servlet.beans.result.UploadResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static server.servlet.beans.operation.OperationUtils.getIndexValue;
import static server.servlet.beans.result.Result.RESULT_CODE.EXCEPTION;
import static server.servlet.beans.result.Result.RESULT_CODE.SUCCESS;

/**
 * Created by user on 2017/12/14.
 */
public class FileUploadOperation {

    private final ArrayList<String> specifyPaths; //指定的文件保存相对路径
    private final ArrayList<String> specifyNames;//指定的文件名
    private final ArrayList<String> specifyMd5;//保存MD5文件名
    private final List<FileItem> fileItems;

    public FileUploadOperation(ArrayList<String> specifyPaths, ArrayList<String> specifyNames, ArrayList<String> specifyMd5, List<FileItem> fileItems) {
        this.specifyPaths = specifyPaths;
        this.specifyNames = specifyNames;
        this.specifyMd5 = specifyMd5;
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
        boolean isSaveMD5Name;
        UploadResult uploadResult;

        for (int i = 0 ;i< fileItems.size();i++) {
            uploadResult = new UploadResult();
            fileItem = fileItems.get(i);
            areaName = fileItem.getFieldName();
            areaFileName = fileItem.getName();
            specifyPath = getIndexValue(specifyPaths,i,"/defaults/"+areaName+"/");
            specifyFileName = getIndexValue(specifyNames,i,areaFileName);
            isSaveMD5Name = getIndexValue(specifyMd5,i,false);
            saveFile(fileItem,specifyPath,specifyFileName, isSaveMD5Name,uploadResult);
            Log4j.info("表单域名 :"+areaName+" , 表单名 :"+areaFileName+" , 上传文件: " + specifyPath+specifyFileName);
            resultList.add(uploadResult);//添加结果集合
        }
        return resultList;
    }

    private void saveFile(FileItem fileItem, String specifyPath, String specifyFileName, boolean isSaveMD5Name, UploadResult uploadResult) {

        final String dirPath = WebProperties.rootPath; //本地绝对目录
        //创建目录
        if (!FileUtils.checkDir(dirPath+specifyPath)){
            uploadResult.error = "directory does not exist or created fail";
            return;
        }
        //获取后缀
        String suffix = "";
        if (specifyFileName.contains(".")){
            suffix = specifyFileName.substring(specifyFileName.lastIndexOf(".")); //包含 '.'
        }
        //相对完成路径
        String localRelativePath = specifyPath + specifyFileName;

        String md5FileRelativePath = null;

        try {
            File file = new File(dirPath + localRelativePath);
            boolean isWrite = true;

            if (file.exists()) {
                isWrite = FileUtils.deleteFile(file.getCanonicalPath());
            }
           if (isWrite) fileItem.write(file); //流写入文件
            fileItem.delete(); //删除临时文件
            String fileMd5 = EncryptUtils.getFileMd5ByString(file);//文件MD5

            if (isSaveMD5Name){
                //创建目录
                if (FileUtils.checkDir(dirPath + "/md5s" + specifyPath)){
                    md5FileRelativePath = "/md5s" + specifyPath + fileMd5 + "." +suffix;
                    FileUtils.copyFile(file,new File(dirPath + md5FileRelativePath)); //文件复制
                }
            }

            uploadResult.httpUrl = WebProperties.domain + localRelativePath;
            uploadResult.relativePath = localRelativePath;
            uploadResult.fileMd5 = fileMd5;
            uploadResult.currentFileName = specifyFileName;
            uploadResult.suffix = suffix;
            uploadResult.md5FileRelativePath = md5FileRelativePath;
            uploadResult.fileSize = file.length();
            uploadResult.success = true;
        } catch (Exception e) {
            e.printStackTrace();
            uploadResult.error = "save file error";
        }
    }



}
