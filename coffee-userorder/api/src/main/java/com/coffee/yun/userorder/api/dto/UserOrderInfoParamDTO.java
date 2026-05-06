package com.coffee.yun.userorder.api.dto;

import com.coffee.yun.dto.BasePageDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 订单查询请求参数 DTO
 *
 * 作用：封装前端传来的订单查询条件。
 *   继承 BasePageDTO，自动拥有 pageNum 和 pageSize 分页字段。
 *   实现 Serializable，支持 Dubbo RPC 网络传输。
 *
 * 查询逻辑：字段之间是"AND"关系，传哪个字段就按哪个字段过滤，不传则不过滤。
 *
 * 使用示例（前端 POST 请求体）：
 *   按订单号查询：{ "order_id": "ORDER001", "pageNum": 1, "pageSize": 10 }
 *   按用户名查询：{ "member_name": "张三", "pageNum": 1, "pageSize": 10 }
 *
 * @Getter / @Setter 是 Lombok 注解，编译时自动生成所有字段的 getter 和 setter 方法，
 *   无需手动写 getOrderId()/setOrderId() 等样板代码。
 */
@Getter
@Setter
public class UserOrderInfoParamDTO extends BasePageDTO implements Serializable {

    // 用户唯一标识（对应数据库 member 表的 OneID 字段）
    private String OneID;

    // 用户姓名（模糊查询条件）
    private String member_name;

    // 订单编号（精确查询条件）
    private String order_id;

    // 订单状态，如"已发货""已签收"（精确查询条件）
    private String order_status;

}
