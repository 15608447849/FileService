package server.undertow;

import bottle.util.Log4j;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import server.undertow.WebServer;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: leeping
 * @Date: 2020/3/10 14:03
 * 跨域说明:  http://www.ruanyifeng.com/blog/2016/04/cors.html
 */
public class AccessControlAllowOriginFilter implements javax.servlet.Filter{

    public static final Map<Thread,String> lastAccessRequestMap = new ConcurrentHashMap<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        //Log4j.info(" AccessControlAllowOriginFilter init "+ filterConfig.getServletContext());
    }


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {



        StringBuilder sb = new StringBuilder();
        sb.append( request.getRemoteAddr()).append(":");
        sb.append( request.getRemotePort()).append(" >> ");


        Log4j.debug( " 接入访问: " + request);

        if (request instanceof HttpServletRequestImpl){
            HttpServletRequestImpl imp = (HttpServletRequestImpl) request;
            sb.append(imp.getMethod() ).append(" ");
            sb.append(imp.getRequestURI()).append("\n\t");
            Enumeration<String> header = imp.getHeaderNames();
            while (header.hasMoreElements()){
                String headerStr = header.nextElement();
                sb.append(headerStr).append("=").append(imp.getHeader(headerStr)).append("\t");
            }

            String time = Log4j.sdf.format(new Date());
            lastAccessRequestMap.put(Thread.currentThread(),time + " 访问信息\t" + sb );
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
