# 第 11 章：把微服务部署到阿里云（多路径上云）

> **本章目标**：把第 05 章在本地跑通的整套项目（三个后端服务 + Vue 前端 + MySQL + Nacos），原封不动地搬到阿里云上运行。
>
> **本章特色**：阿里云不是只有一种上云方式。本章按"运维负担从重到轻"介绍 **多条上云路径**，让你看清不同方式的差异,再根据项目体量选择合适的方案。
>
> | 路径 | 形态 | 你管什么 | 阿里云管什么 | 本章状态 |
> |------|------|---------|------------|---------|
> | **A**：ECS 自管 | IaaS | 操作系统、JDK、进程、日志 | 服务器硬件、网络 | ✅ 已就绪 |
> | **B**：EDAS 托管 | PaaS | 写代码、上传 jar | JDK、进程、服务治理、伸缩 | ✅ 已就绪 |
> | **C**：SAE/函数计算 | Serverless | 只写代码 | 一切运行时 | 🚧 后续章节 |
>
> **阅读建议**：第一次上云的同学先把 **公共部分（Part 1–4）** 读完并跑完，再 **任选一条路径（Part 5 或 Part 6）** 跑通；想完整理解云原生的同学两条路径都跑一遍，亲身体会差异。

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
- [Part 7 路径 C — Serverless 上云（占位，后续扩展）](#part-7-路径-c--serverless-上云占位后续扩展)
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
（Serverless）                                       🏢 没请求时缩到 0 实例
                                                     🏢 按调用次数计费
```

**核心思想：越往下，你的"运维负担"越轻，但"对底层的控制力"也越弱。** 这正是云原生的一个核心权衡。

### 1.2 应该选哪条路径？

一个简单的决策树：

```
你能/愿意维护 Linux 服务器吗？
├─ 是 ──► 想学底层、成本敏感、追求灵活 ──► 路径 A（ECS）
└─ 否 ──► 需要 Dubbo 服务治理可视化吗？
          ├─ 是 ──► 路径 B（EDAS）
          └─ 否 ──► 流量小且波动大？  ──► 路径 C（Serverless，后续扩展）
```

**本课程推荐学习顺序**：

1. **先做路径 A**：亲手装 JDK、scp 上传 jar、运行 `manage.sh`，把"应用是怎么跑起来的"看透。
2. **再做路径 B**：体会 EDAS 帮你"自动做了什么"——同样的 jar，不需要 SSH 上服务器，控制台点一点就部署完了。
3. 对比两者，你会真正理解 PaaS 的价值。

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

### 3.1 准备 3 台 ECS（一台一个微服务）

#### 3.1.1 ECS 角色规划

3 台 ECS 分别承担一个微服务的角色，是本章最重要的架构决定：

| ECS 编号 | 角色 | 跑什么 | 端口 | 安全组入方向 |
|---------|------|--------|------|------------|
| **ECS-1** | 订单服务节点 | `coffee-userorder-provider.jar` | 7001 | 22, 7001 |
| **ECS-2** | 快递服务节点 | `coffee-expresstrack-provider.jar` | 8001 | 22, 8001 |
| **ECS-3** | 网关 + 前端节点 | `coffee-app.jar` + Nginx 前端 | 8005 + 80 | 22, 80, 8005 |

> 💡 **为什么 ECS-3 同时跑网关和前端？**
> coffee-app 和前端都是"系统入口"，用户的浏览器直接访问它们（前端 80 端口拿页面、网关 8005 端口调 API）。把它们放同一台机器，对外只需对一个公网 IP，简化了部署。生产环境通常前端走 CDN 或独立 ECS——教学环境合并以省成本。

#### 3.1.2 购买 ECS（已有的同学跳过）

**阿里云控制台 → 云服务器 ECS → 创建实例**，重复 3 次：

| 配置项 | 填写值 |
|--------|--------|
| 付费模式 | 按量付费（学完即释放，每台每天几元） |
| 地域 | 选一个（如"华东1 杭州"），**3 台必须同一地域** |
| 规格 | 2 核 2 GB（最低 ecs.t6-c1m1.large 即可） |
| 镜像 | Ubuntu 22.04 64位（或 Alibaba Cloud Linux 3） |
| 系统盘 | 40 GB（默认即可） |
| VPC | **3 台必须选同一 VPC、同一交换机** |
| 公网 IP | 分配，按流量 1 Mbps |
| 登录密码 | 设置 root 密码（3 台用同一个密码，方便管理） |

#### 3.1.3 记录 3 台 ECS 的信息

按下表把 3 台 ECS 的信息一次性记录好，后面所有步骤都会用到：

| 角色 | 公网 IP | 私网 IP | 备注 |
|------|---------|---------|------|
| ECS-1（userorder） | `_____.___.___.___` | `172.16.___.___` | 你 SSH 进去 |
| ECS-2（expresstrack） | `_____.___.___.___` | `172.16.___.___` | 你 SSH 进去 |
| ECS-3（coffee-app + 前端） | `_____.___.___.___` | `172.16.___.___` | **用户浏览器访问的就是这个公网 IP** |

> 💡 **私网 IP 在哪看？** ECS 控制台 → 实例详情 → 网络信息 → 私有 IP 地址。3 台都要记下来，因为 RDS 白名单要把 3 个私网 IP 都加进去。

#### 3.1.4 同一 VPC、同一地域（再次强调）

**🔥 铁律**：

- 3 台 ECS 必须在 **同一 VPC + 同一交换机** ——否则它们之间走公网，慢且贵
- 3 台 ECS、RDS、MSE Nacos **必须在同一地域**

检查方法：ECS 控制台 → 实例详情 → 网络信息 → VPC ID，3 台应完全相同。

### 3.2 准备 RDS MySQL

#### 步骤 ① 购买实例（已有的同学跳过）

控制台搜索 **云数据库 RDS** → 创建实例 → 选 **MySQL 8.0** → **基础版** 1 核 2 GB → 地域和 VPC 跟 ECS 一致 → 完成购买，等 5 分钟初始化。

#### 步骤 ② 创建数据库和账号

进入 **RDS 控制台 → 你的实例 →** 左侧菜单：

**创建数据库**（点两次，分别建两个库）：

| 数据库名 | 字符集 |
|---------|--------|
| `userordertest` | `utf8mb4` |
| `expresstracktest` | `utf8mb4` |

**创建账号**（左侧"账号管理"→ 创建账号）：

| 配置项 | 填写值 |
|--------|--------|
| 账号名 | `userordertest` |
| 账号类型 | 普通账号 |
| 密码 | 自己定，**牢记** |
| 授权数据库 | 同时勾上 `userordertest` 和 `expresstracktest`，权限选"读写" |

#### 步骤 ③ 初始化表结构

RDS 不允许从命令行直接连入，用 **MySQL Workbench**（图形化工具，免费）：

1. 下载 Workbench（官网搜索 "MySQL Workbench"）
2. 新建连接：
   - **Hostname**：RDS 控制台 → 实例详情 → 连接信息 → **外网地址**（用外网地址是因为现在从你电脑连）
   - **Port**：3306
   - **Username**：`userordertest`，**Password**：你刚设的密码
3. 连上后执行项目里的 SQL 脚本：

| 脚本文件 | 在哪个库执行 |
|---------|------------|
| `cloudnativeapp/sql/order初始化.sql` | `userordertest` |
| `cloudnativeapp/sql/express初始化.sql` | `expresstracktest` |

> 💡 这两个脚本就是第 05 章本地建库用的同一份，含 `DROP TABLE IF EXISTS`，可重复执行。

**确认**：Workbench 左侧两个库下都能看到对应的表。

> ❓ **为什么这里用外网地址？后面又说用内网？**
> 现在是 **从你电脑连 RDS 灌数据**，你电脑在公网，所以用外网地址。
> 后面 **ECS 上的应用连 RDS**，ECS 跟 RDS 在同一 VPC，必须用 **内网地址**（更快、不计流量费）。

#### 步骤 ④ 配置 RDS 白名单（必做）

RDS 默认只允许白名单里的 IP 访问。**ECS 不加入白名单，应用就连不上数据库**。

**RDS 控制台 → 实例详情 → 数据安全性 → 白名单设置**：

1. 找到默认分组（或新建），点"修改"
2. 加入 **3 台 ECS 的私网 IP**（Part 3.1.3 记的 3 个 IP，逗号分隔）
3. 或者更省事的做法：直接加整个 VPC 网段（如 `172.16.0.0/16`），允许 VPC 内所有机器访问

**确认**：白名单列表里出现了 3 个 ECS 私网 IP（或一个 VPC 网段）。

> ❓ **为什么 3 台都要加？** ECS-1（userorder）和 ECS-2（expresstrack）会直接连 RDS（用 JDBC），所以必须加。ECS-3（coffee-app）不连 RDS，但加上也无害，省得以后忘了。

#### 步骤 ⑤ 记下两个地址

后面会用到，从 **RDS 控制台 → 实例详情 → 连接信息** 各复制一份：

| 地址类型 | 形如 | 用途 |
|---------|------|------|
| **内网地址** | `rm-xxxxx.mysql.rds.aliyuncs.com:3306` | ECS 上的应用连数据库（**主要用这个**） |
| **外网地址** | `rm-xxxxx.mysql.rds.aliyuncs.com:3306`（端口同，前缀不同） | Workbench 灌数据用 |

### 3.3 准备 MSE Nacos

本地用的是你自己启动的 Nacos，云上用 **MSE Nacos**（阿里云全托管，不用自己运维）。

> ❓ **为什么不能继续用本地 Nacos？** ECS 上的服务访问不到你笔记本的 `127.0.0.1:8848`，本地 Nacos 一关机就没了。
> ❓ **为什么不在 ECS 上自己装 Nacos？** 可以但不推荐：自己维护成本高，且后续路径 B 的 EDAS 服务治理页面只识别 MSE Nacos 里的数据。

#### 步骤 ① 购买实例

控制台搜索 **微服务引擎 MSE** → 注册配置中心 → 实例列表 → 创建实例：

| 配置项 | 填写值 |
|--------|--------|
| 引擎类型 | **Nacos** |
| 版本 | **2.x** |
| 规格 | **开发测试版**（最便宜，教学够用） |
| 地域 / VPC / 交换机 | **必须与 ECS 完全一致** |

创建后等 3–5 分钟变为"运行中"。

#### 步骤 ② 记下两个地址

进入实例详情，记下 **两个** 接入地址：

| 地址类型 | 形如 | 路径 A 用 | 路径 B 用 |
|---------|------|:---:|:---:|
| **内网（VPC）地址** | `mse-xxxxxxxx-p.nacos-ans.mse.aliyuncs.com:8848` | ✅（推荐） | ✅（必用） |
| **公网地址** | `mse-xxxxxxxx-p.nacos-pub.mse.aliyuncs.com:8848` | 备用 | ❌ |

> 💡 **公网地址啥时候用？** 路径 A 时如果你想从本地电脑也能连这个 Nacos 调试，可以用公网地址。生产部署一律用内网地址。
>
> 💡 **公网访问的开关**：MSE Nacos 默认不开公网访问，需要在实例详情→网络配置里手动开启并配置白名单（含你电脑的公网 IP）。

#### 步骤 ③ 验证

ECS 上 SSH 登录后测试：

```bash
# 测试 ECS 能不能 ping 通 MSE Nacos
curl -v telnet://mse-xxxxxxxx-p.nacos-ans.mse.aliyuncs.com:8848
```

看到 `Connected to ...` 即成功。失败的话——通常是 VPC 不一致，或 MSE 白名单没放通 ECS 私网 IP（MSE 实例详情 → 白名单设置）。

---

## Part 4 项目模块结构与制品准备

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

#### 4.2.1 为什么云原生项目要用云效私有仓库

**关键场景**：你想让云效流水线自动构建 `coffee-userorder/provider`。流水线机器是 **全新的临时容器**,没有你的 `~/.m2`,所以构建 provider 时它会去 Maven 中央仓库找 `coffee-userorder-api`——**找不到!** 因为这是你内部的库,中央仓库没有。

**解决方案**:把库模块发布到云效私有仓库,流水线机器从云效拉取。

这正是项目里所有 `pom.xml` 的 `<distributionManagement>` 节点的作用——声明 "执行 `mvn deploy` 时把包推到云效仓库地址":

```xml
<distributionManagement>
    <snapshotRepository>
        <id>aliyun-snapshot</id>
        <url>${aliyun.repo.url}</url>   <!-- 仓库地址由 settings.xml 注入，不写死 -->
    </snapshotRepository>
</distributionManagement>
```

> 💡 **回到学习主线**: 本地开发时为了简单,你用了 `mvn install` 走方式 A。**正式上云时,推荐切换到方式 B**——这才是真正的云原生制品流。本节后面教你怎么切。

#### 4.2.2 创建你自己的云效制品仓库

1. 登录 [云效](https://flow.aliyun.com) → 进入 **制品仓库 Packages**(或访问 [packages.aliyun.com](https://packages.aliyun.com))
2. 左侧 **Maven → 创建仓库**
   - 类型选 **Snapshot**(本项目版本号都是 `1.0-SNAPSHOT`,要进 Snapshot 仓库)
   - 仓库名自定义,如 `coffee-snapshots`
3. 创建后记下 **仓库地址**,形如:
   ```
   https://packages.aliyun.com/maven/repository/<你的命名空间>/coffee-snapshots
   ```
4. 点页面上的 **"指南"** 或 **"凭证"** → 复制 **用户名 + 密码**(云效专门为 Maven 仓库生成的一对,**不是你的阿里云登录密码**)

#### 4.2.3 配置本地 Maven 的 settings.xml

打开你本机的 `settings.xml`(位于 `~/.m2/settings.xml`,没有就新建;Windows 是 `C:\Users\你\.m2\settings.xml`),加入下面两段:

```xml
<settings>
  <!-- 凭据：id 必须是 aliyun-snapshot，与 pom 的 <id> 完全一致 -->
  <servers>
    <server>
      <id>aliyun-snapshot</id>
      <username>云效给你的用户名</username>
      <password>云效给你的密码</password>
    </server>
  </servers>

  <!-- 仓库地址：通过 properties 注入到所有 pom 的 ${aliyun.repo.url} 占位符 -->
  <profiles>
    <profile>
      <id>aliyun-repo</id>
      <properties>
        <aliyun.repo.url>https://packages.aliyun.com/maven/repository/你的命名空间/coffee-snapshots</aliyun.repo.url>
      </properties>
    </profile>
  </profiles>

  <activeProfiles>
    <activeProfile>aliyun-repo</activeProfile>
  </activeProfiles>
</settings>
```

> ❓ **为什么仓库地址不直接写在 pom.xml?**
> 因为 pom.xml 会被推到公开 Git 仓库,而仓库地址含命名空间(暴露你的账号信息)、不同人 / 不同机器的仓库不同。所以 pom 里用 `${aliyun.repo.url}` 占位,**每个人在自己机器的 settings.xml 里填自己的**。

### 4.3 发布库模块到云效(方式 B,推荐)

在项目根目录,按依赖顺序发布:

```cmd
rem 把 3 个库模块发布到云效私有仓库
cd coffee-common
mvn clean deploy -DskipTests

cd ..\coffee-userorder\api
mvn clean deploy -DskipTests

cd ..\..\coffee-expresstrack\api
mvn clean deploy -DskipTests
```

每个命令末尾应看到 `Uploading to aliyun-snapshot: https://packages.aliyun.com/...` 和 `BUILD SUCCESS`。

**确认**:回云效控制台 → 制品仓库 → 你的仓库,能看到 `com.coffee.yun` 命名空间下出现了 3 个 artifact:
- `coffee-common`
- `coffee-userorder-api`
- `coffee-expresstrack-api`

> 💡 **以后这 3 个库的版本怎么管?**
> - 你改了 `coffee-common` 的代码 → 再次 `mvn deploy` → 云效仓库会保留多个 SNAPSHOT 时间戳版本,Maven 自动拉最新的
> - 想发布正式版本(不再带 SNAPSHOT) → 改 `pom.xml` 里的 `<version>` 为 `1.0.0` 不带 SNAPSHOT,但需要在云效另建一个 Release 仓库
> - 教学环境用 SNAPSHOT 就够了

> ⚠️ **方式 A 的简化版本(可选)**: 如果你第一次跑不想搞云效,可以只在本地 `mvn install` 这 3 个库:
> ```cmd
> cd coffee-common && mvn clean install -DskipTests
> cd ..\coffee-userorder\api && mvn clean install -DskipTests
> cd ..\..\coffee-expresstrack\api && mvn clean install -DskipTests
> ```
> 后续的 provider/网关打包能在你本机找到这 3 个库。**但流水线、新机器都用不了**——这就是 "云原生制品流" 和 "本地能跑就行" 的差别。

### 4.4 打包 3 个应用模块为可部署 jar

完成 4.3 之后,3 个 provider/网关在构建时可以从云效(或本地仓库)拉到所需的 api 和 common,正常打包:

```cmd
cd ..\..\coffee-userorder\provider
mvn clean package -DskipTests

cd ..\..\coffee-expresstrack\provider
mvn clean package -DskipTests

cd ..\..\coffee-app
mvn clean package -DskipTests
```

> 💡 **Linux/Mac 同学** 把 `\` 换成 `/`,命令完全一样。

**确认产物**:每个 `mvn` 命令末尾出现 `BUILD SUCCESS`,并且能在以下位置找到 jar:

| 应用模块 | jar 文件位置 | 部署到 |
|---------|------------|-------|
| 订单服务 | `coffee-userorder/provider/target/coffee-userorder-provider-1.0-SNAPSHOT.jar` | ECS-1 |
| 快递服务 | `coffee-expresstrack/provider/target/coffee-expresstrack-provider-1.0-SNAPSHOT.jar` | ECS-2 |
| API 网关 | `coffee-app/target/coffee-app-0.0.1-SNAPSHOT.jar` | ECS-3 |

> ⚠️ **必须用 `mvn` 不要用 `mvnw`**: `mvnw`(Maven Wrapper)会联网下载 Maven,国内常超时。
>
> ⚠️ **如果报错 "Could not find artifact com.coffee.yun:coffee-userorder-api:1.0-SNAPSHOT"**: 说明 4.3 没做或失败,库还没在仓库里,自然找不到。回去检查 4.2.3 的 settings.xml 配置和 4.3 的 deploy 是否成功。

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

---

## Part 5 路径 A — IaaS 上云（3 台 ECS + manage.sh）

> 这条路径让你 **亲手触摸"服务运行"的每一层**：装 JDK、上传 jar、写启动脚本、管理进程、看日志。**3 台 ECS，每台跑 1 个微服务**——这就是教科书里"微服务架构"的真实样子。
>
> **🧭 对照第 05 章**：你在本地是这样启动 3 个服务的（同一台笔记本开 3 个终端窗口）：
> ```bash
> # 本地（第 05 章）：3 个进程跑在同一台机器
> 终端 1：cd coffee-userorder/provider/target && java -jar xxx.jar       (端口 7001)
> 终端 2：cd coffee-expresstrack/provider/target && java -jar xxx.jar    (端口 8001)
> 终端 3：cd coffee-app/target && java -jar xxx.jar                       (端口 8005)
> ```
> 路径 A 把这 3 个进程 **分布到 3 台 ECS**：
> ```
> ECS-1：java -jar coffee-userorder-provider.jar       (端口 7001)
> ECS-2：java -jar coffee-expresstrack-provider.jar    (端口 8001)
> ECS-3：java -jar coffee-app.jar                       (端口 8005)
> ```
> 启动命令几乎一样，区别是：
> - 本地 3 个进程 ──► 云上 3 台机器，每台 1 个进程
> - 本地连 `127.0.0.1:3307` 的 MySQL ──► 3 台都连 RDS 内网地址
> - 本地连 `127.0.0.1:8848` 的 Nacos ──► 3 台都连 MSE Nacos 内网地址
> - 本地终端窗口关闭进程就停了 ──► 云上用 `nohup` 让进程脱离终端常驻

### 5.1 路径 A 的特点

```
你要做的事                                  结果
────────                                  ────
SSH 登录到 3 台 ECS（重复 3 次）          → 拿到 3 台空白 Linux
每台 ECS：apt install openjdk-17           → 装好 Java
每台 ECS：scp 上传 1 个 jar                → 各自的 jar 就位
每台 ECS：./manage.sh start                → 各自的进程跑起来
```

**好处**：

- ✅ **真正的微服务架构**——服务隔离、独立伸缩
- ✅ 完全控制每一层，调试方便
- ✅ 学习价值高，理解"分布式应用是怎么活在多台 Linux 上的"

**代价**：

- ❌ JDK、glibc、磁盘 你要在 **3 台机器上各管一次**
- ❌ 服务挂了你要自己上对应 ECS 看日志、重启
- ❌ 想扩容要再起 ECS、再 scp、再启服务（这正是 EDAS / Serverless 要解决的问题）
- ❌ 没有可视化的服务治理页面（只能去 MSE Nacos 控制台或日志里看）

### 5.2 先认识 Workbench——你的浏览器内置 ECS 终端

零基础同学最大的恐惧:在自己电脑上装 SSH 客户端、配密钥、打 scp 命令——**这些全都可以不用**。阿里云 ECS 自带一个叫 **Workbench** 的网页终端,你只要会"用鼠标点"和"在网页上传文件"就够了。

**怎么打开 Workbench**:

1. 登录阿里云控制台 → 进入 **云服务器 ECS** → 实例列表
2. 找到你要操作的 ECS,**点最右边的"远程连接"** → 选 **"Workbench 远程连接"** → 点 **"立即登录"**
3. 在弹出窗里填登录信息(root + 你设的密码),点连接
4. 浏览器里就出现了一个黑底白字的终端窗口——这就是 ECS 内部的命令行,和"SSH 上去"完全等效

**Workbench 顶部工具栏的两个关键按钮**:

| 按钮 | 用途 | 用法 |
|------|------|------|
| **📤 上传文件** | 把本地文件传到 ECS | 点按钮 → 选本地文件 → 填目标路径 → 上传 |
| **📥 下载文件** | 从 ECS 下载文件到本地 | 一般不用,出问题时下载日志看 |

> 💡 **3 台 ECS 怎么操作?** 给每台 ECS 都各开一个浏览器标签页,3 个 Workbench 标签页并排,操作哪台就切到哪个标签页。比 SSH 客户端切换还方便。
>
> 💡 **下面所有操作只用 3 件事**:
> 1. 在 Workbench 终端窗口 **粘贴并回车** 几条事先准备好的命令
> 2. 点 **📤 上传文件** 按钮把本地准备好的文件传上去
> 3. 偶尔用 Workbench 终端窗口 **输入命令检查状态**

### 5.3 在本地电脑改好两个配置文件(用 VSCode 或记事本)

为了避免在 ECS 上用 nano/vim 编辑文件(那对零基础学生太难了),**改文件这一步全部在你自己电脑上完成**,改完直接上传。

**改 manage.sh** —— 3 台 ECS 用同一份,所以只改 1 次:

用 VSCode(或记事本)打开本地 `cloudnativeapp/deploy/manage.sh`,找到第 20 行附近:

```bash
NACOS_ADDR="mse-xxxxxxxx-p.nacos-ans.mse.aliyuncs.com:8848"
```

把这里换成你在 Part 3.3 记下的 **MSE Nacos 内网地址**,保存。

**改两个 application-dev.yml**:

用 VSCode 打开 `cloudnativeapp/deploy/config/userorder/application-dev.yml`,把 `yourDbUser` / `yourDbPassword` / `your-rds-address` 换成你的 RDS 内网地址和账号,保存。

同样打开 `cloudnativeapp/deploy/config/expresstrack/application-dev.yml`,改另一份(注意数据库名是 `expresstracktest`)。

**确认**:本地的 `deploy/` 目录里 3 个文件都已经改好。**ECS-3 不需要 application-dev.yml**(coffee-app 不连数据库)。

> 💡 **为什么不在 ECS 上改?** ECS 上得用 nano 或 vim 这种命令行编辑器,光是"怎么保存退出"就能卡半小时。在本地用你熟悉的编辑器改完再上传,简单 10 倍。

### 5.4 在每台 ECS 上装 JDK 17(粘贴一条命令)

打开 ECS-1 的 Workbench,在终端窗口里粘贴这条命令然后回车:

```bash
apt update && apt install -y openjdk-17-jdk && java -version
```

> 💡 Ubuntu 用 `apt`;Alibaba Cloud Linux 用 `dnf install -y java-17-openjdk-devel`。本章默认 Ubuntu。

最后一行能看到 `openjdk version "17.0.x"` 即成功。

**对 ECS-2 和 ECS-3 同样操作**——切到对应 Workbench 标签页,粘贴同一条命令,等装好。

> 💡 **就这一条命令,3 台重复 3 次**。后面 Part 6 的 EDAS 会帮你免掉这步,先体验"手动 3 次"的繁琐才能理解 PaaS 的价值。

### 5.5 在每台 ECS 上预建目录(粘贴一条命令)

每台 ECS 的 Workbench 里粘贴同一条命令,回车:

```bash
mkdir -p ~/coffee/jars ~/coffee/config && cd ~/coffee && pwd
```

输出应显示 `/root/coffee`(如果是 Ubuntu 默认用户则是 `/home/ubuntu/coffee`)——这就是后面所有文件要放的"工作目录"。

> 💡 **为什么用 `~/coffee` 不用 `~/coffee`?** `~` 是你当前用户的家目录,本来就你自己拥有,**不需要 sudo**。这避免了"权限不够"这类零基础学生最容易踩的坑。

### 5.6 用 Workbench 上传按钮把文件传到每台 ECS

> **每台 ECS 的目标结构(回顾)**——上传时把文件放到对应位置:
> ```
> ~/coffee/
> ├── manage.sh          ← 3 台都要传
> ├── jars/
> │   └── <本机服务的 jar>  ← 每台只传 1 个
> └── config/
>     └── <本机服务名>/
>         └── application-dev.yml  ← 仅 ECS-1 / ECS-2 传
> ```

#### 操作流程(以 ECS-1 为例)

切到 ECS-1 的 Workbench 标签页,**点顶部 📤 上传文件** 按钮,弹出对话框:

**第 1 次上传:manage.sh**
- 本地文件 → 选 `cloudnativeapp/deploy/manage.sh`
- ECS 目标路径 → 填 `/root/coffee/` (或你 Part 5.5 看到的工作目录)
- 点上传

**第 2 次上传:本机要跑的 jar**
- 本地文件 → 选 `cloudnativeapp/coffee-userorder/provider/target/coffee-userorder-provider-1.0-SNAPSHOT.jar`
- ECS 目标路径 → 填 `/root/coffee/jars/`
- 点上传

**第 3 次上传:本机的 application-dev.yml**
- 本地文件 → 选 `cloudnativeapp/deploy/config/userorder/application-dev.yml` (Part 5.3 已改好)
- ECS 目标路径 → 填 `/root/coffee/config/userorder/` (路径会自动创建子目录;若提示路径不存在,先在终端跑一句 `mkdir -p ~/coffee/config/userorder`)
- 点上传

#### ECS-2(快递服务) 同样三次上传,但选不同的 jar 和 yml

| 第几次上传 | 本地文件 | ECS 目标路径 |
|----------|---------|-----------|
| 1 | `deploy/manage.sh` | `/root/coffee/` |
| 2 | `coffee-expresstrack/provider/target/coffee-expresstrack-provider-1.0-SNAPSHOT.jar` | `/root/coffee/jars/` |
| 3 | `deploy/config/expresstrack/application-dev.yml` | `/root/coffee/config/expresstrack/` |

#### ECS-3(网关) 只需两次上传(不要 application-dev.yml)

| 第几次上传 | 本地文件 | ECS 目标路径 |
|----------|---------|-----------|
| 1 | `deploy/manage.sh` | `/root/coffee/` |
| 2 | `coffee-app/target/coffee-app-0.0.1-SNAPSHOT.jar` | `/root/coffee/jars/` |

#### 检查上传成功

每台 ECS 的 Workbench 终端里粘贴:

```bash
ls -R ~/coffee
```

应该看到 manage.sh、jars/ 下有 1 个 jar、config/(ECS-3 没有这个目录)。

> 💡 **没看到 jars/ 或 config/ 子目录?** 可能是 Workbench 上传时目标路径填错。手动建一下:`mkdir -p ~/coffee/jars ~/coffee/config/userorder` 然后重新上传指定到这个路径。

### 5.7 在每台 ECS 启动服务(粘贴一条命令)

每台 ECS 的 Workbench 里粘贴**完全相同**的命令:

```bash
cd ~/coffee && chmod +x manage.sh && ./manage.sh start
```

**ECS-1 输出**(只启动 userorder,其他两个跳过):
```
>>> 启动本机部署的服务（Nacos: mse-xxx:8848）
    本机检测到 1 个 jar，开始按依赖顺序启动...
  [✓] userorder: 启动成功 (PID=12345, 端口=7001)
  [-] expresstrack: 跳过（本机未部署此服务）
  [-] app: 跳过（本机未部署此服务）
```

**ECS-2 输出**:只启动 expresstrack,另两个显示"跳过"
**ECS-3 输出**:只启动 coffee-app,另两个显示"跳过"

> 💡 **"跳过"是正常的**——manage.sh 检测到本机 `jars/` 里没有那个 jar,自动跳过。这就是"一份脚本 3 台通用"的设计。
>
> 💡 **启动顺序建议**:先启 ECS-1、ECS-2(provider),最后启 ECS-3(consumer)。顺序反了 coffee-app 启动时会有几秒"找不到服务提供者"警告,不影响最终成功。

### 5.8 在 ECS 安全组(网页 GUI)开放端口

不用进 ECS 终端,在阿里云**网页**上点鼠标:

1. ECS 控制台 → 选 ECS-1 → 点 **"安全组"** 列下的链接 → 进入安全组详情
2. **配置规则** → **入方向** → **手动添加**
3. 各 ECS 要放开的端口(用鼠标添加,选 TCP,目标 `0.0.0.0/0`):

| ECS | 端口 | 说明 |
|-----|------|------|
| ECS-1 | 7001 | 订单服务健康检查(可选,只为浏览器能 curl 验证) |
| ECS-2 | 8001 | 快递服务健康检查 |
| ECS-3 | 8005 | API 网关(浏览器要从这里调接口,**必须开**) |
| ECS-3 | 80 | Nginx 前端(下一节用) |

22 端口默认已开(不开 Workbench 也用不了)。

### 5.9 验证 3 个服务都跑起来了

在浏览器地址栏分别访问(浏览器,不是 Workbench 终端):

| 验证地址 | 期望结果 |
|---------|---------|
| `http://<ECS-1 公网IP>:7001/actuator/health` | `{"status":"UP"}` |
| `http://<ECS-2 公网IP>:8001/actuator/health` | `{"status":"UP"}` |
| `http://<ECS-3 公网IP>:8005/actuator/health` | `{"status":"UP"}` |
| MSE Nacos 控制台 → 服务管理 → 服务列表 | 看到 3 个服务都已注册 |

**如果某一个不通**,回对应 ECS 的 Workbench 终端粘贴:

```bash
cd ~/coffee && ./manage.sh status   # 看进程在不在
cd ~/coffee && ./manage.sh logs     # 看启动日志(Ctrl+C 退出)
```

### 5.10 日常管理(每台 ECS 独立管理)

以后想停 / 重启 / 看日志,**回对应 ECS 的 Workbench 终端**,粘贴命令:

| 想做的事 | 在 ECS-1 / ECS-2 / ECS-3 终端粘贴 |
|---------|----------------------------------|
| 看本机服务在不在跑 | `cd ~/coffee && ./manage.sh status` |
| 实时看本机日志 | `cd ~/coffee && ./manage.sh logs` (Ctrl+C 退出) |
| 停本机服务 | `cd ~/coffee && ./manage.sh stop` |
| 重启本机服务 | `cd ~/coffee && ./manage.sh restart` |
| 换新版本 jar | 用 📤 上传新 jar 覆盖旧的 → `./manage.sh restart` |

### 5.11 路径 A 工作原理图解

```
你在 3 台 ECS 的 Workbench 里分别粘贴 ./manage.sh start
        │
        ▼
每台 ECS 上 manage.sh 检测 ~/coffee/jars/ 下的 jar，对存在的服务执行：
   java -jar xxx.jar \
       --dubbo.registry.address=nacos://<MSE 内网地址>:8848 \
       --spring.config.additional-location=file:~/coffee/config/xxx/
        │
        ▼
Spring Boot 启动（dev profile）
   ├─ 读取 jar 内的 application.yml + application-dev.yml
   ├─ 用外部 application-dev.yml 覆盖数据库段
   └─ 命令行参数 --dubbo.registry.address 覆盖 Dubbo 注册地址
        │
        ▼
3 台 ECS 上的服务分别：
   ECS-1: Dubbo 注册 coffee-userorder 到 MSE Nacos      ──► RDS（JDBC）
   ECS-2: Dubbo 注册 coffee-expresstrack 到 MSE Nacos   ──► RDS（JDBC）
   ECS-3: Dubbo 订阅 (userorder, expresstrack) → 从 MSE Nacos 拿到提供者 IP
              ──► 直接 RPC 到 ECS-1:20880 / ECS-2:20880
        │
        ▼
ECS-3 还监听 HTTP 8005，接受前端 / 用户的 REST 调用
```

**这条路径的关键认识**：
- **3 台 ECS 通过 MSE Nacos 互相发现**——这是分布式系统最朴素的服务发现机制。没有 Nacos，ECS-3 不知道 ECS-1 在哪台 IP 上。
- **manage.sh = systemd / supervisor 的简化版**——它就是个 bash 脚本，用 `nohup` + PID 文件管理进程。真生产环境会用 systemd unit。
- **dev profile + 文件覆盖**——为了"改配置不用改 ENV 变量、不用重启脚本"，manage.sh 仍用 dev profile，靠外部 YAML 文件覆盖关键值。和 EDAS 路径用 prod profile 是两种风格，**等价**。

---

## Part 6 路径 B — PaaS 上云（EDAS 托管）

> 这条路径让你看到："不写一行运维代码，应用怎么活在云上"。
>
> **🧭 对照路径 A 和本地**：
> | 步骤 | 本地（第 05 章） | 路径 A（IaaS） | 路径 B（PaaS） |
> |------|--------------|--------------|-------------|
> | 装 JDK | 你装在自己电脑 | 你 SSH 上 ECS apt install | 创建应用时选版本即可 |
> | 启动进程 | 双击或 `java -jar` | `manage.sh start` | 控制台点"部署"按钮 |
> | 改 Nacos 地址 | 改本地 `application-dev.yml` 或环境变量 | 改 `manage.sh` 的 `NACOS_ADDR` | **不用改**，EDAS Agent 自动注入 |
> | 出问题怎么查 | 本地控制台输出 | `manage.sh logs` | 控制台 → 应用监控 → 查看日志 |
>
> 越往右，"你做的事"越少。这就是 PaaS 在 IaaS 之上的"自动化"价值。

### 6.1 这条路径的特点

```
你做的事                                结果
────────                              ────
EDAS 控制台 → 创建微服务空间          → 决定用哪个 Nacos
EDAS 控制台 → 创建集群、加入 ECS       → ECS 上自动装好 EDAS Agent
EDAS 控制台 → 创建应用 → 上传 jar      → Agent 启动 Java 进程
EDAS 控制台 → 服务治理                → 可视化看到服务调用关系
```

**好处**：

- ✅ **零 SSH 部署**——一切在 Web 控制台完成
- ✅ Agent 自动接管 Dubbo 注册，**代码中的 Nacos 地址自动被覆盖**
- ✅ **服务治理可视化**：服务调用拓扑图、调用链追踪、灰度发布
- ✅ 扩容只需勾选更多 ECS

**代价**：

- ❌ EDAS 是付费产品（基础版按节点收费，教学用一台节点月费百元级）
- ❌ 学不到底层运维细节（这既是优点也是缺点）

### 6.2 配置 3 台 ECS 的安全组（出方向）

EDAS Agent 需要主动连阿里云上报数据。**3 台 ECS 都必须** 放通 **出方向** 三个端口：

**ECS 控制台 → 安全组 → 配置规则 → 出方向 → 手动添加**（3 台 ECS 各做一次）：

| 端口 | 协议 | 授权对象 | 说明 |
|------|------|---------|------|
| 8442 | TCP | 0.0.0.0/0 | EDAS Agent 控制通道 |
| 8443 | TCP | 0.0.0.0/0 | EDAS Agent 控制通道 |
| 8883 | TCP | 0.0.0.0/0 | 服务元数据上报 |

> 💡 **省事技巧**：如果 3 台 ECS 在同一个安全组里（同地域同 VPC 默认就是），只改一次即可。
>
> ⚠️ **不放通这三个端口的症状**：EDAS 应用能创建、能部署，但治理页面永远显示"没有数据"。这是最常见的"看似一切正常但就是没数据"的坑。

### 6.3 创建 EDAS 微服务空间（绑定 MSE Nacos）

> **微服务空间** 决定了里面所有应用用哪个注册中心。**创建后注册中心类型不可改**，选错只能删了重建。

**EDAS 控制台 → 资源管理 → 微服务空间 → 创建微服务空间**：

| 配置项 | 填写值 |
|--------|--------|
| 空间名称 | `coffee-prod` |
| 地域 | 与 ECS、MSE 一致 |
| 注册中心类型 | **MSE Nacos**（选这个，不要选默认的"EDAS 注册中心"） |
| MSE Nacos 实例 | 选 Part 3.3 创建的实例 |

**确认**：空间列表"注册中心类型"列显示的是 **MSE 实例 ID**（形如 `mse-xxxxxx-p`），不是 `cn-hangzhou` 这种默认值。

> ❓ **为什么必须选 MSE Nacos 而不是 EDAS 默认注册中心？**
> EDAS 内置的"注册配置中心"是旧版实现，**不支持 Dubbo 3.x 的服务元数据格式**，治理页面查不到服务。本项目用的是 Dubbo 3.x，必须用 MSE Nacos。

### 6.4 创建 EDAS ECS 集群并导入 3 台 ECS

**EDAS 控制台 → 资源管理 → EDAS ECS 集群 → 创建集群**：

| 配置项 | 填写值 |
|--------|--------|
| 集群名称 | `coffeecluster` |
| 微服务空间 | 选 `coffee-prod`（上一步建的） |
| 网络类型 | 专有网络 |
| VPC | 与 ECS 相同 |

> ⚠️ **集群的微服务空间一旦确定不可改**。选错只能删了重建。

创建后：进入集群详情 → **添加 ECS** → **同时勾选 3 台 ECS** → 确认 → 等 3–5 分钟，EDAS 自动在 3 台 ECS 上分别安装 Agent。

**确认**：3 台 ECS 状态都变为"运行中"，集群详情显示"已安装 Agent"。

> 💡 **注意**：这里只是把 3 台 ECS "登记" 到 EDAS 集群里——还没有任何应用跑在上面。下一步创建应用时再决定每个应用部署到哪台 ECS。

> ⚠️ **如果路径 A 还在跑**：导入 EDAS 之前最好先到每台 ECS 上执行 `~/coffee/manage.sh stop` 停掉路径 A 的进程，避免端口冲突和 Dubbo 注册冲突。

### 6.5 部署 3 个应用，每个绑定到指定 ECS

**EDAS 控制台 → 应用管理 → 创建应用**，依次创建三个应用。**每个应用的"部署 ECS"分别选一台**，实现一台 ECS 一个微服务：

| 应用 | 部署到 | 关键参数 |
|------|--------|----------|
| `userorder` | ECS-1 | 需要 `-DDB_*` 连 RDS |
| `expresstrack` | ECS-2 | 需要 `-DDB_*` 连 RDS |
| `coffee-app` | ECS-3 | 不连数据库 |

#### 6.5.1 部署订单服务到 ECS-1

| 配置项 | 填写值 |
|--------|--------|
| 应用名称 | `userorder` |
| 部署环境 | ECS 集群 → `coffeecluster` |
| 应用运行环境 | Java |
| JDK 版本 | OpenJDK 17 |
| 部署包类型 | JAR 包 |
| 部署包 | 上传 `coffee-userorder/provider/target/coffee-userorder-provider-1.0-SNAPSHOT.jar` |
| **部署到哪台 ECS** | **只勾选 ECS-1**（其他两台不勾） |
| JVM 参数 | 见下方 |

**JVM 参数**（把 RDS 内网地址和密码换成你的实际值）：

```
-Xms128m -Xmx512m -DENV=prod -DDB_HOST=rm-xxxxx.mysql.rds.aliyuncs.com:3306 -DDB_USER=userordertest -DDB_PASSWORD=你的RDS密码
```

点 **部署**，等状态变为 **运行中**。

> ❓ **JVM 参数为什么这么写？**
> - `-Xms128m -Xmx512m`：堆内存范围（够用就行）
> - `-DENV=prod`：激活 `application-prod.yml`（与路径 A 的 dev profile 不同——EDAS 走"标准 prod 路径"）
> - `-DDB_*`：覆盖 `application-prod.yml` 里的 RDS 占位地址
> - **没写 `-DDUBBO_REGISTRY`**：因为 **EDAS Agent 会自动拦截 Dubbo 注册**，把它指向微服务空间绑定的 MSE Nacos，写了也会被覆盖

#### 6.5.2 部署快递服务到 ECS-2

| 配置项 | 填写值 |
|--------|--------|
| 应用名称 | `expresstrack` |
| 部署包 | `coffee-expresstrack/provider/target/coffee-expresstrack-provider-1.0-SNAPSHOT.jar` |
| **部署到哪台 ECS** | **只勾选 ECS-2** |
| JVM 参数 | 与 userorder **完全相同**（数据库名在 `application-prod.yml` 里已硬编码为 `expresstracktest`） |

#### 6.5.3 部署 API 网关到 ECS-3

| 配置项 | 填写值 |
|--------|--------|
| 应用名称 | `coffee-app` |
| 部署包 | `coffee-app/target/coffee-app-0.0.1-SNAPSHOT.jar` |
| **部署到哪台 ECS** | **只勾选 ECS-3** |
| JVM 参数 | `-Xms128m -Xmx256m -DENV=prod` |

> ❓ **网关为什么参数这么短？** `coffee-app` 是纯消费者，**不连数据库**，所以不需要任何 `-DDB_*` 参数。Dubbo 注册同样由 Agent 接管。

> 💡 **路径 A vs 路径 B 的关键差异**：
> - 路径 A 是 **你 SSH 到每台 ECS 上手动决定跑什么**
> - 路径 B 是 **EDAS 控制台帮你统一调度** ——创建应用时勾选 ECS，EDAS 远程把 jar 推到那台机器并启动
>
> 这就是 PaaS "中心化管理多机部署" 的核心价值。后期想扩容 userorder，只需在 EDAS 上把它再部署到 ECS-1' / ECS-1''，不用 SSH 上每台机器手动操作。

> **建议部署顺序**：先 userorder（ECS-1）、expresstrack（ECS-2），最后 coffee-app（ECS-3）。顺序反了也能启动，只是 coffee-app 启动时会有几秒"找不到服务提供者"的警告。

**确认**：三个应用状态都是 **运行中**。点进任意应用 → "应用监控" → "查看日志"，末尾出现 `Started ... in X seconds` 即启动成功。

### 6.6 路径 B 工作原理图解

```
EDAS 控制台点"部署"
        │
        ▼
EDAS 控制端通过 SSH 远程在你 ECS 上：
   1. 下载 jar 到 /home/admin/<应用名>/
   2. 启动 Java 进程，JVM 加上你填的参数 + -javaagent:edas-agent.jar
        │
        ▼
Spring Boot 启动（prod profile，因为 -DENV=prod）
   ├─ 读取 application.yml + application-prod.yml
   ├─ 代码里 dubbo.registry.address 默认值是占位的 mse-xxxxxxxx
   └─ JVM 参数 -DDB_HOST 等覆盖数据库占位地址
        │
        ▼
Dubbo 初始化注册时：
   EDAS Agent 拦截 ──► 不管你代码写什么 Nacos 地址，
                       一律重定向到"微服务空间"绑定的 MSE Nacos
        │
        ▼
        ├─► Dubbo 服务注册到 MSE Nacos
        ├─► MyBatis 连接 RDS（用你 JVM 参数里的内网地址）
        └─► HTTP 监听 7001/8001/8005
        │
        ▼
EDAS 治理页面从 MSE Nacos 读元数据 → 显示服务列表 / 调用链
```

**这条路径的关键认识**：

- **Agent 接管 Dubbo，不接管 Spring 配置中心**——再强调一次：`spring.config.import` 那行连的还是你代码里写的地址（默认 127.0.0.1），EDAS 不管它。本项目业务不依赖配置中心实际生效，所以无影响。如果要让 Spring 也连 MSE Nacos，自己加 `-DNACOS_ADDR=mse-xxx:8848`。
- **"运行中"≠"治理页面有数据"**——治理可见性的前置条件是 8442/8443/8883 出方向放通 + 微服务空间选了 MSE Nacos。

---

## Part 7 路径 C — Serverless 上云（占位，后续扩展）

> 本节暂作占位与思路引导，完整操作步骤会在后续版本基于 **SAE（Serverless 应用引擎）** 或 **函数计算 FC** 补全。

### 7.1 Serverless 是什么

**Serverless** 不是"没有服务器"，而是 **"你不用关心服务器"**：

- 没有 ECS 的概念，按"应用"或"函数"为单位部署
- **没请求时，实例可以缩到 0**（不收费）
- **请求来了，秒级冷启动一个新实例**
- 按"实际调用次数 × 运行时长"计费

### 7.2 与路径 A、B 的核心差异

| 维度 | 路径 A（ECS） | 路径 B（EDAS） | 路径 C（SAE / FC） |
|------|--------------|---------------|------------------|
| 实例数 | 固定 1 台 | 固定多台（你定） | **0 → N 弹性伸缩** |
| 缩到 0 | ❌ | ❌ | ✅ |
| 计费粒度 | 按 ECS 小时 | 按 ECS 小时 + EDAS | **按实际请求** |
| 冷启动延迟 | 无（一直在跑） | 无 | 100ms–几秒 |
| 适合场景 | 学习、稳定流量 | 企业生产、稳定流量 | **流量稀疏 / 突发**、定时任务 |

### 7.3 本项目上 Serverless 的注意点（未来章节展开）

- **Dubbo + Serverless 是反直觉的组合**——RPC 长连接和"实例随时被回收"冲突，通常 Serverless 更适合 HTTP 接口（即 coffee-app 适合，两个 provider 不太适合）
- **数据库连接池**在频繁冷启动时容易耗尽，需要用连接代理或调小连接池
- 后续章节预计选 **SAE** 演示（最接近 EDAS 的体验，迁移成本低）

> 这一节先到这。把路径 A 或 B 跑通再来想 Serverless，不晚。

---

## Part 8 部署前端到 ECS-3（路径共用）

> 不管后端走哪条路径，前端都部署到 **ECS-3**（同时跑着 coffee-app 的那台）上，由 Nginx 静态托管。
>
> **为什么前端部署到 ECS-3？** 用户的浏览器需要同时访问"前端页面（80 端口）"和"后端 API（8005 端口）"。把它们放同一台机器，对外只暴露一个公网 IP，简单。生产环境通常前端走 CDN 或独立 ECS，教学环境合并以省成本。
>
> **🧭 对照第 05 章**：本地你是这样跑前端的：
> ```bash
> # 本地（第 05 章 Step 9）
> cd app-admin
> npm install         # 装依赖
> npm run dev         # 启动开发服务器，浏览器开 http://localhost:8080
> ```
> `npm run dev` 启动了一个 **Node.js 开发服务器**，它会"边监听文件变更边热更新"——只适合你一个人开发用。
>
> 上云后用户不可能通过你电脑访问前端，所以要换一种方式：
> - `npm run dev`（一直跑的 Node.js 进程） ──► `npm run build`（只生成静态文件，构建完进程退出）
> - localhost:8080 上的开发服务器 ──► ECS-3 上的 Nginx（专门发静态文件的 Web 服务器）
> - 后端 API 调用 `http://localhost:8005`（本地） ──► `http://<ECS-3公网IP>:8005`（云上）
>
> 同样：**Vue 源码一行不改**，只在打包时通过环境变量告诉它后端地址。

### 8.1 为什么 Vue 要打包成静态文件

`app-admin/` 里都是 `.vue`、`.js` 源文件，**浏览器不认识**——浏览器只认 HTML / CSS / 普通 JS。

`npm run build` 就是把源码 **编译压缩** 成浏览器能直接打开的静态文件，输出到 `app-admin/dist/`：

```
源代码（.vue / ES6+ / SCSS）
        │  npm run build
        ▼
dist/
├── index.html                  ← 入口
└── static/
    ├── js/app.<hash>.js        ← 打包压缩后的 JS
    └── css/app.<hash>.css
```

`dist/` 上传到 ECS 给 Nginx 一发，浏览器就能直接访问。**ECS 上不用装 Node.js**——构建在你电脑上做完了。

### 8.2 配置后端 API 地址

打包时需要告诉前端"用户浏览器把 API 请求发到哪"。这个地址 **构建时永久写进 JS 文件**，所以构建前必须先设：

把 `x.x.x.x` 换成 **ECS-3 的公网 IP**（coffee-app 网关跑在 ECS-3 上，所以 API 调用都打到这台）：

**Windows CMD**：
```cmd
cd app-admin
set VUE_APP_BASE_URL=http://x.x.x.x:8005
npm run build
```

**PowerShell**：
```powershell
cd app-admin
$env:VUE_APP_BASE_URL="http://x.x.x.x:8005"
npm run build
```

**Linux / Mac**：
```bash
cd app-admin
export VUE_APP_BASE_URL=http://x.x.x.x:8005
npm run build
```

**成功标志**：`Build complete. The dist directory is ready to be deployed.`

> ⚠️ **不设这个变量直接 build 会怎样？**
> 默认 `localhost:8005`——用户的浏览器会去请求 **自己电脑** 的 8005 端口，当然找不到。这是最常见的"前端打开了但接口都失败"的坑。

### 8.3 把 dist 打包成 zip(为了方便上传一个文件)

`dist/` 目录里有几十个文件,一个一个上传太麻烦。**在本地把整个 dist 文件夹压缩成一个 zip**:

- **Windows**:进入 `cloudnativeapp/app-admin/` → 右键 `dist` 文件夹 → "发送到" → "压缩文件夹"(或 7-Zip 压缩为 zip) → 得到 `dist.zip`
- **Mac**:在 Finder 里右键 `dist` 文件夹 → "压缩 dist" → 得到 `dist.zip`

> 💡 **为什么不直接拖整个文件夹?** Workbench 网页上传按钮目前对单个文件最稳,文件夹批量上传有时会卡。zip 一下传一个文件最可靠。

### 8.4 用 Workbench 上传按钮把文件传到 ECS-3

切到 **ECS-3** 的 Workbench 标签页(同 Part 5.2)。

#### 上传 1:dist.zip(前端静态文件)

- 点 **📤 上传文件** → 本地选 `app-admin/dist.zip` → 目标路径填 `/root/` → 上传

#### 上传 2:coffee-admin.conf(Nginx 配置文件)

项目里已经准备好了一份配置文件 `cloudnativeapp/deploy/coffee-admin.conf`,**完全不用改**,直接上传:

- 点 **📤 上传文件** → 本地选 `cloudnativeapp/deploy/coffee-admin.conf` → 目标路径填 `/root/` → 上传

### 8.5 在 ECS-3 Workbench 终端粘贴一连串命令(一次完成所有事)

在 ECS-3 的 Workbench 终端里粘贴下面 **一整段** 命令,回车,等执行完:

```bash
# 1. 装 Nginx
apt update && apt install -y nginx unzip

# 2. 解压前端文件到 Nginx 服务目录
mkdir -p /var/www/coffee-admin
unzip -o /root/dist.zip -d /var/www/coffee-admin
# Windows 压缩出来的 zip 解压后顶层可能多套一层 dist/，这里规范化目录
[ -d /var/www/coffee-admin/dist ] && mv /var/www/coffee-admin/dist/* /var/www/coffee-admin/ && rmdir /var/www/coffee-admin/dist

# 3. 把 Nginx 配置文件放到正确位置
cp /root/coffee-admin.conf /etc/nginx/conf.d/coffee-admin.conf

# 4. 启动 Nginx 并设为开机自启
nginx -t                       # 检查语法
systemctl start nginx
systemctl enable nginx

echo "✅ 前端部署完成，浏览器访问 http://你的ECS-3公网IP 即可"
```

> 💡 **这一整段命令做了什么?**
> 1. 装 nginx 和 unzip 两个工具
> 2. 把上传的 dist.zip 解压到 Nginx 默认要读的目录 `/var/www/coffee-admin/`
> 3. 把上传的 Nginx 配置文件放到 Nginx 的"扫描目录"`/etc/nginx/conf.d/`
> 4. 启动 Nginx,并让它开机自启动
>
> 这一段命令是固定模板,不用懂细节,粘贴一次就好。

### 8.6 ECS-3 安全组放通入方向端口

> 如果 Part 5.8 已经放开了 ECS-3 的 80 和 8005,跳过本节。

**阿里云控制台 → ECS → ECS-3 → 安全组 → 配置规则 → 入方向 → 手动添加**(纯网页操作,鼠标点):

| 端口 | 协议 | 授权对象 | 说明 |
|------|------|---------|------|
| 80 | TCP | 0.0.0.0/0 | Nginx 对外提供前端 |
| 8005 | TCP | 0.0.0.0/0 | coffee-app REST API(浏览器从前端调用) |

> 💡 **ECS-1 和 ECS-2 不需要放通这些**(除非要从外部 curl 健康检查)——它们的服务只供 VPC 内部通过 Dubbo RPC 调用,不需要暴露到公网。这正是微服务分层的好处:**只有入口节点(ECS-3)暴露公网**,其他节点藏在内网,更安全。

**确认**:浏览器访问 `http://<ECS-3公网IP>`,应看到 CoffeeTrack 登录页。

---

## Part 9 验证

### 9.1 端到端业务验证（路径 A、B 都用）

浏览器访问 `http://<ECS-3公网IP>`（用户只看到这一个 IP，背后的 3 台机器对用户透明）：

| 步骤 | 期望结果 | 失败说明 |
|------|---------|---------|
| 1. 打开首页 | 看到登录界面 | Nginx / dist 上传问题 |
| 2. 登录 | 进入主页 | 前端构建配置问题 |
| 3. **订单管理 → 订单列表** | 列出订单数据 | 链路：前端 → coffee-app → userorder → RDS |
| 4. **轨迹查询 → 输入 `44556677`** | 查出轨迹数据 | 链路：前端 → coffee-app → **Dubbo RPC** → expresstrack → RDS（涉及跨服务 RPC） |

如果第 4 步能成功，说明 **Dubbo 服务注册与发现整条链路正常**——这是上云最关键的验证点。

### 9.2 Dubbo 服务治理验证

#### 路径 A（IaaS）的验证方式

**没有可视化页面，靠日志和 MSE Nacos 控制台**：

```bash
# 方式 1：MSE Nacos 控制台
# MSE 实例详情 → 服务管理 → 服务列表 → 应看到 coffee-userorder 等服务

# 方式 2：日志
~/coffee/manage.sh logs
# 找到包含 "Dubbo Application has completed" 的行即成功
```

#### 路径 B（PaaS）的验证方式

**EDAS 控制台 → 微服务治理 → Dubbo → 服务查询**：

1. 顶部"所属微服务空间"选 `coffee-prod`
2. 服务列表应出现订单、快递相关的 Dubbo 接口名
3. 点任一服务 → "提供者"标签 → 看到提供者 ECS IP

**这就是 PaaS 的核心价值**：服务调用关系一目了然，不用 SSH 上服务器看日志。

### 9.3 整体工作原理回顾（3 台 ECS 拓扑）

```
                          用户浏览器
                              │
                              │ http://<ECS-3 公网IP>:80
                              ▼
              ┌───────────────────────────────────┐
              │  ECS-3                            │
              │   ├─ Nginx :80 → 返回 Vue 静态文件 │
              │   └─ coffee-app :8005             │
              └────────────┬──────────────────────┘
                           │
              浏览器执行 JS → 发起 API 请求
                  http://<ECS-3 公网IP>:8005
                           │
                           ▼
              ┌─────────────────────────────┐
              │  coffee-app (ECS-3)         │
              │  ↓ Dubbo RPC（内网 VPC）    │
              └──────┬────────────────┬─────┘
                     │                │
        ┌────────────▼────┐    ┌──────▼─────────────┐
        │ ECS-1            │    │ ECS-2              │
        │ coffee-userorder │    │ coffee-expresstrack│
        │   ↓ JDBC（内网） │    │   ↓ JDBC（内网）   │
        └──────────┬───────┘    └──────────┬─────────┘
                   │                       │
                   ▼                       ▼
              ┌────────────────────────────────┐
              │       共用 RDS MySQL           │
              │  (userordertest + expresstracktest) │
              └────────────────────────────────┘

                3 台 ECS 互相发现的方式：
       ┌──────────────────────────────────────────┐
       │  MSE Nacos （所有服务都来这里注册和查询） │
       │  ├─ coffee-userorder       → ECS-1:20880  │
       │  ├─ coffee-expresstrack    → ECS-2:20880  │
       │  └─ coffee-app（consumer）→ ECS-3        │
       └──────────────────────────────────────────┘

注册中心怎么填上的（让 coffee-app 找到 provider）：
  路径 A：每台 ECS 上 manage.sh 通过 --dubbo.registry.address 注入 MSE 地址
  路径 B：每台 ECS 上 EDAS Agent 自动接管，重定向到微服务空间绑定的 MSE Nacos

最终 3 台 ECS 都注册到同一个 MSE Nacos，整条云上链路无需改一行代码。
```

**这就是云原生"环境无关"的核心**：同一份代码，本地一台机器跑 3 个进程，云上 3 台机器各跑 1 个进程，**只换启动参数和部署位置，代码不动**。

**回头看一眼对比表**，体会架构差异：

| 维度 | 本地（第 05 章） | 云上（本章） |
|------|--------------|-----------|
| 进程数量 | 3 个 | 3 个（一样！）|
| 机器数量 | 1 台笔记本 | **3 台 ECS** |
| 用什么区分服务 | 端口（7001/8001/8005） | **物理机器**（外加端口） |
| 服务挂了怎样 | 影响整个开发 | 只影响那一台 ECS，其他正常 |
| 注册中心 | 本地 Nacos | MSE Nacos |
| 数据库 | 本地 MySQL | RDS |
| 用户怎么访问 | localhost | ECS-3 的公网 IP |

---

## Part 10 进阶 — 云效流水线自动化

> 第一次上云可以跳过这一节。前面 Part 4 + Part 5/6 的"本地打包 + 手动上传"已经能把应用跑起来。本节进一步做"提交代码 → 自动构建 → 自动部署"——这才是完整的云原生交付链路。
>
> **前置条件**:Part 4.2/4.3 已经把 3 个库模块发布到了云效私有仓库(流水线机器要能拉到它们,不然构建直接失败)。

### 10.1 路径 A 的流水线:ssh + scp 自动重启

云效流水线选 **"主机部署"** 模板,为 3 台 ECS 分别配置(也可以做成一条流水线分发到 3 台):

| 步骤 | 内容 |
|------|------|
| 代码源 | 绑定你的 Git 仓库 |
| 构建 | `mvn clean deploy -DskipTests`(库模块发布到云效,应用模块同时打 jar) |
| 部署 | SSH 到对应 ECS → `scp` 新 jar 到 `~/coffee/jars/` → `~/coffee/manage.sh restart` |

例如 userorder 的流水线只 deploy 到 ECS-1,expresstrack 只到 ECS-2,coffee-app 只到 ECS-3。

### 10.2 路径 B 的流水线:直接部署到 EDAS

云效流水线选 **"Java 构建 + 部署到 EDAS"** 模板:

| 步骤 | 内容 |
|------|------|
| 代码源 | 绑定你的 Git 仓库 |
| 构建 | `mvn clean deploy -DskipTests` |
| 部署 | 选 **部署到 EDAS** → 目标应用:`userorder`/`expresstrack`/`coffee-app` → JVM 参数与 Part 6 一致 → 目标 ECS 也与 Part 6 一致(一应用一 ECS) |

跑通后,每次 `git push` 自动构建并部署到对应 ECS——这才是完整的云原生交付链路。

---

## 附录 A：三条路径对比速查表

| 维度 | 路径 A：IaaS（ECS） | 路径 B：PaaS（EDAS） | 路径 C：Serverless（SAE/FC） |
|------|-------------------|---------------------|----------------------------|
| **谁装 JDK** | 你 | EDAS（指定版本即可） | 平台 |
| **谁启动进程** | 你（manage.sh / systemd） | EDAS Agent | 平台（按请求触发） |
| **谁挂了谁重启** | 你（写脚本守护） | EDAS（自动重启） | 平台（按需新建实例） |
| **Dubbo 注册地址** | 启动参数显式指定 | Agent 自动接管 | 通常不用 Dubbo |
| **服务治理可视化** | ❌（自己看日志 / MSE 控制台） | ✅ EDAS 治理页面 | 有限 |
| **扩容方式** | 手动多台 scp | 控制台勾选更多 ECS | 自动 |
| **缩到 0 节省成本** | ❌ | ❌ | ✅ |
| **成本** | 最低（仅 ECS） | ECS + EDAS（节点费） | 按调用计费 |
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
4. **ECS 安全组出方向** 是否放通 8442、8443、8883
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

---

[← 返回主文档](../README.md) | [快速启动指南（本地）](05-quick-start.md)
