package com.coffee.yun.userorder.provider.utils;
import cn.hutool.json.JSONUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.coffee.yun.dto.BasePageDTO;
import com.coffee.yun.dto.PageDTO;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
@Component
@Slf4j
public class PageUtil<T> {

    @Autowired
    SqlSessionTemplate sqlSessionTemplate;

    /**
     * 分页查询封装
     * @param template
     * @param basePageDTO
     * @return
     */
    public PageDTO<T> selectPage(String template, BasePageDTO basePageDTO) {
        log.info("开始分页查询"+template+"参数："+ JSONUtil.toJsonStr(basePageDTO));
        PageInfo<T> pageInfo = null;
        try {
            pageInfo = PageHelper.startPage(basePageDTO.getPageNum(), basePageDTO.getPageSize())
                    .doSelectPageInfo(() -> {sqlSessionTemplate.selectList(template, basePageDTO);
                    });
        } catch (Exception e) {
            log.error("分页查询异常："+e.getMessage());
        }
        log.info("分页查询成功:"+JSONUtil.toJsonStr(pageInfo));
        return new PageDTO(pageInfo.getTotal(),pageInfo.getPages(), pageInfo.getList());
    }
}
