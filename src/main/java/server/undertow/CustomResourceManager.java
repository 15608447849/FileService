package server.undertow;
import bottle.util.Log4j;
import io.undertow.server.handlers.cache.LRUCache;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.URLResource;
import server.hwobs.HWOBSAgent;

import java.nio.file.Paths;


public class CustomResourceManager extends PathResourceManager {

    private static final long MAX_CACHE_LEN = 5*1024*1024L;// 5M
    private static final int MAX_CACHE_TIME = 3 * 60 ;// 3分钟
    private final static LRUCache<String, Resource> cache = new LRUCache<>(1000,MAX_CACHE_TIME*1000);
    public static void removeCache(String path){
        cache.remove(path);
    }

    CustomResourceManager(String path) {
        super(Paths.get(path));
        Log4j.info("创建资源管理器: "+ Paths.get(path).toUri());
    }

    @Override
    public Resource getResource(String p) {
        if (Thread.currentThread() instanceof org.xnio.XnioExecutor){
            return null;
        }

        Resource resource = cache.get(p);
        if (resource != null) return resource;


        try {
            if (p.equals("/favicon.ico")){
                resource = new URLResource( Thread.currentThread().getContextClassLoader().getResource("favicon.ico"),null);
            }
            if (resource == null){
                resource = super.getResource(p);
                if (resource == null){
                    resource = HWOBSAgent.getResource(p);
                }
            }
        } catch (Exception e) {
            Log4j.error("",e);
        }

        Log4j.info("获取资源: "+ p +" -> "+ (  resource == null? "暂无资源": resource.getUrl() ));

        if (resource != null && resource.getContentLength() <  MAX_CACHE_LEN){
            cache.add(p,resource);
        }


        return resource;
    }
}
