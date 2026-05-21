# 第 11 章：部署到阿里云 EDAS

> **本章目标**：把第 05 章在本地跑通的微服务项目，迁移到阿里云上运行，并通过 EDAS 控制台观察 Dubbo 服务治理效果。

---

## 开始前：你需要准备好什么

**硬性前置条件（缺一不可）：**

- [ ] 第 05 章已完成——本地能启动三个服务并正常访问前端页面
- [ ] 阿里云账号已注册并完成**实名认证**（部分资源需要认证才能购买）
- [ ] 已购买 **ECS 实例**（推荐：2 核 4 GB，Linux，与后续所有资源选同一地域同一 VPC）
- [ ] 已购买 **RDS MySQL 8.0**（推荐：1 核 2 GB 基础版，同地域同 VPC）
- [ ] 本地安装了 Maven 3.8+（第 05 章已验证）

---

## 本章做的事情，一句话概括

| 本地 | 云上替换为 |
|------|-----------|
| 本地 MySQL | RDS（云上托管数据库） |
| 本地 Nacos | MSE Nacos（云上托管注册中心） |
| 本地启动 Spring Boot | EDAS 部署（应用托管平台） |

三个服务的代码**完全不改**，只通过环境变量告诉它们连哪里。这就是云原生"环境无关"的核心思想。

---

## 什么是 VPC？为什么反复提到它

**VPC（专有网络）** 是你在阿里云上的私有局域网。你购买的 ECS、RDS、MSE Nacos 只要在同一个 VPC 里，就可以互相走内网通信——速度快、不计费。

**关键规则：本章涉及的所有资源，必须在同一地域、同一 VPC。**

如果你已经有了 ECS，后续创建 RDS、MSE Nacos 时，地域和 VPC 都选和 ECS 一样的。

---

## Step 1：初始化云上数据库（RDS）

### 1.1 创建数据库和账号

进入 **RDS 控制台** → 找到你的实例 → 左侧菜单：

**① 创建数据库**（做两次，分别创建订单库和快递库）

- 数据库名：`userordertest`，字符集：`utf8mb4`
- 数据库名：`expresstracktest`，字符集：`utf8mb4`

**② 创建账号**（左侧"账号管理" → 创建账号）

| 配置项 | 填写值 |
|--------|--------|
| 账号名 | `userordertest` |
| 账号类型 | 普通账号 |
| 密码 | 自定义，记住它 |
| 授权数据库 | 同时授权 `userordertest` 和 `expresstracktest`，权限选"读写" |

### 1.2 初始化表结构

RDS 不能直接用命令行连，用本地 **MySQL Workbench** 来连接：

1. Workbench → 新建连接
2. Hostname 填 RDS 的**外网地址**（RDS 控制台 → 实例详情 → 连接信息 → 外网地址）
3. Port：`3306`，Username：`userordertest`，Password：你刚设置的密码
4. 连上后，依次执行项目 `sql/` 目录下的两个脚本：
   - `sql/userorder.sql` → 在 `userordertest` 库执行
   - `sql/expresstrack.sql` → 在 `expresstracktest` 库执行

**确认**：在 Workbench 里能看到两个库下面都有表，说明初始化成功。

> **注意**：用外网地址只是为了从本机初始化数据。ECS 上的应用连数据库用**内网地址**（RDS 控制台 → 连接信息 → 内网地址），内网更快且不计流量费。

---

## Step 2：购买 MSE Nacos（云上注册中心）

本地开发用的是自己启动的 Nacos。云上用 **MSE Nacos**——阿里云全托管，不需要自己运维。

**为什么不能继续用本地 Nacos？**  
ECS 上的服务无法访问你笔记本上的 `127.0.0.1:8848`，而且本地 Nacos 关机就消失了，不适合生产。

**为什么用 MSE Nacos 而不是在 ECS 上自己装 Nacos？**  
EDAS 服务治理页面只识别 MSE Nacos 里的服务数据，自建 Nacos 无法接入 EDAS 治理功能。

### 操作步骤

1. 控制台搜索 **微服务引擎 MSE** → 进入
2. 左侧：**注册配置中心 → 实例列表 → 创建实例**

| 配置项 | 填写值 |
|--------|--------|
| 引擎类型 | **Nacos** |
| 版本 | **2.x** |
| 规格 | 开发测试版（最便宜，教学够用） |
| 地域 | 与 ECS 相同地域 |
| VPC | 与 ECS 相同 VPC |
| 交换机 | 与 ECS 相同交换机 |

3. 创建完成后，进入实例详情 → 记录**内网访问地址**

格式类似：`mse-xxxxxxxx-p.nacos-ans.mse.aliyuncs.com:8848`

**确认**：实例状态显示"运行中"。

---

## Step 3：搭建 EDAS 环境（三步，顺序不能乱）

EDAS 有三层结构，必须按顺序创建：**微服务空间 → ECS 集群 → 应用**。

> **一次性设置**：微服务空间和集群创建后不需要再动，后续只操作"应用"层。

### 3.1 配置 ECS 安全组（先做，否则 EDAS Agent 无法工作）

EDAS Agent 需要从 ECS 主动向阿里云上报数据，必须在安全组放通**出方向**：

**ECS 控制台 → 安全组 → 配置规则 → 出方向 → 手动添加**

| 端口 | 协议 | 目标 IP | 说明 |
|------|------|---------|------|
| 8442 | TCP | 0.0.0.0/0 | EDAS Agent 通信 |
| 8443 | TCP | 0.0.0.0/0 | EDAS Agent 通信 |
| 8883 | TCP | 0.0.0.0/0 | 服务元数据上报 |

### 3.2 创建微服务空间

微服务空间决定了里面所有应用使用哪个注册中心。**创建后注册中心类型不可更改**，选错了只能删掉重建。

1. **EDAS 控制台** → **资源管理 → 微服务空间 → 创建微服务空间**

| 配置项 | 填写值 |
|--------|--------|
| 空间名称 | `coffee-prod` |
| 注册中心类型 | **MSE Nacos** |
| MSE Nacos 实例 | 选择 Step 2 创建的实例 |

**确认**：空间列表里"注册中心类型"列显示 MSE 实例信息（不是 `cn-hangzhou`）。

### 3.3 创建 ECS 集群并导入服务器

1. **EDAS 控制台** → **资源管理 → EDAS ECS 集群 → 创建集群**

| 配置项 | 填写值 |
|--------|--------|
| 集群名称 | `coffeecluster` |
| **微服务空间** | 选择 `coffee-prod`（上一步创建的） |
| VPC | 与 ECS 相同的 VPC |

> **集群的微服务空间创建后不可变更。**

2. 进入集群详情 → **添加 ECS** → 勾选你的 ECS 实例 → 确认

3. 等待 3-5 分钟，EDAS 自动在 ECS 上安装 Agent

**确认**：ECS 状态变为"运行中"，说明 Agent 安装成功。

---

## Step 4：理解代码是怎么区分本地和云上的

**在打包之前，先理解这个机制**，否则后面容易配错。

### ENV 环境变量：本地/云上的切换开关

打开任意一个微服务的 `src/main/resources/application.yml`，你会看到：

```yaml
spring:
  profiles:
    active: ${ENV:dev}
```

`${ENV:dev}` 的意思：读取名为 `ENV` 的 JVM 属性，如果没有就用 `dev`。

- **本地启动**：没有设置 ENV → 默认 `dev` → 加载 `application-dev.yml`（连本地 MySQL、本地 Nacos）
- **EDAS 部署**：在控制台 JVM 参数里填 `-DENV=prod` → 加载 `application-prod.yml`（连 RDS、Nacos 由 EDAS 自动接管）

### 为什么 application-prod.yml 里没有写 Nacos 地址？

```yaml
# application-prod.yml
# Dubbo 注册中心由 EDAS Agent 自动接管，无需在此配置地址

database:
  user: ${DB_USER:userordertest}
  password: ${DB_PASSWORD:Userordertest123}
  host: ${DB_HOST:rm-xxxxxxxxx.mysql.rds.aliyuncs.com:3306}
  dbname: ${DB_NAME:userordertest}
```

EDAS 部署时会在 JVM 里注入一个 Agent（类似 Java 探针），Agent 在字节码层拦截所有 Nacos 连接请求，无论代码里写了什么地址，都会重定向到微服务空间绑定的 MSE Nacos。所以 prod 配置里不需要写 Nacos 地址——写了也会被覆盖。

### 数据库地址怎么传进去？

`application-prod.yml` 里的 `DB_HOST` 默认是占位符 `rm-xxxxxxxxx`，**部署时必须通过 JVM 参数覆盖**：

```
-DDB_HOST=你的RDS内网地址:3306
-DDB_USER=userordertest
-DDB_PASSWORD=你的RDS密码
```

在 EDAS 部署页面的"JVM 参数"输入框里填写这些参数即可。

---

## Step 5：打包应用

在本地项目根目录，打开命令行，**按顺序执行**（和第 05 章 Step 3 的 install 顺序一样）：

```cmd
rem 先安装公共依赖（如果第 05 章已经执行过，可跳过）
cd coffee-common
mvn clean install -DskipTests

cd ..\coffee-userorder\api
mvn clean install -DskipTests

cd ..\..\coffee-expresstrack\api
mvn clean install -DskipTests
```

然后分别打包三个服务：

```cmd
rem 打包订单服务
cd ..\..\coffee-userorder\provider
mvn clean package -DskipTests

rem 打包快递服务
cd ..\..\coffee-expresstrack\provider
mvn clean package -DskipTests

rem 打包 API 网关
cd ..\..\coffee-app
mvn clean package -DskipTests
```

打包完成后，产物在各模块的 `target/` 目录：

| 服务 | JAR 文件路径 |
|------|------------|
| 订单服务 | `coffee-userorder/provider/target/coffee-userorder-provider-1.0-SNAPSHOT.jar` |
| 快递服务 | `coffee-expresstrack/provider/target/coffee-expresstrack-provider-1.0-SNAPSHOT.jar` |
| API 网关 | `coffee-app/target/coffee-app-0.0.1-SNAPSHOT.jar` |

**确认**：每个 `mvn` 命令最后显示 `BUILD SUCCESS`。

---

## Step 6：在 EDAS 部署三个服务

**EDAS 控制台** → **应用管理 → 创建应用**

### 6.1 部署订单服务

| 配置项 | 填写值 |
|--------|--------|
| 应用名称 | `userorder` |
| 部署环境 | ECS 集群 → 选 `coffeecluster` |
| 部署包类型 | JAR 包 |
| 部署包 | 上传 `coffee-userorder/provider/target/coffee-userorder-provider-1.0-SNAPSHOT.jar` |
| **JVM 参数** | 见下方 |

**JVM 参数**（把 RDS 内网地址和密码替换成你的实际值）：

```
-Xms128m -Xmx256m -DENV=prod -DDB_HOST=rm-xxx.mysql.rds.aliyuncs.com:3306 -DDB_USER=userordertest -DDB_PASSWORD=你的RDS密码
```

> ⚠️ 这里填的是 RDS **内网地址**，不是外网地址。内网地址在 RDS 控制台 → 实例详情 → 连接信息里找。

点击"部署"，等待状态变为**运行中**。

### 6.2 部署快递服务

同上，配置如下：

| 配置项 | 填写值 |
|--------|--------|
| 应用名称 | `expresstrack` |
| 部署包 | `coffee-expresstrack/provider/target/coffee-expresstrack-provider-1.0-SNAPSHOT.jar` |
| **JVM 参数** | 与订单服务完全相同（数据库名无需改，`application-prod.yml` 里已为快递服务单独配置 `expresstracktest`） |

### 6.3 部署 API 网关

| 配置项 | 填写值 |
|--------|--------|
| 应用名称 | `coffee-app` |
| 部署包 | `coffee-app/target/coffee-app-0.0.1-SNAPSHOT.jar` |
| **JVM 参数** | `-Xms128m -Xmx256m -DENV=prod -DDB_HOST=rm-xxx.mysql.rds.aliyuncs.com:3306 -DDB_USER=userordertest -DDB_PASSWORD=你的RDS密码` |

> 部署顺序建议：先部署 `userorder` 和 `expresstrack`（提供者），再部署 `coffee-app`（消费者）。顺序错了服务也能启动，但日志里会出现短暂的"找不到服务提供者"警告。

**确认**：三个应用的状态都变为**运行中**。点进任意一个 → "查看日志"，能看到 Spring Boot 的启动日志，末尾出现 `Started ... in X seconds` 表示成功。

---

## Step 7：验证 Dubbo 服务治理

这是本章的核心演示目标。

**EDAS 控制台** → **微服务治理 → Dubbo → 服务查询**

1. 页面顶部"所属微服务空间"选择 `coffee-prod`
2. 服务列表中应出现订单和快递相关的 Dubbo 接口名
3. 点击任意服务名 → "提供者"标签，可以看到提供该服务的 ECS 实例 IP

**这就是 Dubbo 服务治理的效果**：哪台机器提供了哪个服务，一目了然。

---

## Step 8：前端连接云上后端

后端三个服务部署到 EDAS 后，前端 `app-admin` 需要改为调用云上的 `coffee-app`，而不是本地的 `localhost:8005`。

`coffee-app` 已配置 `@CrossOrigin`（允许跨域），所以前端可以**在本地运行、调用云上接口**，不需要把前端也部署到 ECS。

### 方式一：本地运行前端，指向云上后端（推荐，适合课程演示）

**① 确认 ECS 安全组放通了 8005 端口（入方向）**

`coffee-app` 对外提供 REST API 的端口是 8005，外网访问必须在安全组放通：

**ECS 控制台 → 安全组 → 配置规则 → 入方向 → 手动添加**

| 端口 | 协议 | 来源 IP | 说明 |
|------|------|---------|------|
| 8005 | TCP | 0.0.0.0/0 | coffee-app REST API |

**② 修改前端后端地址**

打开 `app-admin/src/api/index.js`，把 `localhost:8005` 改为 ECS 的**公网 IP**：

```js
// 修改前
const baseURL = process.env.VUE_APP_BASE_URL || 'http://localhost:8005'

// 修改后（把 x.x.x.x 替换为你的 ECS 公网 IP）
const baseURL = process.env.VUE_APP_BASE_URL || 'http://x.x.x.x:8005'
```

**③ 启动前端**

```cmd
cd D:\2026教学资料\云原生应用框架与开发\code\cloudnativeapp\app-admin
npm run dev
```

> 路径根据你实际克隆的位置调整。

访问 `http://localhost:8080`，登录后验证订单和轨迹数据能正常加载——数据来自云上数据库，说明前后端联调成功。

> 演示完课程后，把地址改回 `localhost:8005` 即可恢复本地开发模式。

---

### 方式二：把前端部署到 ECS（Nginx 静态托管）

如果需要让前端也在云上运行（其他人不需要在本地起前端就能访问），可以构建静态文件部署到 ECS。

**① 构建生产包**

```cmd
cd D:\2026教学资料\云原生应用框架与开发\code\cloudnativeapp\app-admin
set VUE_APP_BASE_URL=http://x.x.x.x:8005
npm run build
```

> 路径根据你实际克隆的位置调整。

> Windows CMD 用 `set`，PowerShell 用 `$env:VUE_APP_BASE_URL="http://x.x.x.x:8005"`

构建完成后，`app-admin/dist/` 目录下是打包好的静态文件。

**② 上传到 ECS 并配置 Nginx**

用 SFTP 工具（如 WinSCP）把 `dist/` 目录上传到 ECS，比如放到 `/var/www/coffee-admin/`。

在 ECS 上安装 Nginx（如果没有）：

```bash
# CentOS/Alibaba Cloud Linux
yum install -y nginx

# Ubuntu
apt install -y nginx
```

创建 Nginx 配置文件 `/etc/nginx/conf.d/coffee-admin.conf`：

```nginx
server {
    listen 80;
    root /var/www/coffee-admin;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

启动 Nginx：

```bash
systemctl start nginx
systemctl enable nginx
```

**③ 放通 80 端口**

ECS 安全组入方向放通 `80 TCP`，然后访问 `http://ECS公网IP` 即可看到前端页面。

---

## 工作原理：本地 vs EDAS 的区别

```
本地开发：
  Spring Boot 启动（ENV=dev）
    → 加载 application-dev.yml
    → 自己连接 localhost:8848 Nacos
    → 自己连接 localhost:3306 MySQL

EDAS 部署：
  Spring Boot 启动（-DENV=prod，由 JVM 参数注入）
    → 加载 application-prod.yml
    → EDAS 注入的 Agent 拦截 Nacos 连接
    → 自动重定向到微服务空间的 MSE Nacos
    → 通过 -DDB_HOST 等参数连接 RDS
```

代码一行没改，只换了配置和运行环境——这就是"环境无关"的 12-Factor 应用原则。

---

## 常见问题排查

### Q：EDAS 服务查询页面显示"没有数据"

按顺序检查：

1. **页面顶部微服务空间**是否选的是 `coffee-prod`（不是默认的 `cn-hangzhou`）
2. **集群所属空间**：EDAS ECS 集群详情 → 确认"微服务空间"字段是 `coffee-prod`。如果是 `cn-hangzhou`，需删集群重建（不可修改）
3. **JVM 参数**：应用的 JVM 参数是否包含 `-DENV=prod`
4. **安全组**：ECS 安全组出方向是否放通了 8442、8443、8883 端口
5. **等一等**：首次部署后服务元数据上报有约 30 秒延迟，刷新再试

---

### Q：应用日志报 `Unable to connect to Nacos` 或 Nacos 连接失败

MSE Nacos 实例与 ECS 不在同一 VPC，内网无法互通。检查两者的 VPC 设置是否一致。

---

### Q：应用日志报数据库连接失败

- 检查 JVM 参数里的 `-DDB_HOST` 是否填的是 RDS **内网地址**（不是外网地址）
- 检查 RDS 账号 `userordertest` 是否已授权对应数据库的读写权限
- 检查 RDS 白名单是否包含 ECS 的内网 IP（RDS 控制台 → 数据安全性 → 白名单）

---

### Q：导入 ECS 时列表里找不到我的 ECS

ECS 必须与 EDAS 集群在同一**地域**和同一 **VPC** 才会出现在列表中。

---

### Q：集群创建后发现微服务空间选错了

集群的微服务空间创建后不可修改。需要：
1. 先删掉集群内的所有应用
2. 删掉集群
3. 重新建集群，这次选正确的微服务空间（`coffee-prod`）

---

[← 返回主文档](../README.md) | [快速启动指南（本地）](05-quick-start.md)
