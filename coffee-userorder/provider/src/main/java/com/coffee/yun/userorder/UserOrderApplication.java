package com.coffee.yun.userorder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 用户订单微服务启动类
 *
 * 作用：启动 coffee-userorder 服务进程，初始化 Spring 容器、连接数据库、向 Nacos 注册服务。
 *
 * @SpringBootApplication 是一个组合注解，等价于同时使用以下三个注解：
 *   - @SpringBootConfiguration：标记这是 Spring Boot 配置类
 *   - @EnableAutoConfiguration：启用自动配置（根据依赖自动配置 MyBatis、Dubbo 等）
 *   - @ComponentScan：扫描当前包及子包下所有 @Service、@Component 等注解的类并注册为 Bean
 *
 * 启动后的效果：
 *   1. MyBatis 连接 userordertest 数据库
 *   2. Dubbo 将 UserOrderInfoServiceImpl 注册到 Nacos（端口 20880）
 *   3. HTTP 服务在端口 7001 上监听（用于健康检查等）
 */
@SpringBootApplication
public class UserOrderApplication {

    public static void main(String[] args) {
        // SpringApplication.run() 启动整个 Spring Boot 应用
        SpringApplication.run(UserOrderApplication.class, args);
    }

}
