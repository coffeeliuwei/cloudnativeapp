package com.coffee.yun.coffeeapp;

import com.coffee.yun.dto.PageDTO;
import com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoParamDTO;
import com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoResultDTO;
import com.coffee.yun.expresstrack.api.service.ExpressTrackInfoService;
import com.coffee.yun.coffeeapp.base.Result;
import com.coffee.yun.coffeeapp.base.ResultUtil;
import com.coffee.yun.userorder.api.dto.UserOrderCreateDTO;
import com.coffee.yun.userorder.api.dto.UserOrderInfoParamDTO;
import com.coffee.yun.userorder.api.dto.UserOrderInfoResultDTO;
import com.coffee.yun.userorder.api.service.UserOrderInfoService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * API 网关控制器
 *
 * 作用：coffee-app 对外暴露的唯一 HTTP 入口，接收前端请求，
 *   通过 Dubbo RPC 调用后端微服务，聚合结果后统一返回给前端。
 *
 * @CrossOrigin：允许跨域请求。
 *   前端运行在 localhost:8080，后端运行在 localhost:8005，端口不同属于跨域。
 *   加上此注解后浏览器不会拦截跨域请求。
 *
 * @RestController：组合注解，等价于 @Controller + @ResponseBody。
 *   表示此类是控制器，且所有方法的返回值自动序列化为 JSON 响应体。
 */
@CrossOrigin
@RestController
public class CoffeeController {

    /**
     * @DubboReference：Dubbo 消费者注入注解。
     * 不同于普通的 @Autowired（注入本地 Bean），@DubboReference 注入的是远程服务代理对象。
     * 调用 userOrderInfoService 的方法时，Dubbo 自动将请求通过网络发送给 coffee-userorder。
     */
    @DubboReference
    private UserOrderInfoService userOrderInfoService;

    @DubboReference
    private ExpressTrackInfoService expressTrackInfoService;

    /**
     * 根据订单号查询快递轨迹（简单查询接口）
     *
     * 调用流程：
     *   1. 接收前端传来的 orderid（路径参数）
     *   2. 调用订单服务查询订单详情，获取 order_id
     *   3. 用 order_id 调用快递轨迹服务查询轨迹列表
     *   4. 直接返回分页轨迹数据
     *
     * 示例请求：GET http://localhost:8005/hello/ORDER001
     *
     * @param orderid 前端传来的订单号，对应 URL 路径中的 {orderid}
     * @return 该订单对应的快递轨迹分页列表
     */
    @RequestMapping("/hello/{orderid}")
    public PageDTO<ExpressTrackInfoResultDTO> helloCoffee(@PathVariable String orderid) {
        // 第一步：构造订单查询参数，按 order_id 查询订单
        UserOrderInfoParamDTO userOrderInfoParamDTO = new UserOrderInfoParamDTO();
        userOrderInfoParamDTO.setOrder_id(orderid);
        // Dubbo RPC 调用 coffee-userorder，查询订单详情
        UserOrderInfoResultDTO userOrderInfoResultDTO = userOrderInfoService.findUserOrderInfo(userOrderInfoParamDTO);

        // 第二步：用订单中的 order_id 构造快递轨迹查询参数
        ExpressTrackInfoParamDTO expressTrackInfoParamDTO = new ExpressTrackInfoParamDTO();
        expressTrackInfoParamDTO.setOrder_id(userOrderInfoResultDTO.getOrder_id());
        // Dubbo RPC 调用 coffee-expresstrack，查询快递轨迹列表
        return expressTrackInfoService.findExpressTrackInfos(expressTrackInfoParamDTO);
    }

    /**
     * 根据订单信息查询快递轨迹（带统一响应包装的接口）
     *
     * 与 helloCoffee 的区别：返回结果包装在 Result 对象中，包含 success/code/message 等字段，
     * 便于前端统一处理成功和失败两种情况。
     *
     * 示例请求：POST http://localhost:8005/findOrderList
     * 请求体：{ "order_id": "ORDER001", "pageNum": 1, "pageSize": 10 }
     *
     * @param userOrderInfoParamDTO 前端 POST 请求体，Spring 自动将 JSON 反序列化为此对象（@RequestBody）
     * @return 包装了轨迹分页数据的统一响应体
     */
    @PostMapping("findOrderList")
    public Result<PageDTO> findOrderList(@RequestBody UserOrderInfoParamDTO userOrderInfoParamDTO) {
        // 第一步：Dubbo RPC 调用订单服务，获取订单详情
        UserOrderInfoResultDTO userOrderInfoResultDTO = userOrderInfoService.findUserOrderInfo(userOrderInfoParamDTO);

        // 第二步：用订单中的 order_id 查询快递轨迹
        ExpressTrackInfoParamDTO expressTrackInfoParamDTO = new ExpressTrackInfoParamDTO();
        expressTrackInfoParamDTO.setOrder_id(userOrderInfoResultDTO.getOrder_id());

        // 用 ResultUtil 包装返回结果，自动设置 success=true, code=200
        return new ResultUtil<PageDTO>().setData(expressTrackInfoService.findExpressTrackInfos(expressTrackInfoParamDTO));
    }

    /**
     * 创建订单（同步 Dubbo RPC 调用两个微服务）
     *
     * 调用流程：
     *   1. Dubbo RPC 调用 coffee-userorder，将订单写入 order 表，返回 orderId
     *   2. Dubbo RPC 调用 coffee-expresstrack，同步创建快递单和初始轨迹（"商家已揽件"）
     *
     * 两步 RPC 均为同步调用，调用方等待两步都完成后才返回响应。
     *
     * 示例请求：POST http://localhost:8005/createOrder
     * 请求体：{ "order_id": "ORDER099", "OneID": "ONE001", "order_amount": 99.9 }
     *
     * @param userOrderCreateDTO 订单创建参数
     * @return 创建成功的订单编号
     */
    @PostMapping("createOrder")
    public Result<String> createOrder(@RequestBody UserOrderCreateDTO userOrderCreateDTO) {
        // 第一步：Dubbo RPC 调用 coffee-userorder，将订单写入 order 表
        String orderId = userOrderInfoService.createOrder(userOrderCreateDTO);

        // 第二步：Dubbo RPC 调用 coffee-expresstrack，同步创建快递单和初始轨迹
        // createExpress 内部完成两步写库：INSERT express + INSERT track（"商家已揽件"）
        expressTrackInfoService.createExpress(orderId);

        return new ResultUtil<String>().setData(orderId);
    }
}
