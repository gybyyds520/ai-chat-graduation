package com.example.aichat.event;

import com.example.aichat.entity.UserDataRecord;
import com.example.aichat.mapper.UserDataRecordMapper;
import com.example.aichat.service.DataPoolConfigService;
import com.example.aichat.util.UserDataCaptureRules;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户数据采集事件监听器。
 *
 * <p>消费 {@link UserDataCaptureEvent}，完成自动采集闭环：
 * <ol>
 *   <li>读取 autoSave 开关（来自 DB 配置），关闭则跳过；</li>
 *   <li>参数与 dataType 合法性校验，空内容/非法类型拒绝；</li>
 *   <li>隐私脱敏（手机号/邮箱/身份证）+ 超长截断；</li>
 *   <li>基于 userId+dataType+content 的去重（重复则跳过，不新增）；</li>
 *   <li>完整性评分（0-100）并落库到 user_data_record。</li>
 * </ol>
 *
 * <p>使用 {@code @TransactionalEventListener(AFTER_COMMIT, fallbackExecution=true)}：
 * 业务事务提交后才采集，避免把回滚的脏数据采进来；无事务上下文的调用点也能降级立即执行。
 * {@code @Async} 保证采集走异步线程池，对业务接口耗时影响 < 5ms。</p>
 */
@Component
public class UserDataCaptureEventListener {

    private static final Logger logger = LoggerFactory.getLogger(UserDataCaptureEventListener.class);

    private final DataPoolConfigService configService;
    private final UserDataRecordMapper userDataRecordMapper;
    private final ObjectMapper objectMapper;

    public UserDataCaptureEventListener(DataPoolConfigService configService,
                                        UserDataRecordMapper userDataRecordMapper,
                                        ObjectMapper objectMapper) {
        this.configService = configService;
        this.userDataRecordMapper = userDataRecordMapper;
        this.objectMapper = objectMapper;
    }

    @Async("bizTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCapture(UserDataCaptureEvent event) {
        try {
            handle(event);
        } catch (Exception e) {
            // 采集失败绝不能影响主业务
            logger.warn("用户数据采集失败, dataType={}, sourceId={}: {}",
                    event.getDataType(), event.getSourceId(), e.getMessage());
        }
    }

    private void handle(UserDataCaptureEvent event) {
        // 1. autoSave 开关（从 DB 配置读取）
        if (!configService.isAutoSaveEnabled()) {
            logger.debug("自动采集已关闭，跳过, userId={}, dataType={}", event.getUserId(), event.getDataType());
            return;
        }

        // 2. 基础校验
        Long userId = event.getUserId();
        String dataType = event.getDataType();
        if (userId == null) {
            logger.debug("采集事件缺少 userId，拒绝");
            return;
        }
        if (!UserDataCaptureRules.isValidDataType(dataType)) {
            logger.debug("非法 dataType，拒绝采集: {}", dataType);
            return;
        }
        String content = event.getContent();
        if (content == null || content.isBlank()) {
            logger.debug("空内容，拒绝采集, userId={}, dataType={}", userId, dataType);
            return;
        }

        // 3. 脱敏 + 超长截断
        content = UserDataCaptureRules.sanitize(content);
        content = UserDataCaptureRules.truncateIfNeeded(content);

        // 4. 去重：userId + dataType + content 已存在则跳过（避免重复采集）
        UserDataRecord existing = userDataRecordMapper.selectByUniqueKey(userId, dataType, content);
        if (existing != null) {
            logger.debug("重复采集事件，跳过, userId={}, dataType={}, sourceId={}", userId, dataType, event.getSourceId());
            return;
        }

        // 5. 组装 metadata（含 sourceId 与业务透传字段）
        Map<String, Object> metadata = event.getMetadata() != null
                ? new HashMap<>(event.getMetadata()) : new HashMap<>();
        if (event.getSourceId() != null) {
            metadata.put("sourceId", event.getSourceId());
        }
        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            metadataJson = "{}";
        }

        // 6. 组装记录
        UserDataRecord record = new UserDataRecord();
        record.setUserId(userId);
        record.setDataType(dataType);
        record.setContent(content);
        record.setMetadata(metadataJson);
        record.setTags(dataType);
        record.setStatus("VALIDATED");
        record.setBackupStatus("PENDING_BACKUP");
        record.setCaptureTime(event.getCaptureTime() != null ? event.getCaptureTime() : LocalDateTime.now());
        // groupId 从 metadata 提取（群聊等场景），其余用 dataType 作为分组
        Object groupId = metadata.get("groupId");
        record.setGroupId(groupId != null ? String.valueOf(groupId) : dataType);

        // 7. 完整性评分（0-100）
        int score = UserDataCaptureRules.score(content, metadataJson, record.getTags(),
                dataType, record.getCaptureTime());
        record.setIntegrityScore((double) score);

        userDataRecordMapper.insert(record);
        logger.debug("自动采集落库成功, userId={}, dataType={}, sourceId={}, score={}",
                userId, dataType, event.getSourceId(), score);
    }
}
