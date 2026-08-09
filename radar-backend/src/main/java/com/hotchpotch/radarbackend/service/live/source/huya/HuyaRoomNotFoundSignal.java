package com.hotchpotch.radarbackend.service.live.source.huya;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * 虎牙房间不存在的结构化信号判断。
 */
public final class HuyaRoomNotFoundSignal {

    /**
     * 虎牙错误页使用的查询参数名称。
     */
    private static final String ERROR_TYPE_PARAMETER = "errorType";

    /**
     * 虎牙房间不存在的错误类型。
     */
    private static final String ROOM_NOT_FOUND_ERROR_TYPE = "ROOM_NOT_FOUND";

    private HuyaRoomNotFoundSignal() {
    }

    /**
     * 判断重定向响应是否明确表示房间不存在。
     *
     * @param statusCode HTTP 状态码
     * @param location 重定向地址
     * @return 是否为虎牙房间不存在信号
     */
    public static boolean isRoomNotFoundRedirect(int statusCode, String location) {
        if (statusCode < 300 || statusCode >= 400 || isBlank(location)) {
            return false;
        }
        try {
            String errorType = UriComponentsBuilder.fromUriString(location.trim())
                    .build()
                    .getQueryParams()
                    .getFirst(ERROR_TYPE_PARAMETER);
            return ROOM_NOT_FOUND_ERROR_TYPE.equalsIgnoreCase(errorType);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * 判断文本是否为空。
     *
     * @param value 待判断文本
     * @return 是否为空
     */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
