# 数据库设计与初始化

> 本文档完整描述项目使用的数据库表结构、字段含义、关联关系，并提供可直接执行的初始化 SQL。

---

## 目录

1. [数据库规划](#1-数据库规划)
2. [userordertest 库（订单库）](#2-userordertest-库订单库)
3. [expresstracktest 库（快递库）](#3-expresstracktest-库快递库)
4. [完整初始化 SQL](#4-完整初始化-sql)
5. [数据库 ER 图](#5-数据库-er-图)

---

## 1. 数据库规划

本项目遵循微服务**数据库隔离**原则：每个微服务有专属的数据库，服务间不直接访问对方的数据库。

| 数据库名 | 所属服务 | 说明 |
|---------|---------|------|
| `userordertest` | coffee-userorder | 存储用户信息和订单信息 |
| `expresstracktest` | coffee-expresstrack | 存储快递信息和轨迹信息 |

两个数据库通过 **订单ID（order_id）** 进行业务关联，但这种关联只在应用层（Java 代码）体现，数据库层不设置外键约束。

---

## 2. userordertest 库（订单库）

### 2.1 member（用户表）

| 字段名 | 类型 | 是否必填 | 说明 |
|--------|------|---------|------|
| id | INT | 是（主键）| 自增主键 |
| OneID | VARCHAR(50) | 是 | 用户唯一标识（跨系统唯一ID，可理解为"会员号"）|
| member_name | VARCHAR(100) | 否 | 用户姓名 |
| member_phone | VARCHAR(20) | 否 | 手机号 |

**建表语句：**

```sql
CREATE TABLE member (
    id           INT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    OneID        VARCHAR(50)  NOT NULL                COMMENT '用户唯一标识',
    member_name  VARCHAR(100)                         COMMENT '用户姓名',
    member_phone VARCHAR(20)                          COMMENT '手机号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_OneID (OneID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';
```

### 2.2 order（订单表）

> 注意：`order` 是 MySQL 保留关键字，在 SQL 中使用时需要加反引号：`` `order` ``

| 字段名 | 类型 | 是否必填 | 说明 |
|--------|------|---------|------|
| id | INT | 是（主键）| 自增主键 |
| OneID | VARCHAR(50) | 是 | 关联 member 表的 OneID |
| order_id | VARCHAR(50) | 是 | 订单编号（业务主键）|
| order_amount | DECIMAL(10,2) | 否 | 订单金额 |
| order_status | VARCHAR(20) | 否 | 订单状态 |

**订单状态枚举值（建议使用以下值）：**

| 值 | 含义 |
|----|------|
| `待付款` | 用户已下单但未付款 |
| `已付款` | 用户已付款，等待发货 |
| `已发货` | 商家已发货，快递在途 |
| `已完成` | 用户已签收 |
| `已取消` | 订单已取消 |

**建表语句：**

```sql
CREATE TABLE `order` (
    id           INT            NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    OneID        VARCHAR(50)    NOT NULL                COMMENT '用户唯一标识',
    order_id     VARCHAR(50)    NOT NULL                COMMENT '订单编号',
    order_amount DECIMAL(10,2)                          COMMENT '订单金额',
    order_status VARCHAR(20)                            COMMENT '订单状态',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_id (order_id),
    KEY idx_OneID (OneID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息表';
```

---

## 3. expresstracktest 库（快递库）

### 3.1 express（快递表）

| 字段名 | 类型 | 是否必填 | 说明 |
|--------|------|---------|------|
| id | INT | 是（主键）| 自增主键 |
| order_id | VARCHAR(50) | 是 | 关联订单的 order_id（通过应用层关联）|
| express_id | VARCHAR(50) | 是 | 快递单号 |
| express_weight | VARCHAR(20) | 否 | 包裹重量（如 "1.5kg"）|

**建表语句：**

```sql
CREATE TABLE express (
    id             INT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id       VARCHAR(50) NOT NULL                COMMENT '关联订单ID',
    express_id     VARCHAR(50) NOT NULL                COMMENT '快递单号',
    express_weight VARCHAR(20)                         COMMENT '包裹重量',
    PRIMARY KEY (id),
    UNIQUE KEY uk_express_id (express_id),
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='快递信息表';
```

### 3.2 track（轨迹表）

| 字段名 | 类型 | 是否必填 | 说明 |
|--------|------|---------|------|
| id | INT | 是（主键）| 自增主键 |
| express_id | VARCHAR(50) | 是 | 关联 express 表的 express_id |
| track_id | VARCHAR(50) | 否 | 轨迹节点唯一标识 |
| track_show | VARCHAR(500) | 否 | 轨迹描述文字（显示给用户的内容）|

**建表语句：**

```sql
CREATE TABLE track (
    id         INT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    express_id VARCHAR(50)  NOT NULL                COMMENT '快递单号',
    track_id   VARCHAR(50)                          COMMENT '轨迹节点ID',
    track_show VARCHAR(500)                         COMMENT '轨迹描述',
    PRIMARY KEY (id),
    KEY idx_express_id (express_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='快递轨迹表';
```

---

## 4. 完整初始化 SQL

将以下 SQL 在 MySQL 客户端（或阿里云 RDS 的"DMS 数据管理"工具）中执行：

```sql
-- ===================================
-- 初始化 userordertest 数据库
-- ===================================
CREATE DATABASE IF NOT EXISTS userordertest
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE userordertest;

-- 创建用户表
CREATE TABLE IF NOT EXISTS member (
    id           INT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    OneID        VARCHAR(50)  NOT NULL                COMMENT '用户唯一标识',
    member_name  VARCHAR(100)                         COMMENT '用户姓名',
    member_phone VARCHAR(20)                          COMMENT '手机号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_OneID (OneID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 创建订单表
CREATE TABLE IF NOT EXISTS `order` (
    id           INT            NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    OneID        VARCHAR(50)    NOT NULL                COMMENT '用户唯一标识',
    order_id     VARCHAR(50)    NOT NULL                COMMENT '订单编号',
    order_amount DECIMAL(10,2)                          COMMENT '订单金额',
    order_status VARCHAR(20)                            COMMENT '订单状态',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_id (order_id),
    KEY idx_OneID (OneID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息表';

-- 插入测试数据
INSERT INTO member (OneID, member_name, member_phone) VALUES
    ('U001', '张三', '13800138001'),
    ('U002', '李四', '13900139002'),
    ('U003', '王五', '13700137003');

INSERT INTO `order` (OneID, order_id, order_amount, order_status) VALUES
    ('U001', 'ORDER001', 299.00, '已发货'),
    ('U001', 'ORDER002', 89.50,  '已完成'),
    ('U002', 'ORDER003', 1580.00,'已付款'),
    ('U003', 'ORDER004', 45.00,  '待付款');


-- ===================================
-- 初始化 expresstracktest 数据库
-- ===================================
CREATE DATABASE IF NOT EXISTS expresstracktest
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE expresstracktest;

-- 创建快递表
CREATE TABLE IF NOT EXISTS express (
    id             INT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id       VARCHAR(50) NOT NULL                COMMENT '关联订单ID',
    express_id     VARCHAR(50) NOT NULL                COMMENT '快递单号',
    express_weight VARCHAR(20)                         COMMENT '包裹重量',
    PRIMARY KEY (id),
    UNIQUE KEY uk_express_id (express_id),
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='快递信息表';

-- 创建轨迹表
CREATE TABLE IF NOT EXISTS track (
    id         INT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    express_id VARCHAR(50)  NOT NULL                COMMENT '快递单号',
    track_id   VARCHAR(50)                          COMMENT '轨迹节点ID',
    track_show VARCHAR(500)                         COMMENT '轨迹描述',
    PRIMARY KEY (id),
    KEY idx_express_id (express_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='快递轨迹表';

-- 插入测试数据（对应 ORDER001、ORDER002 已发货的订单）
INSERT INTO express (order_id, express_id, express_weight) VALUES
    ('ORDER001', 'EX20240001', '1.5kg'),
    ('ORDER002', 'EX20240002', '0.3kg');

INSERT INTO track (express_id, track_id, track_show) VALUES
    -- ORDER001 的轨迹
    ('EX20240001', 'T001', '2024-01-01 10:00 【北京朝阳区】 快件已揽收'),
    ('EX20240001', 'T002', '2024-01-01 20:00 【北京转运中心】 快件已到达分拣中心，正在分拣'),
    ('EX20240001', 'T003', '2024-01-02 08:00 【发往上海】 快件已离开北京转运中心'),
    ('EX20240001', 'T004', '2024-01-03 06:00 【上海转运中心】 快件已到达上海分拣中心'),
    ('EX20240001', 'T005', '2024-01-03 14:00 【上海浦东区】 快件已送达，签收人：张三本人'),
    -- ORDER002 的轨迹
    ('EX20240002', 'T006', '2024-01-05 09:00 【上海静安区】 快件已揽收'),
    ('EX20240002', 'T007', '2024-01-05 22:00 【上海转运中心】 快件正在分拣'),
    ('EX20240002', 'T008', '2024-01-06 15:00 【上海浦东区】 快件已签收');
```

---

## 5. 数据库 ER 图

```
userordertest 数据库
┌──────────────────────────┐      ┌──────────────────────────────┐
│         member           │      │           order              │
├──────────────────────────┤      ├──────────────────────────────┤
│ id (PK, AUTO_INCREMENT)  │      │ id (PK, AUTO_INCREMENT)      │
│ OneID (UNIQUE)  ◄────────┼──────┼─ OneID                       │
│ member_name              │      │ order_id (UNIQUE)            │
│ member_phone             │      │ order_amount                 │
└──────────────────────────┘      │ order_status                 │
                                  └──────────────┬───────────────┘
                                                 │ order_id（应用层关联）
                                                 │
expresstracktest 数据库           ▼
                                  ┌──────────────────────────────┐
                                  │          express             │
                                  ├──────────────────────────────┤
                                  │ id (PK, AUTO_INCREMENT)      │
                                  │ order_id  ◄──────────────────┘
                                  │ express_id (UNIQUE)  ◄───────┐
                                  │ express_weight               │
                                  └──────────────────────────────┘
                                                                  │
                                  ┌──────────────────────────────┤
                                  │           track              │
                                  ├──────────────────────────────┤
                                  │ id (PK, AUTO_INCREMENT)      │
                                  │ express_id ──────────────────┘
                                  │ track_id                     │
                                  │ track_show                   │
                                  └──────────────────────────────┘
```

**关联说明：**

1. `member.OneID` → `order.OneID`：一个用户可以有多个订单（一对多）
2. `order.order_id` → `express.order_id`：一个订单对应一条快递（应用层关联，不同数据库）
3. `express.express_id` → `track.express_id`：一个快递单有多条轨迹（一对多）

---

[← 返回主文档](../README.md)
