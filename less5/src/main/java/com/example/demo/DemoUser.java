//
//  Created by  fred on 2016/10/26.
//  Copyright © 2016年 Alibaba. All rights reserved.
//

package com.example.demo;

import com.alibaba.cloudapi.sdk.constant.SdkConstant;
import com.alibaba.cloudapi.sdk.model.ApiCallback;
import com.alibaba.cloudapi.sdk.model.ApiRequest;
import com.alibaba.cloudapi.sdk.model.ApiResponse;
import com.alibaba.cloudapi.sdk.model.HttpClientBuilderParams;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.TrustStrategy;
import javax.net.ssl.*;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * 阿里云 API 网关调用演示类
 *
 * 本类演示如何通过阿里云 API 网关 SDK 调用后端接口。
 * API 网关是一种云服务，可以统一管理和暴露后端 API，
 * 提供鉴权、限流、监控等功能，调用者只需提供 AppKey 和 AppSecret 即可。
 *
 * 流程：静态代码块初始化客户端 → 调用测试方法发送请求 → 打印响应结果
 */
public class DemoUser {

    /**
     * 静态代码块：类加载时自动执行，完成 HTTP/HTTPS 客户端的初始化
     *
     * static{} 块在类第一次被使用时执行，且只执行一次，
     * 适合做只需初始化一次的资源配置（如连接池、客户端参数等）。
     *
     * AppKey 和 AppSecret 是调用 API 网关的身份凭证，
     * 相当于"用户名+密码"，在阿里云控制台的访问控制中获取。
     */
    static {
        // ---- 初始化 HTTP 客户端 ----
        HttpClientBuilderParams httpParam = new HttpClientBuilderParams();
        httpParam.setAppKey("204992318");                        // API 网关分配的应用 Key
        httpParam.setAppSecret("2k004iFbY5OWQlIOlUCLPM8XbsqKmhfS"); // 对应的密钥（用于签名请求）
        HttpApiClientUserInfo.getInstance().init(httpParam);     // 将参数传给单例客户端完成初始化

        // ---- 初始化 HTTPS 客户端（HTTPS 比 HTTP 多了 SSL/TLS 加密）----
        HttpClientBuilderParams httpsParam = new HttpClientBuilderParams();
        httpsParam.setAppKey("204992318");
        httpsParam.setAppSecret("2k004iFbY5OWQlIOlUCLPM8XbsqKmhfS");

        // 注意：演示时禁用了 HTTPS 证书校验（getNoVerifyRegistry），生产环境务必开启校验
        // httpsParam.setRegistry(getNoVerifyRegistry());

        HttpApiClientUserInfo.getInstance().init(httpsParam);
    }

    /**
     * 测试方法：以【异步】方式通过 HTTP 调用"用户信息"接口
     *
     * 异步调用：发出请求后当前线程不等待，结果通过匿名内部类（ApiCallback）回调返回。
     * 这种方式不会阻塞主线程，适合高并发场景。
     */
    public static void userInfoHttpTest() {
        HttpApiClientUserInfo.getInstance().userInfo("刘伟", new ApiCallback() {
            // 请求失败时回调（如网络超时、连接拒绝等）
            @Override
            public void onFailure(ApiRequest request, Exception e) {
                e.printStackTrace(); // 打印异常堆栈，便于排查问题
            }

            // 请求成功时回调，response 包含服务端返回的状态码和响应体
            @Override
            public void onResponse(ApiRequest request, ApiResponse response) {
                try {
                    System.out.println(getResultString(response)); // 格式化并打印响应内容
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    /**
     * 程序入口：运行 userInfoHttpTest 进行接口调用演示
     */
    public static void main(String[] args) {
        userInfoHttpTest();
    }

    /**
     * 测试方法：以【同步】方式通过 HTTP 调用"用户信息"接口
     *
     * 同步调用：发出请求后当前线程阻塞等待，直到服务端响应才继续执行。
     * 代码逻辑更直观，适合调试或对执行顺序有要求的场景。
     */
    public static void userInfoHttpSyncTest() {
        // sendSyncRequest 会阻塞直到收到响应，response 即为服务端返回的结果
        ApiResponse response = HttpApiClientUserInfo.getInstance().userInfoSyncMode("刘伟");
        try {
            System.out.println(getResultString(response));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 测试方法：以【异步】方式通过 HTTPS 调用"用户信息"接口（演示用，跳过证书验证）
     */
    public static void userInfoHttpsTest() {
        HttpApiClientUserInfo.getInstance().userInfo("default", new ApiCallback() {
            @Override
            public void onFailure(ApiRequest request, Exception e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(ApiRequest request, ApiResponse response) {
                try {
                    System.out.println(getResultString(response));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    /**
     * 测试方法：以【同步】方式通过 HTTPS 调用"用户信息"接口（演示用，跳过证书验证）
     */
    public static void userInfoHttpsSyncTest() {
        ApiResponse response = HttpApiClientUserInfo.getInstance().userInfoSyncMode("default");
        try {
            System.out.println(getResultString(response));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 工具方法：将 ApiResponse 格式化为可读字符串，便于控制台输出
     *
     * @param response 服务端返回的响应对象
     * @return 格式化后的字符串，包含状态码、错误信息（如有）、响应体
     */
    private static String getResultString(ApiResponse response) throws IOException {
        StringBuilder result = new StringBuilder();
        result.append("Response from backend server").append(SdkConstant.CLOUDAPI_LF).append(SdkConstant.CLOUDAPI_LF);
        // 拼接 HTTP 状态码（200=成功，其他值表示异常）
        result.append("ResultCode:").append(SdkConstant.CLOUDAPI_LF).append(response.getCode())
                .append(SdkConstant.CLOUDAPI_LF).append(SdkConstant.CLOUDAPI_LF);
        if (response.getCode() != 200) {
            // 非 200 时，从响应头 X-Ca-Error-Message 中读取错误描述
            result.append("Error description:").append(response.getFirstHeaderValue("X-Ca-Error-Message".toLowerCase()))
                    .append(SdkConstant.CLOUDAPI_LF).append(SdkConstant.CLOUDAPI_LF);
        }
        if (response.getBody() != null) {
            // 将响应体字节数组按指定编码转为字符串输出
            result.append("ResultBody:").append(SdkConstant.CLOUDAPI_LF)
                    .append(new String(response.getBody(), SdkConstant.CLOUDAPI_ENCODING));
        }
        return result.toString();
    }

    /**
     * 工具方法：构建一个跳过 HTTPS 证书验证的 Socket 工厂注册表
     *
     * 注意：仅用于演示！生产环境绝对不能跳过证书验证，否则会有中间人攻击风险。
     * 正常情况下应使用系统信任的 CA 证书进行校验。
     */
    private static Registry<ConnectionSocketFactory> getNoVerifyRegistry() {
        RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();
        try {
            // 注册 HTTP 协议使用普通 Socket 工厂
            registryBuilder.register("http", PlainConnectionSocketFactory.INSTANCE).build();
            // 注册 HTTPS 协议，使用自定义的 TrustStrategy（信任所有证书）
            registryBuilder.register(
                    "https",
                    new SSLConnectionSocketFactory(new SSLContextBuilder().loadTrustMaterial(
                            KeyStore.getInstance(KeyStore.getDefaultType()), new TrustStrategy() {
                                @Override
                                public boolean isTrusted(X509Certificate[] x509Certificates, String s)
                                        throws CertificateException {
                                    return true; // 无条件信任所有证书（仅演示，不安全）
                                }
                            }).build(),
                            new HostnameVerifier() {
                                @Override
                                public boolean verify(String paramString, SSLSession paramSSLSession) {
                                    return true; // 跳过主机名校验（仅演示，不安全）
                                }
                            }));
        } catch (Exception e) {
            throw new RuntimeException("HttpClientUtil init failure !", e);
        }
        return registryBuilder.build();
    }

    /**
     * 工具方法：全局禁用 HTTPS 证书验证（仅演示用，生产环境禁止使用）
     *
     * 通过自定义 TrustManager（miTM）替换系统默认的证书验证逻辑，
     * 使所有 HTTPS 连接都绕过证书校验。
     */
    private static void trustAllHttpsCertificates() throws Exception {
        javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
        javax.net.ssl.TrustManager tm = new miTM(); // 使用自定义的"信任一切"管理器
        trustAllCerts[0] = tm;
        javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, null);
        // 将自定义 SSL 工厂设置为全局默认，影响所有 HttpsURLConnection
        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    /**
     * 自定义 TrustManager：无条件信任所有服务端证书
     *
     * X509TrustManager 是 Java 标准的证书验证接口，
     * 这里将所有验证方法实现为"直接通过"，仅用于演示 HTTPS 调用，
     * 生产环境必须使用正规证书验证。
     */
    static class miTM implements javax.net.ssl.TrustManager,
            javax.net.ssl.X509TrustManager {

        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null; // 不限制受信任的 CA，返回 null 表示接受任何 CA 签发的证书
        }

        public boolean isServerTrusted(java.security.cert.X509Certificate[] certs) {
            return true; // 信任所有服务端证书
        }

        public boolean isClientTrusted(java.security.cert.X509Certificate[] certs) {
            return true; // 信任所有客户端证书
        }

        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
                throws java.security.cert.CertificateException {
            return; // 不抛异常 = 验证通过，服务端证书无需校验
        }

        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)
                throws java.security.cert.CertificateException {
            return; // 不抛异常 = 验证通过，客户端证书无需校验
        }
    }
}
