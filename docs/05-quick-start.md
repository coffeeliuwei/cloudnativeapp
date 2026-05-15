# 快速启动指南

> 本文档是给**完全没有基础**的同学准备的手把手启动教程。按步骤操作，30 分钟内能把项目跑起来。每一步都有验证方法，确认成功再进行下一步。

---

## 前置准备清单

在开始之前，请确认以下工具已安装并可用（在命令提示符/终端输入命令验证）：

```bash
java -version    # 应显示 17.x.x
mvn -version     # 应显示 Apache Maven 3.8.x 或更高
node -v          # 应显示 v16.x 或更高
```

### 安装 JDK 17（如果 java -version 不是 17）

**第一步：下载**

打开 [https://adoptium.net](https://adoptium.net)，选择：
- **Version**：`Temurin 17（LTS）`
- **OS**：`Windows`
- **Architecture**：`x64`（64位系统选这个）

点击 **Latest release** 下载 `.msi` 安装包（约 180MB）。

> Adoptium Temurin 是 Eclipse 基金会维护的免费 JDK，与 Oracle JDK 完全兼容，推荐教学使用。

**第二步：安装**

1. 双击 `.msi` 文件，点击 **Next**
2. 安装路径建议保持默认（`C:\Program Files\Eclipse Adoptium\jdk-17.x.x.x-hotspot`）
3. **关键步骤**：在 "Custom Setup" 页面，确认以下两项已勾选（默认勾选，不要取消）：
   - `Set JAVA_HOME variable` ← 自动设置环境变量
   - `Add to PATH` ← 让命令行能直接用 `java` 命令
4. 点击 **Install**，等待安装完成，点击 **Finish**

**第三步：验证**

重新打开命令提示符（必须重新打开，旧窗口不会读取新环境变量），执行：

```cmd
java -version
```

应显示：
```
openjdk version "17.x.x" ...
```

**如果还是显示旧版本（多 Java 并存的情况）：**

需要手动更新 `JAVA_HOME`：
1. 右键"此电脑" → "属性" → "高级系统设置" → "环境变量"
2. 在"系统变量"中找到 `JAVA_HOME`，双击修改值为 JDK 17 的安装目录
   （例如：`C:\Program Files\Eclipse Adoptium\jdk-17.0.11.9-hotspot`）
3. 在"系统变量"的 `Path` 中，将 JDK 17 的 `bin` 目录移到最前面
   （例如：`C:\Program Files\Eclipse Adoptium\jdk-17.0.11.9-hotspot\bin`）
4. 点击确定，**重新打开**命令提示符，再次执行 `java -version` 验证

---

**确认本机 MySQL 连接参数（必须提前记下来）：**

本项目需要连接本地 MySQL，启动前请确认以下三项，后续配置时会用到：

| 参数 | 说明 | 常见默认值 |
|------|------|---------|
| 端口 | MySQL 监听端口 | **3306**（绝大多数安装默认值）|
| 用户名 | 数据库账号 | `root` |
| 密码 | 该账号的密码 | 安装时自己设置，没有统一默认值 |

> **端口确认方法：** 打开 MySQL Workbench 或 Navicat，查看已有连接的端口号；或在命令行执行 `mysql -u root -p` 不指定端口，能连上说明是默认 3306。

> **Java 版本注意**：本项目要求 Java 17+。如果你的系统安装了多个 Java 版本，请确认 `JAVA_HOME` 环境变量指向 Java 17 的安装目录。

---

## VS Code 首次配置（必做，否则后续步骤会报错）

打开 VS Code，**在导入项目之前**先完成以下设置，避免后续出现莫名其妙的编译错误。

### 安装必要插件

按 `Ctrl+Shift+X` 打开扩展面板，搜索并安装以下插件包：

- **Extension Pack for Java**（微软官方，包含 Java 语言支持、调试器、Maven、测试等）
- **Spring Boot Extension Pack**（包含 Spring Boot Dashboard，可统一管理所有 Spring Boot 服务）

> 安装完成后重启 VS Code，等待右下角 Java 插件初始化完成（首次可能需要 1-2 分钟）。

### 确认 JDK 版本

按 `Ctrl+Shift+P` 打开命令面板，输入并选择 **`Java: Configure Java Runtime`**：

- 如果已显示 Java 17，无需额外操作
- 如果没有，点击页面内的"Install JDK"按钮，或手动指定 JDK 17 安装目录

### 确认 Maven 设置

按 `Ctrl+,` 打开设置，搜索 `maven`：

- `Maven › Executable: Path`：填写 Maven 的 `mvn` 可执行文件路径（如 `D:\tools\apache-maven-3.8.8\bin\mvn`）
- `Maven › Global Settings`：如果你配置了阿里云镜像，填写 `settings.xml` 路径（如 `D:\tools\apache-maven-3.8.8\conf\settings.xml`）

### 设置文件编码为 UTF-8

按 `Ctrl+,` 打开设置，搜索 `encoding`，将 **`Files: Encoding`** 设为 `utf8`。

> **为什么要设置编码？** 项目中有中文注释和中文测试数据。如果编码设置错误，中文会显示乱码，或者 `.properties` 文件中的中文配置项无法读取。

### 打开项目

`文件` → `打开文件夹`（快捷键 `Ctrl+K Ctrl+O`）→ 选择 `cloudnativeapp` 根目录（不是某个子模块）

VS Code 会自动识别 Maven 多模块项目，右下角会出现 Java 项目加载进度，等待完成（首次可能需要几分钟）。

---

## Step 1：启动 Nacos（服务注册中心）

Nacos 是所有微服务的"服务目录"，必须**最先启动**。其他服务启动时会向 Nacos 注册，如果 Nacos 还没跑，微服务启动后会一直重试注册，最终报错。

### 下载与安装 Nacos

1. 打开 Nacos 发布页：[https://github.com/alibaba/nacos/releases](https://github.com/alibaba/nacos/releases)
2. 找到 **2.3.x** 最新版本（如 `2.3.2`），下载 `nacos-server-2.3.2.zip`（Windows 用 zip，Linux/Mac 用 tar.gz）

   > **版本必须选 2.3.x，不能选 3.x！**
   > - Nacos **1.x**：不支持 Dubbo 3.x 的服务发现协议，启动后微服务注册会失败
   > - Nacos **2.x**（✅ 正确）：本项目要求的版本
   > - Nacos **3.x**：与本项目的 Dubbo 配置不兼容，standalone 模式下内嵌数据库初始化失败，报 `standaloneDatabaseOperateImpl` 错误
   >
   > 发布页默认显示最新版，**注意向下滚动找到 2.3.x**，不要直接下载页面顶部的最新版本。

3. 解压到本地目录，例如 `D:\tools\nacos`，目录结构如下：

   ```
   D:\tools\nacos\
   ├── bin\          ← 启动脚本在这里
   ├── conf\
   └── target\
   ```

   > **路径建议**：不要放在有中文或空格的目录下，否则启动脚本可能报错。

### 启动 Nacos

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
| 配置管理 → 配置列表 | 集中管理配置文件（动态推送） | 存放动态配置项（支持热更新） |
| 命名空间 | 隔离不同环境（dev/test/prod） | 本项目使用默认命名空间 |

> **保持这个命令窗口开着！** 关闭它会停止 Nacos，所有微服务将丢失注册信息，Dubbo 调用会立即失败并报 `No provider available`。

---

## Step 2：确认数据库可用

按照 [数据库初始化文档](./04-database.md#4-初始化步骤) 执行 `sql/` 目录下的两个脚本，确保两个数据库都已创建并有测试数据。

**快速验证（在 MySQL 客户端或 Navicat 中执行）：**

```sql
USE userordertest;
SELECT * FROM `order`;   -- 应该看到 3 条测试数据

USE expresstracktest;
SELECT * FROM track;     -- 应该看到 2 条轨迹数据
```

如果数据存在，继续下一步。如果提示"表不存在"或"数据库不存在"，回到 [数据库初始化文档](./04-database.md#4-初始化步骤) 重新执行。

---

## Step 3：安装本地 Maven 依赖

由于三个 Java 项目之间存在依赖，需要先把公共包安装到本地 Maven 仓库（`~/.m2/repository`）。

**为什么要这一步？**

`coffee-app` 依赖 `coffee-userorder-api` 和 `coffee-expresstrack-api`，这两个 api 模块又依赖 `coffee-common`。Maven 只会从本地仓库或远程仓库找依赖。由于这些是本项目内部模块，没有发布到 Maven Central，所以必须先 `install` 到本地仓库，后续模块才能找到它们。

**在 VS Code 的终端中执行（菜单 `终端` → `新建终端`，或快捷键 `Ctrl+\``）：**

> **必须用 `mvn`，不要用 `mvnw`！** 项目目录下有 `mvnw` 脚本（Maven Wrapper），VS Code 有时会自动调用它。`mvnw` 启动时会去网络下载 `maven-wrapper.jar`，国内网络经常失败，报 `MavenWrapperMain` 找不到的错误。直接在终端手动输入 `mvn` 命令可以绕过这个问题。

```bash
# 进入项目根目录（VS Code 终端默认已在项目根目录，可跳过这行）
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
- 检查 Java 版本是否是 17+（`java -version`）
- 查看错误信息中 `ERROR` 行定位具体原因
- 最常见原因：`JAVA_HOME` 指向的不是 Java 17

---

## Step 4：了解配置方式——环境变量

打开 `coffee-userorder/provider/src/main/resources/application-dev.yml`，你会看到这样的写法：

```yaml
database:
  user: ${DB_USER:root}
  password: ${DB_PASSWORD:123456}
  host: ${DB_HOST:localhost:3306}
  dbname: ${DB_NAME:userordertest}
```

`${DB_USER:root}` 的意思是：**先读名为 `DB_USER` 的环境变量，如果没有设置就用 `root` 作为默认值**。

这样设计的好处是：**不需要改任何代码文件**，只需要在启动时注入对应的环境变量，就能切换本地/云上不同的配置。

---

### 本项目用到的所有环境变量

| 变量名 | 作用 | 本地默认值 | 需要改的场景 |
|--------|------|-----------|------------|
| `DB_USER` | 数据库用户名 | `root` | 本地 MySQL 用户名不是 root 时 |
| `DB_PASSWORD` | 数据库密码 | `123456` | 本地 MySQL 密码不是 123456 时 |
| `DB_HOST` | 数据库地址和端口 | `localhost:3306` | 本机端口不是 3306，或连阿里云 RDS 时 |
| `DB_NAME` | 数据库名 | `userordertest` / `expresstracktest` | 一般不需要改 |
| `NACOS_ADDR` | Nacos 配置中心地址 | `127.0.0.1:8848` | 连云上 MSE Nacos 时 |
| `DUBBO_REGISTRY` | Dubbo 注册中心地址 | `nacos://127.0.0.1:8848` | 连云上 MSE Nacos 时 |
| `ENV` | 激活哪套配置文件 | `dev`（加载 application-dev.yml）| 部署到 EDAS 时设为 `prod` |

---

### 四种场景，该怎么做

**场景一：本地 Nacos + 本地 MySQL**

> **先对照前置准备清单确认你的 MySQL 连接参数。** 配置文件默认值为：用户名 `root`、密码 `123456`、端口 `3306`。**只有这三项与你本机完全一致时**，才能直接启动，否则请看场景二。

---

**场景二：本地 Nacos + 本地 MySQL（密码不是 123456）**

**方式一（推荐）：直接修改 yml 默认值**

打开 `coffee-userorder/provider/src/main/resources/application-dev.yml`，把冒号后面的默认值换成你的实际值，保存后在 VS Code 中直接启动：

```yaml
database:
  user: ${DB_USER:你的MySQL用户名}          # ← 换成你的实际用户名（通常是 root）
  password: ${DB_PASSWORD:你的MySQL密码}   # ← 换成你安装 MySQL 时设置的密码
  host: ${DB_HOST:localhost:3306}          # ← 端口默认 3306，如不同请修改
  dbname: ${DB_NAME:userordertest}
```

`coffee-expresstrack` 同理，修改它自己的 `application-dev.yml`。

<details>
<summary>方式二（进阶）：通过 VS Code launch.json 注入环境变量，不改文件</summary>

在项目根目录创建 `.vscode/launch.json`，添加如下配置：
```json
{
  "configurations": [
    {
      "type": "java",
      "name": "UserOrderApplication",
      "request": "launch",
      "mainClass": "com.coffee.yun.userorder.UserOrderApplication",
      "env": {
        "DB_USER": "你的用户名",
        "DB_PASSWORD": "你的密码"
      }
    }
  ]
}
```
保存后在 VS Code 左侧"运行和调试"面板（`Ctrl+Shift+D`）选择该配置启动。优点：文件保持原样，不用担心误提交到 Git（`.vscode/launch.json` 已在 `.gitignore` 中）。
</details>

---

**场景三：本地 Nacos + 阿里云 RDS**

**方式一（推荐）：直接修改 yml 默认值**

修改 `application-dev.yml`，把数据库地址指向 RDS 外网地址：

```yaml
database:
  user: ${DB_USER:userordertest}                              # ← RDS 用户名
  password: ${DB_PASSWORD:你的RDS密码}                        # ← RDS 密码
  host: ${DB_HOST:rm-xxx.mysql.rds.aliyuncs.com:3306}        # ← RDS 外网地址
  dbname: ${DB_NAME:userordertest}
```

<details>
<summary>方式二（进阶）：通过 VS Code launch.json 注入环境变量，不改文件</summary>

在 `.vscode/launch.json` 的 `env` 中添加：
```json
"env": {
  "DB_HOST": "rm-xxx.mysql.rds.aliyuncs.com:3306",
  "DB_USER": "userordertest",
  "DB_PASSWORD": "你的密码"
}
```
</details>

---

**场景四：本地运行 + 云上 MSE Nacos + 阿里云 RDS**

**方式一（推荐）：直接修改 yml 默认值**

`coffee-userorder/provider/src/main/resources/application-dev.yml`：

```yaml
dubbo:
  registry:
    address: ${DUBBO_REGISTRY:nacos://mse-xxx.nacos.aliyuncs.com:8848}  # ← 换成你的 MSE 地址

database:
  user: ${DB_USER:userordertest}                              # ← 换成你的 RDS 用户名
  password: ${DB_PASSWORD:你的RDS密码}                        # ← 换成你的 RDS 密码
  host: ${DB_HOST:rm-xxx.mysql.rds.aliyuncs.com:3306}        # ← 换成你的 RDS 外网地址
  dbname: ${DB_NAME:userordertest}
```

`application.yml` 里的 Nacos 配置中心地址同样修改：

```yaml
spring:
  config:
    import: "optional:nacos:coffee-userorder.properties?server-addr=${NACOS_ADDR:mse-xxx.nacos.aliyuncs.com:8848}&refreshEnabled=true"
    #                                                                               ↑ 换成你的 MSE 地址
```

<details>
<summary>方式二（进阶）：通过 VS Code launch.json 注入环境变量，不改文件</summary>

在 `.vscode/launch.json` 的 `env` 中添加：
```json
"env": {
  "NACOS_ADDR": "mse-xxx.nacos.aliyuncs.com:8848",
  "DUBBO_REGISTRY": "nacos://mse-xxx.nacos.aliyuncs.com:8848",
  "DB_HOST": "rm-xxx.mysql.rds.aliyuncs.com:3306",
  "DB_USER": "userordertest",
  "DB_PASSWORD": "你的密码"
}
```

或命令行启动：
```bash
java -DNACOS_ADDR=mse-xxx.nacos.aliyuncs.com:8848 \
     -DDUBBO_REGISTRY=nacos://mse-xxx.nacos.aliyuncs.com:8848 \
     -DDB_HOST=rm-xxx.mysql.rds.aliyuncs.com:3306 \
     -DDB_USER=userordertest \
     -DDB_PASSWORD=你的密码 \
     -jar coffee-userorder-provider-1.0-SNAPSHOT.jar
```
</details>

> **注意：** 直接改 yml 默认值后，记得不要 git commit，否则你的密码和云上地址会提交到 Git。测试完成后用 `git checkout -- .` 还原文件。

---

**场景五：部署到 EDAS（云上完整部署）**

在 EDAS 控制台创建/部署应用时，找到 **JVM 参数**输入框，填入：

```
-Xms128m -Xmx256m -DENV=prod -DDB_HOST=rm-xxx:3306 -DDB_USER=userordertest -DDB_PASSWORD=你的密码
```

Nacos 由 EDAS Agent 自动接管，不需要设 `NACOS_ADDR` 和 `DUBBO_REGISTRY`。

详细步骤见 [EDAS ECS 集群部署指南](./06-edas-deployment.md)。

---

> **注意：改完 yml 默认值，不要把修改提交到 Git**
>
> `application-dev.yml` 里包含你自己的密码和云上地址，提交后其他同学 clone 下来会连到你的资源。测试完成后用 `git checkout -- .` 还原文件，或在 VS Code 源代码管理面板（`Ctrl+Shift+G`）中右键对应文件 → `放弃更改`。

---

## Step 5：启动订单微服务

**方法 A：在 VS Code 中启动（推荐）**

1. 左侧资源管理器：`coffee-userorder` → `provider` → `src/main/java` → `com.coffee.yun.userorder` → `UserOrderApplication.java`
2. 打开文件，点击 `main` 方法上方出现的 **`Run`** CodeLens（蓝色小字），或右键 → `Run Java`

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

此时后端三个服务都已运行。建议使用 VS Code 左侧的 **Spring Boot Dashboard** 面板（安装 Spring Boot Extension Pack 后出现）统一查看所有运行中的 Spring Boot 应用，比分散在多个终端标签页里更清晰。

---

## Step 8：测试后端接口

打开浏览器，访问（使用测试数据中实际存在的订单号）：

```
http://localhost:8005/hello/44556677
```

**期望结果（JSON 格式）：**

```json
{
  "list": [
    {
      "order_id": "44556677",
      "express_id": "33224455",
      "express_weight": 2.0,
      "track_id": "11223344",
      "track_show": "已发出"
    }
  ],
  "total": 1
}
```

看到这个 JSON，后端已经完全跑起来了。

> 测试数据中 order_id `44556677` 关联快递 `33224455`，快递下有 1 条有效轨迹记录。order_id `32423423` 无关联快递，返回空列表属正常现象。

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

浏览器会自动打开管理后台界面。

**登录**：输入任意非空用户名和密码即可进入（前端本地校验，无需后端 auth 接口）。

**验证全栈跑通：**
1. 进入左侧"**订单管理**"页 → 能看到订单列表（空时表格为空行）
2. 进入左侧"**轨迹查询**"页 → 输入 `44556677` 点击搜索，能看到快递轨迹表格，说明全栈完全跑通

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

**含义**：启动程序所用的 Java 版本太低。**Nacos 2.3.x 和本项目微服务均需要 Java 17+**，用 Java 8 或 Java 11 启动都会报这个错。

错误信息中 `class file version` 后面的数字对应关系：

| class file version | 对应 Java 版本 |
|--------------------|--------------|
| 52.0 | Java 8 |
| 55.0 | Java 11 |
| 61.0 | Java 17 |

如果提示"Runtime only recognizes class file versions up to 52.0"，说明当前运行环境是 Java 8。

**解决方法：**
1. 下载安装 JDK 17（推荐：[Adoptium Temurin 17](https://adoptium.net)）
2. 将系统环境变量 `JAVA_HOME` 改为 JDK 17 的安装目录（如 `C:\Program Files\Java\jdk-17`），重开命令窗口后生效
3. 验证：`java -version` 应显示 `17.x.x`
4. 重启 VS Code，按 `Ctrl+Shift+P` 输入 `Java: Configure Java Runtime`，切换到 Java 17

> **Nacos 启动时报此错**：同样原因。在启动 Nacos 之前，确认命令窗口中 `java -version` 显示的是 17，而不是 8 或 11。如果系统装了多个 Java 版本，可在当前命令窗口临时设置：
> ```cmd
> set JAVA_HOME=C:\Program Files\Java\jdk-17
> startup.cmd -m standalone
> ```

---

### 错误1：Nacos 启动报 `standaloneDatabaseOperateImpl` / `UnsatisfiedDependencyException`

**含义**：Nacos 版本不对，下载了 3.x 版本，导致内嵌数据库无法初始化。

**确认方法：** 查看报错日志路径中是否包含 `nacos-config-3.x.x.jar`。

**解决方法：**
1. 删除当前 Nacos 目录
2. 回到发布页 [https://github.com/alibaba/nacos/releases](https://github.com/alibaba/nacos/releases)，**向下滚动**找到 `2.3.x` 版本（如 `2.3.2`）重新下载
3. 解压后重新启动

---

### 错误3：`java.net.ConnectException: Connection refused`

**含义**：连不上某个服务。

**排查步骤：**
1. 确认 Nacos 已启动：访问 [http://localhost:8848/nacos](http://localhost:8848/nacos)
2. 确认对应的微服务已启动（查看 VS Code Spring Boot Dashboard 面板）
3. 确认端口没被防火墙拦截

---

### 错误4：`No provider available for the service`

**含义**：Dubbo 在 Nacos 中找不到服务提供者。这是最常见的错误，通常由启动顺序不对导致。

**排查步骤：**
1. 确认 `coffee-userorder` 和 `coffee-expresstrack` 都已启动
2. 在 Nacos 控制台的服务列表中确认两个服务的**健康实例数 ≥ 1**
3. 检查 `application.yml` 中 `dubbo.registry.address` 的 Nacos 地址是否正确
4. 尝试重启 `coffee-app`（它启动时会从 Nacos 拉取服务地址，如果微服务后来才注册，重启一次能解决）

---

### 错误5：`Could not find artifact` 或 `Could not autowire. No beans of type...`

**含义**：Maven 依赖没有正确安装到本地仓库。

**解决方法：** 重新按顺序执行 Step 3 的 `mvn clean install` 步骤，每步都等 `BUILD SUCCESS` 再继续。

---

### 错误6：`Unknown column 'xx' in 'field list'`

**含义**：数据库表结构与 SQL 不匹配。

**解决方法：** 删除现有的表后重新执行 [数据库初始化 SQL](./04-database.md#4-完整初始化-sql)。

---

### 错误7：数据库连接失败 `Communications link failure`

**含义**：连不上数据库。

**排查步骤：**
1. 确认 MySQL 服务已启动（Windows：`services.msc` 找到 MySQL 服务）
2. 检查 `application-dev.yml` 中数据库地址、账号、密码是否正确
3. 如果用阿里云 RDS，检查白名单是否已添加本机 IP（IP 会变，需要重新确认）
4. 用 Navicat 或命令行以相同账号密码手动连接，确认账号本身可用

---

### 错误8：前端 `npm install` 很慢或失败

**解决方法：** 切换到国内镜像源：

```bash
npm config set registry https://registry.npmmirror.com
npm install
```

---

### 错误9：`找不到或无法加载主类 MavenWrapperMain` / `WebClient 请求期间发生异常`

**含义**：使用了 `mvnw`（Maven Wrapper）而非 `mvn`，Wrapper 尝试从网络下载 `maven-wrapper.jar` 失败。国内访问 `repo.maven.apache.org` 经常超时。

**解决方法：** 不要用 `mvnw`，直接在终端输入 `mvn` 命令：

```cmd
mvn clean install -DskipTests
```

VS Code 右键菜单触发的 Maven 操作可能默认调用 `mvnw`，遇到此错误时改用终端手动输入 `mvn`。

---

### 错误10：前端启动后查询无数据或报网络错误

**排查步骤：**
1. 先在浏览器访问 `http://localhost:8005/hello/ORDER001`，确认后端接口本身正常
2. 检查 `app-admin/src/api/index.js` 中的 `baseURL` 是否为 `http://localhost:8005`
3. 打开浏览器开发者工具（F12）→ Network 标签，查看实际请求的 URL 和响应内容

---

[← 返回主文档](../README.md)
