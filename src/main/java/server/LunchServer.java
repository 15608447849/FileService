package server;


import bottle.backup.client.FtcBackupClient;
import bottle.backup.server.FtcBackupServer;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import server.prop.BackupProperties;
import server.prop.WebProperties;
import server.servlet.imps.*;

import java.io.IOException;
import java.nio.file.Paths;

import static io.undertow.servlet.Servlets.servlet;


/**
 * Created by lzp on 2017/5/13.
 * 容器入口
 */
public class LunchServer {

    public static void main(String[] args) throws IOException {
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
                    .setResourceManager(
                            new PathResourceManager(Paths.get(WebProperties.get().rootPath), 16*4069L)
                    );
            servletBuilder.addServlet(servlet("文件上传-图片裁剪-文件同步", FileBackup.class).addMapping("/upload"));
            servletBuilder.addServlet(servlet("服务器在线监测", Online.class).addMapping("/online"));
            servletBuilder.addServlet(servlet("指定文件列表生成zip", GenerateZip.class).addMapping("/zip"));
            servletBuilder.addServlet(servlet("读取excel", Excel.class).addMapping("/excel"));
            servletBuilder.addServlet(servlet("遍历文件列表", FileErgodic.class).addMapping("/ergodic"));
            servletBuilder.addServlet(servlet("删除文件列表", FileDelete.class).addMapping("/delete"));

            DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);

            manager.deploy();

            HttpHandler httpHandler = manager.start();

            //路径默认处理程序
            PathHandler pathHandler =
                    Handlers.path(httpHandler);

            Undertow.builder()
                    .addHttpListener(WebProperties.get().webPort, WebProperties.get().webIp, pathHandler)
                    .build()
                    .start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }





    private static void startFileBackupServer() {
        try {
            if (!BackupProperties.get().isAccess) return;

            BackupProperties.get().ftcBackupServer = new FtcBackupServer(WebProperties.get().rootPath,WebProperties.get().webIp, BackupProperties.get().localPort,64,5000);

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




