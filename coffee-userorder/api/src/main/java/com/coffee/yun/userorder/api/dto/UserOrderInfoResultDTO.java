package com.coffee.yun.userorder.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 订单查询响应结果 DTO
 *
 * 作用：封装从数据库查出的订单数据，通过 Dubbo RPC 返回给 coffee-app，
 *   再由 coffee-app 转换为 JSON 响应给前端。
 *
 * 注意：只包含前端需要展示的字段，数据库中的敏感字段（如密码）不在此处暴露。
 *
 * 实现 Serializable 的原因：Dubbo RPC 传输对象必须可序列化。
 *
 * 前端收到的 JSON 示例：
 *   {
 *     "order_id": "ORDER001",
 *     "OneID": "U001",
 *     "member_phone": "138****8888",
 *     "member_name": "张三",
 *     "order_status": "已发货",
 *     "order_amount": 199.00
 *   }
 */
@Getter
@Setter
public class UserOrderInfoResultDTO implements Serializable {

    // 订单编号，作为快递轨迹查询的关联键（查完订单后用此字段查轨迹）
    private String order_id;

    // 用户唯一标识
    private String OneID;

    // 用户手机号
    private String member_phone;

    // 用户姓名
    private String member_name;

    // 订单状态，如"待发货""已发货""已签收"
    private String order_status;

    // 订单金额
    private float order_amount;

}
