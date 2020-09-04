package server.prop;

import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.tcps.backup.server.FtcBackupServer;


import java.net.InetSocketAddress;

@PropertiesFilePath("/backup.properties")
public class BackupProperties {

    //文件同步服务是否启动
    @PropertiesName("ftc.backup.isAccess")
    public static boolean isAccess = false;
    //本地socket IP
    @PropertiesName("ftc.backup.server.local.ip")
    public static String localIp = "127.0.0.1";
    //本地socket端口
    @PropertiesName("ftc.backup.server.local.port")
    public static int localPort = 0;
    //远程文件服务器列表
    @PropertiesName("ftc.backup.server.remote.address")
    public static String remoteListStr = "127.0.0.1";
    //服务启动是否立即进行一次文件夹同步
    @PropertiesName("ftc.backup.server.first.boot")
    public static boolean isBoot = false;
    //文件上传后是否同步
    @PropertiesName("ftc.backup.server.upload.auto")
    public static boolean isAuto = false;
    //定时同步 '06:00','23:00'
    @PropertiesName("ftc.backup.server.time")
    public static String time="[06:00,23:00]";

    public static InetSocketAddress[] remoteList;

    public static FtcBackupServer ftcBackupServer;

    static {
        ApplicationPropertiesBase.initStaticFields(BackupProperties.class);

        String[] arr = remoteListStr.split(",");
        remoteList = new InetSocketAddress[arr.length];
        for (int i = 0; i < arr.length ;i++){
            String[] address = arr[i].split(":");
            if (address[0].equals("0.0.0.0")) continue;
            remoteList[i] = new InetSocketAddress(address[0],Integer.parseInt(address[1]));
        }
    }


}
