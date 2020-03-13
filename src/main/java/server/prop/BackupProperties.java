package server.prop;

import bottle.backup.server.FtcBackupServer;
import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;


import java.net.InetSocketAddress;

@PropertiesFilePath("/backup.properties")
public class BackupProperties {

    @PropertiesName("ftc.backup.isAccess")
    public static boolean isAccess;
    @PropertiesName("ftc.backup.server.local.port")
    public static int localPort;
    @PropertiesName("ftc.backup.server.remote.address")
    public static String remoteListStr;
    @PropertiesName("ftc.backup.server.first.boot")
    public static boolean isBoot;
    @PropertiesName("ftc.backup.server.upload.auto")
    public static boolean isAuto;
    @PropertiesName("ftc.backup.server.time")
    public static String time;

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
