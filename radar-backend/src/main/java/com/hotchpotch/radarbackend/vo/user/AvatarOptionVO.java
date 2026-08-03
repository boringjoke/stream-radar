package com.hotchpotch.radarbackend.vo.user;

/**
 * 预置头像选项。
 */
public class AvatarOptionVO {

    /**
     * 项目内头像静态资源路径。
     */
    private final String path;

    /**
     * 头像展示名称。
     */
    private final String name;

    public AvatarOptionVO(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }
}
