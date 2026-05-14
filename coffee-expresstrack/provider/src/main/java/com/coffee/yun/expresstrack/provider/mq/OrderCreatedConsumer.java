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
 * 消息驱动架构的优势（课堂演示要点）：
 *
 *   传统方式（同步 Dubbo RPC）：
 *     订单服务 → 直接调用 → 快递服务（强依赖，快递服务宕机 = 下单失败）
 *
 *   消息队列方式（异步解耦）：
 *     订单服务 → 发布消息 → RocketMQ → 快递服务消费（弱依赖，快递服务恢复后补消费）
 *     ↑ 下单立即成功，创建快递单在后台异步完成
 *
 * @RocketMQMessageListener 参数说明：
 *   - topic：监听的消息主题，与生产者发布时的 topic 一致（"order-created"）
 *   - consumerGroup：消费者组名，同组内的多个实例只有一个能收到同一条消息（负载均衡）
 *
 * 实现 RocketMQListener<String>：
 *   泛型 String 表示消息体类型，与生产者发送的类型对应。
 *   RocketMQ Starter 自动处理反序列化。
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
     * 幂等性说明：
     *   生产环境中，同一条消息可能因网络重试被消费多次。
     *   此处简化演示，未做幂等处理。生产上应在插入前先查询 express 表是否已存在该 order_id。
     *
     * @param orderId 消息体，即刚创建的订单编号
     */
    @Override
    public void onMessage(String orderId) {
        log.info("收到订单创建消息，order_id={}，开始创建快递单", orderId);

        // 生成快递单号（生产环境建议用分布式ID生成器，如雪花算法）
        String expressId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 第一步：插入快递单记录
        Map<String, Object> expressParams = new HashMap<>();
        expressParams.put("express_id", expressId);
        expressParams.put("order_id", orderId);
        expressParams.put("express_weight", "1kg");  // 实际场景从订单详情中获取
        sqlSessionTemplate.insert("ExpressTrackMapper.insertExpress", expressParams);
        log.info("快递单创建成功，express_id={}", expressId);

        // 第二步：插入第一条轨迹记录（订单揽件状态）
        String trackId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Map<String, Object> trackParams = new HashMap<>();
        trackParams.put("track_id", trackId);
        trackParams.put("express_id", expressId);
        trackParams.put("track_show", "商家已揽件");
        sqlSessionTemplate.insert("ExpressTrackMapper.insertTrack", trackParams);
        log.info("初始轨迹记录创建成功，track_id={}，状态=商家已揽件", trackId);
    }
}
