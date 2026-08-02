package com.hotchpotch.radarbackend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 统一业务错误码。
 */
public enum ErrorCode {

    /**
     * 请求参数错误。
     */
    PARAMETER_ERROR(40001, HttpStatus.BAD_REQUEST, "请求参数错误"),

    /**
     * 业务规则校验失败。
     */
    BUSINESS_ERROR(40000, HttpStatus.BAD_REQUEST, "业务处理失败"),

    /**
     * 当前请求尚未认证。
     */
    UNAUTHORIZED(40100, HttpStatus.UNAUTHORIZED, "未登录或登录已过期"),

    /**
     * 当前用户没有执行该操作的权限。
     */
    FORBIDDEN(40300, HttpStatus.FORBIDDEN, "无权执行此操作"),

    /**
     * 请求的资源不存在。
     */
    NOT_FOUND(40400, HttpStatus.NOT_FOUND, "请求的资源不存在"),

    /**
     * 服务端未预期的系统错误。
     */
    INTERNAL_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");

    /**
     * 返回给前端的业务错误码。
     */
    private final int code;

    /**
     * 对应的 HTTP 状态码。
     */
    private final HttpStatus httpStatus;

    /**
     * 默认错误说明。
     */
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
