package com.coffee.yun.userorder.api.dto;

import com.coffee.yun.dto.BasePageDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class UserOrderInfoParamDTO extends BasePageDTO implements Serializable {

    private String OneID;
    private String member_name;
    private String order_id;
    private String order_status;

}
