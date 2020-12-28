package server.servlet.imps;

import bottle.util.Log4j;
import bottle.util.TimeTool;
import server.HuaWeiOBS.OBSUploadPoolUtil;
import server.prop.WebServer;
import server.servlet.beans.operation.FileErgodicOperation;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @Author: leeping
 * @Date: 2020/10/24 15:31
 */
public class ScanFileToOBS implements FileInsideOperation.InsideOperationI{
    private static Set<String> scannedSet = new HashSet<>();

    private static boolean isIng = false;
    private static long startTime;
    private static long endTime;
    private static long lastTotal;
    private static String[] filterSuffix;
    private static StringBuilder sb = new StringBuilder();

    @Override
    public String execute(HttpServletRequest req) {
        String filterList = req.getParameter("filter");
        if (filterList!=null){
            filterSuffix = filterList.split(",");
        }

        String str = "current scanned size: "+ scannedSet.size()+"\t";
        if (!isIng){
            isIng = true;
            str += "start scanner, last used time: " + TimeTool.formatDuring((endTime - startTime)) + ", file total size: "+ lastTotal;
            startTime = System.currentTimeMillis();
            final  Runnable runnable = () -> {
                executeErgodic();
                isIng = false;
                endTime = System.currentTimeMillis();
                lastTotal = scannedSet.size();
                scannedSet.clear();
                sb.delete(0,sb.length());
            };
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.start();
        }else{
            str += "scanner ing, current used time: "+ TimeTool.formatDuring((System.currentTimeMillis() - startTime));
        }
        return str + "\n\n"+sb.toString();
    }

    private static void executeErgodic() {
        FileErgodicOperation op = new FileErgodicOperation(WebServer.rootFolderStr, true);
        op.setCallback(file -> {
            try {
                boolean isPass = false;
                String localFilePath = file.getCanonicalPath();
                if (filterSuffix!=null && filterSuffix.length>0){
                    String suffix = localFilePath.substring(localFilePath.lastIndexOf("."+1));
                    for (String it : filterSuffix){
                        if (it.equals(suffix)){
                            isPass = true;
                            break;
                        }
                    }
                }

                if (!isPass && scannedSet.add(localFilePath)){
                    boolean isAdd = OBSUploadPoolUtil.addFileToQueue(localFilePath);
                    if (!isAdd){
                       sb.append("\n添加OBS队列失败: ").append(localFilePath);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        });
        op.start();

    }





}
