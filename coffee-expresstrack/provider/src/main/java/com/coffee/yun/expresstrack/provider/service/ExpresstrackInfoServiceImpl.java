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
 * 实现 ExpressTrackInfoService 接口，处理快递轨迹的查询。
 * 通过 MyBatis 操作 MySQL 数据库，实现数据持久化。
 *
 * @DubboService 将本类注册为 Dubbo RPC 服务提供者，
 *   coffee-app 通过 Dubbo 远程调用此接口的方法。
 * @Slf4j Lombok 自动生成 log 字段，用于输出日志。
 */
@Slf4j
@DubboService
public class ExpresstrackInfoServiceImpl implements ExpressTrackInfoService {

    // 分页工具，封装了 PageHelper 插件的分页查询逻辑
    @Autowired
    private PageUtil pageUtil;

    /**
     * 根据订单编号分页查询快递轨迹列表
     *
     * 使用 PageHelper 分页插件，根据 pageNum 和 pageSize 参数自动分页。
     *
     * @param expressTrackInfoParamDTO 查询条件，包含 order_id 和分页参数（pageNum/pageSize）
     * @return 分页结果，包含 total（总条数）和 records（当前页快递轨迹列表）
     */
    @Override
    public PageDTO<ExpressTrackInfoResultDTO> findExpressTrackInfos(ExpressTrackInfoParamDTO expressTrackInfoParamDTO) {
        // selectPage 内部使用 PageHelper 插件，根据参数中的 pageNum/pageSize 执行分页 SQL
        return pageUtil.selectPage("ExpressTrackMapper.selectByParam", expressTrackInfoParamDTO);
    }
}
