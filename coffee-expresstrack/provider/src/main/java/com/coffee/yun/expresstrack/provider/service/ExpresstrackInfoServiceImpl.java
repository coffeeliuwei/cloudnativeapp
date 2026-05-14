package com.coffee.yun.expresstrack.provider.service;

import com.coffee.yun.dto.PageDTO;
import com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoParamDTO;
import com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoResultDTO;
import com.coffee.yun.expresstrack.api.service.ExpressTrackInfoService;
import com.coffee.yun.expresstrack.provider.config.CacheProperties;
import com.coffee.yun.expresstrack.provider.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 快递轨迹服务实现类
 *
 * 新增能力（云原生改造）：
 *   Redis 缓存：由 feature.cache.enabled 控制，默认关闭。
 *     关闭时行为与改造前完全相同，直接查数据库。
 *     开启后：先查 Redis，命中直接返回；未命中查 DB 并写入 Redis。
 *
 * 缓存 key 设计：express:track:{order_id}
 *   按订单号缓存整个分页结果，粒度清晰，失效也按订单粒度清除。
 *
 * 缓存 TTL 由 CacheProperties 提供，通过 Nacos Config 可热更新，
 *   无需重启服务即可调整缓存策略。
 *
 * 功能开关在 application-dev.yml 中配置：
 *   feature.cache.enabled=false  （默认关闭，保持原项目可正常启动）
 */
@Slf4j
@DubboService
public class ExpresstrackInfoServiceImpl implements ExpressTrackInfoService {

    @Autowired
    private PageUtil pageUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheProperties cacheProperties;

    /** 是否启用 Redis 缓存，由 application-dev.yml 中 feature.cache.enabled 控制 */
    @Value("${feature.cache.enabled:false}")
    private boolean cacheEnabled;

    /** Redis key 前缀，格式：express:track:{order_id} */
    private static final String CACHE_KEY_PREFIX = "express:track:";

    /**
     * 根据订单编号分页查询快递轨迹列表
     *
     * 缓存策略（仅 feature.cache.enabled=true 时生效，Cache-Aside Pattern）：
     *   1. 先查 Redis（key = "express:track:{order_id}"）
     *   2. 命中 → 直接返回，跳过数据库查询
     *   3. 未命中 → 查数据库 → 写入 Redis（TTL 由 Nacos Config 控制）→ 返回
     *
     * 调用链：
     *   coffee-app → (Dubbo RPC) → 本方法 → Redis（命中则返回）
     *                                      → PageUtil → MyBatis → expresstracktest DB
     */
    @Override
    @SuppressWarnings("unchecked")
    public PageDTO<ExpressTrackInfoResultDTO> findExpressTrackInfos(ExpressTrackInfoParamDTO expressTrackInfoParamDTO) {
        String orderId = expressTrackInfoParamDTO.getOrder_id();

        if (cacheEnabled && orderId != null && !orderId.isEmpty()) {
            String cacheKey = CACHE_KEY_PREFIX + orderId;

            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.info("缓存命中，order_id={}", orderId);
                return (PageDTO<ExpressTrackInfoResultDTO>) cached;
            }

            PageDTO<ExpressTrackInfoResultDTO> result =
                    pageUtil.selectPage("ExpressTrackMapper.selectByParam", expressTrackInfoParamDTO);

            if (result != null) {
                redisTemplate.opsForValue().set(
                        cacheKey, result, cacheProperties.getExpresstrackTtl(), TimeUnit.SECONDS);
                log.info("写入缓存，order_id={}，TTL={}s", orderId, cacheProperties.getExpresstrackTtl());
            }
            return result;
        }

        return pageUtil.selectPage("ExpressTrackMapper.selectByParam", expressTrackInfoParamDTO);
    }
}
