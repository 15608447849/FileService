package server.servlet.beans.operation;


import bottle.tcps.backup.client.FtcBackupClient;
import bottle.threadpool.IOThreadPool;
import server.prop.BackupProperties;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @Author: leeping
 * @Date: 2019/8/2 11:39
 */
public class FileBackUpOperation {

    public static void add(File file){
        if (!BackupProperties.isAccess) return;
        FtcBackupClient client =  BackupProperties.ftcBackupServer.getClient();
        client.addBackupFile(file);
    }

    public static void add(String filePath){
        add(new File(filePath));
    }
}
