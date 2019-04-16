package server.servlet.imps;

import bottle.backup.client.FtcBackupClient;
import server.prop.BackupProperties;
import server.servlet.beans.result.UploadResult;
import server.prop.WebProperties;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static server.servlet.beans.operation.OperationUtils.getIndexValue;

/**
 * Created by user on 2017/12/14.
 * 上传完文件并且同步文件
 */
public class FileBackup extends ImageTailor {

    //is-sync = true;false;...

    @Override
    protected void subHook(HttpServletRequest req, List<UploadResult> resultList) {
        super.subHook(req, resultList);
        if (resultList!=null && resultList.size() > 0){
            if (!BackupProperties.get().isAccess) return;

            //指定对应下标的文件的是否同步
            ArrayList<String> isSyncList = filterData(req.getHeader("is-sync"));
            FtcBackupClient client =  BackupProperties.get().ftcBackupServer.getClient();
            String rootPath = WebProperties.get().rootPath;

            UploadResult it;
            boolean isSync;

            for (int i=0; i<resultList.size();i++){
                it = new UploadResult();

                if (!it.success)  continue;

                isSync = getIndexValue(isSyncList, i ,BackupProperties.get().isAuto);

                if (isSync){
                    client.addBackupFile(new File(rootPath + it.relativePath)); //同步源文件
                    if (it.md5FileRelativePath != null){
                        client.addBackupFile(new File(rootPath + it.md5FileRelativePath));//md5文件同步
                    }
                    if (it.tailorPathList !=null){
                        it.tailorPathList.forEach(path -> client.addBackupFile(new File(rootPath + path))); //图片裁剪文件同步
                    }
                }
            }
        }
    }
}
