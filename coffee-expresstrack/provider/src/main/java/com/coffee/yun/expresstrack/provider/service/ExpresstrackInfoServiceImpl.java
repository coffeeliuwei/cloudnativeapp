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
 * ── 云原生改造说明 ────────────────────────────────────────────────────
 * 本类在原有"查询快递轨迹"功能基础上，新增了 Redis 缓存能力：
 *
 *   Redis 缓存（Cache-Aside Pattern）
 *   由 feature.cache.enabled 控制，默认 false（关闭）。
 *   关闭时：行为与改造前完全相同，直接查数据库。
 *   开启后：查询先走 Redis，命中直接返回；未命中才查 DB 并回写 Redis。
 *
 * ── 缓存 key 设计 ────────────────────────────────────────────────────
 * key 格式：express:track:{order_id}
 * 例如：express:track:ORDER20240514001
 *
 * 设计考量：
 *   1. 按订单号（order_id）作为 key 的唯一标识，与业务查询方式一致
 *   2. 缓存整个分页结果（PageDTO），下次相同参数直接命中
 *   3. 当快递轨迹更新时，只需删除对应 order_id 的 key 即可失效缓存
 *      （本项目为教学简化，未实现主动失效，依赖 TTL 自然过期）
 *
 * ── Nacos Config 热更新 ──────────────────────────────────────────────
 * 缓存 TTL 由 CacheProperties.getExpresstrackTtl() 提供，
 * CacheProperties 使用了 @RefreshScope，支持 Nacos 配置中心动态修改。
 * 在 Nacos 控制台修改 cache.ttl.expresstrack 后立即生效，无需重启。
 *
 * ── 功能开关设计原则 ──────────────────────────────────────────────────
 * Redis 在本地开发环境可能没有启动，因此：
 *   - application-dev.yml 中 feature.cache.enabled=false（默认关闭）
 *   - application-prod.yml 中 feature.cache.enabled=true（生产开启）
 * 同学们在本地不需要安装 Redis，项目依然可以正常启动和使用。
 *
 * @DubboService  将本类注册为 Dubbo RPC 服务提供者，coffee-app 通过 Dubbo 调用此接口
 * @Slf4j  Lombok 自动生成 log 字段，供日志输出使用
 */
@Slf4j
@DubboService
public class ExpresstrackInfoServiceImpl implements ExpressTrackInfoService {

    // ── 依赖注入 ──────────────────────────────────────────────────────

    // 分页工具，封装了 PageHelper 插件的分页查询逻辑
    @Autowired
    private PageUtil pageUtil;

    // 自定义的 RedisTemplate（在 RedisConfig 中定义），
    // key 为 String，value 为 JSON 格式存储的 Java 对象
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 缓存配置，提供 TTL 值（默认 3600 秒），支持 Nacos Config 热更新
    @Autowired
    private CacheProperties cacheProperties;

    // ── 功能开关 ──────────────────────────────────────────────────────

    /**
     * 是否启用 Redis 缓存
     *
     * @Value 从配置文件读取：
     *   - application-dev.yml 中 feature.cache.enabled=false → 关闭缓存
     *   - application-prod.yml 中 feature.cache.enabled=true → 开启缓存
     * 冒号后的 false 是默认值，若配置文件中没有此 key 时使用
     */
    @Value("${feature.cache.enabled:false}")
    private boolean cacheEnabled;

    /**
     * Redis key 前缀
     *
     * 命名规范：模块名:数据类型:唯一标识符
     *   "express:track:{order_id}"
     *   模块名=express，数据类型=track（轨迹），唯一标识=order_id
     *
     * 在 redis-cli 中可以用通配符查询所有轨迹缓存：
     *   KEYS express:track:*
     */
    private static final String CACHE_KEY_PREFIX = "express:track:";

    // ── 业务方法 ──────────────────────────────────────────────────────

    /**
     * 根据订单编号分页查询快递轨迹列表（带缓存的 Cache-Aside Pattern 实现）
     *
     * Cache-Aside（旁路缓存）读取流程：
     *   ① 先查 Redis（key = "express:track:{order_id}"）
     *   ② 命中（缓存存在且未过期）→ 直接返回，跳过数据库查询
     *   ③ 未命中（缓存不存在/已过期）→ 查数据库 → 写入 Redis（TTL 由 Nacos 配置）→ 返回
     *
     * 完整调用链（开启缓存，缓存命中时）：
     *   coffee-app → Dubbo RPC → 本方法 → Redis（返回）
     *
     * 完整调用链（开启缓存，缓存未命中时）：
     *   coffee-app → Dubbo RPC → 本方法 → Redis（未命中）
     *                                   → PageUtil → MyBatis → expresstracktest DB（返回）
     *                                              ↓
     *                                         写入 Redis（供下次命中）
     *
     * @param expressTrackInfoParamDTO  查询条件，包含 order_id 和分页参数（pageNum/pageSize）
     * @return  分页结果，包含 total（总条数）和 records（当前页快递轨迹列表）
     *
     * @SuppressWarnings("unchecked")
     *   从 Redis 取出的是 Object，需要强制转换为 PageDTO<ExpressTrackInfoResultDTO>，
     *   编译器会警告"未检查的类型转换"，此处确认安全（存入时已是正确类型），压制警告。
     */
    @Override
    @SuppressWarnings("unchecked")
    public PageDTO<ExpressTrackInfoResultDTO> findExpressTrackInfos(ExpressTrackInfoParamDTO expressTrackInfoParamDTO) {
        String orderId = expressTrackInfoParamDTO.getOrder_id();

        // ── 缓存路径（feature.cache.enabled=true 且有 order_id 时走此分支）─
        if (cacheEnabled && orderId != null && !orderId.isEmpty()) {
            // 拼装 Redis key，如 "express:track:ORDER20240514001"
            String cacheKey = CACHE_KEY_PREFIX + orderId;

            // opsForValue()：操作 Redis String 数据结构（最基础的 key-value）
            // get(key)：若 key 不存在或已过期，返回 null
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                // 缓存命中：直接返回，跳过数据库查询，减少 DB 压力
                log.info("缓存命中，order_id={}", orderId);
                // 强制转换安全原因：Redis 中存储时已经是 PageDTO<ExpressTrackInfoResultDTO>，
                // Jackson 序列化时记录了完整的类型信息，反序列化可正确还原
                return (PageDTO<ExpressTrackInfoResultDTO>) cached;
            }

            // 缓存未命中：查数据库
            // selectPage 内部使用 PageHelper 插件，根据参数中的 pageNum/pageSize 执行分页 SQL
            PageDTO<ExpressTrackInfoResultDTO> result =
                    pageUtil.selectPage("ExpressTrackMapper.selectByParam", expressTrackInfoParamDTO);

            // 回写 Redis（仅有数据时才缓存，避免缓存空结果占用内存）
            if (result != null) {
                // set(key, value, timeout, unit)：设置带过期时间的缓存
                // getExpresstrackTtl()：从 CacheProperties 读取，默认 3600 秒，可热更新
                redisTemplate.opsForValue().set(
                        cacheKey, result, cacheProperties.getExpresstrackTtl(), TimeUnit.SECONDS);
                log.info("写入缓存，order_id={}，TTL={}s", orderId, cacheProperties.getExpresstrackTtl());
            }
            return result;
        }

        // ── 直通路径（cacheEnabled=false 或 orderId 为空）────────────────
        // 与云原生改造前的行为完全相同，直接查数据库
        return pageUtil.selectPage("ExpressTrackMapper.selectByParam", expressTrackInfoParamDTO);
    }
}
