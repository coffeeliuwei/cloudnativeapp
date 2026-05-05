package com.coffee.yun.expresstrack.provider.service;

import com.coffee.yun.dto.PageDTO;
import com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoParamDTO;
import com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoResultDTO;
import com.coffee.yun.expresstrack.api.service.ExpressTrackInfoService;
import com.coffee.yun.expresstrack.provider.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@DubboService
public class ExpresstrackInfoServiceImpl implements ExpressTrackInfoService {

    @Autowired
    private PageUtil pageUtil;

    @Override
    public PageDTO<ExpressTrackInfoResultDTO> findExpressTrackInfos(ExpressTrackInfoParamDTO expressTrackInfoParamDTO) {
        return pageUtil.selectPage("ExpressTrackMapper.selectByParam", expressTrackInfoParamDTO);
    }
}
