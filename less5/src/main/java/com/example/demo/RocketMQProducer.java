package com.example.demo;

import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.common.RemotingHelper;

import java.util.Date;

/**
 * RocketMQ 消息生产者示例（阿里云版）
 *
 * 消息队列核心概念：
 *   - Producer（生产者）：负责发送消息的一方，本类即为演示示例
 *   - Consumer（消费者）：负责接收和处理消息的一方，见 RocketMQPushConsumer.java
 *   - Topic（主题）：消息的分类标签，生产者和消费者通过 Topic 对接，如 "ordermessage"
 *   - Tag（标签）：Topic 下的进一步分类，消费者可按 Tag 过滤消息
 *   - NameServer：消息队列的"路由中心"，生产者和消费者都需要连接它来发现 Broker 地址
 *   - Broker：实际存储和转发消息的服务器
 *
 * 本示例连接阿里云托管的 RocketMQ 实例，通过公网访问，需要配置身份认证（ACL）。
 */
public class RocketMQProducer {

    /**
     * 构建 ACL（Access Control List）身份认证钩子
     *
     * 公网访问阿里云 RocketMQ 必须配置此项，相当于"登录验证"。
     * SessionCredentials 中填写的是 RocketMQ 实例的用户名和密码，
     * 注意：不是阿里云账号的 AccessKey ID / AccessKey Secret，两者不同！
     *
     * 内网（ECS 同 VPC）访问时不需要此钩子，服务端会自动识别内网身份。
     *
     * @return RPCHook 认证钩子，在发送每条消息前自动附加签名信息
     */
    private static RPCHook getAclRPCHook() {
        // 第一个参数：RocketMQ 实例用户名；第二个参数：对应密码
        return new AclClientRPCHook(new SessionCredentials("OuE5tTCN9alQx8d1", "6W1A21qGZmZ8f5gC"));
    }

    /**
     * 程序入口：初始化 Producer 并连续发送 128 条消息
     *
     * 整体步骤：
     *   1. 创建 DefaultMQProducer（携带认证钩子）
     *   2. 配置 ProducerGroup、接入方式、命名空间、NameServer 地址
     *   3. 调用 producer.start() 建立连接
     *   4. 循环构建 Message 对象并发送
     *   5. 退出前调用 producer.shutdown() 释放资源
     */
    public static void main(String[] args) throws MQClientException {

        // 创建 Producer，传入认证钩子（公网访问必须）
        // 如果是 VPC 内网访问，可改用：DefaultMQProducer producer = new DefaultMQProducer();
        DefaultMQProducer producer = new DefaultMQProducer(getAclRPCHook());

        // 设置生产者组名（Producer Group），在 RocketMQ 控制台中创建
        // 同一业务的生产者归为一组，便于管理和消息追踪
        producer.setProducerGroup("GID_ordermessage_producer");

        // 指定接入方式为阿里云（CLOUD），开启云上消息轨迹功能时必须设置
        // 消息轨迹可以在控制台查看每条消息从发送到消费的完整路径，方便排查问题
        producer.setAccessChannel(AccessChannel.CLOUD);
        // 如需开启消息轨迹（仅 RocketMQ 5.x SDK 支持），取消下行注释：
        // producer.setEnableTrace(true);

        // 设置命名空间（Serverless 实例公网访问时必须设置，需要 SDK ≥ 5.2.0）
        // 命名空间用于隔离不同业务的消息，防止 Topic 名冲突
        producer.setNamespaceV2("rmq-cn-07k4rtyp802");

        // 设置 NameServer 地址（从阿里云控制台的"接入点"信息中获取）
        // 格式：域名:端口，不要加 http:// 或 https:// 前缀
        producer.setNamesrvAddr("rmq-cn-07k4rtyp802.cn-hangzhou.rmq.aliyuncs.com:8080");

        // 启动 Producer，建立到 NameServer 和 Broker 的连接
        producer.start();

        // 循环发送 128 条消息进行压力测试
        for (int i = 0; i < 128; i++) {
            try {
                // 构建消息对象：
                //   参数1：Topic（主题），消费者通过订阅相同 Topic 来接收消息
                //   参数2：Tag（标签），消费者可根据 Tag 过滤消息
                //   参数3：消息体（字节数组），这里发送字符串 "Hello world"
                Message msg = new Message(
                        "ordermessage",                                        // Topic
                        "yourMessageTagA",                                     // Tag
                        "Hello world".getBytes(RemotingHelper.DEFAULT_CHARSET) // 消息体
                );

                // 同步发送消息，返回发送结果（包含消息ID、队列信息等）
                // 同步发送会等待 Broker 确认，保证消息不丢失
                SendResult sendResult = producer.send(msg);
                System.out.printf("%s%n", sendResult); // 打印发送结果
            } catch (Exception e) {
                // 发送失败时的处理：可以记录日志后重试，或将消息持久化到数据库做补偿
                System.out.println(new Date() + " Send mq message failed.");
                e.printStackTrace();
            }
        }

        // 发送完毕后关闭 Producer，释放网络连接和线程资源
        // 注意：如果程序需要持续发送消息，不要在每次发送后 shutdown，
        //       应在应用退出时统一关闭。
        producer.shutdown();
    }
}