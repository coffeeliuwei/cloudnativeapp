package com.coffee.yun.userorder.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 创建订单请求参数 DTO（Data Transfer Object，数据传输对象）
 *
 * ── 什么是 DTO？ ────────────────────────────────────────────────────────
 * DTO 是专门用于在层与层之间（如 Controller → Service、RPC 调用方 → 提供方）
 * 传递数据的对象。它只包含数据字段，不包含业务逻辑。
 * 使用 DTO 的好处：
 *   1. 隔离内部实体（Entity）结构不对外暴露，提高安全性
 *   2. 可以只包含某次调用需要的字段，不传输冗余数据
 *
 * ── 为什么必须实现 Serializable？ ──────────────────────────────────────
 * 此 DTO 定义在 coffee-userorder-api 模块中，会被两个地方引用：
 *   1. coffee-app（消费方）：发起 Dubbo RPC 调用，把 DTO 对象作为参数传给提供方
 *   2. coffee-userorder-provider（提供方）：接收参数并处理
 *
 * Dubbo 在 RPC 调用时需要将 Java 对象序列化后通过网络传输，
 * Java 要求凡是能被序列化（转换为字节流）的类都必须实现 Serializable 接口。
 * 如果不实现，运行时会抛出 NotSerializableException。
 *
 * ── 文件放在 api 模块的原因 ──────────────────────────────────────────────
 * coffee-userorder 工程分为两个 Maven 子模块：
 *   - api 模块：存放接口定义（Service 接口）和传输对象（DTO），会被消费方依赖
 *   - provider 模块：存放接口实现，只在服务端部署
 *
 * coffee-app 的 pom.xml 中依赖 coffee-userorder-api（而非 provider），
 * 所以 DTO 必须放在 api 模块中，消费方才能引用同一个类定义。
 *
 * @Getter  Lombok 自动生成所有字段的 getter 方法
 * @Setter  Lombok 自动生成所有字段的 setter 方法
 */
@Getter
@Setter
public class UserOrderCreateDTO implements Serializable {

    /**
     * 序列化版本号（serialVersionUID）
     *
     * Java 序列化机制使用此版本号来验证反序列化时类的版本是否匹配。
     * 规则：
     *   - 若不声明，JVM 会根据类结构自动生成，一旦类增删字段，版本号就变
     *   - 旧版本序列化的数据就无法被新版本反序列化，导致 InvalidClassException
     *
     * 显式声明 serialVersionUID = 1L 并手动管理版本，
     * 可以让我们在升级字段时（如新增可选字段）保持向后兼容。
     */
    private static final long serialVersionUID = 1L;

    /** 订单编号，全局唯一，由调用方（coffee-app）生成并传入 */
    private String order_id;

    /** 会员唯一标识（One ID），关联 member 表，标识下单会员 */
    private String OneID;

    /** 订单金额，使用 float 存储，单位：元 */
    private float order_amount;
}
