package com.hotchpotch.radarbackend.request.live;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 关注直播间请求。
 */
public class LiveFollowRequest {

    /**
     * 用户输入的直播间链接，由服务端负责平台和房间标识解析。
     */
    @NotBlank(message = "直播间链接不能为空")
    @Size(max = 2048, message = "直播间链接不能超过 2048 个字符")
    private String roomUrl;

    public String getRoomUrl() {
        return roomUrl;
    }

    public void setRoomUrl(String roomUrl) {
        this.roomUrl = roomUrl;
    }
}
