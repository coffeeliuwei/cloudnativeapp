# 系统架构详解

> 本文档深入讲解本项目的架构设计思路、每一层的职责、各模块如何协作，以及关键技术选型背后的原因。适合想真正理解"微服务是什么、Dubbo 是怎么工作的、为什么要这样设计"的同学。

---

## 目录

1. [从单体到微服务](#1-从单体到微服务)
2. [本项目的整体架构](#2-本项目的整体架构)
3. [分层模型解析](#3-分层模型解析)
4. [Dubbo RPC 原理详解](#4-dubbo-rpc-原理详解)
5. [Nacos 服务治理机制](#5-nacos-服务治理机制)
6. [Redis 缓存层详解](#6-redis-缓存层详解)
7. [RocketMQ 消息队列详解](#7-rocketmq-消息队列详解)
8. [Nacos Config 动态配置详解](#8-nacos-config-动态配置详解)
9. [请求完整调用链路](#9-请求完整调用链路)
10. [模块间依赖关系](#10-模块间依赖关系)
11. [配置分层设计](#11-配置分层设计)
12. [核心设计原则与取舍](#12-核心设计原则与取舍)

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
| 扩展困难 | 大促时订单模块压力很大，但没法只扩展订单模块——整个系统必须一起扩容 |
| 部署风险高 | 改了快递模块的一行代码，整个系统都要重新打包、测试、部署，可能影响其他模块 |
| 团队协作难 | 100个人同时修改同一个代码库，分支冲突不断，发布窗口难以协调 |
| 技术栈绑定 | 整个系统只能用一种语言，某个模块想用更合适的技术也换不了 |
| 故障扩散 | 支付模块内存泄漏，可能拖垮整个应用，订单、商品模块也跟着不可用 |

### 1.2 微服务怎么解决这些问题？

**微服务架构**（Microservices）将系统拆分成一组小的、独立运行的服务，每个服务只负责一件事情：

```
用户服务（独立进程，独立部署）
订单服务（独立进程，独立部署）   ← 这三个服务通过网络互相调用
快递服务（独立进程，独立部署）
支付服务（独立进程，独立部署）
```

**好处：**
- 订单服务压力大？只扩展订单服务，其他服务不动
- 修改快递服务？只重新部署快递服务，其他服务正常运行
- 每个团队只维护自己的服务，互不干扰，发布节奏独立
- 支付服务崩了，订单服务仍然可以工作（取决于设计）

**代价：**
- 分布式系统的复杂性：网络调用比本地调用慢，可能失败
- 跨服务的数据一致性需要应用层保证（不能用数据库事务）
- 本地开发需要同时启动多个服务，调试更复杂
- 需要服务注册与发现、分布式追踪、链路监控等基础设施

> **微服务不是银弹。** 小团队小项目用单体架构更合适，微服务的价值在于规模大到一定程度时。本项目的目的是学习微服务的核心概念和技术栈。

---

## 2. 本项目的整体架构

本项目模拟了一个"电商物流查询系统"的云原生微服务架构：

```
┌──────────────────────────────────────────────────────────────────┐
│                         用户层（浏览器）                           │
└────────────────────────────┬─────────────────────────────────────┘
                             │ HTTP/HTTPS
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│                   展示层（app-admin 前端）                         │
│                  Vue.js 3.4 + ViewUI Plus 1.x  端口：8080         │
└────────────────────────────┬─────────────────────────────────────┘
                             │ HTTP REST（Axios，JSON 格式）
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│                   网关层（coffee-app）                             │
│            Spring Boot，统一入口，对外暴露 REST 接口  端口：8005    │
└──────────┬──────────────────────────────┬────────────────────────┘
           │ Dubbo RPC（二进制协议）        │ Dubbo RPC（二进制协议）
           ▼                              ▼
┌─────────────────────┐      ┌─────────────────────────────────────┐
│  coffee-userorder   │      │       coffee-expresstrack           │
│   用户订单微服务      │      │        快递轨迹微服务                 │
│   HTTP端口：7001     │      │    HTTP端口：8001 Dubbo端口：28888   │
│                     │      │                                     │
│  ┌───────────────┐  │      │  ┌───────────────────────────────┐  │
│  │ Redis 缓存    │  │      │  │ Redis 缓存                    │  │
│  │ order:detail: │  │      │  │ express:track:{order_id}      │  │
│  │ {order_id}    │  │      │  └───────────────────────────────┘  │
│  └───────────────┘  │      │                                     │
│         │ 发布消息   │      │  ← 消费 "order-created" 消息        │
└─────┬───┴─────┬──────┘      └──────┬──────────────────┬──────────┘
      │JDBC     │                    │JDBC              │
      ▼         ▼                    ▼                  │
 RDS MySQL   RocketMQ ──────────────消费────────────────┘
(userorder)  Name Server
             :9876
      │                              │
      └──────────────┬───────────────┘
                     ▼
        ┌─────────────────────────────┐
        │       Nacos 2.x :8848       │
        │  服务注册中心 + 动态配置中心   │
        │  Data ID 示例：             │
        │  coffee-userorder.properties│
        │  cache.ttl.order=1800       │
        └─────────────────────────────┘
```

👉 [查看完整可视化框架图（含代码文件细节、端口、Nacos 流向）](./architecture.html)

---

## 3. 分层模型解析

### 3.1 展示层（app-admin）

**职责**：将后端数据用可视化界面展示给用户，收集用户操作提交给后端。

**核心原则**：**展示层不直接操作数据库**。所有数据通过 REST API 从 `coffee-app` 获取。

**典型流程：**
```
用户点击"查询"按钮
   ↓ Vue 双向绑定读取输入框的值
Axios 发送 HTTP GET 请求到 localhost:8005/hello/ORDER001
   ↓ 收到 JSON 响应
Vue 响应式系统自动更新 trackList 变量
   ↓ 绑定了 trackList 的 Table 组件自动重新渲染
用户看到快递轨迹表格
```

### 3.2 网关层（coffee-app）

**职责**：系统对外的**唯一 HTTP 入口**，扮演"总前台"的角色。

**为什么需要网关层：**

```
没有网关（错误做法）：
  前端 → localhost:7001（订单服务）  ← 地址硬编码在前端
  前端 → localhost:8001（快递服务）  ← 跨域问题需要在每个服务分别处理

有网关：
  前端 → localhost:8005（网关，唯一地址）
  网关 → 订单服务（内网调用，对前端不可见）
  网关 → 快递服务（内网调用，对前端不可见）
  网关 → 聚合结果，统一格式，返回给前端
```

**好处：**
- 前端只依赖网关地址，后端服务地址变了也不影响前端
- 权限验证、日志记录、限流可以在网关统一处理
- 内部服务端口不对外暴露，减少攻击面

### 3.3 微服务层（coffee-userorder / coffee-expresstrack）

每个微服务采用 **api/provider 分层模式**：

```
coffee-userorder/
├── api/        接口定义层（"合同"）
│               ─ 定义服务有哪些方法（Java 接口）
│               ─ 定义数据传输对象 DTO
│               ─ 打包成 JAR，供调用方引用
│               ─ 不包含任何实现代码
│
└── provider/   接口实现层（"履行合同"）
                ─ 实现 api 层定义的接口
                ─ 连接数据库，执行真正的查询
                ─ 独立运行的 Spring Boot 应用
```

**为什么 api 和 provider 要分开（面向接口编程）：**

```
coffee-app 只依赖 coffee-userorder-api（只有接口，没有实现）
  ↓ 编译阶段：能调用接口方法，类型检查通过
  ↓ 运行阶段：Dubbo 动态找到实现（coffee-userorder-provider）并执行

好处：
  coffee-app 不知道实现细节，实现可以随时替换
  （比如把 MySQL 换成 MongoDB），coffee-app 无需改动
  这是"依赖倒置原则"的体现
```

### 3.4 数据层（RDS MySQL）

**职责**：持久化存储数据。

**数据库隔离**：每个微服务有**自己专属的数据库**，服务间不共享数据库，不能直接跨库 JOIN。

| 微服务 | 数据库 | 包含表 |
|--------|--------|--------|
| coffee-userorder | userordertest | member（用户）、order（订单）|
| coffee-expresstrack | expresstracktest | express（快递）、track（轨迹）|

跨数据库的关联（如订单与快递轨迹的关联）只能通过**应用层**实现：先查订单拿到 order_id，再用 order_id 查快递。

### 3.5 服务治理层（Nacos）

**职责**：管理微服务的地址，实现服务的注册、发现和健康检查。

详见 [第5节 Nacos 服务治理机制](#5-nacos-服务治理机制)。

---

## 4. Dubbo RPC 原理详解

### 4.1 RPC vs HTTP REST：为什么微服务间不用 HTTP？

很多同学会问：`coffee-app` 调用 `coffee-userorder` 时，为什么不直接用 HTTP（像前端调后端那样），而要用 Dubbo RPC？

**HTTP REST 调用方式：**
```java
// 如果用 HTTP
String url = "http://localhost:7001/findOrder?orderId=ORDER001";
ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
String json = response.getBody();
UserOrderInfoResultDTO result = objectMapper.readValue(json, UserOrderInfoResultDTO.class);
```

**Dubbo RPC 调用方式：**
```java
// 用 Dubbo，像本地方法调用一样
@DubboReference
private UserOrderInfoService userOrderInfoService;

UserOrderInfoResultDTO result = userOrderInfoService.findUserOrderInfo(param);
// 就这一行！Dubbo 帮你处理了网络通信的所有细节
```

**对比分析：**

| 对比项 | HTTP REST | Dubbo RPC |
|--------|-----------|-----------|
| 调用方式 | 构造 URL，发送 HTTP 请求，解析 JSON | 像调用本地方法一样，编译器类型检查 |
| 性能 | HTTP 头部开销大，JSON 序列化/反序列化慢 | 二进制协议，序列化更紧凑，延迟更低 |
| 类型安全 | URL 是字符串，参数容易写错 | 编译时检查接口签名，参数类型严格 |
| 服务发现 | 需要自己实现或引入额外组件 | Dubbo 内置，与 Nacos 深度集成 |
| 负载均衡 | 需要额外配置（如 Nginx）| Dubbo 内置多种负载均衡策略 |
| 适合场景 | 对外暴露的 API（前端调用、跨公司调用）| 内部服务间调用 |

> **总结：** HTTP REST 适合对外暴露（前端能直接用浏览器调用），Dubbo RPC 适合内部服务间（强类型、高性能、内置服务治理）。本项目外部用 HTTP，内部用 Dubbo，是合理的混合架构。

### 4.2 Dubbo 调用的完整机制

一次 Dubbo 调用，底层经历了以下步骤：

```
调用方（coffee-app）                         提供方（coffee-expresstrack）
─────────────────────────────────────────────────────────────────────────

1. 代码层面调用接口方法：
   expressTrackInfoService.findExpressTrackInfos(param)

2. Dubbo 代理对象拦截调用，将参数序列化（fastjson2 → 二进制字节流）

3. 从 Nacos 查询服务地址：
   问 Nacos："coffee-expresstrack 在哪？"
   Nacos 返回："192.168.1.x:28888"

4. 建立 TCP 连接（或复用连接池中的连接）
   将序列化的调用信息通过网络发送到 192.168.1.x:28888

                                        5. 接收字节流，反序列化得到方法名和参数

                                        6. 通过反射调用本地方法：
                                           ExpresstrackInfoServiceImpl.findExpressTrackInfos(param)

                                        7. 执行 MyBatis 查询，得到结果

                                        8. 将结果序列化（fastjson2 → 字节流），通过网络返回

9. 接收返回的字节流，反序列化为 PageDTO<ExpressTrackInfoResultDTO> 对象

10. 调用方拿到结果，就像本地方法返回了一样
```

整个过程对开发者透明，写代码时感觉就是在调用本地方法。

### 4.3 为什么用 fastjson2 序列化？

Dubbo 支持多种序列化方式，本项目配置 `fastjson2`：

```yaml
dubbo:
  protocol:
    serialization: fastjson2
```

| 序列化方式 | 特点 | 适用场景 |
|-----------|------|---------|
| Hessian2（Dubbo默认）| 二进制，兼容性好 | 传统 Dubbo 项目 |
| fastjson2 | JSON 文本格式，可读性好，性能优秀 | 调试方便，与 Java 17 兼容好 |
| Protobuf | 最高性能，最紧凑 | 对性能极致要求的场景 |

选 fastjson2 的原因：与 Spring Boot 3.x 和 Java 17 兼容性好，且 JSON 格式便于调试（可以直接看到传输的内容）。

---

## 5. Nacos 服务治理机制

### 5.1 服务注册流程

```
coffee-userorder 启动时：

1. Spring Boot 扫描到 @DubboService 注解的类
2. Dubbo 框架将服务信息打包：
   {
     "serviceName": "com.coffee.yun.userorder.api.service.UserOrderInfoService",
     "ip": "127.0.0.1",
     "port": 7001,
     "dubboPort": 20880,
     "version": "1.0.0"
   }
3. 向 Nacos 发送注册请求，Nacos 保存到内存注册表
4. 之后每隔 5 秒向 Nacos 发送心跳（"我还活着"）
```

### 5.2 服务发现流程

```
coffee-app 启动时：

1. 扫描到 @DubboReference 注解的字段
2. 向 Nacos 查询 UserOrderInfoService 的提供者列表
3. 在本地缓存服务地址（避免每次调用都请求 Nacos）
4. 创建代理对象，注入到 CoffeeController 中

Nacos 服务列表变化时（新实例上线/下线）：
5. Nacos 主动推送变更通知给已订阅的客户端
6. coffee-app 更新本地缓存的服务地址
```

### 5.3 健康检查与故障摘除

```
正常情况：
coffee-userorder ─── 心跳 ──→ Nacos（每5秒）
                  ←── "保活"─

服务故障（进程崩溃）：
coffee-userorder   ✗（宕机，停止发送心跳）
Nacos 在 15 秒内收不到心跳 → 将该实例标记为"不健康"
再过 30 秒仍无心跳 → 从注册表中删除

coffee-app 下次调用时：
→ 本地缓存已更新（Nacos 推送了变更）
→ 不再向故障的实例发送请求
→ 如果没有其他健康实例，抛出 "No provider available" 异常
```

**实际演示：** 你可以在本地试验：
1. 先启动全部服务，访问 `http://localhost:8005/hello/ORDER001` 确认正常
2. 强制停止 `coffee-userorder`
3. 等待约 20 秒
4. 再次访问，会看到 `No provider available` 错误
5. 重新启动 `coffee-userorder`，等待约 5 秒，再次访问恢复正常

---

## 6. Redis 缓存层详解

### 6.1 为什么要加缓存？

每次查询快递轨迹都要经过：`coffee-app → Dubbo → coffee-expresstrack → MySQL`。

高并发场景下，数据库成为瓶颈：
- 数据库连接池有上限（通常几十个连接）
- 每次 SQL 查询有磁盘 I/O 开销
- 同一订单的轨迹被反复查询（用户不断刷新物流页面）

Redis 作为内存数据库，查询速度比 MySQL 快 **100~1000 倍**，用来缓存热点数据效果显著。

### 6.2 本项目的缓存设计

**缓存 Key 命名规则：**

```
订单详情：order:detail:{order_id}        示例：order:detail:ORDER001
快递轨迹：express:track:{order_id}       示例：express:track:ORDER001
```

**缓存策略（Cache-Aside Pattern，旁路缓存）：**

```
查询请求到来
    │
    ▼
查 Redis（key = "express:track:ORDER001"）
    │
    ├── 命中（Hit）──→ 直接返回 Redis 中的数据，不访问数据库
    │
    └── 未命中（Miss）
            │
            ▼
         查 MySQL
            │
            ▼
         写入 Redis（TTL 由 Nacos Config 控制）
            │
            ▼
         返回结果
```

**为什么订单列表（findUserOrderInfos）不缓存？**

列表查询有多种组合（分页+筛选条件），缓存 key 的变体太多，命中率很低，不适合缓存。只缓存按 `order_id` 精确查询的结果。

### 6.3 缓存 TTL 与 Nacos Config 联动

快递轨迹缓存的过期时间（TTL）由 Nacos Config 控制，默认 3600 秒（1小时）。

修改 TTL 无需重启服务：

```
Nacos 控制台修改 cache.ttl.expresstrack=60
    │
    ▼
Spring Cloud 检测到配置变更事件
    │
    ▼
@RefreshScope 标注的 CacheProperties Bean 自动重建
    │
    ▼
下一次缓存写入使用新的 TTL=60s
```

### 6.4 云上替换（阿里云 Tair）

本地开发连接本地 Redis，生产环境通过环境变量切换到阿里云 Tair：

```bash
# 生产环境设置以下环境变量
REDIS_HOST=r-xxx.redis.rds.aliyuncs.com
REDIS_PORT=6379
REDIS_PASSWORD=your-password
```

代码无需任何修改，只改环境变量。

---

## 7. RocketMQ 消息队列详解

### 7.1 为什么要用消息队列？

**没有消息队列的同步流程（原来的方式）：**

```
用户下单
  ↓ 同步 Dubbo RPC
订单服务写 MySQL
  ↓ 同步 Dubbo RPC（直接调用快递服务）
快递服务创建快递单
  ↓
返回结果给用户
```

**问题：**
- 快递服务宕机 → 下单失败（强依赖）
- 用户等待时间 = 订单写入耗时 + 快递单创建耗时（串行叠加）

**有消息队列的异步流程（当前方式）：**

```
用户下单
  ↓ 同步
订单服务写 MySQL，发布 "order-created" 消息到 RocketMQ
  ↓ 立即返回（不等快递服务）
用户收到"下单成功"

                ↓ 异步（后台）
快递服务消费 RocketMQ 消息
创建快递单
```

**好处：**
- 快递服务宕机不影响下单（消息会积压，恢复后补消费）
- 用户响应更快（下单耗时不包含快递单创建耗时）
- 两个服务解耦，可以独立升级

### 7.2 本项目的消息流

```
Topic：order-created
Producer Group：userorder-producer-group     （coffee-userorder）
Consumer Group：expresstrack-consumer-group  （coffee-expresstrack）

消息体：order_id（字符串）

流程：
  POST /createOrder
    → coffee-app
    → Dubbo RPC
    → UserOrderInfoServiceImpl.createOrder()
        1. INSERT INTO `order`（写 MySQL）
        2. rocketMQTemplate.send("order-created", orderId)
                                    │
                                    ▼ RocketMQ Broker 存储消息
                                    │
                            OrderCreatedConsumer.onMessage(orderId)
                                1. INSERT INTO `express`（创建快递单）
                                2. INSERT INTO `track`（"商家已揽件"）
```

### 7.3 关键代码位置

| 角色 | 文件 | 关键注解 |
|------|------|---------|
| 生产者 | `UserOrderInfoServiceImpl.java` | `rocketMQTemplate.send(topic, msg)` |
| 消费者 | `OrderCreatedConsumer.java` | `@RocketMQMessageListener(topic, consumerGroup)` |
| 消费接口 | `OrderCreatedConsumer.java` | `implements RocketMQListener<String>` |

### 7.4 云上替换（阿里云云消息队列 RocketMQ）

```bash
# 生产环境替换 Name Server 地址
ROCKETMQ_NAMESERVER=MQ_INST_xxx.mq.cn-beijing.aliyuncs.com:8080
```

---

## 8. Nacos Config 动态配置详解

### 8.1 什么是动态配置？

传统配置流程：
```
修改 application.yml → 重新打包 → 重启服务 → 配置生效
```

Nacos Config 的流程：
```
在 Nacos 控制台修改配置 → 服务自动感知变更 → 配置立即生效（不重启）
```

对于需要频繁调整的参数（如缓存时间、限流阈值、开关配置），动态配置可以大幅降低运维成本。

### 8.2 本项目的配置中心设计

**Nacos Config 数据 ID（Data ID）：**

| 服务 | Data ID | Group |
|------|---------|-------|
| coffee-userorder | `coffee-userorder.properties` | `DEFAULT_GROUP` |
| coffee-expresstrack | `coffee-expresstrack.properties` | `DEFAULT_GROUP` |

**当前可配置的项目：**

```properties
# coffee-userorder.properties
cache.ttl.order=1800          # 订单详情缓存时长（秒），默认30分钟

# coffee-expresstrack.properties
cache.ttl.expresstrack=3600   # 快递轨迹缓存时长（秒），默认1小时
```

**可扩展的配置项（课堂演示扩展方向）：**
- `order.max-page-size=100`：最大分页大小
- `feature.cache.enabled=true`：缓存开关
- `mq.topic.order-created=order-created`：消息 topic 名称

### 8.3 @RefreshScope 工作原理

```java
@Component
@RefreshScope       // ← 关键注解
public class CacheProperties {

    @Value("${cache.ttl.order:1800}")  // ← 从 Nacos 或本地配置读取
    private long orderTtl;
}
```

Spring Cloud 在 Nacos 配置变更时：
1. 发布 `EnvironmentChangeEvent`
2. 销毁所有 `@RefreshScope` Bean
3. 下次注入时重新创建 Bean，读取最新配置值

### 8.4 在 Nacos 控制台创建配置

1. 登录 Nacos 控制台：[http://localhost:8848/nacos](http://localhost:8848/nacos)
2. 左侧菜单 → 配置管理 → 配置列表
3. 点击"+"新建配置：
   - Data ID：`coffee-userorder.properties`
   - Group：`DEFAULT_GROUP`
   - 配置格式：`Properties`
   - 配置内容：`cache.ttl.order=60`
4. 点击发布
5. 观察 coffee-userorder 控制台，无需重启，新 TTL 即刻生效

---

## 9. 请求完整调用链路

以用户查询 `ORDER001` 的快递轨迹为例：

```
① 用户在浏览器 app-admin 中输入 ORDER001，点击查询按钮

② app-admin（Vue）通过 Axios 发送：
   GET http://localhost:8005/hello/ORDER001

③ coffee-app/CoffeeController 接收请求：
   @GetMapping("/hello/{orderid}")
   public PageDTO<ExpressTrackInfoResultDTO> helloCoffee(@PathVariable String orderid)

④ CoffeeController 构造参数，发起第一次 Dubbo RPC 调用：
   UserOrderInfoParamDTO param = new UserOrderInfoParamDTO();
   param.setOrder_id("ORDER001");
   UserOrderInfoResultDTO orderResult = userOrderInfoService.findUserOrderInfo(param);
   // 这行代码走的是 Dubbo RPC，不是本地调用

⑤ Dubbo 框架：
   → 从 Nacos 得到 coffee-userorder 地址：127.0.0.1:7001（Dubbo端口）
   → 序列化参数（fastjson2）
   → 通过 TCP 发送到 coffee-userorder

⑥ coffee-userorder/UserOrderInfoServiceImpl 执行：
   → PageHelper 拦截，准备分页 SQL
   → MyBatis 执行：
     SELECT m.member_name, o.order_id, o.order_amount, o.order_status
     FROM member m, `order` o
     WHERE m.OneID = o.OneID AND o.order_id = 'ORDER001'
   → 查询 userordertest 数据库（本地或 RDS）
   → 返回 UserOrderInfoResultDTO（含 order_id = 'ORDER001'）

⑦ coffee-app 拿到 order_id，构造第二次 Dubbo RPC 调用：
   ExpressTrackInfoParamDTO etParam = new ExpressTrackInfoParamDTO();
   etParam.setOrder_id("ORDER001");
   PageDTO<ExpressTrackInfoResultDTO> result =
       expressTrackInfoService.findExpressTrackInfos(etParam);

⑧ Dubbo 框架：
   → 从 Nacos 得到 coffee-expresstrack 地址：127.0.0.1:28888
   → 序列化参数，通过 TCP 发送到 coffee-expresstrack

⑨ coffee-expresstrack/ExpresstrackInfoServiceImpl 执行：
   → MyBatis 执行：
     SELECT express.express_id, order_id, express_weight, track_id, track_show
     FROM `express`
     LEFT JOIN `track` ON `express`.`express_id` = `track`.`express_id`
     WHERE order_id = 'ORDER001'
   → 查询 expresstracktest 数据库
   → 返回 PageDTO（含5条轨迹记录，total=5）

⑩ coffee-app 将 PageDTO 直接作为 HTTP 响应体返回
   → Spring Boot 自动将 PageDTO 序列化为 JSON

⑪ app-admin 的 Axios 收到 JSON 响应
   → Vue 更新 trackList 数据
   → Table 组件自动重新渲染，用户看到5条轨迹记录
```

---

## 10. 模块间依赖关系

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
│ 依赖:            │ │ 依赖:             │ │ 依赖:                   │
│ └─coffee-common  │ │ └─coffee-common   │ │ ├─coffee-userorder-api  │
└────────┬─────────┘ └───────┬───────────┘ │ └─coffee-expresstrack-  │
         ▼                   ▼             │     api                 │
┌──────────────────┐ ┌──────────────────┐ └─────────────────────────┘
│coffee-userorder  │ │coffee-expresstrack│
│    provider      │ │     provider      │
│ 依赖:            │ │ 依赖:             │
│ └─coffee-userorder│ └─coffee-expresstrack│
│     -api         │ │       -api        │
└──────────────────┘ └──────────────────┘
```

**依赖规则（重要！）：**

1. `coffee-common` 不依赖任何项目模块（最底层基础库）
2. `api` 模块只依赖 `coffee-common`
3. `provider` 模块依赖自己的 `api` 模块（需要实现接口）
4. `coffee-app` 只依赖两个 `api` 模块（不依赖 provider，不知道实现细节）

**Maven 构建必须按顺序：**

```
coffee-common（先构建）
    ↓
coffee-userorder/api    coffee-expresstrack/api（并行构建）
    ↓                            ↓
coffee-userorder/provider  coffee-expresstrack/provider  coffee-app（任意顺序）
```

如果顺序不对（如先构建 coffee-app），会报 `Could not find artifact`，因为它依赖的 api 模块还没安装到本地仓库。

---

## 11. 配置分层设计

### 为什么配置要分两个文件？

本项目每个微服务有两层配置文件：

```
application.yml          ← 提交到 Git（不含敏感信息）
application-dev.yml      ← 不提交到 Git（含密码、密钥）
```

**`application.yml` 的职责：**
- 定义配置结构和默认值
- 通过 `${变量名}` 引用外部配置
- 定义哪个 profile 激活（`spring.profiles.active: ${ENV:dev}`）

**`application-dev.yml` 的职责：**
- 填写真实的数据库地址、账号、密码
- 填写 Nacos 地址
- 不提交到 Git（已在 `.gitignore` 中排除）

**为什么要在 `.gitignore` 中排除 `application-dev.yml`？**

```
# 如果把密码提交了 Git，即使后来删除，Git 历史中仍然有记录
# 任何克隆了仓库的人都能看到历史提交

# 正确做法：
echo "*/application-dev.yml" >> .gitignore
```

**不同环境的切换：**

```yaml
# application.yml
spring:
  profiles:
    active: ${ENV:dev}   # 先读环境变量 ENV，没有则默认 dev
```

```bash
# 生产环境启动时指定 profile
java -jar coffee-userorder-provider.jar --spring.profiles.active=prod
# 此时会加载 application-prod.yml（其中配置生产数据库地址）
```

---

## 12. 核心设计原则与取舍

### 9.1 单一职责原则

每个微服务只做一件事：`coffee-userorder` 只管订单，`coffee-expresstrack` 只管快递。这样出了问题能快速定位，代码也更容易理解。

### 9.2 接口与实现分离

`api` 模块定义"做什么"，`provider` 模块实现"怎么做"。调用方只依赖接口，实现可以随时替换。这是依赖倒置原则（DIP）的体现。

### 9.3 数据库隔离及其代价

每个微服务有自己的数据库，带来的好处是**服务自治**（修改数据库结构不影响其他服务），代价是：

| 单体应用可以做的 | 微服务架构不能或很难做 |
|---------------|---------------------|
| 一个 SQL 跨多表 JOIN | 跨库 JOIN 不可行，必须在应用层拼接 |
| 数据库事务保证一致性 | 跨服务的分布式事务非常复杂（本项目不涉及）|
| 外键约束 | 跨库无法设置外键，一致性靠应用层保证 |

本项目 `order.order_id` 与 `express.order_id` 的关联就是应用层关联：
```java
// 第一次查询拿到 order_id
UserOrderInfoResultDTO orderResult = userOrderInfoService.findUserOrderInfo(param);
// 用 order_id 再查快递（而不是一个 SQL JOIN）
etParam.setOrder_id(orderResult.getOrder_id());
PageDTO<ExpressTrackInfoResultDTO> trackPage = expressTrackInfoService.findExpressTrackInfos(etParam);
```

这是有意识的取舍，不是"技术能力不够"，是微服务架构模式的正确做法。

### 9.4 统一入口

对外只暴露 `coffee-app`（端口 8005），内部的微服务端口（7001、8001、28888）不对外开放。这样可以在网关层统一处理：
- 权限验证（本项目未实现，是扩展点）
- 请求日志（每个请求记录一条日志）
- 限流熔断（防止异常流量打垮后端服务）
- 跨域处理（`@CrossOrigin` 只需要在网关加）

---

[← 返回主文档](../README.md)
