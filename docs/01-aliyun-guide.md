# 阿里云服务详细配置指南

> 本文档面向零基础同学，逐步说明项目用到的每一个阿里云服务的注册、配置和使用方式。每节先解释"这个服务是什么、为什么用它"，再给出操作步骤，最后提供常见问题排查方法。

---

## 目录

1. [注册阿里云账号](#1-注册阿里云账号)
2. [创建 RAM 子账号（推荐）](#2-创建-ram-子账号推荐)
3. [阿里云 RDS MySQL（云数据库）](#3-阿里云-rds-mysql云数据库)
4. [Nacos 服务注册中心](#4-nacos-服务注册中心)
5. [阿里云日志服务 SLS](#5-阿里云日志服务-sls)
6. [阿里云制品库（Maven 私服）](#6-阿里云制品库maven-私服)

---

## 1. 注册阿里云账号

1. 打开 [https://www.aliyun.com](https://www.aliyun.com)
2. 点击右上角"免费注册"
3. 使用手机号注册，完成实名认证（学生认证可获得免费资源）
4. 注册完成后，进入[控制台首页](https://console.aliyun.com)

> **学生优惠**：阿里云"云工开物"计划为学生提供免费或低价云产品，访问 [https://university.aliyun.com](https://university.aliyun.com) 了解详情。试用资源通常包括 ECS、RDS、日志服务等，足够完成本课程所有实验。

---

## 2. 创建 RAM 子账号（推荐）

### 为什么要创建子账号？

阿里云账号有两种层级：

| 类型 | 说明 | 风险 |
|------|------|------|
| 主账号（Root）| 拥有所有服务的完整权限，包括付款、注销账号 | 一旦泄露，他人可删除所有资源、产生大额账单 |
| RAM 子账号 | 只有被授权的那些权限 | 即使泄露，影响范围有限 |

**类比**：主账号相当于银行卡+密码，RAM 子账号相当于只能在特定商户消费的副卡。

### 操作步骤

1. 控制台搜索 **RAM 访问控制** → 进入
2. 左侧菜单：**用户** → 点击"创建用户"
3. 填写登录名（如 `cloudnativeapp-dev`），勾选"**OpenAPI 调用访问**"（编程访问）
4. 创建后，**立即保存 AccessKey ID 和 AccessKey Secret**（页面关闭后 Secret 永远无法再查看！）
5. 给子账号授权——在用户详情页点击"添加权限"：
   - `AliyunRDSFullAccess`（数据库读写权限）
   - `AliyunLogFullAccess`（日志服务权限）
   - `AliyunArtifactsFullAccess`（制品库权限）

### AccessKey 的使用原则

- **不要把 AccessKey 写入代码**，应通过环境变量注入（后续各节会说明）
- **不要把包含 AccessKey 的配置文件提交 Git**
- 定期轮换 AccessKey（每3个月建议更换一次）
- 本课程结束后，删除或禁用这个 RAM 子账号

---

## 3. 阿里云 RDS MySQL（云数据库）

### RDS 是什么？为什么用它？

**RDS**（Relational Database Service）是阿里云的云数据库服务。与本地安装 MySQL 相比：

| 对比项 | 本地 MySQL | 阿里云 RDS |
|--------|-----------|-----------|
| 部署 | 自己安装、配置、维护 | 购买即用，阿里云负责维护 |
| 可靠性 | 硬盘坏了数据丢失 | 自动多副本备份，99.95% 可用性 |
| 访问 | 只能本机访问 | 有外网地址，任意地点可连接 |
| 扩容 | 需要迁移数据 | 控制台一键升配 |
| 费用 | 免费（但自己维护）| 按配置付费（学习用最低配约 ¥70/月，或用试用额度） |

**本项目既可以用本地 MySQL，也可以用 RDS**。本地开发用本地 MySQL 更方便，部署到服务器时换成 RDS。

### 3.1 购买 RDS 实例

1. 控制台搜索 **RDS**
2. 点击"创建实例"
3. 选择配置：
   - 数据库类型：**MySQL 8.0**
   - 系列：基础版（单节点，学习用）
   - 规格：最低配置即可（1核2GB）
   - 地域：选择离你最近的城市（影响网络延迟）
   - 存储：20GB（默认，足够学习使用）
4. 完成购买，等待实例初始化（约 5 分钟）

### 3.2 配置白名单（必须做！）

> 不配置白名单，外部任何 IP 都无法连接数据库，这是 RDS 的安全保护机制。

1. 进入 RDS 实例详情 → 左侧"数据安全性" → "白名单设置"
2. 点击"修改"，添加你的本地 IP
   - 在浏览器访问 [https://www.ip.cn](https://www.ip.cn) 查询你的公网 IP
   - 格式：`123.45.67.89`（单个 IP）
3. 开发阶段可临时设置为 `0.0.0.0/0`（允许所有 IP，**生产环境绝对不能这样做**）

> **IP 会变！** 家庭宽带通常是动态 IP，每次断线重连 IP 会变。如果连接突然失败，先检查白名单里的 IP 是否还是你当前的 IP。

### 3.3 创建数据库和账号

**创建第一套（订单库）：**

1. 实例详情 → "账号管理" → "创建账号"
   - 账号名：`userordertest`，账号类型：普通账号，密码：自定义（记住！）
2. "数据库管理" → "创建数据库"
   - 数据库名：`userordertest`，字符集：`utf8mb4`，排序规则：`utf8mb4_unicode_ci`
3. 给账号授权：选择账号 `userordertest` → 修改权限 → 将 `userordertest` 库的**读写权限**授予该账号

**创建第二套（快递库）：** 同理创建 `expresstracktest` 数据库和账号。

### 3.4 获取连接地址（内网 vs 外网的区别）

RDS 提供两种连接地址，用途不同：

| 地址类型 | 格式 | 用途 | 费用 |
|---------|------|------|------|
| 内网地址 | `rm-xxx.mysql.rds.aliyuncs.com`（不含端口）| 同一 VPC 内的 ECS 连接，速度快 | 免费 |
| 外网地址 | `rm-xxx.mysql.rds.aliyuncs.com:3306` | 本地电脑、公网访问 | 少量流量费 |

- **本地开发**：必须用外网地址（本地电脑不在阿里云内网）
- **部署到 ECS**：用内网地址（同一地域的 ECS 与 RDS 在同一 VPC）

获取方式：实例详情 → "基本信息" → 找到"外网地址"，点击"申请外网地址"（如果还没开通）。

### 3.5 更新项目配置

打开 `coffee-userorder/provider/src/main/resources/application-dev.yml`：

```yaml
database:
  user: userordertest
  password: 你设置的密码
  host: rm-xxx.mysql.rds.aliyuncs.com:3306   # 替换为实际外网地址
  dbname: userordertest
```

同样修改 `coffee-expresstrack/provider/src/main/resources/application-dev.yml`（数据库改为 `expresstracktest`）。

### 3.6 MySQL 8.0 连接注意事项

MySQL 8.0 默认使用 `caching_sha2_password` 认证插件，比旧版的 `mysql_native_password` 更安全，但需要 SSL 或特殊参数支持。本项目 JDBC URL 中已包含所需参数：

```
allowPublicKeyRetrieval=true&useSSL=false
```

- `allowPublicKeyRetrieval=true`：允许客户端从服务器获取 RSA 公钥，`caching_sha2_password` 首次连接需要此参数
- `useSSL=false`：开发环境关闭 SSL（生产环境应设为 true）

如果你在 Navicat 中连接时提示"Authentication plugin 'caching_sha2_password' cannot be loaded"，在连接配置中勾选"使用旧版认证"，或者在 RDS 控制台将账号的认证方式改为 `mysql_native_password`。

### 3.7 连接失败排查

| 错误信息 | 原因 | 解决方法 |
|---------|------|---------|
| `Communications link failure` | 网络不通 | 检查白名单是否包含当前 IP |
| `Access denied for user` | 账号密码错误 | 核对 `application-dev.yml` 中的账号密码 |
| `Unknown database` | 数据库不存在 | 在 RDS 控制台创建数据库 |
| `Public Key Retrieval is not allowed` | 缺少连接参数 | JDBC URL 中添加 `allowPublicKeyRetrieval=true` |

---

## 4. Nacos 服务注册中心

### Nacos 是什么？解决什么问题？

**服务注册与发现**是微服务架构的核心问题：A 服务需要调用 B 服务，但 B 服务的 IP 和端口可能随时变化（扩容、迁移、重启），怎么办？

Nacos 的解决方案：

```
B 服务启动时 → 向 Nacos 登记："我叫 coffee-expresstrack，IP 是 192.168.1.5，端口 28888"
A 服务调用时 → 先问 Nacos："coffee-expresstrack 在哪？" → 得到地址 → 发起调用

B 服务宕机时 → Nacos 心跳检测失败 → 自动从注册表移除
A 服务下次调用时 → Nacos 不再返回 B 的地址 → 避免无效调用
```

**Nacos 的三大功能：**

| 功能 | 说明 | 本项目使用情况 |
|------|------|-------------|
| 服务注册与发现 | 管理微服务的地址 | ✅ 核心用途，Dubbo 依赖它 |
| 配置中心 | 集中管理配置文件，支持热更新 | ❌ 本项目使用本地配置文件 |
| 命名空间隔离 | dev/test/prod 环境隔离 | ❌ 本项目使用默认命名空间 |

### 4.1 本地安装（开发环境）

```bash
# 下载 Nacos 2.x（推荐 2.3.x 或更高）
# 下载地址：https://github.com/alibaba/nacos/releases

# 解压后，进入 bin 目录

# Windows 启动单机模式
startup.cmd -m standalone

# Mac/Linux 启动
sh startup.sh -m standalone
```

启动成功后，访问 [http://localhost:8848/nacos](http://localhost:8848/nacos)：
- 用户名：`nacos` / 密码：`nacos`

**关于 Nacos 数据持久化：**

Nacos 单机模式默认使用内嵌的 Derby 数据库保存数据（如配置信息）。**重要：服务注册信息存在内存中，Nacos 重启后需要各微服务重新向它注册**，这是正常现象，不需要担心数据丢失——只要你的微服务还在运行，重启 Nacos 后会自动重新注册（有约 5-10 秒延迟）。

**控制台功能说明：**

| 菜单 | 说明 |
|------|------|
| 服务管理 → 服务列表 | 查看所有已注册的微服务，及每个服务的实例列表、健康状态 |
| 配置管理 | 集中管理配置文件（本项目不使用）|
| 命名空间 | 隔离不同环境（dev/test/prod）|

**验证服务注册成功的方法：**

微服务启动后，在服务列表中确认：
1. 服务名称存在（如 `coffee-userorder`）
2. 实例数 ≥ 1
3. 健康实例数 = 实例数（健康实例数为 0 表示服务在跑但 Nacos 心跳异常）

### 4.2 部署到阿里云（生产环境）

生产环境建议使用阿里云 **MSE（微服务引擎）** 的 Nacos 托管版本，无需自行运维：

1. 控制台搜索 **MSE**
2. 创建 Nacos 实例（有免费试用额度，选择最低规格即可）
3. 获取连接地址，格式类似 `mse-xxx.nacos.mse.aliyuncs.com:8848`
4. 修改两个微服务和 coffee-app 的配置：

```yaml
# application.yml 中
dubbo:
  registry:
    address: nacos://mse-xxx.nacos.mse.aliyuncs.com:8848
```

**MSE vs 自建 Nacos 对比：**

| 对比项 | MSE 托管 | 自建 Nacos（ECS）|
|--------|---------|----------------|
| 运维 | 阿里云负责 | 自己负责（升级、备份、故障处理）|
| 高可用 | 内置集群，自动容灾 | 需要自己搭集群（至少3节点）|
| 费用 | 按实例规格付费 | ECS 费用（通常更便宜）|
| 适合场景 | 生产环境 | 学习/小型项目 |

---

## 5. 阿里云日志服务 SLS

### SLS 是什么？为什么不用本地日志文件？

**SLS**（Simple Log Service，日志服务）是阿里云的日志收集、存储和分析平台。

与本地日志文件相比：

| 对比项 | 本地日志文件 | 阿里云 SLS |
|--------|-----------|-----------|
| 存储位置 | 服务器磁盘（容量有限）| 云端（近乎无限容量）|
| 查询方式 | `grep` 命令（效率低）| SQL 风格的查询语言（秒级检索）|
| 多服务汇聚 | 需要登录不同服务器 | 所有服务的日志集中在一个界面 |
| 告警 | 需要自己写脚本 | 内置告警规则，异常自动通知 |
| 服务崩溃时 | 日志可能丢失 | 已推送到云端的日志不会丢 |

**本项目用 SLS 做什么：** 订单服务和快递服务的业务日志（INFO/ERROR 级别）实时推送到 SLS，可以在阿里云控制台集中查询和分析。

### 5.1 创建日志项目

1. 控制台搜索 **日志服务**
2. 点击"创建 Project"
   - Project 名称：`userorder`
   - 地域：**必须选择与你的服务器相同的地域**（不同地域无法内网通信，会走公网，有延迟和费用）
3. 在 Project 下点击"创建 Logstore"
   - Logstore 名称：`logs`
   - 数据保留时间：7天（学习用，减少费用）
   - 分片数量：1（学习用，生产环境根据吞吐量设置）

**Project 与 Logstore 的关系：**

```
Project（类比"文件夹"）
└── Logstore（类比"日志文件"）
    └── 日志条目
```

一个 Project 下可以有多个 Logstore，用于区分不同类型的日志（如访问日志、业务日志、错误日志）。

### 5.2 获取认证信息

使用第 2 节创建的 RAM 子账号的 AccessKey ID 和 AccessKey Secret。

**不要使用主账号的 AccessKey！** 万一泄露，他人拥有你所有云资源的操作权限。

### 5.3 配置项目日志推送

打开 `coffee-userorder/provider/src/main/resources/logback-spring.xml`，SLS Appender 配置如下：

```xml
<!--
    aliyun-log-logback-appender 0.1.27 配置
    不要把 AccessKey 直接写在这里——通过环境变量注入（见下方说明）
-->
<appender name="aliyun_sls" class="com.aliyun.openservices.log.logback.LoghubAppender">
    <endpoint>cn-beijing.log.aliyuncs.com</endpoint>        <!-- 替换为你的地域 Endpoint -->
    <accessKeyId>${ALIYUN_ACCESS_KEY_ID}</accessKeyId>
    <accessKeySecret>${ALIYUN_ACCESS_KEY_SECRET}</accessKeySecret>
    <projectName>userorder</projectName>                     <!-- 替换为你创建的 Project 名 -->
    <logstore>logs</logstore>                                <!-- 替换为你的 Logstore 名 -->
    <topic>订单</topic>
    <packageTimeoutInMS>3000</packageTimeoutInMS>
    <logsCountPerPackage>4096</logsCountPerPackage>
    <logsBytesPerPackage>3145728</logsBytesPerPackage>
    <retryTimes>3</retryTimes>
</appender>
```

**各地域 Endpoint 对照：**

| 地域 | Endpoint |
|------|---------|
| 华东1（杭州）| `cn-hangzhou.log.aliyuncs.com` |
| 华北2（北京）| `cn-beijing.log.aliyuncs.com` |
| 华南1（深圳）| `cn-shenzhen.log.aliyuncs.com` |
| 华东2（上海）| `cn-shanghai.log.aliyuncs.com` |

### 5.4 设置环境变量（注入 AccessKey）

**为什么用环境变量，不直接写在 XML 里？**

AccessKey 是访问阿里云的凭证，硬编码在代码里会被 Git 记录。即使你之后删除了，Git 历史里仍然存在。GitHub 的密钥扫描机器人会自动扫描公开仓库，发现泄露的 AccessKey 后阿里云会立即收到警告，甚至主动禁用。

**Windows 设置环境变量（两种方式）：**

方式一：命令行临时设置（只对当前终端有效）
```cmd
set ALIYUN_ACCESS_KEY_ID=your_key_id
set ALIYUN_ACCESS_KEY_SECRET=your_key_secret
```

方式二：永久设置（推荐）
```
我的电脑 → 右键"属性" → 高级系统设置 → 环境变量 → 系统变量 → 新建
变量名：ALIYUN_ACCESS_KEY_ID
变量值：你的 AccessKey ID
```
（设置后重启 IDEA 才能读取新环境变量）

**Linux/macOS 设置：**
```bash
# 临时（当前终端有效）
export ALIYUN_ACCESS_KEY_ID=your_key_id
export ALIYUN_ACCESS_KEY_SECRET=your_key_secret

# 永久（写入 ~/.bashrc 或 ~/.zshrc）
echo 'export ALIYUN_ACCESS_KEY_ID=your_key_id' >> ~/.bashrc
echo 'export ALIYUN_ACCESS_KEY_SECRET=your_key_secret' >> ~/.bashrc
source ~/.bashrc
```

### 5.5 在控制台查看和查询日志

1. 日志服务控制台 → 进入 Project `userorder` → Logstore `logs`
2. 点击"查询分析"

**常用查询示例：**

```sql
-- 查看所有 ERROR 级别日志
level: ERROR

-- 查看特定 topic 的日志
topic: 订单

-- 查看包含特定关键词的日志
topic: 订单 AND "ORDER001"

-- 查看最近1小时的错误
level: ERROR | SELECT * ORDER BY __time__ DESC LIMIT 100

-- 统计各级别日志数量
* | SELECT level, COUNT(*) as count GROUP BY level ORDER BY count DESC
```

### 5.6 日志推送失败的排查

如果日志没有出现在 SLS 控制台，按以下顺序排查：

1. **检查 Endpoint 是否与 Project 地域一致** — 这是最常见的错误
2. **检查环境变量是否正确设置** — 在 IDEA Terminal 里执行 `echo %ALIYUN_ACCESS_KEY_ID%`（Windows）或 `echo $ALIYUN_ACCESS_KEY_ID`（Linux），确认有值
3. **检查 RAM 权限** — 子账号必须有 `AliyunLogFullAccess` 权限
4. **检查网络** — 本地开发时需要能访问公网；阿里云 ECS 上建议用内网 Endpoint（格式：`cn-beijing-intranet.log.aliyuncs.com`）
5. **查看 logback 自身的日志** — SLS Appender 推送失败时会在本地控制台打印警告，但不会抛出异常（静默失败是设计行为）

---

## 6. 阿里云制品库（Maven 私服）

### 制品库是什么？为什么要用私服？

**制品库**（Artifact Repository）是存储 Maven 依赖包（JAR 文件）的私有仓库，作用类似于"公司内部的 Maven Central"。

**为什么需要它：**

- `coffee-app` 需要引用 `coffee-userorder-api` 和 `coffee-expresstrack-api`
- 本地开发时用 `mvn install` 安装到本地 `~/.m2/repository`，其他人的电脑上没有
- **如果多人协作或部署到 CI/CD**，私服让所有人能从同一个地方拉取内部依赖

**SNAPSHOT vs RELEASE 版本的区别：**

| 类型 | 版本号格式 | 特点 | 适用场景 |
|------|----------|------|---------|
| SNAPSHOT | `1.0.0-SNAPSHOT` | 可以被覆盖（同一版本号可以多次发布）| 开发中的版本，频繁迭代 |
| RELEASE | `1.0.0` | 不可变（发布后不能修改）| 稳定版本，正式交付 |

本项目各模块版本号带 `-SNAPSHOT`，表示还在开发中，可以反复发布。

### 6.1 创建制品仓库

1. 控制台搜索 **制品库**（或访问 [https://packages.aliyun.com](https://packages.aliyun.com)）
2. 创建两个仓库：
   - 类型：Maven，仓库名：`cloudnativeapp-snapshot`（SNAPSHOT 版本）
   - 类型：Maven，仓库名：`cloudnativeapp-release`（Release 版本）
3. 获取仓库 URL，格式类似：
   ```
   https://packages.aliyun.com/maven/repository/xxxxxx-snapshot-xxxxxx/
   ```

### 6.2 配置 pom.xml（发布目标）

将每个模块 `pom.xml` 中的 `distributionManagement` 替换为你的仓库地址（控制台会提供完整配置，直接复制即可）：

```xml
<distributionManagement>
    <snapshotRepository>
        <id>rdc-snapshots</id>
        <url>https://packages.aliyun.com/maven/repository/你的snapshot仓库地址/</url>
    </snapshotRepository>
    <repository>
        <id>rdc-releases</id>
        <url>https://packages.aliyun.com/maven/repository/你的release仓库地址/</url>
    </repository>
</distributionManagement>
```

### 6.3 配置 settings.xml 认证（发布和拉取都需要）

在 `~/.m2/settings.xml` 中添加服务器认证（`id` 必须与 pom.xml 中一致）：

```xml
<settings>
  <servers>
    <server>
      <id>rdc-snapshots</id>
      <username>你的阿里云账号或RAM子账号用户名</username>
      <password>你的密码</password>
    </server>
    <server>
      <id>rdc-releases</id>
      <username>你的阿里云账号或RAM子账号用户名</username>
      <password>你的密码</password>
    </server>
  </servers>

  <!-- 配置从私服拉取依赖 -->
  <profiles>
    <profile>
      <id>aliyun-artifacts</id>
      <repositories>
        <repository>
          <id>rdc-snapshots</id>
          <url>https://packages.aliyun.com/maven/repository/你的snapshot仓库地址/</url>
          <snapshots><enabled>true</enabled></snapshots>
          <releases><enabled>false</enabled></releases>
        </repository>
        <repository>
          <id>rdc-releases</id>
          <url>https://packages.aliyun.com/maven/repository/你的release仓库地址/</url>
          <snapshots><enabled>false</enabled></snapshots>
          <releases><enabled>true</enabled></releases>
        </repository>
      </repositories>
    </profile>
  </profiles>

  <activeProfiles>
    <activeProfile>aliyun-artifacts</activeProfile>
  </activeProfiles>
</settings>
```

> **只配发布还不够！** 很多同学只配了 `distributionManagement`（用于发布），却没有配 `repositories`（用于拉取）。结果发布成功了，但其他模块构建时仍然从 Maven Central 找依赖，找不到就报 `Could not find artifact`。两者都需要配置。

### 6.4 发布模块到私服

```bash
# 必须按依赖顺序发布

# 1. 公共库（最先发布，其他模块都依赖它）
cd coffee-common
mvn clean deploy -DskipTests

# 2. 订单服务 API
cd ../coffee-userorder/api
mvn clean deploy -DskipTests

# 3. 快递服务 API
cd ../../coffee-expresstrack/api
mvn clean deploy -DskipTests

# 发布完成后，在制品库控制台可以看到已上传的包
```

发布成功后，其他人克隆项目时，Maven 会自动从私服下载这些内部依赖，无需再手动执行 `mvn install`。

---

## 安全提示汇总

> **以下内容请务必遵守：**
>
> 1. **不要把 AccessKey 提交到 Git** — 一旦泄露，他人可能产生大额账单或删除你的数据
> 2. **不要把数据库密码写死在代码中** — 使用环境变量或阿里云配置中心
> 3. **生产环境关闭 RDS 公网访问** — 只允许同 VPC 内网访问，杜绝外网攻击
> 4. **白名单不要设置 `0.0.0.0/0`** — 开发完成后改回具体 IP
> 5. **定期轮换 AccessKey** — 降低泄露风险
> 6. **课程结束后删除或停用云资源** — 避免持续产生费用

---

[← 返回主文档](../README.md)
