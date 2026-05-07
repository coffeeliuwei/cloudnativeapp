//
//  Created by  fred on 2017/1/12.
//  Copyright © 2016年 Alibaba. All rights reserved.
//

package com.example.demo;
import com.alibaba.cloudapi.sdk.client.ApacheHttpClient;
import com.alibaba.cloudapi.sdk.enums.Scheme;
import com.alibaba.cloudapi.sdk.enums.HttpMethod;
import com.alibaba.cloudapi.sdk.model.ApiRequest;
import com.alibaba.cloudapi.sdk.model.ApiResponse;
import com.alibaba.cloudapi.sdk.model.ApiCallback;
import com.alibaba.cloudapi.sdk.model.HttpClientBuilderParams;
import com.alibaba.cloudapi.sdk.enums.ParamPosition;
import com.alibaba.cloudapi.sdk.enums.WebSocketApiType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * 阿里云 API 网关客户端封装类——"用户信息"接口的具体实现
 *
 * 继承自 ApacheHttpClient（阿里云 SDK 提供的基础 HTTP 客户端），
 * 在其基础上封装了调用特定 API（用户信息查询）的逻辑。
 *
 * 使用单例模式（Singleton Pattern）：整个程序只创建一个实例，
 * 通过 getInstance() 方法获取，避免重复创建连接池浪费资源。
 */
public class HttpApiClientUserInfo extends ApacheHttpClient {

    // API 网关分配的服务域名（Host），所有请求都发往这个地址
    public final static String HOST = "de14143ebe0640eb92103d3005ba23bb-cn-hangzhou.alicloudapi.com";

    // 单例对象：类加载时自动创建，保证全局唯一
    static HttpApiClientUserInfo instance = new HttpApiClientUserInfo();

    // 提供外部获取单例的方法
    public static HttpApiClientUserInfo getInstance() { return instance; }

    // Jackson 提供的 JSON 工具，可用于将响应体（字节数组）解析为 Java 对象
    public static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 初始化 HTTP 客户端连接参数
     * 在使用任何接口之前，必须先调用此方法完成初始化
     *
     * @param httpClientBuilderParams 包含 AppKey、AppSecret 等认证信息的参数对象，
     *                                在 DemoUser 的静态代码块中构建并传入
     */
    public void init(HttpClientBuilderParams httpClientBuilderParams) {
        httpClientBuilderParams.setScheme(Scheme.HTTP); // 指定使用 HTTP 协议（非 HTTPS）
        httpClientBuilderParams.setHost(HOST);          // 设置目标服务器域名
        super.init(httpClientBuilderParams);            // 调用父类完成连接池等底层初始化
    }

    /**
     * 【异步方式】调用"用户信息"接口
     *
     * 异步调用不会阻塞当前线程，请求在后台发出，
     * 结果通过 callback 回调通知调用方（类似前端的 Ajax 回调）。
     *
     * 接口路径：GET /user/userInfo/{name}
     *
     * @param name     路径参数：用户名称，会替换 URL 路径中的 [name] 占位符
     * @param callback 回调接口，包含两个方法：
     *                   - onFailure：请求失败时触发（网络错误等）
     *                   - onResponse：请求成功时触发，可在此处理返回数据
     */
    public void userInfo(String name, ApiCallback callback) {
        String path = "/user/userInfo/[name]";                     // 接口路径，[name] 是路径参数占位符
        ApiRequest request = new ApiRequest(HttpMethod.GET, path); // 创建 HTTP GET 请求对象
        // 添加路径参数：将 name 的值填入路径的 [name] 位置，最后参数 true 表示必填
        request.addParam("name", name, ParamPosition.PATH, true);
        sendAsyncRequest(request, callback);                       // 异步发送请求，结果由 callback 接收
    }

    /**
     * 【同步方式】调用"用户信息"接口
     *
     * 同步调用会阻塞当前线程，直到服务器返回结果才继续执行后续代码。
     * 适合调试或对响应顺序有严格要求的场景。
     *
     * @param name 路径参数：用户名称
     * @return ApiResponse 服务端返回的完整响应对象，包含：
     *           - getCode()：HTTP 状态码（200 表示成功）
     *           - getBody()：响应体（字节数组，需转为字符串使用）
     */
    public ApiResponse userInfoSyncMode(String name) {
        String path = "/user/userInfo/[name]";
        ApiRequest request = new ApiRequest(HttpMethod.GET, path);
        request.addParam("name", name, ParamPosition.PATH, true);
        return sendSyncRequest(request); // 同步发送请求，阻塞等待并直接返回结果
    }

}