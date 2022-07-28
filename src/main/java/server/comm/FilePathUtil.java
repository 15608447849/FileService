package server.comm;
import bottle.util.FileTool;

import java.util.ArrayList;

public class FilePathUtil {

    //检测目录路径是否正确
    public static String checkDirPath(String path) {
        if (path.startsWith("..")) path = path.replace("..","");
        if (path.startsWith(".")) path = path.replace(".","");

        path = path.replace("\\", FileTool.SEPARATOR);
        if (!path.startsWith(FileTool.SEPARATOR)) path = FileTool.SEPARATOR + path;//保证前面有 '/'
        if (!path.endsWith(FileTool.SEPARATOR)) path += FileTool.SEPARATOR; //后面保证 '/'
        return path;
    }

    //检测文件路径是否正确
    public static String checkFilePath(String path) {
        if (path.startsWith("..")) path = path.replace("..","");
        if (path.startsWith(".")) path = path.replace(".","");

        path = path.replace("\\", FileTool.SEPARATOR);
        if (!path.startsWith(FileTool.SEPARATOR)) path = FileTool.SEPARATOR + path;//保证前面有 '/'
        return path;
    }

    public static ArrayList<String> checkDirPathByList(ArrayList<String> pathList){
        if (pathList.size()>0){
            for(int i=0;i<pathList.size();i++){
                pathList.set(i,checkDirPath(pathList.get(i)));
            }
        }
        return pathList;
    }





}
