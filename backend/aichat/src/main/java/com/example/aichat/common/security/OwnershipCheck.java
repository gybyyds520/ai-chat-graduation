package com.example.aichat.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * B2: IDOR 归属校验注解。
 *
 * 标注在 Controller 方法上，由 {@link OwnershipAspect} 切面在方法执行前自动校验资源归属。
 * 替代各 Controller 中重复的 verifyXxxOwnership(id) 私有方法。
 *
 * 用法示例：
 * <pre>
 * {@literal @}OwnershipCheck(resource = "elf", idParam = "elfId")
 * {@literal @}PutMapping("/{elfId}")
 * public R updateElf({@literal @}PathVariable Long elfId, ...) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OwnershipCheck {

    /**
     * 资源类型标识，对应 {@link OwnershipChecker#supports()} 的返回值。
     * 例如 "elf"、"alarm"、"schedule"。
     */
    String resource();

    /**
     * 方法参数中资源 ID 的参数名。切面通过方法参数名或 @RequestParam/@PathVariable 注解
     * 找到对应的值，传给 OwnershipChecker 做归属校验。
     */
    String idParam();
}
