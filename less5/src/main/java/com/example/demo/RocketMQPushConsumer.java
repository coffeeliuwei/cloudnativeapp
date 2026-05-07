package com.example.demo;

import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.RPCHook;

/**
 * RocketMQ 消息消费者示例（Push 模式，阿里云版）
 *
 * RocketMQ 消费模式有两种：
 *   - Push（推模式）：Broker 主动将消息推送给 Consumer，响应及时，本类采用此模式
 *   - Pull（拉模式）：Consumer 主动向 Broker 拉取消息，灵活但需自行控制拉取频率
 *
 * 本消费者与 RocketMQProducer 配合使用：
 *   Producer 向 Topic "ordermessage" 发送消息，
 *   Consumer 订阅同一 Topic，消息到达后自动触发 consumeMessage 回调方法。
 *
 * 消费者启动后会持续运行，保持对 Broker 的长连接，等待新消息到来。
 */
public class RocketMQPushConsumer {

    /**
     * 构建 ACL 身份认证钩子（公网访问阿里云 RocketMQ 必须配置）
     *
     * 认证信息（用户名和密码）在阿里云控制台 → 访问控制 → 智能身份识别 中获取。
     * 注意：这里填写的是 RocketMQ 实例的用户名密码，
     *       不是阿里云账号的 AccessKey ID / AccessKey Secret，切勿混淆！
     */
    private static RPCHook getAclRPCHook() {
        return new AclClientRPCHook(new SessionCredentials("OuE5tTCN9alQx8d1", "6W1A21qGZmZ8f5gC"));
    }

    /**
     * 程序入口：初始化 Consumer 并启动监听，等待消息到来
     *
     * 整体步骤：
     *   1. 创建 DefaultMQPushConsumer（携带认证钩子）
     *   2. 配置 ConsumerGroup、接入方式、命名空间、NameServer 地址
     *   3. 设置消费位点（从哪条消息开始消费）
     *   4. 订阅 Topic 和 Tag
     *   5. 注册消息监听器（定义收到消息后的处理逻辑）
     *   6. 调用 consumer.start() 启动，程序保持运行
     */
    public static void main(String[] args) throws Exception {

        // 创建 Push 模式消费者，传入认证钩子（公网访问必须）
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(getAclRPCHook());

        // 设置消费者组名（与生产者的 ProducerGroup 对应，同一业务消费者归为一组）
        // 同一组内多个消费者实例会分摊消息（负载均衡），提高吞吐量
        consumer.setConsumerGroup("GID_odermessage");

        // 指定接入方式为阿里云（CLOUD），使用阿里云托管的 RocketMQ 时必须设置
        consumer.setAccessChannel(AccessChannel.CLOUD);

        // 设置命名空间（Serverless 实例公网访问时必须设置，需要 SDK ≥ 5.2.0）
        consumer.setNamespaceV2("rmq-cn-07k4rtyp802");

        // 设置 NameServer 地址（消费者通过 NameServer 发现 Broker 的位置）
        // 格式：域名:端口，不要加 http:// 或 https:// 前缀
        consumer.setNamesrvAddr("rmq-cn-07k4rtyp802.cn-hangzhou.rmq.aliyuncs.com:8080");

        // 设置消费位点：决定消费者启动时从哪条消息开始消费
        // CONSUME_FROM_LAST_OFFSET  —— 从最新消息开始，忽略历史积压消息（适合先启动Consumer再启动Producer的场景）
        // CONSUME_FROM_FIRST_OFFSET —— 从最早消息开始，会消费队列中所有历史消息（适合先发消息再启动Consumer的场景）
        // CONSUME_FROM_TIMESTAMP    —— 从指定时间点开始消费
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);

        // 订阅 Topic 和 Tag
        // 第一个参数：Topic 名称（必须与生产者发送时一致）
        // 第二个参数：Tag 过滤表达式，"*" 表示接收该 Topic 下所有 Tag 的消息
        //             也可以指定特定 Tag，如 "yourMessageTagA || yourMessageTagB"
        consumer.subscribe("ordermessage", "*");

        // 注册并发消息监听器（MessageListenerConcurrently）
        // 并发监听器：多线程并行处理消息，吞吐量高，但不保证消息顺序
        // 若需要顺序消费，应使用 MessageListenerOrderly
        consumer.registerMessageListener(new MessageListenerConcurrently() {

            /**
             * 消息到达时自动回调此方法
             *
             * @param msgs    本次推送的消息列表（通常包含 1~多 条消息）
             * @param context 并发消费上下文（包含重试次数等信息）
             * @return 消费状态：
             *           CONSUME_SUCCESS —— 消费成功，Broker 会标记该消息为已消费
             *           RECONSUME_LATER —— 消费失败，Broker 会在一段时间后重新推送（最多重试 16 次）
             */
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(
                    java.util.List<MessageExt> msgs,
                    org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext context) {

                // 遍历本次推送的所有消息（通常为 1 条，批量消费时可能多条）
                for (MessageExt msg : msgs) {
                    System.out.println("Receive message: " + msg);                        // 消息完整信息
                    System.out.println("Message body: " + new String(msg.getBody()));     // 消息体内容（字符串形式）
                    System.out.println("Topic: " + msg.getTopic());                       // 消息所属 Topic
                    System.out.println("Tags: " + msg.getTags());                         // 消息 Tag 标签
                    System.out.println("Keys: " + msg.getKeys());                         // 消息业务 Key（用于检索）
                    System.out.println("MsgId: " + msg.getMsgId());                       // 消息唯一 ID（全局唯一）
                    System.out.println("QueueId: " + msg.getQueueId());                   // 消息所在队列编号
                    System.out.println("QueueOffset: " + msg.getQueueOffset());           // 消息在队列中的偏移量
                    System.out.println("StoreTime: " + msg.getStoreTimestamp());          // 消息存储到 Broker 的时间
                    System.out.println("----------------------------");
                }

                // 返回消费成功，通知 Broker 可以将该消息标记为已消费（提交 offset）
                // 如果业务处理失败，应返回 ConsumeConcurrentlyStatus.RECONSUME_LATER
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        // 启动消费者，建立长连接开始监听消息
        consumer.start();
        System.out.println("Consumer started successfully, waiting for messages...");

        // 消费者需要持续运行才能接收消息，不调用 shutdown()
        // 程序会在此处阻塞，直到手动停止（Ctrl+C 或关闭进程）
    }
}