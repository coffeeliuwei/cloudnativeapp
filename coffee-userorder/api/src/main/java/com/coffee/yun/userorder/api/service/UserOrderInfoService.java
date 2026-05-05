package com.coffee.yun.userorder.api.service;

import com.coffee.yun.dto.PageDTO;
import com.coffee.yun.userorder.api.dto.UserOrderInfoParamDTO;
import com.coffee.yun.userorder.api.dto.UserOrderInfoResultDTO;

public interface UserOrderInfoService {

    /**
     *  根据用户信息查询订单信息
     * @param userOrderInfoParamDTO
     * @return
     */
    UserOrderInfoResultDTO findUserOrderInfo(UserOrderInfoParamDTO userOrderInfoParamDTO);

    /**
     * 根据用户信息查询订单列表
     * @param userOrderInfoParamDTO
     * @return
     */
    PageDTO<UserOrderInfoResultDTO> findUserOrderInfos(UserOrderInfoParamDTO userOrderInfoParamDTO);

}
