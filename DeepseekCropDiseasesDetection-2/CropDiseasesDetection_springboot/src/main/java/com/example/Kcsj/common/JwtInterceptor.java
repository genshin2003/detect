package com.example.Kcsj.common;

import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果不是映射到方法，直接通过
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 获取请求头中的 Authorization (Token)
        String token = request.getHeader("Authorization");
        
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            response.getWriter().write("{\"code\": 401, \"msg\": \"No token provided\"}");
            return false;
        }

        // 验证 Token
        Claims claims = JwtUtils.parseToken(token);
        if (claims == null) {
            response.setStatus(401);
            response.getWriter().write("{\"code\": 401, \"msg\": \"Invalid or expired token\"}");
            return false;
        }

        // 将解析出来的用户信息存入 request，方便后续 Controller 使用
        request.setAttribute("userId", claims.get("userId"));
        request.setAttribute("username", claims.get("username"));
        request.setAttribute("role", claims.get("role"));

        return true;
    }
}
