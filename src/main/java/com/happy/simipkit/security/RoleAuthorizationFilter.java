package com.happy.simipkit.security;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Membatasi endpoint tertentu hanya bisa diakses role "admin".
 * Filter ini jalan SETELAH AuthenticationFilter (urutan penting,
 * lihat mapping di web.xml) - jadi saat filter ini jalan, session
 * "authenticated" sudah pasti true.
 */
public class RoleAuthorizationFilter implements Filter {

    // Prefix path yang cuma boleh diakses admin
    private static final List<String> ADMIN_ONLY_PATHS = Arrays.asList(
            "/user-management",
            "/report-template-upload",
            "/reports/summary/delete",
            "/audit-log/delete-all",
            "/bank-sync-log/delete-all");

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String contextPath = httpRequest.getContextPath();
        String uri = httpRequest.getRequestURI();
        String path = uri.substring(contextPath.length());

        boolean isAdminOnlyPath = ADMIN_ONLY_PATHS.stream().anyMatch(path::startsWith);

        if (isAdminOnlyPath) {
            HttpSession session = httpRequest.getSession(false);
            String role = (session != null) ? (String) session.getAttribute("role") : null;

            if (!"admin".equalsIgnoreCase(role)) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Akses ditolak: memerlukan role admin");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}