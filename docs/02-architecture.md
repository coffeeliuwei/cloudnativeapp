# 系统架构详解

> 本文档像写书一样，深入讲解本项目的架构设计思路、每一层的职责、以及各模块如何协作。适合想真正理解"微服务是什么"的同学。

---

## 目录

1. [从单体到微服务](#1-从单体到微服务)
2. [本项目的整体架构](#2-本项目的整体架构)
3. [分层模型解析](#3-分层模型解析)
4. [请求完整调用链路](#4-请求完整调用链路)
5. [模块间依赖关系](#5-模块间依赖关系)
6. [核心设计原则](#6-核心设计原则)

---

## 1. 从单体到微服务

### 1.1 什么是单体应用？

假设我们在做一个电商系统，最直接的方式是把所有功能写在一个项目里：

```
my-shop/
├── 用户模块（注册、登录）
├── 订单模块（下单、查单）
├── 商品模块（上下架、库存）
├── 快递模块（轨迹查询）
└── 支付模块（收款、退款）
```

这就是**单体应用**（Monolithic Application）。

**单体应用的问题：**

| 问题 | 表现 |
|------|------|
| 扩展困难 | 订单模块压力大，但无法只扩展订单模块，必须整体扩展 |
| 部署风险高 | 改一个小功能，整个系统都要重新部署，可能影响其他功能 |
| 团队协作难 | 100个人同时修改一个项目，代码冲突不断 |
| 技术栈绑定 | 整个系统只能用一种语言，换不了 |

### 1.2 微服务怎么解决这些问题？

**微服务架构**（Microservices）将系统拆分成一组小的、独立部署的服务，每个服务只负责一件事情：

```
用户服务（独立部署）   ←→
订单服务（独立部署）   ←→   通过网络互相调用
快递服务（独立部署）   ←→
支付服务（独立部署）   ←→
```

**好处：**
- 订单服务压力大？只扩展订单服务，其他服务不动
- 修改快递服务？只重新部署快递服务，其他服务不受影响
- 每个团队只维护自己的服务，互不干扰

---

## 2. 本项目的整体架构

本项目模拟了一个"电商物流查询系统"的简化微服务架构：

```
┌──────────────────────────────────────────────────────────────────┐
│                         用户层（浏览器）                           │
└────────────────────────────┬─────────────────────────────────────┘
                             │ HTTP/HTTPS
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│                   展示层（app-admin 前端）                         │
│                    Vue.js 2 + iView UI                            │
│                       端口：8080                                  │
└────────────────────────────┬─────────────────────────────────────┘
                             │ HTTP REST API
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│                   网关层（coffee-app）                             │
│            Spring Boot 统一入口，对外暴露 REST 接口                │
│                       端口：8005                                  │
└──────────┬──────────────────────────────┬────────────────────────┘
           │ Dubbo RPC                    │ Dubbo RPC
           ▼                              ▼
┌─────────────────────┐      ┌─────────────────────────────────────┐
│  coffee-userorder   │      │       coffee-expresstrack           │
│   用户订单微服务      │      │        快递轨迹微服务                 │
│   端口：7001         │      │    HTTP端口：8001 Dubbo端口：28888   │
└──────────┬──────────┘      └──────────────────┬──────────────────┘
           │ JDBC                               │ JDBC
           ▼                                    ▼
┌─────────────────────┐      ┌─────────────────────────────────────┐
│   RDS MySQL         │      │           RDS MySQL                 │
│   userordertest     │      │         expresstracktest            │
└─────────────────────┘      └─────────────────────────────────────┘
           │                                    │
           └──────────────┬─────────────────────┘
                          ▼
             ┌─────────────────────────┐
             │      Nacos 注册中心      │
             │      端口：8848          │
             │   统一管理服务地址        │
             └─────────────────────────┘
```

---

## 3. 分层模型解析

### 3.1 用户层

用户通过浏览器访问系统，无论是查询订单还是查看快递，都通过 HTTP 协议与前端交互。

### 3.2 展示层（app-admin）

**职责**：将后端数据用可视化界面展示给用户，并收集用户操作提交给后端。

**不直接操作数据库**，所有数据都来自后端 API。

**典型流程：**
```
用户点击"查询"按钮
   ↓
Vue 组件调用 Axios 发送 HTTP 请求
   ↓
收到 JSON 响应
   ↓
Vue 响应式地更新页面数据，页面自动刷新
```

### 3.3 网关层（coffee-app）

**职责**：
1. 接收前端的所有 HTTP 请求（统一入口）
2. 通过 Dubbo 调用后端微服务
3. 将微服务返回的数据聚合成统一格式，返回给前端

**为什么需要网关？**

没有网关时，前端需要知道每个微服务的地址：
```
前端 → 直接调用 localhost:7001（订单服务）
前端 → 直接调用 localhost:8001（快递服务）
```

问题：服务地址变了，每个前端页面都要改。

有网关时：
```
前端 → 只知道 localhost:8005（网关）
网关 → 内部调用订单服务
网关 → 内部调用快递服务
网关 → 聚合结果返回前端
```

好处：前端只依赖网关，微服务怎么变动与前端无关。

### 3.4 微服务层（coffee-userorder / coffee-expresstrack）

**职责**：每个微服务只做一件事——一个负责订单，一个负责快递。

每个微服务包含两个子模块：

```
coffee-userorder/
├── api/        接口定义层（Interface Layer）
│               ─ 定义服务有哪些方法
│               ─ 定义数据传输结构（DTO）
│               ─ 打包成 jar，供调用方（coffee-app）引用
│
└── provider/   接口实现层（Implementation Layer）
                ─ 实现 api 层定义的接口
                ─ 真正执行数据库查询
                ─ 是一个可以独立运行的 Spring Boot 应用
```

**为什么 api 和 provider 要分开？**

这是 Dubbo 的推荐架构模式，也是"面向接口编程"的体现：

```
coffee-app 只依赖 coffee-userorder-api（接口定义）
coffee-userorder-provider 实现这个接口并注册到 Nacos

调用时：
coffee-app 发出"我要调用 UserOrderInfoService.findUserOrderInfo()"
Dubbo 框架从 Nacos 找到实现者（coffee-userorder-provider）
透明地完成远程调用，coffee-app 感觉像在本地调用一样
```

### 3.5 数据层（RDS MySQL）

**职责**：持久化存储数据。

每个微服务有自己独立的数据库（**数据库隔离**），这是微服务架构的重要原则——服务之间不共享数据库，避免直接的数据耦合。

### 3.6 服务治理层（Nacos）

**职责**：
1. **服务注册**：微服务启动时，向 Nacos 登记"我在哪、叫什么、端口是多少"
2. **服务发现**：调用方启动时，问 Nacos"xxx 服务在哪"，得到地址后再发起调用
3. **健康检查**：定期检查服务是否存活，自动下线故障服务

**没有 Nacos 会怎样？**

调用方必须在配置文件里写死服务地址：
```yaml
userorder.url: 192.168.1.100:7001   # 写死了！
```

服务器地址一换，所有依赖它的配置都要改。Nacos 解决了这个问题，地址由它动态管理。

---

## 4. 请求完整调用链路

以用户查询 `ORDER001` 的快递轨迹为例，完整链路如下：

```
① 浏览器输入：http://localhost:8080/order/track?id=ORDER001
   ↓
② app-admin（Vue）调用：GET http://localhost:8005/hello/ORDER001

③ coffee-app/CoffeeController 接收请求：
   @RequestMapping("/hello/{orderid}")
   public PageDTO<...> helloCoffee(@PathVariable String orderid)

④ CoffeeController 调用 UserOrderInfoService（Dubbo 接口）：
   UserOrderInfoParamDTO param = new UserOrderInfoParamDTO();
   param.setOrder_id("ORDER001");
   UserOrderInfoResultDTO orderResult = userOrderInfoService.findUserOrderInfo(param);

⑤ Dubbo 框架介入：
   - 查询 Nacos，找到 coffee-userorder 的地址（127.0.0.1:7001）
   - 将方法调用序列化，通过网络发送给 coffee-userorder

⑥ coffee-userorder/UserOrderInfoServiceImpl 执行：
   - MyBatis 执行 SQL：SELECT ... FROM member m, `order` o WHERE m.OneID=o.OneID AND order_id='ORDER001'
   - 查询 RDS MySQL 的 userordertest 库
   - 返回订单信息（含 order_id='ORDER001'）

⑦ CoffeeController 拿到 order_id，再调用 ExpressTrackInfoService（Dubbo 接口）：
   ExpressTrackInfoParamDTO etParam = new ExpressTrackInfoParamDTO();
   etParam.setOrder_id("ORDER001");
   PageDTO result = expressTrackInfoService.findExpressTrackInfos(etParam);

⑧ Dubbo 框架介入：
   - 查询 Nacos，找到 coffee-expresstrack 的地址（127.0.0.1:8001）
   - 将调用通过 Dubbo 协议（端口 28888）发送

⑨ coffee-expresstrack/ExpresstrackInfoServiceImpl 执行：
   - MyBatis 执行 SQL：SELECT ... FROM express LEFT JOIN track ON ... WHERE order_id='ORDER001'
   - 查询 RDS MySQL 的 expresstracktest 库
   - 返回快递轨迹分页数据

⑩ CoffeeController 将 PageDTO 返回给前端（JSON 格式）

⑪ app-admin 接收 JSON，Vue 更新页面，用户看到快递轨迹列表
```

---

## 5. 模块间依赖关系

```
                    ┌────────────────────┐
                    │   coffee-common    │
                    │  (BasePageDTO,     │
                    │   PageDTO)         │
                    └───────┬────────────┘
           ┌────────────────┼────────────────┐
           ▼                ▼                ▼
┌──────────────────┐ ┌──────────────────┐ ┌─────────────────────────┐
│coffee-userorder  │ │coffee-expresstrack│ │     coffee-app          │
│      api         │ │       api         │ │                         │
│                  │ │                   │ │  依赖:                   │
│ 依赖:            │ │ 依赖:             │ │  ├─ coffee-userorder-api │
│ └─coffee-common  │ │ └─coffee-common   │ │  └─ coffee-expresstrack-│
└────────┬─────────┘ └───────┬───────────┘ │       api               │
         ▼                   ▼             └─────────────────────────┘
┌──────────────────┐ ┌──────────────────┐
│coffee-userorder  │ │coffee-expresstrack│
│    provider      │ │     provider      │
│                  │ │                   │
│ 依赖:            │ │ 依赖:             │
│ └─coffee-userorder│ └─coffee-expresstrack│
│     -api         │ │       -api        │
└──────────────────┘ └──────────────────┘
```

**依赖规则：**

1. `coffee-common` 不依赖任何项目模块（最底层）
2. `api` 模块只依赖 `coffee-common`
3. `provider` 模块依赖自己的 `api` 模块（需要实现接口）
4. `coffee-app` 依赖两个 `api` 模块（只需要接口，不需要实现）

**Maven 构建顺序（必须严格遵守）：**

```
coffee-common  →  coffee-userorder/api  →  coffee-expresstrack/api  →  coffee-app
                                       ↘                            ↗
                               coffee-userorder/provider  coffee-expresstrack/provider
```

---

## 6. 核心设计原则

### 6.1 单一职责原则

每个微服务只做一件事。`coffee-userorder` 只管订单，`coffee-expresstrack` 只管快递。好处：哪里有问题，一眼就知道去哪里修复。

### 6.2 接口与实现分离

`api` 模块定义"做什么"，`provider` 模块实现"怎么做"。调用方只依赖接口，实现可以随时替换（比如把 MySQL 换成 MongoDB），调用方不用改代码。

### 6.3 数据库隔离

每个微服务有自己的数据库，互不共享。避免了一个服务的数据库变更影响其他服务。代价是：跨服务的数据一致性需要应用层保证（这是分布式系统的难题，本项目暂不涉及）。

### 6.4 统一入口

对外只暴露 `coffee-app`（端口 8005），内部的微服务端口不对外开放。这样可以在网关层统一处理权限验证、日志记录、限流等横切关注点。

---

[← 返回主文档](../README.md)
