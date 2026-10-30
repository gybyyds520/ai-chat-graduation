package com.example.aichat.service;

import com.example.aichat.entity.FriendChatMessage;

import java.util.List;
import java.util.Map;

public interface FriendChatService {
    FriendChatMessage sendMessage(Long senderId, Long receiverId, String content);
    FriendChatMessage sendFileMessage(Long senderId, Long receiverId, String fileName, String filePath, Long fileSize);
    List<FriendChatMessage> getChatHistory(Long userId, Long friendId, int limit);
    int countUnreadMessages(Long userId);
    void markMessagesAsRead(Long userId, Long friendId);
    List<Map<String, Object>> getRecentChats(Long userId);

    /**
     * M3-A9：删除一条好友聊天消息（仅消息发送者本人可删）。
     *
     * @return true 删除成功；false 消息不存在或不属于当前用户
     */
    boolean deleteMessage(Long messageId, Long userId);

    /**
     * M1 ④：按主键查询好友聊天消息（用于撤回后通知接收方）。不存在返回 null。
     */
    FriendChatMessage getMessageById(Long messageId);

    /**
     * M1 ④：撤回本人好友聊天消息（发送后 2 分钟内）。
     * 不物理删除，将内容改写为占位文案，保留消息痕迹。
     *
     * @return true 撤回成功；false 消息不存在、不归属或超时
     */
    boolean recallMessage(Long messageId, Long userId, int recallWindowMinutes);
}