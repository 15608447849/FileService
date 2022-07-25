package server.servlet.imps;


import bottle.util.*;

import server.hwobs.HWOBSServer;
import server.LunchServer;
import server.undertow.ServletAnnotation;
import server.undertow.WebServer;
import server.comm.SysUtil;
import server.undertow.CustomServlet;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import java.text.DecimalFormat;
import java.util.*;

import static server.comm.RuntimeUtil.*;
import static server.undertow.AccessControlAllowOriginFilter.lastAccessRequestMap;

// 回复当前文件服务器的信息/状态

@ServletAnnotation(name = "服务监测",path = "/online")
public class Online extends CustomServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-type", "text/html;charset=UTF-8");

//        Log4j.info( Thread.currentThread() + " 查看系统状态" );

        String params = req.getParameter("state");
        if (params==null){
            HashMap<String,String> map  = new HashMap<>();
            //文件下载地址
            map.put("download", WebServer.domain);
            //文件上传地址
            map.put("upload", WebServer.domain + "/upload");
            //文件遍历遍历
            map.put("ergodic", WebServer.domain + "/ergodic");
            //文件删除地址
            map.put("delete", WebServer.domain + "/delete");
            //文件批量下载
            map.put("zip", WebServer.domain + "/zip");
            //obs下载地址
            map.put("obs_download",HWOBSServer.convertLocalFileToOBSUrl(""));
            //cnd下载地址
            map.put("cnd_download",HWOBSServer.convertLocalFileToCDNUrl(""));
            writeJson(resp,map);
        }else{
            if (params.equals("restart")){
                synchronized (LunchServer.class){
                    LunchServer.class.notifyAll();
                    writeString(resp, "服务2秒后将重启",true);
                }
            }
            if (params.equals("lastRequest")){
                StringBuilder s = new StringBuilder();
                List<Thread> list = new ArrayList<>(lastAccessRequestMap.keySet());
                list.sort((o1, o2) -> (int) (o1.getId()-o2.getId()));

                for (Thread thread : list){
                    s.append("<br/>").append(thread).append(" :: ").append(lastAccessRequestMap.get(thread));
                }
                writeString(resp, s.toString(),true);
            }
            else{
                ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
                DecimalFormat percent = new DecimalFormat("0.00%");
                String str =
                                "<br/>\t" + "当前进程" + "\t" + ManagementFactory.getRuntimeMXBean().getName() +

                                "<br/>\t" + "系统 CUP 内核数" + "\t" + Runtime.getRuntime().availableProcessors() +
                                "<br/>\t" + "系统 CPU 使用率" + "\t" + percent.format(getSystemCpuLoad()) +

                                "<br/>\t" + "系统 物理内存 总大小" + "\t" + byteLength2StringShow(getTotalPhysicalMemorySize()) +
                                "<br/>\t" + "系统 物理内存 已使用" + "\t" + byteLength2StringShow(getUsedPhysicalMemorySize()) +

                                "<br/>\t" + "JVM 内存 最大值" + "\t" + byteLength2StringShow(getJvmMaxMemory()) +
                                "<br/>\t" + "JVM 内存 总大小" + "\t" + byteLength2StringShow(getJvmTotalMemory()) +
                                "<br/>\t" + "JVM 内存 已使用" + "\t" + byteLength2StringShow(getJvmUsedMemory()) +

                                "<br/>\t" + "JVM CPU 已使用" + "\t" + percent.format(getProcessCpuLoad()) +
                                "<br/>\t" + "JVM CPU 已使用(线程总和)" + "\t" + percent.format(SysUtil.getInstance().getProcessCpu()) +

                                "<br/>\t" + "JVM 线程 总数" + "\t" + tmx.getThreadCount() +
                                "<br/>\t" + "JVM 线程 活跃数" + "\t" + Thread.activeCount() +

                                "<br/>\t" + "运行时长 " + "\t" + TimeTool.formatDuring((System.currentTimeMillis() - WebServer.startTime));


                writeString(resp, str,true);
            }

        }

    }


}
