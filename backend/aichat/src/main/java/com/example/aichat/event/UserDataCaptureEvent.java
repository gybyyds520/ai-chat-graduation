package com.example.aichat.event;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户数据采集事件（Spring 事件驱动自动采集机制的载体）。
 *
 * <p>各业务 Service 在写操作成功后发布本事件，由
 * {@link UserDataCaptureEventListener} 异步消费，完成开关判断、去重、
 * 脱敏、完整性评分并落库到 user_data_record。</p>
 *
 * <p>设计要点：事件只携带必要信息，不持有任何业务实体引用，避免与业务事务耦合；
 * sourceId 为业务记录主键，用于同一业务事件的幂等去重。</p>
 */
public class UserDataCaptureEvent {

    /** 触发采集的用户 ID。 */
    private final Long userId;

    /** 数据类型（snake_case，模块_行为），详见规则文档《数据类型映射规范》。 */
    private final String dataType;

    /** 采集内容（原始文本，落库前由监听器统一脱敏）。 */
    private final String content;

    /** 扩展元数据（如 groupId、toolName、documentId、actionType 等）。 */
    private final Map<String, Object> metadata;

    /** 业务记录 ID（帖子/消息/记忆等主键），与 userId+dataType 联合去重。 */
    private final String sourceId;

    /** 采集时间，为空时由监听器补当前时间。 */
    private final LocalDateTime captureTime;

    public UserDataCaptureEvent(Long userId, String dataType, String content,
                                Map<String, Object> metadata, String sourceId,
                                LocalDateTime captureTime) {
        this.userId = userId;
        this.dataType = dataType;
        this.content = content;
        this.metadata = metadata;
        this.sourceId = sourceId;
        this.captureTime = captureTime;
    }

    public Long getUserId() {
        return userId;
    }

    public String getDataType() {
        return dataType;
    }

    public String getContent() {
        return content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getSourceId() {
        return sourceId;
    }

    public LocalDateTime getCaptureTime() {
        return captureTime;
    }
}
