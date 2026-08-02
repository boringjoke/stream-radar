package com.hotchpotch.radarbackend.common.response;

/**
 * 单个请求参数校验错误。
 */
public final class ValidationError {

    /**
     * 发生校验错误的字段或参数名称。
     */
    private final String field;

    /**
     * 面向调用方的校验错误说明。
     */
    private final String message;

    public ValidationError(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public String getField() {
        return field;
    }

    public String getMessage() {
        return message;
    }
}
