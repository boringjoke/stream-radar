package com.hotchpotch.radarbackend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * StreamRadar 安全相关配置。
 */
@ConfigurationProperties(prefix = "radar.security")
public class RadarSecurityProperties {

    /**
     * CSRF 配置。
     */
    private final Csrf csrf = new Csrf();

    public Csrf getCsrf() {
        return csrf;
    }

    /**
     * CSRF Cookie 配置。
     */
    public static class Csrf {

        /**
         * CSRF Cookie 是否启用 Secure 属性。
         */
        private boolean cookieSecure;

        /**
         * CSRF Cookie 的 SameSite 属性。
         */
        private String cookieSameSite = "lax";

        public boolean isCookieSecure() {
            return cookieSecure;
        }

        public void setCookieSecure(boolean cookieSecure) {
            this.cookieSecure = cookieSecure;
        }

        public String getCookieSameSite() {
            return cookieSameSite;
        }

        public void setCookieSameSite(String cookieSameSite) {
            this.cookieSameSite = cookieSameSite;
        }
    }
}
