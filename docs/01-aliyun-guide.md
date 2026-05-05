# 阿里云服务详细配置指南

> 本文档面向零基础同学，逐步说明项目用到的每一个阿里云服务的注册、配置和使用方式。

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

> **学生优惠**：阿里云"云工开物"计划为学生提供免费或低价云产品，
> 访问 [https://university.aliyun.com](https://university.aliyun.com) 了解详情。

---

## 2. 创建 RAM 子账号（推荐）

**为什么要创建子账号？**
主账号权限太大，一旦泄露后果严重。建议为每个项目创建一个只有必要权限的子账号。

**操作步骤：**

1. 控制台搜索 **RAM 访问控制** → 进入
2. 左侧菜单：**用户** → 点击"创建用户"
3. 填写登录名（如 `cloudnativeapp-dev`），勾选"编程访问"
4. 创建后，**立即保存 AccessKey ID 和 AccessKey Secret**（关闭页面后无法再查看！）
5. 给子账号授权：
   - 搜索 `AliyunRDSFullAccess`（数据库权限）
   - 搜索 `AliyunLogFullAccess`（日志服务权限）
   - 搜索 `AliyunArtifactsFullAccess`（制品库权限）

---

## 3. 阿里云 RDS MySQL（云数据库）

### 3.1 购买 RDS 实例

1. 控制台搜索 **RDS**
2. 点击"创建实例"
3. 选择配置：
   - 数据库类型：**MySQL 8.0**（项目已升级至 MySQL 8 驱动，推荐选择 8.0）
   - 规格：最低配置即可（学习用）
   - 地域：选择离你最近的城市
4. 完成购买，等待实例初始化（约 5 分钟）

### 3.2 配置白名单（必须做！）

> 不配置白名单，本地电脑无法连接数据库。

1. 进入 RDS 实例详情 → 左侧"数据安全性" → "白名单设置"
2. 点击"修改"，添加你的本地 IP（可在 [https://www.ip.cn](https://www.ip.cn) 查询）
3. 开发阶段可临时设置为 `0.0.0.0/0`（允许所有IP，**生产环境绝对不能这样做**）

### 3.3 创建数据库和账号

1. 实例详情 → "账号管理" → "创建账号"
   - 账号名：`userordertest`，密码：自定义
2. "数据库管理" → "创建数据库"
   - 数据库名：`userordertest`，字符集：`utf8mb4`
3. 给账号授权：选择账号 → 修改权限 → 将 `userordertest` 库的读写权限授予对应账号

> 同理创建 `expresstracktest` 数据库和账号。

### 3.4 获取连接地址

1. 实例详情 → "基本信息"
2. 找到"外网地址"（格式：`rm-xxx.mysql.rds.aliyuncs.com:3306`）
3. 将此地址填入 `application-dev.yml` 的 `database.host` 字段

### 3.5 更新项目配置

打开 `coffee-userorder/provider/src/main/resources/application-dev.yml`：

```yaml
database:
  user: userordertest          # 你创建的账号名
  password: 你设置的密码
  host: rm-xxx.mysql.rds.aliyuncs.com:3306   # 替换为实际外网地址
  dbname: userordertest
```

同样修改 `coffee-expresstrack/provider/src/main/resources/application-dev.yml`。

---

## 4. Nacos 服务注册中心

### 4.1 本地安装（开发环境）

```bash
# 下载 Nacos 2.x（Dubbo 3.3.x 要求 Nacos 2.x，不兼容 1.x）
# 下载地址：https://github.com/alibaba/nacos/releases
# 推荐版本：2.3.x 或更高

# 解压后，进入 bin 目录

# Windows 启动单机模式
startup.cmd -m standalone

# Mac/Linux 启动
sh startup.sh -m standalone
```

> **版本注意**：Dubbo 3.3.x 使用 gRPC 风格的三元组协议，需要 Nacos 2.x 才能正确注册和发现服务。如果使用 Nacos 1.x，服务列表页面将看不到注册的服务。

启动成功后，访问 [http://localhost:8848/nacos](http://localhost:8848/nacos)：
- 用户名：`nacos`
- 密码：`nacos`

**控制台功能说明：**

| 菜单 | 说明 |
|------|------|
| 服务管理 → 服务列表 | 查看所有已注册的微服务 |
| 配置管理 | 集中管理配置文件（高级用法）|
| 命名空间 | 隔离不同环境（dev/test/prod）|

### 4.2 部署到阿里云（生产环境）

如果项目部署到阿里云服务器，可以使用阿里云 **MSE（微服务引擎）** 的 Nacos 托管版本：

1. 控制台搜索 **MSE**
2. 创建 Nacos 实例（有免费试用额度）
3. 获取连接地址，格式类似 `mse-xxx.nacos.mse.aliyuncs.com:8848`
4. 修改 `application.yml`：
   ```yaml
   dubbo:
     registry:
       address: nacos://mse-xxx.nacos.mse.aliyuncs.com:8848
   ```

---

## 5. 阿里云日志服务 SLS

### 5.1 创建日志项目

1. 控制台搜索 **日志服务**
2. 点击"创建 Project"
   - Project 名称：`userorder`
   - 地域：选择与服务器相同的地域
3. 在 Project 下点击"创建 Logstore"
   - Logstore 名称：`logs`
   - 数据保留时间：7天（学习用）

### 5.2 获取认证信息

使用第 2 节创建的 RAM 子账号的 AccessKey ID 和 Secret。

### 5.3 修改项目日志配置

打开 `coffee-userorder/provider/src/main/resources/logback-spring.xml`，找到以下部分并替换：

```xml
<!--
    aliyun-log-logback-appender 0.1.27 配置
    注意：0.1.16+ 版本将 <accessKey> 改名为 <accessKeySecret>
    不要把 AccessKey 直接写在这里——通过环境变量注入：
      export ALIYUN_ACCESS_KEY_ID=your_key_id
      export ALIYUN_ACCESS_KEY_SECRET=your_key_secret
-->
<appender name="aliyun_sls" class="com.aliyun.openservices.log.logback.LoghubAppender">
    <endpoint>cn-beijing.log.aliyuncs.com</endpoint>        <!-- 替换为你的地域 Endpoint -->
    <accessKeyId>${ALIYUN_ACCESS_KEY_ID}</accessKeyId>
    <accessKeySecret>${ALIYUN_ACCESS_KEY_SECRET}</accessKeySecret>
    <projectName>userorder</projectName>                     <!-- 替换为你创建的 Project 名 -->
    <logstore>logs</logstore>                                <!-- 替换为你的 Logstore 名 -->
    <topic>订单</topic>
</appender>
```

> **配置项变更说明**：旧版本（0.1.15 及以前）使用 `<accessKey>` 标签；从 0.1.16 开始改为 `<accessKeySecret>`。当前项目使用 0.1.27，请确保使用新的标签名，否则密钥无法读取，日志会推送失败但不报错。

**各地域 Endpoint 对照：**

| 地域 | Endpoint |
|------|---------|
| 华东1（杭州）| `cn-hangzhou.log.aliyuncs.com` |
| 华北2（北京）| `cn-beijing.log.aliyuncs.com` |
| 华南1（深圳）| `cn-shenzhen.log.aliyuncs.com` |
| 华东2（上海）| `cn-shanghai.log.aliyuncs.com` |

### 5.4 在控制台查看日志

1. 日志服务控制台 → 进入 Project → Logstore
2. 点击"查询分析" → 输入查询语句，如：
   ```
   topic: 订单 AND level: ERROR
   ```

---

## 6. 阿里云制品库（Maven 私服）

### 6.1 创建制品仓库

1. 控制台搜索 **制品库**（或访问 [https://packages.aliyun.com](https://packages.aliyun.com)）
2. 创建仓库：
   - 类型：Maven
   - 仓库名：`cloudnativeapp-snapshot`（SNAPSHOT版本）
   - 仓库名：`cloudnativeapp-release`（Release版本）
3. 获取仓库 URL，格式类似：
   ```
   https://packages.aliyun.com/maven/repository/xxxxxx-snapshot-xxxxxx/
   ```

### 6.2 配置 pom.xml

将每个模块 `pom.xml` 中的 `distributionManagement` 替换为你的仓库地址：

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

### 6.3 配置 Maven settings.xml 认证

在 `~/.m2/settings.xml` 中添加（`id` 必须与 pom.xml 中的 `id` 一致）：

```xml
<servers>
    <server>
        <id>rdc-snapshots</id>
        <username>你的阿里云账号或RAM子账号</username>
        <password>你的密码</password>
    </server>
    <server>
        <id>rdc-releases</id>
        <username>你的阿里云账号或RAM子账号</username>
        <password>你的密码</password>
    </server>
</servers>
```

### 6.4 发布模块

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

# 4. 验证：在制品库控制台可以看到已上传的包
```

---

## 安全提示

> **以下内容请务必在实际项目中遵守：**
>
> 1. **不要把 AccessKey 提交到 Git** — 一旦泄露，他人可能产生大额账单
> 2. **不要把数据库密码写死在代码中** — 使用环境变量或配置中心
> 3. **生产环境关闭 RDS 公网访问** — 只允许同VPC内网访问
> 4. **定期轮换密码和 AccessKey** — 降低泄露风险

---

[← 返回主文档](../README.md)
