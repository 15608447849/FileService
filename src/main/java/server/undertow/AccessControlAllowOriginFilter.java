package server.undertow;

import bottle.util.Log4j;
import bottle.util.TimeTool;
import io.undertow.servlet.spec.HttpServletRequestImpl;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: leeping
 * @Date: 2020/3/10 14:03
 * 跨域说明:  http://www.ruanyifeng.com/blog/2016/04/cors.html
 */
public class AccessControlAllowOriginFilter implements javax.servlet.Filter{

    public static boolean isPrintAccess;
    public static final Map<Thread, List<String>> accessRequestMap = new ConcurrentHashMap<>();
    public static final Map<String, Long> accessRequestPathMap = new ConcurrentHashMap<>();

    public static long accessCount;
    public static int accessCountCollect;

    static {
        setTimerClearMap();
    }

    // 固定时间点清理Map
    private static void setTimerClearMap() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0); //凌晨0点
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date date=calendar.getTime(); //第一次执行定时任务的时间

        if (date.before(new Date())) {
            //  第一次执行定时任务的时间加一天
            Calendar startDT = Calendar.getInstance();
            startDT.setTime(date);
            startDT.add(Calendar.DAY_OF_MONTH, 1);
            date = startDT.getTime();
        }

        new Timer(true).schedule(new TimerTask() {
            @Override
            public void run() {
                for (Thread thread : accessRequestMap.keySet()){
                    List<String> values = accessRequestMap.get(thread);
                    values.clear();
                }
                accessRequestPathMap.clear();
            }
        },date,24 * 60 * 60 * 1000L);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        //Log4j.info(" AccessControlAllowOriginFilter init "+ filterConfig.getServletContext());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (accessCount>=Long.MAX_VALUE) {
            accessCountCollect ++;
            accessCount=0;
        }
        accessCount++;


        StringBuilder sb = new StringBuilder();
        sb.append( request.getRemoteAddr()).append(":");
        sb.append( request.getRemotePort()).append(" >> ");


        if (isPrintAccess) Log4j.info( " 接入访问: " + request );

        if (request instanceof HttpServletRequestImpl){
            HttpServletRequestImpl imp = (HttpServletRequestImpl) request;
            sb.append(imp.getMethod() ).append(" ");
            sb.append(imp.getRequestURI()).append("\n\t");
            Enumeration<String> header = imp.getHeaderNames();
            while (header.hasMoreElements()){
                String headerStr = header.nextElement();
                sb.append(headerStr).append("=").append(imp.getHeader(headerStr)).append("\t");
            }

            String time = TimeTool.date_yMd_Hms_2Str(new Date());

            String str =
                    " TIME: "+ time +
                    " HOST: "+imp.getRemoteHost()+":"+imp.getRemotePort() +
                    " METHOD: "+ imp.getMethod() +
                    " PATH: "+ imp.getRequestURI();

            List<String> list = accessRequestMap.computeIfAbsent(Thread.currentThread(), key -> new ArrayList<>());
            list.add(str);
            if (list.size()>=Integer.MAX_VALUE){
                list.clear();
            }

            String absPath = imp.getRequestURL().toString();
            Long  count = accessRequestPathMap.getOrDefault(absPath,0L);
            accessRequestPathMap.put(absPath,++count);

        }

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        try {
            req.setCharacterEncoding("UTF-8");
            resp.setCharacterEncoding("UTF-8");

            resp.addHeader("Access-Control-Allow-Origin", "*");
            resp.addHeader("Access-Control-Allow-Methods", "GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS");
            resp.addHeader("Access-Control-Allow-Headers", "x-requested-with");

            resp.addHeader("Access-Control-Allow-Headers",
                    "specify-path,specify-filename,save-md5,is-sync,tailor-list," +
                            "path-list,excel-path,ergodic-sub,"+
                            "delete-list,image-compress,image-logo,image-size-limit,image-spec-suffix-limit,image-compress-size,image-min-exist,"+
                            "delete-time,"+
                            "image-base64," +
                            "image-pix-color");

            chain.doFilter(req, resp);
        } catch (Exception e) {
            Log4j.error("服务错误",e);
        }
    }

    @Override
    public void destroy() {

    }
}
