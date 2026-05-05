package com.coffee.yun.expresstrack.api.service;

import com.coffee.yun.dto.PageDTO;
import com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoParamDTO;
import com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoResultDTO;

public interface ExpressTrackInfoService {

    /**
     * 根据用户订单查询快递轨迹
     * @param expressTrackInfoParamDTO
     * @return
     */
    PageDTO<ExpressTrackInfoResultDTO> findExpressTrackInfos(ExpressTrackInfoParamDTO expressTrackInfoParamDTO);
}
