package com.coffee.yun.userorder.provider.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.coffee.yun.dto.PageDTO;
import com.coffee.yun.userorder.api.dto.UserOrderInfoParamDTO;
import com.coffee.yun.userorder.api.dto.UserOrderInfoResultDTO;
import com.coffee.yun.userorder.api.service.UserOrderInfoService;
import com.coffee.yun.userorder.provider.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@DubboService
public class UserOrderInfoServiceImpl implements UserOrderInfoService {
    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;
    @Autowired
    private PageUtil pageUtil;
    @Override
    public UserOrderInfoResultDTO findUserOrderInfo(UserOrderInfoParamDTO userOrderInfoParamDTO) {

        log.info(" 订单查询 根据用户订单ID查询订单详情："+ JSONUtil.toJsonStr(userOrderInfoParamDTO));
        UserOrderInfoResultDTO userOrderInfoResultDTO = sqlSessionTemplate.
                selectOne("UserOrderMapper.selectByParam",userOrderInfoParamDTO);
        return userOrderInfoResultDTO;

    }

    /**
     * 按条件查询用户订单列表
     * @param userOrderInfoParamDTO
     * @return
     */
    @Override
    public PageDTO<UserOrderInfoResultDTO> findUserOrderInfos(UserOrderInfoParamDTO userOrderInfoParamDTO) {
        return pageUtil.selectPage("UserOrderMapper.selectByParam",userOrderInfoParamDTO);
    }
}
