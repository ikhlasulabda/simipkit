package com.happy.simipkit.security;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Filter otentikasi global.
 * Memeriksa flag session "authenticated" untuk semua request kecuali
 * path /login dan static resources (/resources/**).
 * Juga menambahkan HTTP response header keamanan standar (nosniff, DENY).
 */
public class AuthenticationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Security headers
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");

        String contextPath = httpRequest.getContextPath();
        String uri = httpRequest.getRequestURI();
        String path = uri.substring(contextPath.length());

        // CORS headers khusus untuk endpoint integrasi partner (bisa diakses dari device/domain lain)
        if (path.startsWith("/api/sync/")) {
            httpResponse.setHeader("Access-Control-Allow-Origin", "*");
            httpResponse.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
            httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type");

            // Browser kirim OPTIONS dulu (preflight) sebelum POST asli, harus dijawab 200 langsung
            if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
                httpResponse.setStatus(HttpServletResponse.SC_OK);
                return;
            }
        }

        // Exception paths
        if (path.equals("/login") || path.startsWith("/resources/") || path.startsWith("/api/sync/")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        boolean authenticated = (session != null && Boolean.TRUE.equals(session.getAttribute("authenticated")));

        if (!authenticated) {
            httpResponse.sendRedirect(contextPath + "/login");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
