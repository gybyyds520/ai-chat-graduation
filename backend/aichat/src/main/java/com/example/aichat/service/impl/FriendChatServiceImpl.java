package com.example.aichat.service.impl;

import com.example.aichat.entity.FriendChatMessage;
import com.example.aichat.entity.Friendship;
import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.SocialErrorCode;
import com.example.aichat.dto.audit.AuditResult;
import com.example.aichat.event.UserDataCaptureEvent;
import com.example.aichat.mapper.FriendChatMessageMapper;
import com.example.aichat.mapper.FriendshipMapper;
import com.example.aichat.service.ContentAuditService;
import com.example.aichat.service.FriendChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class FriendChatServiceImpl implements FriendChatService {

    private static final Logger logger = LoggerFactory.getLogger(FriendChatServiceImpl.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private static final int STATUS_UNREAD = 0;
    private static final int STATUS_READ = 1;
    /** 私聊消息内容长度上限（与 WebSocket 路径保持一致）。 */
    private static final int MAX_MESSAGE_LENGTH = 8000;

    private final FriendChatMessageMapper chatMessageMapper;
    private final FriendshipMapper friendshipMapper;
    private final ContentAuditService contentAuditService;

    /** 数据池自动采集事件发布器。 */
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public FriendChatServiceImpl(FriendChatMessageMapper chatMessageMapper, FriendshipMapper friendshipMapper,
                                ContentAuditService contentAuditService) {
        this.chatMessageMapper = chatMessageMapper;
        this.friendshipMapper = friendshipMapper;
        this.contentAuditService = contentAuditService;
    }

    @Override
    public FriendChatMessage sendMessage(Long senderId, Long receiverId, String content) {
        logger.info("[好友消息发送] 发送者ID: {}, 接收者ID: {}", senderId, receiverId);

        // 参数校验（P0）：原实现直接调用 content.length()，content 为 null 时抛 NPE；
        // 同时补充空内容与超长校验，与 WebSocket 路径保持一致。校验失败返回 null（由 Controller 统一转错误响应）。
        if (content == null || content.trim().isEmpty()) {
            logger.warn("[好友消息发送] 失败：消息内容为空");
            return null;
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            logger.warn("[好友消息发送] 失败：消息超过 {} 字符", MAX_MESSAGE_LENGTH);
            return null;
        }
        logger.info("[好友消息发送] 消息内容: {}", content.length() > 50 ? content.substring(0, 50) + "..." : content);

        Friendship friendship = friendshipMapper.findByUserAndFriend(senderId, receiverId);
        if (friendship == null || !"accepted".equals(friendship.getStatus())) {
            logger.warn("[好友消息发送] 验证失败: 不是好友关系");
            logger.info("[好友消息发送] 当前好友关系状态: {}", friendship == null ? "不存在" : friendship.getStatus());
            logger.info("═══════════════════════════════════════════════════════════════");
            return null;
        }
        
        logger.info("[好友消息发送] 好友关系验证通过");

        // 内容审核：命中敏感词则记录日志并抛 CONTENT_VIOLATION（不入库）
        AuditResult auditResult = contentAuditService.audit(content, "private_chat", senderId);
        contentAuditService.recordAuditLog(senderId, "private_chat", null, content, auditResult);
        if (!auditResult.isPassed()) {
            logger.warn("[好友消息发送] 内容命中敏感词，已被拦截 senderId={}", senderId);
            throw new BusinessException(SocialErrorCode.CONTENT_VIOLATION);
        }

        FriendChatMessage message = new FriendChatMessage();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setType("text");
        message.setMessageType("text");
        message.setIsRead(false);
        message.setCreateTime(LocalDateTime.now());
        message.setStatus(STATUS_UNREAD);
        
        int result = chatMessageMapper.insert(message);
        if (result > 0) {
            logger.info("[好友消息发送] 消息插入成功");
            logger.info("[好友消息发送] 消息ID: {}", message.getId());
            logger.info("[好友消息发送] 创建时间: {}", message.getCreateTime().format(FORMATTER));
            logger.info("[好友消息发送] 消息状态: 未读");
        } else {
            logger.error("[好友消息发送] 消息插入失败");
        }

        logger.info("═══════════════════════════════════════════════════════════════");

        // 数据池自动采集：私聊消息。仅记录发送方 userId 与消息内容，
        // 不记录接收者用户名/手机号（receiverId 为数字 ID，可入 metadata）。
        if (eventPublisher != null) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("receiverId", receiverId);
            eventPublisher.publishEvent(new UserDataCaptureEvent(
                    senderId, "private_chat", content, meta,
                    "pmsg:" + message.getId(), message.getCreateTime()));
        }
        return message;
    }

    @Override
    public FriendChatMessage sendFileMessage(Long senderId, Long receiverId, String fileName, String filePath, Long fileSize) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("[好友文件发送] 开始处理文件发送请求");
        logger.info("[好友文件发送] 发送者ID: {}, 接收者ID: {}", senderId, receiverId);
        logger.info("[好友文件发送] 文件名: {}, 文件大小: {}", fileName, fileSize);
        
        Friendship friendship = friendshipMapper.findByUserAndFriend(senderId, receiverId);
        if (friendship == null || !"accepted".equals(friendship.getStatus())) {
            logger.warn("[好友文件发送] 验证失败: 不是好友关系");
            logger.info("═══════════════════════════════════════════════════════════════");
            return null;
        }
        
        logger.info("[好友文件发送] 好友关系验证通过");

        // P1-12 修复：filePath 落库前必须净化。虽然后端上传路径由 FileUploadUtil 生成，
        // 但服务方法信任入参 filePath，若被传入 "../../../etc/passwd" 等穿越路径，
        // 接收端按该路径读盘即可越权读任意文件。这里拒绝任何含 ".." 分量或 Windows 盘符的路径。
        if (!isSafeFilePath(filePath)) {
            logger.warn("[好友文件发送] 拒绝不安全的文件路径 senderId={}, filePath={}", senderId, filePath);
            return null;
        }

        FriendChatMessage message = new FriendChatMessage();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent("[文件] " + fileName);
        message.setType("file");
        message.setMessageType("file");
        message.setFileName(fileName);
        message.setFilePath(filePath);
        message.setFileSize(fileSize);
        message.setIsRead(false);
        message.setCreateTime(LocalDateTime.now());
        message.setStatus(STATUS_UNREAD);
        
        int result = chatMessageMapper.insert(message);
        if (result > 0) {
            logger.info("[好友文件发送] 文件消息插入成功");
            logger.info("[好友文件发送] 消息ID: {}", message.getId());
        } else {
            logger.error("[好友文件发送] 文件消息插入失败");
        }
        
        logger.info("═══════════════════════════════════════════════════════════════");
        return message;
    }

    /**
     * P1-12：校验落库的文件路径是否安全。
     * 允许服务端生成的 web 相对路径（如 /uploads/chat/uuid.png），拒绝：
     *   - Windows 盘符绝对路径（C:\...）；
     *   - 任何 ".." 路径分量（../../../etc/passwd 等穿越载荷）。
     */
    private boolean isSafeFilePath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return false;
        }
        String normalized = filePath.replace('\\', '/');
        if (normalized.matches("^[a-zA-Z]:/.*")) {
            return false;
        }
        for (String part : normalized.split("/")) {
            if ("..".equals(part)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<FriendChatMessage> getChatHistory(Long userId, Long friendId, int limit) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("[获取聊天记录] 开始获取对话历史");
        logger.info("[获取聊天记录] 用户ID: {}, 好友ID: {}, 限制数量: {}", userId, friendId, limit);
        
        List<FriendChatMessage> messages = chatMessageMapper.getChatHistory(userId, friendId, limit);
        logger.info("[获取聊天记录] 查询结果数量: {}", messages.size());
        
        Collections.reverse(messages);
        
        markMessagesAsRead(userId, friendId);
        
        logger.info("[获取聊天记录] 聊天记录已按时间正序排列");
        logger.info("[获取聊天记录] 已自动标记消息为已读");
        logger.info("═══════════════════════════════════════════════════════════════");
        
        return messages;
    }

    @Override
    public int countUnreadMessages(Long userId) {
        int count = chatMessageMapper.countUnreadMessages(userId);
        logger.debug("[未读消息统计] 用户ID: {}, 未读消息数: {}", userId, count);
        return count;
    }

    @Override
    public void markMessagesAsRead(Long userId, Long friendId) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("[标记消息已读] 开始标记消息状态");
        logger.info("[标记消息已读] 用户ID: {}, 好友ID: {}", userId, friendId);
        
        chatMessageMapper.markMessagesAsRead(userId, friendId);
        
        logger.info("[标记消息已读] 标记完成");
        logger.info("═══════════════════════════════════════════════════════════════");
    }

    @Override
    public List<Map<String, Object>> getRecentChats(Long userId) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("[获取最近聊天] 开始获取用户最近聊天列表");
        logger.info("[获取最近聊天] 用户ID: {}", userId);
        
        List<Friendship> friends = friendshipMapper.getFriendList(userId);
        logger.info("[获取最近聊天] 好友数量: {}", friends.size());
        
        List<Map<String, Object>> recentChats = new ArrayList<>();
        
        for (Friendship friend : friends) {
            Long friendId = friend.getFriendId();
            List<FriendChatMessage> messages = chatMessageMapper.getChatHistory(userId, friendId, 1);
            
            Map<String, Object> chat = new HashMap<>();
            chat.put("friendId", friendId);
            chat.put("username", friend.getUsername());
            chat.put("nickname", friend.getNickname());
            chat.put("avatar", friend.getAvatar());
            
            if (!messages.isEmpty()) {
                FriendChatMessage lastMessage = messages.get(0);
                String lastContent = lastMessage.getContent();
                chat.put("lastMessage", lastContent);
                chat.put("lastTime", lastMessage.getCreateTime());
                // 防御：content 可能为 null（历史脏数据），避免 NPE
                String preview = lastContent == null ? ""
                        : (lastContent.length() > 20 ? lastContent.substring(0, 20) + "..." : lastContent);
                logger.debug("[获取最近聊天] 好友ID: {}, 最后消息: {}", friendId, preview);
            } else {
                chat.put("lastMessage", "暂无消息");
                chat.put("lastTime", null);
                logger.debug("[获取最近聊天] 好友ID: {}, 状态: 暂无消息", friendId);
            }
            
            recentChats.add(chat);
        }
        
        recentChats.sort((a, b) -> {
            Object timeA = a.get("lastTime");
            Object timeB = b.get("lastTime");
            if (timeA == null) return 1;
            if (timeB == null) return -1;
            return ((LocalDateTime) timeB).compareTo((LocalDateTime) timeA);
        });
        
        logger.info("[获取最近聊天] 聊天列表已按时间排序");
        logger.info("[获取最近聊天] 返回聊天数量: {}", recentChats.size());
        logger.info("═══════════════════════════════════════════════════════════════");

        return recentChats;
    }

    @Override
    public boolean deleteMessage(Long messageId, Long userId) {
        FriendChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null || message.getSenderId() == null || !userId.equals(message.getSenderId())) {
            return false;
        }
        return chatMessageMapper.deleteById(messageId) > 0;
    }

    @Override
    public FriendChatMessage getMessageById(Long messageId) {
        if (messageId == null) {
            return null;
        }
        return chatMessageMapper.selectById(messageId);
    }

    @Override
    public boolean recallMessage(Long messageId, Long userId, int recallWindowMinutes) {
        FriendChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null || message.getSenderId() == null || !userId.equals(message.getSenderId())) {
            logger.warn("[消息撤回] 失败：消息不存在或不归属 - messageId: {}, userId: {}", messageId, userId);
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = message.getCreateTime();
        if (created != null && created.plusMinutes(recallWindowMinutes).isBefore(now)) {
            logger.warn("[消息撤回] 失败：超过时间窗 - messageId: {}, created: {}", messageId, created);
            return false;
        }
        message.setContent("消息已撤回");
        message.setMessageType("recall");
        message.setType("recall");
        boolean ok = chatMessageMapper.updateById(message) > 0;
        logger.info("[消息撤回] 成功 - messageId: {}, userId: {}, ok: {}", messageId, userId, ok);
        return ok;
    }
}