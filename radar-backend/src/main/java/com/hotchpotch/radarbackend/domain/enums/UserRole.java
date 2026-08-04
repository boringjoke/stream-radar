package com.hotchpotch.radarbackend.domain.enums;

/**
 * 系统用户角色。
 */
public enum UserRole {

    /**
     * 普通用户。
     */
    USER("USER"),

    /**
     * 管理员。
     */
    ADMIN("ADMIN");

    /**
     * 数据库存储编码。
     */
    private final String code;

    /**
     * 创建用户角色。
     *
     * @param code 数据库存储编码
     */
    UserRole(String code) {
        this.code = code;
    }

    /**
     * 获取数据库存储编码。
     *
     * @return 数据库存储编码
     */
    public String getCode() {
        return code;
    }
}
