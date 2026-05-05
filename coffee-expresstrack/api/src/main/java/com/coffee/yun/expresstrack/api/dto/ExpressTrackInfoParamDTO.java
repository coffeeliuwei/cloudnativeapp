package com.coffee.yun.expresstrack.api.dto;

import com.coffee.yun.dto.BasePageDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class ExpressTrackInfoParamDTO extends BasePageDTO implements Serializable {

    private String order_id;

}
