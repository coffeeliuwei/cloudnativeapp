package com.coffee.yun.userorder.provider.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 缓存配置属性（支持 Nacos Config 热更新）
 *
 * 作用：集中管理 Redis 缓存的 TTL（过期时间）配置，
 *   使其可以通过 Nacos Config Center 动态修改，无需重启服务。
 *
 * @RefreshScope 工作原理：
 *   1. 在 Nacos 控制台修改 coffee-userorder.properties 中的 cache.ttl.order
 *   2. Spring Cloud 监听到配置变更事件（ContextRefreshedEvent）
 *   3. 标注了 @RefreshScope 的 Bean 会被销毁并重新创建
 *   4. 新 Bean 使用更新后的配置值
 *   5. 整个过程不重启服务，业务无感知
 *
 * 演示方法：
 *   1. 启动服务，查询一条订单（数据写入 Redis，TTL=1800s）
 *   2. 在 Nacos 控制台将 cache.ttl.order 改为 60
 *   3. 再查询同一订单，Redis 中新写入的缓存 TTL 变为 60s
 */
@Getter
@Setter
@Component
@RefreshScope
public class CacheProperties {

    /**
     * 订单详情缓存时长（秒）
     * 默认值 1800（30分钟），可在 Nacos 中配置 cache.ttl.order=xxx 覆盖
     */
    @Value("${cache.ttl.order:1800}")
    private long orderTtl;
}
