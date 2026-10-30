package com.example.aichat.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计注解 - 标注需要记录访问日志的方法。
 *
 * Phase 2 扩展架构目的：
 *   原 UserController 等业务方法里手写 logger.info("[LOGIN_SUCCESS]...")，
 *   代码冗余且日志格式不统一、缺乏结构化字段（耗时/状态/IP）。
 *   通过 @Audited + AuditAspect 统一切面收集：
 *   - 操作类型（action，如 LOGIN/REGISTER/UPDATE_PROFILE）
 *   - 资源类型（resourceType，如 USER）
 *   - 执行耗时（responseTime）
 *   - 调用结果（success/exception）
 *   - 调用方信息（方法名、参数摘要）
 *
 *   使用方式：
 *     @Audited(action = "LOGIN", resourceType = "USER")
 *     public R<User> login(@Valid @RequestBody LoginRequest req) { ... }
 *
 *   与 AccessLogService 集成，日志以异步方式写入 access_log 表，
 *   失败时降级为 logger.warn 输出，不影响业务流程。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /** 操作类型，建议使用全大写下划线命名，如 LOGIN、REGISTER、UPDATE_PROFILE */
    String action();

    /** 资源类型，如 USER、SOCIAL_POST、AI_CHARACTER */
    String resourceType() default "UNKNOWN";

    /** 是否记录方法参数摘要（脱敏后），默认开启 */
    boolean logArgs() default true;

    /** 是否记录方法返回值摘要，默认关闭（避免泄露敏感数据） */
    boolean logResult() default false;
}
