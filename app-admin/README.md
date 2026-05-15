# app-admin — CoffeeTrack 前端管理界面

基于 **Vue.js 3 + ViewUI Plus** 构建的前端，是 `coffee-app`（API 网关，端口 8005）的管理界面。

---

## 启动

```bash
npm install
npm run dev
# 访问 http://localhost:8080
```

## 登录

本地校验登录，**输入任意非空用户名和密码**即可进入，无需后端 auth 接口。

---

## 功能模块

| 菜单 | 路由 | 后端接口 | 说明 |
|------|------|---------|------|
| 订单管理 | `/order-manage` | `POST /findOrders`、`POST /createOrder` | 分页查询订单列表，支持新建订单 |
| 轨迹查询 | `/order` | `POST /findOrderList` | 按订单号查询快递轨迹 |

---

## 关键文件

```
src/
├── api/index.js              业务接口（findOrders / findOrderList / createOrder）
├── config/index.js           baseUrl 配置（默认 http://localhost:8005）
├── store/module/user.js      登录/用户信息（本地 mock，不调后端 auth 接口）
├── router/routers.js         路由与左侧菜单定义
└── view/
    ├── order-manage/         订单管理页
    └── order/                轨迹查询页
```

## 环境变量

| 变量 | 默认值 | 说明 |
|------|-------|------|
| `VUE_APP_BASE_URL` | `http://localhost:8005` | 后端 API 网关地址 |

云上部署时设置为 `coffee-app` 的公网/内网地址即可。

---

## 构建

```bash
npm run build
# 产物在 dist/ 目录，可直接部署到 Nginx / OSS
```
