package com.hotchpotch.radarbackend.security;

import java.io.IOException;

import com.hotchpotch.radarbackend.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 将未认证请求转换为项目统一的 JSON 响应。
 */
@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * 统一安全响应写入器。
     */
    private final SecurityJsonResponseWriter responseWriter;

    /**
     * 创建未认证处理器。
     *
     * @param responseWriter 统一安全响应写入器
     */
    public ApiAuthenticationEntryPoint(SecurityJsonResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        responseWriter.writeError(
                response,
                ErrorCode.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED.getMessage());
    }
}
