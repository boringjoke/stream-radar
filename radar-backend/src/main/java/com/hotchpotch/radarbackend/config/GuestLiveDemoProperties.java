package com.hotchpotch.radarbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 游客首页真实演示主播配置。
 */
@ConfigurationProperties(prefix = "radar.live.guest-demo")
public class GuestLiveDemoProperties {

    /**
     * 游客首页 B站演示直播间地址。
     */
    private String bilibiliUrl = "https://live.bilibili.com/22637261";

    /**
     * 游客首页斗鱼演示直播间地址。
     */
    private String douyuUrl = "https://www.douyu.com/9999";

    /**
     * 游客首页虎牙演示直播间地址。
     */
    private String huyaUrl = "https://www.huya.com/998";

    /**
     * 游客首页抖音演示直播间地址。
     */
    private String douyinUrl = "https://live.douyin.com/690434662";

    public String getBilibiliUrl() {
        return bilibiliUrl;
    }

    public void setBilibiliUrl(String bilibiliUrl) {
        this.bilibiliUrl = bilibiliUrl;
    }

    public String getDouyuUrl() {
        return douyuUrl;
    }

    public void setDouyuUrl(String douyuUrl) {
        this.douyuUrl = douyuUrl;
    }

    public String getHuyaUrl() {
        return huyaUrl;
    }

    public void setHuyaUrl(String huyaUrl) {
        this.huyaUrl = huyaUrl;
    }

    public String getDouyinUrl() {
        return douyinUrl;
    }

    public void setDouyinUrl(String douyinUrl) {
        this.douyinUrl = douyinUrl;
    }
}
