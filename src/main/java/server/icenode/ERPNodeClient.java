package server.icenode;

import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.util.Log4j;
import framework.client.IceClientUtils;
import server.undertow.WebServer;

@PropertiesFilePath("/web.properties")
public class ERPNodeClient {

    @PropertiesName("erp.node.inf.uploadMedia")
    private static String iceinf_uploadMedia ;
    static {
        ApplicationPropertiesBase.initStaticFields(ERPNodeClient.class);
    }

    public static void updateMediaIndex(String url){
        if (url.contains("/media/drug/")){
            String  tempstr  = url.substring(url.indexOf("/media/drug/")+"/media/drug/".length());
            String skuid = tempstr.substring(0,tempstr.indexOf("/"));
            if (iceinf_uploadMedia == null) return;
            String s = IceClientUtils.requestPro(iceinf_uploadMedia, "文件服务器", 0, 0, new String[]{skuid});
            Log4j.info("[ERP]上传文件修改媒体标识 : "+skuid+" >> "+ s);
        }
    }





}
