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
> 你在第 05 章本地做过的事，本章里每一件都会一一对应到云上：

| 第 05 章本地做过的 | 本章云上对应替换为 | 在哪一节 |
|------------------|------------------|---------|
| 本地装的 MySQL 8.0（端口 3307） | 阿里云 RDS MySQL（内网地址 3306） | Part 3.2 |
| 本地启动的 nacos-server-2.x（127.0.0.1:8848） | MSE Nacos 托管实例 | Part 3.3 |
| 本地 `mvn package` 生成的 jar | **同一份 jar，不改一行代码** | Part 4 |
| 本地 `java -jar xxx.jar` 启动进程 | 路径 A：`manage.sh start` / 路径 B：EDAS 控制台部署 | Part 5 / Part 6 |
| 本地 `application-dev.yml`（连本地 MySQL / 本地 Nacos） | 同一文件 + 启动时注入云上地址，**或** 用 `application-prod.yml` | Part 5 / Part 6 |
| 本地 `npm run dev`（开发服务器 :8080） | `npm run build` 生成静态文件 → ECS + Nginx 托管 | Part 8 |
| 本地浏览器访问 `http://localhost:8080` | 浏览器访问 `http://<ECS公网IP>` | Part 9 |

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

| 资源 | 作用 | 路径 A 用 | 路径 B 用 | 在哪一节准备 |
|------|------|:---:|:---:|---|
| ECS 虚拟机 | 跑后端 jar / 前端 Nginx | ✅ | ✅ | 自备 |
| RDS MySQL | 云上数据库 | ✅ | ✅ | Part 3.2 |
| MSE Nacos | 云上注册中心 + 配置中心 | ✅ | ✅ | Part 3.3 |
| EDAS | 应用托管 + 服务治理 | ❌ | ✅ | Part 6 |

**只用一台 ECS：路径 A 和 路径 B 共用同一台 ECS 即可**——路径 B 的 EDAS 实际上也是把 jar 部署到你的 ECS 上跑（区别在于由 EDAS Agent 管理），所以两条路径在 ECS 这层是兼容的。

### 1.4 前置条件（缺一不可）

- [ ] **第 05 章已完成**：本地能启动三个后端服务并正常打开前端页面
- [ ] **阿里云账号**已注册并完成 **实名认证**
- [ ] 已购买 **ECS 实例**：推荐 **2 核 4 GB**，操作系统 **Alibaba Cloud Linux 3** 或 **Ubuntu 22.04**
- [ ] 已购买 **RDS MySQL 8.0**：推荐 1 核 2 GB 基础版（开发测试足够）
- [ ] 你的电脑上已装 **Maven 3.8+** 和 **Node.js 18+**（第 05 章已验证）

> 💡 **ECS 操作系统怎么选？** Alibaba Cloud Linux 3 是阿里云的官方推荐系统，软件源国内访问快；Ubuntu 22.04 命令熟悉度高，本章以 Ubuntu 为主线（apt 命令），Alibaba Cloud Linux 同学把 `apt` 换成 `yum / dnf` 即可。

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

### 3.1 准备 ECS

如果你已经买好了 ECS，跳过本节，只需确认：

- [ ] **地域**：记下来（如"华东1 杭州"），后续 RDS、MSE Nacos 全选这个
- [ ] **VPC**：ECS 控制台 → 实例详情 → 网络信息 → 看到 VPC ID，记下来
- [ ] **公网 IP**：用于你的电脑 SSH 进去和访问前端
- [ ] **私网 IP**：用于 RDS 白名单
- [ ] **登录密码**：root 密码或你创建的用户密码

> 💡 **没买过 ECS 的同学**：阿里云控制台 → 云服务器 ECS → 创建实例 → 选 **按量付费**（学完即释放，几元钱）→ 规格 **2 核 4 GB** → 镜像 **Ubuntu 22.04** → 公网 IP **分配**、带宽 **按流量** 1 Mbps → 设置 root 密码 → 创建。

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
2. 加入 **ECS 的私网 IP**（ECS 控制台 → 实例详情 → 私网 IP）
3. 也可以直接加整个 VPC 网段（如 `172.16.0.0/16`），允许 VPC 内所有机器访问，更省事

**确认**：白名单列表里出现了 ECS 的私网 IP。

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

## Part 4 本地打包

> 两条路径都要把 jar 准备好。这一节只做一次，产物给后面所有路径用。
>
> **🧭 对照第 05 章**：这一步的命令你已经在本地跑过一次了（第 05 章 Step 4 打包后 `java -jar` 启动 jar）。这里只是 **再打一次包**——产出的 jar **和本地启动用的是同一个文件**，**没有任何"云上专用版本"**。这正是云原生最关键的一句话："**Build once, run anywhere**"（构建一次，到处运行）。

### 4.1 模块依赖关系（为什么要按顺序打包）

```
coffee-common  ←─────────┐  被所有模块引用
                         │
coffee-userorder/api     ┤
       │                 │
       ▼                 │
coffee-userorder/provider├──► 产物 jar
                         │
coffee-expresstrack/api  ┤
       │                 │
       ▼                 │
coffee-expresstrack/provider──► 产物 jar
                         │
coffee-app ──────────────┘──► 产物 jar
（依赖两个 api 模块以发起 Dubbo 调用）
```

**规则**：被依赖的模块要先 `mvn install`（装进本地 Maven 仓库），上层模块才能找到它。

### 4.2 一键打包

在项目根目录（`cloudnativeapp/`）打开命令行（CMD / PowerShell / 终端均可），**按顺序执行**：

```cmd
rem ① 安装公共依赖与 API 接口包（被三个 provider/网关引用）
cd coffee-common
mvn clean install -DskipTests

cd ..\coffee-userorder\api
mvn clean install -DskipTests

cd ..\..\coffee-expresstrack\api
mvn clean install -DskipTests
```

```cmd
rem ② 打包三个可部署的 jar
cd ..\..\coffee-userorder\provider
mvn clean package -DskipTests

cd ..\..\coffee-expresstrack\provider
mvn clean package -DskipTests

cd ..\..\coffee-app
mvn clean package -DskipTests
```

> 💡 **Linux/Mac 同学** 把 `\` 换成 `/`，命令完全一样。

**确认产物**：每个 `mvn` 命令末尾出现 `BUILD SUCCESS`，并且：

| 服务 | jar 文件位置 |
|------|------------|
| 订单服务 | `coffee-userorder/provider/target/coffee-userorder-provider-1.0-SNAPSHOT.jar` |
| 快递服务 | `coffee-expresstrack/provider/target/coffee-expresstrack-provider-1.0-SNAPSHOT.jar` |
| API 网关 | `coffee-app/target/coffee-app-0.0.1-SNAPSHOT.jar` |

> ⚠️ **必须用 `mvn` 不要用 `mvnw`**：`mvnw`（Maven Wrapper）会联网下载 Maven，国内常超时。

---

## Part 5 路径 A — IaaS 上云（ECS + manage.sh）

> 这条路径让你 **亲手触摸"服务运行"的每一层**：装 JDK、上传 jar、写启动脚本、管理进程、看日志。
>
> **🧭 对照第 05 章**：你在本地是这样启动服务的：
> ```bash
> # 本地（第 05 章 Step 5）：
> cd coffee-userorder/provider/target
> java -jar coffee-userorder-provider-1.0-SNAPSHOT.jar
> ```
> 路径 A 在 ECS 上做的事 **本质完全一样**，只是：
> - 你的笔记本 ──► 换成 ECS（一台 Linux 远程机器）
> - 双击启动 ──► SSH 进去用脚本启动
> - 本地 MySQL（127.0.0.1:3307）──► RDS 内网地址（rm-xxx:3306）
> - 本地 Nacos（127.0.0.1:8848）──► MSE Nacos 内网地址
>
> 看懂这个对应关系，下面的步骤就只是"把熟悉的事换个地方做"。

### 5.1 这条路径的特点

```
你做的事                         结果
────────                       ────
SSH 登录 ECS                  → 拿到一台空白 Linux 机器
apt install openjdk-17        → 装好 Java 运行时
scp 上传 jar                   → jar 出现在 /opt/coffee/jars/
nohup java -jar &              → 进程跑起来
配置 Nacos 地址                → 服务能找到注册中心
```

**好处**：

- ✅ 完全控制每一层，调试方便
- ✅ 成本低（一台 ECS 跑三个服务，月费几十元）
- ✅ 学习价值高，理解"应用是怎么活在 Linux 上的"

**代价**：

- ❌ JDK、glibc、磁盘 都要你自己维护
- ❌ 服务挂了要自己重启
- ❌ 想加机器要自己一台台 scp
- ❌ 没有可视化的服务治理页面（要靠日志看）

### 5.2 在 ECS 上安装 JDK 17

SSH 登录 ECS（Windows 同学用 **WinSCP** 或 **PuTTY** 或 PowerShell 内置的 `ssh root@<ECS公网IP>`）：

```bash
# Ubuntu / Debian
sudo apt update
sudo apt install -y openjdk-17-jdk

# Alibaba Cloud Linux 3 / CentOS
sudo dnf install -y java-17-openjdk-devel

# 确认
java -version
```

输出包含 **17.0.x** 即成功。

> ❓ **为什么要 JDK 17？** 本项目用 Spring Boot 3.x，要求 JDK 17+。**JDK 8 / 11 启动会报 `UnsupportedClassVersionError`**。

### 5.3 在 ECS 上规划目录结构

项目里已经准备好了一整套部署文件，在 `cloudnativeapp/deploy/` 目录。最终 ECS 上的目录长这样：

```
/opt/coffee/
├── jars/                                       ← 你的三个 jar 放这里
│   ├── coffee-userorder-provider-1.0-SNAPSHOT.jar
│   ├── coffee-expresstrack-provider-1.0-SNAPSHOT.jar
│   └── coffee-app-0.0.1-SNAPSHOT.jar
├── config/
│   ├── userorder/
│   │   └── application-dev.yml                 ← 填你的 RDS 信息
│   └── expresstrack/
│       └── application-dev.yml                 ← 填你的 RDS 信息
├── logs/                                       ← 启动后自动生成
└── manage.sh                                   ← 一键管理脚本
```

在 ECS 上预先建好目录：

```bash
sudo mkdir -p /opt/coffee/jars
sudo chown -R $USER:$USER /opt/coffee
```

### 5.4 把 deploy/ 目录上传到 ECS

在你本地项目根目录执行（替换 `<ECS_IP>` 为你的 ECS 公网 IP）：

```bash
# 上传 deploy/ 下的脚本和配置模板
scp -r deploy/manage.sh deploy/config root@<ECS_IP>:/opt/coffee/
```

> 💡 **Windows 同学没装 scp？** PowerShell 7 自带；或用 **WinSCP**（图形界面拖拽即可）。

**确认**：SSH 登录 ECS 后 `ls /opt/coffee/` 能看到 `manage.sh` 和 `config/`。

### 5.5 上传打包产物（三个 jar）

继续在本地执行：

```bash
scp coffee-userorder/provider/target/coffee-userorder-provider-1.0-SNAPSHOT.jar      root@<ECS_IP>:/opt/coffee/jars/
scp coffee-expresstrack/provider/target/coffee-expresstrack-provider-1.0-SNAPSHOT.jar root@<ECS_IP>:/opt/coffee/jars/
scp coffee-app/target/coffee-app-0.0.1-SNAPSHOT.jar                                   root@<ECS_IP>:/opt/coffee/jars/
```

每个 jar 大约 40–80 MB，按你的网速可能需要几十秒。

**确认**：ECS 上 `ls -lh /opt/coffee/jars/` 能看到三个 jar 文件。

### 5.6 配置 Nacos 地址

SSH 登录 ECS，编辑 `manage.sh`：

```bash
nano /opt/coffee/manage.sh
```

找到第 16 行（在脚本顶部的"学生必填配置区"）：

```bash
NACOS_ADDR="mse-xxxxxxxx-p.nacos-ans.mse.aliyuncs.com:8848"
```

**改成你在 Part 3.3 记下的内网地址**。保存退出（nano：`Ctrl+O` 保存，`Ctrl+X` 退出）。

> ❓ **这里为什么不用公网地址？** ECS 和 MSE Nacos 在同一 VPC，走内网更快更稳。公网地址是给你本地电脑临时调试用的。

### 5.7 填写数据库配置

ECS 上编辑两个数据库配置文件：

```bash
nano /opt/coffee/config/userorder/application-dev.yml
nano /opt/coffee/config/expresstrack/application-dev.yml
```

把 `yourDbUser`、`yourDbPassword`、`your-rds-address` 替换为你在 Part 3.2 记下的实际值。

例如 `userorder/application-dev.yml` 改完后是：

```yaml
database:
  user:     ${DB_USER:userordertest}
  password: ${DB_PASSWORD:你的实际密码}
  host:     ${DB_HOST:rm-xxxxx.mysql.rds.aliyuncs.com:3306}
  dbname:   ${DB_NAME:userordertest}
```

> ❓ **为什么把数据库配置放成一个独立文件？** 这是 manage.sh 的设计：把"环境敏感信息"和 jar 分离，**改密码不用重新打包**。脚本启动时用 `--spring.config.additional-location=file:/opt/coffee/config/userorder/` 加载这个文件，覆盖 jar 内的默认值。

### 5.8 启动服务

```bash
chmod +x /opt/coffee/manage.sh
/opt/coffee/manage.sh start
```

`manage.sh` 会按顺序启动三个服务：**userorder（7001）→ expresstrack（8001）→ app（8005）**。每个服务有 30 秒超时检测，启动成功会看到：

```
  [→] userorder: 正在启动（端口=7001）...
  [✓] userorder: 启动成功 (PID=12345, 端口=7001)
  [→] expresstrack: 正在启动（端口=8001）...
  [✓] expresstrack: 启动成功 (PID=12346, 端口=8001)
  [→] app: 正在启动（端口=8005）...
  [✓] app: 启动成功 (PID=12347, 端口=8005)
```

### 5.9 日常管理命令

| 命令 | 作用 |
|------|------|
| `/opt/coffee/manage.sh status` | 查看三个服务在不在跑 |
| `/opt/coffee/manage.sh logs` | 实时跟踪三个服务的日志（Ctrl+C 退出） |
| `/opt/coffee/manage.sh stop` | 停止所有服务 |
| `/opt/coffee/manage.sh restart` | 重启所有服务 |

**确认整体跑通**：浏览器访问 `http://<ECS_IP>:8005/actuator/health`，返回 `{"status":"UP"}` 即正常。

> ⚠️ **第一次启动前要做的两件事**：
> 1. ECS 安全组 **入方向** 开放 7001、8001、8005 三个端口（如果想从外部访问健康检查）
> 2. 确认 RDS 白名单已加 ECS 私网 IP（Part 3.2 步骤 ④）

### 5.10 路径 A 工作原理图解

```
你执行 ./manage.sh start
        │
        ▼
manage.sh 对每个服务执行：
   java -jar xxx.jar \
       --dubbo.registry.address=nacos://<你的MSE地址>:8848 \
       --spring.config.additional-location=file:/opt/coffee/config/xxx/
        │
        ▼
Spring Boot 启动（dev profile）
   ├─ 读取 jar 内的 application.yml + application-dev.yml
   ├─ 用 /opt/coffee/config/xxx/application-dev.yml 覆盖数据库段
   └─ 命令行参数 --dubbo.registry.address 覆盖 Dubbo 注册地址
        │
        ▼
        ├─► Dubbo 用接口级模式向 MSE Nacos 注册
        ├─► MyBatis 连接 RDS（内网地址，VPC 直通）
        └─► HTTP 监听 7001/8001/8005
        │
        ▼
进程通过 nohup 后台运行，PID 写入 /opt/coffee/xxx.pid
```

**这条路径的关键认识**：
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

### 6.2 配置 ECS 安全组（出方向）

EDAS Agent 需要主动连阿里云上报数据。**必须** 在 ECS 安全组放通 **出方向** 三个端口：

**ECS 控制台 → 安全组 → 配置规则 → 出方向 → 手动添加**：

| 端口 | 协议 | 授权对象 | 说明 |
|------|------|---------|------|
| 8442 | TCP | 0.0.0.0/0 | EDAS Agent 控制通道 |
| 8443 | TCP | 0.0.0.0/0 | EDAS Agent 控制通道 |
| 8883 | TCP | 0.0.0.0/0 | 服务元数据上报 |

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

### 6.4 创建 EDAS ECS 集群并导入 ECS

**EDAS 控制台 → 资源管理 → EDAS ECS 集群 → 创建集群**：

| 配置项 | 填写值 |
|--------|--------|
| 集群名称 | `coffeecluster` |
| 微服务空间 | 选 `coffee-prod`（上一步建的） |
| 网络类型 | 专有网络 |
| VPC | 与 ECS 相同 |

> ⚠️ **集群的微服务空间一旦确定不可改**。选错只能删了重建。

创建后：进入集群详情 → **添加 ECS** → 勾选你的 ECS → 确认 → 等 3–5 分钟，EDAS 自动在 ECS 上安装 Agent。

**确认**：ECS 状态变为"运行中"，集群详情显示"已安装 Agent"。

> ⚠️ **如果路径 A 还在跑**：导入 EDAS 之前最好先 `manage.sh stop` 停掉路径 A 的进程，避免端口冲突和 Dubbo 注册冲突。

### 6.5 部署三个应用

**EDAS 控制台 → 应用管理 → 创建应用**，依次创建三个应用。

#### 6.5.1 部署订单服务（coffee-userorder）

| 配置项 | 填写值 |
|--------|--------|
| 应用名称 | `userorder` |
| 部署环境 | ECS 集群 → `coffeecluster` |
| 应用运行环境 | Java |
| JDK 版本 | OpenJDK 17 |
| 部署包类型 | JAR 包 |
| 部署包 | 上传 `coffee-userorder/provider/target/coffee-userorder-provider-1.0-SNAPSHOT.jar` |
| JVM 参数 | 见下方 |

**JVM 参数**（把 RDS 内网地址和密码换成你的实际值）：

```
-Xms128m -Xmx512m -DENV=prod -DDB_HOST=rm-xxxxx.mysql.rds.aliyuncs.com:3306 -DDB_USER=userordertest -DDB_PASSWORD=你的RDS密码
```

点 **部署**，等状态变为 **运行中**。

> ❓ **JVM 参数为什么这么写？**
> - `-Xms128m -Xmx512m`：堆内存范围（够用就行，开发测试 2GB ECS 跑三个服务需要省着用）
> - `-DENV=prod`：激活 `application-prod.yml`（与路径 A 的 dev profile 不同——EDAS 走"标准 prod 路径"）
> - `-DDB_*`：覆盖 `application-prod.yml` 里的 RDS 占位地址
> - **没写 `-DDUBBO_REGISTRY`**：因为 **EDAS Agent 会自动拦截 Dubbo 注册**，把它指向微服务空间绑定的 MSE Nacos，写了也会被覆盖

#### 6.5.2 部署快递服务（coffee-expresstrack）

| 配置项 | 填写值 |
|--------|--------|
| 应用名称 | `expresstrack` |
| 部署包 | `coffee-expresstrack/provider/target/coffee-expresstrack-provider-1.0-SNAPSHOT.jar` |
| JVM 参数 | 与 userorder **完全相同**（数据库名在 `application-prod.yml` 里已硬编码为 `expresstracktest`） |

#### 6.5.3 部署 API 网关（coffee-app）

| 配置项 | 填写值 |
|--------|--------|
| 应用名称 | `coffee-app` |
| 部署包 | `coffee-app/target/coffee-app-0.0.1-SNAPSHOT.jar` |
| JVM 参数 | `-Xms128m -Xmx256m -DENV=prod` |

> ❓ **网关为什么参数这么短？** `coffee-app` 是纯消费者，**不连数据库**，所以不需要任何 `-DDB_*` 参数。Dubbo 注册同样由 Agent 接管。

> **建议部署顺序**：先两个 provider，最后 coffee-app（消费者）。顺序反了也能启动，只是日志里会有几秒"找不到服务提供者"的警告。

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

## Part 8 部署前端到 ECS（路径共用）

> 不管后端走哪条路径，前端都部署到 ECS 上由 Nginx 静态托管。
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
> - localhost:8080 上的开发服务器 ──► ECS 上的 Nginx（专门发静态文件的 Web 服务器）
> - 后端 API 调用 `http://localhost:8005`（本地） ──► `http://<ECS公网IP>:8005`（云上）
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

把 `x.x.x.x` 换成你的 **ECS 公网 IP**：

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

### 8.3 上传 dist/ 到 ECS

用 **WinSCP**（Windows 图形化 SFTP 工具，winscp.net 下载安装）：

1. WinSCP → 新建站点 → SFTP / 你的 ECS 公网 IP / 22 / root / ECS 密码
2. 右侧（ECS）建目录 `/var/www/coffee-admin/`
3. 把左侧（本地）`app-admin/dist/` 下的 **所有文件和文件夹** 全选拖到右侧

**Linux/Mac 同学** 用 scp：
```bash
scp -r app-admin/dist/* root@<ECS_IP>:/var/www/coffee-admin/
```

**确认**：ECS 上 `ls /var/www/coffee-admin/` 能看到 `index.html` 和 `static/`。

### 8.4 安装并配置 Nginx

SSH 登录 ECS：

```bash
# Ubuntu
sudo apt install -y nginx

# Alibaba Cloud Linux
sudo dnf install -y nginx
```

创建站点配置：

```bash
sudo tee /etc/nginx/conf.d/coffee-admin.conf > /dev/null << 'EOF'
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

启动：

```bash
sudo nginx -t                 # 检查语法
sudo systemctl start nginx
sudo systemctl enable nginx   # 开机自启
```

### 8.5 安全组放通入方向端口

**ECS 控制台 → 安全组 → 入方向 → 手动添加**：

| 端口 | 协议 | 授权对象 | 说明 |
|------|------|---------|------|
| 80 | TCP | 0.0.0.0/0 | Nginx 对外提供前端 |
| 8005 | TCP | 0.0.0.0/0 | coffee-app REST API（浏览器从前端调用） |

> 💡 **本地调试还要放通 22**（SSH），不过 ECS 默认就开着 22。

**确认**：浏览器访问 `http://<ECS公网IP>`，应看到 CoffeeTrack 登录页。

---

## Part 9 验证

### 9.1 端到端业务验证（路径 A、B 都用）

浏览器访问 `http://<ECS公网IP>`：

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
/opt/coffee/manage.sh logs
# 找到包含 "Dubbo Application has completed" 的行即成功
```

#### 路径 B（PaaS）的验证方式

**EDAS 控制台 → 微服务治理 → Dubbo → 服务查询**：

1. 顶部"所属微服务空间"选 `coffee-prod`
2. 服务列表应出现订单、快递相关的 Dubbo 接口名
3. 点任一服务 → "提供者"标签 → 看到提供者 ECS IP

**这就是 PaaS 的核心价值**：服务调用关系一目了然，不用 SSH 上服务器看日志。

### 9.3 整体工作原理回顾

```
用户浏览器
  │ http://<ECS公网IP>:80
  ▼
Nginx（ECS:80）→ 返回 Vue 静态文件
  │
浏览器执行 JS → 发起 API 请求
  │ http://<ECS公网IP>:8005
  ▼
coffee-app（ECS:8005）─── Dubbo RPC ──► coffee-userorder（ECS:7001）─── JDBC ──► RDS
                                  └──► coffee-expresstrack（ECS:8001）─── JDBC ──► RDS

注册中心（让 coffee-app 找到 provider）：
  路径 A：代码里指定 Nacos 地址，由 manage.sh 通过 --dubbo.registry.address 注入
  路径 B：EDAS Agent 自动接管，重定向到微服务空间绑定的 MSE Nacos

最终都注册到 MSE Nacos，整条云上链路无需改一行代码。
```

**这就是云原生"环境无关"的核心**：同一份代码，本地连本地，云上连云上，只换启动参数。

---

## Part 10 进阶 — 云效流水线自动化

> 第一次上云完全可以跳过这一节。第 5 / 6 节的"本地打包 + 手动上传"已经能把应用跑起来。本节介绍更"云原生"的做法：把制品发布到云上私有仓库，再用流水线实现"提交代码 → 自动构建 → 自动部署"。

### 10.1 公共步骤：私有制品仓库

#### 为什么需要私有仓库

第 05 章本地开发时，`mvn install` 把 `coffee-common`、各 `api` 装进了 **你电脑的本地仓库**（`~/.m2`），其他模块从本地找到它们。

但 **云上流水线每次都是一台全新的干净机器**，没有你的本地仓库。要让它能构建依赖了 `coffee-common` 的模块，就得把 `coffee-common` 等内部包 **发布（`mvn deploy`）到云上仓库**——这个云上仓库就是 **云效制品仓库**。

这正是项目里 `pom.xml` 的 `<distributionManagement>` 的作用——声明"执行 `mvn deploy` 时把包推到哪里"。

> **第 05 章和 Part 4 用的都是 `install` / `package`，根本不读 `<distributionManagement>`**，所以本地开发哪怕没配云效仓库也跑得通。`mvn deploy` 才会用到它。

#### 创建云效制品仓库

1. 登录 [云效](https://flow.aliyun.com) → 进入 **制品仓库 Packages**
2. 左侧 **Maven → 创建仓库**
   - 类型选 **Snapshot**（本项目版本号都是 `1.0-SNAPSHOT`）
   - 记下 **仓库地址** 形如 `https://packages.aliyun.com/<命名空间>/maven/<仓库名>`
3. 点页面的 **指南** → 获取 **用户名 + 密码**（云效专用，**不是阿里云登录密码**）

#### 配置 Maven settings.xml

打开 Maven 的 `settings.xml`（位于 `~/.m2/settings.xml` 或 Maven 安装目录的 `conf/settings.xml`）：

```xml
<servers>
  <server>
    <id>aliyun-snapshot</id>             <!-- 必须与 pom 中 distributionManagement 的 id 一致 -->
    <username>云效给的用户名</username>
    <password>云效给的密码</password>
  </server>
</servers>

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

#### 发布制品

按依赖顺序：

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

成功后，回云效制品仓库页面，能看到 `com.coffee.yun` 下的所有制品。

### 10.2 路径 A 的流水线：ssh + scp 自动重启

云效流水线选 **"主机部署"** 模板，配置：

| 步骤 | 内容 |
|------|------|
| 代码源 | 绑定你的 Git 仓库 |
| 构建 | `mvn clean deploy -DskipTests` |
| 部署 | SSH 到 ECS → 执行 `scp 新 jar 到 /opt/coffee/jars/` → `/opt/coffee/manage.sh restart` |

### 10.3 路径 B 的流水线：直接部署到 EDAS

云效流水线选 **"Java 构建 + 部署到 EDAS"** 模板：

| 步骤 | 内容 |
|------|------|
| 代码源 | 绑定你的 Git 仓库 |
| 构建 | `mvn clean deploy -DskipTests` |
| 部署 | 选 **部署到 EDAS** → 目标应用：`userorder` / `expresstrack` / `coffee-app` → JVM 参数与第 6 节一致 |

跑通后，每次 `git push` 自动构建并部署——这才是完整的云原生交付链路。

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
- `tail -100 /opt/coffee/logs/xxx.log` 看真实错误
- 最常见：数据库密码不对 / Nacos 地址不对 / 端口被另一个进程占用（`ss -tlnp | grep 7001`）

**Q：服务启动了但 MSE Nacos 看不到注册**
- 确认 `manage.sh` 里 `NACOS_ADDR` 改成了你自己的地址（不是模板的 `mse-xxxxxxxx`）
- 看日志里是否有 `Failed to connect to nacos`

**Q：怎么知道服务真在跑**
- `/opt/coffee/manage.sh status`
- `curl http://localhost:7001/actuator/health`

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
