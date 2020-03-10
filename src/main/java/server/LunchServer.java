package server;


import bottle.backup.client.FtcBackupClient;
import bottle.backup.server.FtcBackupServer;
import bottle.util.Log4j;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.SetHeaderHandler;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionListener;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ListenerInfo;
import server.prop.BackupProperties;
import server.prop.WebProperties;
import server.servlet.iface.AccessControlAllowOriginFilter;
import server.servlet.imps.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Paths;


import static io.undertow.servlet.Servlets.servlet;


/**
 * Created by lzp on 2017/5/13.
 * 容器入口
 */
public class LunchServer {

    public static void main(String[] args) throws Exception {
          //开启web文件服务器
          startWebServer();
          //开启文件备份服务
          startFileBackupServer();
    }


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
            servletBuilder.addServlet(servlet("读取excel", Excel.class).addMapping("/excel"));
            servletBuilder.addServlet(servlet("遍历文件列表", FileErgodic.class).addMapping("/ergodic"));
            servletBuilder.addServlet(servlet("删除文件列表", FileDelete.class).addMapping("/delete"));
            servletBuilder.addServlet(servlet("查询图片大小或对所有图片进行压缩", ImageSizeQuery.class).addMapping("/imageSize"));
            servletBuilder.addServlet(servlet("图片识别OCR", ImageOCR.class).addMapping("/ocr"));
            servletBuilder.addServlet(servlet("图片像素颜色", ImagePixColor.class).addMapping("/pixColor"));

            DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);

            manager.deploy();
            HttpHandler httpHandler = manager.start();

            //路径默认处理程序
//            PathHandler pathHandler =
//                    Handlers.path(httpHandler);

            Undertow.builder()
                    .addHttpListener(WebProperties.webPort, WebProperties.webIp, httpHandler)
                    .build()
                    .start();
            System.out.println("打开服务 : " + WebProperties.webIp+" - "+WebProperties.webPort +" - "+ WebProperties.rootPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void startFileBackupServer() {
        try {
            if (!BackupProperties.get().isAccess) return;

            BackupProperties.get().ftcBackupServer = new FtcBackupServer(WebProperties.rootPath,WebProperties.webIp, BackupProperties.get().localPort,64,5000);

            FtcBackupClient client = BackupProperties.get().ftcBackupServer.getClient();

            BackupProperties.get().ftcBackupServer.setCallback(client::addBackupFile);

            client.addFilterSuffix(".tmp");
            client.addServerAddress(BackupProperties.get().remoteList);
            if (BackupProperties.get().isBoot){
                client.ergodicDirectory();
            }
            client.setTime(BackupProperties.get().time);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }



}




