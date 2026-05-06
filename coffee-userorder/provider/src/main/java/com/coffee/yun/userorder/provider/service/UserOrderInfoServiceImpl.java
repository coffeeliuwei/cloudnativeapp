package com.coffee.yun.userorder.provider.service;

import com.alibaba.fastjson2.JSON;
import com.coffee.yun.dto.PageDTO;
import com.coffee.yun.userorder.api.dto.UserOrderInfoParamDTO;
import com.coffee.yun.userorder.api.dto.UserOrderInfoResultDTO;
import com.coffee.yun.userorder.api.service.UserOrderInfoService;
import com.coffee.yun.userorder.provider.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 用户订单服务实现类
 *
 * 作用：实现 UserOrderInfoService 接口，连接 userordertest 数据库执行实际查询。
 *
 * @DubboService：
 *   将本类注册为 Dubbo 服务提供者（Provider）。
 *   服务启动时，Dubbo 自动将本服务的地址（IP + 端口）注册到 Nacos。
 *   coffee-app 通过 @DubboReference 就能发现并远程调用本类的方法。
 *
 * @Slf4j：Lombok 自动生成 log 日志对象，用于记录关键操作日志。
 */
@Slf4j
@DubboService
public class UserOrderInfoServiceImpl implements UserOrderInfoService {

    // MyBatis SQL 执行模板，由 Spring 自动注入，直接执行 Mapper 中定义的 SQL 语句
    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    // 分页查询工具，封装了 PageHelper 的分页逻辑
    @Autowired
    private PageUtil pageUtil;

    /**
     * 根据查询条件查询单条订单信息
     *
     * 实现逻辑：
     *   调用 MyBatis 的 selectOne 方法，执行 UserOrderMapper.xml 中 id="selectByParam" 的 SQL，
     *   将 userOrderInfoParamDTO 作为查询条件传入，返回第一条匹配记录。
     *
     * 使用场景：coffee-app 查询订单后，用返回的 order_id 去查快递轨迹。
     */
    @Override
    public UserOrderInfoResultDTO findUserOrderInfo(UserOrderInfoParamDTO userOrderInfoParamDTO) {
        log.info("订单查询 根据用户订单ID查询订单详情：{}", JSON.toJSONString(userOrderInfoParamDTO));
        // "UserOrderMapper.selectByParam" 对应 UserOrderMapper.xml 中 id="selectByParam" 的 SQL
        return sqlSessionTemplate.selectOne("UserOrderMapper.selectByParam", userOrderInfoParamDTO);
    }

    /**
     * 根据查询条件分页查询订单列表
     *
     * 实现逻辑：
     *   委托给 PageUtil.selectPage()，由 PageHelper 自动拦截 SQL 并追加 LIMIT 分页子句。
     *
     * 使用场景：前端订单列表页展示多条订单，支持翻页。
     */
    @Override
    public PageDTO<UserOrderInfoResultDTO> findUserOrderInfos(UserOrderInfoParamDTO userOrderInfoParamDTO) {
        return pageUtil.selectPage("UserOrderMapper.selectByParam", userOrderInfoParamDTO);
    }
}
