package com.coffee.yun.userorder.provider.config;

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
 * Redis 配置类
 *
 * 作用：自定义 RedisTemplate 的序列化方式。
 *
 * 为什么要自定义？
 *   Spring Boot 默认的 RedisTemplate 使用 JDK 序列化（二进制），
 *   存入 Redis 的数据不可读（乱码），也无法跨语言共享。
 *   改为 JSON 序列化后：
 *   - Redis 中存储的是可读的 JSON 字符串，便于调试
 *   - key 为普通字符串（如 "order:detail:ORDER001"），清晰直观
 *   - value 包含类型信息，反序列化时能还原为正确的 Java 对象
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // key 使用 String 序列化，Redis 中可直接看到 key 名称
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // value 使用 Jackson JSON 序列化，Redis 中存储的是可读的 JSON 字符串
        ObjectMapper objectMapper = new ObjectMapper();
        // 序列化所有字段（包括 private 字段），不依赖 getter
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 在 JSON 中写入类型信息（如 "@class":"com.coffee.yun...DTO"），反序列化时能还原正确类型
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        Jackson2JsonRedisSerializer<Object> jsonSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
