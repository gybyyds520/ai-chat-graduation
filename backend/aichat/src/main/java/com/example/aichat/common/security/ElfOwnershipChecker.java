package com.example.aichat.common.security;

import com.example.aichat.common.exception.AiErrorCode;
import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.entity.AiElf;
import com.example.aichat.service.AiElfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * B2: 精灵（elf）资源归属校验器。
 *
 * 替代 AiElfController 中手写的 verifyElfOwnership(elfId) 方法。
 */
@Component
public class ElfOwnershipChecker implements OwnershipChecker {

    private static final Logger logger = LoggerFactory.getLogger(ElfOwnershipChecker.class);

    private final AiElfService aiElfService;

    @Autowired
    public ElfOwnershipChecker(AiElfService aiElfService) {
        this.aiElfService = aiElfService;
    }

    @Override
    public String supports() {
        return "elf";
    }

    @Override
    public void checkOwnership(Object resourceId, Long currentUserId) {
        Long elfId = toLong(resourceId);
        AiElf elf = aiElfService.getElfById(elfId);
        if (elf == null) {
            throw new BusinessException(AiErrorCode.ELF_NOT_FOUND, "精灵不存在");
        }
        if (!currentUserId.equals(elf.getUserId())) {
            logger.warn("[IDOR_BLOCK] userId={} 试图操作 elfId={} (归属用户={})",
                    currentUserId, elfId, elf.getUserId());
            throw new BusinessException(AiErrorCode.ELF_FORBIDDEN, "无权操作该精灵");
        }
    }

    private Long toLong(Object resourceId) {
        if (resourceId instanceof Long) {
            return (Long) resourceId;
        }
        if (resourceId instanceof Number) {
            return ((Number) resourceId).longValue();
        }
        return Long.valueOf(resourceId.toString());
    }
}
