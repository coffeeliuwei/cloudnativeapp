# 各模块代码详解

> 本文档按照系统架构层次，从用户能看到的前端界面开始，逐层向内讲解每个模块的代码结构、设计意图和关键实现。
>
> **阅读建议**：跟着架构图的数据流方向阅读——先看前端（展示层），再看网关，最后深入微服务内部——这样能建立起完整的调用心智模型。

## 技术栈版本矩阵（2025）

| 组件 | 旧版本（2021）| 当前版本 | 主要变化 |
|------|-------------|---------|---------|
| Java | 8 | **17** | 语言特性增强，LTS 长期支持版本 |
| Spring Boot | 2.3.x / 2.6.x | **3.3.4** | `javax.*` → `jakarta.*`，需 Java 17+ |
| Apache Dubbo | 2.7.x | **3.3.4** | `@Reference` → `@DubboReference`，原生 Nacos 支持增强 |
| MyBatis Spring Boot | 1.3.x | **3.0.3** | 适配 Spring Boot 3.x |
| MySQL 驱动 | mysql-connector-java 5.1 | **mysql-connector-j 8.3.0** | groupId 变更，驱动类名变更 |
| FastJSON | fastjson 1.2.x | **fastjson2 2.0.53** | 重写版本，安全性更好，artifactId 变更 |
| Hutool | 4.x | **5.8.26** | API 兼容，修复历史安全漏洞 |
| PageHelper | 1.2.x | **2.1.0** | 适配 MyBatis 3.x / Spring Boot 3.x |
| SLS Logback Appender | 0.1.12 | **0.1.27** | `accessKey` → `accessKeySecret` 配置项 |
| Vue.js | 2.5.x | **2.7.16** | Composition API 支持，axios 0.18 → 1.7.7 |

---

## 架构层次总览

```
┌──────────────────────────────────────────────────────────┐
│  第1层：展示层（app-admin）                                │
│  Vue.js 2 + iView UI  端口:8080                          │
│  用户直接看到的管理界面                                     │
└──────────────────────────┬───────────────────────────────┘
                           │ HTTP REST（Axios 发起）
                           ▼
┌──────────────────────────────────────────────────────────┐
│  第2层：网关层（coffee-app）                               │
│  Spring Boot  端口:8005                                  │
│  系统唯一对外入口，聚合微服务结果                            │
└────────────┬──────────────────────┬──────────────────────┘
             │ Dubbo RPC            │ Dubbo RPC
             ▼                      ▼
┌────────────────────┐  ┌──────────────────────────────────┐
│  第3层：微服务       │  │  第3层：微服务                    │
│  coffee-userorder  │  │  coffee-expresstrack             │
│  用户订单  端口:7001 │  │  快递轨迹  HTTP:8001 Dubbo:28888 │
└────────────────────┘  └──────────────────────────────────┘
```

| 模块 | 层次 | 技术 | 端口 |
|------|------|------|------|
| [app-admin](#1-展示层--app-admin) | 展示层 | Vue.js 2.7 + iView UI 4 | 8080 |
| [coffee-app](#2-网关层--coffee-app) | 网关层 | Spring Boot | 8005 |
| [coffee-userorder](#3-微服务层--coffee-userorder用户订单微服务) | 微服务层 | Spring Boot + Dubbo + MyBatis | 7001 |
| [coffee-expresstrack](#4-微服务层--coffee-expresstrack快递轨迹微服务) | 微服务层 | Spring Boot + Dubbo + MyBatis | HTTP:8001 / Dubbo:28888 |
| [coffee-common](#5-公共基础库--coffee-common) | 公共基础库 | Java | — |

---

## 1. 展示层 — app-admin

### 定位与职责

`app-admin` 是用户直接使用的管理后台界面。它基于开源模板 **iview-admin**（Vue.js 2.7 + iView UI 4）构建，负责：

- 呈现订单和快递轨迹数据（从 `coffee-app` 获取）
- 提供查询表单，收集用户输入并发起 HTTP 请求
- 通过 Vue 响应式系统，在数据变化时自动刷新页面

> **展示层不直接连数据库**，所有数据都来自后端的 REST 接口。这是前后端分离架构的核心原则。

### 目录结构

```
app-admin/
├── package.json              Node.js 项目配置和依赖声明
├── src/
│   ├── main.js               Vue 应用入口（创建 Vue 实例）
│   ├── App.vue               根组件（页面框架壳）
│   ├── api/                  后端接口调用封装
│   │   └── index.js          所有接口函数（axios 请求）
│   ├── router/
│   │   └── routers.js        路由配置（URL ↔ 组件 映射）
│   ├── store/
│   │   └── module/
│   │       └── user.js       用户状态（登录信息、token）
│   ├── components/           公共 Vue 组件
│   └── view/
│       ├── login/login.vue   登录页
│       ├── single-page/home/ 首页
│       └── order/order.vue   订单查询页（核心业务页面）
└── static/                   静态资源（图片、字体等）
```

---

### src/main.js — 应用入口

```javascript
import Vue from 'vue'
import App from './App.vue'
import router from './router'   // 路由
import store from './store'     // 状态管理
import iView from 'iview'       // UI 组件库
import axios from 'axios'

Vue.use(iView)
Vue.prototype.$axios = axios    // 挂载到 Vue 原型，组件内用 this.$axios 调用

new Vue({
  el: '#app',       // 挂载到 index.html 中 id="app" 的元素
  router,
  store,
  render: h => h(App)
})
```

**关键点：** `Vue.prototype.$axios = axios` 让每个 Vue 组件都可以通过 `this.$axios` 发起 HTTP 请求，不需要在每个文件里单独 `import`。

---

### src/router/routers.js — 路由配置

路由定义了"访问哪个 URL，显示哪个页面组件"：

```javascript
export default [
  {
    path: '/login',
    component: () => import('@/view/login/login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: Main,              // 主框架（含侧边菜单、顶部导航）
    children: [
      {
        path: 'home',
        component: () => import('@/view/single-page/home/home.vue'),
        meta: { title: '首页', icon: 'md-home' }
      },
      {
        path: 'order',            // 访问 /order 时显示订单页
        component: () => import('@/view/order/order.vue'),
        meta: { title: '订单管理' }
      }
    ]
  }
]
```

**懒加载（`() => import(...)`）：** 只有当用户真正访问该路由时，才加载对应的 Vue 组件代码，减少初始加载体积。

---

### src/store/module/user.js — 用户状态管理

Vuex 是 Vue 的官方状态管理方案，解决"多个组件需要共享同一份数据"的问题：

```javascript
export default {
  state: {
    userName: '',    // 当前登录用户名
    token: ''        // 登录凭证（每次请求带上，后端验证身份）
  },
  mutations: {
    // mutation 是修改 state 的唯一方式（保证状态变化可追踪）
    setUser(state, user) {
      state.userName = user.name
      state.token = user.token
    },
    logout(state) {
      state.userName = ''
      state.token = ''
    }
  },
  actions: {
    // action 处理异步操作（比如网络请求），再提交 mutation
    login({ commit }, { username, password }) {
      return api.login(username, password).then(user => {
        commit('setUser', user)    // 登录成功后更新 state
      })
    }
  }
}
```

**state / mutations / actions 三者的关系：**

```
用户操作（如点击登录按钮）
    ↓ dispatch
  actions（处理异步请求）
    ↓ commit
  mutations（同步修改 state）
    ↓
  state 更新 → 页面自动重新渲染
```

---

### src/view/order/order.vue — 订单查询页

这是本项目最核心的业务页面，调用 `coffee-app` 的接口展示快递轨迹：

```vue
<template>
  <!-- template：页面的 HTML 结构 -->
  <div class="order-page">
    <!-- iView 组件：输入框和按钮 -->
    <Input v-model="searchOrderId"
           placeholder="输入订单号，如 ORDER001"
           style="width: 300px" />
    <Button type="primary" @click="search" :loading="loading">查询</Button>

    <!-- iView 组件：数据表格 -->
    <Table :columns="columns" :data="trackList" border />
  </div>
</template>

<script>
export default {
  name: 'OrderPage',
  data() {
    return {
      searchOrderId: '',  // v-model 双向绑定输入框的值
      loading: false,     // 控制按钮 loading 状态
      trackList: [],      // 表格数据（快递轨迹列表）
      columns: [          // 表格列定义
        { title: '订单号',   key: 'order_id' },
        { title: '快递单号', key: 'express_id' },
        { title: '重量',     key: 'express_weight' },
        { title: '轨迹ID',   key: 'track_id' },
        { title: '轨迹详情', key: 'track_show', minWidth: 300 }
      ]
    }
  },
  methods: {
    async search() {
      if (!this.searchOrderId) return
      this.loading = true
      try {
        // 调用 coffee-app 接口：GET /hello/{orderid}
        const res = await this.$axios.get(
          `/hello/${this.searchOrderId}`
        )
        // res.data 是后端返回的 PageDTO：{ list: [...], total: 5 }
        this.trackList = res.data.list || []
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
/* scoped：样式只作用于当前组件，不影响其他页面 */
.order-page { padding: 20px; }
</style>
```

**Vue 核心概念一览：**

| 概念 | 代码体现 | 含义 |
|------|---------|------|
| 双向绑定 | `v-model="searchOrderId"` | 输入框和变量自动同步 |
| 事件绑定 | `@click="search"` | 点击按钮执行 search 方法 |
| 属性绑定 | `:data="trackList"` | 表格数据来自 trackList 变量 |
| 响应式 | `this.trackList = res.data.list` | 赋值后页面自动刷新，无需手动操作 DOM |

---

## 2. 网关层 — coffee-app

### 定位与职责

`coffee-app` 是整个系统对外的**唯一入口**，扮演"总前台"的角色：

- 接收来自 `app-admin` 的所有 HTTP 请求（端口 8005）
- 通过 **Dubbo RPC** 调用后端微服务
- 将多个微服务的数据聚合后，以统一格式返回给前端

**为什么需要这一层？**

```
没有网关（直接调用）：
  前端 → localhost:7001（订单服务）  ← 地址暴露，变了就要改前端
  前端 → localhost:8001（快递服务）  ← 跨域处理分散，难以统一

有了网关：
  前端 → localhost:8005（网关）← 只需要知道这一个地址
  网关 → 订单服务（内网，对外不可见）
  网关 → 快递服务（内网，对外不可见）
```

### 目录结构

```
coffee-app/
├── pom.xml
└── src/main/java/com/coffee/yun/coffeeapp/
    ├── CoffeeAppApplication.java      启动类
    ├── CoffeeController.java          REST 控制器（核心业务逻辑）
    └── base/
        ├── Result.java                统一响应体结构
        └── ResultUtil.java            响应体构建工具
```

---

### CoffeeAppApplication.java — 启动类

```java
package com.coffee.yun.coffeeapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * @SpringBootApplication 的 exclude 参数：排除数据源自动配置。
 * coffee-app 本身不连数据库——数据由微服务提供。
 * 不排除的话，Spring Boot 启动时会尝试配置数据库连接，
 * 因为没有数据库配置而报错。
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class CoffeeAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoffeeAppApplication.class, args);
    }
}
```

---

### CoffeeController.java — 核心控制器

控制器是网关层最重要的文件，它把前端请求"翻译"成对微服务的调用：

```java
package com.coffee.yun.coffeeapp;

import com.coffee.yun.dto.PageDTO;
import com.coffee.yun.expresstrack.api.dto.*;
import com.coffee.yun.expresstrack.api.service.ExpressTrackInfoService;
import com.coffee.yun.coffeeapp.base.Result;
import com.coffee.yun.coffeeapp.base.ResultUtil;
import com.coffee.yun.userorder.api.dto.*;
import com.coffee.yun.userorder.api.service.UserOrderInfoService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * @CrossOrigin    允许跨域请求。
 *                 前端(localhost:8080) 调用后端(localhost:8005) 属于跨域，
 *                 浏览器默认会拦截，这个注解告诉浏览器"允许"。
 *
 * @RestController 标记这是 REST 控制器，方法返回值自动序列化为 JSON 响应。
 */
@CrossOrigin
@RestController
public class CoffeeController {

    /**
     * @DubboReference 是 Dubbo 3.x 的核心注解（Dubbo 2.x 用 @Reference）。
     * 它不会在本地找这个接口的实现类，而是：
     *   1. 启动时向 Nacos 查询"谁实现了 UserOrderInfoService"
     *   2. 创建一个网络代理对象，调用它时自动走 RPC
     *   3. 对代码来说，像普通本地对象一样使用
     */
    @DubboReference
    private UserOrderInfoService userOrderInfoService;

    @DubboReference
    private ExpressTrackInfoService expressTrackInfoService;

    /**
     * 接口1：GET /hello/{orderid}
     * 根据订单ID查询快递轨迹（被 app-admin 的 order.vue 调用）
     *
     * @PathVariable 从 URL 路径中提取参数：
     *   请求 /hello/ORDER001 → orderid = "ORDER001"
     */
    @GetMapping("/hello/{orderid}")
    public PageDTO<ExpressTrackInfoResultDTO> helloCoffee(
            @PathVariable String orderid) {

        // Step 1：根据订单ID查询订单信息（RPC 调用 coffee-userorder）
        UserOrderInfoParamDTO orderParam = new UserOrderInfoParamDTO();
        orderParam.setOrder_id(orderid);
        UserOrderInfoResultDTO orderResult =
            userOrderInfoService.findUserOrderInfo(orderParam);

        // Step 2：用订单信息中的 order_id 查询快递轨迹（RPC 调用 coffee-expresstrack）
        ExpressTrackInfoParamDTO trackParam = new ExpressTrackInfoParamDTO();
        trackParam.setOrder_id(orderResult.getOrder_id());

        return expressTrackInfoService.findExpressTrackInfos(trackParam);
    }

    /**
     * 接口2：POST /findOrderList
     * 查询订单及其快递轨迹（供管理后台列表页使用）
     *
     * @RequestBody 从 HTTP 请求体中读取 JSON 并转换为 Java 对象：
     *   请求体 {"order_id":"ORDER001"} → param.order_id = "ORDER001"
     */
    @PostMapping("/findOrderList")
    public Result<PageDTO<ExpressTrackInfoResultDTO>> findOrderList(
            @RequestBody UserOrderInfoParamDTO param) {

        UserOrderInfoResultDTO orderResult =
            userOrderInfoService.findUserOrderInfo(param);

        ExpressTrackInfoParamDTO trackParam = new ExpressTrackInfoParamDTO();
        trackParam.setOrder_id(orderResult.getOrder_id());

        PageDTO<ExpressTrackInfoResultDTO> trackPage =
            expressTrackInfoService.findExpressTrackInfos(trackParam);

        // ResultUtil 将数据包装进统一响应体 Result<T>
        return new ResultUtil<PageDTO<ExpressTrackInfoResultDTO>>().setData(trackPage);
    }
}
```

**两个接口的对比：**

| | `/hello/{orderid}` | `/findOrderList` |
|---|---|---|
| HTTP 方法 | GET（`@GetMapping`） | POST（`@PostMapping`） |
| 参数位置 | URL 路径（`@PathVariable`） | 请求体 JSON（`@RequestBody`） |
| 返回格式 | 直接返回 `PageDTO` | 包装在 `Result<PageDTO>` 中 |
| 使用场景 | 前端直接查轨迹 | 管理后台列表查询 |

---

### base/Result.java — 统一响应格式

良好的 API 设计要求所有接口返回统一的格式，方便前端统一处理：

```java
package com.coffee.yun.coffeeapp.base;

/**
 * 统一 HTTP 响应体。
 * 泛型 T 是实际数据的类型，可以是任意对象。
 *
 * 响应示例：
 * {
 *   "success": true,
 *   "code": 200,
 *   "message": "success",
 *   "timestamp": 1704038400000,
 *   "result": { "list": [...], "total": 5 }
 * }
 */
public class Result<T> implements Serializable {
    private boolean success;
    private String message;
    private Integer code;
    private long timestamp = System.currentTimeMillis();
    private T result;   // 实际业务数据（泛型，可以是任意类型）

    // getter/setter...
}
```

---

### src/main/resources/application.properties

```properties
# 应用在 Dubbo 注册中心的名称
dubbo.application.name=coffee-app

# Nacos 地址——启动时从这里发现微服务位置
dubbo.registry.address=nacos://localhost:8848

# HTTP 服务端口（前端调用的端口）
server.port=8005
```

> **为什么用 `.properties` 而不是 `.yml`？**
> 两者功能完全相同，只是格式不同。`.yml` 层级结构更清晰，`.properties` 更简洁，本模块配置项少，用 `.properties` 足够。

---

## 3. 微服务层 — coffee-userorder（用户订单微服务）

### 定位与职责

`coffee-userorder` 专门负责**用户和订单数据**，是一个独立运行的 Spring Boot 服务。当 `coffee-app` 通过 Dubbo 调用时，它从 `userordertest` 数据库查询数据并返回。

**遵循 Dubbo 推荐的 api/provider 分层模式：**

```
coffee-userorder/
├── api/        接口定义层（"合同"）
│               - 定义服务方法签名
│               - 定义数据传输对象（DTO）
│               - 打包成 jar，供 coffee-app 引用
│               - 不包含任何实现代码
│
└── provider/   接口实现层（"履行合同"）
                - 实现 api 层定义的接口
                - 连接数据库，执行真正的查询
                - 是独立运行的 Spring Boot 应用
```

**为什么要分 api 和 provider？**

```
coffee-app 引用的是 coffee-userorder-api（只有接口定义）
→ 编译阶段：coffee-app 能调用接口方法，类型检查通过
→ 运行阶段：Dubbo 自动找到 coffee-userorder-provider 执行实际逻辑

好处：coffee-app 不依赖实现代码，实现可以随时更换
      （比如把 MySQL 换成 MongoDB），coffee-app 无需改动
```

---

### 3.1 api 子模块

#### UserOrderInfoParamDTO.java — 查询参数

```java
package com.coffee.yun.userorder.api.dto;

import com.coffee.yun.dto.BasePageDTO;

/**
 * 查询订单的请求参数。
 * 继承 BasePageDTO 自动获得分页参数（pageNum, pageSize）。
 */
public class UserOrderInfoParamDTO extends BasePageDTO {
    private String order_id;     // 订单编号，精确匹配
    private String member_name;  // 用户姓名，支持模糊查询

    // getter/setter
}
```

**字段命名为什么用下划线（`order_id`）而不是驼峰（`orderId`）？**

与数据库列名保持一致。MyBatis 做字段映射时更简单，不需要额外的驼峰转换配置。

---

#### UserOrderInfoResultDTO.java — 返回结果

```java
package com.coffee.yun.userorder.api.dto;

/**
 * 订单查询的返回结果，字段与数据库列名一一对应。
 */
public class UserOrderInfoResultDTO {
    private String member_name;   // 用户姓名（来自 member 表）
    private String member_phone;  // 手机号
    private String OneID;         // 用户唯一标识
    private String order_id;      // 订单编号（来自 order 表）
    private String order_amount;  // 订单金额
    private String order_status;  // 订单状态（待付款/已付款/已发货/已完成）

    // getter/setter
}
```

---

#### UserOrderInfoService.java — 服务接口

```java
package com.coffee.yun.userorder.api.service;

import com.coffee.yun.userorder.api.dto.UserOrderInfoParamDTO;
import com.coffee.yun.userorder.api.dto.UserOrderInfoResultDTO;

/**
 * 用户订单查询服务接口——只定义"能做什么"，不写"怎么做"。
 *
 * Dubbo 通过这个接口进行服务注册和调用：
 *   - coffee-userorder-provider 注册："我实现了这个接口，地址是 xxx"
 *   - coffee-app 声明："我要调用这个接口"
 *   - Dubbo 在运行时自动将二者连接
 */
public interface UserOrderInfoService {
    UserOrderInfoResultDTO findUserOrderInfo(UserOrderInfoParamDTO param);
}
```

---

### 3.2 provider 子模块

#### UserOrderApplication.java — 启动类

```java
package com.coffee.yun.userorder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @SpringBootApplication 做了三件事：
 *   1. 开启自动配置（根据 classpath 中的 jar 自动配置 DataSource、MyBatis 等）
 *   2. 开启组件扫描（自动发现同包及子包下的 @Service、@Repository 等注解类）
 *   3. 标识这是一个 Spring 配置类
 */
@SpringBootApplication
public class UserOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserOrderApplication.class, args);
    }
}
```

---

#### UserOrderInfoServiceImpl.java — 接口实现

```java
package com.coffee.yun.userorder.provider.service;

import com.coffee.yun.dto.PageDTO;
import com.coffee.yun.userorder.api.dto.*;
import com.coffee.yun.userorder.api.service.UserOrderInfoService;
import com.coffee.yun.userorder.provider.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @DubboService  最关键的注解！
 *                作用：将这个类注册到 Nacos，标明"我是 UserOrderInfoService 的提供者"
 *                没有这个注解，coffee-app 就找不到这个服务
 *
 * @Slf4j         Lombok 注解，自动生成 Logger 对象 log
 *                等价于手写：private static final Logger log = LoggerFactory.getLogger(...)
 */
@Slf4j
@DubboService
public class UserOrderInfoServiceImpl implements UserOrderInfoService {

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;  // MyBatis 执行 SQL 的模板

    @Autowired
    private PageUtil pageUtil;  // 封装了分页查询逻辑的工具类

    @Override
    public UserOrderInfoResultDTO findUserOrderInfo(UserOrderInfoParamDTO param) {
        log.info("查询订单信息，参数: {}", param.getOrder_id());

        // 调用 PageUtil 执行分页查询
        // "UserOrderMapper.selectByParam" 对应 UserOrderMapper.xml 中的 <select id="selectByParam">
        PageDTO<UserOrderInfoResultDTO> pageResult =
            pageUtil.selectPage("UserOrderMapper.selectByParam", param);

        // 此方法用于精确查询单条订单，取第一条返回
        if (pageResult.getList() != null && !pageResult.getList().isEmpty()) {
            return pageResult.getList().get(0);
        }
        return null;
    }
}
```

**`@DubboService` / `@DubboReference` 与 Spring 注解的核心区别：**

| 注解 | 归属 | 作用 | 使用场景 |
|------|------|-----|---------|
| `@Service`（Spring） | Spring 框架 | 注册到 Spring 容器 | 同一应用内的组件 |
| `@Autowired`（Spring） | Spring 框架 | 注入本地 Bean | 同一进程内依赖注入 |
| `@DubboService`（Dubbo 3.x）| Dubbo 框架 | 注册到 Nacos，暴露 RPC 接口 | **需要被其他应用远程调用**的服务 |
| `@DubboReference`（Dubbo 3.x）| Dubbo 框架 | 创建远程代理，注入调用方 | **调用远程 Dubbo 服务** |

> **版本说明：** Dubbo 2.x 使用 `@Service` + `@Reference`（已废弃），Dubbo 3.x 改为语义更明确的 `@DubboService` + `@DubboReference`，避免与 Spring 的 `@Service` 混淆。

---

#### UserOrderMapper.xml — SQL 映射文件

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<!-- namespace：命名空间，Java 代码中通过 "UserOrderMapper.selectByParam" 引用 -->
<mapper namespace="UserOrderMapper">

    <!-- resultMap：定义数据库列名 → Java 对象字段 的映射规则 -->
    <resultMap id="BaseResultMap"
               type="com.coffee.yun.userorder.api.dto.UserOrderInfoResultDTO">
        <result column="member_name"  property="member_name"  jdbcType="VARCHAR"/>
        <result column="member_phone" property="member_phone" jdbcType="VARCHAR"/>
        <result column="OneID"        property="OneID"        jdbcType="VARCHAR"/>
        <result column="order_id"     property="order_id"     jdbcType="VARCHAR"/>
        <result column="order_amount" property="order_amount" jdbcType="VARCHAR"/>
        <result column="order_status" property="order_status" jdbcType="VARCHAR"/>
    </resultMap>

    <select id="selectByParam"
            resultMap="BaseResultMap"
            parameterType="com.coffee.yun.userorder.api.dto.UserOrderInfoParamDTO">
        SELECT m.member_name,
               m.member_phone,
               o.OneID,
               o.order_id,
               o.order_amount,
               o.order_status
        FROM member m, `order` o
        WHERE m.OneID = o.OneID

        <!-- 动态 SQL：只有参数非空时才加上对应的 WHERE 条件 -->
        <if test="order_id != null and order_id != ''">
            AND o.order_id = #{order_id, jdbcType=VARCHAR}
        </if>
        <if test="member_name != null and member_name != ''">
            AND m.member_name = #{member_name, jdbcType=VARCHAR}
        </if>
    </select>

</mapper>
```

**重要的安全知识 — `#{}` 和 `${}` 的区别：**

| 写法 | 处理方式 | 安全性 |
|------|---------|------|
| `#{order_id}` | 预编译（PreparedStatement），参数值被转义 | ✅ 安全，**防 SQL 注入** |
| `${order_id}` | 直接字符串拼接进 SQL | ❌ 存在 SQL 注入风险，**不要使用** |

**动态 SQL `<if>` 的价值：**

查询时，用户可能只填了订单ID，也可能只填了用户名。`<if>` 让 SQL 根据实际传入的参数动态构建，避免了"如果没有传订单ID就查全表"的问题。

---

#### application.yml — 主配置

```yaml
server:
  port: 7001                    # HTTP 端口（Spring Boot 需要，虽然服务通过 Dubbo 调用）

spring:
  application:
    name: coffee-userorder      # 服务在 Nacos 中注册的名称

  profiles:
    active: ${ENV:dev}          # 激活配置文件：先读环境变量 ENV，不存在则用 dev

  datasource:
    name: coffee-userorder
    # allowPublicKeyRetrieval=true 是 MySQL 8.x 密钥交换所需参数
    url: jdbc:mysql://${database.host}/${database.dbname}?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true
    username: ${database.user}
    password: ${database.password}
    # MySQL 8 新驱动类（旧版 com.mysql.jdbc.Driver 已废弃）
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis:
  mapper-locations: classpath:mapper/*.xml   # 扫描 resources/mapper/ 下所有 XML
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl   # 打印 SQL 到控制台（开发调试用）

dubbo:
  scan:
    base-packages: com.coffee.yun.userorder  # 扫描此包，找 @DubboService 注解的类并注册
  application:
    name: coffee-userorder
  registry:
    address: nacos://${nacos.host:127.0.0.1}:${nacos.port:8848}  # 支持环境变量覆盖
  protocol:
    name: dubbo
    serialization: fastjson2                 # Dubbo 3.x 推荐序列化方式
```

**`${database.host}` 这类变量从哪里来？**

来自 `application-dev.yml`（开发环境配置文件），包含真实的数据库地址和密码：

```yaml
# application-dev.yml（不提交到 Git，含敏感信息）
database:
  host: rm-xxx.mysql.rds.aliyuncs.com:3306
  dbname: userordertest
  user: your_username
  password: your_password
```

这种配置分层方式让敏感信息（密码、密钥）可以单独管理，不放入代码仓库。

---

## 4. 微服务层 — coffee-expresstrack（快递轨迹微服务）

### 定位与职责

`coffee-expresstrack` 专门负责**快递和轨迹数据**，结构与 `coffee-userorder` 完全对称，但有以下关键区别：

| 对比项 | coffee-userorder | coffee-expresstrack |
|--------|-----------------|---------------------|
| 数据库 | `userordertest`（用户、订单）| `expresstracktest`（快递、轨迹）|
| 返回值 | 单个对象（一个订单）| 列表（多条轨迹）|
| Dubbo 端口 | 使用默认端口 | 明确指定 **28888** |
| HTTP 端口 | 7001 | 8001 |

### 目录结构

```
coffee-expresstrack/
├── pom.xml
├── api/
│   └── src/main/java/com/coffee/yun/expresstrack/api/
│       ├── dto/
│       │   ├── ExpressTrackInfoParamDTO.java    查询参数
│       │   └── ExpressTrackInfoResultDTO.java   返回结果
│       └── service/
│           └── ExpressTrackInfoService.java     服务接口
└── provider/
    └── src/main/
        ├── java/com/coffee/yun/expresstrack/
        │   ├── ExpressTrackApplication.java     启动类
        │   └── provider/service/
        │       └── ExpresstrackInfoServiceImpl.java   实现类
        └── resources/
            ├── application.yml
            ├── application-dev.yml
            ├── logback-spring.xml
            └── mapper/
                └── ExpressTrackMapper.xml
```

---

### 4.1 api 子模块

#### ExpressTrackInfoService.java — 服务接口

```java
package com.coffee.yun.expresstrack.api.service;

import com.coffee.yun.dto.PageDTO;
import com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoParamDTO;
import com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoResultDTO;

/**
 * 快递轨迹查询接口。
 * 返回值是 PageDTO<ExpressTrackInfoResultDTO>（分页列表），
 * 因为一个订单通常有多条轨迹记录（揽收 → 转运 → 派送 → 签收）。
 */
public interface ExpressTrackInfoService {
    PageDTO<ExpressTrackInfoResultDTO> findExpressTrackInfos(
        ExpressTrackInfoParamDTO expressTrackInfoParamDTO);
}
```

**为什么返回 `PageDTO`（列表）而订单服务返回单个对象？**

| 服务 | 关系 | 返回值 |
|------|------|------|
| 订单服务 | 订单ID → 1条订单记录 | 单个 `UserOrderInfoResultDTO` |
| 快递服务 | 快递单号 → N 条轨迹记录 | `PageDTO<ExpressTrackInfoResultDTO>`（列表）|

---

#### ExpressTrackInfoResultDTO.java — 返回结果

```java
package com.coffee.yun.expresstrack.api.dto;

/**
 * 快递轨迹查询结果。
 * 包含快递信息（来自 express 表）和单条轨迹（来自 track 表）。
 * SQL 使用 LEFT JOIN 将两表合并，每条轨迹都携带快递基本信息。
 */
public class ExpressTrackInfoResultDTO {
    private String order_id;        // 订单编号
    private String express_id;      // 快递单号
    private String express_weight;  // 包裹重量
    private String track_id;        // 轨迹节点ID
    private String track_show;      // 轨迹描述文字

    // getter/setter
}
```

---

### 4.2 provider 子模块

#### ExpressTrackMapper.xml — SQL 映射文件

```xml
<mapper namespace="ExpressTrackMapper">

    <resultMap id="BaseResultMap"
               type="com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoResultDTO">
        <result column="order_id"        property="order_id"        jdbcType="VARCHAR"/>
        <result column="express_id"      property="express_id"      jdbcType="VARCHAR"/>
        <result column="express_weight"  property="express_weight"  jdbcType="VARCHAR"/>
        <result column="track_id"        property="track_id"        jdbcType="VARCHAR"/>
        <result column="track_show"      property="track_show"      jdbcType="VARCHAR"/>
    </resultMap>

    <select id="selectByParam"
            resultMap="BaseResultMap"
            parameterType="com.coffee.yun.expresstrack.api.dto.ExpressTrackInfoParamDTO">
        SELECT express.express_id,
               order_id,
               express_weight,
               track_id,
               track_show
        FROM `express`
        LEFT JOIN `track` ON `express`.`express_id` = `track`.`express_id`
        <if test="order_id != null and order_id != ''">
            AND order_id = #{order_id, jdbcType=VARCHAR}
        </if>
    </select>

</mapper>
```

**`LEFT JOIN` 的原理图示：**

```
express 表（快递基本信息）：
express_id | order_id | express_weight
EX20240001 | ORDER001 | 1.5kg

track 表（轨迹记录，一个快递多条）：
express_id | track_id | track_show
EX20240001 | T001     | 2024-01-01 10:00 已揽收
EX20240001 | T002     | 2024-01-02 08:00 已转运
EX20240001 | T003     | 2024-01-03 15:00 派送中

LEFT JOIN 结果（每条轨迹带上快递基本信息）：
express_id | order_id | express_weight | track_id | track_show
EX20240001 | ORDER001 | 1.5kg          | T001     | 已揽收
EX20240001 | ORDER001 | 1.5kg          | T002     | 已转运
EX20240001 | ORDER001 | 1.5kg          | T003     | 派送中
```

这样前端拿到的每一行数据都既有快递单号、重量，也有轨迹描述，一次请求获取所有需要的信息。

---

#### application.yml — 特殊配置项说明

```yaml
server:
  port: 8001                       # HTTP 端口

spring:
  application:
    name: coffee-expresstrack      # Nacos 中注册的服务名

dubbo:
  protocol:
    name: dubbo
    port: 28888                    # Dubbo RPC 专用端口（关键！）
    serialization: fastjson2       # Dubbo 3.x 推荐序列化方式
  scan:
    base-packages: com.coffee.yun.expresstrack
  application:
    name: coffee-expresstrack
  registry:
    address: nacos://${nacos.host:localhost}:${nacos.port:8848}
```

**为什么 `coffee-expresstrack` 需要单独指定 Dubbo 端口（28888）？**

Dubbo 默认端口是 20880。本机同时运行 `coffee-userorder` 和 `coffee-expresstrack` 时，如果都用默认端口会发生冲突。通过显式指定 28888 避免冲突。

**HTTP 端口（8001）和 Dubbo 端口（28888）的用途不同：**

| 端口 | 协议 | 用途 |
|------|------|------|
| 8001 | HTTP | Spring Boot Web，用于健康检查、管理接口 |
| 28888 | Dubbo 二进制协议 | RPC 调用——`coffee-app` 调用此服务就走这个端口 |

---

#### logback-spring.xml — 日志配置

```xml
<configuration>
    <!-- 日志格式：服务名 + 时间 + 消息 + 级别 + 类名 -->
    <property name="LOG_PATTERN"
              value=" 用户订单中心  %d{yyyy-MM-dd HH:mm:ss.SSS}   %msg      %-5level %logger{50} %n"/>

    <!-- CONSOLE：打印到控制台（开发时看日志用）-->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>info</level>    <!-- 只打印 INFO 及以上级别 -->
        </filter>
        <encoder>
            <Pattern>${LOG_PATTERN}</Pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!--
        aliyun_sls：将日志推送到阿里云日志服务（SLS）
        使用 aliyun-log-logback-appender 0.1.27
        注意：0.1.16+ 版本配置项 accessKey → accessKeySecret
    -->
    <appender name="aliyun_sls" class="com.aliyun.openservices.log.logback.LoghubAppender">
        <endpoint>cn-beijing.log.aliyuncs.com</endpoint>
        <!-- 使用环境变量，不在代码中硬编码密钥 -->
        <accessKeyId>${ALIYUN_ACCESS_KEY_ID}</accessKeyId>
        <accessKeySecret>${ALIYUN_ACCESS_KEY_SECRET}</accessKeySecret>
        <projectName>userorder</projectName>
        <logstore>logs</logstore>
        <topic>订单</topic>
        <!-- 批量发送配置（减少网络请求次数）-->
        <packageTimeoutInMS>3000</packageTimeoutInMS>      <!-- 最长等待 3 秒发一批 -->
        <logsCountPerPackage>4096</logsCountPerPackage>    <!-- 一批最多 4096 条 -->
        <logsBytesPerPackage>3145728</logsBytesPerPackage> <!-- 一批最大 3MB -->
        <retryTimes>3</retryTimes>
    </appender>

    <!-- dev 环境：同时输出到控制台和阿里云 SLS -->
    <springProfile name="dev">
        <root level="INFO">
            <appender-ref ref="CONSOLE" />
            <appender-ref ref="aliyun_sls" />
        </root>
    </springProfile>

    <!-- prod 环境：仅推送到 SLS，不输出到控制台 -->
    <springProfile name="prod">
        <root level="INFO">
            <appender-ref ref="aliyun_sls" />
        </root>
    </springProfile>
</configuration>
```

**为什么用 `${ALIYUN_ACCESS_KEY_ID}` 而不是直接写密钥？**

AccessKey 是访问阿里云的凭证，相当于"用户名+密码"。硬编码在代码里会被 Git 记录，一旦代码公开就等于泄露密钥。GitHub 的密钥扫描会自动拦截包含明文 AccessKey 的推送。正确做法是通过环境变量注入，运行时设置：

```bash
# Linux/macOS
export ALIYUN_ACCESS_KEY_ID=your_key_id
export ALIYUN_ACCESS_KEY_SECRET=your_key_secret

# Windows PowerShell
$env:ALIYUN_ACCESS_KEY_ID = "your_key_id"
$env:ALIYUN_ACCESS_KEY_SECRET = "your_key_secret"
```

> **阿里云建议：** 生产环境使用 RAM 角色而非 AccessKey，通过 ECS 元数据服务自动获取临时凭证，避免密钥管理问题。

详细的 SLS 配置步骤见 [阿里云配置指南](./01-aliyun-guide.md#3-日志服务-sls)。

---

## 5. 公共基础库 — coffee-common

### 定位与职责

`coffee-common` 是所有模块共享的**基础工具库**，就像项目的"零件标准库"——不包含任何业务逻辑，只存放多个服务都需要的通用类。

**谁依赖它：**

```
coffee-common
    ├── coffee-userorder/api（依赖 BasePageDTO、PageDTO）
    ├── coffee-expresstrack/api（依赖 BasePageDTO、PageDTO）
    └── coffee-app（间接通过 api 模块使用）
```

### 目录结构

```
coffee-common/
├── pom.xml
└── src/main/java/com/coffee/yun/dto/
    ├── BasePageDTO.java    分页查询基础参数
    └── PageDTO.java        分页查询返回结果
```

---

### BasePageDTO.java — 分页基础参数

```java
package com.coffee.yun.dto;

/**
 * 所有分页查询参数的基类。
 * 通过继承，各查询 DTO 无需重复声明 pageNum 和 pageSize。
 */
public class BasePageDTO {
    private Integer pageNum;    // 当前页码，从 1 开始
    private Integer pageSize;   // 每页显示条数

    // getter/setter
}
```

**使用示例：**

```java
// 订单查询参数继承后，自动拥有分页能力
public class UserOrderInfoParamDTO extends BasePageDTO {
    private String order_id;    // 业务字段
    // 同时继承了 pageNum、pageSize
}
```

---

### PageDTO.java — 分页返回结果

```java
package com.coffee.yun.dto;

import java.util.List;

/**
 * 所有分页查询的统一返回格式。
 * 泛型 T：数据列表中每条记录的类型，根据实际业务指定。
 */
public class PageDTO<T> {
    private List<T> list;    // 当前页的数据列表
    private Long total;      // 数据总条数（供前端计算总页数）

    // getter/setter
}
```

**泛型的意义：**

```java
// 快递服务返回的分页结果
PageDTO<ExpressTrackInfoResultDTO> trackPage = ...
// list 里是快递轨迹对象，total 是总轨迹条数

// 如果未来加了商品服务
PageDTO<ProductDTO> productPage = ...
// 同一个 PageDTO 类复用，无需重复编写分页包装逻辑
```

---

### pom.xml 关键依赖说明

```xml
<dependencies>
    <!-- Lombok：自动生成 getter/setter/toString/构造方法 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>

    <!-- FastJSON2：fastjson 的重写版本，性能更好、安全性更高（fastjson 1.x 多次出现安全漏洞）-->
    <dependency>
        <groupId>com.alibaba.fastjson2</groupId>
        <artifactId>fastjson2</artifactId>
        <version>2.0.53</version>
    </dependency>

    <!-- PageHelper：MyBatis 分页插件，自动处理分页 SQL（无需手写 LIMIT） -->
    <dependency>
        <groupId>com.github.pagehelper</groupId>
        <artifactId>pagehelper-spring-boot-starter</artifactId>
    </dependency>

    <!-- Hutool：国产工具类库，提供字符串处理、日期转换等常用方法 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
    </dependency>
</dependencies>
```

---

[← 返回主文档](../README.md)
