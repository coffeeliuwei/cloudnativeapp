package com.coffee.yun.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class BasePageDTO {
    /**
     * 当前页 如果小于1，则值取1
     */
    private int pageNum;
    /**
     * 每页显示数量
     */
    @Max(value = 200, message = "每页显示不超过200条记录")
    @Min(value = 1,message = "每页显示不能小于1")
    private int pageSize;

    public void setPageNum(int pageNum) {
        if( pageNum<1){
            pageNum=1;
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
