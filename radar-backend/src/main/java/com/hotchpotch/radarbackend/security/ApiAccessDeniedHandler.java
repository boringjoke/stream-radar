package com.hotchpotch.radarbackend.security;

import java.io.IOException;

import com.hotchpotch.radarbackend.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

/**
 * 将无权限和 CSRF 校验失败转换为项目统一的 JSON 响应。
 */
@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * 统一安全响应写入器。
     */
    private final SecurityJsonResponseWriter responseWriter;

    /**
     * 创建无权限处理器。
     *
     * @param responseWriter 统一安全响应写入器
     */
    public ApiAccessDeniedHandler(SecurityJsonResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        String message = accessDeniedException instanceof CsrfException
                ? "CSRF 校验失败"
                : ErrorCode.FORBIDDEN.getMessage();
        responseWriter.writeError(response, ErrorCode.FORBIDDEN, message);
    }
}
