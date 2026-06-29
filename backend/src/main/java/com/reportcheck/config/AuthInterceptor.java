package com.reportcheck.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportcheck.common.AuthContext;
import com.reportcheck.common.AuthInfo;
import com.reportcheck.common.Result;
import com.reportcheck.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Set;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/") || path.startsWith("/api/health")) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已过期");
            return false;
        }

        try {
            AuthInfo authInfo = jwtUtil.parseToken(authorization.substring(7));
            AuthContext.set(authInfo);
            if (!isAllowed(authInfo.getRoleCode(), request.getMethod(), path)) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "当前角色无权访问该功能");
                return false;
            }
            return true;
        } catch (Exception exception) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "令牌无效或已过期");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private boolean isAllowed(String roleCode, String method, String path) {
        if ("ADMIN".equals(roleCode)) {
            return isAdminAllowed(method, path);
        }
        if ("TEACHER".equals(roleCode)) {
            return isTeacherAllowed(method, path);
        }
        if ("STUDENT".equals(roleCode)) {
            return isStudentAllowed(method, path);
        }
        return false;
    }

    private boolean isAdminAllowed(String method, String path) {
        if (path.startsWith("/api/statistics")) return true;
        if (path.startsWith("/api/users")) return true;
        if (path.startsWith("/api/courses")) return true;
        if (path.startsWith("/api/classes")) return true;
        if (path.startsWith("/api/student-classes")) return true;
        if (path.startsWith("/api/experiment-tasks")) return "GET".equals(method);
        if (path.startsWith("/api/reports")) return "GET".equals(method);
        if (path.startsWith("/api/check-tasks")) return "GET".equals(method);
        if (path.startsWith("/api/check-results")) return "GET".equals(method);
        return false;
    }

    private boolean isTeacherAllowed(String method, String path) {
        if (path.startsWith("/api/statistics")) return true;
        if (path.startsWith("/api/users")) return "GET".equals(method);
        if (path.startsWith("/api/courses")) return "GET".equals(method);
        if (path.startsWith("/api/classes")) return "GET".equals(method);
        if (path.startsWith("/api/student-classes")) return "GET".equals(method);
        if (path.startsWith("/api/experiment-tasks")) return true;
        if (path.startsWith("/api/reports")) return true;
        if (path.startsWith("/api/check-tasks")) return true;
        if (path.startsWith("/api/check-results")) return "GET".equals(method);
        return false;
    }

    private boolean isStudentAllowed(String method, String path) {
        if (path.startsWith("/api/courses")) return "GET".equals(method);
        if (path.startsWith("/api/classes")) return "GET".equals(method);
        if (path.startsWith("/api/experiment-tasks")) return "GET".equals(method);
        if (path.startsWith("/api/reports")) return Set.of("GET", "POST").contains(method);
        return false;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(status, message)));
    }
}
