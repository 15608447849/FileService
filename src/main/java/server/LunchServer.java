package server;




import bottle.tcps.backup.client.FtcBackupClient;
import bottle.tcps.backup.server.FtcBackupServer;
import bottle.util.Log4j;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;

import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import server.prop.BackupProperties;
import server.prop.WebProperties;
import server.servlet.beans.operation.FileClearThread;
import server.servlet.iface.AccessControlAllowOriginFilter;
import server.servlet.imps.*;
import javax.servlet.*;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;


import static io.undertow.servlet.Servlets.servlet;


/**
 * Created by lzp on 2017/5/13.
 * 容器入口
 */
public class LunchServer {

    //获取本机所有IP地址
    private static List<String> getLocalIPList() {
        List<String> ipList = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            NetworkInterface networkInterface;
            Enumeration<InetAddress> inetAddresses;
            InetAddress inetAddress;
            String ip;
            while (networkInterfaces.hasMoreElements()) {
                networkInterface = networkInterfaces.nextElement();
                inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    inetAddress = inetAddresses.nextElement();
                    if (inetAddress instanceof Inet4Address) { // IPV4
                        ip = inetAddress.getHostAddress();
                        ipList.add(ip);
                    }
                }
            }
        } catch (SocketException e) {
            Log4j.error("文件服务错误",e);
        }
        return ipList;
    }

    public static void main(String[] args) throws Exception {
          //开启web文件服务器
          startWebServer();
          //开启文件备份服务
          startFileBackupServer();
    }


    private static Undertow webObject;

    private static void startWebServer() {
        try {

            //开启web文件服务器
            DeploymentInfo servletBuilder = Servlets.deployment()
                    .setClassLoader(LunchServer.class.getClassLoader())
                    .setContextPath("/")
                    .setDeploymentName("file_server.war")
                    .addFilter(new FilterInfo("跨域过滤", AccessControlAllowOriginFilter.class))
                    .addFilterUrlMapping("跨域过滤","/*",DispatcherType.REQUEST)
                    .setResourceManager(
                            new PathResourceManager(Paths.get(WebProperties.rootPath), 16*4069L)
                    );

            servletBuilder.addServlet(servlet("文件上传-文件同步-图片处理", ImageHandle.class).addMapping("/upload"));
            servletBuilder.addServlet(servlet("服务器在线监测", Online.class).addMapping("/online"));
            servletBuilder.addServlet(servlet("指定文件列表生成zip", GenerateZip.class).addMapping("/zip"));

            servletBuilder.addServlet(servlet("遍历文件列表", FileErgodic.class).addMapping("/ergodic"));
            servletBuilder.addServlet(servlet("删除文件列表", FileDelete.class).addMapping("/delete"));
            servletBuilder.addServlet(servlet("查询图片大小或对所有图片进行压缩", ImageSizeQuery.class).addMapping("/imageSize"));

            servletBuilder.addServlet(servlet("图片像素颜色", ImagePixColor.class).addMapping("/pixColor"));

            DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);

            manager.deploy();
            HttpHandler httpHandler = manager.start();

            //获取本机所有IP信息
            List<String> ipList = getLocalIPList();

            if (ipList.isEmpty()) throw new RuntimeException("没有可用的IP地址");

            Undertow.Builder builder = Undertow.builder();
            for (String ip : ipList){
                builder.addHttpListener(WebProperties.webPort,ip,httpHandler);
            }

            webObject = builder.build();
            webObject.start();
            FileClearThread.get().start();
        } catch (Exception e) {
            Log4j.error("文件服务错误",e);
        }
    }

    private static void startFileBackupServer() {
        try {
            if (!BackupProperties.isAccess) return;

            BackupProperties.ftcBackupServer = new FtcBackupServer(WebProperties.rootPath,
                    BackupProperties.localIp,
                    BackupProperties.localPort,
                    64,
                    5000);

            FtcBackupClient client = BackupProperties.ftcBackupServer.getClient();

            BackupProperties.ftcBackupServer.setCallback(client::addBackupFile);

            client.addFilterSuffix(".tmp");
            client.addServerAddress(BackupProperties.remoteList);
            if (BackupProperties.isBoot){
                client.ergodicDirectory();
            }
            client.setTime(BackupProperties.time);

        } catch (IOException e) {
            Log4j.error("文件服务错误",e);
        }

    }

}




