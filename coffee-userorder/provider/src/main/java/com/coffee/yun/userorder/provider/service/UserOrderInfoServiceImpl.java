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
 * 新增能力（云原生改造）：
 *   1. Redis 缓存：由 feature.cache.enabled 控制，默认关闭。
 *      关闭时行为与改造前完全相同，直接查数据库。
 *      开启后：先查 Redis，命中直接返回；未命中查 DB 并写入 Redis。
 *
 *   2. RocketMQ 生产者：由 feature.mq.enabled 控制，默认关闭。
 *      关闭时 createOrder 只写数据库，不发消息。
 *      开启后：写 DB 后发布 "order-created" 消息，coffee-expresstrack 异步创建快递单。
 *
 *   3. Nacos Config：缓存 TTL 可在 Nacos 控制台热更新，无需重启。
 *
 * 功能开关在 application-dev.yml 中配置：
 *   feature.cache.enabled=false  （默认关闭，保持原项目可正常启动）
 *   feature.mq.enabled=false     （默认关闭，保持原项目可正常启动）
 */
@Slf4j
@DubboService
public class UserOrderInfoServiceImpl implements UserOrderInfoService {

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    @Autowired
    private PageUtil pageUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheProperties cacheProperties;

    /**
     * required=false：rocketmq.name-server 为空时 RocketMQTemplate Bean 不存在，
     * 此处注入 null 而不是报错，服务正常启动。
     */
    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    /** 是否启用 Redis 缓存，由 application-dev.yml 中 feature.cache.enabled 控制 */
    @Value("${feature.cache.enabled:false}")
    private boolean cacheEnabled;

    /** 是否启用 RocketMQ，由 application-dev.yml 中 feature.mq.enabled 控制 */
    @Value("${feature.mq.enabled:false}")
    private boolean mqEnabled;

    private static final String CACHE_KEY_PREFIX = "order:detail:";
    private static final String ORDER_CREATED_TOPIC = "order-created";

    /**
     * 根据查询条件查询单条订单信息
     *
     * 缓存策略（仅 feature.cache.enabled=true 时生效）：
     *   命中 Redis → 直接返回；未命中 → 查 DB → 写 Redis → 返回
     */
    @Override
    public UserOrderInfoResultDTO findUserOrderInfo(UserOrderInfoParamDTO userOrderInfoParamDTO) {
        log.info("订单查询：{}", JSON.toJSONString(userOrderInfoParamDTO));

        String orderId = userOrderInfoParamDTO.getOrder_id();

        if (cacheEnabled && orderId != null && !orderId.isEmpty()) {
            String cacheKey = CACHE_KEY_PREFIX + orderId;

            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.info("缓存命中，order_id={}", orderId);
                return (UserOrderInfoResultDTO) cached;
            }

            UserOrderInfoResultDTO result = sqlSessionTemplate.selectOne(
                    "UserOrderMapper.selectByParam", userOrderInfoParamDTO);

            if (result != null) {
                redisTemplate.opsForValue().set(cacheKey, result, cacheProperties.getOrderTtl(), TimeUnit.SECONDS);
                log.info("写入缓存，order_id={}，TTL={}s", orderId, cacheProperties.getOrderTtl());
            }
            return result;
        }

        return sqlSessionTemplate.selectOne("UserOrderMapper.selectByParam", userOrderInfoParamDTO);
    }

    /**
     * 根据查询条件分页查询订单列表（不走缓存）
     */
    @Override
    public PageDTO<UserOrderInfoResultDTO> findUserOrderInfos(UserOrderInfoParamDTO userOrderInfoParamDTO) {
        return pageUtil.selectPage("UserOrderMapper.selectByParam", userOrderInfoParamDTO);
    }

    /**
     * 创建订单
     *
     * 当 feature.mq.enabled=false 时：只写数据库，与传统同步方式相同。
     * 当 feature.mq.enabled=true 时：写 DB 后发布 RocketMQ 消息，coffee-expresstrack 异步创建快递单。
     */
    @Override
    public String createOrder(UserOrderCreateDTO createDTO) {
        log.info("创建订单：{}", JSON.toJSONString(createDTO));

        Map<String, Object> params = new HashMap<>();
        params.put("order_id", createDTO.getOrder_id());
        params.put("OneID", createDTO.getOneID());
        params.put("order_amount", createDTO.getOrder_amount());
        params.put("order_status", "待发货");
        sqlSessionTemplate.insert("UserOrderMapper.insertOrder", params);
        log.info("订单写入数据库成功，order_id={}", createDTO.getOrder_id());

        if (mqEnabled && rocketMQTemplate != null) {
            try {
                rocketMQTemplate.send(
                        ORDER_CREATED_TOPIC,
                        MessageBuilder.withPayload(createDTO.getOrder_id()).build()
                );
                log.info("消息已发布到 RocketMQ，topic={}，order_id={}", ORDER_CREATED_TOPIC, createDTO.getOrder_id());
            } catch (Exception e) {
                log.warn("RocketMQ 消息发布失败（不影响下单），原因：{}", e.getMessage());
            }
        }

        return createDTO.getOrder_id();
    }
}
