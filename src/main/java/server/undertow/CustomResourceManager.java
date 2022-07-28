package server.undertow;
import bottle.util.Log4j;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.URLResource;
import server.hwobs.HWOBSAgent;

import java.nio.file.Paths;


public class CustomResourceManager extends PathResourceManager {

    public CustomResourceManager(String path) {
        super(Paths.get(path));
    }
    @Override
    public Resource getResource(String p) {
        Resource resource = null;
        try {
            if (p.equals("/favicon.ico")){
                resource = new URLResource( Thread.currentThread().getContextClassLoader().getResource("favicon.ico"),null);
            }
            if (resource == null){
                resource = super.getResource(p);
//                Log4j.info("获取本地资源: "+ p +" "+ resource);
                if (resource == null){
                    resource = HWOBSAgent.getResource(p);
                }
            }
        } catch (Exception e) {
            Log4j.error("",e);
        }

        return resource;
    }
}
