package com.coffee.yun.dto;

import java.io.Serializable;
import java.util.List;

public class PageDTO<T> implements Serializable {
    public PageDTO(long total, long pages, List<T> list) {
        this.total = total;
        this.pages = pages;
        this.list = list;
    }

    private long total;

    private long pages;

    private List<T> list;

    public long getTotal() {
        return total;
    }

    public long getPages() {
        return pages;
    }

    public List<T> getList() {
        return list;
    }
}
