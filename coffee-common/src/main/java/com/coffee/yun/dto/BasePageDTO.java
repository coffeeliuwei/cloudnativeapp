package com.coffee.yun.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class BasePageDTO {

    private int pageNum;

    @Max(value = 200, message = "每页显示不超过200条记录")
    @Min(value = 1, message = "每页显示不能小于1")
    private int pageSize;

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
