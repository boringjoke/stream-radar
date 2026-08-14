package com.hotchpotch.radarbackend.domain.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.hotchpotch.radarbackend.domain.entity.SysUser;
import com.hotchpotch.radarbackend.domain.mapper.SysUserMapper;

/**
 * 系统用户数据访问仓库。
 */
@Repository
public class SysUserRepository {

    /**
     * 系统用户 Mapper。
     */
    private final SysUserMapper mapper;

    /**
     * 创建系统用户数据访问仓库。
     *
     * @param mapper 系统用户 Mapper
     */
    public SysUserRepository(SysUserMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按主键查询用户。
     *
     * @param id 用户主键
     * @return 用户实体，用户不存在时返回空 Optional
     */
    public Optional<SysUser> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    /**
     * 按用户名查询用户。
     *
     * @param username 用户名
     * @return 用户实体，用户不存在时返回空 Optional
     */
    public Optional<SysUser> findByUsername(String username) {
        return Optional.ofNullable(mapper.selectByUsername(username));
    }

    /**
     * 按邮箱查询用户。
     *
     * @param email 邮箱地址
     * @return 用户实体，用户不存在时返回空 Optional
     */
    public Optional<SysUser> findByEmail(String email) {
        return Optional.ofNullable(mapper.selectByEmail(email));
    }

    /**
     * 统计启用普通用户数量。
     *
     * @return 启用普通用户数量
     */
    public long countEnabledNormalUsers() {
        return mapper.countEnabledNormalUsers();
    }

    /**
     * 新增用户。
     *
     * @param entity 待新增的用户实体
     * @return 受影响的记录数
     */
    public int insert(SysUser entity) {
        return mapper.insert(entity);
    }

    /**
     * 按主键更新用户。
     *
     * @param entity 待更新的用户实体，必须包含主键
     * @return 受影响的记录数
     */
    public int updateById(SysUser entity) {
        return mapper.updateById(entity);
    }
}
