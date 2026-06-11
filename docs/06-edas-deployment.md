# 第 11 章：把微服务部署到阿里云（多路径上云）

> **本章目标**：把第 05 章在本地跑通的整套项目（三个后端服务 + Vue 前端 + MySQL + Nacos），原封不动地搬到阿里云上运行。
>
> **本章特色**：阿里云不是只有一种上云方式。本章按"运维负担从重到轻"介绍 **多条上云路径**，让你看清不同方式的差异,再根据项目体量选择合适的方案。
>
> | 路径 | 形态 | 你管什么 | 阿里云管什么 | 主要操作方式 | 本章定位 |
> |------|------|---------|------------|------------|---------|
> | **B**：EDAS 托管 | PaaS | 写代码、上传 jar | JDK、进程、服务治理、伸缩 | **控制台点鼠标** | ⭐ **推荐主线** |
> | **C**：SAE Serverless | Serverless | 只写代码 + 上传 jar | JDK、进程、伸缩、网络 | **控制台点鼠标** | 推荐（弹性场景） |
> | **A**：ECS 自管 | IaaS | 操作系统、JDK、进程、日志 | 服务器硬件、网络 | 终端敲命令 | 🔧 进阶 / 选读 |
>
> **阅读建议**：本课程主线是 **"能在控制台点鼠标完成的就不敲命令"**——所以**推荐第一次上云的同学走点击式的路径 B（EDAS，Part 6）**，先把"上云"跑通、建立信心。**路径 A（Part 5）需要在终端敲一连串命令，已降为"进阶/选读"**，留给想理解 Linux 底层、`java -jar` 怎么把应用跑起来的同学。先把 **公共部分（Part 1–4）** 读完，再做 **Part 6（推荐）**。

---

## 🧭 本章学习主线：本地一步步 ──► 云上一步步替换

> 这是贯穿本课程的核心理念，也是本章每一节的展开顺序。
> **云原生不是"重学一套云上的东西"，而是"把本地做过的每一步在云上对应替换"**。

### 关键升级：从"一台机器跑三个进程"到"一台机器一个微服务"

本地开发时，为了图省事，你的 3 个微服务全跑在 **同一台笔记本** 上，靠端口（7001 / 8001 / 8005）区分。但 **真实的微服务架构** 是这样的：

```
本地（第 05 章）：                       云上（本章）：
                                        
   ┌────────────────────┐              ECS-1 ─── coffee-userorder（:7001）
   │  你的笔记本电脑    │                 │
   │  ┌──────────────┐  │              ECS-2 ─── coffee-expresstrack（:8001）
   │  │ userorder    │  │                 │
   │  │ expresstrack │  │              ECS-3 ─── coffee-app（:8005）+ Nginx（:80）
   │  │ coffee-app   │  │                 │
   │  └──────────────┘  │                 │
   └────────────────────┘                 ▼
                                     共用：MSE Nacos + RDS MySQL
```

**为什么云上要拆成多台 ECS？**

- **故障隔离**：一台机器挂了不影响另外两个服务
- **独立扩容**：订单服务流量大就只给 ECS-1 加资源，不浪费在其他服务
- **独立发布**：改了快递服务只重启 ECS-2，订单业务不中断
- **资源隔离**：一个服务内存泄漏不会拖累另一个

这就是 **微服务架构的本质**——服务之间在物理层面也隔离开。本地"一机三进程"只是开发期的妥协，云上要回归微服务该有的样子。

### 本地做过的事 → 云上一一对应

| 第 05 章本地做过的 | 本章云上对应替换为 | 在哪一节 |
|------------------|------------------|---------|
| 一台笔记本跑 3 个进程 | **3 台 ECS，每台跑 1 个服务** | Part 3.1 |
| 本地装的 MySQL 8.0（端口 3307） | 阿里云 RDS MySQL（内网地址 3306） | Part 3.2 |
| 本地启动的 nacos-server-2.x（127.0.0.1:8848） | MSE Nacos 托管实例 | Part 3.3 |
| 本地 `mvn install` 把库装进 `~/.m2`（只你这台机能用） | **`mvn deploy` 把库发到云效私有仓库**（任何机器都能拉） | Part 4.2 / 4.3 |
| 本地 `mvn package` 生成的 jar | **同一份 jar，不改一行代码** | Part 4.4 |
| 本地 `java -jar xxx.jar` 启动 3 个进程 | 路径 A：3 台 ECS 各跑一个 `manage.sh` / 路径 B：EDAS 把 3 个应用部署到 3 台 ECS | Part 5 / Part 6 |
| 本地 `application-dev.yml`（连本地 MySQL / 本地 Nacos） | 同一文件 + 启动时注入云上地址，**或** 用 `application-prod.yml` | Part 5 / Part 6 |
| 本地 `npm run dev`（开发服务器 :8080） | `npm run build` 生成静态文件 → ECS-3 + Nginx 托管 | Part 8 |
| 本地浏览器访问 `http://localhost:8080` | 浏览器访问 `http://<ECS-3公网IP>` | Part 9 |

**关键洞察**：jar 一行代码不改，只通过"环境变量 / 启动参数"告诉它去连云上的地址——这就是云原生"环境无关"的本质。学完本章你会真正理解 Part 2.2 的 `${ENV:dev}` 和 Part 2.3 的 `${DB_HOST:...}` 是怎么做到的。

---

## 目录

- [Part 1 上云形态总览与选择](#part-1-上云形态总览与选择)
- [Part 2 必懂的 4 个概念（所有路径共用）](#part-2-必懂的-4-个概念所有路径共用)
- [Part 3 公共云资源准备](#part-3-公共云资源准备)
- [Part 4 本地打包](#part-4-本地打包)
- [Part 5 路径 A — IaaS 上云（ECS + manage.sh）](#part-5-路径-a--iaas-上云ecs--managesh)
- [Part 6 路径 B — PaaS 上云（EDAS 托管）](#part-6-路径-b--paas-上云edas-托管)
- [Part 7 路径 C — Serverless 上云（SAE 托管）](#part-7-路径-c--serverless-上云sae-托管)
- [Part 8 部署前端到 ECS（路径共用）](#part-8-部署前端到-ecs路径共用)
- [Part 9 验证](#part-9-验证)
- [Part 10 进阶 — 云效流水线自动化](#part-10-进阶--云效流水线自动化)
- [附录 A：三条路径对比速查表](#附录-a三条路径对比速查表)
- [附录 B：常见问题排查（按路径分组）](#附录-b常见问题排查按路径分组)

---

## Part 1 上云形态总览与选择

### 1.1 一张图看懂三种上云形态

```
             你的责任                              阿里云的责任
             ──────                                  ──────
路径 A       ✋ 装操作系统补丁
（IaaS）     ✋ 装 JDK
             ✋ scp 上传 jar
             ✋ 写 manage.sh 启进程
             ✋ 看 /var/log 排查问题      ─────►      🏢 提供 ECS 虚拟机
                                                     🏢 网络、磁盘、机房

路径 B       ✋ 在控制台上传 jar
（PaaS）     ✋ 填 JVM 参数              ─────►      🏢 自动装 JDK
                                                     🏢 启动/重启进程
                                                     🏢 Dubbo 注册自动接管
                                                     🏢 治理页面可视化

路径 C       ✋ 写代码、提交              ─────►      🏢 帮你打包、扩缩容
（Serverless）                                       🏢 实例数最小 1，可按指标弹性扩容
                                                     🏢 按 vCPU·秒 + GB·秒计费
```

**核心思想：越往下，你的"运维负担"越轻，但"对底层的控制力"也越弱。** 这正是云原生的一个核心权衡。

### 1.2 应该选哪条路径？

一个简单的决策树：

```
你想"控制台点鼠标"还是"自己敲命令管服务器"？
├─ 点鼠标（推荐，本课程主线）
│   ├─ 要 Dubbo 服务治理可视化 / 常规部署 ──► 路径 B（EDAS）⭐推荐
│   └─ 流量小且波动大、想压成本      ──► 路径 C（SAE Serverless）
└─ 我就想敲命令学 Linux 底层 ────────────► 路径 A（ECS）🔧进阶/选读
```

**本课程推荐学习顺序**：

1. **先做路径 B（EDAS，推荐主线）**：同样一份 jar，不用 SSH 上服务器、不敲命令，**控制台点一点就部署完**——这正是本课程"少敲键盘"的主线做法，最快跑通看到效果。
2. **（进阶/选读）再回头做路径 A**：如果你想搞清楚"控制台点的那一下，背后到底发生了什么"，再去 Part 5 亲手装 JDK、上传 jar、运行 `manage.sh`，把底层看透。
3. 对比两者，你会真正理解 PaaS"帮你省掉了哪些命令"的价值。

> **为什么把路径 A 降为进阶？** 路径 A 要在终端敲一连串命令（装 JDK、scp、`chmod`、`manage.sh`…），初学者容易抄错、卡壳。本课程的原则是**能点鼠标就别敲命令**，所以默认推荐点击式的 EDAS；命令行那套留给"想学底层"的进阶同学，不挡在主线上。

### 1.3 本章会用到的云资源清单

| 资源 | 作用 | 数量 | 路径 A 用 | 路径 B 用 | 在哪一节准备 |
|------|------|:---:|:---:|:---:|---|
| ECS 虚拟机 | 每台跑 1 个微服务 | **3 台** | ✅ | ✅ | Part 3.1 |
| RDS MySQL | 云上数据库 | 1 个 | ✅ | ✅ | Part 3.2 |
| MSE Nacos | 云上注册中心 + 配置中心 | 1 个 | ✅ | ✅ | Part 3.3 |
| EDAS | 应用托管 + 服务治理 | 1 套 | ❌ | ✅ | Part 6 |

> 💡 **3 台 ECS 路径 A 和路径 B 共用**：路径 B 的 EDAS 实际上也是把 jar 部署到你的 ECS 上跑（区别在于由 EDAS Agent 管理），所以两条路径在 ECS 这层兼容。学完路径 A 想切到路径 B，3 台 ECS 不用重买。

### 1.4 前置条件（缺一不可）

- [ ] **第 05 章已完成**：本地能启动三个后端服务并正常打开前端页面
- [ ] **阿里云账号**已注册并完成 **实名认证**
- [ ] 已购买 **3 台 ECS 实例**：推荐 **2 核 2 GB**（按量付费，学完释放，每天几元）
- [ ] 已购买 **RDS MySQL 8.0**：推荐 1 核 2 GB 基础版（开发测试足够）
- [ ] 你的电脑上已装 **Maven 3.8+** 和 **Node.js 18+**（第 05 章已验证）

> 💡 **为什么是 2 核 2 GB？** 每台 ECS 只跑一个 Spring Boot 微服务，2 GB 内存足够（堆内存设 512 MB 还剩 1.5 GB 给系统）。如果坚持一台机器跑多个服务才需要 4 GB+。**这就是微服务化的另一个好处——单机规格可以下调**。
>
> 💡 **ECS 操作系统怎么选？** Alibaba Cloud Linux 3 是阿里云的官方推荐系统，软件源国内访问快；Ubuntu 22.04 命令熟悉度高，本章以 Ubuntu 为主线（apt 命令），Alibaba Cloud Linux 同学把 `apt` 换成 `yum / dnf` 即可。
>
> 💡 **同样的操作要在 3 台 ECS 上重复做** 怎么办？后面 Part 5 会教你用 ssh 循环或脚本批量执行；初学者第一次手动操作 3 遍，体验完整流程即可。

---

## Part 2 必懂的 4 个概念（所有路径共用）

> 动手之前花十分钟把这 4 个概念搞清楚，否则后面 80% 的"连不上"问题都是因为对这里有误解。

### 2.1 VPC：所有云资源的"内网"

**VPC（Virtual Private Cloud，专有网络）** 是阿里云为你划出的一块私有局域网。ECS、RDS、MSE Nacos 只要在 **同一个 VPC** 里，就能走 **内网** 互相通信：

- **快**：不出机房，延迟 < 1ms
- **省**：内网流量不计费（公网流量按 GB 计费）
- **安全**：不暴露在公网

```
        ┌─────────────── VPC（你的私有网络）───────────────┐
        │                                                  │
        │   ┌──────┐    内网   ┌──────┐    内网   ┌──────┐ │
        │   │ ECS  │ ─────────►│ RDS  │           │ MSE  │ │
        │   │      │ ◄─────────┤      │           │Nacos │ │
        │   └──────┘           └──────┘           └──────┘ │
        │                                                  │
        └──────────────────────────────────────────────────┘
                              ▲
                              │ 公网（你的电脑访问）
                              │
                          浏览器 / SSH 客户端
```

> **🔥 铁律**：本章涉及的 ECS、RDS、MSE Nacos **必须选同一地域（如华东1 杭州）、同一 VPC**。地域和 VPC 一旦创建后大多不可修改，错了只能删了重建——这是新手最常踩的坑。
>
> **操作建议**：先看你的 ECS 在哪个地域、哪个 VPC，后续创建 RDS、MSE Nacos 时跟着它走。

### 2.2 ENV 开关：一份代码，两套配置

打开任意一个微服务的 `src/main/resources/application.yml`（或 `coffee-app` 的 `application.properties`），开头都有这么一行：

```yaml
spring:
  profiles:
    active: ${ENV:dev}
```

**这是 Spring Boot 的 profile 切换机制**：

- `${ENV:dev}` 读取系统环境变量 `ENV` 的值
- 如果没设置 `ENV`，默认值是 `dev` → 加载 `application-dev.yml`
- 启动时设置 `ENV=prod`（用 JVM 参数 `-DENV=prod`），就会加载 `application-prod.yml`

| 场景 | 启动参数 | 加载的配置 | 结果 |
|------|---------|-----------|------|
| 本地开发 | 不设（默认） | `application-dev.yml` | 连本地 MySQL、本地 Nacos |
| 云上部署 | `-DENV=prod` | `application-prod.yml` | 连云上 RDS、云上 Nacos |

> ⚠️ **重要例外（路径 A 特殊用法）**：路径 A 的 `manage.sh` 出于"修改配置无需改环境变量"的考虑，**不**设 `ENV=prod`，而是仍走 `dev` profile，然后通过 `--spring.config.additional-location` 加载 ECS 上独立的 `application-dev.yml` 文件覆盖默认值。两种用法都有效，原因见 Part 5。

### 2.3 环境变量驱动：把"环境差异"挪出代码

打开 `coffee-userorder/provider/src/main/resources/application-prod.yml`，你会看到：

```yaml
dubbo:
  registry:
    address: ${DUBBO_REGISTRY:nacos://mse-xxxxxxxx-p.nacos.mse.aliyuncs.com:8848}

database:
  user:     ${DB_USER:userordertest}
  password: ${DB_PASSWORD:Userordertest123}
  host:     ${DB_HOST:rm-xxxxxxxxx.mysql.rds.aliyuncs.com:3306}
  dbname:   ${DB_NAME:userordertest}
```

**`${变量名:默认值}` 的含义**：
- 启动时如果设置了环境变量 / JVM 参数（如 `-DDB_HOST=xxx`），用设置的值
- 没设置，用冒号后面的默认值

**为什么这样设计？** 这是云原生的核心实践之一——"配置与代码分离"：

- ✅ 同一份编译好的 jar，可以部署到任意环境（本地、测试、生产），只需启动时改变量
- ✅ 数据库密码、Nacos 地址不写死在代码里，不会随着代码被推到 Git 公开仓库
- ✅ 换机房、换数据库时不用重新打包

> 💡 **课程中你会见到 6 个关键变量**，全部用于上云时覆盖默认值：
>
> | 变量名 | 含义 | 举例 |
> |--------|------|------|
> | `ENV` | 激活的 profile | `prod` |
> | `DUBBO_REGISTRY` | Dubbo 注册中心地址 | `nacos://mse-xxx:8848` |
> | `NACOS_ADDR` | Nacos 配置中心地址（不带 `nacos://` 前缀） | `mse-xxx:8848` |
> | `DB_HOST` | RDS 连接地址（含端口） | `rm-xxx:3306` |
> | `DB_USER` | RDS 账号 | `userordertest` |
> | `DB_PASSWORD` | RDS 密码 | `xxxxxx` |

### 2.4 注册中心 vs 配置中心（都用 Nacos，但用途完全不同）

新手最容易把这两个搞混。本项目里 **Nacos 同时承担了两个角色**：

| 角色 | 谁用它 | 用来做什么 | 配置项 |
|------|--------|----------|--------|
| **注册中心** | Dubbo（RPC 框架） | provider 启动时上报"我在 IP:Port 提供 xxx 接口"；consumer 启动时来查"谁提供 xxx" | `dubbo.registry.address` |
| **配置中心** | Spring Boot | 应用启动时从 Nacos 拉取 `coffee-userorder.properties`，运行中可以热更新 | `spring.config.import` |

```
         ┌────── 注册中心角色（给 Dubbo 用） ──────┐
         │                                       │
provider ──注册"我在 192.168.1.5:20880"──► Nacos │
                                          ▲     │
consumer ──查询"谁提供 OrderService"────────┘     │
         ◄──返回 192.168.1.5:20880──────────────  │
         │                                       │
         └───────────────────────────────────────┘

         ┌────── 配置中心角色（给 Spring Boot 用） ─┐
         │                                       │
应用启动 ──拉取 coffee-userorder.properties──► Nacos │
         │ 例如 cache.ttl.order=1800             │
         └───────────────────────────────────────┘
```

**重要细节（直接影响后面排查）**：

- **路径 B（EDAS）的 Agent 会拦截 Dubbo 注册请求**，自动重定向到 EDAS 微服务空间绑定的 MSE Nacos，**但不接管 `spring.config.import`**（配置中心）。
- 本项目的业务逻辑 **没有真正消费 Nacos 配置中心里的任何配置项**（`cache.ttl.*` 只是演示占位），所以配置中心连不上不影响下单、查询等核心功能——`spring.config.import` 前面的 `optional:` 前缀就是为此设计的。

> 这一段不理解没关系，后面 Part 6 部署 EDAS 时回头看就懂了。先记住一句话：**Dubbo 用 Nacos 找服务，Spring 用 Nacos 拉配置，是两件事。**

---

## Part 3 公共云资源准备

> 路径 A 和路径 B 都需要这一节里的所有资源。提前一次性建好，后面两条路径都能用。
>
> **🧭 对照第 05 章**：本节做的事就是把你本地装的两样东西在云上"找替身"——
> - 本地 MySQL（你电脑上 Docker 或安装包跑的那个 3307 端口的）→ **RDS MySQL**
> - 本地 Nacos（你电脑上 `bash startup.sh -m standalone` 跑的那个 8848 端口的）→ **MSE Nacos**
>
> 替身建好后，等下启动 jar 时只需要把"地址"换一下，业务代码完全不动。

### 3.1 准备 3 台 ECS

我们要按"一台机器跑一个微服务"来部署:

| ECS 编号 | 跑什么 | 端口 |
|---------|--------|------|
| **ECS-1** | `coffee-userorder` 订单服务 | 7001 |
| **ECS-2** | `coffee-expresstrack` 快递服务 | 8001 |
| **ECS-3** | `coffee-app` 网关 + Nginx 前端 | 8005 + 80 |

> ECS-3 之所以同时跑网关和前端,是因为它俩都是"对外入口",合在一台机器上对外只暴露一个公网 IP,省事。

#### 第 1 步:在阿里云控制台买 3 台 ECS

1. 浏览器打开 [阿里云控制台](https://ecs.console.aliyun.com) 并登录
2. 顶部搜索 `云服务器 ECS`,点进控制台
3. 左上角点 **"创建实例"** 蓝色按钮
4. 进入购买页,从上往下选:
   - **付费模式**:选 `按量付费`(学完释放,每台每天几元)
   - **地域**:挑一个,比如"华东1(杭州)"——**3 台必须同地域**
   - **规格**:选 `2 vCPU 2 GiB`(最低规格就够,如 `ecs.t6-c1m1.large`)
   - **镜像**:
     - **ECS-1 / ECS-2（跑 Java 后端）**:`Ubuntu 22.04 64位`(裸系统即可,这两台不需要 Web 面板)
     - **ECS-3（跑前端 + 网关）**:**推荐在镜像页签切到「镜像市场」搜 `宝塔面板`,选一个预装「宝塔 Linux 面板」的镜像**——这样 Nginx 已经装好,后面 Part 8 部署前端全程在网页面板点鼠标,不用敲命令。想练命令行的同学也可以和前两台一样选 `Ubuntu 22.04`,走 Part 8 的"进阶/命令行"备选。详见 Part 8 开头的"怎么选"。
   - **存储**:系统盘 `40 GiB`,默认即可
   - **专有网络 VPC**:**3 台必须选同一个**,记住你选的 VPC
   - **虚拟交换机**:同样 3 台一致
   - **公网 IP**:**勾上"分配公网 IPv4 地址"**,带宽选`按使用流量`,`1 Mbps`
   - **登录凭证**:选"自定义密码",设个 root 密码(3 台用同一个密码方便记)
   - **实例名称**:第一次买就填 `coffee-ecs-1`
5. 点底部 **"确认订单"** → **"创建实例"**
6. **重复步骤 3-5 再买两次**,实例名称分别填 `coffee-ecs-2` 和 `coffee-ecs-3`,其他配置完全一样

#### 第 2 步:记下 3 台 ECS 的 IP 地址

7. 回 ECS 控制台 → 左侧 **"实例与镜像 → 实例"**,你能看到刚买的 3 台
8. 等 1-2 分钟实例状态变成 **"运行中"**
9. 找张纸或开个记事本,**把下面这张表填满**:

   | 角色 | 实例名 | 公网 IP | 私网 IP |
   |------|--------|---------|---------|
   | ECS-1(订单) | coffee-ecs-1 | ___________ | ___________ |
   | ECS-2(快递) | coffee-ecs-2 | ___________ | ___________ |
   | ECS-3(网关+前端) | coffee-ecs-3 | ___________ | ___________ |

   **公网 IP / 私网 IP 在哪看**:实例列表里每台 ECS 那行都直接显示

10. 后面所有步骤都要用到这张表,**收好别丢**

> **铁律**:3 台 ECS、RDS、MSE Nacos **必须在同一个地域、同一个 VPC**。错了大多不能改,只能删了重建——下面 Part 3.2 / 3.3 创建 RDS 和 MSE 时,**地域和 VPC 都跟着 ECS 选**。

### 3.2 准备 RDS MySQL

云上的 MySQL,替代你电脑上装的本地 MySQL。

#### 第 1 步:买一个 RDS 实例

1. 浏览器登录阿里云控制台,顶部搜索框输入 `云数据库 RDS`,点进控制台
2. 左上角点 **"创建实例"**
3. 在表单里选:
   - **付费类型**:`按量付费`
   - **数据库类型**:`MySQL`
   - **版本**:`8.0`
   - **系列**:`基础版`(便宜)
   - **存储类型**:默认即可
   - **规格**:`1 核 2 GB`(够课程用)
   - **地域 + VPC**:**跟你 ECS 一模一样**(超重要)
4. 点 **"立即购买"** → 完成订单
5. 回实例列表,**等约 5 分钟**实例状态变 **"运行中"**

#### 第 2 步:创建两个数据库

6. 点实例的**名字**(蓝色链接)进入实例详情
7. 左侧菜单点 **"数据库管理"** → 右上角 **"创建数据库"** 按钮
8. **第一个数据库**:
   - **数据库(DB)名称**:填 `userordertest`
   - **字符集**:选 `utf8mb4`
   - 点 **"确定"**
9. 再点一次 **"创建数据库"**,**第二个数据库**:
   - **数据库名称**:填 `expresstracktest`
   - **字符集**:选 `utf8mb4`
   - 点 **"确定"**

#### 第 3 步:创建一个数据库账号

10. 左侧菜单点 **"账号管理"** → **"创建账号"**
11. 填写:
    - **数据库账号**:`userordertest`
    - **账号类型**:`普通账号`
    - **密码**:自己设一个,**记到记事本里**(后面要用)
    - **已授权数据库**:左边那一栏的 `userordertest` 和 `expresstracktest` 两个都**点右箭头**移到右边,**权限选 `读写`**
12. 点 **"确定"**

#### 第 4 步:把 RDS 地址加到白名单

13. 左侧菜单点 **"数据安全性"** → **"白名单设置"**
14. 找到默认的白名单分组,点 **"修改"**
15. 在 IP 列表里填:`172.16.0.0/16`(允许整个 VPC 内访问,最简单的填法)
16. 点 **"确定"** 保存

#### 第 5 步:记下两个 RDS 地址

17. 左侧菜单点 **"数据库连接"**
18. 你会看到两个地址,**都复制到记事本里**:
    - **内网地址**(形如 `rm-xxx.mysql.rds.aliyuncs.com:3306`) —— ECS 上的应用用这个
    - **外网地址**(同样形式但前缀略不同) —— 等下你本机灌数据用这个

> 第一次用 RDS 可能还需要点 **"申请外网地址"** 才能看到外网地址。

#### 第 6 步:用 MySQL Workbench 灌入表结构

19. 你本地电脑下载并安装 [MySQL Workbench](https://dev.mysql.com/downloads/workbench/)(免费图形化客户端)
20. 打开 Workbench → **"+"号** 新建连接
21. 填写连接信息:
    - **Hostname**:第 18 步记的 **外网地址**(不带 `:3306`,Port 单独填)
    - **Port**:`3306`
    - **Username**:`userordertest`
    - 点 **"Test Connection"** → 输入第 11 步的密码,**Successfully Connected** 即通
22. 双击保存的连接进入数据库
23. 左侧能看到 `userordertest` 和 `expresstracktest` 两个库
24. **执行第一个 SQL**:
    - 双击左侧 `userordertest`(库名变粗体表示选中)
    - 顶部菜单 **File → Open SQL Script** → 选 `cloudnativeapp/sql/order初始化.sql`
    - 点工具栏 **⚡(闪电)** 按钮执行
    - 看到底部输出 `xxx row(s) affected` 即成功
25. **执行第二个 SQL**:
    - 双击左侧 `expresstracktest`(切换到这个库)
    - **File → Open SQL Script** → 选 `cloudnativeapp/sql/express初始化.sql`
    - 点 **⚡** 执行
26. 刷新左侧库列表,两个库下面都能看到生成的表,**说明灌数据成功** ✅

> **为什么这里用外网地址,后面又说用内网?**
> - 现在是**你电脑**连 RDS 灌数据,你电脑在公网,所以用 **外网地址**
> - 后面**ECS 上的应用**连 RDS,ECS 和 RDS 在同一 VPC,用 **内网地址** 更快免费

### 3.3 准备 MSE Nacos

云上的 Nacos,替代你电脑上启动的本地 Nacos。下面跟着步骤一步一步做。

#### 第 1 步:进入 MSE 控制台

1. 浏览器打开 [阿里云控制台](https://home.console.aliyun.com),用你的账号登录
2. 看到顶部有一个**搜索框**(写着"搜索产品、服务、文档…"),在里面输入 `微服务引擎`,按回车
3. 在搜索结果里点击 **"微服务引擎 MSE"**,等页面跳转到 MSE 控制台

#### 第 2 步:创建一个 Nacos 实例

4. 进入 MSE 后,看左侧菜单,点击 **"注册配置中心"**
5. 在新打开的页面上方,点 **"创建实例"** 蓝色按钮
6. 进入创建表单,按真实表单从上往下依次选:
   - **商品类型**:选 `普通实例(按量付费)`(学完释放,按小时计费)
   - **地域**:选你 ECS 所在的地域(比如"华东1(杭州)")
   - **产品版本**:选 **`专业版`**（⚠️ **不要选"开发版"**——本项目是 Dubbo 3.x，路径 C 在 SAE 上要走 **"MSE Nacos 专业版"官方集成**，只支持专业版；开发版接不进 SAE 的官方集成）
   - **引擎类型**:`Nacos`
   - **引擎版本**:`2.x`（无缝兼容 Nacos 1.x 客户端；除非要用 MCP Registry 否则不用选 3.x）
   - **引擎规格**:默认 `1核2G` 即可(教学够用)
   - **集群节点数**:`3`（**专业版最少 3 节点**，官方要求,保障高可用——这也是专业版比开发版贵的地方；开发版只有 1 节点）
   - **网络类型**:`专有网络` → **专有网络 VPC** 选 ECS 同一个 VPC → **交换机** 选 ECS 同 VPC 里那个
   - **公网带宽**:默认 `0` = 不开公网。本地电脑要连它调试,就把带宽拉到 **大于 0**(如 1–2 Mbps,按流量计费)
7. 点页面右下角 **"立即创建"**,确认订单

#### 第 3 步:等实例就绪,记下两个地址

8. 回到实例列表,你能看到刚才创建的实例,状态是"创建中"。**等 3-5 分钟**,刷新页面,直到状态变成 **"运行中"**
9. 点击实例的**名字**(蓝色链接),进入实例详情页
10. 在详情页找到 **"接入地址"** 区域,这里有两行地址,分别**复制到记事本里存好**:
    - **内网访问地址** —— 后面云上的 3 台 ECS 用这个
    - **公网访问地址** —— 后面你本地电脑用这个

#### 第 4 步:开放公网白名单(教学用)

11. 还在实例详情页,左侧菜单点 **"网络配置"**
12. 找到 **"公网白名单"** 那一栏,点右边的 **"修改"**
13. 在白名单输入框里填:`0.0.0.0/0`(意思是允许任何地址访问,**仅教学用**,生产环境绝对不能这么填)
14. 点 **"确定"** 保存

#### 第 5 步:本地验证一下能不能连上

15. 回你自己的电脑,打开 VSCode,打开文件 `coffee-userorder/provider/src/main/resources/application-dev.yml`
16. 找到 `dubbo.registry.address` 那一行,把地址改成 `nacos://<第 10 步记下的公网地址>`,保存
17. 在 VSCode 终端启动 coffee-userorder/provider(`mvn spring-boot:run` 或者按 IDE 的运行按钮)
18. 启动成功后,**回 MSE 控制台**,左侧菜单点 **"服务管理 → 服务列表"**
19. 列表里能看到 `coffee-userorder` 这个服务,**就说明本地电脑成功连上了云上的 Nacos**——这一节就过了 ✅

> **两个地址的用途别搞混**:
> - **内网地址**:云上 3 台 ECS 之间通信用(同 VPC,快、免费)
> - **公网地址**:你本地电脑临时调试用(必须配 `0.0.0.0/0` 白名单才能访问)

---

## Part 4 项目模块结构与制品准备

> **📌 关于"敲命令打包"先说一句**：本节会用到 `mvn deploy` / `mvn package` 等命令在你本地电脑构建 jar。**本课程的推荐主线其实是不用手敲这些——交给 [第 12 章云效流水线](07-cicd-pipeline.md)，`git push` 后在网页上点几下自动构建发布。** 本节的本地命令属于 **进阶/理解原理**：第一次跑、想搞清楚"jar 和库是怎么来的"时手动做一遍即可；日常迭代请走流水线。模块结构（4.1）这一小节是理解流水线配置的基础，**建议都读**。

> 这一节做两件事：① 搞清项目的 6 个模块谁是"库"谁是"应用"；② 把它们准备到可以部署的状态。
>
> **🧭 对照第 05 章**：你在本地已经做过 `mvn install` 和 `mvn package`，命令一样。**但有一件事第 05 章为了简化没讲：项目里有 3 个模块（coffee-common 和两个 api）是设计成"放进 Maven 私有仓库供其他模块引用"的**——它们不是要被部署运行的 jar，是要被 **依赖** 的库。本节把这层结构补齐。

### 4.1 项目模块分类：3 个库 + 3 个应用

打开 `cloudnativeapp/` 你能看到 6 个模块（每个都是一个独立的 `pom.xml`）。它们分两类，命运完全不同：

```
                    ┌───────── 库模块（Library）─────────┐
                    │  被其他模块引用，不会被独立运行       │
                    │  ▼                                  │
                    │  coffee-common          公共工具类  │
                    │  coffee-userorder/api   订单接口定义 │
                    │  coffee-expresstrack/api 快递接口定义│
                    └─────────────┬───────────────────────┘
                                  │ 被依赖
                                  ▼
                    ┌───────── 应用模块（Deployable）────┐
                    │  打成 jar 后部署到 ECS 上运行        │
                    │                                    │
                    │  coffee-userorder/provider  → ECS-1│
                    │  coffee-expresstrack/provider→ ECS-2│
                    │  coffee-app                  → ECS-3│
                    └────────────────────────────────────┘
```

| 模块 | 类型 | 干什么 | 谁依赖它 |
|------|------|--------|---------|
| `coffee-common` | **库** | 公共工具类、统一返回结构 `Result<T>` 等 | 所有 5 个其他模块 |
| `coffee-userorder/api` | **库** | 订单服务的 Dubbo 接口定义（`OrderService` 等） | `coffee-userorder/provider`、`coffee-app` |
| `coffee-expresstrack/api` | **库** | 快递服务的 Dubbo 接口定义 | `coffee-expresstrack/provider`、`coffee-app` |
| `coffee-userorder/provider` | **应用** | 订单服务实现 + Spring Boot 启动类 | （部署到 ECS-1） |
| `coffee-expresstrack/provider` | **应用** | 快递服务实现 + Spring Boot 启动类 | （部署到 ECS-2） |
| `coffee-app` | **应用** | API 网关 + Dubbo 消费者 + Spring Boot 启动类 | （部署到 ECS-3） |

> 💡 **为什么 api 要单独成模块？**
> Dubbo RPC 的本质是 "本地接口调用 → 网络传输 → 远端实现执行"。consumer（coffee-app）和 provider（coffee-userorder）**必须共享同一份接口定义**才能编译——所以接口定义被抽到独立的 `api` 模块,两边都通过 Maven 依赖把它拉进来。这是 RPC 框架的标准做法。

### 4.2 库模块放哪：本地仓库 vs 云效私有仓库

3 个库模块编译出来后,要"存"在某个 Maven 仓库里,让应用模块构建时能拉取到它们。**有两种存放方式**:

| 方式 | 命令 | 存到哪 | 谁能用 | 适用场景 |
|------|------|--------|--------|---------|
| **A. 本地仓库** | `mvn install` | `~/.m2/repository/` | **只有你这台电脑** | 第一次本地跑通、单人开发 |
| **B. 云效私有仓库** | `mvn deploy` | 阿里云云效制品仓库 | **任何机器**（你的电脑、同事电脑、云效流水线、EDAS 构建机器都能拉） | 多人协作、CI/CD、生产部署 |

#### 为什么要用云效私有仓库

后面你会用云效流水线自动构建 `coffee-userorder/provider`。**流水线机器是台全新的临时机器,没有你电脑的 `~/.m2` 仓库**,它要去哪里找 `coffee-userorder-api`?Maven 中央仓库里没有这个内部包。

**解决办法**:把 3 个库模块 `mvn deploy` 到云效私有仓库,流水线机器从云效拉。

这正是项目所有 `pom.xml` 里 `<distributionManagement>` 节点的作用——告诉 Maven "执行 deploy 时把包推到云效仓库地址":

```xml
<distributionManagement>
    <snapshotRepository>
        <id>aliyun-snapshot</id>
        <url>${aliyun.repo.url}</url>   <!-- 地址不写死在 pom，由 settings.xml 注入 -->
    </snapshotRepository>
</distributionManagement>
```

#### 第 1 步:创建你自己的云效制品仓库

1. 浏览器打开 [云效控制台](https://flow.aliyun.com),用阿里云账号登录
2. 顶部菜单点 **"制品仓库"**(或直接打开 https://packages.aliyun.com)
3. 左侧菜单选 **"Maven"** → 右上角点 **"创建仓库"**
4. 弹出表单填写:
   - **仓库名称**:自己起,比如 `coffee-snapshots`
   - **类型**:选 `Snapshot`(本项目所有 pom 的 version 都是 `1.0-SNAPSHOT`)
   - **描述**:随便填
5. 点 **"确定"**
6. 进入新建好的仓库详情页,**记下两件事到记事本**:
   - **仓库地址**(形如 `https://packages.aliyun.com/maven/repository/<命名空间>/coffee-snapshots`)
   - 点页面 **"指南"** 或 **"凭据"** 按钮 → 显示一对**用户名和密码**,复制保存(这是云效自动给你生成的,**不是你阿里云登录密码**)

#### 第 2 步:在本地电脑配置 Maven settings.xml

7. 打开本机 Maven 的 settings.xml,位置:
   - **Windows**:`C:\Users\你的用户名\.m2\settings.xml`(没有这个文件就新建)
   - **Mac/Linux**:`~/.m2/settings.xml`
8. 用 VSCode 打开,**整个文件内容替换成**(替换 4 个占位):
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <settings>
     <!-- 云效仓库的用户名和密码 -->
     <servers>
       <server>
         <id>aliyun-snapshot</id>
         <username>替换为第 6 步记的用户名</username>
         <password>替换为第 6 步记的密码</password>
       </server>
     </servers>

     <!-- 云效仓库地址 -->
     <profiles>
       <profile>
         <id>aliyun-repo</id>
         <properties>
           <aliyun.repo.url>替换为第 6 步记的仓库地址</aliyun.repo.url>
         </properties>
       </profile>
     </profiles>

     <activeProfiles>
       <activeProfile>aliyun-repo</activeProfile>
     </activeProfiles>
   </settings>
   ```
9. 保存文件

> **为什么仓库地址不写在 pom.xml?**
> pom.xml 会推到公开 Git 仓库,而仓库地址包含命名空间(等于暴露账号信息)。所以 pom 里用 `${aliyun.repo.url}` 占位,每个学生在自己机器的 settings.xml 里填自己的——这样代码可以共享,凭据各管各的。

### 4.3 把 3 个库模块发布到云效

回你本机,打开终端(Windows 用 CMD/PowerShell,Mac/Linux 用 Terminal)。

1. 进项目根目录 `cloudnativeapp/`
2. **按顺序粘贴下面 3 条命令**,每条等执行完(看到 `BUILD SUCCESS`)再粘下一条:
   ```bash
   cd coffee-common && mvn clean deploy -DskipTests
   ```
   ```bash
   cd ../coffee-userorder/api && mvn clean deploy -DskipTests
   ```
   ```bash
   cd ../../coffee-expresstrack/api && mvn clean deploy -DskipTests
   ```
3. 每条命令末尾要能看到两行关键日志:
   - `Uploading to aliyun-snapshot: https://packages.aliyun.com/...`
   - `BUILD SUCCESS`
4. 全部跑完后,浏览器回云效制品仓库的页面,刷新,**应看到 3 个 artifact**:
   - `coffee-common`
   - `coffee-userorder-api`
   - `coffee-expresstrack-api`

> **报错处理**:
> - `401/403 认证失败` → settings.xml 里用户名密码不对,或 `<id>` 不是 `aliyun-snapshot`
> - `aliyun.repo.url 为空` → settings.xml 里 `<activeProfiles>` 没激活,检查那一段

### 4.4 打包 3 个应用模块

接着在终端继续粘贴 3 条命令打 jar:

```bash
cd ../../coffee-userorder/provider && mvn clean package -DskipTests
```
```bash
cd ../../coffee-expresstrack/provider && mvn clean package -DskipTests
```
```bash
cd ../../coffee-app && mvn clean package -DskipTests
```

每条最后看到 `BUILD SUCCESS` 即成功。产物在 3 个不同位置:

| jar 文件 | 后面部署到 |
|---------|----------|
| `coffee-userorder/provider/target/coffee-userorder-provider-1.0-SNAPSHOT.jar` | ECS-1 |
| `coffee-expresstrack/provider/target/coffee-expresstrack-provider-1.0-SNAPSHOT.jar` | ECS-2 |
| `coffee-app/target/coffee-app-0.0.1-SNAPSHOT.jar` | ECS-3 |

> **报错处理**:
> - `Could not find artifact com.coffee.yun:coffee-userorder-api:1.0-SNAPSHOT` → 说明 4.3 没成功,3 个库没上传到云效,回去检查
> - 一定用 `mvn` 不要用 `mvnw`(国内下载 Maven 常超时)

### 4.5 制品流总结

```
你的代码                                                  云上跑起来
─────────                                              ─────────
                  mvn deploy ↗ 云效私有仓库
3 个库模块 ───────┘                ↓
                                  ↓ (provider/网关构建时拉取)
                                  ↓
3 个应用模块 ─── mvn package → 3 个 jar → 上传到 3 台 ECS → 启动
```

**关键认识**:云效私有仓库是 **库模块的"中转站"** ——库不需要被部署运行,它只需要"在那里",等应用模块构建时来拉取。这是云原生 "制品与代码分离、库与应用分离" 思想的体现。

> ### ❓ 常见疑问：上云的程序要"调用"制品仓库的库，那 jar 里的配置要不要为此改？
>
> **不用改。** 这里有两件**完全不同**的事，别混在一起：
>
> **① 制品仓库只在「构建时」被用到，和运行无关。**
> 你 `mvn package` 打包应用时，Maven 才去云效仓库把 `coffee-common` / `api` 这几个库拉下来，**直接打进 Spring Boot fat jar 里**。打完包，这些库就已经**变成 jar 自身的一部分**了。
>
> ```
> 构建时（本地 / 云效流水线）              运行时（云上 ECS / SAE）
> mvn package                              java -jar xxx.jar
>   └─ 去云效仓库拉库 → 打进 fat jar  ──►   jar 自包含，运行时不再碰制品仓库
> ```
>
> 所以**云上跑的 jar 运行时根本不连制品仓库**，自然没有"为了调用仓库而改 jar 里配置"这回事。而且这 3 个**库模块本身不含任何配置文件**（没有 yml/properties，纯代码），更没有可改的东西。
>
> **② 真正要改的"配置"是数据库 / Nacos 地址，和制品仓库无关。**
> 应用要改的只有连云上 RDS、MSE 的地址，而且**不靠改 jar 内置的 yml**，而是外部注入——路径 A 用 ECS 上的外部 `application-dev.yml` + `manage.sh` 的 `NACOS_ADDR`；路径 B/C 用 JVM 参数 / 环境变量 / SAE 服务注册发现选项（详见 Part 5/6/7）。
>
> **③ 唯一要"指向云效仓库"的配置，是打包机器上的 `settings.xml`（Part 4.2 配的那个）**，它告诉 Maven 去云效拉私有库——这只在**构建机**（你本地电脑或云效流水线）上生效，**既不在 jar 里、也不上云**。
>
> | 东西 | 要不要为"用制品仓库"改 | 说明 |
> |---|---|---|
> | 库模块（common / api） | ❌ 不用 | 纯代码，无配置文件 |
> | 应用 jar 里的 `application*.yml` | ❌ 不用 | 制品仓库是构建时的事，运行时 jar 自包含 |
> | 应用的 DB / Nacos 地址 | ✅ 要改（但与仓库无关） | 外部注入，见各路径 Part 5/6/7 |
> | 打包机的 `settings.xml` | ✅ 配一次 | 指向云效私有仓库，只在构建时生效 |

---

## Part 5 路径 A — IaaS 上云(3 台 ECS + manage.sh)🔧 进阶 / 选读

> **⚠️ 这是进阶/选读章节，不是本课程主线。** 路径 A 需要在终端敲一连串命令（装 JDK、scp、`chmod`、`manage.sh`…），适合**已经跑通过推荐主线（Part 6 EDAS），想进一步理解"应用到底是怎么被 `java -jar` 跑起来的"** 的同学。
>
> **第一次上云、只想快点跑通的同学请直接做 [Part 6 路径 B（EDAS，推荐主线）](#part-6-路径-b--paas-上云edas-托管)——全程控制台点鼠标，不敲命令。**

这条路径让你 **自己装 JDK、自己上传 jar、自己启进程**,亲手把 3 个服务跑到 3 台 ECS 上。

**对照本地(第 05 章)**:你之前在一台笔记本开 3 个终端窗口分别 `java -jar`。**现在把这 3 个进程分散到 3 台 ECS**,每台跑一个。代码完全不改,只是把"3 个终端"换成"3 台机器"。

> ### 📝 动手前先记住：路径 A 你总共只需要改 **3 个文件、5 处值**
>
> 下面后续所有命令、上传、脚本**一律照抄不用改**；唯一要你亲手填的就是这张表里的值——而且**全部来自 Part 3 准备资源时记下的**：
>
> | 要改的文件 | 改哪几处 | 填什么（来源） |
> |---|---|---|
> | `deploy/manage.sh` | `NACOS_ADDR`（约第 22 行，**1 处**） | 你的 **MSE Nacos 内网地址**，结尾带 `:8848`（Part 3.3 记的；ECS 与 MSE 同 VPC 走内网，公网地址是给本地调试用的，ECS 上别填） |
> | `deploy/config/userorder/application-dev.yml` | `user` / `password` / `host`（**3 处**） | 你的 **RDS 账号 / 密码 / 地址**（`host` 结尾保留 `:3306`）（Part 3.2 记的） |
> | `deploy/config/expresstrack/application-dev.yml` | `user` / `password` / `host`（**3 处，和 userorder 填一样的**） | 与 userorder **同一套 RDS 账号/密码/地址** |
>
> **几个不用改的点，先打消顾虑：**
> - **网关 coffee-app 不连数据库**——没有它的配置文件要改，它只用到 `manage.sh` 里的 `NACOS_ADDR`。
> - 两个 `application-dev.yml` 里的 `dbname`（`userordertest` / `expresstracktest`）**已经填好，不要动**。
> - 3 台 ECS 用的是**同一份 `manage.sh`**（脚本自动识别本机有哪个 jar），所以 `manage.sh` 只改 `NACOS_ADDR` 这 1 处、改一次即可。
> - jar 包、目录路径、所有终端命令**全部照抄**。

### 5.1 先在本地改好 3 个文件（就上面那 5 处值）

为了避免后面在 ECS 上用 vim 编辑文件,**所有配置改动都在你本机用 VSCode 改完,然后整个文件上传**。

1. 用 VSCode 打开本地 `cloudnativeapp/deploy/manage.sh`
2. 找到顶部第 20 行附近的 `NACOS_ADDR="..."`
3. 把双引号里的地址换成 **Part 3.3 第 10 步记下的 MSE Nacos 内网地址**,保存
4. 用 VSCode 打开 `cloudnativeapp/deploy/config/userorder/application-dev.yml`
5. 把 `yourDbUser` / `yourDbPassword` / `your-rds-address` 替换成 **Part 3.2 记的 RDS 账号 / 密码 / 内网地址**(注意 host 后面要带 `:3306`),保存
6. 用 VSCode 打开 `cloudnativeapp/deploy/config/expresstrack/application-dev.yml`,做同样的替换,保存

> 改完 3 个文件,确认本地的 `deploy/` 目录都已经填好了你自己的实际值。

### 5.2 打开 3 个"会话管理"网页终端

不要在自己电脑装 SSH 客户端,**直接在阿里云网页里开终端**。

7. 浏览器打开阿里云 ECS 控制台 → 左侧 **"实例与镜像 → 实例"**
8. 找到 **ECS-1** 那一行,**最右边点"远程连接"** → 弹窗里展开找到 **"通过会话管理远程连接"**(Session Manager,通过云助手 Agent 接入,不需要 SSH 密码) → 点 **"免密登录"**
9. 浏览器里出现一个黑底白字的终端窗口,**这就是 ECS 内部的命令行**。注意:Linux 实例默认以 `ecs-assist-user` 普通用户接入(不是 root),**先粘贴这一条切到 root**(后面所有命令都以 root 执行):
    ```bash
    sudo su - root
    ```
10. 用同样的方法,给 **ECS-2 和 ECS-3 各开一个新的浏览器标签页**(每个都先 `sudo su - root`)——最后你应该有 3 个会话管理标签页并排

> 后面所有"在 ECS 上敲的命令",就是切到对应标签页 **粘贴命令**。
>
> 💡 **会话管理 vs Workbench**:两者都是阿里云提供的网页连接方式。**会话管理** 走云助手通道,不依赖 SSH 端口、不要密码,更安全也更简单(本课程选这条);**Workbench** 走 SSH 协议,需要密码。注意一个差别:**会话管理只有命令行界面,没有上传/下载文件按钮**(Workbench 才有)——所以下面 5.4 上传文件走的是 ECS **实例详情页的"上传/下载文件"功能**,和终端是两个入口,别在终端里找上传按钮。

### 5.3 在每台 ECS 上装 JDK 17 并建目录

12. 切到 **ECS-1** 的 会话管理标签页
13. 在终端里**粘贴这一条命令并回车**:
    ```bash
    apt update && apt install -y openjdk-17-jdk && mkdir -p ~/coffee/jars ~/coffee/config && java -version
    ```
14. **等 1-2 分钟** 装完。最后一行能看到 `openjdk version "17.0.x"` 即成功
15. 切到 **ECS-2** 标签页,**粘贴同一条命令**,等装完
16. 切到 **ECS-3** 标签页,**粘贴同一条命令**,等装完

### 5.4 给每台 ECS 上传文件(用实例详情页的"上传/下载文件")

> 📌 **上传入口不在终端里**:会话管理的网页终端只有命令行。上传文件用 ECS 控制台 **实例详情页** 自带的 **"上传/下载文件"** 功能(同样基于云助手,不需要公网 SSH,单文件最大 500 MB,本项目的 jar 几十 MB 完全够)。入口:ECS 控制台 → 实例列表 → 点实例 ID 进详情页 → 找到 **"上传/下载文件"**(在"定时与自动化任务"相关页签下,以你控制台实际看到的为准) → **"上传文件"**。

#### 上传 ECS-1(订单服务,3 个文件)

17. 浏览器再开一个标签页,进 **ECS-1 的实例详情页**,找到 **"上传/下载文件"** → 点 **"上传文件"**
18. **第 1 次上传 manage.sh**:
    - **本地文件**:点"选择文件",找到 `cloudnativeapp/deploy/manage.sh`
    - **目标路径**:填 `/root/coffee/`
    - 提交,等状态变"成功"
19. **第 2 次上传订单服务 jar**:再点 上传文件:
    - **本地文件**:`cloudnativeapp/coffee-userorder/provider/target/coffee-userorder-provider-1.0-SNAPSHOT.jar`
    - **目标路径**:`/root/coffee/jars/`
20. **第 3 次上传订单服务配置**:再点 上传文件:
    - **本地文件**:`cloudnativeapp/deploy/config/userorder/application-dev.yml`
    - **目标路径**:`/root/coffee/config/userorder/`(如果提示路径不存在,先回会话管理终端粘 `mkdir -p /root/coffee/config/userorder` 再重传)

#### 上传 ECS-2(快递服务,3 个文件)

21. 进 **ECS-2 的实例详情页** → "上传/下载文件",**重复同样的 3 次上传**,但文件不同:
    | 上传次序 | 本地文件 | 目标路径 |
    |--------|---------|---------|
    | 1 | `deploy/manage.sh` | `/root/coffee/` |
    | 2 | `coffee-expresstrack/provider/target/coffee-expresstrack-provider-1.0-SNAPSHOT.jar` | `/root/coffee/jars/` |
    | 3 | `deploy/config/expresstrack/application-dev.yml` | `/root/coffee/config/expresstrack/` |

#### 上传 ECS-3(网关,只需 2 个文件)

22. 进 **ECS-3 的实例详情页** → "上传/下载文件",网关不连数据库,**只传 2 个文件**:
    | 上传次序 | 本地文件 | 目标路径 |
    |--------|---------|---------|
    | 1 | `deploy/manage.sh` | `/root/coffee/` |
    | 2 | `coffee-app/target/coffee-app-0.0.1-SNAPSHOT.jar` | `/root/coffee/jars/` |

#### 检查上传成功

23. 每台 ECS 的会话管理终端里粘贴 `ls -R /root/coffee` 看一眼:
    - **ECS-1** 应该看到 manage.sh、jars/ 下有 userorder 的 jar、config/userorder/ 下有 application-dev.yml
    - **ECS-2** 类似,但是 expresstrack 的 jar 和 config
    - **ECS-3** 只有 manage.sh 和 jars/ 下的 coffee-app jar

### 5.5 启动服务

24. 切到 **ECS-1** 标签页,粘贴:
    ```bash
    cd ~/coffee && chmod +x manage.sh && ./manage.sh start
    ```
25. **等约 30 秒**,看到下面的输出即成功(只启动 userorder,另两个显示"跳过"是正常的):
    ```
    本机检测到 1 个 jar，开始按依赖顺序启动...
      [✓] userorder: 启动成功 (PID=12345, 端口=7001)
      [-] expresstrack: 跳过（本机未部署此服务）
      [-] app: 跳过（本机未部署此服务）
    ```
26. 切到 **ECS-2** 标签页,粘贴**同一条命令**,等启动成功(本机会启 expresstrack)
27. 切到 **ECS-3** 标签页,粘贴**同一条命令**,等启动成功(本机会启 coffee-app)

> "跳过"是正常的——manage.sh 检测到本机没有那个 jar 就跳过,这样 3 台 ECS 才能用同一份脚本。
>
> 顺序最好:**先 ECS-1 / ECS-2(provider),再 ECS-3(consumer)**。反了 coffee-app 启动时会有几秒"找不到提供者"警告,不影响最终成功。

### 5.6 配置 3 台 ECS 的安全组(在网页 GUI 点鼠标)

让外部能访问你 ECS 上的端口,需要在安全组开"入方向"规则。

28. 回浏览器,ECS 控制台 → 左侧 **"实例与镜像 → 实例"**
29. 点 ECS-1 那一行的 **"安全组"** 列(或选实例进详情 → 找到"安全组"卡片)
30. 进入安全组详情后,顶部点 **"访问规则"** Tab → **"入方向"** 子 Tab → **"手动添加"** 按钮
31. 在 ECS-1 添加规则:
    - **协议类型**:`自定义 TCP`
    - **端口范围**:`7001/7001`
    - **授权对象**:`0.0.0.0/0`
    - 点 **"保存"**
32. **对 ECS-2 重复**(端口填 `8001/8001`)
33. **对 ECS-3 重复两次**(分别加 `8005/8005` 和 `80/80`)

> ECS-1 / ECS-2 的端口只是为了让你浏览器能 curl 健康检查,生产环境通常不开。

### 5.7 验证服务都跑起来了

34. 浏览器分别打开下面 3 个地址,**都应返回 `{"status":"UP"}`**:
    - `http://<ECS-1 公网IP>:7001/actuator/health`
    - `http://<ECS-2 公网IP>:8001/actuator/health`
    - `http://<ECS-3 公网IP>:8005/actuator/health`
35. 回 MSE Nacos 控制台 → 实例详情 → **"服务管理 → 服务列表"**,应能看到 3 个服务都已注册

**有问题怎么办**:回那台 ECS 的会话管理终端,粘贴下面查问题:
```bash
cd ~/coffee && ./manage.sh status    # 看进程在不在跑
cd ~/coffee && ./manage.sh logs      # 看启动日志(Ctrl+C 退出)
```

> 💡 **日志里报连不上 Nacos（`failed to connect to nacos`）**：多半是 MSE 的 **内网白名单** 没放行 ECS 的私网网段。回 MSE 控制台 → 实例详情 → **网络配置 → 内网白名单**，把 ECS 所在 VPC 的网段（如 `172.16.0.0/16`）加进去即可（做法和 7.4 给 SAE 加 虚拟交换机 网段一样）。

### 5.8 日常管理

以后想停 / 重启 / 看日志,回对应 ECS 的会话管理终端粘命令:

| 想做的事 | 在会话管理终端粘贴 |
|---------|------------------|
| 看本机服务状态 | `cd ~/coffee && ./manage.sh status` |
| 实时看本机日志 | `cd ~/coffee && ./manage.sh logs` |
| 停本机服务 | `cd ~/coffee && ./manage.sh stop` |
| 重启本机服务 | `cd ~/coffee && ./manage.sh restart` |
| 换新版本 jar | 实例详情页"上传/下载文件"传新 jar 覆盖旧的 → 跑 `./manage.sh restart` |

---

## Part 6 路径 B — PaaS 上云(EDAS 托管)⭐ 推荐主线

> **✅ 这是本课程推荐的默认上云方式。** 全程在 EDAS 网页控制台 **点鼠标** 完成——不用 SSH、不用敲命令装 JDK、不用 `manage.sh`。第一次上云的同学跟着这一节走即可。

这条路径你 **不用 SSH 上 ECS** ——在 EDAS 网页控制台勾几下,3 个应用就部署到 3 台 ECS 了。

**对照路径 A**:路径 A 你要自己装 JDK、自己 scp、自己启进程;路径 B 这些 **EDAS 都自动帮你做**,你只在网页上选"把 X 应用部署到 ECS-Y"。

> 如果路径 A 还在跑,**先到 3 台 ECS 的会话管理终端各执行 `cd ~/coffee && ./manage.sh stop`** 停掉,避免端口冲突。

### 6.1 确认 3 台 ECS 的安全组出方向放行(通常什么都不用做)

EDAS 要在 ECS 上装个 Agent 帮你管进程,Agent 需要通过 **出方向** 和阿里云的 EDAS 服务端通信。

1. **阿里云安全组的出方向默认是"全部放行"**——只要你没有人为收紧过出方向规则,这一节**不需要任何操作**,直接进 6.2
2. 怎么确认:ECS 控制台 → 找 ECS-1 → 点 **"安全组"** 列 → 进入安全组详情 → **"访问规则"** Tab → **"出方向"** 子 Tab,看到默认那条"允许全部"规则在即可
3. 另外,EDAS 导入 ECS 时还会**自动添加**它需要的安全组规则(如监控日志采集端口),这些系统规则**不要删改**

> **只有当你的安全组出方向被收紧成"白名单制"时**,才需要手动放行 Agent 的出方向流量(此时建议直接咨询 EDAS 文档/工单确认所需端口,不要凭印象填)。症状是:EDAS 应用看似部署成功,但"微服务治理"页面长期显示"没有数据"。

### 6.2 创建 EDAS 微服务空间

5. 阿里云控制台顶部搜索 `EDAS`,点进 EDAS 控制台
6. 左侧 **"资源管理 → 微服务空间"** → 右上角 **"创建微服务空间"**
7. 弹出表单填:
   - **空间名称**:`coffee-prod`
   - **地域**:跟 ECS 同地域
   - **注册中心类型**:选 **`MSE Nacos`**(**不要**选默认的"EDAS 注册中心")
   - **MSE Nacos 实例**:下拉选你 Part 3.3 创建的那个 MSE 实例
8. 点 **"确定"**
9. 在空间列表里能看到 `coffee-prod`,"注册中心"那列显示的是 MSE 实例 ID(形如 `mse-xxxxxx-p`)

> **必须用 MSE Nacos 不能用 EDAS 默认注册中心** ——EDAS 内置那个是旧版,不支持 Dubbo 3.x,治理页面会查不到服务。

### 6.3 创建 EDAS ECS 集群并导入 3 台 ECS

10. 左侧 **"资源管理 → EDAS ECS 集群"** → **"创建集群"**
11. 填:
    - **集群名称**:`coffeecluster`
    - **微服务空间**:选 `coffee-prod`(刚才建的)
    - **网络类型**:专有网络
    - **VPC**:与你 ECS 相同的那个
12. 点 **"确定"**
13. 进入新建的集群详情页 → 点 **"添加 ECS 实例"**
14. **同时勾选 3 台 ECS**(ECS-1 / ECS-2 / ECS-3) → 点 **"下一步"** → **"导入"**
15. **等 3-5 分钟**,刷新页面,3 台 ECS 都应显示 **"运行中"** + **"已安装 Agent"**

### 6.4 部署订单服务到 ECS-1

16. 左侧 **"应用管理 → 应用列表"** → 右上角 **"创建应用"**
17. 第一步"基本信息":
    - **应用名称**:`userorder`
    - **微服务空间**:选 `coffee-prod`
    - **集群类型**:ECS 集群
    - **集群**:选 `coffeecluster`
    - **应用运行环境**:Java
    - **JDK 版本**:OpenJDK 17
18. 点 **"下一步"** 到"应用配置"
19. **部署包来源**:`上传 JAR 包`
20. 点 **"选择文件"** → 选 `coffee-userorder/provider/target/coffee-userorder-provider-1.0-SNAPSHOT.jar`
21. 往下滚到 **"实例分组"** 或 **"部署 ECS"** → **只勾选 ECS-1**(另外两台不勾)
22. 找到 **"JVM 参数"** 输入框,填入(把 RDS 地址、密码换成你 Part 3.2 记的实际值):
    ```
    -Xms128m -Xmx512m -DENV=prod -DDB_HOST=rm-xxx.mysql.rds.aliyuncs.com:3306 -DDB_USER=userordertest -DDB_PASSWORD=你的RDS密码
    ```
23. 点底部 **"确认创建"**
24. **等 2-3 分钟**,应用状态变成 **"运行中"** 即成功

### 6.5 部署快递服务到 ECS-2

25. 重复 16-23 步,但是这次填:
    - **应用名称**:`expresstrack`
    - **部署包**:`coffee-expresstrack/provider/target/coffee-expresstrack-provider-1.0-SNAPSHOT.jar`
    - **部署 ECS**:**只勾 ECS-2**
    - **JVM 参数**:跟 userorder 完全一样(数据库名在配置文件里已硬编码为 `expresstracktest`,不用改)

### 6.6 部署网关到 ECS-3

26. 重复 16-23 步,这次填:
    - **应用名称**:`coffee-app`
    - **部署包**:`coffee-app/target/coffee-app-0.0.1-SNAPSHOT.jar`
    - **部署 ECS**:**只勾 ECS-3**
    - **JVM 参数**:`-Xms128m -Xmx256m -DENV=prod` (网关不连数据库,不需要 -DDB_*)

> **顺序建议**:先部署 userorder / expresstrack,最后部署 coffee-app。反了 coffee-app 启动会有几秒"找不到提供者"的警告,不影响最终成功。

### 6.7 验证服务治理页面有数据

27. 左侧 **"微服务治理 → Dubbo → 服务查询"**
28. 页面顶部 **"所属微服务空间"** 下拉选 `coffee-prod`
29. 应看到 `com.coffee.yun.userorder.api.OrderService` 等 Dubbo 接口出现在列表
30. 点击任一服务名 → **"提供者"** 标签 → 看到提供者的 ECS IP(就是 ECS-1 或 ECS-2 的私网 IP)

**这就是 PaaS 的核心价值**:服务调用关系一目了然,不用 SSH 上服务器看日志。

### 6.8 验证端到端业务

31. 浏览器访问 `http://<ECS-3 公网IP>:8005/actuator/health` → 应返回 `{"status":"UP"}`
32. 后面 Part 8 部署完前端,可以做完整的端到端业务验证

> **路径 A vs 路径 B 关键不同**:
> - 路径 A:你 SSH 上每台 ECS 自己装 JDK、scp、起进程
> - 路径 B:EDAS 网页统一调度——选 ECS 勾一下,EDAS 自动远程推 jar 并启动
> - 路径 B 还多了 **EDAS Agent 自动拦截 Dubbo 注册**,代码里写什么 Nacos 地址都会被改成 MSE,所以 JVM 参数里**不用写** `-DDUBBO_REGISTRY`

---

## Part 7 路径 C — Serverless 上云（SAE 托管）

这条路径你 **连 ECS 都不用买** ——把 jar 上传到 **SAE（Serverless 应用引擎）**，它自己开实例跑你的进程。

> ⚠️ **关于"缩容到 0"的说明（重要，别被误导）**：你创建 SAE 应用时，**"实例数"字段最小就是 `1`**（填不了 0，弹性伸缩里的"最小实例数"同样最小为 1）。所以本课程三个应用都至少有 **1 个常驻实例**。阿里云宣传的"缩容到 0"是 SAE 2.0 的**高级弹性能力**（需另配弹性策略、有版本/条件限制），按本章的创建流程**根本接触不到**，**本课程不使用**。别把"Serverless"等同于"缩到 0、账单 ¥0"——SAE 的真实价值是 **免运维 + 按 vCPU·秒计费（用多少付多少）+ 可按指标自动加实例**。（注："无请求自动缩 0、按调用次数计费"那套是**函数计算 FC** 的模型，和 SAE 不是一回事。）

**对照路径 A / B**：
- 路径 A：自己买 ECS、装 JDK、scp jar、起进程
- 路径 B：自己买 ECS，剩下交给 EDAS
- 路径 C：**连 ECS 都不要**，SAE 按需开"无服务器实例"

> 如果路径 A / B 还在跑，先停掉对应应用，避免 3 个服务在 3 个地方各自往 MSE Nacos 注册一份，业务调用变成"撞大运"路由。

### 7.1 SAE 是什么（一图看懂）

```
路径 B（EDAS）：           路径 C（SAE）：

你买 3 台 ECS               你不买 ECS
   │                          │
EDAS Agent ─► 跑你的 jar    SAE 调度 ─► 弹出一个"无服务器实例"跑你的 jar
                              │
                              └─► 没请求时整个实例销毁，按秒计费
```

**核心区别**：路径 B 的 ECS 是 **你的固定资产**，关机也要收费；路径 C 的"实例"是 **SAE 借给你的临时算力**，按 vCPU·秒 + GB·秒计费。

### 7.2 本项目上 SAE 的可行性提醒

不是所有微服务都适合 Serverless，先看清这张表再继续：

| 模块 | 适合 SAE 吗 | 原因 |
|------|-----------|------|
| **coffee-app**（HTTP 网关） | ✅ 很合适 | HTTP 请求触发、无状态，可随流量弹性扩容 |
| **coffee-userorder-provider**（Dubbo） | ⚠️ 能跑但反直觉 | Dubbo 是长连接 RPC，实例被弹性回收时正在调用的消费方会瞬间报错 |
| **coffee-expresstrack-provider**（Dubbo） | ⚠️ 同上 | 同上 |

**本节怎么做**：3 个服务都部署到 SAE 跑通，**实例数都用 1**（provider 必须常驻保住 Dubbo 注册；网关也保持 1 个）。生产里两个 provider 应继续用 EDAS / ECS。

### 7.3 关键前提：数据库与 Nacos 的内外网配置（最容易踩坑）

> 这一节是 **本章最重要的"内外网配置"专题**。先建立一个关键认知:**VPC 解决的是"网络能不能到达",白名单解决的是"到达了准不准进"——这是两层独立的关卡。** RDS / MSE 各自有一套**独立于 VPC 的访问白名单**:就算 SAE 和它们在同一个 VPC、网络本来就通,只要源 IP 不在白名单里,连接照样被拒(这跟你用 ECS 时要把 ECS 私网 IP 加进白名单是同一回事,不是 SAE 特有)。SAE 之所以在这里多踩一层,是因为它的实例 IP 从 虚拟交换机 网段**动态分配、还会随伸缩变化**,没有固定 IP 可填——所以要把整个 虚拟交换机 网段显式加进白名单。

#### 内外网的三条铁律

1. **永远用 RDS 内网地址**（`rm-xxx.mysql.rds.aliyuncs.com:3306`），**不要切公网**
   - 公网走的是公共出口，慢、收流量费、还要 RDS 申请外网地址
   - SAE 实例本来就在阿里云 VPC 里，走内网才合理

2. **RDS / MSE 的白名单独立于 VPC——"同一个 VPC"不等于自动放行**
   - SAE 实例的 IP 从它所在 **虚拟交换机（英文 VSwitch，VPC 控制台菜单叫"交换机"）网段动态分配**（弹性网卡 ENI，实例伸缩时 IP 还会变），没有固定 IP 可填
   - 所以即使和 RDS / MSE 同一个 VPC，也要把 **整个 虚拟交换机 网段** 显式加进 **RDS 白名单** 和 **MSE Nacos 的"内网白名单"**

3. **本地灌数据仍走公网**：第 05 章 / Part 3.2 你已经用 RDS 外网地址 + MySQL Workbench 灌过表结构。**这一节不需要再开外网**，只验证 SAE 实例从内网能连上即可。

#### 怎么知道 SAE 用的是哪个 虚拟交换机 网段

后面 **7.4 步创建 SAE 命名空间** 时会让你选 VPC 和 虚拟交换机——**选你 ECS 同 VPC 里那个空闲的 虚拟交换机 就行**。虚拟交换机 的网段（形如 `172.16.1.0/24`）在 VPC 控制台 → 交换机 列表能直接看到，**等下要把它复制到 RDS 和 MSE 的白名单**。

> 一句话总结：**地址用内网的，白名单加 SAE 网段的**。这一节后面每一步都围绕这两件事。

### 7.4 开通 SAE 并创建命名空间

#### 第 1 步：开通 SAE 服务

1. 浏览器打开阿里云控制台，顶部搜索框输入 `SAE`，点进 **"Serverless 应用引擎"**
2. 第一次进会让你勾选服务协议，**勾选并开通**（开通本身不收费，只在跑实例时计费）

#### 第 2 步：创建命名空间

3. 左侧 **"命名空间"** → 右上角 **"创建命名空间"**
4. 填：
   - **命名空间名称**：`coffee-prod-sae`
   - **地域**：跟你 ECS / RDS / MSE **同地域**（必须一致）
5. 点 **"确定"**

> ⚠️ **注册中心不在这一步选**：SAE 的命名空间只做环境隔离，**不绑定注册中心**。注册中心是在后面 **7.5 创建应用 → 高级设置 → 服务注册发现** 里**逐个应用**选的。这点和路径 B 的"EDAS 微服务空间创建时就绑定注册中心"**不一样**，别把 EDAS 的做法套到 SAE 上（这正是本节早前的一个表述错误）。

#### 第 3 步：在 RDS 和 MSE 白名单加上 SAE 的 虚拟交换机 网段（数据库内外网配置关键步骤）

6. 浏览器开新标签 → VPC 控制台 → 左侧 **"交换机"** → 找到你 ECS 所在 VPC 那几个 虚拟交换机
7. 任选一个 **可用 IP 数量够（≥ 64）** 的 虚拟交换机，把它的 **CIDR 网段**（形如 `172.16.1.0/24`）复制到记事本。后面 7.5 创建应用时也选这个 虚拟交换机。
8. **改 RDS 白名单**：
   - 回 RDS 控制台 → 你的实例 → 左侧 **"数据安全性 → 白名单设置"**
   - 点默认白名单分组的 **"修改"**
   - 在 IP 列表里 **追加** 第 7 步的 虚拟交换机 网段（用英文逗号和原来的 `172.16.0.0/16` 隔开）
   - 点 **"确定"**
9. **改 MSE 内网白名单**：
   - 回 MSE 控制台 → 实例详情 → 左侧 **"网络配置"**
   - 找到 **"内网白名单"** → **"修改"**
   - 追加同一个 虚拟交换机 网段，**"确定"**

> **路径 B 跳过这一步**是因为 EDAS 用的就是你自己的 ECS，私网 IP 已经在 `172.16.0.0/16` 里。**SAE 用的是 SAE 弹性 ENI，必须显式放行 虚拟交换机 网段，否则启动后报"无法连接到 MySQL / Nacos"**。这是本节最常踩的坑。

### 7.5 部署订单服务到 SAE

> 📍 **先记住：SAE 创建应用是「两步向导」——① 应用基本信息 → ② 高级设置。环境变量、服务注册发现都在第 ② 步「高级设置」里。** 很多人"找不到环境变量在哪"，就是因为在第 ① 步找——它不在第 ① 步。

#### 第 1 步：应用基本信息

10. SAE 控制台 → 左侧 **"应用列表"** → 右上角 **"创建应用"**
11. 第 ① 步 **"应用基本信息"** 填：
    - **应用名称**：`userorder-sae`
    - **命名空间**：选 `coffee-prod-sae`
    - **应用部署方式**：选 **`代码包部署`** → **JAR 包** → 上传 `coffee-userorder/provider/target/coffee-userorder-provider-1.0-SNAPSHOT.jar`（技术栈 Java / **OpenJDK 17**，须与本地打包 JDK 一致）
    - **资源规格**：`0.5 Core / 1 GB`（教学够用）· **实例数**：`1`
    - **专有网络 VPC / 交换机**：选 ECS 同 VPC，交换机选 **7.4 第 7 步那个已加白名单的 虚拟交换机**
12. 点 **"下一步：高级设置"**

#### 第 2 步：高级设置（环境变量 + 服务注册发现）★ 关键都在这一步

> 📍 **环境变量就在"高级设置"页最上方的"环境变量"区域**（第 ① 步没有这一项，所以容易找不到）。**给已经建好的应用改环境变量**：进 **应用详情 → "部署应用"（发布新版本）→ 高级设置 → 环境变量**，保存后会重新部署并重启应用（官方提示：发新版会重启，建议低峰期操作）。

13. 在 **"环境变量"** 区域点 **"+ 添加"** → 类型选 **"自定义"**。本项目**只需加一个**：

    | 变量名 | 值 | 说明 |
    |--------|----|----|
    | `ENV` | `prod` | 让 `application-prod.yml` 生效（`spring.profiles.active=${ENV:dev}` 默认是 dev，不设就走 dev） |

    > ✅ **DB 地址 / 账号 / 密码为什么不用在这里加**：本项目 `application-prod.yml` 用 `${DB_HOST:默认值}` 写法——**打包前你已经把真实 RDS 内网地址、账号、密码填进了这些"默认值"**（见 Part 4 / [01 章方式一](01-aliyun-guide.md)），jar 里已自带，SAE 上**不必再设** `DB_HOST/DB_USER/DB_PASSWORD`。
    > ⚠️ 前提：**打包前**务必把 `application-prod.yml` 里 `DB_HOST` 的默认值改成你的 **RDS 内网地址**（带 `:3306`），别用外网地址。
    > （可选）若你不想改 prod.yml，也可以反过来在这里用环境变量 `DB_HOST/DB_USER/DB_PASSWORD` 覆盖默认值——二选一即可。

14. 同在"高级设置"里找到 **"服务注册发现 → Nacos 注册中心服务发现"** → 选 **`MSE Nacos 专业版`** → 选 Part 3 创建的那个**专业版** MSE 实例。
    > **千万不要用默认的"SAE 内置 Nacos"**——本项目是 Dubbo 3.x，SAE 内置 Nacos 的 Client 版本与 Dubbo 3.x **不兼容**，服务注册/发现会失败（这也是另设 Dubbo 2.x 降级分支才用内置 Nacos 的原因）。`userorder-sae`、`expresstrack-sae`、`coffee-app-sae` 三个应用创建时**都要选 MSE Nacos 专业版**，它们才注册到同一个 Nacos、Dubbo 才能互相发现。
    > ✅ **`DUBBO_REGISTRY` 不用设**：控制台在这一项下方有官方提示——"**若选择了某个具体的 MSE Nacos 实例，则无需在程序中配置注册中心地址，系统会自动帮您注册到对应的 MSE Nacos 注册中心**"（若不选实例才需要自己在程序里配地址）。另外本项目打包前 `application-prod.yml` 里 `DUBBO_REGISTRY` 的默认值也写了 MSE 内网地址（和 DB 地址同理，见上一条），双保险。

15. 同在"高级设置"里找到 **"启动命令 / JVM 参数"**，填：
    ```
    -Xms256m -Xmx512m
    ```

16. 滚到底，**"确认创建"** → 等约 2–3 分钟，应用列表里 `userorder-sae` 状态变 **"运行中"**，说明实例起来了

#### 第 3 步：看日志确认连上 RDS 和 Nacos

17. 点进 `userorder-sae` 详情 → **"实例部署信息" → "实时日志"**
18. 滚到日志最底部，应能看到：
    - `HikariPool-1 - Start completed.` → 数据库连上了
    - `Nacos service registry started` 或 `register dubbo service` → Nacos 注册成功
19. 如果看到 `Communications link failure` / `Connection refused`，**99% 是 7.4 第 8 步 RDS 白名单没加 SAE 虚拟交换机 网段**，回去补

### 7.6 部署快递服务和网关

#### 快递服务（expresstrack）

20. 重复 7.5 的全部步骤，**只改 4 处**：
    - 应用名称：`expresstrack-sae`
    - jar 包：`coffee-expresstrack/provider/target/coffee-expresstrack-provider-1.0-SNAPSHOT.jar`
    - 环境变量 **不需要改**（`DB_HOST` 仍是同一个 RDS 内网地址，数据库名 `expresstracktest` 已硬编码在 `application-prod.yml`，用同一个账号即可——Part 3.2 已经给账号授权了这两个库）
    - 实例数：`1`

#### 网关（coffee-app）

21. 再创建一个应用，关键差异：
    - 应用名称：`coffee-app-sae`
    - jar 包：`coffee-app/target/coffee-app-0.0.1-SNAPSHOT.jar`
    - 环境变量 **只填 `ENV=prod`**（网关不连数据库）
    - **实例数**：`1`（和 provider 一样常驻）
    - JVM 参数：`-Xms256m -Xmx512m`

22. （可选）部署成功后，可给网关**配指标弹性，让它按流量自动加实例**：
    - 应用详情 → 左侧 **"应用扩缩 → 弹性伸缩"** → 添加**指标弹性**策略
    - **最小实例数**：`1`（最小就是 1，填不了 0）
    - **最大实例数**：`3`
    - **触发指标**：CPU 使用率 > 60%（教学默认）→ **保存**

> 这样网关在流量高时会自动加到 3 个实例、回落时缩回 1 个，但**始终至少保留 1 个实例**。SAE 的省钱点在于"按 vCPU·秒计费、用多少付多少 + 高峰才扩容"，**不是缩到 0**——创建和弹性配置里实例数最小都是 1。两个 provider 保持 1 个常驻实例不配缩容，避免 Dubbo 注册中断。

### 7.7 暴露公网入口（公网 CLB / 公网访问）

> **术语说明**：负载均衡产品已更名——**SAE 2.0（现行版）控制台显示的是"CLB"**（传统型负载均衡 Classic Load Balancer），只有 SAE 1.0 经典版旧界面才叫"SLB"。本文统一用 CLB；你若在老界面看到"SLB"，是同一个东西。

SAE 应用默认 **没有公网 IP**，路径 A 的"ECS 公网 IP + 端口"在这里不存在。注意：前端静态页是靠 **ECS-3 的公网 IP** 对外的，真正"困在 VPC 里没有公网地址、又要被浏览器直接调用"的是 **coffee-app**，所以公网入口加在 coffee-app，而不是前端。

23. 进 `coffee-app-sae` 详情 → **"基础信息"** 页的 **"应用访问设置"** 区域（官方路径：应用详情页 → 基础信息 → 应用访问设置）
24. 在 **"公网访问地址"** 那一行 → 点 **"添加公网 CLB 访问"**（控制台按钮原文）
25. 弹窗里：
    - **CLB 实例**：`新建`（让 SAE 自动代购一个公网 CLB）
    - **协议**：`HTTP`
    - **CLB 端口**：`80`（用户浏览器访问）
    - **容器端口**：`8005`（coffee-app 自身监听的端口）
26. **"确定"** → 等约 1 分钟，会返回一个 **公网 IP / 域名**，**记到记事本**——这就是替代"ECS-3 公网 IP"的新入口。

> provider 不需要做这一步——它们只通过 Dubbo 给 coffee-app 内部调用，不暴露公网。
>
> ⚠️ 这个公网 CLB 是 **会持续计费的关联资源**，按实例规格/带宽单独计费（和应用的 vCPU·秒分开算）。

### 7.8 验证

27. 浏览器访问 `http://<7.7 第 26 步的公网 IP>/actuator/health` → 应返回 `{"status":"UP"}`
28. 回 MSE Nacos 控制台 → 服务列表 → 应能看到：
    - `com.coffee.yun.userorder.api.OrderService`（来自 `userorder-sae`）
    - `com.coffee.yun.expresstrack.api.ExpressTrackService`（来自 `expresstrack-sae`）
    - `coffee-app`（HTTP 服务，来自 `coffee-app-sae`）
29. 这一节业务级验证（下单、查快递）等 **Part 8** 部署完前端一起做——**路径 C 的前端同样部署在 ECS-3 的 Nginx 上**（与路径 A/B 完全相同的做法），只是构建时把 API 地址指向本节第 26 步的 **SAE 公网 CLB 域名**，而不是 `ECS-3:8005`。详见 Part 8.1。

### 7.9 SAE 上线后的"Serverless 思维"小结

- **没有 ECS** → 没有 "上去看日志"，看日志去 SAE 控制台 → 应用 → 实时日志
- **没有 manage.sh** → 重启就在控制台点 **"重启应用"**；发新版本就在 **"应用部署"** 里上传新 jar
- **数据库连接**：内网地址 + SAE 虚拟交换机 加白名单 = 唯一正确解；外网地址永远不要在这里用
- **冷启动**：新部署/重启后首次访问要等 5–15 秒（SAE 现拉镜像、启 JVM）——演示前先 `curl` 预热一次
- **账单**：按实例运行的 vCPU·秒 + GB·秒计（用多少付多少）。可以在 SAE 控制台 → **"账单中心"** 看到逐分钟的费用

> **3 条路径选哪个**：
> - 学技术、看底层 → **路径 A**
> - 企业生产、要服务治理 → **路径 B**
> - 流量稀疏、想压成本 → **路径 C**
>
> 本课程推荐：**A 跑一遍理解原理 → B 跑一遍理解 PaaS → C 跑一遍理解 Serverless 弹性**，三条路径都走过才算真的懂"云原生"。

---

## Part 8 部署前端到 ECS-3(三条路径都用)

前端只部署一份,放在 **ECS-3**,由 Nginx 静态托管。**三条路径都用这一套**——前端是几十个静态文件,放哪条路径都一样,留一台最小规格 ECS 跑 Nginx 即可,不必为路径 C 再引入 OSS 静态托管那套额外知识。

> **三条路径的唯一差别在"前端把 API 发到哪"**：
> - **路径 A / B**:ECS-3 上同时跑着 coffee-app 网关,API 地址就是 `ECS-3 公网 IP:8005`
> - **路径 C(SAE)**:coffee-app 跑在 SAE 上,API 地址改成 Part 7.7 的 **SAE 公网 CLB 域名**
>
> 除了这一个地址,构建、压缩、上传、Nginx 部署的每一步对三条路径**完全一致**。

**对照本地(第 05 章)**:本地用 `npm run dev` 起开发服务器,**云上换成 `npm run build` 出静态文件 → Nginx 托管**。Vue 源码一行不改,只在构建时告诉它"用户浏览器要把 API 发到哪个地址"。

### 8.0 镜像 / Web 服务器怎么选（先读这段）

前端就是一堆静态文件，得有个 **Web 服务器（Nginx）** 把它对外吐出去。把 Nginx 弄到 ECS-3 上有两条路，**本课程推荐第一条**：

| 方式 | 你要做什么 | 适合谁 | 敲命令吗 |
|------|----------|--------|:---:|
| **A. 预装镜像 + 宝塔面板（推荐）** | 买 ECS-3 时在 **镜像市场** 选预装「宝塔 Linux 面板」的镜像 → Nginx 已经装好，建站点、传文件、配置全在 **网页面板里点鼠标** | **初学者**、想快点跑通看到效果 | 几乎不用 |
| **B. 裸镜像 + 命令行装 Nginx（进阶）** | 选裸 `Ubuntu` / `Alibaba Cloud Linux` → 在会话管理终端 `apt/dnf install nginx` 自己装 | 想理解 Linux、练命令行 | 要 |

> **为什么推荐 A？** 这门课的原则是 **"能在阿里云控制台点鼠标生成的，就别让你去敲命令搭"**。宝塔面板把"装 Web 服务器、上传网站、配置站点"做成了网页上的按钮，初学者不必记 `systemctl` / `nginx -t` 这些命令，出错也少。**只有当你明确想练 Linux 底层（和路径 A 的精神一致）时，才走 B。**
>
> **为什么不用 OSS？** 阿里云 OSS 静态网站托管确实也是"纯点鼠标"，但它会牵出 Bucket 读写权限、静态页 404 兜底等额外概念。用 ECS-3 + Nginx 托管前端，直接复用 06 章 Part 8 已讲过的 Nginx 部署即可，认知负担最低——所以本课程统一用 ECS-3，不绕 OSS。
>
> （**跨域说明**：前端页面在 ECS-3，后端 API 在 `ECS-3:8005`（路径 A/B）或 SAE 公网 CLB 域名（路径 C），端口/域名不同，**本就是跨域**，这跟用不用 OSS 无关。本项目由网关 coffee-app 的 `@CrossOrigin` 统一放行，前端无需额外配置。）

> 下面 8.1（本地构建）、8.2（压缩）两步 **两种方式都要做**，是一样的；从 8.3 开始分 **方式 A（宝塔，鼠标）** 和 **方式 B（命令行）** 两条，按你买 ECS-3 时选的镜像走对应那条即可。

### 8.1 在本地电脑构建前端(并告诉它后端地址)

构建时要先设一个环境变量 `VUE_APP_BASE_URL`,这是后端 API 的地址。**这个地址会被永久写进打包后的 JS 文件**,不设就默认 `localhost:8005`(用户浏览器会找自己电脑,失败)。**地址按你的路径填**:

- **路径 A / B**:`http://<ECS-3 公网 IP>:8005`
- **路径 C(SAE)**:`http://<SAE 公网 CLB 域名>`(Part 7.7 第 26 步记下的,无端口号,CLB 默认 80)

1. 用 VSCode 终端进项目根目录的 `app-admin/`:
   ```bash
   cd app-admin
   ```
2. 下面以 **路径 A / B** 的 `http://x.x.x.x:8005` 为例(把 `x.x.x.x` 换成你 **ECS-3 的公网 IP**);**路径 C** 则把整个 `http://x.x.x.x:8005` 换成你的 **SAE 公网 CLB 域名**(无端口号)。

   **Windows CMD**:
   ```cmd
   set VUE_APP_BASE_URL=http://x.x.x.x:8005
   npm run build
   ```
   **Windows PowerShell**:
   ```powershell
   $env:VUE_APP_BASE_URL="http://x.x.x.x:8005"
   npm run build
   ```
   **Mac/Linux**:
   ```bash
   export VUE_APP_BASE_URL=http://x.x.x.x:8005
   npm run build
   ```
3. 等约 1-2 分钟,看到 `Build complete. The dist directory is ready to be deployed.` 即成功
4. `app-admin/` 下会生成一个 `dist/` 文件夹(里面是 index.html + static/)

### 8.2 把 dist 文件夹压缩成 zip

`dist/` 里有几十个文件,一个个上传太麻烦,压成 zip 一次传一个文件最稳。

5. 文件管理器打开 `cloudnativeapp/app-admin/`
6. **右键点 `dist` 文件夹** → 选 "发送到压缩文件夹"(Windows) 或 "压缩 dist"(Mac)
7. 会得到 `dist.zip`,记住它的位置

### 8.3 方式 A：用宝塔面板部署前端（鼠标，推荐）

> 适用于 Part 3.1 给 ECS-3 选了 **预装宝塔面板镜像** 的同学。全程在网页面板点鼠标，不敲命令。**选了裸镜像的同学跳到 8.4。**

> 📌 **关于具体按钮位置**：宝塔面板版本更新较快，下面只用一句话写清"每一步要做什么"，具体按钮以你面板里看到的为准，并配 📷 截图占位 + 🔗 官方文档链接，按官方截图对照即可。

8. **打开宝塔面板**：ECS-3 实例详情页（或镜像市场该镜像的说明里）会给出 **面板地址、初始账号密码**；浏览器打开、登录，按提示完成首次绑定。

   > 📷 截图占位：宝塔面板登录后的首页
   > 🔗 官方文档参考：[宝塔 Linux 面板 - 快速上手](https://www.bt.cn/new/index.html)

9. **新建一个站点**：面板左侧 **「网站」→「添加站点」**，**域名** 先填 `ECS-3 的公网 IP`，**根目录** 用默认（形如 `/www/wwwroot/<你的IP>`），其余默认，提交。

   > 📷 截图占位：宝塔"添加站点"弹窗

10. **上传并解压前端**：进该站点的 **「文件」/ 根目录** → 点 **上传** 选 8.2 生成的 `dist.zip` → 上传完 **右键「解压」** → 把解压出来的 `index.html`、`static/` 等 **放到站点根目录下**（若解压出的是一层 `dist/` 文件夹，把里面的内容移到根目录）。

    > 📷 截图占位：宝塔文件管理上传 / 解压

11. **开启 SPA 刷新兼容（伪静态）**：进该站点 **「设置」→「伪静态」**，选 **Vue / history 模式**（等价于本项目 `coffee-admin.conf` 里的 `try_files $uri $uri/ /index.html;`）。不设的话，刷新子页面会 404。

    > 📷 截图占位：宝塔站点"伪静态"设置
    > 🔗 官方文档参考：[宝塔面板 - 伪静态规则](https://www.bt.cn/new/index.html)

12. 宝塔镜像里 Nginx 已默认启动并监听 80，**到这一步前端就部署好了**。跳到 **8.5 放开 80 端口**，再 8.6 验证。

### 8.4 方式 B：命令行部署前端（进阶，裸镜像同学走这条）

> 适用于 ECS-3 选了裸 `Ubuntu` / `Alibaba Cloud Linux` 的同学。**用了宝塔的同学不用做这一节。**

#### 第 1 步：把 zip 和 Nginx 配置上传到 ECS-3

8. 浏览器进 **ECS-3 的实例详情页** → 找到 **"上传/下载文件"**(和 Part 5.4 同一个入口;会话管理终端里没有上传按钮)
9. **第 1 次上传 dist.zip**:
   - **本地文件**:刚生成的 `app-admin/dist.zip`
   - **目标路径**:`/root/`
   - 提交,等状态变"成功"(几秒到几十秒)
10. **第 2 次上传 Nginx 配置文件**:再点 上传文件:
    - **本地文件**:`cloudnativeapp/deploy/coffee-admin.conf`(项目里已准备好,**不用改**)
    - **目标路径**:`/root/`

#### 第 2 步：在 ECS-3 终端粘一整段命令安装 + 启动 Nginx

11. 在 ECS-3 的会话管理终端里 **整段粘贴下面这段** 并回车。**按你买 ECS 时选的操作系统挑一段**（两段只有第一行"装包命令"不同）:

    **Ubuntu 22.04（用 apt）**:
    ```bash
    apt update && apt install -y nginx unzip && \
    mkdir -p /var/www/coffee-admin && \
    unzip -o /root/dist.zip -d /var/www/coffee-admin && \
    ([ -d /var/www/coffee-admin/dist ] && mv /var/www/coffee-admin/dist/* /var/www/coffee-admin/ && rmdir /var/www/coffee-admin/dist || true) && \
    cp /root/coffee-admin.conf /etc/nginx/conf.d/coffee-admin.conf && \
    nginx -t && \
    systemctl start nginx && \
    systemctl enable nginx && \
    echo "✅ 前端部署完成"
    ```

    **Alibaba Cloud Linux 3 / CentOS（用 dnf）**:
    ```bash
    dnf install -y nginx unzip && \
    mkdir -p /var/www/coffee-admin && \
    unzip -o /root/dist.zip -d /var/www/coffee-admin && \
    ([ -d /var/www/coffee-admin/dist ] && mv /var/www/coffee-admin/dist/* /var/www/coffee-admin/ && rmdir /var/www/coffee-admin/dist || true) && \
    cp /root/coffee-admin.conf /etc/nginx/conf.d/coffee-admin.conf && \
    nginx -t && \
    systemctl start nginx && \
    systemctl enable nginx && \
    echo "✅ 前端部署完成"
    ```
12. **等约 30 秒**,看到最后一行 `✅ 前端部署完成` 即成功

> 这段命令做了 4 件事:装 nginx、解压前端到 Nginx 目录、放置配置文件、启动 Nginx。固定模板,不懂细节没关系,粘一次就好。
>
> **两段为什么只差第一行？** 裸镜像的 ECS 都 **不预装 Nginx**——区别只在用哪个包管理器装：Ubuntu 用 `apt`，Alibaba Cloud Linux / CentOS 用 `dnf`（老版本是 `yum`，命令通用）。装完之后配置目录 `/etc/nginx/conf.d/`、`systemctl` 启动方式两系统完全一致，所以后面几行不用改。**这一整节也正说明了 8.0 推荐宝塔的原因——这些命令初学者很容易抄错。**

### 8.5 在网页 GUI 放开 ECS-3 的 80 端口

> 如果 Part 5.6 已经加过 80 / 8005,跳过本节。

13. 浏览器 ECS 控制台 → ECS-3 → 安全组 → 入方向 → **手动添加**
14. 加规则(授权对象都填 `0.0.0.0/0`):
    - 协议 TCP / 端口 `80/80`(Nginx 前端,三条路径都要开)
    - 协议 TCP / 端口 `8005/8005`(**仅路径 A / B 需要**——coffee-app 跑在 ECS-3 上;路径 C 的 coffee-app 在 SAE 上,ECS-3 不需要开 8005)

> **用宝塔的同学**：访问宝塔面板还需放通 **面板端口**（默认 `8888`，以你面板登录地址里的端口为准）。很多镜像市场的宝塔镜像在创建时会引导你放行，没放的话照上面同样的方式加一条对应端口的入方向规则即可。

### 8.6 验证

15. 浏览器打开 `http://<ECS-3 公网IP>`
16. **应看到 CoffeeTrack 登录页** ✅

> ECS-1 / ECS-2 不需要开公网端口,它们只在 VPC 内部通过 Dubbo RPC 被 ECS-3 上的 coffee-app 调用——**只有入口节点 ECS-3 暴露公网**,其他节点藏在内网,这正是微服务的安全分层。

---

## Part 9 验证

### 9.1 业务端到端验证(三条路径都用)

把整条链路从前端走一遍,跑通就说明上云成功。**操作步骤三条路径完全一样**,只是背后的调用链不同(见每步下方说明)。

1. 浏览器打开 `http://<ECS-3 公网IP>`
2. 看到 CoffeeTrack 登录页 → 用默认账号登录
3. 进入主页 → 点 **"订单管理 → 订单列表"** → 应能看到订单数据
   - **路径 A / B 链路**:浏览器 → ECS-3 的 coffee-app → ECS-1 的 userorder → RDS
   - **路径 C 链路**:浏览器 → **SAE 的 coffee-app**(经公网 CLB) → **SAE 的 userorder**(Dubbo) → RDS
4. 点 **"轨迹查询"** → 输入运单号 `44556677` → 点查询 → 应能查出快递轨迹
   - **路径 A / B**:浏览器 → ECS-3 → **Dubbo RPC** → ECS-2 → RDS
   - **路径 C**:浏览器 → SAE coffee-app → **Dubbo RPC** → SAE expresstrack → RDS

> 第 4 步能成功是上云最关键的验证点——说明三个服务通过 MSE Nacos 互相发现、Dubbo RPC 跨实例调用正常(路径 A/B 是跨 ECS,路径 C 是跨 SAE 实例)。

### 9.2 服务治理验证

#### 路径 A(IaaS)的查法

5. 浏览器回 MSE Nacos 实例详情 → 左侧 **"服务管理 → 服务列表"**
6. 列表里应能看到 `coffee-userorder`、`coffee-expresstrack` 等服务
7. 点任一服务能看到提供者的 IP(就是 ECS-1 / ECS-2 的私网 IP)

#### 路径 B(PaaS)的查法

5. 浏览器回 EDAS 控制台 → **"微服务治理 → Dubbo → 服务查询"**
6. 顶部"所属微服务空间"选 `coffee-prod`
7. 列表能看到 Dubbo 接口,点进去看到提供者 ECS IP
8. **额外福利**:左侧"应用监控"还能看到调用拓扑图、调用链 —— 这是 PaaS 的核心价值

### 9.3 整体架构回顾

#### 路径 A / B（ECS / EDAS）的拓扑

3 个服务跑在 3 台 ECS 上,前端和 coffee-app 网关都在 ECS-3:

```
                          用户浏览器
                              │
                              │  http://<ECS-3 公网IP>:80
                              ▼
              ┌───────────────────────────────────┐
              │  ECS-3                            │
              │   ├─ Nginx :80 → 返回 Vue 静态文件 │
              │   └─ coffee-app :8005             │
              └────────────┬──────────────────────┘
                           │ Dubbo RPC(VPC 内网)
              ┌────────────┴────────────┐
              ▼                         ▼
       ┌──────────────┐         ┌──────────────────┐
       │ ECS-1        │         │ ECS-2            │
       │ userorder    │         │ expresstrack     │
       │  :7001       │         │  :8001           │
       └──────┬───────┘         └──────┬───────────┘
              │ JDBC(VPC 内网)         │ JDBC
              └─────────┬───────────────┘
                        ▼
                  ┌──────────┐
                  │  RDS     │
                  │  MySQL   │
                  └──────────┘

         3 台 ECS 都注册到同一个 MSE Nacos 互相发现
                  ┌──────────────┐
                  │ MSE Nacos    │
                  └──────────────┘
```

#### 路径 C（SAE）的拓扑

3 个服务全跑在 SAE 无服务器实例上,**ECS-3 只剩 Nginx 托管前端**(coffee-app 移到了 SAE):

```
                          用户浏览器
                    │                     │
   ① 取前端 http://<ECS-3 IP>:80          │ ③ 前端 JS 把 API 打到 SAE CLB
                    ▼                     ▼
        ┌────────────────────┐   ┌──────────────────────────┐
        │  ECS-3             │   │  SAE 公网 CLB            │
        │   └─ Nginx :80     │   │  (coffee-app 的对外入口) │
        │      返回 Vue 静态   │   └────────────┬─────────────┘
        └────────────────────┘                │
                                                ▼
                                  ┌──────────────────────────┐
                                  │ SAE: coffee-app-sae       │
                                  │ (HTTP 网关,实例数 1)      │
                                  └────────────┬──────────────┘
                                               │ Dubbo RPC(VPC 内网)
                                  ┌────────────┴────────────┐
                                  ▼                         ▼
                       ┌──────────────────┐     ┌──────────────────────┐
                       │ SAE: userorder-sae│     │ SAE: expresstrack-sae │
                       │ (常驻 1 实例)      │     │ (常驻 1 实例)         │
                       └────────┬──────────┘     └──────────┬────────────┘
                                │ JDBC(VPC 内网)            │ JDBC
                                └─────────────┬──────────────┘
                                              ▼
                                        ┌──────────┐
                                        │  RDS     │
                                        │  MySQL   │
                                        └──────────┘

              3 个 SAE 应用都注册到同一个 MSE Nacos 互相发现
                            ┌──────────────┐
                            │ MSE Nacos    │
                            └──────────────┘
```

> **两张图对比着看**:换路径时 **MySQL / Nacos / Dubbo 调用关系 / 前端在 ECS-3** 这几样完全不变,变的只是"3 个服务跑在哪"(3 台 ECS ↔ SAE 实例)和"coffee-app 的对外入口"(ECS-3:8005 ↔ SAE CLB)。这正是云原生"同一份 jar、换个地方跑"的直观体现。

| 维度 | 本地(第 05 章) | 云上·路径 A/B | 云上·路径 C(SAE) |
|------|--------------|--------------|------------------|
| 进程数 | 3 个 | 3 个(一样) | 3 个(一样) |
| 跑在哪 | 1 台笔记本 | **3 台 ECS** | **SAE 无服务器实例** |
| 前端托管 | npm dev | ECS-3 Nginx | ECS-3 Nginx(同左) |
| MySQL | 本地 3307 | RDS 内网 3306 | RDS 内网 3306 |
| Nacos | 本地 8848 | MSE Nacos 内网 8848 | MSE Nacos 内网 8848 |
| 用户访问站点 | localhost:8080 | ECS-3 公网 IP | ECS-3 公网 IP(同左) |
| 后端 API 入口 | localhost:8005 | ECS-3:8005 | SAE 公网 CLB |
| 代码改了几行 | / | **0 行** | **0 行** |

---

## Part 10 ⭐ 推荐：用云效流水线自动构建发布（点击式）

> **这才是本课程推荐的"正式"上云方式。** 本章前面手工"敲命令打 jar → 上传 → 启动"是为了让你**理解每一步在干什么**；真正日常使用时，**一律走云效流水线**——`git push` 之后在网页上点几下，构建、发布全自动，不用再手敲 `mvn` / scp / 重启命令。这与本课程"能点鼠标就不敲键盘"的主线一致。
>
> 已扩展为独立章节，覆盖 **3 条后端路径 + 1 条前端流水线**（后端 EDAS / SAE / ECS + 前端统一用 ECS-3 Nginx）的完整自动化。
>
> 👉 移步 **[第 12 章：CI/CD 流水线自动化（基于云效 Flow）](07-cicd-pipeline.md)**。
>
> **建议**：理解了本章手工流程后，**正式部署就别再手敲了**，直接照第 12 章把流程交给流水线。

---

## 附录 A：三条路径对比速查表

| 维度 | 路径 A：IaaS（ECS）🔧进阶 | 路径 B：PaaS（EDAS）⭐推荐 | 路径 C：Serverless（SAE） |
|------|-------------------|---------------------|----------------------------|
| **操作方式** | 终端敲命令 | **控制台点鼠标** | **控制台点鼠标** |
| **本课程定位** | 进阶 / 选读 | **推荐主线** | 推荐（弹性场景） |
| **谁装 JDK** | 你 | EDAS（指定版本即可） | 平台 |
| **谁启动进程** | 你（manage.sh / systemd） | EDAS Agent | 平台（按请求触发） |
| **谁挂了谁重启** | 你（写脚本守护） | EDAS（自动重启） | 平台（按需新建实例） |
| **Dubbo 注册地址** | 启动参数显式指定 | Agent 自动接管 | 通常不用 Dubbo |
| **服务治理可视化** | ❌（自己看日志 / MSE 控制台） | ✅ EDAS 治理页面 | 有限 |
| **扩容方式** | 手动多台 scp | 控制台勾选更多 ECS | 自动 |
| **按用量计费（vCPU·秒）** | ❌（ECS 包月） | ❌（ECS 包月） | ✅ |
| **成本** | 最低（仅 ECS） | ECS + EDAS（节点费） | 按 vCPU·秒 + GB·秒 |
| **配置传参风格** | `--key=value` 命令行参数 + 外部 YAML | `-DKEY=VALUE` JVM 参数 + prod profile | 环境变量 |
| **学习深度** | 高（理解底层） | 中（关注业务） | 低（无服务器思维） |
| **适合场景** | 学习、成本敏感、定制需求多 | **企业生产 + 服务治理** | 流量稀疏、突发、任务型 |

---

## 附录 B：常见问题排查（按路径分组）

### 通用问题（不分路径）

**Q：本地能连 RDS，ECS 上连不上**
- 检查 ECS 私网 IP 是否在 RDS 白名单（RDS → 数据安全性 → 白名单设置）
- 检查 ECS 和 RDS 是否同一 VPC
- 内网地址不要带 `jdbc:mysql://` 前缀，只填 `rm-xxx:3306`

**Q：MSE Nacos 内网连不上**
- ECS 和 MSE 不在同一 VPC（最常见）
- MSE 实例白名单没放通 ECS 私网 IP（MSE → 实例详情 → 网络配置 → 白名单）

**Q：前端能打开但接口全失败**
- F12 → Network 看请求地址，若是 `localhost:8005` → Part 8.2 重新构建，设好 `VUE_APP_BASE_URL`
- 若请求的是正确 IP 但返回失败 → 检查 ECS 入方向是否放通 8005

### 路径 A 特有问题

**Q：`manage.sh start` 30 秒后报"未监听到端口"**
- `tail -100 ~/coffee/logs/xxx.log` 看真实错误
- 最常见：数据库密码不对 / Nacos 地址不对 / 端口被另一个进程占用（`ss -tlnp | grep 7001`）

**Q：服务启动了但 MSE Nacos 看不到注册**
- 确认 `manage.sh` 里 `NACOS_ADDR` 改成了你自己的地址（不是模板的 `mse-xxxxxxxx`）
- 看日志里是否有 `Failed to connect to nacos`

**Q：怎么知道服务真在跑**
- 在对应 ECS 上：`~/coffee/manage.sh status`
- 在对应 ECS 上：`curl http://localhost:7001/actuator/health`（端口换成本机服务的端口）

**Q：`manage.sh status` 显示某些服务"未部署到本机"是不是错了？**

不是错。多 ECS 部署时，**这是正常显示**：
- ECS-1 上只部署了 userorder，所以 expresstrack 和 app 都显示"未部署到本机"
- ECS-2 上只部署了 expresstrack，所以 userorder 和 app 都显示"未部署到本机"
- 同理 ECS-3

只要 **本机部署的那个服务显示 [✓] 运行中** 就 OK。要看全局状态，分别 SSH 到 3 台 ECS 各执行一次 `manage.sh status`。

### 路径 B 特有问题

**Q：EDAS 服务查询页面显示"没有数据"**

按顺序检查：
1. 页面顶部 **"所属微服务空间"** 是否选的 `coffee-prod`（不是默认的 `cn-hangzhou`）
2. **集群所属空间**：EDAS ECS 集群详情 → "微服务空间"字段是否 `coffee-prod`（若是 `cn-hangzhou`，需删集群重建）
3. **JVM 参数** 是否包含 `-DENV=prod`
4. **ECS 安全组出方向** 是否被收紧过（默认全放行就没问题；EDAS 自动添加的安全组规则是否被误删）
5. **等一等**：首次部署后元数据上报有约 30 秒延迟，刷新再试

**Q：应用日志报 `Unable to connect to Nacos`**

MSE Nacos 实例与 ECS 不在同一 VPC，内网不通。检查两者 VPC 是否一致。

> 注意区分：如果报错来自 **配置中心**（`spring.config.import`）且服务仍能正常启动运行，那是 Part 2.4 说的"配置中心未接管"现象，对核心功能无影响，可忽略；只有 **Dubbo 注册** 连不上才会导致服务调用失败。

**Q：导入 ECS 时列表里找不到我的 ECS**

ECS 必须与 EDAS 集群在 **同一地域、同一 VPC** 才会出现。

**Q：集群创建后发现微服务空间选错了**

集群的微服务空间不可改，需：① 删掉集群内所有应用 → ② 删集群 → ③ 重建集群并选对空间（`coffee-prod`）。

**Q：路径 A 和路径 B 切换时**

- A → B：先 `manage.sh stop` 停掉所有进程，避免端口冲突，再去 EDAS 部署
- B → A：先在 EDAS 把三个应用停掉（不要删，便于切回去），再 `manage.sh start`

### 路径 C 特有问题

**Q：SAE 应用启动报 `Communications link failure` 连不上 RDS**

99% 是 **RDS 白名单没放 SAE 虚拟交换机 网段**。SAE 实例的 IP 来自 SAE 弹性 ENI，不在 `172.16.0.0/16` 这条默认白名单里。回 Part 7.4 第 8 步，把 SAE 应用所在 虚拟交换机 的 CIDR（VPC 控制台 → 交换机里查）加到 RDS 白名单。

**Q：SAE 应用启动报 `Failed to connect to Nacos`**

同上的根本原因——**MSE 内网白名单没放 SAE 虚拟交换机 网段**。补上 Part 7.4 第 9 步即可。

**Q：能不能让 SAE 应用连 RDS 的外网地址绕过白名单？**

**强烈不建议**：① 外网走公共出口，慢且按流量计费；② RDS 还要先"申请外网地址"；③ 安全暴露面更大。**正确做法永远是：用内网地址 + 加 虚拟交换机 白名单**——这是路径 C 的"数据库内外网配置"标准答案。

**Q：coffee-app-sae 新部署/重启后，第一次访问要等好几秒**

这是 Serverless 的 **冷启动**——SAE 新实例启动要现拉镜像、启 JVM、跑 Spring Boot 初始化，整个过程 5–15 秒正常。教学演示前先 `curl` 一次预热即可。

**Q：能不能给 provider 配按指标自动缩容（减少实例）？**

不建议：Dubbo 是长连接 RPC，provider 实例被弹性策略回收时，consumer（coffee-app）会瞬间报 `RpcException: No provider available`，直到 Nacos 通知新实例并重连。**本课程 provider 固定 1 个常驻实例，不配缩容策略**。（顺带一提：创建/弹性配置里实例数最小都是 1，本就不存在"缩到 0"。）

**Q：路径 B 和路径 C 切换时**

- B → C：先在 EDAS 把 3 个应用停掉，再到 SAE 部署。两边都在跑会导致同一服务名在 Nacos 上注册两套提供者，调用方按权重路由结果不可预测。
- C → B：先在 SAE 把 3 个应用停掉（点"停止应用"，不删），再回 EDAS 启动。

---

[← 返回主文档](../README.md) | [快速启动指南（本地）](05-quick-start.md)
