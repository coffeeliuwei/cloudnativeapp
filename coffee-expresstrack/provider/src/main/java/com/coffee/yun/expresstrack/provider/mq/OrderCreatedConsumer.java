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
 * ── 这个类解决什么问题？ ────────────────────────────────────────────────
 *
 * 业务场景：用户下单后，快递系统需要为该订单创建快递单和初始轨迹记录。
 *
 * 传统方案（同步调用）：
 *   coffee-userorder → Dubbo RPC → coffee-expresstrack.createExpress()
 *   问题：强耦合。若快递服务宕机或响应慢，下单接口也会失败或超时，影响用户体验。
 *
 * 消息驱动方案（本类实现的方式）：
 *   coffee-userorder → 发布消息到 RocketMQ → 立即返回成功给用户
 *                                           ↓（异步）
 *                                   coffee-expresstrack 消费消息 → 创建快递单
 *
 *   优势：
 *     1. 解耦：订单服务和快递服务独立运行，互不影响
 *     2. 异步：下单不需要等待快递单创建完成，响应更快
 *     3. 削峰：高峰期消息堆积在 MQ，快递服务按自己的节奏消费，不会过载
 *     4. 可靠：MQ 保存消息，快递服务重启后可以继续消费，不丢数据
 *
 * ── @ConditionalOnProperty 的作用 ────────────────────────────────────
 * 仅当 feature.mq.enabled=true 时，Spring 才会实例化本类并注册消费者。
 * 当 feature.mq.enabled=false（默认值）时：
 *   - 本类不会被 Spring 扫描和实例化
 *   - 不会向 RocketMQ 注册任何消费者
 *   - 本地没有 RocketMQ 的环境也能正常启动，不报连接错误
 *
 * matchIfMissing = false：
 *   若配置文件中根本没有 feature.mq.enabled 这个 key，视为"未开启"，
 *   Bean 不注册（等同于 false）。
 *
 * ── @RocketMQMessageListener 的作用 ──────────────────────────────────
 *   topic = "order-created"：
 *     监听名为 "order-created" 的 Topic，该 Topic 由 coffee-userorder 的
 *     UserOrderInfoServiceImpl.createOrder() 方法发布消息时创建。
 *
 *   consumerGroup = "expresstrack-consumer-group"：
 *     消费者分组名。RocketMQ 通过分组管理消费进度（offset），
 *     同一分组内多个实例负载均衡消费，不同分组各自独立消费同一条消息。
 *     命名规范：建议以服务名为前缀，明确归属。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "feature.mq.enabled", havingValue = "true", matchIfMissing = false)
@RocketMQMessageListener(
        topic = "order-created",
        consumerGroup = "expresstrack-consumer-group"
)
public class OrderCreatedConsumer implements RocketMQListener<String> {

    // RocketMQ 框架回调 onMessage() 时，本 Bean 已由 Spring 完成依赖注入，
    // 因此可以直接使用 sqlSessionTemplate 访问数据库
    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    /**
     * 消费消息入口 —— 由 RocketMQ Starter 框架自动调用
     *
     * 泛型 RocketMQListener<String> 表示消息体的类型是 String，
     * 即 coffee-userorder 发送的 order_id 字符串（如 "ORDER20240514001"）。
     *
     * 处理逻辑（三步）：
     *   Step 1. 接收 order_id
     *   Step 2. 生成 express_id，插入 express 表（快递单基本信息）
     *   Step 3. 生成 track_id，插入 track 表（初始轨迹"商家已揽件"）
     *
     * 异常处理：
     *   若本方法抛出异常，RocketMQ 会根据重试策略（默认重试16次）重新投递消息。
     *   因此本方法应保持幂等性（相同 order_id 重复调用不会产生重复记录）。
     *   当前实现未做幂等处理，生产环境需加"先查后插"防重逻辑。
     *
     * @param orderId  消息体，即刚创建的订单编号（coffee-userorder 发布的内容）
     */
    @Override
    public void onMessage(String orderId) {
        log.info("收到订单创建消息，order_id={}，开始创建快递单", orderId);

        // ── Step 2：创建快递单（express 表）──────────────────────────────
        // UUID.randomUUID() 生成全局唯一标识（格式：550e8400-e29b-41d4-a716-446655440000）
        // .replace("-", "") 去掉连字符，得到 32 位十六进制字符串
        // .substring(0, 16) 截取前 16 位作为 express_id（数据库字段长度限制）
        String expressId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 用 Map 组装插入参数（对应 ExpressTrackMapper.xml 中 insertExpress 语句的 #{} 占位符）
        Map<String, Object> expressParams = new HashMap<>();
        expressParams.put("express_id", expressId);
        expressParams.put("order_id", orderId);        // 关联到触发本消息的订单
        expressParams.put("express_weight", "1kg");    // 演示数据，实际项目应从订单获取重量
        // 执行 MyBatis INSERT 语句，对应 mapper/ExpressTrackMapper.xml 中 id="insertExpress" 的 SQL
        sqlSessionTemplate.insert("ExpressTrackMapper.insertExpress", expressParams);
        log.info("快递单创建成功，express_id={}", expressId);

        // ── Step 3：创建初始轨迹记录（track 表）──────────────────────────
        String trackId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        Map<String, Object> trackParams = new HashMap<>();
        trackParams.put("track_id", trackId);
        trackParams.put("express_id", expressId);     // 关联到刚创建的快递单
        trackParams.put("track_show", "商家已揽件");   // 快递轨迹的第一个状态节点
        // 执行 MyBatis INSERT 语句，对应 mapper/ExpressTrackMapper.xml 中 id="insertTrack" 的 SQL
        sqlSessionTemplate.insert("ExpressTrackMapper.insertTrack", trackParams);
        log.info("初始轨迹记录创建成功，track_id={}，状态=商家已揽件", trackId);
    }
}
