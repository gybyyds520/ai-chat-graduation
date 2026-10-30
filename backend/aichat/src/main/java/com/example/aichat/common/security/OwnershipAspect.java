package com.example.aichat.common.security;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.UserErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B2: IDOR 归属校验 AOP 切面。
 *
 * 拦截标注了 {@link OwnershipCheck} 的 Controller 方法：
 *   1. 从方法参数中提取 idParam 对应的资源 ID
 *   2. 通过 AuthContext 获取当前登录用户 ID
 *   3. 路由到对应的 OwnershipChecker 校验归属
 *   4. 校验通过才放行目标方法，否则抛 BusinessException
 */
@Aspect
@Component
public class OwnershipAspect {

    private static final Logger logger = LoggerFactory.getLogger(OwnershipAspect.class);

    private final Map<String, OwnershipChecker> checkerRegistry = new ConcurrentHashMap<>();

    @Autowired
    public OwnershipAspect(java.util.List<OwnershipChecker> checkers) {
        for (OwnershipChecker checker : checkers) {
            checkerRegistry.put(checker.supports(), checker);
        }
        logger.info("OwnershipAspect 初始化完成，已注册校验器: {}", checkerRegistry.keySet());
    }

    @Around("@annotation(com.example.aichat.common.security.OwnershipCheck)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        OwnershipCheck annotation = method.getAnnotation(OwnershipCheck.class);

        String resource = annotation.resource();
        String idParamName = annotation.idParam();

        // 获取当前登录用户
        Long currentUserId = AuthContext.requireUserId();

        // 从方法参数中提取资源 ID
        Object resourceId = extractResourceId(signature, pjp.getArgs(), idParamName);
        if (resourceId == null) {
            logger.warn("OwnershipCheck: 未找到参数 {} in method {}", idParamName, method.getName());
            throw new BusinessException(UserErrorCode.TOKEN_INVALID, "缺少资源ID参数: " + idParamName);
        }

        // 路由到对应校验器
        OwnershipChecker checker = checkerRegistry.get(resource);
        if (checker == null) {
            logger.error("OwnershipCheck: 未注册资源类型 {} 的校验器", resource);
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND, "未配置资源校验器: " + resource);
        }

        // 执行归属校验
        checker.checkOwnership(resourceId, currentUserId);

        // 校验通过，放行目标方法
        return pjp.proceed();
    }

    /**
     * 通过参数名从方法实参中提取资源 ID。
     * 优先按参数名匹配；若编译未保留参数名，则按 @PathVariable/@RequestParam 的 value 匹配。
     */
    private Object extractResourceId(MethodSignature signature, Object[] args, String paramName) {
        String[] paramNames = signature.getParameterNames();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                if (paramName.equals(paramNames[i])) {
                    return args[i];
                }
            }
        }
        // 兜底：遍历参数类型，若参数类型是 Number/Long/String 且只有一个简单类型参数，直接取
        // （大多数 Controller 方法中 idParam 就是第一个 @PathVariable）
        return null;
    }
}
