package server.prop;


import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.util.FileTool;
import bottle.util.Log4j;
import bottle.util.StringUtil;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import server.LunchServer;
import server.servlet.iface.AccessControlAllowOriginFilter;
import server.servlet.imps.*;

import javax.servlet.DispatcherType;
import java.io.File;
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

@PropertiesFilePath("/web.properties")
public class WebServer {

    @PropertiesName("web.port")
    private static int webPort = 80;
    @PropertiesName("web.file.directory")
    private static String rootPath = "./file_server_root";
    @PropertiesName("web.domain")
    public static String domain = "http://127.0.0.1:80";

    @PropertiesName("image.logo.text")
    public static String logoText = null;
    @PropertiesName("image.logo.text.position")
    public static int logoTextPosition = 0;
    @PropertiesName("image.logo.text.rotate")
    public static int logoTextRotate = 0;
    @PropertiesName("image.logo.text.color")
    public static String logoTextColor = "000000";
    @PropertiesName("image.logo.text.alpha")
    public static float logoTextAlpha = 0.15f;

    @PropertiesName("image.logo.icon")
    public static String logoIcon = null;
    @PropertiesName("image.logo.icon.position")
    public static int logoIconPosition = 0;
    @PropertiesName("image.logo.icon.alpha")
    public static float logoIconAlpha = 0.15f;
    @PropertiesName("image.logo.icon.rotate")
    public static int logoIconRotate = 0;

    //根目录文件夹
    public static File rootFolder;
    public static String rootFolderStr;
    //临时目录文件夹
    public static File temporaryFolder;

    static {
        ApplicationPropertiesBase.initStaticFields(WebServer.class);

        rootFolder = new File(rootPath);
        temporaryFolder = new File(rootPath + FileTool.SEPARATOR+"temporary");

        if (!rootFolder.exists() ){
            if (!rootFolder.mkdirs()) throw new IllegalStateException("启动失败,无效的主目录路径:"+rootFolder);
        }
        if (!temporaryFolder.exists() ){
            if (!temporaryFolder.mkdirs()) throw new IllegalStateException("启动失败,无效的临时目录路径:"+temporaryFolder);
        }

        try {
            rootFolderStr = rootFolder.getCanonicalPath();
            Log4j.info("本地文件根目录: "+ rootFolderStr);
        } catch (IOException e) {
            throw new RuntimeException("获取根目录全路径异常");
        }
    }

    private static Undertow instance;
    public static void startWebServer() {
        try {
            if (instance!=null) return;
            //开启web文件服务器
            DeploymentInfo servletBuilder = Servlets.deployment()
                    .setClassLoader(LunchServer.class.getClassLoader())
                    .setContextPath("/")
                    .setDeploymentName("file_server.war")
                    .addFilter(new FilterInfo("跨域过滤", AccessControlAllowOriginFilter.class))
                    .addFilterUrlMapping("跨域过滤","/*", DispatcherType.REQUEST)
                    .setResourceManager(
                            new PathResourceManager(Paths.get(WebServer.rootPath), 16*4069L)
                    );

            servletBuilder.addServlet(servlet("文件上传-图片处理", ImageHandle.class).addMapping("/upload"));
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
            List<String> ipList = StringUtil.getLocalIPList();

            if (ipList.isEmpty()) throw new RuntimeException("没有可用的IP地址");

            Undertow.Builder builder = Undertow.builder();
            for (String ip : ipList){
                builder.addHttpListener(WebServer.webPort,ip,httpHandler);
                Log4j.info("监听本地地址:  " + ip + " " + WebServer.webPort );
            }

            instance = builder.build();
            instance.start();

        } catch (Exception e) {
            Log4j.error("文件服务错误",e);
        }
    }

}
