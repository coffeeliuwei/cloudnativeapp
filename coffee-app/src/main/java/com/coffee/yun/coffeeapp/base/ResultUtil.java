package com.coffee.yun.coffeeapp.base;

/**
 * 返回结果工具类
 */
public class ResultUtil<T> {

    private Result<T> result;

    public ResultUtil() {
        result = new Result<>();
        result.setSuccess(true);
        result.setMessage("success");
        result.setCode(200);
    }

    public Result<T> setData(T t) {
        this.result.setResult(t);
        this.result.setCode(200);
        return this.result;
    }

    public Result<T> setSuccessMsg(String msg) {
        this.result.setSuccess(true);
        this.result.setMessage(msg);
        this.result.setCode(200);
        this.result.setResult(null);
        return this.result;
    }

    public Result<T> setData(T t, String msg) {
        this.result.setResult(t);
        this.result.setCode(200);
        this.result.setMessage(msg);
        return this.result;
    }

    public Result<T> setData(T t, Integer code, String msg) {
        this.result.setResult(t);
        this.result.setCode(code);
        this.result.setMessage(msg);
        return this.result;
    }

    public Result<T> setData(Integer code, String msg) {
        this.result.setResult(null);
        this.result.setCode(code);
        this.result.setMessage(msg);
        return this.result;
    }

    public Result<T> setErrorMsg(String msg) {
        this.result.setSuccess(false);
        this.result.setMessage(msg);
        this.result.setCode(500);
        return this.result;
    }

    public Result<T> setErrorMsg(String msg, String msg2) {
        this.result.setSuccess(false);
        this.result.setMessage(msg + "（" + msg2 + "）");
        this.result.setCode(500);
        return this.result;
    }

    public Result<T> setErrorMsg(Integer code, String msg) {
        this.result.setSuccess(false);
        this.result.setResult(null);
        this.result.setMessage(msg);
        this.result.setCode(code);
        return this.result;
    }

    public Result<T> setErrorMsg(Integer code, String msg, String msg2) {
        this.result.setSuccess(false);
        this.result.setResult(null);
        this.result.setMessage(msg + "（" + msg2 + "）");
        this.result.setCode(code);
        return this.result;
    }
    public Result<T> setErrorData(T t, String msg){
        this.result.setSuccess(false);
        this.result.setResult(t);
        this.result.setCode(500);
        this.result.setMessage(msg);
        return this.result;
    }
    public Result<T> setErrorData(T t, Integer code, String msg){
        this.result.setSuccess(false);
        this.result.setResult(t);
        this.result.setCode(code);
        this.result.setMessage(msg);
        return this.result;
    }
}
