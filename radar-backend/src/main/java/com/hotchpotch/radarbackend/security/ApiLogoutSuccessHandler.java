package com.hotchpotch.radarbackend.security;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * 将退出登录成功结果转换为项目统一的 JSON 响应。
 */
@Component
public class ApiLogoutSuccessHandler implements LogoutSuccessHandler {

    /**
     * 统一安全响应写入器。
     */
    private final SecurityJsonResponseWriter responseWriter;

    /**
     * 创建退出登录成功处理器。
     *
     * @param responseWriter 统一安全响应写入器
     */
    public ApiLogoutSuccessHandler(SecurityJsonResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        responseWriter.writeSuccess(response);
    }
}
