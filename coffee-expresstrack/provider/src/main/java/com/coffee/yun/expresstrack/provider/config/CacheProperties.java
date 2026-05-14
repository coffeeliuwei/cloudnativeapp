package com.coffee.yun.expresstrack.provider.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 缓存配置属性（支持 Nacos Config 热更新）
 *
 * @RefreshScope 使本 Bean 在 Nacos Config 发生变更时自动重新初始化，
 * 无需重启服务即可使新的 TTL 值生效。
 *
 * 对应 Nacos 数据 ID：coffee-expresstrack.properties
 * 可配置的 Key：cache.ttl.expresstrack
 */
@Getter
@Component
@RefreshScope
public class CacheProperties {

    @Value("${cache.ttl.expresstrack:3600}")
    private long expresstrackTtl;
}
