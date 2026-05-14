package com.coffee.yun.userorder.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 创建订单请求参数（Dubbo RPC 传输对象）
 *
 * 实现 Serializable 是 Dubbo 序列化的必要条件。
 * 调用方：coffee-app POST /createOrder
 */
@Getter
@Setter
public class UserOrderCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String order_id;
    private String OneID;
    private float order_amount;
}
