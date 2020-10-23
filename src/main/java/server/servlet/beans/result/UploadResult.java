package server.servlet.beans.result;

import java.util.List;

/**
 * Created by user on 2017/7/11.
 */
public class UploadResult {
    public boolean success = false; //是否本地保存成功
    public String error;//错误信息
    public String httpUrl;//http下载的绝对路径
    public String currentFileName;//现在的文件名
    public String suffix; //文件后缀
    public String md5;//文件MD5值
    public String localAbsolutelyPath;//本地存储的绝对
    public long fileSize = 0L;// 文件大小
}
