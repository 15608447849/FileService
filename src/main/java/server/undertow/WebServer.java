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
import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.server.handlers.resource.CachingResourceManager;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import org.xnio.*;
import org.xnio.ssl.JsseXnioSsl;
import server.LunchServer;
import server.servlet.imps.*;

import javax.net.ssl.*;
import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


import static io.undertow.servlet.Servlets.servlet;

@PropertiesFilePath("/web.properties")
public class WebServer {

    private static String webHost = "0.0.0.0";
    @PropertiesName("web.port")
    private static int webPort = 80;

    @PropertiesName("web.domain")
    public static String domain = null;
    @PropertiesName("web.file.directory")
    private static String rootPath = "./file_server_root";

    @PropertiesName("web.ssl.key.keystore")
    private static String keystorePath = "./CA/certs/client.keystore";
    @PropertiesName("web.ssl.key.password")
    private static String keystorePassword ;
    @PropertiesName("web.ssl.trust.keystore")
    private static String trustKeystorePath = "./CA/certs/ca-trust.keystore";

    @PropertiesName("http.header.referer")
    private static String http_head_referer = null;


    //根目录文件夹
    public static File rootFolder ;
    public static String rootFolderStr;

    public static long startTime;
    private static Undertow instance;

    private static void initRootDir() throws Exception{
        rootFolder = new File(rootPath);
        rootFolderStr = rootFolder.getCanonicalPath();

        if (!rootFolder.exists() ){
            if (!rootFolder.mkdirs()) throw new IllegalStateException("启动失败,无效的主目录路径:"+rootFolder);
        }

        File temporaryFolder = new File(rootFolder.getParent() ,"file_server_temporary");
        if (!temporaryFolder.exists() ){
            if (!temporaryFolder.mkdirs()) throw new IllegalStateException("启动失败,无效的临时目录路径:"+temporaryFolder);
        }

        // 设置临时文件
        FileUpLoad.setTemporaryFolder(1024*10,1024 * 1024 * 1024 * 5L,temporaryFolder);

        Log4j.info("本地文件根目录: "+ rootFolderStr
                +" 本地文件临时目录: "+ temporaryFolder.getCanonicalPath());
    }

    //临时文件目录
    public static String GET_TEMP_FILE_DIR(){
        return rootFolderStr + File.separator+ "temp" ;
    }

    private static void loadUndertow() throws Exception{
            if (instance==null){

                SSLContext sslContext = initSSL();

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
//                        .setResourceManager(
//                                new CachingResourceManager(
//                                        1000,1024*1024*5,new DirectBufferCache(100,100,1024*1024*5),
//                                        new CustomResourceManager(rootPath),10
//                                )
//                        )
                        .setResourceManager(
                                new CustomResourceManager(rootPath)
                        )
                        ;

                // 添加servlet
                loadServlet(servletBuilder);

                // 构建http处理器
                DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
                manager.deploy();
                HttpHandler httpHandler = manager.start();

                Undertow.Builder builder = Undertow.builder();
                // 连接访问参数配置
                builder.setIoThreads(64);
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

                if (sslContext == null){
                    builder.addHttpListener(WebServer.webPort,webHost,httpHandler);
                }else {
                    builder.addHttpsListener(WebServer.webPort,webHost,sslContext,httpHandler);
                }

                // 构建实例
                instance = builder.build();
                if (domain==null) domain = "http://" + webHost + ":" + webPort;
                Log4j.info("尝试监听本地地址:  " + domain );
            }
    }

    /*
    * jks->p12
    * keytool -importkeystore -srckeystore sslkey.jks -srcstoretype JKS -deststoretype PKCS12 -destkeystore sslkey.p12
    * p12->jks
    * keytool -importkeystore -srckeystore keystore.p12 -srcstoretype PKCS12 -deststoretype JKS -destkeystore keystore.jks
    * */
    private static SSLContext initSSL() {
        try {

            File keystoreFile = new File(keystorePath);
            if (!keystoreFile.exists() || keystoreFile.length()<=0)  return null;
            File trustKeystoreFile = new File(trustKeystorePath);
            if (!trustKeystoreFile.exists() || trustKeystoreFile.length()<=0) return null;
            if (keystorePassword == null) return null;
            Log4j.info("创建SSL: " + keystorePath +"\t"+trustKeystorePath+"\t"+keystorePassword);

            //客户端证书库
            KeyStore clientKeystore = KeyStore.getInstance("pkcs12");
            FileInputStream keystoreFis = new FileInputStream(keystoreFile);
            clientKeystore.load(keystoreFis, keystorePassword.toCharArray());

            //密钥库
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("sunx509");
            kmf.init(clientKeystore, keystorePassword.toCharArray());

            //信任证书库
            KeyStore trustKeystore = KeyStore.getInstance("jks");
            FileInputStream trustKeystoreFis = new FileInputStream(trustKeystoreFile);
            trustKeystore.load(trustKeystoreFis, keystorePassword.toCharArray());

            //信任库
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("sunx509");
            tmf.init(trustKeystore);

            //初始化SSL上下文
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            Log4j.info("创建 SSL context : "+ sslContext.toString());

            return sslContext;
        } catch (Exception e) {
           Log4j.error("初始化SSL失败",e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static void loadServlet(DeploymentInfo servletBuilder) {
        String packageName = "server.servlet.imps";
        // 读取执行路径下的类
        Set<Class<?>> classes = DynamicLoadClassUtil.scanCurrentAllClassBySpecPath(packageName,javax.servlet.http.HttpServlet.class);
        for (Class<?> cls: classes){
            try{
                if (Servlet.class.isAssignableFrom(cls)){
                    ServletAnnotation annotation = cls.getAnnotation(ServletAnnotation.class);
                    if (annotation == null) continue;

                    servletBuilder.addServlet(servlet(annotation.name(),  (Class<? extends Servlet>) cls).addMapping(annotation.path()));
                    Log4j.info("添加servlet\t"+cls +"\t"+ annotation.name() +"\t"+ "http://"+webHost+":"+webPort + annotation.path()  );
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

    public static void initWebServer(int port){
        if(port>0 && webPort!=port) {

            try {
                webPort = port;
                Log4j.info("更改web端口 "+ webPort);
                stopWebServer();
                loadUndertow();
            } catch (Exception e) {
                throw new RuntimeException();
            }
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
            instance = null;
            startTime = 0;
        }
    }

    public static Map<String, String> getCommHeader() {
        Map<String,String> header = new HashMap<String,String>(){
            {
                put("Referer",http_head_referer==null?"":http_head_referer);
            }
        };
        return header;
    }

    public static void setCommHeader(URLConnection connection) {
        if (http_head_referer!=null){
            connection.setRequestProperty("Referer",http_head_referer);
        }
    }
}
