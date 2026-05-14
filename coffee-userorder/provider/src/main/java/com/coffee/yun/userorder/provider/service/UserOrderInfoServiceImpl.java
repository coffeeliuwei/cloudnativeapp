package com.coffee.yun.userorder.provider.service;

import com.alibaba.fastjson2.JSON;
import com.coffee.yun.dto.PageDTO;
import com.coffee.yun.userorder.api.dto.UserOrderCreateDTO;
import com.coffee.yun.userorder.api.dto.UserOrderInfoParamDTO;
import com.coffee.yun.userorder.api.dto.UserOrderInfoResultDTO;
import com.coffee.yun.userorder.api.service.UserOrderInfoService;
import com.coffee.yun.userorder.provider.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户订单服务实现类
 *
 * 实现 UserOrderInfoService 接口，处理订单查询和订单创建。
 * 通过 MyBatis 操作 MySQL 数据库（userordertest），实现数据持久化。
 *
 * @DubboService 将本类注册为 Dubbo RPC 服务提供者，
 *   coffee-app 通过 Dubbo 远程调用此接口的方法。
 * @Slf4j Lombok 自动生成 log 字段，用于输出日志。
 */
@Slf4j
@DubboService
public class UserOrderInfoServiceImpl implements UserOrderInfoService {

    // MyBatis 会话模板，用于执行 SQL 操作
    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    // 分页工具，封装了 PageHelper 插件的分页查询逻辑
    @Autowired
    private PageUtil pageUtil;

    /**
     * 查询单条订单信息
     *
     * @param dto 查询参数，包含 order_id 等条件
     * @return 订单详情，若不存在返回 null
     */
    @Override
    public UserOrderInfoResultDTO findUserOrderInfo(UserOrderInfoParamDTO dto) {
        log.info("订单查询：{}", JSON.toJSONString(dto));
        return sqlSessionTemplate.selectOne("UserOrderMapper.selectByParam", dto);
    }

    /**
     * 分页查询订单列表
     *
     * @param dto 查询参数，包含分页参数（pageNum/pageSize）和过滤条件
     * @return 分页结果，包含 total（总条数）和当前页订单列表
     */
    @Override
    public PageDTO<UserOrderInfoResultDTO> findUserOrderInfos(UserOrderInfoParamDTO dto) {
        return pageUtil.selectPage("UserOrderMapper.selectByParam", dto);
    }

    /**
     * 创建订单（写入 order 表）
     *
     * 只负责将订单数据写入数据库并返回 orderId。
     * 快递单的创建由 coffee-app 在调用本方法之后，再通过 Dubbo RPC 调用
     * coffee-expresstrack.createExpress(orderId) 同步完成。
     *
     * @param createDTO 创建订单的请求参数
     * @return 订单编号（即传入的 order_id）
     */
    @Override
    public String createOrder(UserOrderCreateDTO createDTO) {
        Map<String, Object> params = new HashMap<>();
        params.put("order_id",     createDTO.getOrder_id());
        params.put("OneID",        createDTO.getOneID());
        params.put("order_amount", createDTO.getOrder_amount());
        params.put("order_status", "待发货");
        sqlSessionTemplate.insert("UserOrderMapper.insertOrder", params);
        log.info("订单已创建，order_id={}", createDTO.getOrder_id());
        return createDTO.getOrder_id();
    }
}
