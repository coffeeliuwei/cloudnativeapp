# 各模块代码详解

> 本文档按照系统架构层次，从用户能看到的前端界面开始，逐层向内讲解每个模块的代码结构、设计意图和关键实现。
>
> **阅读建议**：跟着架构图的数据流方向阅读——先看前端（展示层），再看网关，最后深入微服务内部——这样能建立起完整的调用心智模型。

## 架构层次总览

```
┌──────────────────────────────────────────────────────────┐
│  第1层：展示层（app-admin）                                │
│  Vue.js 3 + ViewUI Plus  端口:8080                       │
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
| [app-admin](#1-展示层--app-admin) | 展示层 | Vue.js 3.4 + ViewUI Plus 1.x | 8080 |
| [coffee-app](#2-网关层--coffee-app) | 网关层 | Spring Boot | 8005 |
| [coffee-userorder](#3-微服务层--coffee-userorder用户订单微服务) | 微服务层 | Spring Boot + Dubbo + MyBatis | 7001 |
| [coffee-expresstrack](#4-微服务层--coffee-expresstrack快递轨迹微服务) | 微服务层 | Spring Boot + Dubbo + MyBatis | HTTP:8001 / Dubbo:28888 |
| [coffee-common](#5-公共基础库--coffee-common) | 公共基础库 | Java | — |

---

## 1. 展示层 — app-admin

### 定位与职责

`app-admin` 是用户直接使用的管理后台界面。它基于 **Vue.js 3.4 + ViewUI Plus**（iView 的 Vue 3 兼容版本）构建，负责：

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
import { createApp } from 'vue'      // Vue 3：用 createApp() 取代 new Vue()
import App from './App'
import router from './router'        // vue-router 4.x
import store from './store'          // vuex 4.x
import ViewUIPlus from 'view-ui-plus' // ViewUI Plus（iView 的 Vue 3 版本）
import i18n from '@/locale'          // vue-i18n 9.x
import config from '@/config'
import importDirective from '@/directive'
import installPlugin from '@/plugin'
import './index.less'
import 'view-ui-plus/dist/styles/viewuiplus.css'

const app = createApp(App)

// 注册 ViewUI Plus，并接入 vue-i18n 国际化
app.use(ViewUIPlus, {
  i18n: (key, value) => i18n.global.t(key, value)
})

app.use(router)
app.use(store)
app.use(i18n)

// Vue 3：用 globalProperties 替代 Vue 2 的 Vue.prototype
app.config.globalProperties.$config = config

// 注册自定义指令和插件
importDirective(app)
installPlugin(app)

app.mount('#app')  // 挂载到 index.html 中 id="app" 的元素
```

**Vue 2 → Vue 3 核心变化：**

| Vue 2 写法 | Vue 3 写法 | 说明 |
|-----------|-----------|------|
| `new Vue({ el: '#app' })` | `createApp(App).mount('#app')` | 应用实例创建方式 |
| `Vue.prototype.$xxx = ...` | `app.config.globalProperties.$xxx = ...` | 全局属性挂载 |
| `import iView from 'iview'` | `import ViewUIPlus from 'view-ui-plus'` | UI 组件库升级 |
| `Vue.use(iView)` | `app.use(ViewUIPlus, { i18n: ... })` | 插件注册方式 |

**关键点：** API 发起请求在本项目中通过 `src/api/index.js` 封装，各组件直接 `import` 对应函数调用，无需挂载到全局。

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

这是本项目最核心的业务页面，调用 `coffee-app` 的接口展示快递轨迹。组件逻辑抽离到 `order.js`，`order.vue` 只保留模板：

```vue
<!-- order.vue：只负责 HTML 结构，逻辑在 order.js -->
<template>
  <div class="order-page">
    <!-- 查询条件行 -->
    <Row>
      <Form ref="searchForm" :model="searchForm" inline class="search-form">
        <FormItem>
          <!-- ViewUI Plus 组件：输入框（与 iView 同名，API 兼容） -->
          <Input type="text" v-model="searchForm.order_id"
                 clearable placeholder="输入订单号" style="width: 200px" />
        </FormItem>
        <FormItem>
          <Button type="primary" @click="handleSubmit">搜索</Button>
        </FormItem>
      </Form>
    </Row>
    <!-- 数据表格 + 分页 -->
    <Row>
      <Table stripe border :columns="columns" :data="data"
             @on-row-click="getTrip" />
      <Row type="flex" justify="end" class="page">
        <Page :current="searchForm.pageNum" :total="total"
              :page-size="searchForm.pageSize"
              :page-size-opts="[10,20,50]"
              size="small" show-total show-elevator show-sizer />
      </Row>
    </Row>
  </div>
</template>

<script>
import vm from './order.js'  // 逻辑复用：将 Options 对象抽到单独的 js 文件
export default vm
</script>
```

```javascript
// order.js：组件的 Options API 逻辑（Vue 3 完全兼容 Options API）
import { findOrderList } from '@/api/index'

export default {
  data() {
    return {
      searchForm: { pageNum: 1, pageSize: 10, order_id: '' },
      columns: [
        { type: 'index', width: 60, align: 'center', fixed: 'left' },
        { title: '订单编号', key: 'order_id' },
        { title: '快递编号', key: 'express_id' },
        { title: '重量',     key: 'express_weight' },
        { title: '轨迹',     key: 'track_show' }
      ],
      data: [],
      total: 0
    }
  },
  mounted() {
    this.getOrderList()
  },
  methods: {
    handleSubmit() {
      this.searchForm.pageNum = 1
      this.getOrderList()
    },
    getOrderList() {
      // findOrderList 封装了 POST /findOrderList 接口调用
      findOrderList(this.searchForm).then(res => {
        const body = res.data
        if (body && body.success === true) {
          this.data = body.result.list   // 后端 PageDTO.list
          this.total = body.result.total // 后端 PageDTO.total
        }
      })
    },
    getTrip(row) {
      // 点击行时记录选中项（可扩展详情弹窗）
      this.chooseItem = row
    }
  }
}
```

**Vue 核心概念一览：**

| 概念 | 代码体现 | 含义 |
|------|---------|------|
| 双向绑定 | `v-model="searchForm.order_id"` | 输入框和变量自动同步 |
| 事件绑定 | `@click="handleSubmit"` | 点击按钮执行 handleSubmit 方法 |
| 属性绑定 | `:data="data"` | 表格数据来自 data 变量 |
| 响应式 | `this.data = body.result.list` | 赋值后页面自动刷新，无需手动操作 DOM |
| 生命周期 | `mounted()` | 组件挂载后自动发起首次查询 |

> **Vue 3 兼容性说明：** Vue 3 完全支持 Options API（`data / methods / mounted`），不强制使用 Composition API。ViewUI Plus 的组件名（`<Input>`、`<Button>`、`<Table>`、`<Page>`）与 iView 保持一致，迁移无需修改模板。

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
     * @DubboReference 是 Dubbo 的核心注解。
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
├── api/
│   └── src/main/java/com/coffee/yun/userorder/api/
│       ├── dto/
│       │   ├── UserOrderInfoParamDTO.java    查询订单请求参数
│       │   ├── UserOrderInfoResultDTO.java   查询订单返回结果
│       │   └── UserOrderCreateDTO.java       创建订单请求参数（RocketMQ 演示用）
│       └── service/
│           └── UserOrderInfoService.java     服务接口（含 createOrder 方法）
└── provider/
    └── src/main/
        ├── java/com/coffee/yun/userorder/
        │   ├── UserOrderApplication.java         启动类
        │   ├── config/
        │   │   ├── RedisConfig.java               Redis 序列化配置（JSON 存储）
        │   │   └── CacheProperties.java           缓存 TTL 配置（支持 Nacos 热更新）
        │   └── provider/service/
        │       └── UserOrderInfoServiceImpl.java  接口实现（含缓存 + RocketMQ 逻辑）
        └── resources/
            ├── application.yml
            ├── application-dev.yml
            ├── logback-spring.xml
            └── mapper/
                └── UserOrderMapper.xml
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

#### UserOrderCreateDTO.java — 创建订单请求参数

这是为 RocketMQ 演示新增的 DTO，携带创建订单所需的字段：

```java
package com.coffee.yun.userorder.api.dto;

import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

/**
 * 创建订单请求参数 DTO
 *
 * 放在 api 模块的原因：coffee-app 通过 Dubbo 调用 createOrder() 时，
 * 需要把此 DTO 序列化后通过网络传输，因此必须实现 Serializable，
 * 且定义在 api 模块中，消费方才能引用同一个类。
 *
 * 请求体示例（POST /createOrder）：
 *   { "order_id": "ORDER100", "OneID": "U001", "order_amount": 199.00 }
 */
@Getter
@Setter
public class UserOrderCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String order_id;      // 订单编号（建议使用 UUID 或雪花算法）
    private String OneID;         // 会员唯一标识，关联 member 表
    private float  order_amount;  // 订单金额（元）
}
```

---

#### UserOrderInfoService.java — 服务接口

```java
package com.coffee.yun.userorder.api.service;

import com.coffee.yun.dto.PageDTO;
import com.coffee.yun.userorder.api.dto.*;

/**
 * 用户订单服务接口——只定义"能做什么"，不写"怎么做"。
 *
 * 新增 createOrder() 用于演示 RocketMQ 异步解耦：
 *   coffee-app 调用 createOrder() → 订单入库 → 发布 MQ 消息
 *   coffee-expresstrack 异步消费消息 → 创建快递单
 */
public interface UserOrderInfoService {
    UserOrderInfoResultDTO findUserOrderInfo(UserOrderInfoParamDTO param);
    PageDTO<UserOrderInfoResultDTO> findUserOrderInfos(UserOrderInfoParamDTO param);
    String createOrder(UserOrderCreateDTO createDTO);  // 新增：创建订单 + 触发 MQ
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

#### RedisConfig.java — Redis 序列化配置

Spring Boot 默认的 `RedisTemplate` 使用 JDK 序列化，key 和 value 都是二进制乱码，无法在 `redis-cli` 中直接阅读。本配置类将其替换为：**key 用字符串**、**value 用 JSON**。

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // key 序列化：直接存为 UTF-8 字符串，redis-cli 可读
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // value 序列化：存为 JSON，并嵌入类型信息供反序列化时还原正确的 Java 类型
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);

        // Spring Boot 3.x 新 API（旧版用 setObjectMapper()）
        Jackson2JsonRedisSerializer<Object> jsonSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
```

**Redis key 命名规范（本服务）：**

```
order:detail:{order_id}      示例：order:detail:ORDER001
```

在 `redis-cli` 中可以用 `KEYS order:detail:*` 查看所有订单缓存。

---

#### CacheProperties.java — 缓存 TTL 配置（支持 Nacos 热更新）

```java
@Getter
@Setter
@Component
@RefreshScope   // 关键：让这个 Bean 在 Nacos 配置变更时自动重建，读取最新值
public class CacheProperties {

    // 从 Nacos Config 或本地配置读取，默认 1800 秒（30 分钟）
    // 在 Nacos 控制台修改 cache.ttl.order=60 后，下次缓存写入立即使用新值，无需重启
    @Value("${cache.ttl.order:1800}")
    private long orderTtl;
}
```

**`@RefreshScope` 工作流程：**

```
Nacos 控制台修改 cache.ttl.order=60
    ↓ Spring Cloud 检测到配置变更，触发 ContextRefreshedEvent
    ↓ RefreshScope 销毁旧的 CacheProperties Bean
    ↓ 下次注入时重新创建，@Value 读取新值 60
    ↓ 下一次缓存写入时 TTL=60s（无需重启任何服务）
```

---

#### UserOrderInfoServiceImpl.java — 接口实现（含缓存 + RocketMQ）

这是本服务改造的核心文件，新增了三项能力（均可通过配置开关控制）：

```java
@Slf4j
@DubboService
public class UserOrderInfoServiceImpl implements UserOrderInfoService {

    @Autowired private SqlSessionTemplate sqlSessionTemplate;
    @Autowired private PageUtil pageUtil;
    @Autowired private RedisTemplate<String, Object> redisTemplate;
    @Autowired private CacheProperties cacheProperties;

    // required=false：没有 RocketMQ 时不报错，Bean 保持 null
    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    @Value("${feature.cache.enabled:false}")
    private boolean cacheEnabled;   // dev 默认 false，prod 默认 true

    @Value("${feature.mq.enabled:false}")
    private boolean mqEnabled;      // dev 默认 false，prod 默认 true

    private static final String CACHE_KEY_PREFIX   = "order:detail:";
    private static final String ORDER_CREATED_TOPIC = "order-created";

    /** 查询单条订单——Cache-Aside Pattern（旁路缓存）*/
    @Override
    public UserOrderInfoResultDTO findUserOrderInfo(UserOrderInfoParamDTO param) {
        String orderId = param.getOrder_id();

        if (cacheEnabled && orderId != null && !orderId.isEmpty()) {
            String cacheKey = CACHE_KEY_PREFIX + orderId;

            // ① 先查 Redis
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.info("缓存命中，order_id={}", orderId);
                return (UserOrderInfoResultDTO) cached;  // 直接返回，不查数据库
            }

            // ② 缓存未命中，查数据库
            UserOrderInfoResultDTO result = sqlSessionTemplate.selectOne(
                    "UserOrderMapper.selectByParam", param);

            // ③ 写回 Redis（TTL 由 Nacos Config 控制，支持热更新）
            if (result != null) {
                redisTemplate.opsForValue().set(cacheKey, result,
                        cacheProperties.getOrderTtl(), TimeUnit.SECONDS);
            }
            return result;
        }

        // cacheEnabled=false：直接查数据库（与改造前行为相同）
        return sqlSessionTemplate.selectOne("UserOrderMapper.selectByParam", param);
    }

    /** 创建订单——写数据库 + 可选发布 RocketMQ 消息 */
    @Override
    public String createOrder(UserOrderCreateDTO createDTO) {
        // ① 写入 order 表
        Map<String, Object> params = new HashMap<>();
        params.put("order_id",     createDTO.getOrder_id());
        params.put("OneID",        createDTO.getOneID());
        params.put("order_amount", createDTO.getOrder_amount());
        params.put("order_status", "待发货");
        sqlSessionTemplate.insert("UserOrderMapper.insertOrder", params);

        // ② 发布消息（feature.mq.enabled=true 时才执行）
        if (mqEnabled && rocketMQTemplate != null) {
            try {
                rocketMQTemplate.send(ORDER_CREATED_TOPIC,
                        MessageBuilder.withPayload(createDTO.getOrder_id()).build());
                log.info("消息已发布，topic={}", ORDER_CREATED_TOPIC);
            } catch (Exception e) {
                // MQ 失败不影响下单，仅记录警告
                log.warn("RocketMQ 消息发布失败：{}", e.getMessage());
            }
        }
        return createDTO.getOrder_id();
    }
}
```

**功能开关设计的意义：**

| 开关 | dev（本地） | prod（生产） | 说明 |
|------|------------|------------|------|
| `feature.cache.enabled` | false | true | 本地没有 Redis 也能启动 |
| `feature.mq.enabled` | false | true | 本地没有 RocketMQ 也能启动 |

**`@DubboService` / `@DubboReference` 与 Spring 注解的核心区别：**

| 注解 | 归属 | 作用 | 使用场景 |
|------|------|-----|---------|
| `@Service`（Spring） | Spring 框架 | 注册到 Spring 容器 | 同一应用内的组件 |
| `@Autowired`（Spring） | Spring 框架 | 注入本地 Bean | 同一进程内依赖注入 |
| `@DubboService` | Dubbo 框架 | 注册到 Nacos，暴露 RPC 接口 | **需要被其他应用远程调用**的服务 |
| `@DubboReference` | Dubbo 框架 | 创建远程代理，注入调用方 | **调用远程 Dubbo 服务** |

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
        │   ├── config/
        │   │   ├── RedisConfig.java              Redis 序列化配置（JSON 存储）
        │   │   └── CacheProperties.java          缓存 TTL 配置（支持 Nacos 热更新）
        │   ├── mq/
        │   │   └── OrderCreatedConsumer.java     RocketMQ 消费者（异步创建快递单）
        │   └── provider/service/
        │       └── ExpresstrackInfoServiceImpl.java   实现类（含缓存逻辑）
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

#### RedisConfig.java — Redis 序列化配置

与 `coffee-userorder` 中的 `RedisConfig.java` 结构完全相同，区别仅在于 key 前缀和 value 类型：

- **key** 格式：`express:track:{order_id}`，示例：`express:track:ORDER001`
- **value** 类型：`PageDTO<ExpressTrackInfoResultDTO>`（分页列表）

序列化配置代码见 [coffee-userorder RedisConfig 说明](#redisconfigjava--redis-序列化配置)，原理一致。

---

#### CacheProperties.java — 缓存 TTL 配置（支持 Nacos 热更新）

```java
@Getter
@Setter
@Component
@RefreshScope   // Nacos Config 变更时自动重建 Bean，TTL 立即生效
public class CacheProperties {

    // 从 Nacos Config 读取，默认 3600 秒（1 小时）
    // 在 Nacos 控制台修改 coffee-expresstrack.properties 中的 cache.ttl.expresstrack=60
    // 下次缓存写入立即使用新值，无需重启
    @Value("${cache.ttl.expresstrack:3600}")
    private long expresstrackTtl;
}
```

**Nacos Config 配置示例：**

```
Data ID：coffee-expresstrack.properties
Group：DEFAULT_GROUP
内容：cache.ttl.expresstrack=60
```

---

#### OrderCreatedConsumer.java — RocketMQ 消息消费者（核心新增文件）

这是 RocketMQ 异步解耦的核心实现。当 `coffee-userorder` 创建订单并发布消息后，本类负责异步消费消息，为订单创建快递单和初始轨迹记录。

```java
@Slf4j
@Component
// 条件装配：仅当 feature.mq.enabled=true 时注册此 Bean，否则不连 RocketMQ，本地无 MQ 也能启动
@ConditionalOnProperty(name = "feature.mq.enabled", havingValue = "true", matchIfMissing = false)
// 声明监听的 Topic 和消费者分组
@RocketMQMessageListener(
        topic = "order-created",
        consumerGroup = "expresstrack-consumer-group"
)
public class OrderCreatedConsumer implements RocketMQListener<String> {

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    /**
     * RocketMQ 框架收到消息后自动调用此方法
     * @param orderId  消息体，即刚创建的订单编号（由 UserOrderInfoServiceImpl.createOrder() 发布）
     */
    @Override
    public void onMessage(String orderId) {
        log.info("收到订单创建消息，order_id={}，开始创建快递单", orderId);

        // 步骤 1：创建快递单（INSERT INTO express）
        String expressId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Map<String, Object> expressParams = new HashMap<>();
        expressParams.put("express_id",     expressId);
        expressParams.put("order_id",       orderId);
        expressParams.put("express_weight", "1kg");
        sqlSessionTemplate.insert("ExpressTrackMapper.insertExpress", expressParams);

        // 步骤 2：创建初始轨迹记录（INSERT INTO track）
        String trackId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Map<String, Object> trackParams = new HashMap<>();
        trackParams.put("track_id",   trackId);
        trackParams.put("express_id", expressId);
        trackParams.put("track_show", "商家已揽件");
        sqlSessionTemplate.insert("ExpressTrackMapper.insertTrack", trackParams);
    }
}
```

**消息流转图：**

```
POST /createOrder
    → coffee-app
    → Dubbo RPC
    → UserOrderInfoServiceImpl.createOrder()
        ① INSERT INTO order（写数据库）
        ② rocketMQTemplate.send("order-created", orderId)  ← 立即返回成功给用户
                                    ↓ 异步（后台）
                    RocketMQ Broker 存储消息
                                    ↓
            OrderCreatedConsumer.onMessage(orderId)
                ① INSERT INTO express（创建快递单）
                ② INSERT INTO track（"商家已揽件"）
```

**关键注解说明：**

| 注解 | 作用 |
|------|------|
| `@ConditionalOnProperty` | 功能开关：`feature.mq.enabled=false` 时不创建此 Bean |
| `@RocketMQMessageListener` | 声明 Topic 和消费者组，Starter 自动创建消费者连接 |
| `implements RocketMQListener<String>` | 消息体类型为 String，与生产者发送的 orderId 对应 |

---

#### ExpresstrackInfoServiceImpl.java — 接口实现（含 Redis 缓存）

查询轨迹时的 Cache-Aside Pattern：

```java
@Slf4j
@DubboService
public class ExpresstrackInfoServiceImpl implements ExpressTrackInfoService {

    @Autowired private PageUtil pageUtil;
    @Autowired private RedisTemplate<String, Object> redisTemplate;
    @Autowired private CacheProperties cacheProperties;

    @Value("${feature.cache.enabled:false}")
    private boolean cacheEnabled;

    private static final String CACHE_KEY_PREFIX = "express:track:";

    @Override
    @SuppressWarnings("unchecked")
    public PageDTO<ExpressTrackInfoResultDTO> findExpressTrackInfos(
            ExpressTrackInfoParamDTO param) {
        String orderId = param.getOrder_id();

        if (cacheEnabled && orderId != null && !orderId.isEmpty()) {
            String cacheKey = CACHE_KEY_PREFIX + orderId;

            // ① 查 Redis
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.info("缓存命中，order_id={}", orderId);
                return (PageDTO<ExpressTrackInfoResultDTO>) cached;
            }

            // ② 查数据库
            PageDTO<ExpressTrackInfoResultDTO> result =
                    pageUtil.selectPage("ExpressTrackMapper.selectByParam", param);

            // ③ 写回 Redis，TTL 由 Nacos Config 控制
            if (result != null) {
                redisTemplate.opsForValue().set(
                        cacheKey, result, cacheProperties.getExpresstrackTtl(), TimeUnit.SECONDS);
            }
            return result;
        }

        // 直通路径：关闭缓存时直接查数据库
        return pageUtil.selectPage("ExpressTrackMapper.selectByParam", param);
    }
}
```

---

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

    <!-- FastJSON2：JSON 序列化库，性能更好、安全性更高 -->
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

---

## 6. 技术原理深度解析

> 本章独立讲解代码中涉及的关键技术原理，补充"代码是什么"之外的"为什么这样设计"。

---

### 6.1 PageHelper 分页原理——它怎么"拦截"SQL？

项目使用 PageHelper 做分页，但代码里看不到任何 `LIMIT` 语句——分页是自动加上去的。原理是 **MyBatis 拦截器（Interceptor）**。

**没有 PageHelper 时，手写分页的痛苦：**

```java
// 每个查询都要手动写两条 SQL
// 1. 查数据（带 LIMIT）
List<User> list = sqlSession.selectList(
    "selectUsers", null, new RowBounds(offset, limit));

// 2. 查总数（不带 LIMIT）
Long total = sqlSession.selectOne("countUsers", param);
```

**使用 PageHelper 后：**

```java
// 调用前设置分页参数
PageHelper.startPage(pageNum, pageSize);   // 比如：第2页，每页10条

// 正常执行查询（不带 LIMIT 的 SQL）
List<User> list = sqlSession.selectList("selectUsers", param);
// PageHelper 自动将 SQL 改写为：
//   SELECT ... FROM user WHERE ... LIMIT 10 OFFSET 10

// 结果自动包含总数
PageInfo<User> pageInfo = new PageInfo<>(list);
long total = pageInfo.getTotal();   // PageHelper 还自动执行了 COUNT(*) 查询
```

**拦截器工作机制图示：**

```
代码调用 selectList("selectUsers", param)
    ↓
MyBatis 拦截器链（PageHelper 注册在此）
    ↓  PageHelper 检测到当前线程有分页参数（ThreadLocal 存储）
    ↓  将原 SQL: SELECT ... FROM user WHERE ...
    ↓  改写为:  SELECT ... FROM user WHERE ... LIMIT 10 OFFSET 10
    ↓  同时执行: SELECT COUNT(*) FROM (原 SQL) 获取总数
    ↓
数据库执行改写后的 SQL，返回结果
```

`PageHelper.startPage()` 通过 **ThreadLocal** 传递分页参数——同一线程里，`startPage()` 和后续的查询共享这个参数，其他线程不受影响。这就是为什么必须在查询**紧接之前**调用 `startPage()`，中间不能插入其他查询操作。

---

### 6.2 `SqlSessionTemplate` 和 Mapper 接口——两种 MyBatis 使用方式

本项目使用 `SqlSessionTemplate` 直接执行 SQL：

```java
@Autowired
private SqlSessionTemplate sqlSessionTemplate;

// 通过字符串 statement 名称执行查询
List<UserOrderInfoResultDTO> list = sqlSessionTemplate.selectList(
    "UserOrderMapper.selectByParam", param);
```

另一种更常见的方式是 **Mapper 接口**：

```java
// 定义接口，方法名对应 XML 中的 id
@Mapper
public interface UserOrderMapper {
    List<UserOrderInfoResultDTO> selectByParam(UserOrderInfoParamDTO param);
}

// 注入使用——MyBatis 自动生成代理实现类
@Autowired
private UserOrderMapper userOrderMapper;

List<UserOrderInfoResultDTO> list = userOrderMapper.selectByParam(param);
```

**两种方式的对比：**

| 对比维度 | `SqlSessionTemplate` | Mapper 接口 |
|---------|---------------------|------------|
| 类型安全 | ❌ 字符串引用，拼写错误只在运行时发现 | ✅ 编译时检查，IDE 自动补全 |
| 代码量 | 较少（不需要额外接口） | 需要写接口文件 |
| 重构友好 | ❌ 重命名 XML id 时需要全局搜索字符串 | ✅ 重构工具可自动追踪 |
| 主流程度 | 早期写法，现在较少见 | ✅ **当前主流推荐写法** |

**本项目为何用 `SqlSessionTemplate`？** 主要是历史原因——早期 MyBatis 版本 Mapper 接口功能不完整，部分老代码使用这种方式。新项目建议优先使用 Mapper 接口。

---

### 6.3 Dubbo fastjson2 序列化——为什么要指定序列化方式？

在 Dubbo 配置中经常看到：

```yaml
dubbo:
  provider:
    serialization: fastjson2
```

这控制的是 **RPC 调用时数据如何在网络上传输**。

**序列化的必要性：**

```
coffee-app 内存中的 Java 对象：
  UserOrderInfoParamDTO { order_id = "ORDER001", pageNum = 1 }
             ↓ 序列化（对象 → 字节流）
网络传输：  [0x7B 0x22 0x6F 0x72 0x64 0x65 0x72 ...]（字节数据）
             ↓ 反序列化（字节流 → 对象）
coffee-userorder 收到：
  UserOrderInfoParamDTO { order_id = "ORDER001", pageNum = 1 }
```

**Dubbo 支持的序列化方式对比：**

| 序列化方式 | 性能 | 可读性 | 跨语言 | 适用场景 |
|-----------|------|--------|--------|---------|
| Hessian2（默认）| 中 | ❌ 二进制 | 有限 | Dubbo 传统方案 |
| **fastjson2** | 高 | ✅ JSON 文本 | ✅ | Java 微服务，调试方便 |
| Protobuf | 最高 | ❌ 二进制 | ✅ 最佳 | 高性能跨语言场景 |
| JDK 原生 | 低 | ❌ 二进制 | ❌ | 不推荐 |

**不指定会怎样？** Dubbo 默认用 Hessian2，功能上没问题，但：
- 日志里看到的是二进制乱码，调试困难
- 与某些版本的 Dubbo 存在兼容性问题
- fastjson2 在 JSON 解析速度上有明显优势

**fastjson2 的一个注意事项：** 序列化的类必须有无参构造方法（默认都有，使用 Lombok 的 `@Data` 时注意如果同时加了 `@AllArgsConstructor` 需要再加 `@NoArgsConstructor`）。

---

### 6.4 `@SpringBootApplication` 三合一原理

启动类上这一个注解包含了三个功能：

```java
@SpringBootApplication
// 等价于同时加上：
@SpringBootConfiguration    // 1. 这是一个 Spring 配置类
@EnableAutoConfiguration    // 2. 开启自动配置
@ComponentScan              // 3. 开启组件扫描
public class CoffeeAppApplication { ... }
```

**三个功能分别做什么：**

#### `@SpringBootConfiguration`

标记此类是 Spring 的配置类，相当于 XML 时代的 `applicationContext.xml`。可以在此类中用 `@Bean` 方法手动注册 Bean。

#### `@EnableAutoConfiguration`（最重要）

Spring Boot 的核心魔法——**根据 classpath 里有什么 jar，自动推断并配置对应的 Bean**。

```
classpath 里有 spring-boot-starter-web？
  → 自动配置 DispatcherServlet、Jackson（JSON序列化）、Tomcat 服务器

classpath 里有 mybatis-spring-boot-starter？
  → 自动配置 SqlSessionFactory、MapperScannerConfigurer

classpath 里有 dubbo-spring-boot-starter？
  → 自动注册 Dubbo 服务、连接 Nacos

没有 DataSourceAutoConfiguration？（被 exclude 排除）
  → 不配置数据库连接池（coffee-app 不连库，所以排除它）
```

原理是读取每个 jar 包里的 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件，里面列出了该 jar 提供的自动配置类。

#### `@ComponentScan`

扫描**启动类所在包及其子包**下的所有 `@Component`、`@Service`、`@Repository`、`@Controller` 注解类，将它们注册为 Spring Bean。

```
CoffeeAppApplication 在 com.coffee.yun.coffeeapp 包
→ 扫描范围：com.coffee.yun.coffeeapp 及所有子包
→ 找到 CoffeeController（@RestController）→ 注册为 Bean
```

**这就是为什么启动类必须放在最顶层包**——如果放错位置，子包里的 Controller 就扫描不到，接口就无法访问。

---

### 6.5 配置分层与敏感信息保护

#### 配置文件的两层结构

```
application.yml          ← 提交到 Git，只放"骨架"配置
application-dev.yml      ← 不提交 Git，放真实密码和地址
```

`application.yml` 用占位符引用变量：

```yaml
spring:
  datasource:
    url: jdbc:mysql://${database.host}/${database.dbname}
    username: ${database.user}
    password: ${database.password}
```

`application-dev.yml` 填充真实值：

```yaml
# 这个文件不提交 Git！
database:
  host: rm-bp1xxxx.mysql.rds.aliyuncs.com:3306
  dbname: userordertest
  user: admin
  password: MySecretPassword123
```

Spring Boot 启动时，`spring.profiles.active=dev` 激活 dev 配置，两个文件的内容合并，占位符被真实值替换。

#### `.gitignore` 的作用

`.gitignore` 文件告诉 Git "这些文件不要追踪"：

```gitignore
# 个人环境配置（含数据库密码）
application-dev.yml
application-local.yml

# 编译产物
target/
*.class

# IDE 配置（每个人不同，不应共享）
.idea/
*.iml
```

**如果密钥被意外提交了怎么办？**

即使立刻删除文件再提交，Git 历史里仍然保存着密钥——任何能看到仓库历史的人都能找到。**正确处理方式是立即更换密钥**，不要指望通过修改历史来掩盖。

#### 生产环境的最佳实践

生产环境不应该用文件存储密码，而是用环境变量注入：

```bash
# Linux/macOS（ECS 服务器上执行）
export DATABASE_PASSWORD=MySecretPassword123
export ALIYUN_ACCESS_KEY_ID=LTAI4xxxxx
export ALIYUN_ACCESS_KEY_SECRET=xxxxxxxxxxxxxx
```

```yaml
# application.yml 中用环境变量
spring:
  datasource:
    password: ${DATABASE_PASSWORD}
```

这样密码只存在于服务器内存和运维人员的安全工具中，代码仓库里没有任何敏感信息。

---

[← 返回主文档](../README.md)
