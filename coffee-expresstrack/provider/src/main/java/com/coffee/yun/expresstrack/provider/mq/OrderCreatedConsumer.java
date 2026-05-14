package com.coffee.yun.expresstrack.provider.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 订单创建事件消费者（RocketMQ Consumer）
 *
 * 作用：监听 RocketMQ 的 "order-created" topic，
 *   收到消息后自动为新订单创建快递单和第一条轨迹记录。
 *
 * @ConditionalOnProperty：
 *   仅当 feature.mq.enabled=true 时 Bean 才被注册。
 *   未配置或为 false 时，本类不会被 Spring 实例化，
 *   避免在没有 RocketMQ 的环境下因注册消费者失败导致启动报错。
 *
 * 消息驱动架构的优势（课堂演示要点）：
 *
 *   传统方式（同步 Dubbo RPC）：
 *     订单服务 → 直接调用 → 快递服务（强依赖，快递服务宕机 = 下单失败）
 *
 *   消息队列方式（异步解耦）：
 *     订单服务 → 发布消息 → RocketMQ → 快递服务消费（弱依赖，快递服务恢复后补消费）
 *     ↑ 下单立即成功，创建快递单在后台异步完成
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "feature.mq.enabled", havingValue = "true", matchIfMissing = false)
@RocketMQMessageListener(
        topic = "order-created",
        consumerGroup = "expresstrack-consumer-group"
)
public class OrderCreatedConsumer implements RocketMQListener<String> {

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    /**
     * 消费消息入口，RocketMQ Starter 框架自动调用
     *
     * 处理逻辑：
     *   1. 收到 order_id（消息体）
     *   2. 生成唯一的 express_id，插入 express 表
     *   3. 生成唯一的 track_id，插入 track 表（初始状态"商家已揽件"）
     *
     * @param orderId 消息体，即刚创建的订单编号
     */
    @Override
    public void onMessage(String orderId) {
        log.info("收到订单创建消息，order_id={}，开始创建快递单", orderId);

        String expressId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        Map<String, Object> expressParams = new HashMap<>();
        expressParams.put("express_id", expressId);
        expressParams.put("order_id", orderId);
        expressParams.put("express_weight", "1kg");
        sqlSessionTemplate.insert("ExpressTrackMapper.insertExpress", expressParams);
        log.info("快递单创建成功，express_id={}", expressId);

        String trackId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Map<String, Object> trackParams = new HashMap<>();
        trackParams.put("track_id", trackId);
        trackParams.put("express_id", expressId);
        trackParams.put("track_show", "商家已揽件");
        sqlSessionTemplate.insert("ExpressTrackMapper.insertTrack", trackParams);
        log.info("初始轨迹记录创建成功，track_id={}，状态=商家已揽件", trackId);
    }
}
