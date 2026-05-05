package com.coffee.yun.expresstrack.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class ExpressTrackInfoResultDTO implements Serializable {

    private String order_id;
    private String express_id;
    private String express_weight;
    private String track_id;
    private String track_show;

}
