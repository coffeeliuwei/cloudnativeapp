# 数据库设计与初始化

> 本文档完整描述项目使用的数据库表结构、字段含义、关联关系，并提供可直接执行的初始化 SQL。

---

## 目录

1. [数据库规划](#1-数据库规划)
2. [userordertest 库（订单库）](#2-userordertest-库订单库)
3. [expresstracktest 库（快递库）](#3-expresstracktest-库快递库)
4. [完整初始化 SQL](#4-完整初始化-sql)
5. [关键设计决策解析](#5-关键设计决策解析)
6. [数据库 ER 图](#6-数据库-er-图)

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

## 5. 关键设计决策解析

### 5.1 为什么用 `utf8mb4` 而不是 `utf8`？

MySQL 中的 `utf8` **不是真正的 UTF-8**——这是一个历史遗留 bug。

标准 UTF-8 编码最多使用 4 个字节表示一个字符。但 MySQL 的 `utf8` 实际上是"utf8mb3"，最多只支持 3 个字节。超出 3 字节的字符（如 emoji 😊、部分中文生僻字、某些数学符号）插入时会**直接报错或被截断**。

| 字符集 | 最大字节/字符 | 能存 emoji？ | 能存所有中文？|
|--------|------------|------------|------------|
| `utf8`（MySQL）| 3 字节 | ❌ 报错 | 大部分能，偏僻字不行 |
| `utf8mb4` | 4 字节 | ✅ | ✅ 完整支持 |

**本项目所有表都使用 `utf8mb4`。** 新项目的数据库和表默认应始终选择 `utf8mb4`，没有理由再用 `utf8`。

排序规则（`COLLATE`）使用 `utf8mb4_unicode_ci`：
- `unicode`：按 Unicode 标准排序，对多语言友好
- `ci`：case-insensitive，查询时大小写不敏感（`WHERE name = 'zhang'` 能匹配 `Zhang`）

---

### 5.2 为什么 `order_amount` 用 `DECIMAL` 而不是 `FLOAT` 或 `DOUBLE`？

**金额类字段绝对不能用浮点类型（FLOAT / DOUBLE）**，这是数据库设计的铁律。

原因是浮点数在计算机中用二进制表示，无法精确存储大多数十进制小数：

```sql
-- 浮点数的精度问题演示
SELECT 0.1 + 0.2;   -- 结果：0.30000000000000004（不是 0.3！）

-- DECIMAL 精确计算
SELECT CAST(0.1 AS DECIMAL(10,2)) + CAST(0.2 AS DECIMAL(10,2));  -- 结果：0.30
```

`DECIMAL(10, 2)` 的含义：
- `10`：总共最多 10 位数字
- `2`：小数点后保留 2 位

所以 `DECIMAL(10, 2)` 能存储的最大金额是 `99,999,999.99`（约一亿元），满足绝大多数业务场景。

> **浮点数误差的实际危害**：电商系统中，每笔订单金额精度丢失 0.01 元，百万订单后会产生万元级别的对账差异，财务报表无法核对。

---

### 5.3 索引设计说明

表中的索引分为两类：

#### 唯一索引（UNIQUE KEY）

```sql
UNIQUE KEY uk_OneID (OneID)          -- member 表：用户唯一标识不可重复
UNIQUE KEY uk_order_id (order_id)    -- order 表：订单号全局唯一
UNIQUE KEY uk_express_id (express_id) -- express 表：快递单号唯一
```

唯一索引同时承担两个职责：
1. **业务约束**：数据库层面保证字段值不重复，即使代码有 bug 也不会写入脏数据
2. **查询加速**：按这些字段查询时，数据库走索引而非全表扫描

#### 普通索引（KEY）

```sql
KEY idx_OneID (OneID)                -- order 表：按用户查所有订单
KEY idx_order_id (order_id)          -- express 表：按订单查快递
KEY idx_express_id (express_id)      -- track 表：按快递单查轨迹（最高频的查询）
```

普通索引的核心价值是**减少查询扫描的行数**：

```
没有索引的查询：
SELECT * FROM track WHERE express_id = 'EX20240001'
→ 扫描 track 表所有行，找出 express_id 匹配的
→ 100万条数据就要比较100万次

有索引的查询：
→ 通过 B+ 树索引直接定位 express_id = 'EX20240001' 的位置
→ 只读取匹配的几行，时间复杂度从 O(n) 降至 O(log n)
```

**哪些字段不需要建索引？**

`track_show`（轨迹描述文字）不建索引——它是展示字段，业务上不会"按轨迹内容查询"，建索引浪费存储空间且降低写入性能。

**索引对写操作的代价：** 每次 INSERT/UPDATE 时，数据库需要维护索引结构，写操作比无索引时稍慢。这是用写性能换读性能的取舍，对读多写少的业务（如订单查询）完全值得。

---

### 5.4 跨库无外键：数据一致性怎么保障？

#### 为什么不设置外键？

两个数据库之间（`userordertest` 和 `expresstracktest`）无法设置跨库外键——这是 MySQL 的硬性限制，外键只能在**同一个数据库**内的表之间建立。

即使在同一数据库内，微服务架构中也**刻意不使用外键**，原因如下：

| 原因 | 说明 |
|------|------|
| 性能 | 每次写操作都会触发外键约束检查，高并发时成为瓶颈 |
| 独立部署 | 服务 A 部署新版本时，不能因为外键而影响服务 B |
| 灵活迁移 | 某个服务换数据库（如从 MySQL 换 MongoDB），不会因外键牵连其他服务 |

#### 没有外键，一致性怎么保障？

**应用层约束**——在 Java 代码中保证数据完整性：

```
业务规则：快递记录必须关联到存在的订单

外键方式（数据库层）：
  INSERT INTO express (order_id = 'ORDER999') → 数据库自动报错"订单不存在"

应用层方式（Java 代码）：
  1. 先查询订单服务：orderExists = userOrderService.findByOrderId('ORDER999')
  2. if (orderExists == null) throw new BusinessException("订单不存在")
  3. 再插入快递记录
```

**这种方式的代价：** 如果代码有 bug 跳过了校验，脏数据会直接进库，不像外键那样被数据库强制拦截。因此需要更严格的代码审查和集成测试。

---

### 5.5 `order` 是 MySQL 保留字——详细说明

`ORDER BY` 是 SQL 中的排序关键字，因此 `order` 被 MySQL 保留。直接使用会导致解析错误：

```sql
-- 这样写会报语法错误
SELECT * FROM order WHERE order_id = 'ORDER001';
-- ERROR: You have an error in your SQL syntax near 'order'

-- 必须加反引号
SELECT * FROM `order` WHERE order_id = 'ORDER001';  -- ✅ 正确
```

**本项目所有涉及 `order` 表的 SQL 都加了反引号**，包括：

```xml
<!-- UserOrderMapper.xml -->
FROM member m, `order` o

<!-- 建表语句 -->
CREATE TABLE `order` ( ... )
```

**避免踩这个坑的好习惯：** 取表名和字段名时避开 SQL 保留字。常见的保留字陷阱：`order`、`group`、`key`、`index`、`table`、`select`、`from`、`where`。如果必须用，始终加反引号。

---

## 6. 数据库 ER 图

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
