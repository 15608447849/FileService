package server.undertow;

import bottle.objectref.DynamicLoadClassUtil;
import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.util.FileTool;
import bottle.util.Log4j;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import org.xnio.CompressionType;
import org.xnio.Options;
import server.LunchServer;
import server.servlet.imps.*;

import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import java.io.File;
import java.util.Set;


import static io.undertow.servlet.Servlets.servlet;

@PropertiesFilePath("/web.properties")
public class WebServer {
    private static String webHost = "0.0.0.0";
    @PropertiesName("web.port")
    private static int webPort = 80;
    @PropertiesName("web.domain")
    public static String domain = "http://127.0.0.1:80";
    @PropertiesName("web.file.directory")
    private static String rootPath = "./file_server_root";
    //根目录文件夹
    public static File rootFolder ;
    public static String rootFolderStr;

    public static long startTime;
    private static Undertow instance;

    private static void initRootDir() throws Exception{
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

        // 设置临时文件
        FileUpLoad.setTemporaryFolder(1024*10,1024 * 1024 * 1024 * 5L,temporaryFolder);
    }

    //临时文件目录
    public static String GET_TEMP_FILE_DIR(){
        return rootFolderStr + FileTool.SEPARATOR+ "temp" ;
    }

    private static void loadUndertow() throws Exception{
            if (instance==null){
                //开启web文件服务器
                DeploymentInfo servletBuilder = Servlets.deployment()
                        .setClassLoader(LunchServer.class.getClassLoader())
                        .setContextPath("/")
                        .setDeploymentName("file_server.war")
                        .addFilter( new FilterInfo("跨域过滤", AccessControlAllowOriginFilter.class) )
                        .addFilterUrlMapping("跨域过滤","/*", DispatcherType.REQUEST)

//                        .setResourceManager(
//                                new PathResourceManager(Paths.get(WebServer.rootPath), 16*4069L)
//                        )
                        .setResourceManager(
                                new CustomResourceManager(rootPath)
                        )
                        ;

                // 添加servlet
                loadServlet(servletBuilder);

                DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);

                manager.deploy();

                HttpHandler httpHandler = manager.start();

                Undertow.Builder builder = Undertow.builder();

                builder.addHttpListener(WebServer.webPort,webHost,httpHandler);

                // 连接访问参数配置

                builder.setIoThreads(16);
                builder.setWorkerThreads(256);

//                builder.setDirectBuffers(true);
//                builder.setBufferSize(1024 * 1024 * 1024);

//                builder.setServerOption(UndertowOptions.IDLE_TIMEOUT,60*60*1000);

                builder.setServerOption(UndertowOptions.REQUEST_PARSE_TIMEOUT,60 * 60 * 1000);
                builder.setServerOption(UndertowOptions.NO_REQUEST_TIMEOUT, 60 * 1000);

                builder.setSocketOption(Options.READ_TIMEOUT,6 * 60 * 1000);
                builder.setSocketOption(Options.WRITE_TIMEOUT,6 * 60 * 1000);

                builder.setWorkerOption(Options.WORKER_TASK_CORE_THREADS,256);
                builder.setWorkerOption(Options.WORKER_TASK_KEEPALIVE,60 * 1000);

                builder.setWorkerOption(Options.COMPRESSION_TYPE , CompressionType.GZIP);
                builder.setWorkerOption(Options.COMPRESSION_LEVEL ,9);
                instance = builder.build();
            }
    }

    @SuppressWarnings("unchecked")
    private static void loadServlet(DeploymentInfo servletBuilder) {
        String packageName = "server.servlet.imps";
//        System.out.println(packageName);
        // 读取执行路径下的类
        Set<Class<?>> classes = DynamicLoadClassUtil.scanCurrentAllClassBySpecPath(packageName,javax.servlet.http.HttpServlet.class);
        for (Class<?> cls: classes){
            try{
                if (Servlet.class.isAssignableFrom(cls)){
                    ServletAnnotation annotation = cls.getAnnotation(ServletAnnotation.class);
                    if (annotation == null) continue;

                    servletBuilder.addServlet(servlet(annotation.name(),  (Class<? extends Servlet>) cls).addMapping(annotation.path()));
                    Log4j.info("添加servlet\t"+cls +"\t"+ annotation.name() +"\t"+domain+annotation.path()  );
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    static {
        ApplicationPropertiesBase.initStaticFields(WebServer.class);
        try {
            initRootDir();
            loadUndertow();
        } catch (Exception e) {
            throw new RuntimeException("文件服务初始化失败");
        }
    }

    public static void startWebServer() {
        if(instance != null){
            instance.start();
            startTime = System.currentTimeMillis();
            Log4j.info("监听本地地址:  " + webHost + " " + webPort );
        }
    }

    public static void stopWebServer(){
        if (instance != null){
            instance.stop();
            startTime = 0;
        }
    }

}
