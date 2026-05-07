package com.example.demo;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

/**
 * REST 控制器示例：演示如何接收请求参数并返回 JSON 数据
 *
 * @RestController 标注这是一个 RESTful 控制器。
 * 当方法返回 HashMap 对象时，Spring 会自动通过内置的 Jackson 库
 * 将其序列化为 JSON 格式返回给客户端，无需手动转换。
 *
 * 启动后访问：http://localhost:8080/hello?name=张三&welcome=你好
 */
@RestController
public class HelloController {

    /**
     * 处理 /hello 请求，接收两个查询参数并以 JSON 格式返回
     *
     * 请求示例：GET http://localhost:8080/hello?name=张三&welcome=你好
     * 返回示例：{"name":"张三","welcome":"你好","time":1700000000000}
     *
     * @param name    URL 查询参数，例如 ?name=张三，Spring 自动将其绑定到此参数
     * @param welcome URL 查询参数，例如 &welcome=你好，Spring 自动将其绑定到此参数
     * @return 包含 name、welcome、time 三个字段的 HashMap，会被自动转为 JSON 返回
     */
    @RequestMapping("/hello")
    public HashMap<String, Object> hello(String name, String welcome) {
        // 创建 HashMap 存放返回的 JSON 字段，键为字段名，值为字段内容
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("name", name);                        // 将 name 参数放入结果
        hashMap.put("welcome", welcome);                  // 将 welcome 参数放入结果
        hashMap.put("time", System.currentTimeMillis());  // 记录当前时间戳（毫秒）
        // 返回 hashMap，Spring 自动将其序列化为 JSON 写入 HTTP 响应体
        return hashMap;
    }
}
