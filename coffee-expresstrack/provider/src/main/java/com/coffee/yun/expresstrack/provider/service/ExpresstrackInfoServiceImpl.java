package com.coffee.yun.expresstrack.provider.service;

import com.coffee.yun.dto.PageDTO;
import com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoParamDTO;
import com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoResultDTO;
import com.coffee.yun.expresstrack.api.service.ExpressTrackInfoService;
import com.coffee.yun.expresstrack.provider.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 快递轨迹服务实现类
 *
 * 实现 ExpressTrackInfoService 接口，处理快递轨迹的查询和快递单的创建。
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

    // 执行非分页 SQL（INSERT/UPDATE/DELETE/selectOne/selectList）
    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

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

    /**
     * 为新订单同步创建快递单和初始轨迹记录
     *
     * 被 coffee-app 的 /createOrder 接口通过 Dubbo RPC 调用，在订单落库之后紧接着执行。
     * 两步写库操作在同一次 RPC 调用中完成，对调用方而言是同步的。
     *
     * @param orderId 刚创建的订单编号
     */
    @Override
    public void createExpress(String orderId) {
        // Step 1：生成快递单号（16位随机字符串），写入 express 表
        // UUID 去掉中划线后取前 16 位，足够唯一且长度固定
        String expressId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        Map<String, Object> expressParams = new HashMap<>();
        expressParams.put("express_id",     expressId);
        expressParams.put("order_id",       orderId);
        expressParams.put("express_weight", "1kg");   // 默认重量，实际场景可由调用方传入
        sqlSessionTemplate.insert("ExpressTrackMapper.insertExpress", expressParams);
        log.info("快递单已创建，order_id={}，express_id={}", orderId, expressId);

        // Step 2：写入第一条轨迹记录（"商家已揽件"）
        // track_id 同样用 UUID 生成，确保唯一
        String trackId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        Map<String, Object> trackParams = new HashMap<>();
        trackParams.put("track_id",   trackId);
        trackParams.put("express_id", expressId);
        trackParams.put("track_show", "商家已揽件");
        sqlSessionTemplate.insert("ExpressTrackMapper.insertTrack", trackParams);
        log.info("初始轨迹已创建，express_id={}，状态=商家已揽件", expressId);
    }
}
