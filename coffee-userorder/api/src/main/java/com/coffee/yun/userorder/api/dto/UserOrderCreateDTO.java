package com.coffee.yun.userorder.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 创建订单请求参数 DTO
 *
 * 作用：封装前端传来的订单创建数据，通过 Dubbo RPC 传递给 coffee-userorder 服务。
 *
 * 使用场景（RocketMQ 演示流程）：
 *   1. 前端 POST /createOrder，携带此 DTO
 *   2. coffee-app 通过 Dubbo RPC 调用 UserOrderInfoService.createOrder()
 *   3. coffee-userorder 将订单写入 MySQL，并发布 "order-created" 消息到 RocketMQ
 *   4. coffee-expresstrack 消费此消息，异步创建快递单
 *
 * 请求体示例：
 *   {
 *     "order_id": "ORDER100",
 *     "OneID": "U001",
 *     "order_amount": 199.00
 *   }
 */
@Getter
@Setter
public class UserOrderCreateDTO implements Serializable {

    // 订单编号（建议全局唯一，如 UUID 或雪花ID）
    private String order_id;

    // 用户唯一标识（对应 member 表的 OneID）
    private String OneID;

    // 订单金额
    private float order_amount;
}
