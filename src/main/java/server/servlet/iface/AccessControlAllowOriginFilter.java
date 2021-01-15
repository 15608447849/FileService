package server.servlet.iface;

import bottle.util.Log4j;
import io.undertow.servlet.spec.HttpServletRequestImpl;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * @Author: leeping
 * @Date: 2020/3/10 14:03
 * 跨域说明:  http://www.ruanyifeng.com/blog/2016/04/cors.html
 */
public class AccessControlAllowOriginFilter implements javax.servlet.Filter{

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        StringBuffer sb = new StringBuffer();

        sb.append( request.getRemoteAddr()).append(":");
        sb.append( request.getRemotePort()).append(" >> ");

        if (request instanceof HttpServletRequestImpl){
            HttpServletRequestImpl imp = (HttpServletRequestImpl) request;
            sb.append(imp.getMethod() ).append(",");
            sb.append(imp.getRequestURI() );
        }else{
            sb.append(request);
        }


        Log4j.info(Thread.currentThread()+ " 接入访问: " + sb);

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
                            "image-base64,image-pix-color");

            chain.doFilter(req, resp);
        } catch (UnsupportedEncodingException e) {
            Log4j.error(Thread.currentThread() +" 文件服务错误",e);
        }
    }

    @Override
    public void destroy() {

    }
}
