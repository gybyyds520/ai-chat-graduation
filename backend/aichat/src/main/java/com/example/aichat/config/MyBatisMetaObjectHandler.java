package com.example.aichat.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.example.aichat.common.security.AuthContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * MyBatis-Plus 自动填充处理器 - Phase 3 (P3-数据库优化) 引入。
 *
 * 配合 V5__add_audit_fields.sql 中新增的 created_by / updated_by 字段，
 * 在 INSERT/UPDATE 时自动注入操作者 ID 与时间戳，无需业务代码手写。
 *
 * 使用方式：
 *   Entity 上标注 @TableField(fill = FieldFill.INSERT) private Long createdBy;
 *   @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
 *
 * 降级策略：
 *   无 HTTP 上下文（如定时任务、异步任务）时，createdBy/updatedBy 填 null，
 *   时间戳由数据库默认值兜底（create_time DEFAULT CURRENT_TIMESTAMP）。
 */
@Component
public class MyBatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Optional<Long> userId = AuthContext.getCurrentUserId();

        // 创建时间（实体未显式赋值时填充）
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "createBy", Long.class, userId.orElse(null));
        strictInsertFill(metaObject, "createdBy", Long.class, userId.orElse(null));

        // 更新时间（首次插入也填，便于查询排序）
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateBy", Long.class, userId.orElse(null));
        strictInsertFill(metaObject, "updatedBy", Long.class, userId.orElse(null));
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Optional<Long> userId = AuthContext.getCurrentUserId();

        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);
        strictUpdateFill(metaObject, "updateBy", Long.class, userId.orElse(null));
        strictUpdateFill(metaObject, "updatedBy", Long.class, userId.orElse(null));
    }
}
