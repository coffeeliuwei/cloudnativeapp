package com.coffee.yun.userorder.provider.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 缓存配置属性（支持 Nacos Config 热更新）
 *
 * ── 设计目的 ──────────────────────────────────────────────────────────
 * 缓存 TTL（过期时间）是一个运营参数，不同时期可能需要调整：
 *   - 大促活动前：TTL 调大（减少数据库压力）
 *   - 数据变更频繁时：TTL 调小（提高数据新鲜度）
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
 *   5. 下次有代码调用 cacheProperties.getOrderTtl() 时返回的是新值
 *
 * ── 演示步骤 ──────────────────────────────────────────────────────────
 *   1. 启动服务，查询一条订单（数据写入 Redis，TTL=1800s）
 *   2. 在 Nacos 控制台 → 配置管理 → 配置列表 中找到：
 *      Data ID：coffee-userorder.properties  Group：DEFAULT_GROUP
 *   3. 添加或修改：cache.ttl.order=60
 *   4. 保存后，再次查询同一订单，新写入 Redis 的缓存 TTL 变为 60s
 *   5. 整个过程无需重启服务，用于演示"配置中心动态配置"的核心价值
 *
 * @Component  将本类注册为 Spring Bean，其他类可以通过 @Autowired 注入使用
 * @RefreshScope  标记本 Bean 支持动态刷新（Nacos Config 变更时自动重建）
 * @Getter  Lombok 注解，自动生成所有字段的 getter 方法（getOrderTtl()）
 * @Setter  Lombok 注解，自动生成所有字段的 setter 方法（setOrderTtl()）
 */
@Getter
@Setter
@Component
@RefreshScope
public class CacheProperties {

    /**
     * 订单详情缓存的过期时间（单位：秒）
     *
     * @Value("${cache.ttl.order:1800}")  表示：
     *   - 优先读取配置项 cache.ttl.order 的值（来源：Nacos Config 或本地 application.yml）
     *   - 如果该配置项不存在，则使用默认值 1800 秒（30 分钟）
     *
     * 订单数据属于读多写少场景：用户下单后订单状态可能几小时才变一次，
     * 30 分钟的缓存可以有效减少数据库查询次数。
     */
    @Value("${cache.ttl.order:1800}")
    private long orderTtl;
}
