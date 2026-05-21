# 第 11 章：部署到阿里云 EDAS

> **本章目标**：把第 05 章在本地跑通的微服务项目，迁移到阿里云上运行，并通过 EDAS 控制台观察 Dubbo 服务治理效果。

---

## 开始前：你需要准备好什么

**硬性前置条件（缺一不可）：**

- [ ] 第 05 章已完成——本地能启动三个服务并正常访问前端页面
- [ ] 阿里云账号已注册并完成**实名认证**（部分资源需要认证才能购买）
- [ ] 已购买 **ECS 实例**（推荐：2 核 4 GB，Linux，与后续所有资源选同一地域同一 VPC）
- [ ] 已购买 **RDS MySQL 8.0**（推荐：1 核 2 GB 基础版，同地域同 VPC）
- [ ] 你的电脑上安装了 Maven 3.8+（用于在本机打包 JAR，第 05 章已验证）
- [ ] ECS **不需要**手动安装 JDK——EDAS 部署微服务时会自动管理 Java 运行环境

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

### 1.3 配置 RDS 白名单（必做，否则 ECS 连不上数据库）

RDS 默认只允许白名单内的 IP 访问。EDAS 部署的服务运行在 ECS 上，需要把 **ECS 的内网 IP** 加入 RDS 白名单。

**RDS 控制台 → 实例详情 → 数据安全性 → 白名单设置 → 修改**

1. 找到默认分组（或新建一个分组）
2. 在 IP 地址框里填入 ECS 的**内网 IP**（ECS 控制台 → 实例详情 → 网络信息 → 私网 IP）
3. 点击确定

> **一个 ECS 对应一个内网 IP**。如果后续添加了更多 ECS，需要把每台的内网 IP 都加进来。也可以填 VPC 网段（如 `172.16.0.0/16`），允许整个 VPC 内的机器访问，更方便但范围更大。

**确认**：白名单列表中出现了 ECS 的内网 IP。

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

## Step 4：理解代码如何切换运行环境

**在打包之前，先理解这个机制**，否则后面容易配错。

### ENV 参数：环境切换开关

打开任意一个微服务的 `src/main/resources/application.yml`，你会看到：

```yaml
spring:
  profiles:
    active: ${ENV:dev}
```

`${ENV:dev}` 的意思：读取名为 `ENV` 的 JVM 属性，如果没有就用 `dev`。

- **`ENV` 未设置**（默认）：加载 `application-dev.yml`
- **EDAS 部署时设置 `-DENV=prod`**：加载 `application-prod.yml`（连 RDS、Nacos 由 EDAS 自动接管）

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

## Step 8：部署前端到 ECS（Nginx 静态托管）

### 什么是"打包成静态文件"？

`app-admin` 的源代码是用 Vue 写的 `.vue`、`.js` 文件，浏览器**无法直接运行**这些文件——浏览器只认识 HTML、CSS、普通 JavaScript。

`npm run build` 就是把这些源文件**编译打包**成浏览器能直接打开的 HTML/CSS/JS，输出到 `app-admin/dist/` 目录。这个过程叫"构建"，产出的文件叫"静态文件"。

```
源代码（Vue 组件、ES6+ 语法）
    ↓  npm run build（构建/打包）
dist/
  ├── index.html          ← 入口 HTML
  └── static/
       ├── js/app.xxx.js  ← 所有 JS 打包压缩成一个文件
       └── css/app.xxx.css
```

`dist/` 里的文件上传到 ECS 后，Nginx 直接把它们发给用户的浏览器，**ECS 上不需要安装 Node.js**，也不需要运行任何 Vue 相关程序。

---

### 8.1 放通安全组端口

**ECS 控制台 → 安全组 → 配置规则 → 入方向 → 手动添加**

| 端口 | 协议 | 说明 |
|------|------|------|
| 80   | TCP  | Nginx 对外提供前端页面 |
| 8005 | TCP  | coffee-app REST API（前端调用后端用） |

---

### 8.2 构建前端静态文件

> **前置**：你的电脑上需要已安装 Node.js，且执行过 `npm install`（第 05 章 Step 9 已完成）。

打开命令行，进入前端目录：

```cmd
cd D:\2026教学资料\云原生应用框架与开发\code\cloudnativeapp\app-admin
```

（路径根据你实际克隆的位置调整）

**为什么要设置 `VUE_APP_BASE_URL`？**  
构建时这个变量会被**永久写入**输出的 JS 文件，告诉用户浏览器应该把 API 请求发到哪个地址。如果不设置，默认值是 `localhost:8005`——用户的浏览器会去请求自己电脑上的 8005 端口，当然找不到。

把 `x.x.x.x` 替换为你的 **ECS 公网 IP**，然后执行：

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

构建需要 1-3 分钟。

**成功标志**：
```
  Build complete. The dist directory is ready to be deployed.
```

**确认**：`app-admin/dist/` 目录下出现了 `index.html` 和 `static/` 文件夹。

---

### 8.3 把 dist/ 上传到 ECS

用 **WinSCP** 把构建产物上传到 ECS。WinSCP 是 Windows 上的图形化文件传输工具，专门用来在 Windows 和 Linux 服务器之间传文件。

**下载**：[https://winscp.net](https://winscp.net) → 点 Download → 安装

**连接 ECS**：

打开 WinSCP → 新建站点：

| 配置项 | 填写值 |
|--------|--------|
| 文件协议 | SFTP |
| 主机名 | ECS 公网 IP |
| 端口 | 22 |
| 用户名 | root |
| 密码 | ECS 登录密码 |

点"登录"，连接成功后左边是你的电脑，右边是 ECS。

在 ECS（右侧）创建目录 `/var/www/coffee-admin/`（右键 → 新建目录）。

在你的电脑（左侧）导航到 `app-admin/dist/`，**全选 dist/ 下的所有文件和文件夹**，拖到右侧 `/var/www/coffee-admin/` 里。

**确认**：ECS 的 `/var/www/coffee-admin/` 下能看到 `index.html`。

---

### 8.4 在 ECS 上安装并配置 Nginx

SSH 登录 ECS，执行：

```bash
# Alibaba Cloud Linux / CentOS
yum install -y nginx

# Ubuntu
apt install -y nginx
```

创建前端站点配置文件：

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

检查配置语法并启动：

```bash
nginx -t                        # 检查配置是否有语法错误
systemctl start nginx
systemctl enable nginx          # 设置开机自启
```

---

### 8.5 验证

在浏览器访问 `http://ECS公网IP`，应看到 CoffeeTrack 登录页。

登录后：
- "订单管理"能显示订单列表 → 前端 → coffee-app → RDS 链路正常
- "轨迹查询"输入 `44556677` 能查到数据 → 整条云上链路全部跑通

---

## EDAS 工作原理

```
用户浏览器
  → http://ECS公网IP（Nginx 80端口）
  → 返回 Vue 静态文件

浏览器执行 JS，发起 API 请求
  → http://ECS公网IP:8005（coffee-app）
  → coffee-app 通过 Dubbo RPC 调用 userorder / expresstrack
  → 各服务从 RDS 读写数据

EDAS 内部流程（-DENV=prod）：
  Spring Boot 启动
    → 加载 application-prod.yml
    → EDAS Agent 拦截 Nacos 连接，重定向到 MSE Nacos
    → Dubbo 服务注册到 MSE Nacos
    → 通过 -DDB_HOST 等参数连接 RDS
```

代码一行没改，只换了 JVM 参数和运行平台——这就是云原生"环境无关"的核心思想。

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
