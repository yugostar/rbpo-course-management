package ru.mtuci.coursemanagement.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // allow public
        if (path.equals("/login") || path.equals("/logout") || path.startsWith("/css") || path.startsWith("/js")) {
            return true;
        }

        // allow H2 console only locally (optional)
        if (path.startsWith("/h2-console")) {
            return true;
        }

        // protect app pages + api
        if (path.startsWith("/courses") || path.startsWith("/students") || path.startsWith("/api/")) {
            HttpSession s = request.getSession(false);
            if (s == null || s.getAttribute("username") == null) {
                response.sendRedirect("/login");
                return false;
            }
        }

        return true;
    }
}
