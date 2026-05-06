package com.coffee.yun.coffeeapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * API 网关服务启动类
 *
 * 作用：启动 coffee-app 服务进程，初始化 Spring 容器、连接 Nacos 发现其他服务。
 *
 * exclude = {DataSourceAutoConfiguration.class} 的原因：
 *   coffee-app 是纯网关层，不直接操作数据库，无需配置数据源（DataSource）。
 *   如果不排除，Spring Boot 启动时会尝试自动配置数据库连接，
 *   因为没有配置数据库 URL，会导致启动失败并报错：
 *   "Failed to configure a DataSource: 'url' attribute is not specified"
 *
 * 启动后的效果：
 *   1. Dubbo 消费者初始化，从 Nacos 发现 coffee-userorder 和 coffee-expresstrack 的地址
 *   2. CoffeeController 就绪，开始在端口 8005 监听前端的 HTTP 请求
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class CoffeeAppApplication {

    public static void main(String[] args) {
        // SpringApplication.run() 启动整个 Spring Boot 应用
        SpringApplication.run(CoffeeAppApplication.class, args);
    }

}
