package server.hwobs;

import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.util.Log4j;
import server.comm.FileErgodicExecute;
import server.undertow.WebServer;

import static server.comm.SuffixConst.isSystemDefaultFileSuffix;

@PropertiesFilePath("/hwobs.properties")
public class HWOBSErgodic {

    @PropertiesName("hwobs.ergodic.enable")
    private static boolean isRunning;

    @PropertiesName("hwobs.ergodic.interval")
    private static long loopTime;

    @PropertiesName("hwobs.ergodic.file.retained")
    private static long retainedTime;

    public static boolean isEnable = true;

    public static long start_scan_time = 0;

    public static long end_scan_time = 0;

    public static long current_ergodic=0;

    public static long current_ergodic_file_len=0;

    public static long current_ergodic_add_upload_queue=0;

    public static boolean isScan = false;

    protected static final Thread thread = new Thread(){
        @Override
        public void run() {
            while (isRunning ){
                try {
                    Thread.sleep(loopTime * 1000L);
                    localErgodic();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };


    // 本地文件遍历
    public static void localErgodic() {
        if (isScan) return;

        FileErgodicExecute execute = new FileErgodicExecute(WebServer.rootFolderStr, true);
        execute.setCallback(file -> {
            try {
                current_ergodic ++;
                current_ergodic_file_len += file.length();

                if ( isEnable  && !isSystemDefaultFileSuffix(file.getName()) && System.currentTimeMillis() - file.lastModified() > (retainedTime*1000L)){
                    boolean isAdd = HWOBSAgent.addFileToQueue(file.getCanonicalPath());
                    if (isAdd) current_ergodic_add_upload_queue++;
                }
            } catch (Exception e) {
              Log4j.error("OBS本地文件扫描",e);
            }
            return false;
        });

        isScan = true;
        current_ergodic_add_upload_queue = 0;
        current_ergodic = 0;
        current_ergodic_file_len = 0;
        end_scan_time = 0;
        start_scan_time = System.currentTimeMillis();
        execute.start();
        end_scan_time = System.currentTimeMillis();
        isScan = false;
    }


    static {
        ApplicationPropertiesBase.initStaticFields(HWOBSErgodic.class);
    }
}
