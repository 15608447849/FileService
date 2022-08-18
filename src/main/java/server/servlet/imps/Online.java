package server.servlet.imps;


import bottle.tuples.Tuple2;
import bottle.util.*;

import server.clear.FileClear;
import server.comm.RuntimeUtil;
import server.hwobs.HWOBSAgent;
import server.hwobs.HWOBSErgodic;
import server.hwobs.HWOBSServer;
import server.undertow.*;
import server.comm.SysUtil;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import java.text.DecimalFormat;
import java.util.*;

import static server.comm.RuntimeUtil.*;
import static server.undertow.AccessControlAllowOriginFilter.*;

// 回复当前文件服务器的信息/状态

@ServletAnnotation(name = "服务在线状态信息查看",path = "/online")
public class Online extends CustomServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-type", "text/html;charset=UTF-8");

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
            return;
        }

        try {

            if (params.equalsIgnoreCase("access")){

                StringBuilder s = new StringBuilder();
                List<Thread> list = new ArrayList<>(accessRequestMap.keySet());
                list.sort((o1, o2) -> (int) (o1.getId()-o2.getId()));

                for (Thread thread : list){
                    List<String> values = accessRequestMap.get(thread);
                    Collections.reverse(values);
                    s.append("<br/>&#160;").append(thread.getName()+","+thread.getId()).append(" &#160; 使用次数: "+ values.size());
                    int i=0;
                    for (String info : values){
                        s.append("<br/>&#160;&#160;").append( info );
                        i++;
                        if (i>=3) break;
                    }
                    s.append("<br/>");
                }

                writeString(resp, s.toString(),true);

            }

            else if (params.startsWith("accesspath:")){
                StringBuilder s = new StringBuilder();
                List<Tuple2<String,Long>> list = new ArrayList<>();
                for (String path: accessRequestPathMap.keySet()){
                    list.add(new Tuple2<>(path,accessRequestPathMap.get(path)));
                }

                String orderby = params.replace("accesspath:","");
                if (orderby.equalsIgnoreCase("ASC")){
                    list.sort((o1, o2) -> (int) (o1.getValue1()-o2.getValue1()));
                }
                if (orderby.equalsIgnoreCase("DESC")){
                    list.sort((o1, o2) -> (int) (o2.getValue1()-o1.getValue1()));
                }

                for (Tuple2<String,Long> tuple2 : list){
                    s.append("<br/>&#160;访问次数: "+ tuple2.getValue1()).append("&#160;").append(tuple2.getValue0());
                }
                writeString(resp, s.toString(),true);
            }

            else if (params.equalsIgnoreCase("system")){

                ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
                DecimalFormat percent = new DecimalFormat("0.00%");
                String str =
                        "<br/>&#160;" + "当前进程: " + ManagementFactory.getRuntimeMXBean().getName() +
                        "<br/>&#160;" + "存储目录: " + WebServer.rootFolderStr + " 最大存储时间: "+ TimeTool.formatDuring(FileClear.storageFileExpireTimeout * 1000L) +
                        "<br/>&#160;" + "临时目录: " +  WebServer.GET_TEMP_FILE_DIR() + " 最大存储时间: "+ TimeTool.formatDuring(FileClear.tempFileExpireTimeout * 1000L) +

                        "<br/>&#160;" + "系统 CUP 内核数: " +  Runtime.getRuntime().availableProcessors() +
                        "<br/>&#160;" + "系统 CPU 使用率: " +  percent.format(getSystemCpuLoad()) +

                        "<br/>&#160;" + "系统 物理内存 总大小: " +  byteLength2StringShow(getTotalPhysicalMemorySize()) +
                        "<br/>&#160;" + "系统 物理内存 已使用: " +  byteLength2StringShow(getUsedPhysicalMemorySize()) +

                        "<br/>&#160;" + "JVM 内存 最大值: " + byteLength2StringShow(getJvmMaxMemory()) +
                        "<br/>&#160;" + "JVM 内存 总大小: " + byteLength2StringShow(getJvmTotalMemory()) +
                        "<br/>&#160;" + "JVM 内存 已使用: " + byteLength2StringShow(getJvmUsedMemory()) +

                        "<br/>&#160;" + "JVM CPU 已使用: " + percent.format(getProcessCpuLoad()) +
                        "<br/>&#160;" + "JVM CPU 已使用(线程总和): " + percent.format(SysUtil.getInstance().getProcessCpu()) +

                        "<br/>&#160;" + "JVM 线程 总数: " + tmx.getThreadCount() +
                        "<br/>&#160;" + "JVM 线程 活跃数: " + Thread.activeCount() +

                        "<br/>&#160;" + "IO访问 线程 总数: " + accessRequestMap.size() +
                        "<br/>&#160;" + "IO访问 总次数: " + accessCountCollect+"@"+accessCount +

                        "<br/>&#160;" + "允许缓存文件长度(MAX): " + byteLength2StringShow(CustomResourceManager.max_cache_len) +
                        "<br/>&#160;" + "允许缓存文件时效: " + TimeTool.formatDuring(CustomResourceManager.max_cache_time*1000L) +

                        "<br/>&#160; 存储文件清理 "
                                + " 时间: "+  ( FileClear.delFileStartTime==0? "未开始" : TimeTool.date_yMd_Hms_2Str(new Date(FileClear.delFileStartTime)) )
                                + " 用时: "+  ( FileClear.delFileUseTime==0? " / " : TimeTool.formatDuring(FileClear.delFileUseTime) )
                                + " 数量: "+ FileClear.delFileCountTotal
                                + " 大小: "+ RuntimeUtil.byteLength2StringShow(FileClear.delFileLenTotal) +

                        "<br/>&#160;" + "运行时长: " + "&#160;" + TimeTool.formatDuring((System.currentTimeMillis() - WebServer.startTime)) ;

                writeString(resp, str,true);
            }
            else if (params.equalsIgnoreCase("hwobs")){
                String str =    "<br/>&#160; 上次扫描开始时间: " + ( HWOBSErgodic.start_scan_time==0? "未进行": TimeTool.date_yMd_Hms_2Str(new Date(HWOBSErgodic.start_scan_time)) ) +
                                "<br/>&#160; 上次扫描结束时间: " + ( HWOBSErgodic.end_scan_time==0? "未进行": TimeTool.date_yMd_Hms_2Str(new Date(HWOBSErgodic.end_scan_time)) )  +
                                "<br/>&#160;" + (  HWOBSErgodic.isScan ? " 正在扫描":" 扫描完成" ) +
                                "<br/>&#160; 已扫描文件数: " + HWOBSErgodic.current_ergodic +
                                "<br/>&#160; 已扫描文件大小: " + RuntimeUtil.byteLength2StringShow(HWOBSErgodic.current_ergodic_file_len) +
                                "<br/>&#160; 已添加文件数: " + HWOBSErgodic.current_ergodic_add_upload_queue +
                                "<br/>&#160; 待上传队列数: " + HWOBSAgent.getQueueSize() +
                                "<br/>&#160; 最大上传数: " +   HWOBSAgent.max_upload_size +
                                "<br/>&#160; 上传间隔(秒): " + HWOBSAgent.force_interval_sec +
                                "<br/>&#160; 分段线程数: " + HWOBSServer.uploadSegThreads +
                                "<br/>&#160; 分段大小: " +  HWOBSServer.uploadSegSize;

                writeString(resp, str,true);
            }else {
                writeString(resp, "command error...",true);
            }
        } catch (Exception e) {
            writeString(resp,e.getMessage(),true);
        }

    }


}
