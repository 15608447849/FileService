package server.servlet.iface;

import bottle.util.Log4j;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @Author: leeping
 * @Date: 2020/3/10 14:03
 */
public class AccessControlAllowOriginFilter implements javax.servlet.Filter{

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse resp = (HttpServletResponse) response;
        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Access-Control-Allow-Methods", "GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS");
        resp.addHeader("Access-Control-Allow-Headers", "x-requested-with");
        chain.doFilter(request, resp);
    }

    @Override
    public void destroy() {

    }
}
