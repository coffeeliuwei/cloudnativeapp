package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 应用程序入口类
 *
 * @SpringBootApplication 是一个组合注解，等价于同时加上以下三个注解：
 *   1. @Configuration        —— 声明当前类是 Spring 配置类
 *   2. @EnableAutoConfiguration —— 开启自动配置，Spring Boot 会根据 pom.xml 中的依赖自动装配 Bean
 *   3. @ComponentScan        —— 自动扫描当前包及其子包下带有注解的类（如 @Controller、@Service 等）
 */
@SpringBootApplication
public class DemoApplication {

    /**
     * 程序主方法，整个 Spring Boot 应用从这里启动
     *
     * SpringApplication.run() 会做以下几件事：
     *   1. 创建 Spring 应用上下文（IoC 容器）
     *   2. 启动内嵌的 Tomcat 服务器（默认端口 8080）
     *   3. 扫描并注册所有带注解的类到容器中
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
