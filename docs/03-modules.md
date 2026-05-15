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
│       ├── login/login.vue        登录页（本地校验，无需后端接口）
│       ├── single-page/home/      首页（静态图表）
│       ├── order-manage/          订单管理页（查询订单列表、新建订单）
│       └── order/order.vue        轨迹查询页（按订单号查快递轨迹）
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
      }
    ]
  },
  {
    path: '/order-manage',        // 访问 /order-manage 时显示订单管理页
    component: Main,
    children: [
      {
        path: '/order-manage',
        component: () => import('@/view/order-manage/order-manage.vue'),
        meta: { title: '订单管理', icon: 'md-cart' }  // 调用 POST /findOrders、POST /createOrder
      }
    ]
  },
  {
    path: '/order',               // 访问 /order 时显示轨迹查询页
    component: Main,
    children: [
      {
        path: '/order',
        component: () => import('@/view/order/order.vue'),
        meta: { title: '轨迹查询', icon: 'md-locate' }  // 调用 POST /findOrderList
      }
    ]
  }
]
```

**懒加载（`() => import(...)`）：** 只有当用户真正访问该路由时，才加载对应的 Vue 组件代码，减少初始加载体积。

---

### src/store/module/user.js — 用户状态管理

Vuex 是 Vue 的官方状态管理方案，解决"多个组件需要共享同一份数据"的问题。

**本项目的登录采用本地校验**：输入任意非空用户名和密码即可进入，无需后端 `/login` 接口。后端只负责业务接口（订单、快递），不提供认证服务。

```javascript
export default {
  state: {
    userName: '',    // 当前登录用户名
    token: ''        // 本地生成的 token（'local-token-' + userName）
  },
  actions: {
    // 登录：本地校验，非空即通过，无需调后端
    handleLogin({ commit }, { userName, password }) {
      return new Promise((resolve, reject) => {
        if (userName && password) {
          commit('setToken', 'local-token-' + userName)
          resolve()
        } else {
          reject(new Error('用户名或密码不能为空'))
        }
      })
    },
    // 用户信息：返回本地固定数据，无需调后端 /get_info
    getUserInfo({ state, commit }) {
      return new Promise((resolve) => {
        const data = { name: state.token.replace('local-token-', ''), access: ['super_admin'] }
        commit('setUserName', data.name)
        commit('setAccess', data.access)
        commit('setHasGetInfo', true)
        resolve(data)
      })
    }
  }
}
```

**state / mutations / actions 三者的关系：**

```
用户操作（如点击登录按钮）
    ↓ dispatch
  actions（处理逻辑，本项目为本地校验）
    ↓ commit
  mutations（同步修改 state）
    ↓
  state 更新 → 页面自动重新渲染
```

> **教学说明：** 生产系统中登录应调用后端接口验证账号密码、颁发 JWT。本项目为了让学习者聚焦在微服务核心逻辑（Dubbo RPC、服务注册、数据库），将登录简化为本地校验。

---

### src/view/order-manage/ — 订单管理页

调用 `POST /findOrders` 展示订单列表，并通过弹窗调用 `POST /createOrder` 新建订单。

```javascript
// order-manage.js（关键逻辑摘要）
import { findOrders, createOrder } from '@/api/index'

export default {
  data() {
    return {
      searchForm: { pageNum: 1, pageSize: 10, order_id: '', member_name: '' },
      columns: [
        { title: '订单编号', key: 'order_id' },
        { title: '会员姓名', key: 'member_name' },
        { title: '手机号',   key: 'member_phone' },
        { title: '订单状态', key: 'order_status' },
        { title: '金额（元）', key: 'order_amount' }
      ],
      showCreate: false,   // 控制新建订单弹窗显示
      createForm: { order_id: '', OneID: '', order_amount: '' }
    }
  },
  methods: {
    getOrders() {
      findOrders(this.searchForm).then(res => {  // POST /findOrders
        if (res.data.success) {
          this.data = res.data.result.list
          this.total = res.data.result.total
        }
      })
    },
    handleCreate() {
      createOrder(this.createForm).then(res => { // POST /createOrder
        if (res.data.success) {
          this.showCreate = false
          this.getOrders()   // 创建成功后刷新列表
        }
      })
    }
  }
}
```

---

### src/view/order/order.vue — 轨迹查询页

调用 `POST /findOrderList` 展示快递轨迹，支持按订单号过滤。组件逻辑抽离到 `order.js`，`order.vue` 只保留模板：

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

    /**
     * 接口3：POST /createOrder
     * 创建订单并同步创建快递单（Dubbo RPC 同步编排）
     *
     * 两个 Dubbo RPC 调用串行执行，由 coffee-app 统一编排：
     *   ① userOrderInfoService.createOrder()   → order 表写入订单
     *   ② expressTrackInfoService.createExpress() → express 表写快递单 + track 表写"商家已揽件"
     *
     * 调用后立即 GET /hello/{order_id} 即可查到轨迹记录。
     */
    @PostMapping("createOrder")
    public Result<String> createOrder(@RequestBody UserOrderCreateDTO userOrderCreateDTO) {
        // 第一步：Dubbo RPC 调用 coffee-userorder，将订单写入 order 表
        String orderId = userOrderInfoService.createOrder(userOrderCreateDTO);

        // 第二步：Dubbo RPC 调用 coffee-expresstrack，同步创建快递单和初始轨迹
        // createExpress 内部完成两步写库：INSERT express + INSERT track（"商家已揽件"）
        expressTrackInfoService.createExpress(orderId);

        return new ResultUtil<String>().setData(orderId);
    }
}
```

**三个接口的对比：**

| | `/hello/{orderid}` | `/findOrderList` | `/createOrder` |
|---|---|---|---|
| HTTP 方法 | GET | POST | POST |
| 参数位置 | URL 路径（`@PathVariable`） | 请求体 JSON（`@RequestBody`） | 请求体 JSON（`@RequestBody`） |
| 返回格式 | 直接返回 `PageDTO` | 包装在 `Result<PageDTO>` 中 | 包装在 `Result<String>` 中 |
| 使用场景 | 按单号查轨迹 | 管理后台列表查询 | 创建订单 + 快递单 |
| RPC 调用数 | 2（userorder + expresstrack） | 2（userorder + expresstrack） | 2（userorder + expresstrack） |

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
│       │   └── UserOrderCreateDTO.java       创建订单请求参数
│       └── service/
│           └── UserOrderInfoService.java     服务接口
└── provider/
    └── src/main/
        ├── java/com/coffee/yun/userorder/
        │   ├── UserOrderApplication.java         启动类
        │   └── provider/service/
        │       └── UserOrderInfoServiceImpl.java  接口实现
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

携带创建订单所需的字段：

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
 */
public interface UserOrderInfoService {
    UserOrderInfoResultDTO findUserOrderInfo(UserOrderInfoParamDTO param);
    PageDTO<UserOrderInfoResultDTO> findUserOrderInfos(UserOrderInfoParamDTO param);
    String createOrder(UserOrderCreateDTO createDTO);
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
@Slf4j
@DubboService
public class UserOrderInfoServiceImpl implements UserOrderInfoService {

    @Autowired private SqlSessionTemplate sqlSessionTemplate;
    @Autowired private PageUtil pageUtil;

    @Override
    public UserOrderInfoResultDTO findUserOrderInfo(UserOrderInfoParamDTO dto) {
        log.info("订单查询：{}", JSON.toJSONString(dto));
        return sqlSessionTemplate.selectOne("UserOrderMapper.selectByParam", dto);
    }

    @Override
    public PageDTO<UserOrderInfoResultDTO> findUserOrderInfos(UserOrderInfoParamDTO dto) {
        return pageUtil.selectPage("UserOrderMapper.selectByParam", dto);
    }

    @Override
    public String createOrder(UserOrderCreateDTO createDTO) {
        Map<String, Object> params = new HashMap<>();
        params.put("order_id",     createDTO.getOrder_id());
        params.put("OneID",        createDTO.getOneID());
        params.put("order_amount", createDTO.getOrder_amount());
        params.put("order_status", "待发货");
        sqlSessionTemplate.insert("UserOrderMapper.insertOrder", params);
        return createDTO.getOrder_id();
    }
}
```

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
 * 快递轨迹服务接口——定义快递服务对外暴露的所有能力。
 */
public interface ExpressTrackInfoService {

    /**
     * 根据订单编号分页查询快递轨迹列表
     * 一个订单通常有多条轨迹（揽收 → 转运 → 派送 → 签收），所以返回分页列表。
     */
    PageDTO<ExpressTrackInfoResultDTO> findExpressTrackInfos(
        ExpressTrackInfoParamDTO expressTrackInfoParamDTO);

    /**
     * 为新订单同步创建快递单和初始轨迹记录
     * 由 coffee-app 在 /createOrder 接口中，订单落库后通过 Dubbo RPC 调用。
     * 执行两步写库：INSERT express + INSERT track（"商家已揽件"）。
     *
     * @param orderId 刚创建的订单编号
     */
    void createExpress(String orderId);
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

#### ExpresstrackInfoServiceImpl.java — 接口实现

实现两个方法：一个查询轨迹，一个同步创建快递单（由 coffee-app 在下单后调用）：

```java
@Slf4j
@DubboService
public class ExpresstrackInfoServiceImpl implements ExpressTrackInfoService {

    @Autowired private PageUtil pageUtil;
    @Autowired private SqlSessionTemplate sqlSessionTemplate;  // 执行 INSERT 语句

    /** 分页查询快递轨迹列表 */
    @Override
    public PageDTO<ExpressTrackInfoResultDTO> findExpressTrackInfos(
            ExpressTrackInfoParamDTO dto) {
        return pageUtil.selectPage("ExpressTrackMapper.selectByParam", dto);
    }

    /**
     * 同步创建快递单 + 初始轨迹（被 coffee-app.createOrder 通过 Dubbo RPC 调用）
     *
     * 两步写库在同一次 RPC 调用中完成：
     *   ① INSERT INTO express（express_id 在此方法内用 UUID 生成）
     *   ② INSERT INTO track（"商家已揽件"初始轨迹）
     */
    @Override
    public void createExpress(String orderId) {
        // ① 生成快递单号，写 express 表
        String expressId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Map<String, Object> expressParams = new HashMap<>();
        expressParams.put("express_id",     expressId);
        expressParams.put("order_id",       orderId);
        expressParams.put("express_weight", "1kg");
        sqlSessionTemplate.insert("ExpressTrackMapper.insertExpress", expressParams);
        log.info("快递单已创建，order_id={}，express_id={}", orderId, expressId);

        // ② 写 track 表，插入第一条轨迹记录
        String trackId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Map<String, Object> trackParams = new HashMap<>();
        trackParams.put("track_id",   trackId);
        trackParams.put("express_id", expressId);
        trackParams.put("track_show", "商家已揽件");
        sqlSessionTemplate.insert("ExpressTrackMapper.insertTrack", trackParams);
        log.info("初始轨迹已创建，express_id={}，状态=商家已揽件", expressId);
    }
}
```

**createOrder 的完整调用链：**

```
POST /createOrder（前端或 Postman）
    ↓ HTTP
  CoffeeController.createOrder()   ← coffee-app（网关，端口 8005）
    ↓ Dubbo RPC（第一次）
  UserOrderInfoServiceImpl.createOrder()  ← coffee-userorder（端口 7001）
      → INSERT INTO order（写订单）
      → 返回 orderId
    ↓ Dubbo RPC（第二次，用第一次的 orderId）
  ExpresstrackInfoServiceImpl.createExpress(orderId)  ← coffee-expresstrack（端口 8001）
      → INSERT INTO express（写快递单，express_id 自动生成）
      → INSERT INTO track（写初始轨迹"商家已揽件"）
    ↓ 两个 RPC 都成功
  返回 {"success":true,"result":"ORDER100"}
```

**关键点：** 两次 RPC 调用都由 `coffee-app` 发起，串行执行，整个过程对前端只有一次 HTTP 请求和响应，无需了解内部微服务边界。

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
