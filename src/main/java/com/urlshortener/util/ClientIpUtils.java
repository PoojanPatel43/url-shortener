package com.urlshortener.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpUtils {

    private static final String[] PROXY_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP"
    };

    private ClientIpUtils() {}

    /**
     * Checks whether the request targets infrastructure paths (actuator, swagger, api-docs, favicon)
     * that should be excluded from authentication and rate limiting filters.
     */
    public static boolean isInfrastructurePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/actuator") ||
               path.startsWith("/api/swagger") ||
               path.startsWith("/api/api-docs") ||
               path.equals("/favicon.ico");
    }

    public static String getClientIp(HttpServletRequest request) {
        for (String header : PROXY_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
