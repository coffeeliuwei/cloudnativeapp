package com.coffee.yun.expresstrack.provider.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 缓存配置属性（支持 Nacos Config 热更新）
 *
 * @RefreshScope 使该 Bean 可以感知 Nacos 配置变更并自动刷新。
 *
 * 演示步骤：
 *   1. 在 Nacos 控制台创建配置：
 *      Data ID：coffee-expresstrack.properties
 *      内容：cache.ttl.expresstrack=60
 *   2. 服务运行中，不重启直接在 Nacos 修改该值
 *   3. 新查询写入 Redis 的缓存 TTL 会自动变为新值
 */
@Getter
@Setter
@Component
@RefreshScope
public class CacheProperties {

    /**
     * 快递轨迹缓存时长（秒）
     * 默认值 3600（1小时），可在 Nacos 中配置 cache.ttl.expresstrack=xxx 覆盖
     */
    @Value("${cache.ttl.expresstrack:3600}")
    private long expresstrackTtl;
}
