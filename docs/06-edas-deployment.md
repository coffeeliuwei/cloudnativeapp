# 第 11 章：部署到阿里云 EDAS

> **本章目标**：把第 05 章在本地跑通的微服务项目，原封不动地搬到阿里云上运行，并在 EDAS 控制台观察 Dubbo 服务治理效果。
>
> **阅读建议**：本章较长，但主线只有一条——先准备云资源，再搭 EDAS，然后打包、部署、验证。**第八节（云效流水线）是进阶内容，第一次做可以跳过，用第五节的手动上传即可。**

---

## 本章总览

### 一张图看懂要做什么

```
本地（第 05 章）                      云上（本章）
─────────────                       ─────────────
本地 MySQL          ──搬到──►        RDS（托管数据库）
本地 Nacos          ──搬到──►        MSE Nacos（托管注册中心）
本地 java -jar 启动 ──搬到──►        EDAS（应用托管平台，跑在 ECS 上）
本地 npm run dev    ──搬到──►        ECS + Nginx（托管前端静态页面）
```

**核心思想：三个服务的代码一行都不改**，只通过"环境变量"告诉它们去连云上的地址。这就是云原生"环境无关"——同一份代码，本地连本地、云上连云上。

### 全程会用到的云资源

| 资源 | 作用 | 本章在哪一节准备 |
|------|------|----------------|
| ECS | 一台云服务器，跑后端服务和前端 Nginx | 前置条件（需提前买好） |
| RDS MySQL | 云上数据库，替代本地 MySQL | 第二节 |
| MSE Nacos | 云上注册中心，替代本地 Nacos | 第二节 |
| EDAS | 把后端 jar 部署到 ECS 并做服务治理 | 第三节起 |

### 开始前的前置条件（缺一不可）

- [ ] **第 05 章已完成**——本地能启动三个后端服务并正常访问前端页面
- [ ] 阿里云账号已注册并完成**实名认证**（部分资源需实名才能购买）
- [ ] 已购买 **ECS 实例**（推荐 2 核 4 GB，Linux 系统）
- [ ] 已购买 **RDS MySQL 8.0**（推荐 1 核 2 GB 基础版）
- [ ] 你的电脑上已装好 **Maven 3.8+**（第 05 章已验证，用于本机打包）

> **ECS 不需要手动装 JDK**——EDAS 部署时会自动管理 Java 运行环境。

---

## 第一节：三个必懂的概念

动手之前先花五分钟搞懂这三件事，否则后面极易配错。

### 1.1 VPC：所有资源必须在同一个"内网"

**VPC（专有网络）** 是你在阿里云上的私有局域网。ECS、RDS、MSE Nacos 只要在**同一个 VPC**里，就能走内网互相通信——速度快、不花流量费。

> **铁律：本章涉及的 ECS、RDS、MSE Nacos，必须选同一地域、同一 VPC。**
> 如果你已经有 ECS，后面创建 RDS、MSE Nacos 时，地域和 VPC 都跟 ECS 选一样的。这是新手最常踩的坑——VPC 不一致，服务之间就是连不上，且建好后大多不可改、只能删了重建。

### 1.2 ENV 开关：一份代码，两套配置

打开任意一个微服务的 `src/main/resources/application.yml`，开头有这么一行：

```yaml
spring:
  profiles:
    active: ${ENV:dev}
```

`${ENV:dev}` 的意思是：读取名为 `ENV` 的启动参数，**没设置就用 `dev`**。

| 场景 | 启动参数 | 加载的配置文件 | 连什么 |
|------|---------|--------------|--------|
| 本地开发 | 不设（默认） | `application-dev.yml` | 本地 MySQL、本地 Nacos |
| 云上部署 | `-DENV=prod` | `application-prod.yml` | 云上 RDS、云上 Nacos |

所以"上云"在代码层面就是一件事：**部署时加上 `-DENV=prod`，再把云上的数据库地址等通过启动参数传进去。**

### 1.3 EDAS 自动接管"什么"，不接管"什么"（重点，别搞混）

EDAS 部署服务时，会在 JVM 里注入一个 Agent（类似 Java 探针）。但它接管的范围是有限的，初学者最容易在这里误解：

| 能力 | 谁负责 | 你要不要手动配地址 |
|------|--------|------------------|
| **Dubbo 服务注册 / 发现** | ✅ EDAS Agent 自动接管 | **不用**。Agent 会拦截 Dubbo 的注册请求，自动指向"微服务空间"绑定的 MSE Nacos，代码里写的地址会被覆盖 |
| **Nacos 配置中心**（`spring.config.import`） | ❌ EDAS **不**自动接管 | 默认连不上（见下方说明） |

**关于配置中心的真相（务必读）：**

两个 provider 的 `application.yml` 里有这么一行，用来从 Nacos 拉取动态配置：

```yaml
spring:
  config:
    import: "optional:nacos:coffee-userorder.properties?server-addr=${NACOS_ADDR:127.0.0.1:8848}&refreshEnabled=true"
```

- 它连的是 `${NACOS_ADDR}`，**EDAS Agent 不会改写这个地址**。云上若不设 `NACOS_ADDR`，它会去连 `127.0.0.1:8848`（ECS 本机，不存在）→ 连接失败。
- 但开头的 `optional:` 表示**连不上也不影响启动**，服务照常运行。
- **本项目的业务代码并没有真正读取 Nacos 里的任何配置项**（`cache.ttl.*` 只是演示用的占位，没有代码消费它）。所以**配置中心连不上对功能零影响**，本章主线可以完全忽略它。

> **如果你想在云上演示"配置中心热更新"**：在每个 provider 的 EDAS JVM 参数里额外加上
> `-DNACOS_ADDR=<你的 MSE Nacos 内网地址>:8848`
> 这样配置中心才会真正连上 MSE Nacos。不加则配置中心处于"未启用"状态，不报错、不影响下单查询等核心功能。

---

## 第二节：准备云资源（RDS 数据库 + MSE Nacos）

### 2.1 初始化 RDS 数据库

#### ① 创建数据库和账号

进入 **RDS 控制台** → 找到你的实例 → 左侧菜单：

**创建数据库**（做两次）：

| 数据库名 | 字符集 |
|---------|--------|
| `userordertest` | `utf8mb4` |
| `expresstracktest` | `utf8mb4` |

**创建账号**（左侧"账号管理" → 创建账号）：

| 配置项 | 填写值 |
|--------|--------|
| 账号名 | `userordertest` |
| 账号类型 | 普通账号 |
| 密码 | 自定义，**记住它** |
| 授权数据库 | 同时授权 `userordertest` 和 `expresstracktest`，权限选"读写" |

#### ② 初始化表结构

RDS 不能用命令行直连，用本地 **MySQL Workbench** 连接：

1. Workbench → 新建连接
2. Hostname 填 RDS 的**外网地址**（RDS 控制台 → 实例详情 → 连接信息 → 外网地址）
3. Port `3306`，Username `userordertest`，Password 你刚设置的密码
4. 连上后，执行项目 `sql/` 目录下的两个脚本（**注意每个脚本要在对应的数据库下执行**）：

| 脚本文件 | 在哪个库执行 |
|---------|------------|
| `sql/order初始化.sql` | `userordertest` |
| `sql/express初始化.sql` | `expresstracktest` |

> 这两个脚本就是第 05 章 Step 2 本地建库时用的同一份文件，内容含 `DROP TABLE IF EXISTS`，可重复执行。

**确认**：Workbench 里两个库下面都能看到表，即初始化成功。

> **为什么用外网地址？** 仅仅是为了从你本机初始化数据。ECS 上的应用连数据库要用**内网地址**（更快、不计流量费），内网地址在第五节部署时填入。

#### ③ 配置 RDS 白名单（必做，否则 ECS 连不上库）

RDS 默认只允许白名单内的 IP 访问。

**RDS 控制台 → 实例详情 → 数据安全性 → 白名单设置 → 修改**

1. 找到默认分组（或新建分组）
2. 填入 **ECS 的内网 IP**（ECS 控制台 → 实例详情 → 网络信息 → 私网 IP）
3. 确定

> 也可以填整个 VPC 网段（如 `172.16.0.0/16`），允许 VPC 内所有机器访问，更省事但范围更大。

**确认**：白名单列表中出现了 ECS 的内网 IP。

### 2.2 购买 MSE Nacos（云上注册中心）

本地用的是你自己启动的 Nacos，云上用全托管的 **MSE Nacos**，不用自己运维。

> **为什么不能继续用本地 Nacos？** ECS 上的服务访问不到你笔记本的 `127.0.0.1:8848`，本地 Nacos 一关机就没了。
> **为什么不在 ECS 上自己装 Nacos？** EDAS 服务治理页面只识别 MSE Nacos 里的数据，自建的接不进治理功能。

**操作步骤：**

1. 控制台搜索 **微服务引擎 MSE** → 进入
2. 左侧 **注册配置中心 → 实例列表 → 创建实例**

| 配置项 | 填写值 |
|--------|--------|
| 引擎类型 | **Nacos** |
| 版本 | **2.x** |
| 规格 | 开发测试版（最便宜，教学够用） |
| 地域 / VPC / 交换机 | **与 ECS 完全一致** |

3. 创建后进入实例详情 → 记录**内网访问地址**，形如 `mse-xxxxxxxx-p.nacos-ans.mse.aliyuncs.com:8848`

**确认**：实例状态显示"运行中"。

---

## 第三节：搭建 EDAS 环境

EDAS 有三层结构，**必须按顺序创建**：微服务空间 → ECS 集群 → 应用（应用在第五节创建）。

> 微服务空间和集群属于一次性设置，建好后不用再动。

### 3.1 配置 ECS 安全组（出方向）

EDAS Agent 需要从 ECS 主动向阿里云上报数据，必须放通**出方向**这几个端口：

**ECS 控制台 → 安全组 → 配置规则 → 出方向 → 手动添加**

| 端口 | 协议 | 目标 IP | 说明 |
|------|------|---------|------|
| 8442 | TCP | 0.0.0.0/0 | EDAS Agent 通信 |
| 8443 | TCP | 0.0.0.0/0 | EDAS Agent 通信 |
| 8883 | TCP | 0.0.0.0/0 | 服务元数据上报 |

### 3.2 创建微服务空间

微服务空间决定了里面的应用用哪个注册中心。**创建后注册中心类型不可改**，选错只能删了重建。

**EDAS 控制台 → 资源管理 → 微服务空间 → 创建微服务空间**

| 配置项 | 填写值 |
|--------|--------|
| 空间名称 | `coffee-prod` |
| 注册中心类型 | **MSE Nacos** |
| MSE Nacos 实例 | 选 2.2 创建的实例 |

**确认**：空间列表"注册中心类型"列显示的是 MSE 实例信息（不是 `cn-hangzhou`）。

### 3.3 创建 ECS 集群并导入服务器

**EDAS 控制台 → 资源管理 → EDAS ECS 集群 → 创建集群**

| 配置项 | 填写值 |
|--------|--------|
| 集群名称 | `coffeecluster` |
| 微服务空间 | 选 `coffee-prod`（上一步建的） |
| VPC | 与 ECS 相同 |

> **集群的微服务空间建好后不可改。**

然后：进入集群详情 → **添加 ECS** → 勾选你的 ECS → 确认 → 等 3-5 分钟，EDAS 自动装 Agent。

**确认**：ECS 状态变为"运行中"，说明 Agent 安装成功。

---

## 第四节：打包后端应用

在项目根目录打开命令行，**按顺序执行**（顺序和第 05 章 Step 4 一致——先装公共依赖，再打包服务）：

```cmd
rem ① 安装公共依赖（第 05 章已执行过可跳过）
cd coffee-common
mvn clean install -DskipTests

cd ..\coffee-userorder\api
mvn clean install -DskipTests

cd ..\..\coffee-expresstrack\api
mvn clean install -DskipTests
```

```cmd
rem ② 打包三个可部署的服务
cd ..\..\coffee-userorder\provider
mvn clean package -DskipTests

cd ..\..\coffee-expresstrack\provider
mvn clean package -DskipTests

cd ..\..\coffee-app
mvn clean package -DskipTests
```

打包产物在各模块的 `target/` 目录：

| 服务 | JAR 文件路径 |
|------|------------|
| 订单服务 | `coffee-userorder/provider/target/coffee-userorder-provider-1.0-SNAPSHOT.jar` |
| 快递服务 | `coffee-expresstrack/provider/target/coffee-expresstrack-provider-1.0-SNAPSHOT.jar` |
| API 网关 | `coffee-app/target/coffee-app-0.0.1-SNAPSHOT.jar` |

**确认**：每个 `mvn` 命令最后显示 `BUILD SUCCESS`。

> **必须用 `mvn`，不要用 `mvnw`**（Wrapper 会联网下载组件，国内常超时）。
> 想把 jar 发布到云上私有仓库、用流水线自动部署？那是进阶玩法，见**第八节**。本节打出 jar 即可，下一节手动上传到 EDAS。

---

## 第五节：在 EDAS 部署三个服务

**EDAS 控制台 → 应用管理 → 创建应用**，依次部署三个服务。

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

> ⚠️ `-DDB_HOST` 填的是 RDS **内网地址**（RDS 控制台 → 实例详情 → 连接信息 → 内网地址），不是外网地址。
> 💡 想同时启用配置中心，可再加 `-DNACOS_ADDR=你的MSE内网地址:8848`（见 1.3，不加不影响核心功能）。

点"部署"，等状态变为**运行中**。

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

> **网关的 JVM 参数为什么这么短？** `coffee-app` 是纯消费者（只转发请求、不连数据库），所以**不需要** `-DDB_*` 参数。只要 `-DENV=prod` 即可。

> **部署顺序**：先 `userorder`、`expresstrack`（提供者），再 `coffee-app`（消费者）。顺序反了也能启动，只是日志里会有短暂的"找不到服务提供者"警告。

**确认**：三个应用状态都变"运行中"。点进任意应用 → "查看日志"，末尾出现 `Started ... in X seconds` 即成功。

---

## 第六节：部署前端到 ECS（Nginx 静态托管）

### 6.0 先理解："打包成静态文件"是什么意思

`app-admin` 的源码是 Vue 的 `.vue`、`.js` 文件，浏览器**不认识**这些，只认识 HTML/CSS/普通 JS。

`npm run build` 就是把源码**编译打包**成浏览器能直接打开的静态文件，输出到 `app-admin/dist/`：

```
源代码（Vue 组件、ES6+ 语法）
    ↓  npm run build（构建）
dist/
  ├── index.html          ← 入口页面
  └── static/
       ├── js/app.xxx.js   ← 打包压缩后的 JS
       └── css/app.xxx.css
```

`dist/` 上传到 ECS 后，Nginx 直接把这些文件发给浏览器，**ECS 上不用装 Node.js**。

### 6.1 放通安全组端口（入方向）

**ECS 控制台 → 安全组 → 配置规则 → 入方向 → 手动添加**

| 端口 | 协议 | 说明 |
|------|------|------|
| 80 | TCP | Nginx 对外提供前端页面 |
| 8005 | TCP | coffee-app 的 REST API（浏览器调用后端用） |

### 6.2 构建前端静态文件

> **前置**：本机已装 Node.js 且执行过 `npm install`（第 05 章 Step 9 完成）。

进入前端目录（路径按你的实际克隆位置）：

```cmd
cd D:\2026教学资料\云原生应用框架与开发\code\cloudnativeapp\app-admin
```

**为什么要先设 `VUE_APP_BASE_URL`？** 构建时这个变量会被**永久写进** JS 文件，告诉用户浏览器把 API 请求发到哪。不设的话默认是 `localhost:8005`——用户浏览器会去请求自己电脑的 8005，当然找不到。

把 `x.x.x.x` 换成你的 **ECS 公网 IP**，然后构建：

Windows CMD：
```cmd
set VUE_APP_BASE_URL=http://x.x.x.x:8005
npm run build
```

PowerShell：
```powershell
$env:VUE_APP_BASE_URL="http://x.x.x.x:8005"
npm run build
```

**成功标志**：`Build complete. The dist directory is ready to be deployed.`
**确认**：`app-admin/dist/` 下出现了 `index.html` 和 `static/`。

### 6.3 把 dist/ 上传到 ECS

用 **WinSCP**（Windows 上的图形化文件传输工具）上传。

**下载**：[https://winscp.net](https://winscp.net) → Download → 安装

**连接 ECS**（WinSCP → 新建站点）：

| 配置项 | 填写值 |
|--------|--------|
| 文件协议 | SFTP |
| 主机名 | ECS 公网 IP |
| 端口 | 22 |
| 用户名 | root |
| 密码 | ECS 登录密码 |

登录后：左边是你的电脑，右边是 ECS。在右侧建目录 `/var/www/coffee-admin/`，把左侧 `app-admin/dist/` 下的**所有文件和文件夹**全选拖到右侧。

**确认**：ECS 的 `/var/www/coffee-admin/` 下能看到 `index.html`。

### 6.4 在 ECS 上安装并配置 Nginx

SSH 登录 ECS：

```bash
# Alibaba Cloud Linux / CentOS
yum install -y nginx
# Ubuntu
apt install -y nginx
```

创建站点配置：

```bash
cat > /etc/nginx/conf.d/coffee-admin.conf << 'EOF'
server {
    listen 80;
    root /var/www/coffee-admin;
    index index.html;

    # Vue Router history 模式：刷新页面不报 404
    location / {
        try_files $uri $uri/ /index.html;
    }
}
EOF
```

检查并启动：

```bash
nginx -t                # 检查语法
systemctl start nginx
systemctl enable nginx  # 开机自启
```

---

## 第七节：验证

### 7.1 端到端验证（最直观）

浏览器访问 `http://ECS公网IP`，应看到 CoffeeTrack 登录页。登录后：

- **订单管理**能显示订单列表 → 说明 `前端 → coffee-app → userorder → RDS` 链路正常
- **轨迹查询**输入 `44556677` 能查到数据 → 说明 `前端 → coffee-app → Dubbo RPC → expresstrack → RDS` 整条云上链路全部跑通

### 7.2 验证 Dubbo 服务治理（本章核心演示）

**EDAS 控制台 → 微服务治理 → Dubbo → 服务查询**

1. 页面顶部"所属微服务空间"选 `coffee-prod`
2. 服务列表应出现订单、快递相关的 Dubbo 接口名
3. 点任意服务名 → "提供者"标签 → 能看到提供该服务的 ECS 实例 IP

**这就是 Dubbo 服务治理的效果**：哪台机器提供了哪个服务，一目了然。

### 7.3 整体工作原理回顾

```
用户浏览器
  → http://ECS公网IP（Nginx 80 端口）→ 返回 Vue 静态文件

浏览器执行 JS，发起 API 请求
  → http://ECS公网IP:8005（coffee-app）
  → coffee-app 通过 Dubbo RPC 调用 userorder / expresstrack
  → 各服务从 RDS 读写数据

EDAS 内部（-DENV=prod）：
  Spring Boot 启动 → 加载 application-prod.yml
    → EDAS Agent 拦截 Dubbo 注册，重定向到微服务空间绑定的 MSE Nacos
    → Dubbo 服务注册成功
    → 通过 -DDB_HOST 等参数连接 RDS
```

代码一行没改，只换了启动参数和运行平台——这就是云原生"环境无关"的核心。

---

## 第八节（进阶）：用云效流水线实现自动化部署

> **这一节是进阶内容，第一次上云可以完全跳过。** 第五节的"本地打包 + 手动上传 jar"已经能把应用跑起来。本节介绍更"云原生"的做法：把制品发布到云上私有仓库，再用流水线实现"提交代码 → 自动构建 → 自动部署"。

### 8.1 为什么云原生需要私有制品仓库

第 05 章本地开发时，`mvn install` 把 `coffee-common`、各 `api` 装进了**你电脑的本地仓库**（`~/.m2`），其他模块从本地就能找到它们。

但**云上流水线每次都是一台全新的干净机器，没有你的本地仓库**。要让它能构建依赖了 `coffee-common` 的模块，就得先把 `coffee-common` 这些内部包**发布（`mvn deploy`）到云上仓库**，流水线再从云上拉取。这个云上仓库就是**云效制品仓库**。

这正是项目里 8 个 `pom.xml` 中 `<distributionManagement>` 的作用——声明"执行 `mvn deploy` 时，把包推到哪个远程仓库"：

```xml
<distributionManagement>
    <snapshotRepository>
        <id>aliyun-snapshot</id>                 <!-- 凭据标识，与 settings.xml 的 <server> 对应 -->
        <name>云效 Snapshot 仓库</name>
        <url>${aliyun.repo.url}</url>             <!-- 仓库地址，由 settings.xml 注入，不写死在代码里 -->
    </snapshotRepository>
</distributionManagement>
```

> **初学者必读：第 05 章没配仓库为什么也能跑？会冲突吗？**
> **不冲突。** 关键看 Maven 三个命令的区别：
>
> | 命令 | 做什么 | 读 `distributionManagement` 吗 |
> |------|--------|:--:|
> | `mvn package` | 编译 + 打 jar，放本模块 `target/` | 否 |
> | `mvn install` | 再把 jar 复制进**本地仓库** `~/.m2` | 否 |
> | `mvn deploy` | 再把 jar **上传到远程私有仓库** | **是** |
>
> 第 05 章和本章第四、五节用的都是 `install`/`package`，**根本不读** `<distributionManagement>`，所以哪怕没配云效仓库、`${aliyun.repo.url}` 是空的，也照样跑通。只有执行 `mvn deploy` 才会用到它。一句话：**`deploy` 就是 `install` 再多走一步——上传到云上。**

> **仓库地址和账号密码都不写在 `pom.xml` 里**，而是放在你本机的 `settings.xml`。这样代码库可以公开共享，每个人用自己的仓库和凭据，互不影响——这就是 pom 里用 `${aliyun.repo.url}` 占位的原因。

### 8.2 创建你自己的云效制品仓库

1. 登录 [云效](https://flow.aliyun.com) → 进入 **制品仓库 Packages**（或 [packages.aliyun.com](https://packages.aliyun.com)）
2. 左侧 **Maven → 创建仓库**
   - 类型选 **Snapshot**（本项目版本号都是 `1.0-SNAPSHOT`）
   - 记下**仓库地址**，形如 `https://packages.aliyun.com/<命名空间>/maven/<仓库名>`
3. 点页面的 **指南 / 凭证** 按钮，获取访问凭据：**用户名** 和 **密码**（云效为 Maven 仓库单独生成的一组，不是阿里云登录密码）

### 8.3 在 settings.xml 配置仓库地址和凭据

打开 Maven 的 `settings.xml`（在 `Maven安装目录/conf/settings.xml` 或 `~/.m2/settings.xml`），填两部分：

**① 凭据**（`<servers>` 内，`id` 必须是 `aliyun-snapshot`，与 pom 的 `<id>` 一致）：

```xml
<servers>
  <server>
    <id>aliyun-snapshot</id>
    <username>云效生成的用户名</username>
    <password>云效生成的密码</password>
  </server>
</servers>
```

**② 仓库地址**（`<profiles>` 内，定义 pom 用到的 `aliyun.repo.url` 并激活）：

```xml
<profiles>
  <profile>
    <id>aliyun-repo</id>
    <properties>
      <aliyun.repo.url>https://packages.aliyun.com/你的命名空间/maven/你的仓库名</aliyun.repo.url>
    </properties>
  </profile>
</profiles>

<activeProfiles>
  <activeProfile>aliyun-repo</activeProfile>
</activeProfiles>
```

> 这样 pom 里的 `${aliyun.repo.url}` 就会被替换成你自己的地址。换电脑或换人，只改各自的 `settings.xml`，**不动任何 pom 代码**。

### 8.4 发布制品（mvn deploy）

把 `install`/`package` 换成 `deploy`，**按依赖顺序执行**：

```cmd
cd coffee-common
mvn clean deploy -DskipTests

cd ..\coffee-userorder
mvn clean deploy -DskipTests

cd ..\coffee-expresstrack
mvn clean deploy -DskipTests

cd ..\coffee-app
mvn clean deploy -DskipTests
```

**成功标志**：日志末尾出现 `Uploading to aliyun-snapshot: https://packages.aliyun.com/...` 和 `BUILD SUCCESS`。
**确认**：回云效制品仓库页面，能看到 `com.coffee.yun` 下的 `coffee-common`、`coffee-userorder-api` 等制品。

> **报 401/403（认证失败）**：`settings.xml` 里 `<server>` 用户名密码不对，或 `id` 不是 `aliyun-snapshot`（必须与 pom 完全一致）。
> **报 `aliyun.repo.url` 无法解析 / URL 为空**：`<profile>` 没激活，检查 `<activeProfiles>` 是否加了 `aliyun-repo`。

### 8.5 用云效流水线自动部署到 EDAS

制品上云后，就能做"提交代码 → 自动构建 → 自动部署"的全自动流程：

1. 云效 **流水线 Flow** → 新建流水线 → 选 **Java 构建、部署到 EDAS** 模板
2. **代码源**：绑定你的代码仓库
3. **构建步骤**：执行 `mvn clean deploy -DskipTests`（流水线运行环境读取项目 `settings.xml`，或在流水线里配同样的仓库凭据）。干净的构建机正是靠 8.4 发布到云效仓库的 `coffee-common` 等内部包才能编译成功
4. **部署步骤**：选 **部署到 EDAS** → 目标应用选第五节创建的 `userorder`/`expresstrack`/`coffee-app` → 部署包选构建产物 jar → 填写与第五节相同的 JVM 参数
5. 保存并运行

跑通后，每次向代码仓库提交，流水线自动构建并部署到 EDAS，不用再手动打包上传——这就是完整的云原生交付链路。

---

## 常见问题排查

### Q：EDAS 服务查询页面显示"没有数据"

按顺序检查：
1. **页面顶部微服务空间**是否选的 `coffee-prod`（不是默认的 `cn-hangzhou`）
2. **集群所属空间**：EDAS ECS 集群详情 → "微服务空间"字段是否为 `coffee-prod`（若是 `cn-hangzhou`，需删集群重建）
3. **JVM 参数**是否包含 `-DENV=prod`
4. **安全组出方向**是否放通 8442、8443、8883
5. **等一等**：首次部署后元数据上报有约 30 秒延迟，刷新再试

### Q：应用日志报 `Unable to connect to Nacos`

MSE Nacos 实例与 ECS 不在同一 VPC，内网不通。检查两者 VPC 是否一致。

> 注意区分：如果报错来自**配置中心**（`spring.config.import`）且服务仍能正常启动运行，那是 1.3 说的"配置中心未接管"现象，对核心功能无影响，可忽略；只有 **Dubbo 注册**连不上才会导致服务调用失败。

### Q：应用日志报数据库连接失败

- `-DDB_HOST` 是否填的 RDS **内网地址**（不是外网）
- RDS 账号 `userordertest` 是否已授权对应数据库读写
- RDS 白名单是否包含 ECS 内网 IP

### Q：导入 ECS 时列表里找不到我的 ECS

ECS 必须与 EDAS 集群在同一**地域**、同一 **VPC** 才会出现。

### Q：集群创建后发现微服务空间选错了

集群的微服务空间不可改，需：① 删掉集群内所有应用 → ② 删集群 → ③ 重建集群并选对空间（`coffee-prod`）。

### Q：前端页面能打开但查询无数据

1. 先直接访问后端 `http://ECS公网IP:8005/hello/44556677`，确认后端正常
2. 浏览器 F12 → Network，看 API 请求地址是否是 `http://ECS公网IP:8005`（而非 `localhost`）——若是 `localhost`，说明 6.2 构建时没设对 `VUE_APP_BASE_URL`，重新构建上传
3. 确认安全组入方向放通了 8005

---

[← 返回主文档](../README.md) | [快速启动指南（本地）](05-quick-start.md)
