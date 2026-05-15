# EDAS ECS 集群部署指南

> 本文档说明如何将本项目的微服务部署到阿里云 **EDAS（企业级分布式应用服务）** ECS 集群，并在 EDAS 控制台实现 Dubbo 服务治理（服务查询、调用链追踪等）。

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
| JDK | 17 |
| Maven | 3.8+ |

---

## 3. 购买 MSE Nacos 实例

> **MSE Nacos** 是阿里云全托管的 Nacos 服务，阿里云负责高可用和运维，无需自行搭建 Nacos 集群。

**为什么 EDAS 治理必须用 MSE Nacos？**

EDAS 内置注册中心（旧版 EDAS 注册配置中心）仅兼容 Dubbo 2.x 的元数据格式。本项目使用 Dubbo 3.x，只有 MSE Nacos 才能在 EDAS 治理页面正确展示服务信息。

### 操作步骤

1. 控制台搜索 **微服务引擎 MSE** → 进入
2. 左侧菜单：**注册配置中心 → 实例列表 → 创建实例**
3. 关键配置：

   | 配置项 | 填写值 |
   |--------|--------|
   | 引擎类型 | **Nacos** |
   | 版本 | **2.x** |
   | 规格 | 开发测试版（教学用最低规格） |
   | 地域 | 与 ECS 相同地域（如华东1 杭州） |
   | **VPC** | 与 ECS 相同 VPC |
   | 交换机 | 与 ECS 相同交换机 |

4. 创建完成后，进入实例详情，记录**内网访问地址**（格式：`mse-xxxxxxxx-nacos-ans.mse.aliyuncs.com:8848`）

> 内网地址仅在同 VPC 内可访问，阿里云 ECS 通过内网连接，速度快且不收流量费。

---

## 4. 创建绑定 MSE 的微服务空间

**微服务空间**决定了该空间内所有应用使用哪种注册中心。**创建后不可更改注册中心类型**，需谨慎选择。

### 操作步骤

1. **EDAS 控制台** → **资源管理 → 微服务空间 → 创建微服务空间**
2. 关键配置：

   | 配置项 | 填写值 |
   |--------|--------|
   | 空间名称 | `coffee-prod`（自定义，便于识别） |
   | **注册中心类型** | **MSE Nacos** |
   | MSE Nacos 实例 | 选择第 3 步创建的实例 |

3. 创建完成后，在微服务空间列表确认该空间的"注册中心类型/ID"列已显示 MSE 实例信息

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
    active: ${ENV:dev}   # 读取名为 ENV 的环境变量，默认值是 dev

dubbo:
  application:
    serialize-check-status: WARN
    service-discovery:
      migration: FORCE_INTERFACE   # Dubbo 3.x 使用接口级注册，兼容 EDAS 治理页面
  registry:
    address: ${DUBBO_REGISTRY:nacos://127.0.0.1:8848}   # 本地默认连接本地 Nacos
```

---

#### 重点：`-DENV=prod` 是什么，在哪里设置，为什么必须设置

**它是什么：**

`${ENV:dev}` 的意思是："读取 JVM 系统属性 `ENV` 的值，如果没有设置就用 `dev` 作为默认值"。

- `ENV=dev` → Spring Boot 加载 `application-dev.yml`（连接本地 Nacos 和本地数据库）
- `ENV=prod` → Spring Boot 加载 `application-prod.yml`（连接云上 RDS，Nacos 由 EDAS Agent 接管）

**如果部署到 EDAS 时忘了设置 `-DENV=prod`，会发生什么：**

```
服务启动 → ENV 没有值 → 默认加载 application-dev.yml
→ 尝试连接 localhost:8848（本地 Nacos）
→ 云上服务器没有本地 Nacos → 连接失败 → 服务启动报错
```

所以这个参数**至关重要**，是本地和云上环境的切换开关。

**在哪里设置：**

在 EDAS 控制台创建或部署应用时，有一个"**JVM 参数**"输入框，填入：

```
-Xms128m -Xmx256m -DENV=prod
```

`-DENV=prod` 就是通过这个输入框传给 Java 进程的。`-D` 是 Java 的固定语法，表示"设置一个 JVM 系统属性"，`ENV=prod` 就是属性名和值。

> 详细截图操作见第 8 步"在 EDAS 部署应用"中的配置表格。

---

**`FORCE_INTERFACE` 的作用：** 强制 Dubbo 3.x 使用与 Dubbo 2.x 兼容的接口级注册格式，使 EDAS 治理页面能够正确识别和展示服务信息。

### `application-prod.yml`（生产环境配置，已配置）

```yaml
# Dubbo 注册中心由 EDAS Agent 自动接管，无需在此配置地址

database:
  user: ${DB_USER:userordertest}
  password: ${DB_PASSWORD:Userordertest123}
  host: ${DB_HOST:rm-xxxxxxxxx.mysql.rds.aliyuncs.com:3306}
  dbname: ${DB_NAME:userordertest}
```

> 生产环境不需要写 Dubbo registry 地址——EDAS Agent 在字节码层拦截 Nacos 连接，自动重定向到微服务空间绑定的 MSE Nacos 实例。

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

### 7.1 前置：确保内部依赖可用

本项目的 `coffee-common`、`coffee-userorder-api`、`coffee-expresstrack-api` 是项目内部模块，不在 Maven Central。构建前需要确保 Maven 能找到它们。

**请先完成 [阿里云服务详细配置指南 § 6 制品库](./01-aliyun-guide.md#6-阿里云制品库maven-私服) 中的配置**，将内部依赖发布到阿里云制品库并在 `settings.xml` 中配置下载地址，Maven 在构建时就能从制品库自动拉取。

---

> **可选替代方案：使用项目内置 `lib-repo/`**
>
> 如果你暂时不想配置阿里云制品库，或只是在本地运行学习，项目已将编译好的内部 JAR 内置在 `lib-repo/` 目录中并随 GitHub 代码一起分发。相关 `pom.xml` 已预先配置好，Maven 会自动从该目录读取，**无需任何手动操作**，clone 后直接构建即可。
>
> **lib-repo 的两种失效场景：**
> - `settings.xml` 里有 `<mirror mirrorOf="*">`：mirror 会拦截所有仓库请求，包括 `lib-repo/`，内部 JAR 必须实际上传到制品库才能找到
> - 已配置制品库但内部 JAR 未上传（未执行 `mvn deploy`）+ 配了 mirror：构建报 `Could not find artifact`，需先完成 [§ 6.4 上传 JAR](./01-aliyun-guide.md#64-上传-jar-到私服)
>
> 如果没有配 mirror，只是在 `<profile>` 里加了仓库地址，则无论制品库里有没有内部 JAR，`lib-repo/` 都能正常兜底。
>
> 注意：EDAS 云端构建（在 EDAS 控制台上传 JAR 包）依赖制品库，`lib-repo/` 仅适用于本地开发场景。

---

### 7.2 打包微服务 JAR

```bash
# 打包订单服务（产物：coffee-userorder/provider/target/coffee-userorder-provider-1.0-SNAPSHOT.jar）
cd coffee-userorder/provider
mvn clean package -DskipTests

# 打包快递服务（产物：coffee-expresstrack/provider/target/coffee-expresstrack-provider-1.0-SNAPSHOT.jar）
cd ../../coffee-expresstrack/provider
mvn clean package -DskipTests

# 打包 API 网关（产物：coffee-app/target/coffee-app-0.0.1-SNAPSHOT.jar）
cd ../../coffee-app
mvn clean package -DskipTests
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
   | 部署包 | 上传 `coffee-userorder/provider/target/provider-1.0-SNAPSHOT.jar` |
   | **JVM 参数** | `-Xms128m -Xmx256m -DENV=prod` |

   > `-DENV=prod` 激活 `application-prod.yml`，使服务以生产模式运行。

3. 点击"部署"，等待应用状态变为**运行中**

### 8.2 部署快递服务

同上，应用名称改为 `expresstrack`，JAR 包上传 `coffee-expresstrack/provider/target/provider-1.0-SNAPSHOT.jar`。

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
  └─ 服务注册到本地 Nacos

EDAS 部署流程：
应用启动（-DENV=prod）
  └─ EDAS 自动注入 ARMS/Pandora Agent（-javaagent）
  └─ Agent 在字节码层拦截所有 Nacos 连接请求
  └─ 无论代码里写了什么 Nacos 地址，Agent 都重定向到
       └─ 微服务空间（coffee-prod）绑定的 MSE Nacos 内网地址
  └─ Dubbo 以 FORCE_INTERFACE 模式完成接口级注册
  └─ EDAS 治理页面从 MSE Nacos 读取服务元数据 → 展示服务列表
```

**关键设计决策：**

- **为什么 `application-prod.yml` 不写 registry 地址？** EDAS Agent 在字节码层接管注册中心连接，写了也会被覆盖，不如保持配置整洁，明确注释说明由 Agent 接管。
- **为什么需要 `FORCE_INTERFACE`？** Dubbo 3.x 默认使用应用级注册（Application-Level Service Discovery），注册的是应用元数据，EDAS 治理 API 无法解析。`FORCE_INTERFACE` 强制使用接口级注册，EDAS 能直接读取到每个 Dubbo 接口的提供者信息。

---

## 11. 常见问题排查

### Q：EDAS 服务查询页面"没有数据"

按以下顺序排查：

1. **确认微服务空间** — 服务查询页面顶部"所属微服务空间"是否选择的是绑定 MSE Nacos 的空间（如 `coffee-prod`），而不是默认的 `cn-hangzhou`（EDAS注册配置中心）

2. **确认集群所属空间** — EDAS ECS 集群详情页的"微服务空间"字段是否为 `coffee-prod`（而非 `cn-hangzhou`）。如果是 `cn-hangzhou`，该集群使用 EDAS 内置注册中心，与 Dubbo 3.x 不兼容，需重建集群。

3. **确认 JVM 参数** — 应用的 JVM 参数是否包含 `-DENV=prod`，缺少此参数会加载 `application-dev.yml`，导致服务注册到本地 Nacos 而不是 MSE Nacos

4. **确认安全组** — ECS 安全组出方向是否放通了 8442、8443、8883 端口

5. **等待时间** — 应用首次部署后服务元数据上报约有 30 秒延迟，刷新页面再试

---

### Q：应用启动报错 `Unable to connect to Nacos`

检查 MSE Nacos 实例与 ECS 是否在同一 VPC。不同 VPC 之间无法直接内网通信。

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
