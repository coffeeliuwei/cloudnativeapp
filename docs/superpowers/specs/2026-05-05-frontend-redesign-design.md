# 前端视觉改造设计文档

**日期：** 2026-05-05  
**作者：** 刘伟  
**项目：** cloudnativeapp / app-admin  

---

## 目标

将现有基于 iview-admin 模板的管理后台改造为视觉上独立、具有咖啡品牌调性的新项目，品牌名改为 **CoffeeTrack**。不触碰业务逻辑、路由、状态管理。

---

## 设计系统

### 配色

| 角色 | 色值 | 用途 |
|------|------|------|
| 主深色 | `#7c4a2d` | 侧边栏背景、标题、按钮深色态 |
| 主亮色 | `#c8825a` | 主按钮、强调色、链接 |
| 背景色 | `#fdf6ee` | 全局页面背景、卡片次级背景 |
| 成功绿 | `#6a9e72` | 已送达状态标签 |
| 警告黄 | `#f59e0b` | 派送中状态标签 |
| 错误红 | `#e05c5c` | 失败/异常状态标签 |
| 文字主色 | `#7c4a2d` | 标题、重要文字 |
| 文字次色 | `#9d7a5e` | 说明文字、表头 |
| 文字弱色 | `#bda998` | 占位符、辅助信息 |
| 边框色 | `#e8d8c4` | 输入框、卡片边框 |

### 字体

沿用系统默认字体栈，不引入新字体依赖。品牌名 "CoffeeTrack" 使用 `letter-spacing: 0.5px` + `font-weight: 700`。

---

## 页面改造方案

### 1. 登录页（`view/login/login.vue` + `login.less`）

**风格：** 全屏咖啡渐变背景 + 浮动白色卡片

- 背景：`linear-gradient(135deg, #3d1c08, #7c4a2d, #c8825a)`
- 装饰：低透明度 ☕ 图标 + 点阵纹理叠加（`radial-gradient` 实现）
- 卡片：`background: rgba(255,255,255,0.97)`，`border-radius: 16px`，`box-shadow: 0 20px 60px rgba(0,0,0,0.35)`
- 卡片内：Logo 区（渐变圆角方块 + ☕ emoji + COFFEETRACK 文字）、用户名输入、密码输入、登录按钮（渐变）
- 移除登录页的 "输入任意用户名和密码即可" 提示文字

### 2. 侧边栏（`components/main/components/side-menu/`）

**风格：** 图标竖向侧边栏，深棕色背景

- 侧边栏宽度：64px（折叠态），展开态保持原有逻辑
- 背景色：`#7c4a2d`
- 顶部 Logo 区：☕ emoji + 品牌名（折叠态只显示 emoji）
- 菜单激活项：`background: rgba(255,255,255,0.18)`，`border-radius: 8px`
- 菜单未激活项：文字/图标透明度 60%
- 底部用户头像区：圆形头像，边框 `rgba(255,255,255,0.3)`

### 3. 顶部 Header（`components/main/components/header-bar/`）

- 背景色：`#fff`
- 底部边框：`1px solid #e8d8c4`
- 面包屑文字颜色：`#7c4a2d` / `#bda998`

### 4. 订单列表页（`view/order/order.vue` + `order.less`）

- 页面背景：`#fdf6ee`
- 搜索栏：圆角输入框（`border-radius: 20px`），边框 `#e8d8c4`，聚焦态边框 `#c8825a`
- 搜索按钮：`background: #c8825a`，`border-radius: 20px`
- 表格：
  - 表头背景：`#f5ede3`，文字颜色：`#9d7a5e`
  - 行奇偶色：白色 / `#fdf6ee`
  - 行悬停：`background: #f5ede3`
  - 状态标签：圆角胶囊样式，各状态独立配色
- 分页组件：激活页码色 `#c8825a`

### 5. 首页 Dashboard（`view/single-page/home/home.vue`）

- 页面背景：`#fdf6ee`
- KPI 卡片：白色背景，顶部 2px 色条区分类型，`border-radius: 10px`，`box-shadow: 0 2px 8px rgba(0,0,0,0.06)`
- 区块标题：`color: #7c4a2d`，`font-weight: 700`

### 6. 全局样式（`index.less` / `App.vue`）

- `body` 背景改为 `#fdf6ee`
- iView 组件主题变量覆盖（通过 less 变量或 scoped 样式）：
  - `@primary-color: #c8825a`
  - `@link-color: #c8825a`
  - `@border-radius-base: 8px`
- 项目标题（`<title>` + config）：改为 `CoffeeTrack`

---

## 不改动的部分

- `src/api/` — 所有 API 调用逻辑
- `src/store/` — Vuex 状态管理
- `src/router/` — 路由配置
- `src/libs/` — 工具函数
- `src/mock/` — Mock 数据
- 所有业务组件的功能逻辑

---

## 文件改动清单

| 文件 | 改动类型 |
|------|---------|
| `src/view/login/login.vue` | 重写模板 + 样式 |
| `src/view/login/login.less` | 重写 |
| `src/view/order/order.vue` | 样式调整 |
| `src/view/order/order.less` | 重写 |
| `src/view/single-page/home/home.vue` | 样式调整 |
| `src/components/main/components/side-menu/side-menu.vue` | 样式调整 |
| `src/components/main/components/header-bar/header-bar.vue` | 样式调整 |
| `src/index.less` | 全局变量覆盖 |
| `src/config/index.js` | 修改 title 为 CoffeeTrack |
| `public/index.html` | 修改 `<title>` |

---

## 验收标准

1. 登录页显示全屏咖啡渐变背景，白色卡片居中浮动
2. 登录后侧边栏为深棕色，Logo 区显示 ☕ 图标
3. 订单页搜索栏、表格、分页均为暖色调风格
4. 页面 `<title>` 显示 CoffeeTrack
5. 所有原有功能（登录、订单搜索、分页）正常工作
6. 无新增 JS/CSS 依赖
