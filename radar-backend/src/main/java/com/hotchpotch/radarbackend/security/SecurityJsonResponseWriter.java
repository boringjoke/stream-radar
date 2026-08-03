package com.hotchpotch.radarbackend.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.hotchpotch.radarbackend.common.exception.ErrorCode;
import com.hotchpotch.radarbackend.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 将 Spring Security 过滤器阶段的结果写成统一 JSON 响应。
 */
@Component
public class SecurityJsonResponseWriter {

    /**
     * JSON 序列化器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建安全响应写入器。
     *
     * @param objectMapper JSON 序列化器
     */
    public SecurityJsonResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 写入统一错误响应。
     *
     * @param response 当前 HTTP 响应
     * @param errorCode 统一错误码
     * @param message 对外错误说明
     * @throws IOException 响应写入失败
     */
    public void writeError(HttpServletResponse response, ErrorCode errorCode, String message)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }

        prepareResponse(response, errorCode.getHttpStatus());
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.of(errorCode.getCode(), message, null));
    }

    /**
     * 写入统一成功响应。
     *
     * @param response 当前 HTTP 响应
     * @throws IOException 响应写入失败
     */
    public void writeSuccess(HttpServletResponse response) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        prepareResponse(response, HttpStatus.OK);
        objectMapper.writeValue(response.getWriter(), ApiResponse.success());
    }

    private void prepareResponse(HttpServletResponse response, HttpStatus status) {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }
}
