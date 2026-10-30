package com.example.aichat.common.security;

/**
 * B2: 资源归属校验器接口。
 *
 * 每种资源类型（elf、alarm、schedule 等）实现本接口并注册为 Spring Bean，
 * 由 {@link OwnershipAspect} 根据 {@link OwnershipCheck#resource()} 路由到对应实现。
 */
public interface OwnershipChecker {

    /**
     * 本校验器支持的资源类型标识（对应 @OwnershipCheck.resource）。
     */
    String supports();

    /**
     * 校验指定资源 ID 是否属于当前登录用户。
     *
     * @param resourceId  资源 ID（从方法参数中提取）
     * @param currentUserId 当前登录用户 ID（已由 AuthContext.requireUserId() 保证非空）
     * @throws com.example.aichat.common.exception.BusinessException 资源不存在或归属他人时抛出
     */
    void checkOwnership(Object resourceId, Long currentUserId);
}
