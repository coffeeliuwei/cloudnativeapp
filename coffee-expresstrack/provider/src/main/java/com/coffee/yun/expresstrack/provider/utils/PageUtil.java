package com.coffee.yun.expresstrack.provider.utils;

import com.alibaba.fastjson2.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.coffee.yun.dto.BasePageDTO;
import com.coffee.yun.dto.PageDTO;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 分页查询工具类
 *
 * 作用：封装 MyBatis + PageHelper 的分页查询流程，避免在每个 Service 里重复写相同的分页代码。
 *   与 coffee-userorder 中的 PageUtil 结构完全相同，两个微服务各自维护一份，互不依赖。
 *
 * 核心原理（PageHelper 拦截机制）：
 *   1. PageHelper.startPage() 将分页参数存入 ThreadLocal（线程本地变量）
 *   2. 紧接着执行的第一条 MyBatis 查询会被 PageHelper 自动拦截
 *   3. PageHelper 在 SQL 末尾追加 LIMIT 子句，例如：
 *      原 SQL：SELECT * FROM express WHERE order_id = 'ORDER001'
 *      拦截后：SELECT * FROM express WHERE order_id = 'ORDER001' LIMIT 0, 10
 *   4. 同时自动执行一条 COUNT(*) 查询统计总记录数
 *   5. 将结果封装进 PageInfo，包含 total、pages、list 等分页信息
 *
 * @Component：声明为 Spring Bean，可在 Service 中通过 @Autowired 注入使用
 * @Slf4j：Lombok 自动生成 log 日志对象，等价于手写 Logger log = LoggerFactory.getLogger(...)
 */
@Component
@Slf4j
public class PageUtil<T> {

    // MyBatis 的 SQL 执行模板，由 Spring 自动注入，用于执行 Mapper 中定义的 SQL
    @Autowired
    SqlSessionTemplate sqlSessionTemplate;

    /**
     * 执行分页查询并返回封装好的分页结果
     *
     * @param template    MyBatis Mapper 的 SQL 语句 ID，格式为"MapperName.methodName"
     *                    例如："ExpressTrackMapper.selectByParam"
     * @param basePageDTO 查询参数，包含分页信息（pageNum、pageSize）和业务查询条件
     * @return PageDTO 分页结果，包含 total（总记录数）、pages（总页数）、list（当前页数据）
     */
    public PageDTO<T> selectPage(String template, BasePageDTO basePageDTO) {
        log.info("开始分页查询 {} 参数：{}", template, JSON.toJSONString(basePageDTO));
        PageInfo<T> pageInfo = null;
        try {
            // startPage() 设置分页参数，doSelectPageInfo() 执行查询并封装为含分页信息的 PageInfo
            pageInfo = PageHelper.startPage(basePageDTO.getPageNum(), basePageDTO.getPageSize())
                    .doSelectPageInfo(() -> sqlSessionTemplate.selectList(template, basePageDTO));
        } catch (Exception e) {
            log.error("分页查询异常：{}", e.getMessage());
        }
        log.info("分页查询成功：{}", JSON.toJSONString(pageInfo));
        // 将 PageHelper 的 PageInfo 转换为本项目统一的 PageDTO 格式返回
        return new PageDTO<>(pageInfo.getTotal(), pageInfo.getPages(), pageInfo.getList());
    }
}
