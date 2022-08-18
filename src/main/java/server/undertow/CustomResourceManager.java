package server.undertow;
import bottle.util.Log4j;
import io.undertow.server.handlers.cache.LRUCache;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.URLResource;
import server.comm.RuntimeUtil;
import server.hwobs.HWOBSAgent;

import java.nio.file.Paths;

import static server.comm.RuntimeUtil.getJvmMaxMemory;


public class CustomResourceManager extends PathResourceManager {

    public static long max_cache_len = 15 * 1024 * 1024L;// 10M
    public static int max_cache_time = 15 * 60 ;// 15分钟

    private static LRUCache<String, Resource> cache = new LRUCache<>(  Integer.MAX_VALUE ,max_cache_time*1000);


    public static void resetCache(long len,int sec){
        if (len>0){
            max_cache_len = len;
        }

        if (sec>0){
            max_cache_time = sec;
        }

        if (cache!=null){
            cache.clear();
        }

        LRUCache<String, Resource> _cache = cache;
        cache = new LRUCache<>(  Integer.MAX_VALUE ,max_cache_time*1000);
        Log4j.info("重置资源管理器缓存:  "+ _cache + " ==>> "+ cache);
    }

    public static void removeCache(String path){
        if (cache!=null){
            cache.remove(path);
        }
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

        Resource resource = null;

        boolean isCache = true;
        if (cache!=null){
            resource = cache.get(p);
        }
        if (resource == null) {
            isCache = false;
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
            if (resource != null && cache!=null && resource.getContentLength() <  max_cache_len){
                cache.add(p,resource);
            }
        }

        if (resource != null ){
            Log4j.info("["+(isCache?"内存":"磁盘")+"] 获取资源: "+ p +" -> "+ resource.getUrl() +" 大小: "+ RuntimeUtil.byteLength2StringShow( resource.getContentLength() ));
        }

        return resource;
    }


}
