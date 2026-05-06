package com.coffee.yun.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应包装类
 *
 * 作用：将数据库分页查询的结果统一打包，返回给前端。
 *   前端通过 total 显示"共 xx 条"，通过 pages 渲染分页器页码，通过 list 渲染表格数据。
 *
 * 泛型 T 表示列表中每条数据的类型，例如：
 *   PageDTO<UserOrderInfoResultDTO>  — 订单分页结果
 *   PageDTO<ExpressTrackInfoResultDTO> — 快递轨迹分页结果
 *
 * 实现 Serializable 的原因：
 *   Dubbo RPC 在网络上传输对象时，需要先将对象序列化成字节流。
 *   不实现 Serializable 会导致运行时抛出 NotSerializableException。
 *
 * 前端收到的 JSON 示例：
 *   { "total": 58, "pages": 6, "list": [ {...}, {...} ] }
 */
public class PageDTO<T> implements Serializable {

    /**
     * 构造方法：由 PageUtil 在查询完成后调用，将 PageHelper 的查询结果转换为此对象
     *
     * @param total 数据库中符合条件的总记录数（用于前端显示"共 xx 条"）
     * @param pages 总页数（= total ÷ pageSize，向上取整）
     * @param list  当前页的数据列表
     */
    public PageDTO(long total, long pages, List<T> list) {
        this.total = total;
        this.pages = pages;
        this.list = list;
    }

    // 总记录数，例如数据库中共有 58 条符合条件的数据
    private long total;

    // 总页数，例如 58 条数据每页 10 条，共 6 页
    private long pages;

    // 当前页的数据列表，列表长度 = pageSize（最后一页可能不足 pageSize 条）
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
