# 第 11 章：部署到阿里云 EDAS（dubbo2 分支）

> **本章目标**：把第 05 章在本地跑通的微服务，原封不动搬到阿里云 EDAS 上运行，并在 EDAS 控制台观察 Dubbo 服务治理效果。
>
> **本分支特点（重要）**：这是 **dubbo2 分支**，使用 **Dubbo 2.7.x**。Dubbo 2.7 与 **EDAS 内置注册中心**天然兼容，所以**本分支不需要购买 MSE Nacos**，直接用 EDAS 自带的注册配置中心即可——这是本分支相比 main 分支（Dubbo 3.x + 必须买 MSE Nacos）最大的区别，也是教学上"两种注册中心方案对比"的演示点。
>
> **阅读建议**：主线只有一条——准备资源 → 搭 EDAS → 打包 → 部署 → 验证。第八节（云效流水线）是进阶，第一次做可跳过。

---

## 本章总览

### 一张图看懂要做什么

```
本地（第 05 章）                      云上（本章）
─────────────                       ─────────────
本地 MySQL          ──搬到──►        RDS（托管数据库）
本地 Nacos          ──搬到──►        EDAS 内置注册中心（无需另买 MSE Nacos）
本地 java -jar 启动 ──搬到──►        EDAS（应用托管平台，跑在 ECS 上）
本地 npm run dev    ──搬到──►        ECS + Nginx（托管前端静态页面）
```

**核心思想：三个服务的代码一行都不改**，只通过启动参数告诉它们去连云上的地址。

### dubbo2 分支 vs main 分支

| | main 分支 | **dubbo2 分支（本章）** |
|---|---|---|
| Dubbo 版本 | 3.x | **2.7.x** |
| 接口级注册 | 需显式配 `FORCE_INTERFACE` | **2.7 默认就是接口级，无需配置** |
| 注册中心 | 必须买 **MSE Nacos** | **EDAS 内置注册中心，无需购买** |
| 微服务空间 | 绑定 MSE Nacos | 注册中心类型选 **EDAS 注册配置中心** |

### 开始前的前置条件

- [ ] **第 05 章已完成**——本地能启动三个后端服务并访问前端页面
- [ ] 阿里云账号已**实名认证**
- [ ] 已购买 **ECS 实例**（推荐 2 核 4 GB，Linux）
- [ ] 已购买并初始化 **RDS MySQL 8.0**——建库建账号、表结构、白名单见 [阿里云配置指南 §3 RDS](./01-aliyun-guide.md#3-阿里云-rds-mysql云数据库)
- [ ] 已完成**制品库**配置并上传内部依赖——见 [阿里云配置指南 §6 制品库](./01-aliyun-guide.md#6-阿里云制品库maven-私服)（EDAS 云端构建依赖它）
- [ ] 本机已装 **Maven 3.8+**、**JDK 17**

> **ECS 不需要手动装 JDK**——EDAS 部署时自动管理 Java 运行环境。

---

## 第一节：三个必懂的概念

### 1.1 VPC：所有资源必须在同一个"内网"

**VPC（专有网络）** 是你在阿里云上的私有局域网。ECS、RDS 只要在**同一个 VPC**里，就能走内网互通——快且不计流量费。

> **铁律：ECS 和 RDS 必须同一地域、同一 VPC。** 这是新手最常踩的坑，且很多资源建好后 VPC 不可改、只能删了重建。

### 1.2 ENV 开关：一份代码，两套配置

每个微服务的 `application.yml` 开头都有：

```yaml
spring:
  profiles:
    active: ${ENV:dev}
```

`${ENV:dev}` 的意思：读取启动参数 `ENV`，**没设就用 `dev`**。

| 场景 | 启动参数 | 加载的配置 | 连什么 |
|------|---------|-----------|--------|
| 本地开发 | 不设（默认） | `application-dev.yml` | 本地 MySQL、本地 Nacos |
| 云上部署 | `-DENV=prod` | `application-prod.yml` | 云上 RDS，注册中心由 EDAS 接管 |

所以上云在代码层面就一件事：**部署时加 `-DENV=prod`，再把 RDS 地址等通过启动参数传进去。**

> **忘了设 `-DENV=prod` 会怎样？** 服务会默认加载 `application-dev.yml` → 尝试连 `localhost:8848` 的本地 Nacos → 云上没有本地 Nacos → 启动报错。所以这个参数至关重要。

### 1.3 EDAS 内置注册中心 + Dubbo 2.7：为什么不用买 MSE Nacos

main 分支用 Dubbo 3.x。Dubbo 3.x 默认是**应用级注册**，EDAS 内置注册中心解析不了，所以 main 必须买 MSE Nacos 并显式配 `FORCE_INTERFACE` 兼容。

**本分支用 Dubbo 2.7.x，默认就是接口级注册**，与 EDAS 内置注册中心的元数据格式天然匹配。所以：

- **不需要购买 MSE Nacos**，直接用 EDAS 自带的"注册配置中心"。
- 代码里**不需要** `FORCE_INTERFACE`（你看 `application.yml` 里确实没有这一行）。
- 部署时**不需要手动配注册中心地址**：EDAS 在 JVM 里注入 Agent，自动把 Dubbo 的注册请求接管、指向所在微服务空间的注册中心。所以 `application-prod.yml` 里只有数据库配置，没有 registry 地址。

```yaml
# application-prod.yml（本分支）
# Dubbo 注册中心由 EDAS Pandora Agent 自动接管，无需在此配置
database:
  user: ${DB_USER:userordertest}
  password: ${DB_PASSWORD:Userordertest123}
  host: ${DB_HOST:rm-xxxxxxxxx.mysql.rds.aliyuncs.com:3306}
  dbname: ${DB_NAME:userordertest}
```

> 数据库地址用 `${DB_HOST:...}` 占位，**部署时通过 JVM 参数传入真实的 RDS 内网地址**（见第五节）。

---

## 第二节：准备云资源

### 2.1 RDS 数据库

按 [阿里云配置指南 §3](./01-aliyun-guide.md#3-阿里云-rds-mysql云数据库) 完成：创建 `userordertest`、`expresstracktest` 两个库和账号、初始化表结构、把 **ECS 内网 IP 加入 RDS 白名单**。

记下 RDS 的**内网地址**（第五节部署时填入 JVM 参数）。

### 2.2 制品库（让 EDAS 能拿到内部依赖）

`coffee-app` 依赖 `coffee-userorder-api` 等内部模块，EDAS 云端构建时本机仓库帮不上忙，必须从制品库拉取。按 [阿里云配置指南 §6](./01-aliyun-guide.md#6-阿里云制品库maven-私服) 完成：创建自己的云效制品仓库、在 `settings.xml` 配好 `aliyun-snapshot` 凭据和 `aliyun.repo.url`、执行 `mvn deploy` 上传内部依赖。

> 项目的 pom 已参数化（仓库地址不写死在代码里），你只改自己的 `settings.xml` 即可，详见 §6。

### 2.3 配置 ECS 安全组（出方向）

EDAS Agent 需要从 ECS 主动上报数据，必须放通**出方向**：

**ECS 控制台 → 安全组 → 配置规则 → 出方向 → 手动添加**

| 端口 | 协议 | 目标 IP | 说明 |
|------|------|---------|------|
| 8442 | TCP | 0.0.0.0/0 | EDAS Agent 通信 |
| 8443 | TCP | 0.0.0.0/0 | EDAS Agent 通信 |
| 8883 | TCP | 0.0.0.0/0 | 服务元数据上报 |

---

## 第三节：搭建 EDAS 环境（内置注册中心）

EDAS 三层结构，**按顺序创建**：微服务空间 → ECS 集群 → 应用（应用在第五节建）。

### 3.1 创建微服务空间（注册中心选 EDAS 内置）

微服务空间决定里面的应用用哪个注册中心，**创建后不可改**。

**EDAS 控制台 → 资源管理 → 微服务空间 → 创建微服务空间**

| 配置项 | 填写值 |
|--------|--------|
| 空间名称 | `coffee-prod` |
| **注册中心类型** | **EDAS 注册配置中心**（内置，不要选 MSE Nacos） |

> **这正是 dubbo2 分支的关键**：选 EDAS 内置注册中心，不用买、不用配 MSE Nacos。因为 Dubbo 2.7 与它兼容。
> （也可直接使用 EDAS 默认空间，默认空间用的就是内置注册中心。新建一个 `coffee-prod` 只是便于识别。）

### 3.2 创建 ECS 集群并导入服务器

**EDAS 控制台 → 资源管理 → EDAS ECS 集群 → 创建集群**

| 配置项 | 填写值 |
|--------|--------|
| 集群名称 | `coffeecluster` |
| 微服务空间 | 选 `coffee-prod`（上一步建的） |
| VPC | 与 ECS 相同 |

> 集群的微服务空间建好后不可改。

然后：进入集群详情 → **添加 ECS** → 勾选你的 ECS → 确认 → 等 3-5 分钟，EDAS 自动装 Agent。

**确认**：ECS 状态变"运行中"，说明 Agent 安装成功。

---

## 第四节：打包后端应用

> **前置**：已完成 §6 制品库配置并上传内部依赖（[阿里云配置指南 §6.5](./01-aliyun-guide.md#65-上传-jar-到私服)），否则打包 `coffee-app` 时会报 `Could not find artifact`。

在项目根目录打开命令行，**按顺序打包三个服务**：

```bash
# 打包订单服务
cd coffee-userorder/provider
mvn clean package -DskipTests

# 打包快递服务
cd ../../coffee-expresstrack/provider
mvn clean package -DskipTests

# 打包 API 网关
cd ../../coffee-app
mvn clean package -DskipTests
```

打包产物：

| 服务 | JAR 文件路径 |
|------|------------|
| 订单服务 | `coffee-userorder/provider/target/coffee-userorder-provider-1.0-SNAPSHOT.jar` |
| 快递服务 | `coffee-expresstrack/provider/target/coffee-expresstrack-provider-1.0-SNAPSHOT.jar` |
| API 网关 | `coffee-app/target/coffee-app-0.0.1-SNAPSHOT.jar` |

**确认**：每个命令最后显示 `BUILD SUCCESS`。

---

## 第五节：在 EDAS 部署三个服务

**EDAS 控制台 → 应用管理 → 创建应用**，依次部署。

### 5.1 部署订单服务

| 配置项 | 填写值 |
|--------|--------|
| 应用名称 | `userorder` |
| 部署环境 | ECS 集群 → 选 `coffeecluster` |
| 部署包类型 | JAR 包 |
| 部署包 | 上传 `coffee-userorder/provider/target/coffee-userorder-provider-1.0-SNAPSHOT.jar` |
| JVM 参数 | 见下方 |

**JVM 参数**（把 RDS 内网地址和密码换成你的实际值）：

```
-Xms128m -Xmx256m -DENV=prod -DDB_HOST=rm-xxx.mysql.rds.aliyuncs.com:3306 -DDB_USER=userordertest -DDB_PASSWORD=你的RDS密码
```

> ⚠️ `-DDB_HOST` 填 RDS **内网地址**（同 VPC 的 ECS 走内网，快且免费），不是外网地址。
> 注册中心地址**不用填**——EDAS Agent 自动接管，指向 `coffee-prod` 空间的内置注册中心。

点"部署"，等状态变"运行中"。

### 5.2 部署快递服务

| 配置项 | 填写值 |
|--------|--------|
| 应用名称 | `expresstrack` |
| 部署包 | `coffee-expresstrack/provider/target/coffee-expresstrack-provider-1.0-SNAPSHOT.jar` |
| JVM 参数 | 与订单服务**完全相同**（数据库名不用改，`application-prod.yml` 里已为快递服务配好 `expresstracktest`） |

### 5.3 部署 API 网关

| 配置项 | 填写值 |
|--------|--------|
| 应用名称 | `coffee-app` |
| 部署包 | `coffee-app/target/coffee-app-0.0.1-SNAPSHOT.jar` |
| JVM 参数 | `-Xms128m -Xmx256m -DENV=prod` |

> **网关的 JVM 参数为什么这么短？** `coffee-app` 是纯消费者（只转发请求、不连数据库），所以不需要 `-DDB_*`，只要 `-DENV=prod`。

> **部署顺序**：先 `userorder`、`expresstrack`（提供者），再 `coffee-app`（消费者）。

**确认**：三个应用都"运行中"。点进任意应用 → "查看日志"，末尾出现 `Started ... in X seconds` 即成功。

---

## 第六节：验证

### 6.1 端到端验证

浏览器访问 `http://ECS公网IP`（前端部署见下方说明），登录后：

- **订单管理**显示订单列表 → `前端 → coffee-app → userorder → RDS` 正常
- **轨迹查询**输入测试单号能查到 → `前端 → coffee-app → Dubbo RPC → expresstrack → RDS` 整条云上链路跑通

> 前端部署到 ECS + Nginx 的步骤与 main 分支一致，可参考第 05 章云上前端部分或 main 分支 06 文档第六节。

### 6.2 验证 Dubbo 服务治理（核心演示）

**EDAS 控制台 → 微服务治理 → Dubbo → 服务查询**

1. 页面顶部"所属微服务空间"选 `coffee-prod`
2. 服务列表应出现订单、快递相关的 Dubbo 接口名（如 `com.coffee.yun.userorder.api.service.UserOrderInfoService`）
3. 点服务名 → "提供者"标签 → 看到提供该服务的 ECS 实例 IP

**这就是 Dubbo 服务治理的效果**：哪台机器提供了哪个服务，一目了然。

---

## 工作原理详解

```
本地开发（ENV=dev）：
  加载 application-dev.yml → Dubbo 连本地 Nacos（127.0.0.1:8848）→ 注册到本地 Nacos

EDAS 部署（-DENV=prod）：
  加载 application-prod.yml（只有 RDS 配置）
    → EDAS 自动注入 Pandora Agent（-javaagent）
    → Agent 在字节码层拦截 Dubbo 注册请求
    → 指向微服务空间 coffee-prod 绑定的【EDAS 内置注册中心】
    → Dubbo 2.7 以接口级注册（默认行为，无需 FORCE_INTERFACE）
    → EDAS 治理页面从内置注册中心读取元数据 → 展示服务列表
    → 通过 -DDB_HOST 等参数连接 RDS
```

**为什么本分支不用 MSE Nacos？** Dubbo 2.7 的接口级注册元数据，EDAS 内置注册中心能直接解析。而 main 分支的 Dubbo 3.x 默认应用级注册，内置注册中心解析不了，才必须上 MSE Nacos。

---

## 常见问题排查

### Q：EDAS 服务查询页面"没有数据"

按顺序检查：
1. **页面顶部微服务空间**是否选的 `coffee-prod`
2. **集群所属空间**：EDAS ECS 集群详情 → "微服务空间"字段是否为 `coffee-prod`（本分支用 EDAS 内置注册中心是**正确**的；这点与 main 分支相反——main 用 MSE，本分支用内置）
3. **JVM 参数**是否包含 `-DENV=prod`（缺它会加载 dev 配置，注册到本地 Nacos）
4. **安全组出方向**是否放通 8442、8443、8883
5. **等一等**：首次部署后元数据上报约 30 秒延迟，刷新再试

### Q：打包 `coffee-app` 报 `Could not find artifact com.coffee.yun:coffee-userorder-api`

内部依赖没上传到制品库。先完成 [阿里云配置指南 §6.5 上传 JAR](./01-aliyun-guide.md#65-上传-jar-到私服)，再重新打包。

### Q：应用日志报数据库连接失败

- `-DDB_HOST` 是否填的 RDS **内网地址**（不是外网）
- RDS 账号是否已授权对应数据库读写
- RDS 白名单是否包含 ECS 内网 IP

### Q：导入 ECS 时列表里找不到我的 ECS

ECS 必须与 EDAS 集群在同一**地域**、同一 **VPC** 才会出现。

### Q：集群创建后发现微服务空间选错了

集群的微服务空间不可改，需：① 删集群内所有应用 → ② 删集群 → ③ 重建并选对空间（`coffee-prod`，注册中心类型为 EDAS 内置）。

---

## 第八节（进阶）：用云效流水线自动化部署

> 进阶内容，第一次上云可跳过。第五节"本地打包 + 手动上传 jar"已能跑起来。

制品库（§6 已配）让内部依赖上了云，再加一条流水线就能"提交代码 → 自动构建 → 自动部署"：

1. 云效 **流水线 Flow** → 新建 → 选 **Java 构建、部署到 EDAS** 模板
2. **代码源**：绑定你的代码仓库
3. **构建步骤**：`mvn clean deploy -DskipTests`（构建机读取 `settings.xml`，靠 §6 发布到制品库的内部依赖才能编译成功）
4. **部署步骤**：选 **部署到 EDAS** → 目标应用选第五节的 `userorder`/`expresstrack`/`coffee-app` → 部署包选构建产物 → 填写与第五节相同的 JVM 参数
5. 保存并运行

跑通后，每次提交代码自动构建并部署到 EDAS，无需再手动打包上传。

---

[← 返回主文档](../README.md) | [阿里云配置指南](01-aliyun-guide.md) | [快速启动指南（本地）](05-quick-start.md)
