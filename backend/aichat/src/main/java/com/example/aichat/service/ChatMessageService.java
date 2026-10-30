package com.example.aichat.service;

import com.example.aichat.entity.ChatMessage;

import java.util.List;

public interface ChatMessageService {
    void saveMessage(String sessionId, String senderType, String content);
    void saveMessage(String sessionId, Long userId, Long characterId, String senderType, String content);
    List<ChatMessage> getRecentMessages(String sessionId, int limit);

    /**
     * 删除某个会话下当前用户的全部消息（删除整段 AI 对话）。
     *
     * @return 实际删除的消息条数
     */
    int deleteSessionMessages(String sessionId, Long userId);

    /**
     * 删除单条消息（仅当该消息归属当前用户时）。
     *
     * @return true 删除成功；false 消息不存在或不属于当前用户
     */
    boolean deleteMessage(Long messageId, Long userId);

    // ==================== M1 社交聊天体验 ====================

    /**
     * 按主键查询消息（含归属校验前的原始记录）。不存在返回 null。
     */
    ChatMessage findById(Long messageId);

    /**
     * 查询归属当前用户的消息；不存在或不归属返回 null。
     */
    ChatMessage findOwnedMessage(Long messageId, Long userId);

    /**
     * 更新消息内容（仅用于本人用户消息的编辑）。
     *
     * @return true 更新成功
     */
    boolean updateContent(Long messageId, String content);

    /**
     * 删除会话中指定消息之后创建的所有消息（用于编辑/重新生成后清除下游回复）。
     *
     * @return 删除条数
     */
    int deleteAfterMessage(String sessionId, Long messageId);

    /**
     * 物理删除单条消息（用于重新生成时移除旧 AI 回复）。
     */
    void deleteById(Long messageId);

    /**
     * 撤回消息：将本人消息内容改写为占位文案，保留消息痕迹。
     * 调用方需自行校验归属与时间窗口。
     *
     * @return true 撤回成功
     */
    boolean recallMessage(Long messageId);
}
