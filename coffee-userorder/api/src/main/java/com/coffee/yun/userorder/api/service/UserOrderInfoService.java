package com.coffee.yun.userorder.api.service;

import com.coffee.yun.dto.PageDTO;
import com.coffee.yun.userorder.api.dto.UserOrderCreateDTO;
import com.coffee.yun.userorder.api.dto.UserOrderInfoParamDTO;
import com.coffee.yun.userorder.api.dto.UserOrderInfoResultDTO;

/**
 * 用户订单服务接口（Dubbo RPC 接口定义）
 *
 * 这是一个"合同"接口，定义了订单服务能做什么，但不包含任何实现代码。
 *
 * 模块职责分工：
 *   - 本接口（api 模块）：只定义方法签名，打包成 JAR 供调用方引用
 *   - UserOrderInfoServiceImpl（provider 模块）：实现本接口，连接数据库执行查询
 *   - coffee-app：通过 @DubboReference 注入本接口，像调用本地方法一样远程调用 provider
 *
 * 为什么 api 和 provider 要分开：
 *   coffee-app 只依赖 api（接口），不依赖 provider（实现）。
 *   这样 provider 的实现细节（数据库类型、查询方式）可以随时更换，coffee-app 无需改动。
 *   这是"面向接口编程"和"依赖倒置原则"的体现。
 */
public interface UserOrderInfoService {

    /**
     * 根据查询条件查询单条订单信息
     *
     * 使用场景：coffee-app 先调用此方法拿到 order_id，再去查快递轨迹。
     *
     * @param userOrderInfoParamDTO 查询条件（可按 order_id、member_name 等字段查询）
     * @return 匹配的订单信息，包含 order_id、member_name、order_status 等字段
     */
    UserOrderInfoResultDTO findUserOrderInfo(UserOrderInfoParamDTO userOrderInfoParamDTO);

    /**
     * 根据查询条件分页查询订单列表
     *
     * 使用场景：前端订单列表页，需要展示多条订单并支持翻页。
     *
     * @param userOrderInfoParamDTO 查询条件，其中 pageNum 和 pageSize 控制分页
     * @return 分页结果，包含总记录数、总页数、当前页订单列表
     */
    PageDTO<UserOrderInfoResultDTO> findUserOrderInfos(UserOrderInfoParamDTO userOrderInfoParamDTO);

    /**
     * 创建订单（RocketMQ 生产者入口）
     *
     * 完整流程：
     *   1. 将订单写入 MySQL userordertest.order 表
     *   2. 向 RocketMQ topic "order-created" 发布消息（携带 order_id）
     *   3. coffee-expresstrack 消费该消息，异步创建对应快递单
     *
     * 这是异步解耦的典型示例：
     *   - 订单服务不直接调用快递服务，两者通过消息队列解耦
     *   - 即使快递服务临时不可用，订单仍可成功创建，消息会等待消费
     *
     * @param createDTO 创建参数，包含 order_id、OneID、order_amount
     * @return 创建成功的 order_id
     */
    String createOrder(UserOrderCreateDTO createDTO);

}
