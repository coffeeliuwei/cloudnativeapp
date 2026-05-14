package com.coffee.yun.userorder.provider.service;

import com.alibaba.fastjson2.JSON;
import com.coffee.yun.dto.PageDTO;
import com.coffee.yun.userorder.api.dto.UserOrderCreateDTO;
import com.coffee.yun.userorder.api.dto.UserOrderInfoParamDTO;
import com.coffee.yun.userorder.api.dto.UserOrderInfoResultDTO;
import com.coffee.yun.userorder.api.service.UserOrderInfoService;
import com.coffee.yun.userorder.provider.config.CacheProperties;
import com.coffee.yun.userorder.provider.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户订单服务实现类
 *
 * ── 云原生改造说明 ────────────────────────────────────────────────────
 * 本类在原有"查询订单、创建订单"功能基础上，新增了三项云原生能力：
 *
 *   1. Redis 缓存（Cache-Aside Pattern）
 *      由 feature.cache.enabled 控制，默认 false（关闭）。
 *      关闭时：行为与改造前完全相同，直接查数据库。
 *      开启后：查询先走 Redis，命中直接返回；未命中才查 DB 并回写 Redis。
 *
 *   2. RocketMQ 消息发布（异步解耦）
 *      由 feature.mq.enabled 控制，默认 false（关闭）。
 *      关闭时：createOrder 只写数据库，与传统同步方式相同。
 *      开启后：写 DB 后向 RocketMQ 发布 "order-created" 消息，
 *              coffee-expresstrack 服务异步消费消息创建快递单。
 *
 *   3. Nacos Config 热更新
 *      缓存 TTL 存储在 CacheProperties 中，该类使用 @RefreshScope，
 *      Nacos 控制台修改 cache.ttl.order 后立即生效，无需重启。
 *
 * ── 功能开关设计原则 ──────────────────────────────────────────────────
 * Redis 和 RocketMQ 在本地开发环境可能没有启动，因此：
 *   - application-dev.yml 中两个开关均默认 false
 *   - 只有生产环境（application-prod.yml）才默认 true
 * 这样同学们在本地运行原始项目时，不需要安装配置 Redis 和 RocketMQ，
 * 项目依然可以正常启动和运行，降低学习门槛。
 *
 * @DubboService  将本类注册为 Dubbo RPC 服务提供者，coffee-app 通过 Dubbo 调用此接口
 * @Slf4j  Lombok 自动生成 log 字段，供日志输出使用
 */
@Slf4j
@DubboService
public class UserOrderInfoServiceImpl implements UserOrderInfoService {

    // ── 依赖注入 ──────────────────────────────────────────────────────

    // MyBatis 的 SqlSessionTemplate 是线程安全的，封装了 SqlSession 的创建和关闭，
    // 直接注入即可用于执行 mapper/*.xml 中定义的 SQL 语句
    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    // 分页工具，封装了 PageHelper 插件的分页查询逻辑
    @Autowired
    private PageUtil pageUtil;

    // 自定义的 RedisTemplate（在 RedisConfig 中定义），
    // key 为 String，value 为 JSON 格式存储的 Java 对象
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 缓存配置，提供 TTL 值，支持 Nacos Config 热更新
    @Autowired
    private CacheProperties cacheProperties;

    /**
     * required=false：非必须注入
     *
     * 当 rocketmq.name-server 配置为空（dev 环境默认值）时，
     * RocketMQ Starter 不会创建 RocketMQTemplate Bean，
     * 若此处 required=true（默认），Spring 启动时会因找不到 Bean 而报错。
     * required=false 改为"如果存在就注入，不存在就保持 null"，
     * 服务可以在没有 RocketMQ 的环境下正常启动。
     * （在 createOrder 方法中用 "mqEnabled && rocketMQTemplate != null" 双重保护）
     */
    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    // ── 功能开关 ──────────────────────────────────────────────────────

    /**
     * 是否启用 Redis 缓存
     *
     * @Value 从配置文件读取：
     *   - application-dev.yml 中 feature.cache.enabled=false → 关闭缓存
     *   - application-prod.yml 中 feature.cache.enabled=true → 开启缓存
     * 冒号后的 false 是默认值，若配置文件中没有此 key 时使用
     */
    @Value("${feature.cache.enabled:false}")
    private boolean cacheEnabled;

    /**
     * 是否启用 RocketMQ 消息发布
     *
     * 与 cacheEnabled 同理：dev 默认 false，prod 默认 true
     */
    @Value("${feature.mq.enabled:false}")
    private boolean mqEnabled;

    // ── 常量 ──────────────────────────────────────────────────────────

    /**
     * Redis key 前缀，格式：order:detail:{order_id}
     *
     * Redis key 命名规范：使用冒号分隔层级，便于在控制台按前缀过滤。
     * 例如：KEYS order:detail:* 可以列出所有订单缓存的 key
     */
    private static final String CACHE_KEY_PREFIX = "order:detail:";

    /**
     * RocketMQ Topic 名称，与 coffee-expresstrack 的消费者监听的 topic 必须一致
     */
    private static final String ORDER_CREATED_TOPIC = "order-created";

    // ── 业务方法 ──────────────────────────────────────────────────────

    /**
     * 根据查询条件查询单条订单信息（带缓存的 Cache-Aside Pattern 实现）
     *
     * Cache-Aside（旁路缓存）是最常见的缓存策略，流程如下：
     *
     *   读取时：
     *     ① 先查 Redis（"order:detail:{order_id}"）
     *     ② 命中（缓存存在）→ 直接返回，不访问数据库
     *     ③ 未命中（缓存不存在/已过期）→ 查数据库 → 把结果写入 Redis → 返回
     *
     *   更新时（本项目未实现，仅供参考）：
     *     ① 先更新数据库
     *     ② 再删除对应的 Redis key（下次读取时重新从 DB 加载最新数据）
     *
     * 注意：仅当 order_id 不为空时才使用缓存（列表查询不走缓存，避免 key 复杂）
     *
     * @param userOrderInfoParamDTO  查询条件（order_id、member_name 等）
     * @return  订单信息，若查询无结果则返回 null
     */
    @Override
    public UserOrderInfoResultDTO findUserOrderInfo(UserOrderInfoParamDTO userOrderInfoParamDTO) {
        log.info("订单查询：{}", JSON.toJSONString(userOrderInfoParamDTO));

        String orderId = userOrderInfoParamDTO.getOrder_id();

        // ── 缓存路径（feature.cache.enabled=true 且有 order_id 时走此分支）─
        if (cacheEnabled && orderId != null && !orderId.isEmpty()) {
            // 拼装 Redis key，如 "order:detail:ORDER20240514001"
            String cacheKey = CACHE_KEY_PREFIX + orderId;

            // opsForValue() 对应 Redis 的 String 数据结构（最基础的 key-value 存储）
            // get(key)：从 Redis 读取数据，返回 Object（实际是 UserOrderInfoResultDTO 的 JSON 反序列化结果）
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                // 缓存命中：直接返回，节省一次数据库查询
                log.info("缓存命中，order_id={}", orderId);
                // 强制类型转换：Jackson 序列化时已记录了类型信息，反序列化后是正确的类型
                return (UserOrderInfoResultDTO) cached;
            }

            // 缓存未命中：查数据库
            // selectOne：执行 UserOrderMapper.xml 中 id="selectByParam" 的 SQL，期望返回一条记录
            UserOrderInfoResultDTO result = sqlSessionTemplate.selectOne(
                    "UserOrderMapper.selectByParam", userOrderInfoParamDTO);

            // 把查询结果写入 Redis（仅当查到数据时才缓存，避免缓存空值）
            if (result != null) {
                // set(key, value, timeout, unit)：设置带过期时间的缓存
                // cacheProperties.getOrderTtl() 返回秒数，支持 Nacos 热更新
                redisTemplate.opsForValue().set(cacheKey, result, cacheProperties.getOrderTtl(), TimeUnit.SECONDS);
                log.info("写入缓存，order_id={}，TTL={}s", orderId, cacheProperties.getOrderTtl());
            }
            return result;
        }

        // ── 直通路径（cacheEnabled=false 或 orderId 为空）────────────────
        // 与云原生改造前的行为完全相同，直接查数据库
        return sqlSessionTemplate.selectOne("UserOrderMapper.selectByParam", userOrderInfoParamDTO);
    }

    /**
     * 根据查询条件分页查询订单列表
     *
     * 列表查询不走缓存，原因：
     *   1. 查询条件组合复杂（多个字段组合），缓存 key 设计困难
     *   2. 列表结果变化频繁，缓存命中率低，收益不大
     *   3. 分页查询本身对性能要求没有单条查询高
     *
     * @param userOrderInfoParamDTO  查询条件，pageUtil 内部读取 pageNum/pageSize 参数
     * @return  分页结果，包含 total（总条数）和 records（当前页数据列表）
     */
    @Override
    public PageDTO<UserOrderInfoResultDTO> findUserOrderInfos(UserOrderInfoParamDTO userOrderInfoParamDTO) {
        // 直接调用 PageUtil 执行分页查询，分页参数由 pageUtil 内部通过 PageHelper 注入
        return pageUtil.selectPage("UserOrderMapper.selectByParam", userOrderInfoParamDTO);
    }

    /**
     * 创建订单（写数据库 + 可选发布 RocketMQ 消息）
     *
     * 执行流程：
     *   ① 将订单数据写入数据库（`order` 表）← 核心操作，始终执行
     *   ② 若 feature.mq.enabled=true 且 RocketMQTemplate 可用，
     *      则向 RocketMQ 发布 "order-created" 消息
     *   ③ coffee-expresstrack 的 OrderCreatedConsumer 异步消费消息，
     *      为订单创建快递单和初始轨迹记录
     *
     * 消息发送失败处理策略：
     *   使用 try-catch 捕获异常并打印警告日志，不向上抛出。
     *   原则：MQ 消息是异步辅助流程，其失败不应影响核心下单功能。
     *   （生产环境可配合本地消息表实现可靠投递，本项目为教学简化）
     *
     * @param createDTO  创建订单的参数（order_id、OneID、order_amount）
     * @return  成功创建的 order_id
     */
    @Override
    public String createOrder(UserOrderCreateDTO createDTO) {
        log.info("创建订单：{}", JSON.toJSONString(createDTO));

        // ── Step 1：写入数据库 ───────────────────────────────────────────
        // 组装 MyBatis 参数 Map，对应 UserOrderMapper.xml 中 insertOrder 的 #{} 占位符
        Map<String, Object> params = new HashMap<>();
        params.put("order_id", createDTO.getOrder_id());
        params.put("OneID", createDTO.getOneID());
        params.put("order_amount", createDTO.getOrder_amount());
        params.put("order_status", "待发货");   // 新订单默认状态
        // 执行 INSERT SQL，对应 UserOrderMapper.xml 中 id="insertOrder" 的语句
        sqlSessionTemplate.insert("UserOrderMapper.insertOrder", params);
        log.info("订单写入数据库成功，order_id={}", createDTO.getOrder_id());

        // ── Step 2：发布 RocketMQ 消息（可选）──────────────────────────
        // 双重保护：mqEnabled 为 true 且 rocketMQTemplate 不为 null（确认 MQ 已初始化）
        if (mqEnabled && rocketMQTemplate != null) {
            try {
                // rocketMQTemplate.send()：同步发送消息（等待 Broker 确认收到，默认超时 3s）
                // ORDER_CREATED_TOPIC：消息的 Topic，消费者通过 @RocketMQMessageListener(topic=...) 订阅
                // MessageBuilder.withPayload()：构建消息体，此处只传 order_id 字符串即可，
                //   快递服务收到 order_id 后自行查询或直接创建记录
                rocketMQTemplate.send(
                        ORDER_CREATED_TOPIC,
                        MessageBuilder.withPayload(createDTO.getOrder_id()).build()
                );
                log.info("消息已发布到 RocketMQ，topic={}，order_id={}", ORDER_CREATED_TOPIC, createDTO.getOrder_id());
            } catch (Exception e) {
                // 消息发送失败：只记录警告，不影响下单结果
                // 生产环境建议引入本地消息表（Transactional Outbox Pattern）保证可靠投递
                log.warn("RocketMQ 消息发布失败（不影响下单），原因：{}", e.getMessage());
            }
        }

        // 返回 order_id，coffee-app 收到后可以回显给用户
        return createDTO.getOrder_id();
    }
}
