package com.hotchpotch.radarbackend.common.response;

/**
 * JSON API 统一响应结构。
 *
 * @param <T> 响应数据类型
 */
public final class ApiResponse<T> {

    /**
     * 业务响应码，0 表示成功，非 0 表示失败。
     */
    private final int code;

    /**
     * 面向调用方的响应说明。
     */
    private final String message;

    /**
     * 响应业务数据，无数据时为 null。
     */
    private final T data;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建一个自定义响应。
     *
     * @param code 业务响应码
     * @param message 响应说明
     * @param data 响应业务数据
     * @param <T> 响应数据类型
     * @return 统一响应对象
     */
    public static <T> ApiResponse<T> of(int code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }

    /**
     * 创建成功响应。
     *
     * @param data 响应业务数据
     * @param <T> 响应数据类型
     * @return 成功响应对象
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "操作成功", data);
    }

    /**
     * 创建无业务数据的成功响应。
     *
     * @return 成功响应对象
     */
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(0, "操作成功", null);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
