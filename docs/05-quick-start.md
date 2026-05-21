# 快速启动指南

> 本文档分两个阶段：**第一阶段** 把项目在本机跑起来，**第二阶段** 将本地环境平移到阿里云。  
> 建议完整跑通第一阶段后再进入第二阶段。

---

## 第一阶段：本地环境启动

---

## 前置准备：安装必要工具

在开始之前，逐项完成以下安装。每项结束后执行验证命令，确认通过再继续。

---

### 1. 安装 JDK 17

**下载**

打开 [https://adoptium.net](https://adoptium.net)，选择：
- **Version**：`Temurin 17（LTS）`
- **OS**：`Windows`
- **Architecture**：`x64`

点击 **Latest release** 下载 `.msi` 安装包（约 180MB）。

**安装**

1. 双击 `.msi`，一路 Next
2. 在 "Custom Setup" 页面，确认以下两项已勾选（默认勾选，不要取消）：
   - `Set JAVA_HOME variable`
   - `Add to PATH`
3. 点击 Install → Finish

**验证**（重新打开命令提示符后执行）

```cmd
java -version
```

期望输出：
```
openjdk version "17.x.x" ...
```

> 如果显示旧版本，需要手动更新 `JAVA_HOME`：右键"此电脑" → 属性 → 高级系统设置 → 环境变量 → 系统变量 → 找到 `JAVA_HOME` → 改为 JDK 17 安装目录（如 `C:\Program Files\Eclipse Adoptium\jdk-17.0.x-hotspot`），同时在 `Path` 中把 JDK 17 的 `bin` 目录移到最前面，重开命令窗口验证。

---

### 2. 安装 MySQL 8.0

**下载**

打开 MySQL 官网下载页：[https://dev.mysql.com/downloads/installer/](https://dev.mysql.com/downloads/installer/)

选择 **Windows (x86, 32-bit), MSI Installer**，下载较大的那个（`mysql-installer-community-8.0.x.msi`，约 400MB）。

**安装**

1. 双击安装包，Setup Type 选 **Developer Default**（包含 MySQL Server、Workbench 等），点击 Execute 安装所有组件
2. 配置阶段选择：
   - **Config Type**：`Development Computer`
   - **Port**：默认 `3306`（记下来，后面配置时用到）
   - 勾选 **Start the MySQL Server at System Startup**
3. 设置 root 密码（记住这个密码，后续配置数据库连接时要用）
4. 完成安装

**验证**

```cmd
mysql -u root -p
```

输入密码后进入 MySQL 命令行，看到 `mysql>` 提示符即表示成功，输入 `exit` 退出。

> **记下你的 MySQL 连接信息，后续配置需要用到：**
>
> | 参数 | 你的值 |
> |------|------|
> | 主机 | `localhost` |
> | 端口 | `3306`（安装时确认的值）|
> | 用户名 | `root` |
> | 密码 | 安装时设置的密码 |

---

### 3. 安装 Maven 3.8+

**下载**

打开 [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi)，下载 **Binary zip archive**（如 `apache-maven-3.9.x-bin.zip`）。

**安装**

1. 解压到本地目录，建议放在无中文无空格的路径，例如 `D:\tools\apache-maven-3.9.x`
2. 配置环境变量：
   - 新建系统变量 `MAVEN_HOME`，值为 Maven 目录（如 `D:\tools\apache-maven-3.9.x`）
   - 在 `Path` 系统变量中新增 `%MAVEN_HOME%\bin`
3. 重新打开命令提示符

**配置国内镜像（强烈建议）**

打开 `D:\tools\apache-maven-3.9.x\conf\settings.xml`，在 `<mirrors>` 标签内添加：

```xml
<mirror>
  <id>aliyun</id>
  <mirrorOf>central</mirrorOf>
  <name>阿里云公共仓库</name>
  <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

**验证**

```cmd
mvn -version
```

期望输出中包含 `Apache Maven 3.x.x` 和 `Java version: 17`。  
如果 Java version 显示的不是 17，说明 `JAVA_HOME` 还没正确指向 JDK 17。

---

### 4. 安装 Node.js 16+

**下载**

打开 [https://nodejs.org](https://nodejs.org)，下载 **LTS 版本**（如 `18.x.x LTS`）的 Windows Installer (`.msi`)。

**安装**

双击安装包，一路默认即可，安装过程会自动配置 PATH。

**验证**

```cmd
node -v    # 应显示 v16.x 或更高
npm -v     # 应显示 8.x 或更高
```

**配置国内镜像（强烈建议）**

```cmd
npm config set registry https://registry.npmmirror.com
```

---

### 5. 下载并准备 Nacos 2.x

**下载**

打开 [https://github.com/alibaba/nacos/releases](https://github.com/alibaba/nacos/releases)，**向下滚动**找到 `2.3.x` 版本（如 `2.3.2`），下载 `nacos-server-2.3.2.zip`。

> **版本必须选 2.3.x，不能选 3.x！** Nacos 3.x 与本项目的 Dubbo 配置不兼容，standalone 模式启动时内嵌数据库初始化会失败。

**解压**

解压到无中文无空格的路径，例如：

```
D:\tools\nacos\
├── bin\        ← 启动脚本在这里
├── conf\
└── target\
```

---

## Step 1：启动 Nacos

Nacos 是所有微服务的注册中心和配置中心，**必须最先启动**。其他服务启动时会向 Nacos 注册，Nacos 不在，微服务会一直报错重试。

```cmd
cd D:\tools\nacos\bin
startup.cmd -m standalone
```

**成功标志**：命令窗口出现：
```
Nacos started successfully in stand alone mode. use embedded storage
```

**验证**：浏览器打开 [http://localhost:8848/nacos](http://localhost:8848/nacos)  
用户名 `nacos` / 密码 `nacos`，能看到控制台主界面即成功。

> **保持这个命令窗口开着**，关闭它会停止 Nacos，所有微服务立即丢失注册，Dubbo 调用会报 `No provider available`。

---

## Step 2：初始化数据库

本项目需要两个数据库：`userordertest`（订单库）和 `expresstracktest`（快递库）。

打开 MySQL Workbench（或命令行），执行以下 SQL：

**第一步：创建两个数据库**

```sql
CREATE DATABASE IF NOT EXISTS userordertest   DEFAULT CHARACTER SET utf8;
CREATE DATABASE IF NOT EXISTS expresstracktest DEFAULT CHARACTER SET utf8;
```

**第二步：初始化订单库**

切换到 `userordertest` 数据库，然后打开并执行项目根目录下的 `sql/order初始化.sql` 文件（Workbench：File → Open SQL Script → 执行）。

**第三步：初始化快递库**

切换到 `expresstracktest` 数据库，执行 `sql/express初始化.sql`。

**验证**：

```sql
USE userordertest;
SELECT * FROM `order`;    -- 应看到 3 条测试数据

USE expresstracktest;
SELECT * FROM track;      -- 应看到 2 条轨迹数据
```

看到测试数据即初始化成功。

---

## Step 3：配置各模块的数据库连接

打开项目，找到以下两个配置文件：

- `coffee-userorder/provider/src/main/resources/application-dev.yml`
- `coffee-expresstrack/provider/src/main/resources/application-dev.yml`

两个文件都有这段配置：

```yaml
database:
  user: ${DB_USER:root}
  password: ${DB_PASSWORD:123456}
  host: ${DB_HOST:localhost:3307}
  dbname: ${DB_NAME:userordertest}   # expresstrack 的是 expresstracktest
```

`${DB_USER:root}` 的含义：**优先读环境变量 `DB_USER`，没有设置时用冒号后面的值 `root` 作为默认值**。

> **注意端口**：配置文件默认端口是 **3307**。如果你的 MySQL 安装在默认端口 3306，需要修改。

**对照下表，确认是否需要修改默认值：**

| 配置项 | 文件默认值 | 你的实际值 | 需要修改？ |
|--------|----------|----------|--------|
| `user` | `root` | ← 填你的用户名 | 不一致则修改 |
| `password` | `123456` | ← 填你的密码 | 不一致则修改 |
| `host` | `localhost:3307` | `localhost:3306`（安装时确认的端口）| **端口大概率不一致，需要改** |

**修改方法**（直接改 yml 默认值，简单直接）：

把冒号后的默认值改成你的实际值。例如密码是 `mypassword`，端口是 3306：

```yaml
database:
  user: ${DB_USER:root}
  password: ${DB_PASSWORD:mypassword}   # ← 改这里
  host: ${DB_HOST:localhost:3306}       # ← 改这里
  dbname: ${DB_NAME:userordertest}
```

两个 `application-dev.yml` 都要改，`dbname` 那行不用改（两个文件各自的默认值已经正确）。

> **不要把改了密码的 yml 文件提交到 Git**，测试完成后用 `git checkout -- .` 还原。

---

## Step 4：安装本地 Maven 依赖

各模块之间存在依赖关系，需要先把公共包安装到本地 Maven 仓库，后续模块才能找到它们。

打开命令提示符，进入项目根目录，**按顺序执行**：

```cmd
rem 第一步：安装公共库（所有模块都依赖它，必须最先安装）
cd coffee-common
mvn clean install -DskipTests

rem 第二步：安装订单服务 API 接口定义
cd ..\coffee-userorder\api
mvn clean install -DskipTests

rem 第三步：安装快递服务 API 接口定义
cd ..\..\coffee-expresstrack\api
mvn clean install -DskipTests
```

每步执行完应看到：
```
[INFO] BUILD SUCCESS
```

> **必须用 `mvn`，不要用 `mvnw`**。项目目录下有 `mvnw`（Maven Wrapper），它会去网络下载 jar，国内网络经常超时失败。

---

## Step 5：启动订单微服务（coffee-userorder）

**方式一：命令行启动**

```cmd
rem 回到项目根目录（Step 4 结束后在 coffee-expresstrack\api 里）
cd 你的项目路径\cloudnativeapp

cd coffee-userorder\provider
mvn spring-boot:run
```

**方式二：VS Code 启动（推荐，方便同时管理多个服务）**

安装 **Extension Pack for Java** 和 **Spring Boot Extension Pack** 两个插件，重启 VS Code 后：

1. 左侧资源管理器找到 `coffee-userorder/provider/src/main/java/com/coffee/yun/userorder/UserOrderApplication.java`
2. 点击 `main` 方法上方的 **`Run`** CodeLens，或右键 → `Run Java`
3. 启动后左侧 **Spring Boot Dashboard** 面板可统一查看所有运行中的服务

**成功标志**：

```
Started UserOrderApplication in x.xxx seconds
```

**在 Nacos 验证注册**：打开 [http://localhost:8848/nacos](http://localhost:8848/nacos) → 服务管理 → 服务列表，应看到 `coffee-userorder` 已注册，健康实例数为 1。

---

## Step 6：启动快递微服务（coffee-expresstrack）

启动 `coffee-expresstrack/provider/src/main/java/.../ExpressTrackApplication.java`，方式同 Step 5。

**成功标志**：

```
Started ExpressTrackApplication in x.xxx seconds
```

**在 Nacos 验证**：服务列表中应同时看到 `coffee-userorder` 和 `coffee-expresstrack` 两个服务，健康实例数都是 1。

---

## Step 7：启动主应用网关（coffee-app）

启动 `coffee-app/src/main/java/.../CoffeeAppApplication.java`。

**成功标志**：

```
Started CoffeeAppApplication in x.xxx seconds
```

至此后端三个服务全部运行中。

---

## Step 8：测试后端接口

浏览器打开：

```
http://localhost:8005/hello/44556677
```

**期望结果**：

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

看到这个 JSON，说明：前端 → coffee-app → Dubbo RPC → coffee-expresstrack → MySQL 整个后端调用链路已完全跑通。

---

## Step 9：启动前端（app-admin）

### 前端是什么，怎么和后端连接

`app-admin` 是基于 Vue 3 的管理后台，负责页面展示和用户交互。它通过 HTTP 请求调用 `coffee-app`（运行在 `8005` 端口）来读写数据。

前端的后端地址配置在 `app-admin/src/api/index.js`：

```js
const baseURL = process.env.VUE_APP_BASE_URL || 'http://localhost:8005'
```

本地开发时没有设置 `VUE_APP_BASE_URL`，所以默认调用本地的 `coffee-app`。

### 安装依赖（只需做一次）

打开新的命令行窗口（**不要关闭前面启动后端的那些窗口**），进入项目根目录下的 `app-admin`：

```cmd
cd D:\2026教学资料\云原生应用框架与开发\code\cloudnativeapp\app-admin
npm install
```

> 路径根据你实际克隆的位置调整。

`npm install` 会根据 `package.json` 把所有前端依赖包下载到 `node_modules/` 目录。第一次需要几分钟，后续无需重复执行。

> 如果下载很慢，先切换镜像源：
> ```cmd
> npm config set registry https://registry.npmmirror.com
> ```
> 再重新执行 `npm install`。

**确认**：看到 `added XXX packages` 且没有 `ERROR` 即成功。

### 启动开发服务器

```cmd
npm run dev
```

**成功标志**：

```
  App running at:
  - Local:   http://localhost:8080/
  - Network: http://192.168.x.x:8080/
```

浏览器会自动打开 `http://localhost:8080`。

### 登录和功能验证

**登录**：在登录页输入任意非空用户名和密码，点击登录即可进入（本地开发模式，前端不做账号验证）。

**验证整个调用链是否跑通**：

1. 点击左侧菜单 **"订单管理"** → 页面加载出订单列表（有数据说明前端→coffee-app→userorder→MySQL 链路正常）
2. 点击左侧菜单 **"轨迹查询"** → 输入单号 `44556677` → 点击搜索 → 出现轨迹记录

能看到轨迹数据，说明 **前端 → coffee-app → Dubbo RPC → expresstrack → MySQL** 全部调用链路跑通，本地环境完全正常。

> **只看到页面骨架但没有数据？** 说明前端能连上，但后端有问题。打开浏览器开发者工具（F12）→ Network 标签 → 看请求的响应状态码和错误信息来定位。

---

## 服务端口一览

| 服务 | 端口 | 说明 |
|------|------|------|
| Nacos | 8848 | 服务注册中心 + 动态配置中心 |
| coffee-userorder | 7001 | 订单微服务 |
| coffee-expresstrack | 8001 | 快递微服务 HTTP |
| coffee-expresstrack | 28888 | 快递微服务 Dubbo RPC 端口 |
| coffee-app | 8005 | 主应用入口（对外 REST API）|
| app-admin | 8080 | 前端管理界面 |

---

---

## 第二阶段：将本地环境平移到阿里云

本地跑通之后，可以逐步把各个本地组件替换成阿里云托管服务。替换是**渐进式**的，可以只替换其中一项而保持其他仍在本地运行。

配置文件中所有连接地址都通过环境变量注入，**不需要修改代码，只需要修改 yml 的默认值或注入环境变量**。

---

## 替换一：本地 MySQL → 阿里云 RDS

**什么时候做这一步**：课程第 6 章，将数据库迁移到云上。

**前置条件**：已完成 [阿里云配置指南 RDS 部分](./01-aliyun-guide.md)，拿到 RDS 外网连接地址、用户名、密码。

**修改方式**（两个配置文件都要改）：

`coffee-userorder/provider/src/main/resources/application-dev.yml`：

```yaml
database:
  user: ${DB_USER:userordertest}                          # ← 改为 RDS 用户名
  password: ${DB_PASSWORD:你的RDS密码}                    # ← 改为 RDS 密码
  host: ${DB_HOST:rm-xxx.mysql.rds.aliyuncs.com:3306}    # ← 改为 RDS 外网地址
  dbname: ${DB_NAME:userordertest}
```

`coffee-expresstrack/provider/src/main/resources/application-dev.yml` 同样修改，`dbname` 保持 `expresstracktest`。

**验证**：重启两个微服务，重新测试 `http://localhost:8005/hello/44556677`，返回数据说明已连接到云上数据库。

> RDS 白名单要添加本机 IP（阿里云控制台 → RDS 实例 → 数据安全 → 白名单设置）。  
> 本机 IP 会变动（尤其在家和学校网络切换时），每次连接前确认 IP 在白名单中。

---

## 替换二：本地 Nacos → 阿里云 MSE Nacos

**什么时候做这一步**：课程第 7 章，将注册中心和配置中心迁移到云上。

**前置条件**：已在 [阿里云配置指南](./01-aliyun-guide.md) 中购买并配置 MSE Nacos，拿到公网访问地址（格式：`mse-xxx.nacos.aliyuncs.com`）。

**修改 Dubbo 注册中心地址**（两个微服务的 `application-dev.yml` 都要改）：

```yaml
dubbo:
  registry:
    address: ${DUBBO_REGISTRY:nacos://mse-xxx.nacos.aliyuncs.com:8848}  # ← 换成你的 MSE 地址
```

**修改 Nacos 配置中心地址**（两个微服务的 `application.yml` 都要改，不是 application-dev.yml）：

```yaml
spring:
  config:
    import: "optional:nacos:coffee-userorder.properties?server-addr=${NACOS_ADDR:mse-xxx.nacos.aliyuncs.com:8848}&refreshEnabled=true"
    #                                                                               ↑ 换成你的 MSE 地址
```

重启所有微服务，在阿里云 MSE Nacos 控制台的服务列表中看到 `coffee-userorder` 和 `coffee-expresstrack` 注册成功即完成。

---

## 替换三：完整云上部署（EDAS）

**什么时候做这一步**：课程第 11 章，将整个应用部署到阿里云 EDAS。

在 EDAS 控制台部署应用时，填写 JVM 参数：

```
-Xms128m -Xmx256m -DENV=prod -DDB_HOST=rm-xxx:3306 -DDB_USER=userordertest -DDB_PASSWORD=你的密码
```

> EDAS 会自动接管 Nacos 注册，不需要额外设置 `NACOS_ADDR` 和 `DUBBO_REGISTRY`。

完整步骤见 [EDAS 部署指南](./06-edas-deployment.md)。

---

---

## 常见错误速查

### 错误 0：`UnsupportedClassVersionError` 或 `无效的目标发行版: 17`

**含义**：Java 版本不对，用了低版本（Java 8 或 11）启动/编译了需要 Java 17 的代码。

**排查**：

```cmd
mvn -version
```

输出中 `Java version` 那行如果不是 17，就是根因。

**解决**：
1. 确认已安装 JDK 17（前置准备步骤 1）
2. 将 `JAVA_HOME` 系统变量改为 JDK 17 安装目录，重开命令窗口
3. 验证：`java -version` 和 `mvn -version` 都显示 17

> Nacos 启动时如果报此错，原因相同。在启动 Nacos 前先确认 `java -version` 是 17。

---

### 错误 1：Nacos 报 `standaloneDatabaseOperateImpl` 或 `UnsatisfiedDependencyException`

**含义**：下载了 Nacos 3.x，内嵌数据库无法初始化。

**解决**：删除当前 Nacos 目录，回到发布页向下滚动找 `2.3.x` 重新下载。

---

### 错误 2：Navicat/数据库工具报 `1251 - Client does not support authentication protocol`

**含义**：Navicat 版本较老，不支持 MySQL 8.0 默认的 `caching_sha2_password` 认证协议。MySQL 8.0 把默认认证方式从旧的 `mysql_native_password` 改成了新协议，老版客户端工具不认识新协议就会报这个错。

**解决方法**：用命令行把 root 账号的认证方式改回旧协议：

```cmd
mysql -u root -p
```

进入后执行（把 `你的密码` 替换为你安装时设置的实际密码）：

```sql
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '你的密码';
FLUSH PRIVILEGES;
```

执行完关闭 Navicat 重新连接即可。

> **为什么不升级 Navicat？** 新版 Navicat 支持新协议，但需要付费授权。改认证方式是更快的教学捷径，不影响本项目任何功能。

---

### 错误 3：`ERROR 2003 (HY000): Can't connect to MySQL server on 'localhost' (10061)`

**含义**：MySQL 服务根本没有启动。错误码 `10061` 是 Windows 的"连接被拒绝"，意思是对应端口没有任何进程在监听，不是密码错误，不是端口错误，就是服务没跑。

**解决方法（二选一）**：

方式一：命令行启动

> **必须用管理员身份运行命令提示符**：在开始菜单搜索"cmd"→ 右键 → **以管理员身份运行**。普通窗口执行会报"发生系统错误 5，拒绝访问"。

```cmd
net start MySQL80
```

看到 `MySQL80 服务已经启动成功` 后再重新连接。  
如果报"找不到服务名"，试 `net start mysql`（不同安装版本服务名可能不同）。

方式二：图形界面

按 `Win+R` → 输入 `services.msc` 回车 → 找到 `MySQL80` → 右键 → 启动。

> **如何让 MySQL 开机自动启动**：在 `services.msc` 中找到 `MySQL80` → 右键 → 属性 → 启动类型改为"自动"，以后开机就不需要手动启动了。

---

### 错误 4：数据库连接失败 `Communications link failure`

**含义**：MySQL 服务在运行，但连不上。通常是端口配错了。

**排查**：
1. 先用错误 3 的方法确认 MySQL 服务正在运行
2. 检查 `application-dev.yml` 中 `host` 端口是否和 MySQL 实际端口一致（**默认配置是 3307，而 MySQL 通常安装在 3306**）
3. 用命令行 `mysql -u root -p -P 3306` 手动连接确认账号密码正确

---

### 错误 5：`Connection refused`（端口连不上某个服务）

**排查**：
1. Nacos 是否在运行：访问 [http://localhost:8848/nacos](http://localhost:8848/nacos)
2. 对应微服务是否在运行（查看终端或 VS Code Spring Boot Dashboard）
3. 确认启动顺序正确：Nacos → userorder → expresstrack → coffee-app

---

### 错误 6：`No provider available for the service`

**含义**：Dubbo 在 Nacos 找不到服务提供者，通常是启动顺序问题。

**排查**：
1. 确认 `coffee-userorder` 和 `coffee-expresstrack` 都已启动
2. Nacos 控制台 → 服务列表，两个服务的健康实例数都应 ≥ 1
3. 如果微服务比 `coffee-app` 晚启动，重启 `coffee-app` 即可

---

### 错误 7：`Could not find artifact` 或 Maven 找不到内部依赖

**含义**：Step 4 的 `mvn clean install` 没有按顺序执行完。

**解决**：重新从 `coffee-common` 开始，按顺序逐个执行 `mvn clean install -DskipTests`，每步等 `BUILD SUCCESS` 再继续。

---

### 错误 8：`Unknown column` 或 `Table doesn't exist`

**含义**：数据库表结构不对，或数据库没初始化。

**解决**：重新执行 Step 2，`sql/` 下的两个 SQL 文件开头有 `DROP TABLE IF EXISTS`，重复执行会清空重建，放心多次运行。

---

### 错误 9：`找不到或无法加载主类 MavenWrapperMain`

**含义**：用了 `mvnw` 而不是 `mvn`，Wrapper 尝试联网下载组件失败。

**解决**：终端手动输入 `mvn` 命令，不要用 `./mvnw` 或 `mvnw.cmd`。

---

### 错误 10：端口被占用 `Address already in use`

**Windows 查找并释放端口（以 7001 为例）：**

```cmd
netstat -ano | findstr :7001
rem 找到输出最后一列的 PID 数字，例如 12345
taskkill /PID 12345 /F
```

---

### 错误 11：前端 `npm install` 很慢或失败

```cmd
npm config set registry https://registry.npmmirror.com
npm install
```

---

### 错误 12：前端页面显示正常但查询无数据

**排查**：
1. 先直接访问后端接口 `http://localhost:8005/hello/44556677`，确认后端本身正常
2. 打开浏览器开发者工具（F12）→ Network 标签，查看请求 URL 和响应
3. 检查 `app-admin/src/api/index.js` 中 `baseURL` 是否为 `http://localhost:8005`

---

[← 返回主文档](../README.md)
