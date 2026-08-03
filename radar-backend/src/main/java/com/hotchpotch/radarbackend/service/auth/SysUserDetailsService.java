package com.hotchpotch.radarbackend.service.auth;

import java.util.Locale;

import com.hotchpotch.radarbackend.domain.repository.SysUserRepository;
import com.hotchpotch.radarbackend.security.RadarUserPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 基于系统用户表的 Spring Security 用户查询服务。
 */
@Service
public class SysUserDetailsService implements UserDetailsService {

    /**
     * 系统用户仓库。
     */
    private final SysUserRepository userRepository;

    /**
     * 创建系统用户查询服务。
     *
     * @param userRepository 系统用户仓库
     */
    public SysUserDetailsService(SysUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedUsername = username == null
                ? ""
                : username.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByUsername(normalizedUsername)
                .filter(user -> Integer.valueOf(1).equals(user.getStatus()))
                .map(RadarUserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("用户名或密码错误"));
    }
}
