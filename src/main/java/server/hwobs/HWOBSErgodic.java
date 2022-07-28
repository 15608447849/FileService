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

    public static int current_ergodic=0;

    public static int current_ergodic_add_upload_queue=0;

    private static boolean isScan = false;

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
                if ( isEnable
                        && System.currentTimeMillis() - file.lastModified() > (retainedTime*1000L)
                        && !isSystemDefaultFileSuffix(file.getName()) ){
                    boolean isAdd = HWOBSAgent.addFileToQueue(file.getCanonicalPath());
                    if (isAdd) current_ergodic_add_upload_queue++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        });
        isScan = true;
        current_ergodic_add_upload_queue = 0;
        current_ergodic = 0;
        execute.start();
        Log4j.info("[华为OBS遍历] 文件总数: "+current_ergodic+"  添加队列: "+ current_ergodic_add_upload_queue + " 队列数量: " + HWOBSAgent.getQueueSize());
        current_ergodic_add_upload_queue = 0;
        current_ergodic = 0;
        isScan = false;
    }


    static {
        ApplicationPropertiesBase.initStaticFields(HWOBSErgodic.class);
    }
}
