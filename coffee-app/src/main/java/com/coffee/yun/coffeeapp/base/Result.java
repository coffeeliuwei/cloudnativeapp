package com.coffee.yun.coffeeapp.base;

import java.io.Serializable;

/**
 * 统一响应体
 *
 * 作用：所有接口返回给前端的数据都包装在此对象中，格式统一便于前端处理。
 *
 * 前端收到的 JSON 示例（成功）：
 *   {
 *     "success": true,
 *     "code": 200,
 *     "message": "success",
 *     "timestamp": 1704867600000,
 *     "result": { ...业务数据... }
 *   }
 *
 * 前端收到的 JSON 示例（失败）：
 *   {
 *     "success": false,
 *     "code": 500,
 *     "message": "订单不存在",
 *     "timestamp": 1704867600000,
 *     "result": null
 *   }
 *
 * 泛型 T 表示 result 字段中业务数据的类型，例如：
 *   Result<PageDTO>  — 分页查询结果
 *   Result<String>   — 纯文本提示
 *
 * 实现 Serializable：支持网络传输和缓存序列化。
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    // 请求是否成功，true=成功，false=失败。前端根据此字段决定显示正常数据还是错误提示
    private boolean success;

    // 错误或提示信息，成功时为"success"，失败时为具体错误原因
    private String message;

    // 状态码，遵循 HTTP 语义：200=成功，500=服务器错误，可自定义其他业务码
    private Integer code;

    // 响应时间戳（毫秒），用于前端计算接口响应时间或做日志记录
    private long timestamp = System.currentTimeMillis();

    // 业务数据主体，查询结果、列表等实际数据放在这里
    private T result;

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
