package com.coffee.yun.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 分页请求基类
 *
 * 作用：封装前端传来的分页参数（第几页、每页几条），供所有需要分页的查询 DTO 继承使用。
 *
 * 使用方式：
 *   其他查询参数类（如 UserOrderInfoParamDTO）继承此类后，
 *   自动拥有 pageNum 和 pageSize 两个分页字段，无需重复定义。
 *
 * 前端传参示例：
 *   { "pageNum": 1, "pageSize": 10, "order_id": "ORDER001" }
 */
public class BasePageDTO {

    // 当前页码，从 1 开始。setPageNum 方法中做了保护：传 0 或负数时自动纠正为 1
    private int pageNum;

    // 每页显示的记录数。@Max 和 @Min 注解由 Spring 框架自动校验：
    // 若前端传入 pageSize < 1 或 > 200，Spring 会直接拒绝请求并返回错误信息
    @Max(value = 200, message = "每页显示不超过200条记录")
    @Min(value = 1, message = "每页显示不能小于1")
    private int pageSize;

    /**
     * 设置页码，并防止页码小于 1 的异常情况
     * 例如前端传了 pageNum=0，这里会自动改为 1，避免数据库查询出错
     */
    public void setPageNum(int pageNum) {
        if (pageNum < 1) {
            pageNum = 1;
        }
        this.pageNum = pageNum;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageSize() {
        return pageSize;
    }
}
