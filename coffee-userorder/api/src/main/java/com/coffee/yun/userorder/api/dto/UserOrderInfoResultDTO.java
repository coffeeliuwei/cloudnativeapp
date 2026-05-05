package com.coffee.yun.userorder.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class UserOrderInfoResultDTO implements Serializable {

    private String order_id;
    private String OneID;
    private String member_phone;
    private String member_name;
    private String order_status;
    private float order_amount;

}
