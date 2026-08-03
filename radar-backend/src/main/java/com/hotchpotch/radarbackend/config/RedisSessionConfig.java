package com.hotchpotch.radarbackend.config;

import java.time.Duration;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.FlushMode;
import org.springframework.session.SaveMode;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Redis Session 配置。
 *
 * Spring Boot 4 不再自动注册 Servlet Redis Session 过滤器，需要显式启用
 * {@link EnableRedisHttpSession}，才能让 HttpSession 保存到 Redis。
 */
@Configuration(proxyBeanMethods = false)
@EnableRedisHttpSession(
        redisNamespace = "${spring.session.redis.namespace:stream-radar:dev:session}")
public class RedisSessionConfig {

    /**
     * 创建 Redis Session 仓库定制器，接入现有环境配置中的超时、刷新和保存策略。
     *
     * @param sessionTimeout Session 超时时间
     * @param flushMode Redis Session 刷新模式
     * @param saveMode Redis Session 保存模式
     * @return Redis Session 仓库定制器
     */
    @Bean
    public SessionRepositoryCustomizer<RedisSessionRepository> redisSessionRepositoryCustomizer(
            @Value("${spring.session.timeout:30m}") String sessionTimeout,
            @Value("${spring.session.redis.flush-mode:on_save}") String flushMode,
            @Value("${spring.session.redis.save-mode:on_set_attribute}") String saveMode) {
        Duration timeout = DurationStyle.detectAndParse(sessionTimeout);
        FlushMode resolvedFlushMode = parseFlushMode(flushMode);
        SaveMode resolvedSaveMode = parseSaveMode(saveMode);

        return repository -> {
            repository.setDefaultMaxInactiveInterval(timeout);
            repository.setFlushMode(resolvedFlushMode);
            repository.setSaveMode(resolvedSaveMode);
        };
    }

    /**
     * 将配置文本转换为 Redis Session 刷新模式。
     *
     * @param value 配置文本
     * @return Redis Session 刷新模式
     */
    private FlushMode parseFlushMode(String value) {
        return FlushMode.valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
    }

    /**
     * 将配置文本转换为 Redis Session 保存模式。
     *
     * @param value 配置文本
     * @return Redis Session 保存模式
     */
    private SaveMode parseSaveMode(String value) {
        return SaveMode.valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
    }
}
