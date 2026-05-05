# 快速启动指南

> 本文档是给**完全没有基础**的同学准备的手把手启动教程。按步骤操作，30 分钟内能把项目跑起来。

---

## 前置准备清单

在开始之前，请确认以下工具已安装并可用（在命令提示符/终端输入命令验证）：

```bash
java -version    # 应显示 17.x.x
mvn -version     # 应显示 Apache Maven 3.8.x 或更高
node -v          # 应显示 v16.x 或更高（Vue CLI 5 要求 Node 16+）
```

如果有任何一项没有输出，请先参考 [环境搭建指南](../README.md#6-本地开发环境搭建) 完成安装。

> **Java 版本注意**：本项目要求 Java 17+。如果你的系统安装了多个 Java 版本，请确认 `JAVA_HOME` 环境变量指向 Java 17 的安装目录。

---

## Step 1：启动 Nacos（服务注册中心）

Nacos 是所有微服务的"电话本"，必须最先启动。

```bash
# 进入 Nacos 的 bin 目录（替换为你的实际路径）
cd D:\tools\nacos\bin

# 以单机模式启动
startup.cmd -m standalone
```

**成功标志：** 命令窗口出现以下内容：

```
Nacos started successfully in stand alone mode. use embedded storage
```

**验证：** 浏览器打开 [http://localhost:8848/nacos](http://localhost:8848/nacos)
- 用户名：`nacos`
- 密码：`nacos`

> **保持这个命令窗口开着！** 关闭它会停止 Nacos。

---

## Step 2：确认数据库可用

执行 [数据库初始化 SQL](./04-database.md#4-完整初始化-sql)，确保两个数据库都已创建并有测试数据。

**快速验证（在 MySQL 客户端执行）：**

```sql
USE userordertest;
SELECT * FROM `order`;   -- 应该看到 4 条测试数据

USE expresstracktest;
SELECT * FROM track;     -- 应该看到 8 条轨迹数据
```

---

## Step 3：在 IDEA 中打开项目

1. 打开 IntelliJ IDEA
2. File → Open → 选择 `cloudnativeapp` 文件夹
3. 等待 IDEA 自动加载 Maven 依赖（右下角有进度条）
4. 如果提示"Maven 导入"，点击"Enable Auto-Import"

---

## Step 4：在本地安装公共依赖

由于三个 Java 项目之间存在依赖，需要先把公共包安装到本地 Maven 仓库。

**在 IDEA 的 Terminal 中执行（或打开系统命令提示符）：**

```bash
# 进入项目根目录
cd D:\你的路径\cloudnativeapp

# 第一步：安装公共库
cd coffee-common
mvn clean install -DskipTests

# 第二步：安装订单服务 API
cd ../coffee-userorder/api
mvn clean install -DskipTests

# 第三步：安装快递服务 API
cd ../../coffee-expresstrack/api
mvn clean install -DskipTests
```

每步执行完应该看到 `BUILD SUCCESS`。

---

## Step 5：启动订单微服务

**方法 A：在 IDEA 中启动（推荐）**

1. 在左侧项目树中找到：
   `coffee-userorder` → `provider` → `src/main/java` → `com.coffee.yun.userorder` → `UserOrderApplication.java`
2. 右键点击 → `Run 'UserOrderApplication.main()'`

**方法 B：命令行启动**

```bash
cd coffee-userorder/provider
mvn spring-boot:run
```

**成功标志：** 控制台最后几行出现：

```
Started UserOrderApplication in x.xxx seconds
```

**在 Nacos 验证：** 刷新 Nacos 控制台 → 服务管理 → 服务列表，应该看到 `coffee-userorder`。

---

## Step 6：启动快递微服务

同样的方式启动 `coffee-expresstrack/provider` 下的 `ExpressTrackApplication.java`。

**成功标志：**

```
Started ExpressTrackApplication in x.xxx seconds
```

**在 Nacos 验证：** 服务列表中应该看到两个服务都已注册：
- `coffee-userorder`
- `coffee-expresstrack`

---

## Step 7：启动主应用网关

启动 `coffee-app` 下的 `CoffeeAppApplication.java`。

**成功标志：**

```
Started CoffeeAppApplication in x.xxx seconds (JVM running for x.xxx)
```

此时后端三个服务都已运行。

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

如果看到这个 JSON，恭喜你，后端已经完全跑起来了！

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
  - Network: http://192.168.x.x:8080/
```

浏览器会自动打开管理后台界面。

---

## 服务端口汇总

| 服务 | 端口 | 用途 |
|------|------|------|
| Nacos | 8848 | 服务注册中心管理界面 |
| coffee-userorder | 7001 | 订单微服务 HTTP 端口 |
| coffee-expresstrack | 8001 | 快递微服务 HTTP 端口 |
| coffee-expresstrack（Dubbo）| 28888 | 快递微服务 RPC 端口 |
| coffee-app | 8005 | 主应用 REST API 入口 |
| app-admin | 8080 | 前端管理界面 |

---

## 常见错误速查

### 错误0：`UnsupportedClassVersionError` 或 `class file has wrong version`

**含义**：Java 版本不够，需要 Java 17+。

**解决方法：**
1. 下载并安装 JDK 17（推荐：[Adoptium Temurin 17](https://adoptium.net)）
2. 设置 `JAVA_HOME` 环境变量指向 JDK 17 目录
3. 重启 IDEA，在 File → Project Structure → SDK 中选择 Java 17

---

### 错误1：`java.net.ConnectException: Connection refused`

**含义**：连不上某个服务。

**排查步骤：**
1. 检查 Nacos 是否已启动（访问 http://localhost:8848/nacos）
2. 检查对应的微服务是否已启动

---

### 错误2：`No provider available for the service`

**含义**：Dubbo 找不到服务提供者。

**排查步骤：**
1. 确认 `coffee-userorder` 和 `coffee-expresstrack` 都已启动
2. 在 Nacos 控制台的服务列表中确认两个服务都已注册
3. 检查 `application.yml` 中 Nacos 地址是否正确

---

### 错误3：`Could not autowire. No beans of type...`

**含义**：Maven 依赖没有正确安装。

**解决方法：** 重新执行 Step 4 的 `mvn clean install` 步骤。

---

### 错误4：`Unknown column 'xx' in 'field list'`

**含义**：数据库表结构与 SQL 不匹配。

**解决方法：** 重新执行 [数据库初始化 SQL](./04-database.md#4-完整初始化-sql)。

---

### 错误5：前端 `npm install` 很慢或失败

**解决方法：** 切换到国内镜像源：

```bash
npm config set registry https://registry.npmmirror.com
npm install
```

---

[← 返回主文档](../README.md)
