package server.servlet.beans;
;
import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.util.EncryptUtil;
import bottle.util.FileTool;
import bottle.util.Log4j;
import org.apache.commons.fileupload.FileItem;
import server.undertow.WebServer;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import static server.comm.OperationUtil.getIndexValue;
import static server.comm.SuffixConst.isEnableSaveToLocal;

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

    public  List<UploadFileItemResult>  execute() throws Exception{
        List<UploadFileItemResult> resultList = new ArrayList<>();
        for (int i = 0 ;i< fileItems.size();i++) {
            FileItem fileItem = fileItems.get(i);
            //普通文本表单-过滤
            if (fileItem.isFormField()) continue;
            //域名
            String areaName = fileItem.getFieldName();
            //域名中的文件名
            String areaFileName = fileItem.getName();
            //指定的保存路径
            String specifyPath = getIndexValue(specifyPaths,i,"/defaults/"+areaName+"/");
            //指定的保存文件名
            String specifyFileName = getIndexValue(specifyNames,i,areaFileName);
            //内容类型
            String contentType = fileItem.getContentType();
            //文件大小
            long size = fileItem.getSize();
            //当前数据保存位置
            boolean isMemStorage = fileItem.isInMemory();
            //保存文件
            UploadFileItemResult uploadResult = saveFile(fileItem,specifyPath,specifyFileName);
            //打印情况
            Log4j.info(" 域名: "+areaName
                    +", contentType: "+ contentType
                    +", 数据大小: "+ size
                    +", 缓存位置: "+ (isMemStorage?"内存":"磁盘")
                    +", 实际文件名: "+areaFileName
                    +", 执行路径: " + specifyPath
                    +", 指定文件名: " + specifyFileName
                    +", 保存结果: "+ uploadResult.success
            );

            //添加结果集合
            resultList.add(uploadResult);
        }
        return resultList;
    }


    private UploadFileItemResult saveFile(FileItem fileItem, String specifyPath, String specifyFileName) {
        UploadFileItemResult uploadResult = new UploadFileItemResult();
        try {
            //本地根目录路径
            final String rootPath = WebServer.rootFolderStr;
            //url访问 相对全路径
            final String localRelativePath = specifyPath + specifyFileName;
            //目录, 本地绝对全路径
            final String localAbsolutelyDictPath = rootPath + specifyPath;
            //文件 本地绝对全路径
            final String localAbsolutelyFilePath = rootPath + localRelativePath;

            String suffix = null;
            if (specifyFileName.contains(".")){
                suffix = specifyFileName.substring(specifyFileName.lastIndexOf(".")+1); //不包含 '.'
            }

            if (suffix == null){
                throw new IllegalArgumentException("文件后缀不存在");
            }


            if(!isEnableSaveToLocal(suffix)){
                throw new IllegalArgumentException("非法的文件后缀( "+suffix+" )");
            }

            //创建指定目录
            if (!FileTool.checkDir(localAbsolutelyDictPath)){
                throw new IllegalArgumentException("目录 ( "+localAbsolutelyDictPath+" ) 不存在或创建失败");
            }

            //创建指定文件
            File file = new File(localAbsolutelyFilePath);
            if (file.exists()) {
                if (!file.delete()){
                    throw new IllegalArgumentException("文件("+localAbsolutelyFilePath+") 存在且无法删除");
                }
            }
            fileItem.write(file); //流写入文件
            uploadResult.localAbsolutelyPath = localAbsolutelyFilePath;
            uploadResult.relativePath = localRelativePath;
            uploadResult.httpUrl = localAbsolutelyFilePath.replace(rootPath,WebServer.domain);
            uploadResult.currentFileName = specifyFileName;
            uploadResult.suffix = suffix;
            uploadResult.fileSize = file.length();
            uploadResult.md5 = EncryptUtil.getFileMd5ByString(file);
            uploadResult.success = true;
        } catch (Exception e) {
            uploadResult.error = e.getMessage();
            if (!(e instanceof IllegalArgumentException)){
                Log4j.error("文件服务错误",e);
            }
        }finally {
            fileItem.delete(); //删除临时文件
        }

        return uploadResult;
    }



}
