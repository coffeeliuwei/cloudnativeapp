# 云原生设计与开发课程实例

**课程**：云原生应用框架与开发 ｜ **讲师**：coffeeliu ｜ **邮箱**：coffee.liu@gmail.com

> 本项目是课程的完整配套代码，演示基于 **Spring Boot + Apache Dubbo + Nacos + MySQL + Vue.js** 的微服务电商系统。
> 即使你从未接触过微服务，跟着文档也能一步步把项目跑起来。

---

## 文档导航

| 文档 | 内容 |
|------|------|
| **本文件** | 项目概览、技术栈、目录结构、快速开始 |
| [架构详解](docs/02-architecture.md) | 从单体到微服务的演进、完整调用链路、分层模型、设计原则 |
| [各模块代码详解](docs/03-modules.md) | 每个文件的作用、关键代码注释讲解 |
| [数据库设计](docs/04-database.md) | 表结构、字段说明、ER图、完整初始化 SQL |
| [阿里云配置指南](docs/01-aliyun-guide.md) | RDS、Nacos、日志服务、制品库的注册与配置教程 |
| [快速启动指南](docs/05-quick-start.md) | 零基础手把手启动教程，含常见错误速查 |

---

## 项目简介

本项目模拟一个"电商物流查询系统"：用户可以输入订单号，查询对应的快递轨迹。

**项目涵盖的云原生核心知识点：**

| 知识点 | 对应技术 | 课程章节 |
|--------|---------|---------|
| 微服务拆分思想 | 订单服务 / 快递服务 独立部署 | 第2章 |
| 服务注册与发现 | Alibaba Nacos | 第3章 |
| 服务间远程调用（RPC）| Apache Dubbo | 第4章 |
| 持久层框架 | MyBatis + MySQL | 第5章 |
| 云数据库 | 阿里云 RDS MySQL | 第6章 |
| 日志收集 | 阿里云日志服务 SLS | 第7章 |
| 制品管理 | 阿里云制品库（Maven 私服）| 第8章 |
| 前端框架 | Vue.js 3.4 + ViewUI Plus 1.x | 第9章 |

---

## 技术栈

```
后端                               前端
├── Java 17                        ├── Vue.js 3.4
├── Spring Boot 3.3.4              ├── ViewUI Plus 1.x（UI 组件库）
├── Apache Dubbo 3.3.4（RPC）       ├── Vuex 4.x（状态管理）
├── Nacos 2.x（注册中心）            ├── Vue Router 4.x（路由）
├── MyBatis 3.x（ORM）              └── Axios 1.x（HTTP 请求）
├── MySQL 8.0
└── Maven（构建）                   阿里云
                                   ├── RDS MySQL（云数据库）
                                   ├── 日志服务 SLS
                                   └── 制品库（Maven 私服）
```

---

## 架构总览

```
浏览器
  │ HTTP
  ▼
app-admin（Vue.js 前端 :8080）
  │ HTTP REST
  ▼
coffee-app（API网关 :8005）        ← 统一对外入口
  │ Dubbo RPC          │ Dubbo RPC
  ▼                    ▼
coffee-userorder    coffee-expresstrack
  (:7001)              (:8001, Dubbo :28888)
  │ JDBC               │ JDBC
  ▼                    ▼
RDS MySQL           RDS MySQL
(userordertest)     (expresstracktest)
  │                    │
  └────────┬───────────┘
           ▼
      Nacos :8848
   （服务注册与发现）
```

> 详细架构说明请阅读 [架构详解](docs/02-architecture.md)。

---

## 目录结构

```
cloudnativeapp/
│
├── README.md                   本文件
├── docs/                       详细文档目录
│   ├── 01-aliyun-guide.md      阿里云配置指南
│   ├── 02-architecture.md      架构设计详解
│   ├── 03-modules.md           各模块代码详解
│   ├── 04-database.md          数据库设计与初始化
│   └── 05-quick-start.md       快速启动手把手教程
│
├── coffee-common/              公共基础库（DTO、工具类）
│   └── src/main/java/
│       └── com/coffee/yun/dto/
│           ├── BasePageDTO.java    分页查询基础参数
│           └── PageDTO.java        分页查询返回结果
│
├── coffee-userorder/           用户订单微服务
│   ├── api/                    接口定义（供调用方引用）
│   │   └── src/main/java/com/coffee/yun/userorder/api/
│   │       ├── dto/            数据传输对象
│   │       └── service/        服务接口定义
│   └── provider/               接口实现（实际运行的服务）
│       └── src/main/
│           ├── java/           业务实现代码
│           └── resources/      配置文件、SQL映射
│
├── coffee-expresstrack/        快递轨迹微服务
│   ├── api/                    接口定义
│   └── provider/               接口实现
│
├── coffee-app/                 主应用（API网关，端口8005）
│   └── src/main/java/com/coffee/yun/coffeeapp/
│       ├── CoffeeAppApplication.java   启动类
│       ├── CoffeeController.java       REST接口控制器
│       └── base/
│           ├── Result.java             统一响应格式
│           └── ResultUtil.java         响应构建工具
│
└── app-admin/                  Vue.js 管理前端（端口8080）
    └── src/
        ├── api/                接口调用封装
        ├── components/         公共Vue组件
        ├── router/             页面路由配置
        ├── store/              Vuex状态管理
        └── view/               页面组件
```

---

## 快速开始

### 1. 环境要求

| 工具 | 版本 | 必须 |
|------|------|------|
| JDK | 17+ | ✅ |
| Maven | 3.8+ | ✅ |
| Node.js | 16+ | ✅ |
| Nacos | 2.x | ✅ |
| MySQL | 8.0 或阿里云RDS | ✅ |

### 2. 启动顺序（必须按序）

```bash
# 第1步：启动 Nacos（服务注册中心）
cd nacos/bin && startup.cmd -m standalone

# 第2步：安装本地依赖（首次使用）
cd coffee-common          && mvn clean install -DskipTests
cd coffee-userorder/api   && mvn clean install -DskipTests
cd coffee-expresstrack/api && mvn clean install -DskipTests

# 第3步：启动订单微服务
cd coffee-userorder/provider && mvn spring-boot:run

# 第4步：启动快递微服务
cd coffee-expresstrack/provider && mvn spring-boot:run

# 第5步：启动主应用网关
cd coffee-app && mvn spring-boot:run

# 第6步：启动前端
cd app-admin && npm install && npm run dev
```

### 3. 验证

浏览器访问：`http://localhost:8005/hello/ORDER001`

返回快递轨迹 JSON 数据即表示成功。

> 详细步骤和截图说明请阅读 [快速启动指南](docs/05-quick-start.md)。

---

## 服务端口一览

| 服务 | 端口 | 说明 |
|------|------|------|
| Nacos | 8848 | 服务注册中心 |
| coffee-userorder | 7001 | 订单微服务 |
| coffee-expresstrack | 8001 | 快递微服务 HTTP |
| coffee-expresstrack | 28888 | 快递微服务 Dubbo RPC |
| coffee-app | 8005 | 主应用入口（对外API）|
| app-admin | 8080 | 前端管理界面 |

---

## 接口说明

| 接口 | 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|------|
| 查询快递轨迹 | GET | `/hello/{orderid}` | 路径参数：订单ID | 返回该订单的快递轨迹列表 |
| 查询订单 | POST | `/findOrderList` | Body: `{"order_id":"xxx"}` | 返回订单及快递信息 |

---

## 阿里云相关配置

本项目涉及以下阿里云服务：

| 服务 | 用途 | 配置位置 |
|------|------|---------|
| RDS MySQL | 云数据库 | `application-dev.yml` |
| 日志服务 SLS | 集中日志收集 | `logback-spring.xml` |
| 制品库 | Maven 私有仓库 | `pom.xml` 的 `distributionManagement` |

> 详细配置步骤请阅读 [阿里云配置指南](docs/01-aliyun-guide.md)。

---

## 常见问题

**Q：启动时报 `Connection refused`？**
> Nacos 还没启动。先启动 Nacos，再启动各微服务。

**Q：报 `No provider available for service`？**
> 订单服务或快递服务还没启动，或没有注册到 Nacos。检查服务列表：[http://localhost:8848/nacos](http://localhost:8848/nacos)

**Q：`BUILD FAILURE` / 找不到依赖包？**
> 未执行本地安装步骤。按顺序运行 `mvn clean install` 命令（见快速开始第2步）。

**Q：数据库连接失败？**
> 检查 `application-dev.yml` 中的数据库地址和账号密码。阿里云 RDS 还需在白名单中添加本机IP。

> 更多问题解答请查阅 [快速启动指南](docs/05-quick-start.md#常见错误速查)。

---

*如有问题请在课程群中提问，或发邮件至 coffee.liu@gmail.com*
