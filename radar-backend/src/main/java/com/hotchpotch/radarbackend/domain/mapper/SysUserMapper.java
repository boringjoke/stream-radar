package com.hotchpotch.radarbackend.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotchpotch.radarbackend.domain.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统用户数据访问 Mapper。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 按用户名查询用户。
     *
     * @param username 用户名
     * @return 用户实体，不存在时返回 null
     */
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 按邮箱查询用户。
     *
     * @param email 邮箱地址
     * @return 用户实体，不存在时返回 null
     */
    SysUser selectByEmail(@Param("email") String email);

    /**
     * 统计启用普通用户数量。
     *
     * @return 启用普通用户数量
     */
    long countEnabledNormalUsers();
}
