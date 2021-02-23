package server.prop;


import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.util.FileTool;
import bottle.util.Log4j;
import bottle.util.StringUtil;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import org.xnio.CompressionType;
import org.xnio.Option;
import org.xnio.Options;
import server.HuaWeiOBS.OBSUploadPoolUtil;
import server.LunchServer;
import server.servlet.beans.operation.FileClear;
import server.servlet.beans.operation.FileUploadOperation;
import server.servlet.beans.operation.ImageOperation;
import server.servlet.iface.AccessControlAllowOriginFilter;
import server.servlet.imps.*;
import server.sqlites.SQLiteUtils;

import javax.servlet.DispatcherType;
import java.io.File;

import java.nio.file.Paths;
import java.util.Arrays;
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
    @PropertiesName("obs.enable.hw")
    public static int hwObsIsOpen = 0;


    @PropertiesName("upload.suffix.black.list")
    public static String upload_suffix_black_list;
    @PropertiesName("upload.suffix.white.list")
    public static String upload_suffix_white_list;

    //根目录文件夹
    public static File rootFolder;
    public static String rootFolderStr;

    public static long startTime;
    private static Undertow instance;

    //临时文件目录
    public static String GET_TEMP_FILE_DIR(){
        return rootFolderStr + FileTool.SEPARATOR+ "temp" ;
    }

    private static void loadPlug() {
        //图片处理
        ImageOperation.start();
//        //文件清理线程
        FileClear.start();
        //OBS同步
        if(hwObsIsOpen>0) OBSUploadPoolUtil.start();
    }

    private static void loadUndertow() throws Exception{
            if (instance==null){
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

                servletBuilder.addServlet(servlet("服务器在线监测", Online.class).addMapping("/online"));
                servletBuilder.addServlet(servlet("文件上传", ImageHandle.class).addMapping("/upload"));
                servletBuilder.addServlet(servlet("指定文件列表生成ZIP", GenerateZip.class).addMapping("/zip"));
                servletBuilder.addServlet(servlet("遍历文件列表", FileErgodic.class).addMapping("/ergodic"));
                servletBuilder.addServlet(servlet("删除文件", FileDelete.class).addMapping("/delete"));
                servletBuilder.addServlet(servlet("图片像素颜色", ImagePixColor.class).addMapping("/pixColor"));
                servletBuilder.addServlet(servlet("文件内部命令", FileInsideOperation.class).addMapping("/operation"));
                servletBuilder.addServlet(servlet("客户端日志记录", LogAppend.class).addMapping("/logAppend"));

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

                builder.setIoThreads(16);
                builder.setWorkerThreads(256);
                builder.setDirectBuffers(true);
//                builder.setBufferSize(1024 * 1024 * 1024);
//                builder.setBufferSize(64 * 1024 * 1024);
                builder.setServerOption(UndertowOptions.IDLE_TIMEOUT,15*1000);
                builder.setServerOption(UndertowOptions.REQUEST_PARSE_TIMEOUT,15*1000);
                builder.setServerOption(UndertowOptions.NO_REQUEST_TIMEOUT,5*1000);
                builder.setSocketOption(Options.READ_TIMEOUT,15*1000);
                builder.setSocketOption(Options.WRITE_TIMEOUT,15*1000);
                builder.setWorkerOption(Options.WORKER_TASK_CORE_THREADS,256);
                builder.setWorkerOption(Options.WORKER_TASK_KEEPALIVE,30*1000);
                builder.setWorkerOption(Options.COMPRESSION_TYPE , CompressionType.GZIP);
                builder.setWorkerOption(Options.COMPRESSION_LEVEL ,9);

                instance = builder.build();
            }
    }


    static {
        ApplicationPropertiesBase.initStaticFields(WebServer.class);
        try {
            rootFolder = new File(rootPath);
            rootFolderStr = rootFolder.getCanonicalPath();
            Log4j.info("本地文件根目录: "+ rootFolderStr);

            if (!rootFolder.exists() ){
                if (!rootFolder.mkdirs()) throw new IllegalStateException("启动失败,无效的主目录路径:"+rootFolder);
            }

            File temporaryFolder = new File(rootFolder.getParent() ,rootFolder.getName()+"_temporary");
            if (!temporaryFolder.exists() ){
                if (!temporaryFolder.mkdirs()) throw new IllegalStateException("启动失败,无效的临时目录路径:"+temporaryFolder);
            }

            FileUpLoad.setTemporaryFolder(1024*10,1024 * 1024 * 1024 * 5L,temporaryFolder);

            String dbStorePath = rootFolder.getParentFile().getCanonicalPath()
                            + File.separator + rootFolder.getName()+"_local.db";

            SQLiteUtils.initLoad(dbStorePath);

            loadPlug();
            loadUndertow();
            Log4j.info("************************************ APPLICATION 初始化完成 ************************************");
        } catch (Exception e) {
            throw new RuntimeException("文件服务初始化失败");
        }
    }


    public static void startWebServer() {
        if(instance != null){
            instance.start();
            startTime = System.currentTimeMillis();
        }
    }

    public static void stopWebServer(){
        if (instance != null){
            instance.stop();
            startTime = 0;
        }
    }

}
