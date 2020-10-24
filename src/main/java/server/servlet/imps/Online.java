package server.servlet.imps;


import bottle.util.EncryptUtil;


import bottle.util.Log4j;
import bottle.util.TimeTool;
import server.HuaWeiOBS.OBSUploadPoolUtil;
import server.prop.WebServer;
import server.servlet.beans.operation.FileErgodicOperation;
import server.servlet.iface.Mservlet;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Online extends Mservlet {

    private static Set<String> scannedSet = new HashSet<>();

    static {
        new Timer(true).schedule(new TimerTask() {
            @Override
            public void run() {
                if (isIng) return;
                scannedSet.clear();
            }
        },0,60 * 60 * 1000L);
    }

    private static boolean isIng = false;
    private static long startTime;
    private static long endTime;
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        final String file_ergodic_path = req.getParameter("file_ergodic_path");
        final int file_ergodic_max = Integer.parseInt(req.getParameter("file_ergodic_max"));

        String str = "current scanned size: "+ scannedSet.size();

        if (!isIng){
            isIng = true;
            str += "\tstart scanner, last used time: " + TimeTool.formatDuring((endTime - startTime));
            startTime = System.currentTimeMillis();
            final  Runnable runnable = () -> {
                executeErgodic( file_ergodic_path,file_ergodic_max);
                isIng = false;
                endTime = System.currentTimeMillis();
            };
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.start();

        }else{
            str += "\tscanner ing, current used time: "+ TimeTool.formatDuring((System.currentTimeMillis() - startTime));
        }


        writeString(resp,"online\n"+str,true);
    }

    private static List<String> executeErgodic(String path,int max) {

        FileErgodicOperation op = new FileErgodicOperation(WebServer.rootFolderStr + path, true);
        List<String> list = new ArrayList<>();

        op.setCallback(file -> {

            if (max==0 || list.size()<max) {
                try {
                    String localFilePath = file.getCanonicalPath();
                    if (scannedSet.add(localFilePath)){
                        boolean isAdd = OBSUploadPoolUtil.addFileToQueue(localFilePath);
                        if (isAdd){
                            list.add(localFilePath);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return false;
        });
        op.start();

        return list;
    }
}
