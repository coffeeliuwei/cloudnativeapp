package com.coffee.yun.expresstrack.api.service;

import com.coffee.yun.dto.PageDTO;
import com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoParamDTO;
import com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoResultDTO;

/**
 * 快递轨迹服务接口（Dubbo RPC 接口定义）
 *
 * 这是一个"合同"接口，定义了快递轨迹服务能做什么，不包含任何实现代码。
 *
 * 模块职责分工：
 *   - 本接口（api 模块）：只定义方法签名，打包成 JAR 供调用方引用
 *   - ExpresstrackInfoServiceImpl（provider 模块）：实现本接口，连接 expresstracktest 数据库
 *   - coffee-app：通过 @DubboReference 注入本接口，发起远程调用
 *
 * Dubbo 工作原理：
 *   coffee-app 编译时只看到这个接口（知道"能做什么"）。
 *   运行时 Dubbo 从 Nacos 找到 coffee-expresstrack 的地址，
 *   将调用请求序列化后通过 TCP 发送过去，对调用方来说就像本地调用一样透明。
 */
public interface ExpressTrackInfoService {

    /**
     * 根据订单编号分页查询快递轨迹列表
     *
     * 使用场景：coffee-app 拿到订单的 order_id 后，调用此方法查询对应的物流轨迹。
     *
     * @param expressTrackInfoParamDTO 查询条件，包含 order_id 和分页参数（pageNum、pageSize）
     * @return 分页结果，list 中每条数据代表一个物流节点（如揽件、转运、签收）
     */
    PageDTO<ExpressTrackInfoResultDTO> findExpressTrackInfos(ExpressTrackInfoParamDTO expressTrackInfoParamDTO);

    /**
     * 为新订单同步创建快递单和初始轨迹记录
     *
     * 调用时机：coffee-app 在 /createOrder 接口中，订单写入 MySQL 后通过 Dubbo RPC 同步调用此方法。
     * 执行两步写库：
     *   1. INSERT INTO express（快递单基本信息，express_id 在此方法内自动生成）
     *   2. INSERT INTO track（第一条轨迹，内容固定为"商家已揽件"）
     *
     * @param orderId 刚创建的订单编号，用于关联 express 表中的 order_id 字段
     */
    void createExpress(String orderId);
}
