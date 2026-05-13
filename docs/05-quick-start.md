# 快速启动指南

> 本文档是给**完全没有基础**的同学准备的手把手启动教程。按步骤操作，30 分钟内能把项目跑起来。每一步都有验证方法，确认成功再进行下一步。

---

## 前置准备清单

在开始之前，请确认以下工具已安装并可用（在命令提示符/终端输入命令验证）：

```bash
java -version    # 应显示 11.x.x
mvn -version     # 应显示 Apache Maven 3.8.x 或更高
node -v          # 应显示 v16.x 或更高
```

如果有任何一项没有输出，请先参考 [环境搭建指南](../README.md) 完成安装。

> **Java 版本注意**：本分支（dubbo2）使用 Dubbo 2.7.x，要求 **Java 11**。如果你的系统安装了多个 Java 版本，请确认 `JAVA_HOME` 环境变量指向 Java 11 的安装目录。Java 17 在 EDAS 部署时会因模块封装限制导致 Javassist 无法加载，本地开发也建议统一使用 Java 11。

---

## IDEA 首次配置（必做，否则后续步骤会报错）

打开 IntelliJ IDEA，**在导入项目之前**先完成以下设置，避免后续出现莫名其妙的编译错误。

### 确认 JDK 版本

`File` → `Project Structure`（快捷键 `Ctrl+Alt+Shift+S`）→ `SDKs`

- 如果列表里有 Java 11，选中它
- 如果没有，点击 `+` → `Add JDK` → 选择 JDK 11 的安装目录

然后在 `Project` 选项卡中，将 `SDK` 和 `Language level` 都设为 **11**。

### 确认 Maven 设置

`File` → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Maven`

- `Maven home path`：指向你安装的 Maven 目录（如 `D:\tools\apache-maven-3.8.8`）
- `User settings file`：默认 `~/.m2/settings.xml`（如果你配置了阿里云镜像，确认路径正确）
- `Local repository`：Maven 本地缓存目录，默认 `~/.m2/repository`

### 设置文件编码为 UTF-8

`File` → `Settings` → `Editor` → `File Encodings`

- `Global Encoding`：UTF-8
- `Project Encoding`：UTF-8
- `Default encoding for properties files`：UTF-8
- 勾选 `Transparent native-to-ascii conversion`

> **为什么要设置编码？** 项目中有中文注释和中文测试数据。如果编码设置错误，中文会显示乱码，或者 `.properties` 文件中的中文配置项无法读取。

### 打开项目

`File` → `Open` → 选择 `cloudnativeapp` 文件夹（选根目录，不是某个子模块）

IDEA 会自动识别 Maven 多模块项目，右下角会出现进度条，等待依赖加载完成（首次可能需要几分钟）。如果弹出"Trust and Open Maven Project"提示，点击 **Trust Project**。

---

## Step 1：启动 Nacos（服务注册中心）

Nacos 是所有微服务的"服务目录"，必须**最先启动**。其他服务启动时会向 Nacos 注册，如果 Nacos 还没跑，微服务启动后会一直重试注册，最终报错。

```bash
# 进入 Nacos 的 bin 目录（替换为你的实际路径）
cd D:\tools\nacos\bin

# 以单机模式启动（开发环境使用单机模式）
startup.cmd -m standalone
```

**成功标志：** 命令窗口最后出现：

```
Nacos started successfully in stand alone mode. use embedded storage
```

**验证：** 浏览器打开 [http://localhost:8848/nacos](http://localhost:8848/nacos)
- 用户名：`nacos` / 密码：`nacos`
- 能看到控制台主界面即表示成功

**Nacos 控制台关键功能说明：**

| 菜单位置 | 用途 | 本项目如何使用 |
|---------|------|-------------|
| 服务管理 → 服务列表 | 查看所有已注册的微服务及健康状态 | 启动微服务后在此验证注册情况 |
| 配置管理 → 配置列表 | 集中管理配置文件（动态推送） | 本项目使用本地配置文件，暂不用此功能 |
| 命名空间 | 隔离不同环境（dev/test/prod） | 本项目使用默认命名空间 |

> **保持这个命令窗口开着！** 关闭它会停止 Nacos，所有微服务将丢失注册信息，Dubbo 调用会立即失败并报 `No provider available`。

---

## Step 2：确认数据库可用

执行 [数据库初始化 SQL](./04-database.md#4-完整初始化-sql)，确保两个数据库都已创建并有测试数据。

**快速验证（在 MySQL 客户端或 Navicat 中执行）：**

```sql
USE userordertest;
SELECT * FROM `order`;   -- 应该看到 4 条测试数据

USE expresstracktest;
SELECT * FROM track;     -- 应该看到 8 条轨迹数据
```

如果数据存在，继续下一步。如果提示"表不存在"或"数据库不存在"，回到数据库文档重新执行初始化 SQL。

---

## Step 3：安装本地 Maven 依赖

由于三个 Java 项目之间存在依赖，需要先把公共包安装到本地 Maven 仓库（`~/.m2/repository`）。

**为什么要这一步？**

`coffee-app` 依赖 `coffee-userorder-api` 和 `coffee-expresstrack-api`，这两个 api 模块又依赖 `coffee-common`。Maven 只会从本地仓库或远程仓库找依赖。由于这些是本项目内部模块，没有发布到 Maven Central，所以必须先 `install` 到本地仓库，后续模块才能找到它们。

**在 IDEA 的 Terminal 中执行（或打开系统命令提示符）：**

```bash
# 进入项目根目录
cd D:\你的路径\cloudnativeapp

# 第一步：安装公共库（其他所有模块都依赖它，必须最先安装）
cd coffee-common
mvn clean install -DskipTests

# 第二步：安装订单服务 API
cd ../coffee-userorder/api
mvn clean install -DskipTests

# 第三步：安装快递服务 API
cd ../../coffee-expresstrack/api
mvn clean install -DskipTests
```

每步执行完应该看到：

```
[INFO] BUILD SUCCESS
[INFO] Total time:  x.xxx s
```

**如果看到 BUILD FAILURE：**
- 检查 Java 版本是否是 11（`java -version`）
- 查看错误信息中 `ERROR` 行定位具体原因
- 最常见原因：`JAVA_HOME` 指向的不是 Java 11

---

## Step 4：配置数据库连接

每个微服务的 `application-dev.yml` 中需要填写实际的数据库连接信息。

**coffee-userorder/provider/src/main/resources/application-dev.yml：**

```yaml
database:
  user: userordertest        # 数据库账号
  password: 你的密码
  host: localhost:3306       # 本地 MySQL；如用阿里云 RDS 则填外网地址
  dbname: userordertest

nacos:
  host: localhost            # Nacos 地址
  port: 8848
```

**coffee-expresstrack/provider/src/main/resources/application-dev.yml：**

```yaml
database:
  user: expresstracktest
  password: 你的密码
  host: localhost:3306
  dbname: expresstracktest

nacos:
  host: localhost
  port: 8848
```

> **本地 MySQL vs 阿里云 RDS：**
> - 本地开发：`host: localhost:3306`
> - 阿里云 RDS：`host: rm-xxx.mysql.rds.aliyuncs.com:3306`（在阿里云控制台 → RDS 实例 → 基本信息 → 外网地址查看）

---

## Step 5：启动订单微服务

**方法 A：在 IDEA 中启动（推荐）**

1. 左侧项目树：`coffee-userorder` → `provider` → `src/main/java` → `com.coffee.yun.userorder` → `UserOrderApplication.java`
2. 右键点击 → `Run 'UserOrderApplication.main()'`

**方法 B：命令行启动**

```bash
cd coffee-userorder/provider
mvn spring-boot:run
```

**成功标志：** 控制台最后出现：

```
Started UserOrderApplication in x.xxx seconds (JVM running for x.xxx)
```

**在 Nacos 验证注册：**

刷新 Nacos 控制台 → 服务管理 → 服务列表，应该看到：

| 服务名 | 实例数 | 健康实例数 |
|--------|--------|-----------|
| coffee-userorder | 1 | 1 |

如果列表里没有，说明 Nacos 地址配置有误或 Nacos 未启动，检查 `application-dev.yml` 中的 `nacos.host` 和 `nacos.port`。

---

## Step 6：启动快递微服务

同样的方式启动 `coffee-expresstrack/provider` 下的 `ExpressTrackApplication.java`。

**成功标志：**

```
Started ExpressTrackApplication in x.xxx seconds
```

**在 Nacos 验证：** 服务列表中应该看到两个服务都已注册且健康实例数为 1。

---

## Step 7：启动主应用网关

启动 `coffee-app` 下的 `CoffeeAppApplication.java`。

**成功标志：**

```
Started CoffeeAppApplication in x.xxx seconds (JVM running for x.xxx)
```

此时后端三个服务都已运行。建议使用 IDEA 的 **Services** 面板（`View` → `Tool Windows` → `Services`，或快捷键 `Alt+8`）统一查看所有运行中的 Spring Boot 应用，比分散在多个 Run 标签页里更清晰。

---

## Step 8：测试后端接口

打开浏览器，访问：

```
http://localhost:8005/hello/ORDER001
```

**期望结果（JSON 格式）：**

```json
{
  "list": [
    {
      "order_id": "ORDER001",
      "express_id": "EX20240001",
      "express_weight": "1.5kg",
      "track_id": "T001",
      "track_show": "2024-01-01 10:00 【北京朝阳区】 快件已揽收"
    },
    {
      "order_id": "ORDER001",
      "express_id": "EX20240001",
      "express_weight": "1.5kg",
      "track_id": "T002",
      "track_show": "2024-01-01 20:00 【北京转运中心】 快件已到达分拣中心，正在分拣"
    }
  ],
  "total": 5
}
```

看到这个 JSON，后端已经完全跑起来了。

---

## Step 9：启动前端

```bash
# 进入前端目录
cd app-admin

# 安装依赖（第一次需要几分钟，请耐心等待）
npm install

# 启动开发服务器
npm run dev
```

**成功标志：** 命令行出现：

```
  App running at:
  - Local:   http://localhost:8080/
```

浏览器会自动打开管理后台界面。进入"订单管理"页面，输入 `ORDER001` 点击查询，能看到快递轨迹表格即表示全栈完全跑通。

---

## 服务端口汇总

| 服务 | 端口 | 协议 | 用途 |
|------|------|------|------|
| Nacos | 8848 | HTTP | 服务注册中心管理界面 |
| coffee-userorder | 7001 | HTTP | 订单微服务（Spring Boot Web）|
| coffee-expresstrack | 8001 | HTTP | 快递微服务（Spring Boot Web）|
| coffee-expresstrack | 28888 | Dubbo TCP | 快递微服务 RPC 端口（被 coffee-app 调用）|
| coffee-app | 8005 | HTTP | 主应用 REST API 入口 |
| app-admin | 8080 | HTTP | 前端管理界面 |

---

## 端口被占用的解决方法

如果启动时报 `Address already in use` 或 `Port xxxx is already in use`，说明端口被其他程序占用。

**Windows 查找并释放端口：**

```cmd
# 查找占用 7001 端口的进程（替换为实际端口号）
netstat -ano | findstr :7001

# 输出示例：
# TCP    0.0.0.0:7001    0.0.0.0:0    LISTENING    12345
# 最后一列 12345 是进程 PID

# 根据 PID 结束进程
taskkill /PID 12345 /F
```

**Mac/Linux 查找并释放端口：**

```bash
lsof -i :7001
# 找到 PID 后：
kill -9 <PID>
```

---

## 常见错误速查

### 错误0：`UnsupportedClassVersionError` 或 `class file has wrong version`

**含义**：class 文件版本不匹配。本分支所有模块编译目标为 Java 11（class version 55），运行时必须使用 Java 11。

**解决方法：**
1. 下载安装 JDK 11（推荐：[Adoptium Temurin 11](https://adoptium.net)）
2. 设置 `JAVA_HOME` 环境变量指向 JDK 11 目录
3. 重启 IDEA，在 `File` → `Project Structure` → `SDK` 中选择 Java 11
4. EDAS 部署时在"Java 环境"下拉框选择 **Open JDK 11**

---

### 错误1：`java.net.ConnectException: Connection refused`

**含义**：连不上某个服务。

**排查步骤：**
1. 确认 Nacos 已启动：访问 [http://localhost:8848/nacos](http://localhost:8848/nacos)
2. 确认对应的微服务已启动（查看 IDEA Services 面板）
3. 确认端口没被防火墙拦截

---

### 错误2：`No provider available for the service`

**含义**：Dubbo 在 Nacos 中找不到服务提供者。这是最常见的错误，通常由启动顺序不对导致。

**排查步骤：**
1. 确认 `coffee-userorder` 和 `coffee-expresstrack` 都已启动
2. 在 Nacos 控制台的服务列表中确认两个服务的**健康实例数 ≥ 1**
3. 检查 `application.yml` 中 `dubbo.registry.address` 的 Nacos 地址是否正确
4. 尝试重启 `coffee-app`（它启动时会从 Nacos 拉取服务地址，如果微服务后来才注册，重启一次能解决）

---

### 错误3：`Could not find artifact` 或 `Could not autowire. No beans of type...`

**含义**：Maven 依赖没有正确安装到本地仓库。

**解决方法：** 重新按顺序执行 Step 3 的 `mvn clean install` 步骤，每步都等 `BUILD SUCCESS` 再继续。

---

### 错误4：`Unknown column 'xx' in 'field list'`

**含义**：数据库表结构与 SQL 不匹配。

**解决方法：** 删除现有的表后重新执行 [数据库初始化 SQL](./04-database.md#4-完整初始化-sql)。

---

### 错误5：数据库连接失败 `Communications link failure`

**含义**：连不上数据库。

**排查步骤：**
1. 确认 MySQL 服务已启动（Windows：`services.msc` 找到 MySQL 服务）
2. 检查 `application-dev.yml` 中数据库地址、账号、密码是否正确
3. 如果用阿里云 RDS，检查白名单是否已添加本机 IP（IP 会变，需要重新确认）
4. 用 Navicat 或命令行以相同账号密码手动连接，确认账号本身可用

---

### 错误6：前端 `npm install` 很慢或失败

**解决方法：** 切换到国内镜像源：

```bash
npm config set registry https://registry.npmmirror.com
npm install
```

---

### 错误7：前端启动后查询无数据或报网络错误

**排查步骤：**
1. 先在浏览器访问 `http://localhost:8005/hello/ORDER001`，确认后端接口本身正常
2. 检查 `app-admin/src/api/index.js` 中的 `baseURL` 是否为 `http://localhost:8005`
3. 打开浏览器开发者工具（F12）→ Network 标签，查看实际请求的 URL 和响应内容

---

[← 返回主文档](../README.md)
