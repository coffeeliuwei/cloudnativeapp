# 各模块代码详解

> 本文档逐文件讲解每个模块的代码结构、设计意图和关键实现，帮助零基础同学真正读懂代码。

---

## 目录

1. [coffee-common（公共基础库）](#1-coffee-common公共基础库)
2. [coffee-userorder（用户订单微服务）](#2-coffee-userorder用户订单微服务)
3. [coffee-expresstrack（快递轨迹微服务）](#3-coffee-expresstrack快递轨迹微服务)
4. [coffee-app（主应用网关）](#4-coffee-app主应用网关)
5. [app-admin（Vue.js 前端）](#5-app-adminvuejs-前端)

---

## 1. coffee-common（公共基础库）

### 作用

存放所有微服务都要用到的"公用零件"。就像一个工具箱，大家都可以从里面取东西，不用每个服务重复制造。

### 目录结构

```
coffee-common/
├── pom.xml                          项目构建配置
└── src/main/java/com/coffee/yun/dto/
    ├── BasePageDTO.java             分页查询的基础参数
    └── PageDTO.java                 分页查询的返回结果
```

---

### pom.xml 解析

```xml
<groupId>com.coffee.yun</groupId>
<artifactId>coffee-common</artifactId>
<version>1.0-SNAPSHOT</version>
```

- `groupId`：组织标识，类似 Java 包名的最顶层，代表"谁开发的"
- `artifactId`：模块名称，代表"这是什么"
- `version`：版本号，`SNAPSHOT` 表示还在开发中的快照版本

**关键依赖说明：**

| 依赖 | 作用 |
|------|------|
| `lombok` | 自动生成 getter/setter/toString，减少样板代码 |
| `fastjson` | 阿里巴巴的 JSON 解析库，用于对象与 JSON 互转 |
| `pagehelper` | MyBatis 分页插件，自动处理分页 SQL |
| `hutool-all` | 国产工具类库，提供各种常用工具方法 |

---

### BasePageDTO.java 详解

```java
package com.coffee.yun.dto;

/**
 * 分页查询的基础参数
 * 所有需要分页的查询 DTO 都继承这个类
 */
public class BasePageDTO {
    private Integer pageNum;   // 当前页码，从 1 开始
    private Integer pageSize;  // 每页显示的条数
    
    // getter/setter 省略（实际使用 Lombok 自动生成）
}
```

**为什么需要它？**

几乎所有列表查询都需要分页（不能一次返回数据库里所有数据）。通过让所有查询参数类继承 `BasePageDTO`，就自动拥有了 `pageNum` 和 `pageSize` 两个字段，不用每次重复写。

---

### PageDTO.java 详解

```java
package com.coffee.yun.dto;

import java.util.List;

/**
 * 分页查询的返回结果
 * 泛型 T 代表每一条数据的类型，可以是任意对象
 */
public class PageDTO<T> {
    private List<T> list;    // 当前页的数据列表
    private Long total;      // 数据总条数（用于前端计算总页数）
    
    // getter/setter 省略
}
```

**泛型 `<T>` 是什么？**

`T` 是一个占位符，表示"某种类型"。使用时再具体指定：

```java
PageDTO<ExpressTrackInfoResultDTO>   // T = 快递轨迹数据
PageDTO<UserOrderInfoResultDTO>      // T = 用户订单数据
```

这样 `PageDTO` 就能被所有服务复用，不用为每种数据写一个分页类。

---

## 2. coffee-userorder（用户订单微服务）

### 整体职责

负责查询用户的订单信息。当 `coffee-app` 需要知道某个订单是谁下的、金额是多少时，就来这里查。

### 目录结构

```
coffee-userorder/
├── pom.xml                          父模块配置（管理 api 和 provider）
├── api/                             接口定义层（被调用方引用）
│   ├── pom.xml
│   └── src/main/java/com/coffee/yun/userorder/api/
│       ├── dto/
│       │   ├── UserOrderInfoParamDTO.java    查询参数
│       │   └── UserOrderInfoResultDTO.java   返回结果
│       └── service/
│           └── UserOrderInfoService.java     服务接口
└── provider/                        接口实现层（实际运行的服务）
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/com/coffee/yun/userorder/
        │   │   ├── UserOrderApplication.java          启动类
        │   │   └── provider/
        │   │       ├── service/
        │   │       │   └── UserOrderInfoServiceImpl.java   接口实现
        │   │       └── utils/
        │   │           └── PageUtil.java               分页工具
        │   └── resources/
        │       ├── application.yml                     主配置
        │       ├── application-dev.yml                 开发环境配置
        │       ├── logback-spring.xml                  日志配置
        │       └── mapper/
        │           └── UserOrderMapper.xml             SQL 语句
        └── ...
```

---

### 2.1 api 子模块

#### UserOrderInfoParamDTO.java

```java
package com.coffee.yun.userorder.api.dto;

import com.coffee.yun.dto.BasePageDTO;

/**
 * 查询用户订单的请求参数
 * 继承 BasePageDTO 自动获得分页参数（pageNum, pageSize）
 */
public class UserOrderInfoParamDTO extends BasePageDTO {
    private String order_id;    // 订单ID，精确查询
    private String member_name; // 用户名，模糊查询
    
    // getter/setter
}
```

**字段命名为什么用下划线？**

这与数据库字段名保持一致（数据库表字段是 `order_id`、`member_name`），MyBatis 映射时更简单，不需要额外配置驼峰转换。

---

#### UserOrderInfoResultDTO.java

```java
package com.coffee.yun.userorder.api.dto;

/**
 * 查询用户订单的返回结果
 * 字段与数据库表列名对应
 */
public class UserOrderInfoResultDTO {
    private String member_name;   // 用户姓名
    private String member_phone;  // 手机号
    private String OneID;         // 用户唯一标识
    private String order_id;      // 订单编号
    private String order_amount;  // 订单金额
    private String order_status;  // 订单状态
    
    // getter/setter
}
```

---

#### UserOrderInfoService.java

```java
package com.coffee.yun.userorder.api.service;

import com.coffee.yun.dto.PageDTO;
import com.coffee.yun.userorder.api.dto.UserOrderInfoParamDTO;
import com.coffee.yun.userorder.api.dto.UserOrderInfoResultDTO;

/**
 * 用户订单查询服务接口
 * 这里只定义"能做什么"，不写"怎么做"
 * Dubbo 会通过这个接口找到远程实现并调用
 */
public interface UserOrderInfoService {

    /**
     * 根据条件查询用户订单信息
     * @param param 查询参数（包含订单ID或用户名）
     * @return 分页结果（包含订单列表和总条数）
     */
    UserOrderInfoResultDTO findUserOrderInfo(UserOrderInfoParamDTO param);
}
```

**接口的意义：**

`coffee-app` 在编译时只需要这个接口文件，不需要知道实现代码在哪。运行时，Dubbo 自动通过 Nacos 找到 `coffee-userorder-provider` 并调用，对 `coffee-app` 来说就像在本地调用一样。

---

### 2.2 provider 子模块

#### UserOrderApplication.java

```java
package com.coffee.yun.userorder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 用户订单服务启动类
 * @SpringBootApplication 是 Spring Boot 的核心注解，做了三件事：
 *   1. 开启自动配置（根据依赖自动配置 DataSource、MyBatis 等）
 *   2. 开启组件扫描（自动发现同包下的 @Service、@Repository 等）
 *   3. 标识这是一个配置类
 */
@SpringBootApplication
public class UserOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserOrderApplication.class, args);
    }
}
```

---

#### UserOrderInfoServiceImpl.java

```java
package com.coffee.yun.userorder.provider.service;

import com.coffee.yun.dto.PageDTO;
import com.coffee.yun.userorder.api.dto.UserOrderInfoParamDTO;
import com.coffee.yun.userorder.api.dto.UserOrderInfoResultDTO;
import com.coffee.yun.userorder.api.service.UserOrderInfoService;
import com.coffee.yun.userorder.provider.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 用户订单服务接口的实现类
 *
 * @DubboService  关键注解！告诉 Dubbo "这个类是服务提供者"
 *                Dubbo 会将此服务注册到 Nacos，供其他服务调用
 *
 * @Slf4j         Lombok 注解，自动生成日志对象 log
 *                使用：log.info("日志内容")
 */
@Slf4j
@DubboService
public class UserOrderInfoServiceImpl implements UserOrderInfoService {

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;  // MyBatis 的操作模板

    @Autowired
    private PageUtil pageUtil;  // 分页工具

    @Override
    public UserOrderInfoResultDTO findUserOrderInfo(UserOrderInfoParamDTO param) {
        // 调用 PageUtil 执行分页查询
        // "UserOrderMapper.selectByParam" 对应 UserOrderMapper.xml 中的 SQL id
        PageDTO<UserOrderInfoResultDTO> pageResult =
            pageUtil.selectPage("UserOrderMapper.selectByParam", param);
        
        // 取第一条数据返回（该方法用于精确查询单条订单）
        if (pageResult.getList() != null && !pageResult.getList().isEmpty()) {
            return pageResult.getList().get(0);
        }
        return null;
    }
}
```

**`@DubboService` 和 `@Service` 的区别：**

| 注解 | 作用 |
|------|------|
| `@Service`（Spring）| 标记为 Spring 管理的组件，只在本地应用内可见 |
| `@DubboService` | 标记为 Dubbo 服务，会注册到 Nacos，**可以被其他应用远程调用** |

---

#### UserOrderMapper.xml 详解

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<!-- namespace：命名空间，在 Java 代码中通过这个名称引用这个 mapper -->
<mapper namespace="UserOrderMapper">

    <!-- resultMap：定义数据库列名 到 Java 对象字段 的映射关系 -->
    <resultMap id="BaseResultMap"
               type="com.coffee.yun.userorder.api.dto.UserOrderInfoResultDTO">
        <result column="member_name"  property="member_name"  jdbcType="VARCHAR"/>
        <result column="member_phone" property="member_phone" jdbcType="VARCHAR"/>
        <result column="OneID"        property="OneID"        jdbcType="VARCHAR"/>
        <result column="order_id"     property="order_id"     jdbcType="VARCHAR"/>
        <result column="order_amount" property="order_amount" jdbcType="VARCHAR"/>
        <result column="order_status" property="order_status" jdbcType="VARCHAR"/>
    </resultMap>

    <!-- select：查询语句
         id：对应 Java 代码中调用时用的名称（UserOrderMapper.selectByParam）
         resultMap：查询结果映射到上面定义的 BaseResultMap
         parameterType：参数类型（传入的查询条件对象）
    -->
    <select id="selectByParam"
            resultMap="BaseResultMap"
            parameterType="com.coffee.yun.userorder.api.dto.UserOrderInfoParamDTO">
        SELECT m.member_name,
               m.member_phone,
               o.OneID,
               o.order_id,
               o.order_amount,
               o.order_status
        FROM member m, `order` o
        WHERE m.OneID = o.OneID

        <!-- 动态 SQL：只有参数不为空时才加上这个条件 -->
        <if test="order_id != null and order_id != ''">
            AND o.order_id = #{order_id, jdbcType=VARCHAR}
        </if>
        <if test="member_name != null and member_name != ''">
            AND m.member_name = #{member_name, jdbcType=VARCHAR}
        </if>
    </select>

</mapper>
```

**`#{}` 和 `${}` 的区别（重要的安全知识）：**

| 写法 | 处理方式 | 安全性 |
|------|---------|------|
| `#{order_id}` | 预编译（PreparedStatement），参数值会被转义 | ✅ 安全，防SQL注入 |
| `${order_id}` | 直接字符串拼接 | ❌ 有 SQL 注入风险，不要用 |

**动态 SQL `<if>` 的用途：**

当用户只填了订单ID时，不需要再按用户名查。`<if>` 标签让 SQL 根据实际传入的参数动态构建，避免了多余的条件。

---

#### application.yml 详解

```yaml
server:
  port: 7001           # 本服务的 HTTP 端口（虽然不直接对外，但 Spring Boot 需要）

spring:
  application:
    name: coffee-userorder     # 服务在 Nacos 中注册的名称

  profiles:
    active: ${ENV:dev}         # 激活的配置文件，优先读取环境变量 ENV，默认 dev

  datasource:
    name: coffee-userorder
    # 数据库连接字符串，${} 引用 application-dev.yml 中的变量
    url: jdbc:mysql://${database.host}/${database.dbname}?serverTimezone=UTC&useUnicode=true&characterEncoding=utf-8&useSSL=false
    username: ${database.user}
    password: ${database.password}
    driverClassName: com.mysql.jdbc.Driver

mybatis:
  mapperLocations: classpath:mapper/*.xml    # 扫描 resources/mapper/ 下的所有 xml
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl   # 将 SQL 打印到控制台（开发时方便调试）

dubbo:
  scan:
    basePackages: com.coffee.yun.userorder   # Dubbo 扫描这个包，找所有 @DubboService 注册
  application:
    name: coffee-userorder                    # 在 Dubbo 中的应用名
  registry:
    address: nacos://127.0.0.1:8848          # Nacos 地址，服务在这里注册和发现
```

---

## 3. coffee-expresstrack（快递轨迹微服务）

### 整体职责

负责查询快递轨迹。当 `coffee-app` 需要知道某个订单的快递走到哪里了，就来这里查。

结构与 `coffee-userorder` 完全对称，下面重点讲不同的地方。

---

### 3.1 ExpressTrackInfoService.java

```java
package com.coffee.yun.expresstrack.api.service;

import com.coffee.yun.dto.PageDTO;
import com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoParamDTO;
import com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoResultDTO;

/**
 * 快递轨迹查询接口
 * 注意返回值是 PageDTO<ExpressTrackInfoResultDTO>
 * 因为一个订单可能有多条轨迹（揽收→转运→派送），所以返回列表
 */
public interface ExpressTrackInfoService {
    PageDTO<ExpressTrackInfoResultDTO> findExpressTrackInfos(
        ExpressTrackInfoParamDTO expressTrackInfoParamDTO);
}
```

**与订单服务的对比：**

| | 订单服务 | 快递服务 |
|---|---------|---------|
| 返回值 | `UserOrderInfoResultDTO`（单个对象）| `PageDTO<ExpressTrackInfoResultDTO>`（列表）|
| 原因 | 一个订单ID对应一条订单 | 一个订单ID对应多条轨迹记录 |

---

### 3.2 ExpressTrackMapper.xml 详解

```xml
<mapper namespace="ExpressTrackMapper">

    <resultMap id="BaseResultMap"
               type="com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoResultDTO">
        <result column="order_id"        property="order_id"        jdbcType="VARCHAR"/>
        <result column="express_id"      property="express_id"      jdbcType="VARCHAR"/>
        <result column="express_weight"  property="express_weight"  jdbcType="VARCHAR"/>
        <result column="track_id"        property="track_id"        jdbcType="VARCHAR"/>
        <result column="track_show"      property="track_show"      jdbcType="VARCHAR"/>
    </resultMap>

    <select id="selectByParam"
            resultMap="BaseResultMap"
            parameterType="com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoParamDTO">
        SELECT express.express_id,
               order_id,
               express_weight,
               track_id,
               track_show
        FROM `express`
        LEFT JOIN `track` ON `express`.`express_id` = `track`.`express_id`
        <if test="order_id != null and order_id != ''">
            AND order_id = #{order_id, jdbcType=VARCHAR}
        </if>
    </select>

</mapper>
```

**`LEFT JOIN` 的含义：**

```
express 表（快递基本信息）：
express_id | order_id | express_weight
EX001      | ORDER001 | 1.5kg

track 表（轨迹记录）：
express_id | track_id | track_show
EX001      | T001     | 2024-01-01 10:00 已揽收
EX001      | T002     | 2024-01-02 08:00 已转运
EX001      | T003     | 2024-01-03 15:00 派送中

LEFT JOIN 结果（每条轨迹与快递信息合并）：
express_id | order_id | express_weight | track_id | track_show
EX001      | ORDER001 | 1.5kg          | T001     | 已揽收
EX001      | ORDER001 | 1.5kg          | T002     | 已转运
EX001      | ORDER001 | 1.5kg          | T003     | 派送中
```

---

### 3.3 application.yml 特殊配置

```yaml
dubbo:
  protocol:
    port: 28888    # Dubbo 服务通信端口（与 HTTP 端口 8001 不同）
```

**Dubbo 为什么需要独立端口？**

Dubbo 默认使用自己的二进制协议（Dubbo Protocol）进行高性能 RPC 通信，不是普通的 HTTP。所以需要一个单独的端口（28888）专门用于服务间调用。

- HTTP 端口（8001）：Spring Boot Web 端口，供健康检查等使用
- Dubbo 端口（28888）：专门用于 RPC 调用

---

## 4. coffee-app（主应用网关）

### 整体职责

对外提供 REST HTTP 接口，内部通过 Dubbo 聚合各微服务的能力。是整个系统的"大门"。

---

### 4.1 CoffeeAppApplication.java

```java
package com.coffee.yun.coffeeapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 主应用启动类
 *
 * exclude = {DataSourceAutoConfiguration.class}
 * 排除数据源自动配置！因为 coffee-app 本身不连接数据库，
 * 数据由微服务提供，这里如果不排除会因为没有数据库配置而报错。
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class CoffeeAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoffeeAppApplication.class, args);
    }
}
```

---

### 4.2 CoffeeController.java 详解

```java
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

/**
 * REST API 控制器
 *
 * @CrossOrigin    允许跨域请求（前端和后端在不同端口时需要这个注解）
 * @RestController 标记这是一个 REST 控制器，方法返回值直接序列化为 JSON
 */
@CrossOrigin
@RestController
public class CoffeeController {

    /**
     * @Reference 是 Dubbo 注解，表示"从 Nacos 获取这个服务的远程代理"
     * 使用时和普通本地对象一样调用，但实际上通过网络调用了远程服务
     */
    @Reference
    private UserOrderInfoService userOrderInfoService;

    @Reference
    private ExpressTrackInfoService expressTrackInfoService;

    /**
     * 接口 1：根据订单ID查询快递轨迹
     *
     * @RequestMapping 映射 HTTP 路径和方法
     * @PathVariable   从 URL 路径中提取参数（{orderid} 部分）
     *
     * 调用示例：GET http://localhost:8005/hello/ORDER001
     */
    @RequestMapping("/hello/{orderid}")
    public PageDTO<ExpressTrackInfoResultDTO> helloCoffee(
            @PathVariable String orderid) {

        // 第一步：用订单ID查询订单信息（获取关联的快递单号等）
        UserOrderInfoParamDTO orderParam = new UserOrderInfoParamDTO();
        orderParam.setOrder_id(orderid);
        UserOrderInfoResultDTO orderResult =
            userOrderInfoService.findUserOrderInfo(orderParam);

        // 第二步：用订单信息查快递轨迹
        ExpressTrackInfoParamDTO trackParam = new ExpressTrackInfoParamDTO();
        trackParam.setOrder_id(orderResult.getOrder_id());

        // 直接返回快递轨迹分页数据
        return expressTrackInfoService.findExpressTrackInfos(trackParam);
    }

    /**
     * 接口 2：查询订单列表
     *
     * @PostMapping  映射 HTTP POST 请求
     * @RequestBody  从请求 body 中读取 JSON 并转换为 Java 对象
     *
     * 调用示例：POST http://localhost:8005/findOrderList
     *           Body: {"order_id": "ORDER001"}
     */
    @PostMapping("findOrderList")
    public Result<PageDTO> findOrderList(
            @RequestBody UserOrderInfoParamDTO param) {

        // 查询订单信息
        UserOrderInfoResultDTO orderResult =
            userOrderInfoService.findUserOrderInfo(param);

        // 查询该订单对应的快递轨迹
        ExpressTrackInfoParamDTO trackParam = new ExpressTrackInfoParamDTO();
        trackParam.setOrder_id(orderResult.getOrder_id());

        PageDTO trackPage =
            expressTrackInfoService.findExpressTrackInfos(trackParam);

        // 用 ResultUtil 包装成统一的 Result 格式返回
        return new ResultUtil<PageDTO>().setData(trackPage);
    }
}
```

---

### 4.3 Result.java — 统一响应格式

```java
package com.coffee.yun.coffeeapp.base;

import java.io.Serializable;

/**
 * HTTP 接口统一响应体
 * 所有接口都应该返回这个结构，前端好处理
 *
 * 典型响应示例：
 * {
 *   "success": true,
 *   "code": 200,
 *   "message": "success",
 *   "timestamp": 1704038400000,
 *   "result": { ...实际数据... }
 * }
 */
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;              // 请求是否成功
    private String message;               // 提示信息
    private Integer code;                 // 状态码（200成功，500错误）
    private long timestamp = System.currentTimeMillis();  // 时间戳
    private T result;                     // 实际返回的数据（泛型）

    // ... getter/setter
}
```

---

### 4.4 application.properties

```properties
# Dubbo 应用名称
dubbo.application.name=coffee-app

# Nacos 地址（服务发现用）
dubbo.registry.address=nacos://localhost:8848

# 本应用 HTTP 服务端口
server.port=8005
```

> **为什么用 `.properties` 而不是 `.yml`？**
> 两者功能相同，只是格式不同。`.yml` 层级结构更清晰，`.properties` 更简洁。项目中可以混用，但同一模块建议统一风格。

---

## 5. app-admin（Vue.js 前端）

### 整体定位

基于开源模板 `iview-admin` 构建的管理后台。iview-admin 提供了完整的菜单、权限、路由框架，我们在此基础上开发业务页面。

### 核心文件详解

#### src/main.js（应用入口）

```javascript
// 创建 Vue 实例，挂载根组件
new Vue({
  el: '#app',
  router,      // 路由（页面跳转）
  store,       // 状态管理（全局数据）
  i18n,        // 国际化（多语言）
  render: h => h(App)  // 渲染根组件 App.vue
})
```

#### src/router/routers.js（路由配置）

定义 URL 路径与 Vue 组件的对应关系：

```javascript
export default [
  {
    path: '/login',              // URL 路径
    component: () => import('@/view/login/login.vue'),  // 对应的页面组件
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: Main,             // 主框架（含菜单、头部）
    children: [
      {
        path: 'home',
        component: () => import('@/view/single-page/home/home.vue'),
        meta: { title: '首页', icon: 'md-home' }
      },
      {
        path: 'order',
        component: () => import('@/view/order/order.vue'),
        meta: { title: '订单管理' }
      }
    ]
  }
]
```

#### src/store/module/user.js（用户状态管理）

```javascript
// Vuex Store：全局共享状态
export default {
  state: {
    userName: '',    // 当前登录用户名
    token: ''        // 登录凭证
  },
  mutations: {
    // 登录成功后更新用户信息
    setUser(state, user) {
      state.userName = user.name;
      state.token = user.token;
    }
  },
  actions: {
    // 登录请求（异步）
    login({ commit }, { username, password }) {
      return api.login(username, password).then(user => {
        commit('setUser', user);
      });
    }
  }
}
```

#### src/view/order/order.vue（订单页面示例）

```vue
<template>
  <!-- 模板：定义页面结构（HTML）-->
  <div>
    <Input v-model="searchOrderId" placeholder="输入订单ID" />
    <Button @click="search">查询</Button>

    <Table :columns="columns" :data="trackList" />
  </div>
</template>

<script>
// 脚本：定义数据和行为（JavaScript）
export default {
  data() {
    return {
      searchOrderId: '',    // 搜索框的值（v-model 双向绑定）
      trackList: [],        // 表格数据
      columns: [            // 表格列定义
        { title: '快递单号', key: 'express_id' },
        { title: '重量',     key: 'express_weight' },
        { title: '轨迹',     key: 'track_show' }
      ]
    }
  },
  methods: {
    async search() {
      // 调用后端接口
      const res = await this.$axios.get(`/hello/${this.searchOrderId}`);
      this.trackList = res.data.list;   // 更新表格数据，页面自动刷新
    }
  }
}
</script>

<style scoped>
/* 样式：只作用于当前组件 */
</style>
```

---

[← 返回主文档](../README.md)
