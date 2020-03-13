package server.prop;


import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.util.FileTool;

@PropertiesFilePath("/web.properties")
public class WebProperties {

    @PropertiesName("web.ip")
    public static String webIp = "127.0.0.1";
    @PropertiesName("web.port")
    public static int webPort = 80;
    @PropertiesName("web.file.directory")
    public static String rootPath = "./file_server_root";
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

    //临时文件目录
    public static String tempPath;

    static {
        ApplicationPropertiesBase.initStaticFields(WebProperties.class);
        if (!FileTool.checkDir(rootPath)){
            throw new IllegalStateException("启动失败,无效的主目录路径:"+rootPath);
        }

        tempPath = rootPath + FileTool.SEPARATOR+"temporary";

        if (!FileTool.checkDir(tempPath)){
            throw new IllegalStateException("启动失败,无效的主目录路径:"+tempPath);
        }
    }

}
