package com.coffee.yun.expresstrack.provider.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 序列化配置
 *
 * ── 为什么需要这个配置类？ ──────────────────────────────────────────────
 * Spring Data Redis 提供了开箱即用的 RedisTemplate<Object, Object>，
 * 但它默认使用 JDK 序列化（JdkSerializationRedisSerializer），存入 Redis
 * 的 key 和 value 都是二进制字节流，例如：
 *
 *   key  →  \xac\xed\x00\x05t\x00\x12express:track:001
 *   value → \xac\xed\x00\x05sr\x00...（无法阅读）
 *
 * 这带来两个问题：
 *   1. 在 Redis 控制台（redis-cli / RedisInsight）无法直接查看内容，调试困难
 *   2. 其他语言（Python、Node.js）的客户端无法读取这些数据
 *
 * 本配置将序列化方案改为：
 *   key   → StringRedisSerializer   存储为可读字符串，如 "express:track:001"
 *   value → Jackson2JsonRedisSerializer  存储为 JSON（含类型信息）
 *
 * ── 注意：为什么不用 @EnableCaching + @Cacheable 注解方式？ ──────────────
 * 注解方式（Spring Cache Abstraction）虽然更简洁，但封装过深，初学者难以理解
 * 缓存的工作原理。本项目使用手动 RedisTemplate，让缓存逻辑完全可见，
 * 便于课堂演示"旁路缓存（Cache-Aside Pattern）"的每一步执行过程。
 *
 * @Configuration  告诉 Spring 这是一个配置类，类中的 @Bean 方法会被 Spring 容器管理
 */
@Configuration
public class RedisConfig {

    /**
     * 自定义 RedisTemplate Bean，替换 Spring Boot 自动装配的默认实例
     *
     * Spring Boot 在检测到 classpath 中有 spring-data-redis 时，会自动创建
     * RedisTemplate<Object, Object>（key/value 都是 Object，使用 JDK 序列化）。
     * 此处声明了同名 Bean（返回类型更具体：RedisTemplate<String, Object>），
     * Spring 容器会优先使用我们手动定义的这个，替换自动装配版本。
     *
     * @param factory  Redis 连接工厂，由 spring.redis.* 配置自动创建（Spring Boot 自动装配）
     *                 包含连接池、主机地址、端口、密码等信息
     * @return  配置好序列化策略的 RedisTemplate，供 @Autowired 注入到 Service 使用
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        // 创建 RedisTemplate 实例，泛型 <String, Object> 表示：
        //   - key 必须是 String 类型（如 "express:track:001"）
        //   - value 可以是任意 Object（如 PageDTO<ExpressTrackInfoResultDTO> 对象）
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 绑定 Redis 连接工厂：所有 Redis 操作都通过这个工厂建立的连接执行
        template.setConnectionFactory(factory);

        // ── 配置 JSON 序列化器（用于 value）────────────────────────────────
        // Jackson 的核心对象，负责 Java 对象 ↔ JSON 字符串 的相互转换
        ObjectMapper om = new ObjectMapper();

        // 设置可见性：允许 Jackson 访问所有字段（包括 private），
        // 不要求必须有 getter/setter 方法，避免因 Lombok 生成代码时序问题导致序列化失败
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 开启"默认类型"：在 JSON 中嵌入 Java 类型信息，例如：
        //   存入 Redis 的 JSON：["com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoResultDTO",{...}]
        // 反序列化时 Jackson 根据这个类型信息还原为正确的 Java 对象，
        // 否则从 Redis 取出后只能得到 LinkedHashMap，需要手动转换
        // NON_FINAL 表示对非 final 类型都加入类型信息
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);

        // 用配置好的 ObjectMapper 创建 JSON 序列化器
        // Object.class 表示可以序列化任意类型的对象
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(Object.class);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        // ── 配置 String 序列化器（用于 key）────────────────────────────────
        // StringRedisSerializer 直接将 String 存为 UTF-8 字节，无任何额外格式，
        // 结果与直接在 redis-cli 中输入 key 完全相同，便于控制台查询
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // ── 将序列化器分别绑定到 key/value/hashKey/hashValue ──────────────
        // setKeySerializer：普通 key-value 结构（opsForValue）的 key 序列化
        template.setKeySerializer(stringRedisSerializer);
        // setHashKeySerializer：Hash 结构（opsForHash）的 field 序列化
        template.setHashKeySerializer(stringRedisSerializer);
        // setValueSerializer：普通 key-value 结构的 value 序列化
        template.setValueSerializer(jackson2JsonRedisSerializer);
        // setHashValueSerializer：Hash 结构的 value 序列化
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        // 触发 InitializingBean 回调，完成内部初始化（必须在所有 setter 调用完后执行）
        template.afterPropertiesSet();
        return template;
    }
}
