package server.undertow;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.Resource;
import server.hwobs.HWOBSUpload;
import java.nio.file.Paths;


public class CustomResourceManager extends PathResourceManager {

    public CustomResourceManager(String path) {
        super(Paths.get(path));
    }
    @Override
    public Resource getResource(String p) {
//        Log4j.info("获取本地资源: "+ p);
        Resource resource = null;
        try {
            resource = super.getResource(p);
            if (resource == null){
                return HWOBSUpload.getResource(p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resource;
    }
}
