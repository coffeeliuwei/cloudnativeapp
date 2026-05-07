package com.coffee.less4_5;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第一个 Spring Boot REST 控制器示例
 *
 * @RestController 是组合注解，等价于 @Controller + @ResponseBody：
 *   - @Controller   —— 告诉 Spring 这个类负责处理 HTTP 请求
 *   - @ResponseBody —— 方法返回值直接写入 HTTP 响应体（不跳转页面，直接返回数据）
 *
 * 启动后访问：http://localhost:8080/hello 即可看到返回结果
 */
@RestController
public class Hello {

	/**
	 * 处理 GET /hello 请求，返回一个带当前时间戳的字符串
	 *
	 * @RequestMapping("/hello") —— 将 URL 路径 /hello 映射到此方法
	 *   可以用浏览器或 Postman 直接访问 http://localhost:8080/hello
	 *
	 * System.currentTimeMillis() —— 返回从1970年1月1日至今的毫秒数，
	 *   拼接在字符串后面可以验证每次请求时间戳都不同（服务是实时响应的）
	 */
	@RequestMapping("/hello")
	public String hello() {
		return "Hello World" + System.currentTimeMillis();
	}
}
