package com.coffee.yun.coffeeapp.base;

/**
 * 统一响应构造工具类
 *
 * 作用：简化 Result 对象的创建，避免在每个 Controller 方法中重复写
 *   result.setSuccess(true); result.setCode(200); result.setMessage("success"); 等样板代码。
 *
 * 使用示例：
 *   // 成功返回数据
 *   return new ResultUtil<PageDTO>().setData(pageDTO);
 *
 *   // 成功返回数据 + 自定义消息
 *   return new ResultUtil<String>().setData("OK", "操作成功");
 *
 *   // 返回错误信息
 *   return new ResultUtil<Void>().setErrorMsg("订单不存在");
 *
 * 泛型 T：与 Result<T> 一致，表示业务数据的类型。
 */
public class ResultUtil<T> {

    private Result<T> result;

    /**
     * 构造时默认设置为成功状态（code=200, success=true, message="success"）
     * 后续调用 setData / setErrorMsg 等方法可覆盖这些默认值
     */
    public ResultUtil() {
        result = new Result<>();
        result.setSuccess(true);
        result.setMessage("success");
        result.setCode(200);
    }

    /** 设置业务数据，返回成功响应 */
    public Result<T> setData(T t) {
        this.result.setResult(t);
        this.result.setCode(200);
        return this.result;
    }

    /** 返回成功响应，携带自定义提示消息（无业务数据） */
    public Result<T> setSuccessMsg(String msg) {
        this.result.setSuccess(true);
        this.result.setMessage(msg);
        this.result.setCode(200);
        this.result.setResult(null);
        return this.result;
    }

    /** 设置业务数据 + 自定义消息，返回成功响应 */
    public Result<T> setData(T t, String msg) {
        this.result.setResult(t);
        this.result.setCode(200);
        this.result.setMessage(msg);
        return this.result;
    }

    /** 设置业务数据 + 自定义状态码 + 自定义消息 */
    public Result<T> setData(T t, Integer code, String msg) {
        this.result.setResult(t);
        this.result.setCode(code);
        this.result.setMessage(msg);
        return this.result;
    }

    /** 设置自定义状态码 + 消息，无业务数据（常用于特殊业务状态返回） */
    public Result<T> setData(Integer code, String msg) {
        this.result.setResult(null);
        this.result.setCode(code);
        this.result.setMessage(msg);
        return this.result;
    }

    /** 返回失败响应，code=500，携带错误描述 */
    public Result<T> setErrorMsg(String msg) {
        this.result.setSuccess(false);
        this.result.setMessage(msg);
        this.result.setCode(500);
        return this.result;
    }

    /** 返回失败响应，合并两段错误描述（如"查询失败（数据库连接超时）"） */
    public Result<T> setErrorMsg(String msg, String msg2) {
        this.result.setSuccess(false);
        this.result.setMessage(msg + "（" + msg2 + "）");
        this.result.setCode(500);
        return this.result;
    }

    /** 返回失败响应，携带自定义状态码 + 错误描述 */
    public Result<T> setErrorMsg(Integer code, String msg) {
        this.result.setSuccess(false);
        this.result.setResult(null);
        this.result.setMessage(msg);
        this.result.setCode(code);
        return this.result;
    }

    /** 返回失败响应，携带自定义状态码 + 两段错误描述 */
    public Result<T> setErrorMsg(Integer code, String msg, String msg2) {
        this.result.setSuccess(false);
        this.result.setResult(null);
        this.result.setMessage(msg + "（" + msg2 + "）");
        this.result.setCode(code);
        return this.result;
    }

    /** 返回失败响应，同时携带业务数据（如部分成功时返回已处理的数据 + 错误说明） */
    public Result<T> setErrorData(T t, String msg) {
        this.result.setSuccess(false);
        this.result.setResult(t);
        this.result.setCode(500);
        this.result.setMessage(msg);
        return this.result;
    }

    /** 返回失败响应，携带业务数据 + 自定义状态码 + 错误描述 */
    public Result<T> setErrorData(T t, Integer code, String msg) {
        this.result.setSuccess(false);
        this.result.setResult(t);
        this.result.setCode(code);
        this.result.setMessage(msg);
        return this.result;
    }
}
