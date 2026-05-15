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
 * 实现 UserOrderInfoService 接口，处理订单的查询和创建。
 * 通过 MyBatis 操作 MySQL 数据库，实现数据持久化。
 *
 * @DubboService 将本类注册为 Dubbo RPC 服务提供者，
 *   coffee-app 通过 Dubbo 远程调用此接口的方法。
 * @Slf4j Lombok 自动生成 log 字段，用于输出日志。
 */
@Slf4j
@DubboService
public class UserOrderInfoServiceImpl implements UserOrderInfoService {

    // MyBatis 的 SqlSessionTemplate 是线程安全的，封装了 SqlSession 的创建和关闭，
    // 直接注入即可用于执行 mapper/*.xml 中定义的 SQL 语句
    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    // 分页工具，封装了 PageHelper 插件的分页查询逻辑
    @Autowired
    private PageUtil pageUtil;

    /**
     * 根据查询条件查询单条订单信息
     *
     * 直接查询数据库，返回匹配的订单记录。
     *
     * @param userOrderInfoParamDTO 查询条件（order_id、member_name 等）
     * @return 订单信息，若查询无结果则返回 null
     */
    @Override
    public UserOrderInfoResultDTO findUserOrderInfo(UserOrderInfoParamDTO userOrderInfoParamDTO) {
        log.info("订单查询：{}", JSON.toJSONString(userOrderInfoParamDTO));
        // selectOne：执行 UserOrderMapper.xml 中 id="selectByParam" 的 SQL，返回单条记录
        return sqlSessionTemplate.selectOne("UserOrderMapper.selectByParam", userOrderInfoParamDTO);
    }

    /**
     * 根据查询条件分页查询订单列表
     *
     * 使用 PageHelper 分页插件，根据 pageNum 和 pageSize 参数自动分页。
     *
     * @param userOrderInfoParamDTO 查询条件，pageUtil 内部读取 pageNum/pageSize 参数
     * @return 分页结果，包含 total（总条数）和 records（当前页数据列表）
     */
    @Override
    public PageDTO<UserOrderInfoResultDTO> findUserOrderInfos(UserOrderInfoParamDTO userOrderInfoParamDTO) {
        // 直接调用 PageUtil 执行分页查询，分页参数由 pageUtil 内部通过 PageHelper 注入
        return pageUtil.selectPage("UserOrderMapper.selectByParam", userOrderInfoParamDTO);
    }

    /**
     * 创建订单
     *
     * 将订单数据写入数据库（order 表），返回生成的订单 ID。
     *
     * @param createDTO 创建订单的参数（order_id、OneID、order_amount）
     * @return 成功创建的 order_id
     */
    @Override
    public String createOrder(UserOrderCreateDTO createDTO) {
        log.info("创建订单：{}", JSON.toJSONString(createDTO));

        Map<String, Object> params = new HashMap<>();
        params.put("order_id", createDTO.getOrder_id());
        params.put("OneID", createDTO.getOneID());
        params.put("order_amount", createDTO.getOrder_amount());
        sqlSessionTemplate.insert("UserOrderMapper.insertOrder", params);
        log.info("订单写入数据库成功，order_id={}", createDTO.getOrder_id());

        // 返回 order_id，coffee-app 收到后回显给用户
        return createDTO.getOrder_id();
    }
}
