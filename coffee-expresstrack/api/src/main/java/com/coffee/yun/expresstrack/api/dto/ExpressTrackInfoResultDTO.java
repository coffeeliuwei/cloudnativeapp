package com.coffee.yun.expresstrack.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 快递轨迹查询响应结果 DTO
 *
 * 作用：封装从 expresstracktest 数据库查出的快递轨迹数据，
 *   通过 Dubbo RPC 返回给 coffee-app，再由 coffee-app 以 JSON 格式响应给前端。
 *
 * 数据来源：由 express 表和 track 表 LEFT JOIN 查询得到，
 *   一条记录代表快递的一个物流节点（如"已揽件""运输中""已到达派送站"）。
 *
 * 实现 Serializable：Dubbo RPC 网络传输必须序列化。
 *
 * 前端收到的 JSON 列表示例（list 中的一条数据）：
 *   {
 *     "order_id": "ORDER001",
 *     "express_id": "SF123456",
 *     "express_weight": "2.5kg",
 *     "track_id": "T001",
 *     "track_show": "2024-01-10 10:00 已从北京仓库发出"
 *   }
 */
@Setter
@Getter
public class ExpressTrackInfoResultDTO implements Serializable {

    // 订单编号（关联字段，方便前端对照展示）
    private String order_id;

    // 快递单号（如顺丰、圆通的运单编号）
    private String express_id;

    // 包裹重量
    private String express_weight;

    // 轨迹记录编号（数据库主键）
    private String track_id;

    // 轨迹描述文本，显示在前端时间轴上（如"已揽件""运输中""派送中""已签收"）
    private String track_show;

}
