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
        // 直接按 order_id 分页查询快递轨迹，order_id 为空时返回全部记录。
        // 不再经过订单服务中转，避免空 order_id 时 selectOne 返回 null 导致 NPE。
        ExpressTrackInfoParamDTO expressTrackInfoParamDTO = new ExpressTrackInfoParamDTO();
        expressTrackInfoParamDTO.setOrder_id(userOrderInfoParamDTO.getOrder_id());
        expressTrackInfoParamDTO.setPageNum(userOrderInfoParamDTO.getPageNum());
        expressTrackInfoParamDTO.setPageSize(userOrderInfoParamDTO.getPageSize());
        return new ResultUtil<PageDTO>().setData(expressTrackInfoService.findExpressTrackInfos(expressTrackInfoParamDTO));
    }

    /**
     * 分页查询订单列表
     *
     * 与 findOrderList 的区别：此接口直接查询订单表，返回多条订单记录（带分页），
     * 不关联快递轨迹，适合订单管理列表页。
     *
     * 示例请求：POST http://localhost:8005/findOrders
     * 请求体：{ "order_id": "", "member_name": "张三", "pageNum": 1, "pageSize": 10 }
     *
     * @param userOrderInfoParamDTO 查询条件，可按 order_id、member_name 过滤，支持分页
     * @return 包装了订单分页数据的统一响应体
     */
    @PostMapping("findOrders")
    public Result<PageDTO> findOrders(@RequestBody UserOrderInfoParamDTO userOrderInfoParamDTO) {
        return new ResultUtil<PageDTO>().setData(userOrderInfoService.findUserOrderInfos(userOrderInfoParamDTO));
    }

    /**
     * 创建订单并同步创建快递单（Dubbo RPC 同步调用）
     *
     * 调用流程：
     *   1. 接收前端传来的订单创建参数
     *   2. Dubbo RPC 调用 coffee-userorder.createOrder()
     *      → coffee-userorder 将订单写入 userordertest 数据库的 order 表，返回 order_id
     *   3. Dubbo RPC 调用 coffee-expresstrack.createExpress(orderId)
     *      → coffee-expresstrack 写入 express 表（快递单）和 track 表（"商家已揽件"初始轨迹）
     *   4. 两个 RPC 调用都成功后，返回 order_id 给前端
     *
     * 示例请求：POST http://localhost:8005/createOrder
     * 请求体：{ "order_id": "ORDER100", "OneID": "U001", "order_amount": 199.0 }
     */
    @PostMapping("createOrder")
    public Result<String> createOrder(@RequestBody UserOrderCreateDTO userOrderCreateDTO) {
        try {
            String orderId = userOrderInfoService.createOrder(userOrderCreateDTO);
            expressTrackInfoService.createExpress(orderId);
            return new ResultUtil<String>().setData(orderId);
        } catch (Exception e) {
            Result<String> result = new Result<>();
            result.setSuccess(false);
            result.setCode(500);
            // Dubbo 跨 RPC 异常：provider 的完整堆栈被序列化成字符串放在 message 里，
            // getCause() 链在 consumer 侧已断，需从 message 字符串提取根因行。
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();
            String msg = root.getMessage();
            if (msg != null && msg.contains("### Cause:")) {
                int idx = msg.lastIndexOf("### Cause:");
                msg = msg.substring(idx + "### Cause:".length()).trim();
                int nl = msg.indexOf('\n');
                if (nl > 0) msg = msg.substring(0, nl).trim();
            }
            result.setMessage("创建订单失败：" + msg);
            return result;
        }
    }
}
