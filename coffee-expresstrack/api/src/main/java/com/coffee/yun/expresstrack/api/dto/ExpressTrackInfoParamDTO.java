package com.coffee.yun.expresstrack.api.dto;

import com.coffee.yun.dto.BasePageDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 快递轨迹查询请求参数 DTO
 *
 * 作用：封装查询快递轨迹所需的条件，由 coffee-app 构造后通过 Dubbo RPC 传给 coffee-expresstrack。
 *
 * 继承 BasePageDTO：自动拥有 pageNum 和 pageSize 分页字段，
 *   因为一个订单可能有多条轨迹记录（揽件→转运→派送→签收），需要分页展示。
 *
 * 实现 Serializable：Dubbo RPC 网络传输必须序列化。
 *
 * 调用链路示例：
 *   1. 用户查询订单 ORDER001
 *   2. coffee-app 先调用订单服务拿到 order_id = "ORDER001"
 *   3. 构造本 DTO：setOrder_id("ORDER001")
 *   4. 调用快递轨迹服务，传入本 DTO，查询该订单的所有轨迹
 */
@Setter
@Getter
public class ExpressTrackInfoParamDTO extends BasePageDTO implements Serializable {

    // 订单编号，作为查询快递轨迹的关联条件（与订单服务返回的 order_id 一致）
    private String order_id;

}
