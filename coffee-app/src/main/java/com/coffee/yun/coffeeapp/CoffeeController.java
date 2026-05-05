package com.coffee.yun.coffeeapp;

import com.coffee.yun.dto.PageDTO;
import com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoParamDTO;
import com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoResultDTO;
import com.coffee.yun.expresstrack.api.service.ExpressTrackInfoService;
import com.coffee.yun.coffeeapp.base.Result;
import com.coffee.yun.coffeeapp.base.ResultUtil;
import com.coffee.yun.userorder.api.dto.UserOrderInfoParamDTO;
import com.coffee.yun.userorder.api.dto.UserOrderInfoResultDTO;
import com.coffee.yun.userorder.api.service.UserOrderInfoService;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
public class CoffeeController {

    @Reference
    private UserOrderInfoService userOrderInfoService;
    @Reference
    private ExpressTrackInfoService expressTrackInfoService;

    @RequestMapping("/hello/{orderid}")
    public PageDTO<ExpressTrackInfoResultDTO> helloCoffee(@PathVariable String orderid){
        UserOrderInfoParamDTO userOrderInfoParamDTO = new UserOrderInfoParamDTO();
        userOrderInfoParamDTO.setOrder_id(orderid);
        UserOrderInfoResultDTO userOrderInfoResultDTO = userOrderInfoService.findUserOrderInfo(userOrderInfoParamDTO);

        ExpressTrackInfoParamDTO expressTrackInfoParamDTO = new ExpressTrackInfoParamDTO();
        expressTrackInfoParamDTO.setOrder_id(userOrderInfoResultDTO.getOrder_id());
        return expressTrackInfoService.findExpressTrackInfos(expressTrackInfoParamDTO);
    }

    @PostMapping("findOrderList")
    public Result<PageDTO> findOrderList(@RequestBody UserOrderInfoParamDTO userOrderInfoParamDTO){
        UserOrderInfoResultDTO userOrderInfoResultDTO = userOrderInfoService.findUserOrderInfo(userOrderInfoParamDTO);
        ExpressTrackInfoParamDTO expressTrackInfoParamDTO = new ExpressTrackInfoParamDTO();
        expressTrackInfoParamDTO.setOrder_id(userOrderInfoResultDTO.getOrder_id());
        return new ResultUtil<PageDTO>().setData(expressTrackInfoService.findExpressTrackInfos(expressTrackInfoParamDTO));
    }

}
