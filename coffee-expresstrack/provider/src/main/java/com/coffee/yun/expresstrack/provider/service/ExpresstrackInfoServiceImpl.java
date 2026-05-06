package com.coffee.yun.expresstrack.provider.service;

import com.coffee.yun.dto.PageDTO;
import com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoParamDTO;
import com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoResultDTO;
import com.coffee.yun.expresstrack.api.service.ExpressTrackInfoService;
import com.coffee.yun.expresstrack.provider.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 快递轨迹服务实现类
 *
 * 作用：实现 ExpressTrackInfoService 接口，连接 expresstracktest 数据库执行实际查询。
 *
 * @DubboService：
 *   将本类注册为 Dubbo 服务提供者（Provider）。
 *   服务启动时，Dubbo 自动将本服务的 IP 和端口（28888）注册到 Nacos。
 *   coffee-app 通过 @DubboReference 发现并远程调用本类的方法。
 *
 * @Slf4j：Lombok 自动生成 log 日志对象，用于记录操作日志。
 */
@Slf4j
@DubboService
public class ExpresstrackInfoServiceImpl implements ExpressTrackInfoService {

    // 分页查询工具，封装了 PageHelper 的分页逻辑，通过 @Autowired 由 Spring 自动注入
    @Autowired
    private PageUtil pageUtil;

    /**
     * 根据订单编号分页查询快递轨迹列表
     *
     * 实现逻辑：
     *   委托给 PageUtil.selectPage()，执行 ExpressTrackMapper.xml 中 id="selectByParam" 的 SQL。
     *   SQL 通过 order_id 关联 express 表和 track 表，查询该订单的所有物流节点。
     *
     * 调用链：
     *   coffee-app → (Dubbo RPC) → 本方法 → PageUtil → MyBatis → expresstracktest 数据库
     */
    @Override
    public PageDTO<ExpressTrackInfoResultDTO> findExpressTrackInfos(ExpressTrackInfoParamDTO expressTrackInfoParamDTO) {
        // "ExpressTrackMapper.selectByParam" 对应 ExpressTrackMapper.xml 中 id="selectByParam" 的 SQL
        return pageUtil.selectPage("ExpressTrackMapper.selectByParam", expressTrackInfoParamDTO);
    }
}
