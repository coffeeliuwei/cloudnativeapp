# EDAS ECS 集群部署指南

> 本文档说明如何将本项目的微服务部署到阿里云 **EDAS（企业级分布式应用服务）** ECS 集群，并在 EDAS 控制台实现 Dubbo 服务治理（服务查询、调用链追踪等）。

> **dubbo2 分支与 main 分支的部署差异**
>
> | 步骤 | `main` 分支（Dubbo 3.x） | `dubbo2` 分支（本分支，Dubbo 2.7.x） |
> |------|------------------------|--------------------------------------|
> | 第3步 注册中心 | 需购买 **MSE Nacos** 实例 | **无需购买**，使用 EDAS 内置注册中心 |
> | 第4步 微服务空间 | 注册中心类型选 MSE Nacos | 注册中心类型选 **EDAS 注册配置中心** |
> | 第6步 代码配置 | 需要 `FORCE_INTERFACE` 参数 | **不需要**，Dubbo 2.x 默认接口级注册 |
> | 第8步 部署 Java 环境 | 选 **Java 17** | 选 **Open JDK 11** |
>
> 以下正文以 `main` 分支（Dubbo 3.x）为基准说明，dubbo2 分支的差异已在上表列出，各步骤内也有单独标注。

---

## 目录

1. [EDAS 是什么，为什么用它](#1-edas-是什么为什么用它)
2. [前置条件](#2-前置条件)
3. [购买 MSE Nacos 实例](#3-购买-mse-nacos-实例)
4. [创建绑定 MSE 的微服务空间](#4-创建绑定-mse-的微服务空间)
5. [创建 EDAS ECS 集群并导入服务器](#5-创建-edas-ecs-集群并导入服务器)
6. [代码配置说明](#6-代码配置说明)
7. [打包构建](#7-打包构建)
8. [在 EDAS 部署应用](#8-在-edas-部署应用)
9. [验证 Dubbo 服务治理](#9-验证-dubbo-服务治理)
10. [工作原理详解](#10-工作原理详解)
11. [常见问题排查](#11-常见问题排查)

---

## 1. EDAS 是什么，为什么用它

**EDAS**（Enterprise Distributed Application Service）是阿里云提供的微服务应用托管平台，在普通 ECS 部署的基础上提供：

| 功能 | 说明 |
|------|------|
| 应用生命周期管理 | 在控制台一键部署、启动、停止、回滚 |
| Dubbo 服务治理 | 可视化查看已注册的 Dubbo 服务及提供者列表 |
| 调用链追踪 | 追踪跨服务的请求链路，快速定位性能瓶颈 |
| 服务鉴权 | 控制哪些服务可以互相调用 |
| 金丝雀发布 | 将新版本先推送给少量实例，验证无误后全量发布 |

**本项目使用 EDAS 的主要目的：** 演示 Dubbo 服务治理页面，展示微服务注册与发现的可视化效果。

---

## 2. 前置条件

### 2.1 ECS 安全组配置

EDAS Agent 需要通过以下端口向阿里云汇报服务元数据，**必须在安全组中放通出方向**：

| 端口 | 协议 | 方向 | 说明 |
|------|------|------|------|
| 8442 | TCP | 出方向 | EDAS Agent 注册通信 |
| 8443 | TCP | 出方向 | EDAS Agent 注册通信 |
| 8883 | TCP | 出方向 | EDAS Agent 服务元数据上报 |

操作路径：**ECS 控制台 → 安全组 → 配置规则 → 出方向 → 手动添加**

> 目标 IP 填 `0.0.0.0/0`，优先级填 `1`。

### 2.2 网络要求

- MSE Nacos 实例与 ECS 必须在同一 **VPC**（专有网络）和**地域**
- ECS 能访问阿里云内网网段 `100.100.0.0/16`（默认放通，无需额外配置）

### 2.3 软件要求

| 工具 | 版本 |
|------|------|
| JDK | 11 |
| Maven | 3.8+ |

---

## 3. 关于注册中心（dubbo2 分支无需购买 MSE）

> **本分支（dubbo2）使用 Dubbo 2.7.x，直接兼容 EDAS 内置注册中心，无需额外购买 MSE Nacos 实例。**

**为什么 dubbo2 分支可以用 EDAS 内置注册中心？**

EDAS 内置注册中心（EDAS 注册配置中心）的元数据格式基于 Dubbo 2.x 接口级注册协议设计。本分支使用 Dubbo 2.7.23，与内置注册中心天然兼容，EDAS 治理页面能直接展示服务信息。

> **对比 main 分支（Dubbo 3.x）**：Dubbo 3.x 默认使用应用级注册，EDAS 内置注册中心无法解析，需要配合 MSE Nacos 并加 `FORCE_INTERFACE` 参数。dubbo2 分支无此限制，部署更简单。

---

## 4. 创建微服务空间（使用默认内置注册中心）

**微服务空间**决定了该空间内所有应用使用哪种注册中心。

### 操作步骤

1. **EDAS 控制台** → **资源管理 → 微服务空间 → 创建微服务空间**
2. 关键配置：

   | 配置项 | 填写值 |
   |--------|--------|
   | 空间名称 | `coffee-prod`（自定义，便于识别） |
   | **注册中心类型** | **EDAS 注册配置中心**（默认，无需改动） |

3. 创建完成后，在微服务空间列表确认该空间已创建

---

## 5. 创建 EDAS ECS 集群并导入服务器

### 5.1 创建集群

1. **EDAS 控制台** → **资源管理 → EDAS ECS集群 → 创建集群**
2. 关键配置：

   | 配置项 | 填写值 |
   |--------|--------|
   | 集群名称 | `coffeecluster`（自定义） |
   | **微服务空间** | 选择第 4 步创建的 `coffee-prod` |
   | VPC | 与 ECS 相同的 VPC |

   > **微服务空间在此处选定**，集群创建后不可变更。

### 5.2 导入 ECS

1. 进入集群详情 → **添加ECS**
2. 从列表中勾选目标 ECS 实例（同 VPC 内可见）
3. 导入完成，等待 EDAS Agent 自动安装（约 3-5 分钟）
4. ECS 状态变为"运行中"表示 Agent 安装成功

---

## 6. 代码配置说明

本项目已预配置好 EDAS 部署所需的所有参数，**无需额外修改代码**，以下是配置说明，便于理解各文件的作用。

### `application.yml`（主配置，已配置）

```yaml
spring:
  profiles:
    active: ${ENV:dev}   # 通过 -DENV=prod 激活生产环境配置

dubbo:
  application:
    serialize-check-status: WARN
    # dubbo2 分支无需 service-discovery.migration 参数，Dubbo 2.x 默认接口级注册
  registry:
    address: ${DUBBO_REGISTRY:nacos://127.0.0.1:8848}   # 本地默认连接本地 Nacos
```

### `application-prod.yml`（生产环境配置，已配置）

```yaml
# Dubbo 注册中心由 EDAS Agent 自动接管，无需在此配置地址

database:
  user: ${DB_USER:userordertest}
  password: ${DB_PASSWORD:Userordertest123}
  host: ${DB_HOST:rm-xxxxxxxxx.mysql.rds.aliyuncs.com:3306}
  dbname: ${DB_NAME:userordertest}
```

> 生产环境不需要写 Dubbo registry 地址——EDAS Agent 在字节码层拦截 Nacos 连接，自动重定向到微服务空间绑定的内置注册中心。Dubbo 2.x 接口级注册格式与 EDAS 内置注册中心天然兼容。

### `application-dev.yml`（开发环境配置，本地使用）

```yaml
dubbo:
  registry:
    address: nacos://127.0.0.1:8848   # 本地 Nacos

database:
  user: ${DB_USER:userordertest}
  # ...（其余数据库配置）
```

---

## 7. 打包构建

### 7.1 安装本地依赖（首次或依赖变更时）

```bash
cd coffee-common && mvn clean install -DskipTests
```

### 7.2 打包微服务

```bash
# 打包订单服务（在模块根目录执行，会同时编译 api 和 provider）
cd coffee-userorder
mvn clean package -DskipTests
# 产物：provider/target/coffee-userorder-provider-1.0-SNAPSHOT.jar

# 打包快递服务
cd ../coffee-expresstrack
mvn clean package -DskipTests
# 产物：provider/target/coffee-expresstrack-provider-1.0-SNAPSHOT.jar

# 打包主应用网关
cd ../coffee-app
mvn clean package -DskipTests
# 产物：target/coffee-app-0.0.1-SNAPSHOT.jar
```

---

## 8. 在 EDAS 部署应用

### 8.1 部署订单服务

1. **EDAS 控制台** → **应用管理 → 创建应用**
2. 关键配置：

   | 配置项 | 填写值 |
   |--------|--------|
   | 应用名称 | `userorder` |
   | 部署环境 | ECS集群 → 选择 `coffeecluster` |
   | 部署包类型 | JAR 包 |
   | 部署包 | 上传 `coffee-userorder/provider/target/coffee-userorder-provider-1.0-SNAPSHOT.jar` |
   | **Java 环境** | **Open JDK 11** |
   | **JVM 参数** | `-Xms128m -Xmx256m -DENV=prod` |

   > `-DENV=prod` 激活 `application-prod.yml`，使服务以生产模式运行。

3. 点击"部署"，等待应用状态变为**运行中**

### 8.2 部署快递服务

同上，应用名称改为 `expresstrack`，JAR 包上传 `coffee-expresstrack/provider/target/coffee-expresstrack-provider-1.0-SNAPSHOT.jar`，Java 环境同样选 **Open JDK 11**。

### 8.3 部署主应用网关

同上，应用名称改为 `coffee-app`，JAR 包上传 `coffee-app/target/coffee-app-0.0.1-SNAPSHOT.jar`，Java 环境选 **Open JDK 11**。

---

## 9. 验证 Dubbo 服务治理

### 9.1 查看服务列表

1. **EDAS 控制台** → **微服务治理 → Dubbo → 服务查询**
2. 顶部"所属微服务空间"选择 `coffee-prod`
3. 服务列表中应出现：
   - `com.coffee.yun.userorder.service.OrderService`（或其他接口名）
   - `com.coffee.yun.expresstrack.service.ExpressTrackService`

4. 点击服务名 → "提供者"标签，可以看到提供该服务的 ECS 实例 IP

### 9.2 查看应用状态

**EDAS 控制台** → **应用管理** → 点击应用名 → **基本信息**：
- 实例总数与运行中实例数相同，且健康检查显示"运行中"表示部署成功
- 点击"查看日志"可以看到 Spring Boot 启动日志

---

## 10. 工作原理详解

```
本地开发流程：
应用启动（ENV=dev）
  └─ 加载 application-dev.yml
  └─ Dubbo 连接本地 Nacos（127.0.0.1:8848）
  └─ 服务以接口级格式注册到本地 Nacos

EDAS 部署流程：
应用启动（-DENV=prod，Java 11）
  └─ EDAS 自动注入 ARMS/Pandora Agent（-javaagent）
  └─ Agent 在字节码层拦截所有 Nacos 连接请求
  └─ 无论代码里写了什么 Nacos 地址，Agent 都重定向到
       └─ 微服务空间（coffee-prod）绑定的 EDAS 内置注册中心
  └─ Dubbo 2.7.x 以默认接口级格式完成注册
  └─ EDAS 治理页面直接读取接口级服务元数据 → 展示服务列表
```

**关键设计决策：**

- **为什么 `application-prod.yml` 不写 registry 地址？** EDAS Agent 在字节码层接管注册中心连接，写了也会被覆盖，不如保持配置整洁，明确注释说明由 Agent 接管。
- **为什么 dubbo2 分支不需要 `FORCE_INTERFACE`？** 该参数是 Dubbo 3.x 专用，用于强制从应用级注册降级到接口级注册。Dubbo 2.7.x 本就使用接口级注册，EDAS 内置注册中心天然兼容，无需额外配置。
- **为什么必须选 Java 11？** Dubbo 2.7.x 使用 Javassist 在运行时动态生成自适应扩展类，Java 17 的强模块封装会阻止 Javassist 访问 `ClassLoader.defineClass()`，导致启动失败。

---

## 11. 常见问题排查

### Q：EDAS 服务查询页面"没有数据"

按以下顺序排查：

1. **确认微服务空间** — 服务查询页面顶部"所属微服务空间"是否选择了 `coffee-prod`

2. **确认集群所属空间** — EDAS ECS 集群详情页的"微服务空间"字段是否为 `coffee-prod`

3. **确认 JVM 参数** — 应用的 JVM 参数是否包含 `-DENV=prod`，缺少此参数会加载 `application-dev.yml`，导致服务注册到本地 Nacos 而不是 EDAS 内置注册中心

4. **确认安全组** — ECS 安全组出方向是否放通了 8442、8443、8883 端口

5. **等待时间** — 应用首次部署后服务元数据上报约有 30 秒延迟，刷新页面再试

---

### Q：应用启动报 `InaccessibleObjectException` / Javassist 相关错误

**含义**：EDAS 使用了 Java 17 运行时，与 Dubbo 2.7.x 不兼容。

**解决方法**：在 EDAS 部署页面的"Java 环境"下拉框中选择 **Open JDK 11**，重新部署。

---

### Q：应用启动报错 `Unable to connect to Nacos`

检查 ECS 安全组出方向是否放通了 8442、8443、8883 端口，以及 ECS 与 EDAS 集群是否在同一地域和 VPC。

---

### Q：EDAS 控制台导入 ECS 时找不到目标实例

ECS 必须与 EDAS ECS 集群在同一地域（如都在华东1杭州）和同一 VPC 才会出现在列表中。

---

### Q：集群无法切换微服务空间

EDAS ECS 集群创建后微服务空间不可修改。如果建集群时误选了错误空间（如默认的 `cn-hangzhou`），需要：
1. 删除集群（先删应用，再删集群）
2. 重新创建集群，在"微服务空间"处选择绑定 MSE Nacos 的空间

---

[← 返回主文档](../README.md) | [阿里云配置指南](01-aliyun-guide.md)
