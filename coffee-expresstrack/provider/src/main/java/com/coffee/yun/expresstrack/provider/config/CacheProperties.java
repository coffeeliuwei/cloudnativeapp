package com.coffee.yun.expresstrack.provider.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 缓存配置属性（支持 Nacos Config 热更新）
 *
 * ── 设计目的 ──────────────────────────────────────────────────────────
 * 缓存 TTL（过期时间）是一个运营参数，不同时期可能需要调整：
 *   - 快递轨迹更新频繁时：TTL 调小（让用户看到更新鲜的数据）
 *   - 已签收订单：TTL 可以调得很大（轨迹不再变化，长期缓存没问题）
 *
 * 如果把 TTL 写死在代码里，每次调整都要重新编译、打包、重启，代价很高。
 * 使用 Nacos Config + @RefreshScope，可以在 Nacos 控制台修改配置后
 * 实时生效，无需重启任何服务。
 *
 * ── @RefreshScope 工作原理 ────────────────────────────────────────────
 * Spring Cloud 的 RefreshScope 是一种特殊的作用域：
 *   1. 正常情况下本 Bean 是单例，@Value 只在启动时注入一次
 *   2. 当 Nacos Config 发生变更时，Spring Cloud 触发 ContextRefreshedEvent
 *   3. RefreshScope 销毁旧的 Bean 实例并重新创建一个新实例
 *   4. 新实例重新执行 @Value 注入，获取最新的配置值
 *   5. 下次有代码调用 cacheProperties.getExpresstrackTtl() 时返回的是新值
 *
 * ── 使用方式 ──────────────────────────────────────────────────────────
 * 在 Nacos 控制台 → 配置管理 → 配置列表 中找到：
 *   Data ID：coffee-expresstrack.properties
 *   Group：DEFAULT_GROUP
 * 添加或修改配置项：
 *   cache.ttl.expresstrack=1200
 * 保存后立即生效，TTL 从 3600 秒变为 1200 秒。
 *
 * @Component  将本类注册为 Spring Bean，其他类可以通过 @Autowired 注入使用
 * @RefreshScope  标记本 Bean 支持动态刷新（Nacos Config 变更时自动重建）
 * @Getter  Lombok 注解，自动生成所有字段的 getter 方法（getExpresstrackTtl()）
 */
@Getter
@Component
@RefreshScope
public class CacheProperties {

    /**
     * 快递轨迹缓存的过期时间（单位：秒）
     *
     * @Value("${cache.ttl.expresstrack:3600}")  表示：
     *   - 优先读取配置项 cache.ttl.expresstrack 的值
     *   - 如果该配置项不存在（Nacos 未配置），则使用默认值 3600 秒（1 小时）
     *
     * 快递轨迹通常每隔数小时才有新节点，1 小时的缓存可以显著减少数据库查询，
     * 同时不会让用户等待太长时间看到最新轨迹。
     */
    @Value("${cache.ttl.expresstrack:3600}")
    private long expresstrackTtl;
}
