# 第 12 章：CI/CD 流水线自动化（基于云效 Flow）

> **本章目标**：把第 11 章手工做的"本地打包 → 上传 jar → 启动进程"全部自动化——你只管 `git push`，云效流水线自动构建并部署到 3 条路径中的任意一条（ECS / EDAS / SAE），前端同样自动发布。
>
> **本章特色**：**6 条流水线全景图**——3 个后端微服务 × 3 条上云路径 + 1 条前端流水线，覆盖第 11 章所有手工步骤的自动化版本。

| 流水线 | 触发分支 | 构建什么 | 部署到哪 | 在哪一节 |
|--------|---------|---------|----------|---------|
| **后端 × 路径 A** | `main` | 3 个微服务 jar | scp 到 3 台 ECS + manage.sh restart | Part 4 |
| **后端 × 路径 B** | `main` | 3 个微服务 jar | EDAS 控制台 API 部署 | Part 5 |
| **后端 × 路径 C** | `main` | 3 个微服务 jar | SAE 控制台 API 部署 | Part 6 |
| **前端 × 路径 A/B** | `main` | Vue dist | scp 到 ECS-3 + nginx -s reload | Part 7.1 |
| **前端 × 路径 C** | `main` | Vue dist | 上传到 OSS 静态网站托管 | Part 7.2 |

---

## 🧭 本章学习主线：手工一步步 ──► 流水线一步步替换

> 这是和第 11 章对称的展开方式。**CI/CD 不是"换一套工具"，而是"把第 11 章里每一个手工动作都让机器替你做"**。

### 第 11 章手工做过的事 → 本章流水线一一对应

| 第 11 章手工步骤 | 本章流水线对应步骤 | 在哪一节 |
|----------------|------------------|---------|
| 本地 `mvn deploy` 把 3 个库推到云效仓库 | **流水线构建阶段自动 `mvn deploy`** | Part 3.3 |
| 本地 `mvn package` 打 3 个微服务 jar | **构建阶段自动跑** | Part 3.4 |
| 浏览器在会话管理里 📤 上传 jar | **部署阶段 "主机部署" 自动 scp** | Part 4 |
| 终端粘 `cd ~/coffee && ./manage.sh restart` | **"部署后执行命令" 自动跑** | Part 4 |
| EDAS 控制台点 "上传新版本部署" | **"部署到 EDAS" 步骤自动调 API** | Part 5 |
| SAE 控制台点 "重新部署" 选新 jar | **"部署到 SAE" 步骤自动调 API** | Part 6 |
| 本地 `npm run build` + Workbench 传 dist.zip | **前端流水线自动构建并上传** | Part 7 |

**关键洞察**：上一章是 "本地→云上"，本章是 "手工→自动"。**两个轴正交**——你写代码不变、配置文件不变、jar 不变，**只是把"按按钮的人"换成"流水线机器人"**。这就是 CI/CD 的本质。

---

## 目录

- [Part 1 云效 Flow 必懂的 5 个概念](#part-1-云效-flow-必懂的-5-个概念)
- [Part 2 准备工作：代码托管 + Maven 凭据](#part-2-准备工作代码托管--maven-凭据)
- [Part 3 通用构建配置（3 条路径共用）](#part-3-通用构建配置3-条路径共用)
- [Part 4 路径 A 流水线 — 后端构建 + scp 到 ECS](#part-4-路径-a-流水线--后端构建--scp-到-ecs)
- [Part 5 路径 B 流水线 — 后端构建 + 部署到 EDAS](#part-5-路径-b-流水线--后端构建--部署到-edas)
- [Part 6 路径 C 流水线 — 后端构建 + 部署到 SAE](#part-6-路径-c-流水线--后端构建--部署到-sae)
- [Part 7 前端流水线 — Vue 构建 + 发布](#part-7-前端流水线--vue-构建--发布)
- [Part 8 触发规则与分支策略](#part-8-触发规则与分支策略)
- [Part 9 高级特性：审批 / 灰度 / 回滚](#part-9-高级特性审批--灰度--回滚)
- [附录 A：6 条流水线一览表](#附录-a6-条流水线一览表)
- [附录 B：常见问题排查](#附录-b常见问题排查)

---

## Part 1 云效 Flow 必懂的 5 个概念

> 这一节零操作，**先把名词搞清楚再去点鼠标**，否则面对云效控制台会一头雾水。

### 1.1 流水线（Pipeline）

一条流水线 = 一条"从代码到运行实例"的传送带。**本课程的命名约定**：

```
coffee-userorder-pipeline-A    （路径 A 后端·订单服务）
coffee-userorder-pipeline-B    （路径 B 后端·订单服务）
coffee-userorder-pipeline-C    （路径 C 后端·订单服务）
coffee-expresstrack-pipeline-A / B / C
coffee-app-pipeline-A / B / C
coffee-front-pipeline           （前端，AB 共用一条；C 单独一条）
```

> 不强制每条路径都建 9 条后端流水线——**实际课程演示时挑一条路径建 3 条就够**。这里列全是让你看清"流水线 = 微服务 × 路径"的组合维度。

### 1.2 阶段（Stage）与步骤（Step）

一条流水线被分成若干 **阶段**（顺序执行），每个阶段里有若干 **步骤**（云效预置的能力，比如 "Java 构建"、"主机部署"）。

```
[阶段：代码源] → [阶段：构建] → [阶段：部署] → [阶段：通知]
                   │                │
                  步骤：Maven 构建   步骤：主机部署 / EDAS 部署 / SAE 部署
```

### 1.3 代码源（Source）

流水线第一步要去拉代码。云效支持：

- **云效 Codeup**（阿里云自家 Git，国内速度最快，本课程推荐）
- **GitHub**（绑账号后能拉，海外节点偶尔抽风）
- **Gitee / 自建 GitLab**

> 教学项目建议把 GitHub 仓库 **镜像同步到云效 Codeup**：Codeup 仓库设置 → "代码同步" → 填 GitHub 地址即可，5 分钟同步一次。这样 GitHub 仍是 source of truth，流水线拉代码走云效内网更快更稳。

### 1.4 变量（Variables）与凭据（Credentials）

流水线里 **不要硬编码** 密码、密钥、私有仓库账号。两个地方放：

- **流水线变量**：明文，每条流水线独立。适合 ECS IP、命名空间名等"非秘密但每条流水线不同"的值
- **凭据管理**（顶部菜单 "凭据管理"）：加密存储，**所有流水线共用**。RDS 密码、ECS 密码、阿里云 AccessKey 都放这里

### 1.5 触发器（Trigger）

什么时候自动跑流水线：

- **代码触发**：监听某个分支 push（最常用，本课程默认监听 `main`）
- **定时触发**：cron 表达式（夜间构建快照版）
- **手动触发**：流水线列表点 "▶ 运行" 按钮（适合 dev 分支）

---

## Part 2 准备工作：代码托管 + Maven 凭据

### 2.1 把代码推到云效 Codeup

> 如果你已经把项目托管在 GitHub 且能用 GitHub 触发流水线，跳过这一步。否则按下面做。

1. 浏览器打开 [云效控制台](https://devops.aliyun.com) → 左侧 **"代码管理 Codeup"**
2. **"新建代码库"** → 名称 `cloudnativeapp` → **"创建"**
3. 终端进项目根目录，把新仓库当作远程仓库推上去：
   ```bash
   git remote add codeup https://codeup.aliyun.com/<你的命名空间>/cloudnativeapp.git
   git push codeup main
   ```
4. 推完进 Codeup 仓库页面，确认文件树和本地一致

### 2.2 在云效凭据管理里加 3 类凭据

5. 云效顶部 **"⚙ 企业设置 → 凭据管理"** → **"新建凭据"**

6. **凭据 1：ECS root 密码**（路径 A 会用）
   - **凭据名称**：`ecs-root-password`
   - **类型**：`用户名密码`
   - **用户名**：`root`
   - **密码**：填 3 台 ECS 的 root 密码（**3 台 ECS 建议设成同一个密码**，省得建 3 个凭据）

7. **凭据 2：阿里云 AccessKey**（路径 B / C 会用，调 EDAS / SAE OpenAPI）
   - **凭据名称**：`aliyun-ak`
   - **类型**：`阿里云访问凭证`
   - 进 [AccessKey 管理控制台](https://ram.console.aliyun.com/manage/ak) 用 RAM 子账号生成一对 AK/SK（**绝对不要用主账号**），把 AccessKey ID / Secret 填到云效凭据里

8. **凭据 3：云效 Maven 仓库账号**（构建时 `mvn deploy` 推 SNAPSHOT 用）
   - **凭据名称**：`yunxiao-maven-credential`
   - 凭据值就是你本地 `~/.m2/settings.xml` 里 `<server>` 节点的 `username` / `password`
   - 找不到？回 Part 4.2 第 4 步的云效制品仓库 → 右上角"配置指引" → 复制凭据

### 2.3 给 RAM 子账号授权 EDAS / SAE 操作权限

9. 还在 RAM 控制台 → 找到 Part 2.2 第 7 步那个子账号 → **"权限管理 → 添加权限"**
10. 系统策略里搜并勾选这 3 个：
    - `AliyunEDASFullAccess`（路径 B 用）
    - `AliyunSAEFullAccess`（路径 C 用）
    - `AliyunOSSFullAccess`（前端走 OSS 时用）
11. **"确定"** 授权

> **为什么必须用子账号 AK**：主账号 AK 一旦泄露损失巨大，云效流水线日志是可下载的——任何把 AK 写在日志里的失误都会变成事故。**子账号 + 最小权限** 是任何 CI/CD 体系的硬铁律。

---

## Part 3 通用构建配置（3 条路径共用）

无论后面走 ECS、EDAS 还是 SAE，**构建阶段几乎一模一样**——都是 `mvn package` 打出 jar。这一节先把构建部分搞定，后面 Part 4/5/6 只换"部署阶段"。

### 3.1 选构建环境

每条流水线创建时云效会问"构建环境"——选 **`公共构建环境`** 即可，里面预装了：

- JDK 8 / 11 / 17（**本课程用 17**）
- Maven 3.6 / 3.8 / 3.9（任选）
- Node.js 14 / 16 / 18（前端流水线用 18）
- Docker / kubectl（高级流水线才用）

### 3.2 配置流水线级 Maven settings

12. 进入流水线编辑页 → 顶部 **"流水线设置 → 环境变量"** → 加：
    - `MAVEN_OPTS` = `-Xmx1024m -Dfile.encoding=UTF-8`
13. 顶部 **"流水线设置 → 通用变量"** → 加（替换成你 Part 4.2 的实际值）：
    - `ALIYUN_REPO_URL` = `https://packages.aliyun.com/maven/repository/xxxxxx-coffee-release/`
    - `ALIYUN_REPO_SNAPSHOT_URL` = `https://packages.aliyun.com/maven/repository/xxxxxx-coffee-snapshot/`

### 3.3 库模块的"先 deploy 再 package"

> 项目有 3 个库模块（`coffee-common`、`coffee-userorder/api`、`coffee-expresstrack/api`），应用模块要拉它们才能编译。**第 11 章你已经手工 `mvn deploy` 过一次**——如果库代码没改，这一节可以跳过；如果库改了就要重新 deploy。

**推荐做法**：再开一条 **专门的"库发布"流水线**，监听 `coffee-common/**`、`coffee-*/api/**` 路径变化：

```bash
mvn -B clean deploy -DskipTests -pl coffee-common,coffee-userorder/api,coffee-expresstrack/api \
    -s settings-yunxiao.xml \
    -Daliyun.repo.url=$ALIYUN_REPO_URL \
    -Daliyun.repo.snapshot.url=$ALIYUN_REPO_SNAPSHOT_URL
```

其中 `settings-yunxiao.xml` 在仓库根目录新建，把 Part 2.2 第 8 步的凭据 ID 引用进去——云效会在构建机器上自动把它替换成真凭据。模板：

```xml
<settings>
  <servers>
    <server>
      <id>rdc-releases</id>
      <username>${env.YUNXIAO_MAVEN_USERNAME}</username>
      <password>${env.YUNXIAO_MAVEN_PASSWORD}</password>
    </server>
    <server>
      <id>rdc-snapshots</id>
      <username>${env.YUNXIAO_MAVEN_USERNAME}</username>
      <password>${env.YUNXIAO_MAVEN_PASSWORD}</password>
    </server>
  </servers>
</settings>
```

然后在云效流水线步骤里勾选 **"使用凭据"** → 关联 `yunxiao-maven-credential`，云效会把它注入成 `YUNXIAO_MAVEN_USERNAME` / `YUNXIAO_MAVEN_PASSWORD` 两个环境变量。

### 3.4 应用模块的构建命令

每个微服务对应一条流水线，构建命令模板（**替换 `<MODULE>` 三处**）：

```bash
mvn -B clean package -DskipTests -pl <MODULE> -am \
    -s settings-yunxiao.xml \
    -Daliyun.repo.url=$ALIYUN_REPO_URL \
    -Daliyun.repo.snapshot.url=$ALIYUN_REPO_SNAPSHOT_URL
```

| 微服务 | `<MODULE>` 填 |
|--------|--------------|
| coffee-userorder | `coffee-userorder/provider` |
| coffee-expresstrack | `coffee-expresstrack/provider` |
| coffee-app | `coffee-app` |

`-am` (`--also-make`) 让 Maven **顺带把上游依赖的模块也编译**。库模块如果已经 deploy 到云效仓库，这里 `-am` 其实只走本地编译加速。

### 3.5 把 jar 当成"构建产物"传给部署阶段

云效流水线 **每个步骤之间默认不共享文件**——构建出来的 jar 不显式声明就到不了部署阶段。

14. 在 **"Java 构建"** 步骤里找到 **"构建物上传"** 区域 → 添加：
    - **产物名**：`app-jar`
    - **路径**：`coffee-*/provider/target/*-1.0-SNAPSHOT.jar`（如果是 coffee-app，填 `coffee-app/target/coffee-app-0.0.1-SNAPSHOT.jar`）
15. 后面部署阶段引用 `${app-jar}` 就能拿到 jar

> 这里是 **CI/CD 新手第二大坑**——构建明明成功，部署却报"找不到 jar"，几乎一定是这一步没配。

---

## Part 4 路径 A 流水线 — 后端构建 + scp 到 ECS

> **对照 06 章 Part 5**：第 11 章你"会话管理 📤 上传 jar → 终端粘 `./manage.sh restart`"那两步，本节让流水线自动做。

### 4.1 给云效"主机组"添加 3 台 ECS

16. 云效顶部 **"⚙ 企业设置 → 主机组管理"** → **"新建主机组"**
17. 填：
    - **主机组名称**：`coffee-prod-ecs`
    - **接入方式**：`阿里云 ECS`
    - **凭据**：选 Part 2.2 第 6 步的 `ecs-root-password`
    - **机器列表**：点 **"从阿里云 ECS 添加"** → 勾选 ECS-1 / ECS-2 / ECS-3 三台 → **"确定"**
18. 等约 30 秒，3 台 ECS 状态变 **"在线"**

> 主机组要求 ECS 的 22 端口对云效构建机网段开放。云效构建机走 **阿里云公网出口**，因此 ECS 安全组需放通 `47.96.0.0/16` 等云效出口网段——**或更直接：把 22/22 对 `0.0.0.0/0` 放通**（教学环境可接受，生产环境用云效官方公布的出口 IP 列表）。

### 4.2 创建 userorder 流水线

19. 云效左侧 **"流水线"** → 顶部 **"新建流水线"** → 模板选 **"Java · 测试、构建、部署到主机"**
20. 填基础信息：
    - **流水线名称**：`coffee-userorder-pipeline-A`
    - **流水线模板** 保持默认（"Java 主机部署"）
21. 第一阶段 **"代码源"**：
    - 选 Codeup 仓库 `cloudnativeapp`，分支 `main`
22. 第二阶段 **"构建"**：
    - 步骤选 **"Java 构建"**
    - JDK 版本：`OpenJDK 17`
    - Maven 版本：`3.8.x`
    - 构建命令：填 Part 3.4 表里 userorder 那行（用 `-pl coffee-userorder/provider -am`）
    - **构建物上传**：产物名 `app-jar`，路径 `coffee-userorder/provider/target/coffee-userorder-provider-1.0-SNAPSHOT.jar`
    - **使用凭据**：勾选 `yunxiao-maven-credential`
23. 第三阶段 **"部署"**：
    - 步骤选 **"主机部署"**
    - **下载路径**：选刚上传的 `app-jar`
    - **部署目标**：主机组 `coffee-prod-ecs` → **过滤标签** 勾选 ECS-1
    - **部署路径**：`/root/coffee/jars/`
    - **暂停方式**：`不暂停`（教学环境）
    - **部署后执行命令**：
      ```bash
      cd ~/coffee && ./manage.sh restart userorder
      ```
24. 第四阶段（可选）**"卡点 / 通知"**：钉钉机器人 / 邮件
25. 右上角 **"保存并运行"** → 看每个阶段绿灯亮起 → 进 ECS-1 看 `~/coffee/jars/` 是否更新

### 4.3 复制 expresstrack 和 coffee-app 流水线

26. 在流水线列表对 `coffee-userorder-pipeline-A` 点 **"⋯ → 复制"**
27. 改 4 处：
    - 名称：`coffee-expresstrack-pipeline-A`
    - 构建命令的 `-pl` 改成 `coffee-expresstrack/provider`
    - 构建物路径改成 `coffee-expresstrack/provider/target/*.jar`
    - 部署目标 ECS-1 → **改成 ECS-2**
    - 部署后命令 `restart userorder` → `restart expresstrack`
28. **再复制一次**做 coffee-app 流水线，部署到 **ECS-3**，命令 `restart app`，构建命令 `-pl coffee-app`

### 4.4 路径 A 流水线全链路演示

29. 在本地随便改一行 `coffee-userorder/provider/.../OrderServiceImpl.java`（比如改一句日志）
30. `git add . && git commit -m "test pipeline" && git push codeup main`
31. **看云效流水线列表**：`coffee-userorder-pipeline-A` 自动开跑 → 约 90 秒后绿色 ✅
32. 回 ECS-1 会话管理终端：`tail -50 ~/coffee/logs/userorder.log` → 应能看到刚改的那行日志

🎉 第一条 CI/CD 链路打通——从你按下 `git push` 到生产环境跑起新版本，**全程零手工**。

---

## Part 5 路径 B 流水线 — 后端构建 + 部署到 EDAS

**对照 06 章 Part 6**：你之前"EDAS 控制台 → 应用详情 → 部署应用 → 上传 jar"的 4 次点击，本节让流水线一次搞定。

### 5.1 准备：确认 EDAS 应用 ID

33. 浏览器进 EDAS 控制台 → 应用列表 → 点 `userorder` 进详情
34. URL 里 `appId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx` 这一段就是 **应用 ID**，复制到记事本
35. 对 `expresstrack` 和 `coffee-app` 各做一次

### 5.2 创建 userorder EDAS 流水线

36. 云效 **"新建流水线"** → 模板选 **"Java · 构建、部署到 EDAS"**
37. 基础信息：
    - **流水线名称**：`coffee-userorder-pipeline-B`
38. **代码源 / 构建阶段** 跟 Part 4.2 第 21-22 步 **完全一样**（构建逻辑是 3 条路径共用的）
39. **部署阶段** 步骤选 **"部署到 EDAS"**：
    - **服务连接**：新建 → 凭据选 Part 2.2 第 7 步的 `aliyun-ak`
    - **地域**：和你 EDAS 同地域
    - **应用 ID**：粘第 34 步那串
    - **部署包路径**：`${app-jar}`（引用构建产物）
    - **分组**：默认（除非你在 EDAS 内分了灰度组）
    - **部署批次**：`1`（教学环境）
    - **批次内部署间隔**：`0 秒`
    - **JVM 参数 / 启动参数**：**不要在这里改**——留空就用 EDAS 应用上现有的配置（06 章 Part 6.4 已经填好的 `-DENV=prod -DDB_HOST=...`）
40. 保存并运行

> **常见误区**：JVM 参数填进流水线后会 **覆盖** EDAS 应用的现有配置。留空让 EDAS 沿用旧值——这是配置管理的"单一来源"原则：**应用配置只在 EDAS 控制台维护，流水线只管推 jar**。

### 5.3 复制另外 2 条 EDAS 流水线

41. 同 Part 4.3 的复制方法做出 `coffee-expresstrack-pipeline-B` 和 `coffee-app-pipeline-B`，每条改：
    - 构建命令 `-pl`
    - 构建物路径
    - 部署阶段的 **应用 ID** 换成对应应用

---

## Part 6 路径 C 流水线 — 后端构建 + 部署到 SAE

**对照 06 章 Part 7**：SAE 控制台"应用详情 → 部署应用 → 上传新版本 jar"的过程，本节让流水线自动做。

### 6.1 关键差异：SAE 没有"主机组"概念

路径 A 是把 jar scp 到固定 ECS；路径 B 是 EDAS Agent 推到 ECS；**路径 C 是云效调 SAE OpenAPI 触发"滚动部署"**——SAE 自己弹实例、自己拉新 jar、自己重启。流水线只负责"打包 + 告诉 SAE 用这个新 jar"。

### 6.2 准备：确认 SAE 应用 ID 和命名空间

42. SAE 控制台 → 应用列表 → 点 `userorder-sae` 进详情
43. **应用 ID**：详情页顶部那一行 `应用 ID：xxxxxxxx`，复制
44. **命名空间 ID**：左侧"命名空间"列表里 `coffee-prod-sae` 那行的 ID 列（形如 `cn-hangzhou:coffee-prod-sae`）
45. 对另两个 SAE 应用各做一次

### 6.3 创建 userorder SAE 流水线

46. 云效 **"新建流水线"** → 模板选 **"Java · 构建、部署到 SAE"**
47. 基础信息：`coffee-userorder-pipeline-C`
48. **代码源 / 构建** 同 Part 4.2 / Part 5.2（不变）
49. **部署阶段** 步骤选 **"部署到 SAE"**：
    - **服务连接**：复用 Part 5.2 第 39 步的（同一个 `aliyun-ak`）
    - **地域**：和 SAE 同地域
    - **命名空间**：粘第 44 步
    - **应用 ID**：粘第 43 步
    - **部署包路径**：`${app-jar}`
    - **部署策略**：`分批发布`
    - **批次数**：`1`
    - **批次间隔**：`0` 秒
    - **是否启用最小就绪实例数**：开（避免发布过程中 0 实例瞬窗）
50. 保存并运行

### 6.4 复制 expresstrack 和 coffee-app 流水线

51. 同 Part 4.3 / Part 5.3，复制改名 + 改 应用 ID + 改构建命令

### 6.5 SAE 流水线的"冷启动注意"

> 如果 coffee-app-sae **缩到 0 实例**（06 章 Part 7.6 那个设置）跑这条流水线时，**部署完毕后再过几秒才能访问**——SAE 收到第一个请求时才真正拉起实例。教学演示前最好手动访问一次预热。

---

## Part 7 前端流水线 — Vue 构建 + 发布

前端构建是一回事，**发布到哪取决于后端走哪条路径**：

| 前端走哪 | 配套 | 静态文件托管在 |
|---------|-----|--------------|
| **路径 A / B 配套** | 后端在 ECS / EDAS | **ECS-3 的 Nginx**（同台机器） |
| **路径 C 配套** | 后端在 SAE | **OSS 静态网站托管 + CDN**（最纯 Serverless） |

### 7.1 前端流水线 A/B 共用版（推到 ECS-3 Nginx）

52. 云效 **"新建流水线"** → 模板选 **"Node.js · 构建、部署到主机"**
53. 基础信息：`coffee-front-pipeline-AB`
54. **代码源**：同 `cloudnativeapp` 仓库，分支 `main`
55. **构建阶段** 步骤选 **"Node.js 构建"**：
    - Node 版本：`18.x`
    - 构建命令：
      ```bash
      cd app-admin && \
      npm config set registry https://registry.npmmirror.com && \
      npm ci && \
      VUE_APP_BASE_URL=$FRONT_API_URL npm run build
      ```
    - **构建物上传**：产物名 `dist`，路径 `app-admin/dist/**`
56. **流水线变量** 加：
    - `FRONT_API_URL` = `http://<ECS-3 公网 IP>:8005`（替换实际 IP，**和 06 章 Part 8.1 是同一个值**）
57. **部署阶段** 步骤选 **"主机部署"**：
    - **下载路径**：`dist`
    - **部署目标**：主机组 `coffee-prod-ecs` → 过滤 ECS-3
    - **部署路径**：`/usr/share/nginx/html/`
    - **部署后执行命令**：
      ```bash
      nginx -t && nginx -s reload
      ```
58. 保存并运行 → 浏览器访问 `http://<ECS-3 公网 IP>/` → 应能看到新版本前端

### 7.2 前端流水线 C 版（推到 OSS 静态网站托管）

走 SAE 路径时后端没有"固定 ECS-3"了，前端最佳搭档是 **OSS 静态网站托管**。

#### 第 1 步：先一次性手工开通 OSS 静态托管

59. 阿里云控制台搜 `OSS` → **新建 Bucket**：
    - 名称：`coffee-front-prod-<随机后缀>`
    - 区域：和 SAE 同区
    - 读写权限：**`公共读`**
60. 进入 Bucket → 左侧 **"基础设置 → 静态页面"** → **"设置"**：
    - **默认首页**：`index.html`
    - **默认 404 页**：`index.html`（SPA 必须这么设，否则刷新页面 404）
61. 在 Bucket **"概览"** 页记下 **"访问域名"**（形如 `xxx.oss-cn-hangzhou.aliyuncs.com`）

#### 第 2 步：把 SAE 公网 SLB 地址告诉前端

62. 回 SAE → `coffee-app-sae` 详情 → 应用访问设置 → 复制 **公网 SLB 域名**（06 章 Part 7.7 创建的那个）

#### 第 3 步：创建 C 版前端流水线

63. 云效 **"新建流水线"** → 模板选 **"Node.js · 构建、部署到 OSS"**
64. 基础信息：`coffee-front-pipeline-C`
65. **构建阶段** 同 Part 7.1 第 55 步，**但流水线变量 `FRONT_API_URL` 改成**：
    - `http://<SAE 公网 SLB 域名>`（**没有端口号**，SAE SLB 默认 80）
66. **部署阶段** 步骤选 **"OSS 上传"**：
    - **服务连接**：复用 `aliyun-ak`
    - **Bucket**：第 59 步那个
    - **上传路径**：本地 `app-admin/dist/`，远程 `/`
    - **是否覆盖**：是
    - **是否清理远端冗余文件**：是（避免上次的 `js/old.xxx.js` 残留）
67. 保存并运行 → 浏览器访问第 61 步的 OSS 访问域名 → 看到前端 ✅

### 7.3 关于 HTTPS 和自定义域名

> 教学版到 Part 7.2 就足够 demo。生产环境前端通常还要：
>
> - 自定义域名（CNAME 到 OSS 访问域名）
> - 阿里云 CDN 接入（加速 + HTTPS 证书）
> - 跨域：SAE 后端的 `coffee-app` 要把 OSS 域名加进 CORS 白名单
>
> 这三件事属于"前端工程化"范畴，本课程不展开。

---

## Part 8 触发规则与分支策略

### 8.1 课程推荐的最小化分支策略

```
main 分支  ──► 自动触发"生产"流水线（路径 A 或 B 或 C 任选一条作为"生产"演示）
dev 分支   ──► 手动触发，部署到同一组资源（教学场景没第二套环境，就用同一套，仅演示）
feature/* ──► 不触发，PR 合并到 dev 时人工把关
```

### 8.2 在云效里配置触发器

68. 进每条流水线 → 顶部 **"流水线设置 → 触发设置"**
69. **代码源触发**：
    - 监听分支：`main`
    - 监听事件：`Push 事件`
    - 路径过滤（可选）：让"前端流水线只在 `app-admin/**` 变化时触发，后端流水线只在对应模块变化时触发"——避免一次 push 把所有 6 条流水线都跑一遍

### 8.3 路径过滤示例

| 流水线 | 监听路径 |
|--------|---------|
| `coffee-userorder-pipeline-*` | `coffee-userorder/**`、`coffee-common/**` |
| `coffee-expresstrack-pipeline-*` | `coffee-expresstrack/**`、`coffee-common/**` |
| `coffee-app-pipeline-*` | `coffee-app/**` |
| `coffee-front-pipeline-*` | `app-admin/**` |

> 这一步省下大量构建时长，**也减少了误部署面积**——改前端不会重启后端。

---

## Part 9 高级特性：审批 / 灰度 / 回滚

这一节是"看过就行"的加分内容，课程不强制做。

### 9.1 部署前人工卡点（审批）

70. 流水线 **部署阶段前** 插入 **"人工卡点"** 步骤：
    - **审批人**：填团队 leader 钉钉号
    - **超时时间**：`24 小时`（超时自动拒绝）
71. 流水线跑到这一步会暂停，等审批人在云效 / 钉钉点 "通过" 才往下走

### 9.2 灰度发布（路径 B/C 适用）

- **路径 B**：EDAS 支持 **金丝雀发布**——流水线部署时选 "分批发布 + 第一批 20%"，看监控没问题再点 "继续下一批"
- **路径 C**：SAE 支持 **灰度发布**——分批 + 暂停时间 + 流量切换

### 9.3 一键回滚

- **路径 A**：回滚 = 把上一版本 jar 重新推上去。建议在 ECS 上保留最近 3 个版本：`~/coffee/jars/userorder-v2025-12-01.jar` 这样命名，回滚就 `cp` + `restart`
- **路径 B / C**：EDAS / SAE 都有 **版本历史** 页——点任意历史版本的 **"回滚"** 按钮，平台自动用旧 jar 重启

---

## 附录 A：6 条流水线一览表

| 流水线名 | 模板 | 构建命令关键参数 | 部署步骤类型 | 部署目标 |
|---------|------|----------------|------------|---------|
| `coffee-userorder-pipeline-A` | Java 主机部署 | `-pl coffee-userorder/provider -am` | 主机部署 | 主机组 `coffee-prod-ecs` / ECS-1 |
| `coffee-userorder-pipeline-B` | Java 部署到 EDAS | 同上 | 部署到 EDAS | EDAS 应用 `userorder` |
| `coffee-userorder-pipeline-C` | Java 部署到 SAE | 同上 | 部署到 SAE | SAE 应用 `userorder-sae` |
| `coffee-expresstrack-pipeline-*` | 同上三条 | `-pl coffee-expresstrack/provider -am` | 同对应类型 | 对应 ECS-2 / EDAS / SAE 应用 |
| `coffee-app-pipeline-*` | 同上三条 | `-pl coffee-app -am` | 同对应类型 | 对应 ECS-3 / EDAS / SAE 应用 |
| `coffee-front-pipeline-AB` | Node.js 主机部署 | `npm ci && npm run build` | 主机部署 | ECS-3 `/usr/share/nginx/html/` |
| `coffee-front-pipeline-C` | Node.js 部署到 OSS | 同上 | OSS 上传 | OSS Bucket `coffee-front-prod-*` |

---

## 附录 B：常见问题排查

### 通用问题

**Q：构建阶段报 `Could not find artifact com.coffee:coffee-common:jar:1.0-SNAPSHOT`**

库模块没 deploy 到云效仓库，或 `settings-yunxiao.xml` 没正确注入凭据。回 Part 3.3 确认"库发布流水线"跑过且绿。

**Q：构建明明成功，部署却报"找不到 jar"**

99% 是 Part 3.5 的 **构建物上传** 路径写错。在构建步骤日志里搜 `Archived artifact`，看实际上传了什么，比对部署步骤的 `下载路径`。

**Q：流水线慢到 5 分钟以上**

- npm / mvn 没用国内镜像：检查构建命令是否加 `npm config set registry https://registry.npmmirror.com`
- 没启用云效 **构建缓存**：流水线设置 → 缓存 → 勾 `~/.m2` 和 `node_modules`

### 路径 A 流水线问题

**Q：主机部署报 `connect to ECS timeout`**

ECS 安全组 22 端口没对云效构建机网段放通。教学环境最简：22/22 临时对 `0.0.0.0/0`；生产用云效官方公布的固定出口 IP。

**Q：jar 推上去了但服务没起来**

部署后执行命令的工作目录是 **HOME 目录** 不是部署路径。本课程命令是 `cd ~/coffee && ./manage.sh restart`——`cd` 不能省。日志在 `~/coffee/logs/<service>.log`。

### 路径 B 流水线问题

**Q：部署到 EDAS 报 `Access denied`**

RAM 子账号缺 `AliyunEDASFullAccess`。回 Part 2.3 第 10 步补授权。

**Q：部署成功但 EDAS 应用状态卡在 "部署中"**

EDAS Agent 拉取 jar 太慢或失败。进 EDAS 应用 → 变更记录 → 看实际错误。常见原因：ECS 出方向 8442/8443/8883 没开（06 章 Part 6.1）。

### 路径 C 流水线问题

**Q：部署到 SAE 报 `InvalidAppId.NotFound`**

应用 ID 写错，或 RAM 子账号没在 SAE 控制台所在的 region 有权限。检查 Part 6.2 第 43 步的 ID + Part 5.2 第 39 步选的地域。

**Q：SAE 应用部署完后 30 秒还连不上**

如果你 06 章 Part 7.6 把网关缩到 0 实例了——这是正常的，**SAE 是按请求触发实例**，第一个请求是冷启动。手动 `curl <SLB 域名>/actuator/health` 触发一次预热。

### 前端流水线问题

**Q：浏览器打开 OSS 域名是 XML 而不是页面**

第 60 步"静态页面"没设。回 OSS Bucket → 基础设置 → 静态页面 → 设默认首页 `index.html`。

**Q：前端打开后所有 API 报 CORS 跨域错误**

后端 coffee-app 没把前端域名加进 CORS 白名单。改 `coffee-app/src/main/.../CorsConfig.java` 把 OSS 域名 / SLB 域名都加进 `allowedOrigins`，重跑后端流水线。

**Q：路径 A 前端构建后 nginx -s reload 报 nginx not found**

ECS-3 没装 Nginx 或 `nginx` 不在 root 的 PATH 里。回 06 章 Part 8.4 确认 Nginx 安装步骤；或在部署后命令里写绝对路径 `/usr/sbin/nginx -s reload`。

---

[← 返回主文档](../README.md) | [上一章：上云部署](06-edas-deployment.md)
