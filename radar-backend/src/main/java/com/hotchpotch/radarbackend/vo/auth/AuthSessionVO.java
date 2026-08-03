package com.hotchpotch.radarbackend.vo.auth;

/**
 * 认证 Session 查询结果。
 */
public class AuthSessionVO {

    /**
     * 当前请求是否已经认证。
     */
    private final boolean authenticated;

    /**
     * 当前登录用户摘要，未认证时为 null。
     */
    private final SessionUserVO user;

    public AuthSessionVO(boolean authenticated, SessionUserVO user) {
        this.authenticated = authenticated;
        this.user = user;
    }

    /**
     * 创建未认证结果。
     *
     * @return 未认证 Session 结果
     */
    public static AuthSessionVO unauthenticated() {
        return new AuthSessionVO(false, null);
    }

    /**
     * 创建已认证结果。
     *
     * @param user 当前登录用户摘要
     * @return 已认证 Session 结果
     */
    public static AuthSessionVO authenticated(SessionUserVO user) {
        return new AuthSessionVO(true, user);
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public SessionUserVO getUser() {
        return user;
    }
}
