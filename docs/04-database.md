# 数据库设计与初始化

> 本文档完整描述项目使用的数据库表结构、字段含义、关联关系，并说明如何执行初始化脚本。

---

## 目录

1. [数据库规划](#1-数据库规划)
2. [userordertest 库（订单库）](#2-userordertest-库订单库)
3. [expresstracktest 库（快递库）](#3-expresstracktest-库快递库)
4. [初始化步骤](#4-初始化步骤)
5. [关键设计说明](#5-关键设计说明)
6. [数据库 ER 图](#6-数据库-er-图)

---

## 1. 数据库规划

本项目遵循微服务**数据库隔离**原则：每个微服务有专属的数据库，服务间不直接访问对方的数据库。

| 数据库名 | 所属服务 | 初始化脚本 |
|---------|---------|----------|
| `userordertest` | coffee-userorder | `sql/order初始化.sql` |
| `expresstracktest` | coffee-expresstrack | `sql/express初始化.sql` |

两个数据库通过 **订单ID（order_id）** 进行业务关联，这种关联只在应用层（Java 代码）体现，数据库层不设置外键约束。

---

## 2. userordertest 库（订单库）

### 2.1 member（用户表）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `OneID` | varchar(32)，主键 | 用户唯一标识（会员号）|
| `member_name` | varchar(100) | 用户姓名 |
| `certificate_number` | varchar(100) | 证件号码 |
| `member_phone` | varchar(15) | 手机号 |
| `member_level` | char(1) | 会员等级（'1'=普通，'2'=银卡，...）|
| `gmt_create` | timestamp | 注册时间 |

**建表语句：**

```sql
CREATE TABLE `member` (
  `OneID`              varchar(32)  NOT NULL COMMENT '用户ID',
  `member_name`        varchar(100) DEFAULT NULL COMMENT '姓名',
  `certificate_number` varchar(100) DEFAULT NULL COMMENT '证件号码',
  `member_phone`       varchar(15)  DEFAULT NULL COMMENT '手机号',
  `member_level`       char(1)      DEFAULT NULL COMMENT '会员等级',
  `gmt_create`         timestamp    NULL DEFAULT NULL COMMENT '注册时间',
  PRIMARY KEY (`OneID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户表';
```

### 2.2 order（订单表）

> 注意：`order` 是 MySQL 保留关键字，在 SQL 中使用时需要加反引号：`` `order` ``

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `order_id` | varchar(32)，主键 | 订单编号 |
| `channel_id` | varchar(32) | 渠道编号 |
| `OneID` | varchar(32) | 关联用户 ID |
| `member_phone` | varchar(32) | 用户手机号（冗余存储，避免联表）|
| `product_id` | varchar(32) | 商品编号 |
| `order_type` | char(1) | 订单类型（'1'=普通，'2'=特殊）|
| `order_status` | char(1) | 订单状态（'1'=待付款，'2'=已付款，'3'=已发货）|
| `order_amount` | float | 订单金额 |

**建表语句：**

```sql
CREATE TABLE `order` (
  `order_id`     varchar(32) NOT NULL COMMENT '订单编号',
  `channel_id`   varchar(32) NOT NULL COMMENT '渠道编号',
  `OneID`        varchar(32) NOT NULL COMMENT '用户标识',
  `member_phone` varchar(32) NOT NULL COMMENT '手机号',
  `product_id`   varchar(32) NOT NULL COMMENT '商品编号',
  `order_type`   char(1)     NOT NULL COMMENT '订单类型',
  `order_status` char(1)     NOT NULL COMMENT '订单状态',
  `order_amount` float       NOT NULL COMMENT '支付金额',
  PRIMARY KEY (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='订单表';
```

**测试数据（3条）：**

| order_id | OneID | order_status | order_amount |
|----------|-------|-------------|-------------|
| 32423423 | 0001 | 3（已发货）| 3434.88 |
| 44556677 | 0002 | 2（已付款）| 57 |
| 76561256 | 0002 | 3（已发货）| 443.87 |

---

## 3. expresstracktest 库（快递库）

### 3.1 express（快递表）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `express_id` | varchar(32)，主键 | 快递单号 |
| `order_id` | varchar(32) | 关联订单 ID（应用层关联）|
| `express_weight` | float | 包裹重量（kg）|
| `express_status` | char(1) | 快递状态（'1'=在途，'2'=已签收）|
| `express_amount` | float | 快递费用 |
| `express_address` | varchar(200) | 收货地址 |

**建表语句：**

```sql
CREATE TABLE `express` (
  `express_id`     varchar(32)  NOT NULL COMMENT '快递标识',
  `order_id`       varchar(32)  NOT NULL COMMENT '订单编号',
  `express_weight` float        NOT NULL COMMENT '重量',
  `express_status` char(1)      NOT NULL COMMENT '状态',
  `express_amount` float        NOT NULL COMMENT '快递费用',
  `express_address` varchar(200) DEFAULT NULL COMMENT '收货地址',
  PRIMARY KEY (`express_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='快递';
```

### 3.2 track（轨迹表）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `track_id` | varchar(32)，主键 | 轨迹编号 |
| `track_status` | char(1) | 轨迹状态 |
| `track_time` | timestamp | 轨迹时间 |
| `track_show` | varchar(32) | 轨迹说明（展示给用户）|
| `express_id` | varchar(32) | 关联快递单号 |

**建表语句：**

```sql
CREATE TABLE `track` (
  `track_id`     varchar(32) NOT NULL COMMENT '轨迹编号',
  `track_status` char(1)     NOT NULL COMMENT '轨迹状态',
  `track_time`   timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP COMMENT '轨迹时间',
  `track_show`   varchar(32) NOT NULL COMMENT '轨迹说明',
  `express_id`   varchar(32) NOT NULL COMMENT '快递标识',
  PRIMARY KEY (`track_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

**测试数据（2条）：**

| track_id | track_show | express_id |
|----------|-----------|-----------|
| 11223344 | 已发出 | 33224455 |
| 22334455 | 到达 | 到达中转站石家庄 |

> **注意**：第2条轨迹的 `express_id` 字段值 `到达中转站石家庄` 是测试数据中的错误，正确值应为某个快递单号。这条数据查询时不会命中任何快递记录。

---

## 4. 初始化步骤

初始化脚本在项目根目录的 `sql/` 目录下。执行时需要先手动创建数据库，再运行脚本。

### 第一步：创建两个数据库

在 MySQL 客户端（Navicat、DBeaver 或命令行）中执行：

```sql
CREATE DATABASE IF NOT EXISTS userordertest   DEFAULT CHARACTER SET utf8;
CREATE DATABASE IF NOT EXISTS expresstracktest DEFAULT CHARACTER SET utf8;
```

### 第二步：执行初始化脚本

**订单库：**

1. 在 MySQL 客户端中切换到 `userordertest` 数据库
2. 打开并执行 `sql/order初始化.sql`

**快递库：**

1. 切换到 `expresstracktest` 数据库
2. 打开并执行 `sql/express初始化.sql`

> **提示：** 脚本开头有 `DROP TABLE IF EXISTS`，重复执行会清空数据重建，放心多次运行。

### 验证

```sql
USE userordertest;
SELECT * FROM `order`;   -- 应看到 3 条测试数据

USE expresstracktest;
SELECT * FROM track;     -- 应看到 2 条轨迹数据
```

---

## 5. 关键设计说明

### 5.1 为什么用 varchar 做主键而不是自增 INT？

本项目的 `order_id`、`express_id` 等主键使用 varchar(32) 字符串，而不是常见的 `INT AUTO_INCREMENT`。

**原因：** 分布式系统中，多个服务实例同时写入时，自增 INT 会产生 ID 冲突（两台机器各自从1开始自增）。字符串 ID（UUID 或业务编号）由上游系统生成后传入，天然不冲突。

**代价：** 字符串比整数占用更多存储，且 B+ 树索引在字符串比较时比整数慢。对本项目规模无影响，但百亿级数据的系统会选雪花算法（64位整型）代替 UUID。

### 5.2 member_phone 在 order 表中冗余存储

`order` 表同时保存了 `OneID`（用户ID）和 `member_phone`（手机号），而手机号在 `member` 表中已有。

**原因：** 查询订单时不用再联表查 `member` 就能拿到手机号（减少一次跨服务调用）。这是微服务中常见的**数据冗余换性能**的设计模式。

**代价：** 用户改手机号后需要同步更新 `order` 表的冗余字段，否则数据不一致。

### 5.3 状态字段用 char(1) 枚举

`order_status`、`express_status`、`track_status` 均用 `char(1)` 存储，如 `'1'`、`'2'`、`'3'`。

**原因：** 节省存储（1字节 vs varchar 的变长开销），且枚举值有限，char(1) 足够。

**代价：** 可读性差——看到 `'3'` 不知道是什么状态，需要查代码注释或常量定义。实际项目中通常用 `ENUM` 类型或独立的状态码表。

### 5.4 跨库无外键：数据一致性由应用层保障

`express.order_id` 引用 `userordertest.order.order_id`，但两个数据库之间无法设置外键（MySQL 硬性限制）。

一致性通过 Java 代码保障：创建快递记录前先调用订单服务确认订单存在，不存在则抛业务异常，不落库。

**这种方式的代价：** 代码有 bug 跳过校验时，脏数据会直接进库，不像外键那样被数据库强制拦截。因此需要更严格的代码审查和集成测试。

---

## 6. 数据库 ER 图

```
userordertest 数据库
┌─────────────────────────────┐      ┌─────────────────────────────────┐
│           member            │      │             order               │
├─────────────────────────────┤      ├─────────────────────────────────┤
│ OneID (PK)  ◄───────────────┼──────┼─ OneID                          │
│ member_name                 │      │ order_id (PK)                   │
│ certificate_number          │      │ channel_id                      │
│ member_phone                │      │ member_phone                    │
│ member_level                │      │ product_id                      │
│ gmt_create                  │      │ order_type / order_status       │
└─────────────────────────────┘      │ order_amount                    │
                                     └──────────────┬──────────────────┘
                                                    │ order_id（应用层关联）
expresstracktest 数据库                              │
                                                    ▼
                                     ┌─────────────────────────────────┐
                                     │            express              │
                                     ├─────────────────────────────────┤
                                     │ express_id (PK)  ◄──────────────┐
                                     │ order_id ◄──────────────────────┘
                                     │ express_weight / express_status │
                                     │ express_amount / express_address│
                                     └──────────────┬──────────────────┘
                                                    │ express_id
                                                    ▼
                                     ┌─────────────────────────────────┐
                                     │             track               │
                                     ├─────────────────────────────────┤
                                     │ track_id (PK)                   │
                                     │ express_id                      │
                                     │ track_status / track_time       │
                                     │ track_show                      │
                                     └─────────────────────────────────┘
```

**关联说明：**

1. `member.OneID` → `order.OneID`：一个用户可以有多个订单（一对多）
2. `order.order_id` → `express.order_id`：一个订单对应一条快递（应用层关联，跨数据库）
3. `express.express_id` → `track.express_id`：一个快递单有多条轨迹（一对多）

---

[← 返回主文档](../README.md)
