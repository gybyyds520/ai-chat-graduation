package com.example.aichat.service.impl;

import com.example.aichat.entity.FriendChatMessage;
import com.example.aichat.entity.Friendship;
import com.example.aichat.mapper.FriendChatMessageMapper;
import com.example.aichat.mapper.FriendshipMapper;
import com.example.aichat.service.ContentAuditService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * P1-12 回归：sendFileMessage 落库前必须拒绝含 ".." 穿越段或绝对盘符的 filePath，
 * 防止接收端按该路径读盘造成路径穿越读任意文件。
 */
class FriendFileTraversalTest {

    private FriendChatServiceImpl newService(FriendshipMapper fm) {
        return new FriendChatServiceImpl(mock(FriendChatMessageMapper.class), fm, mock(ContentAuditService.class));
    }

    private Friendship accepted() {
        Friendship f = new Friendship();
        f.setStatus("accepted");
        return f;
    }

    @Test
    void 穿越路径_拒绝入库() {
        FriendshipMapper fm = mock(FriendshipMapper.class);
        when(fm.findByUserAndFriend(1L, 2L)).thenReturn(accepted());
        FriendChatMessageMapper cm = mock(FriendChatMessageMapper.class);
        FriendChatServiceImpl service = new FriendChatServiceImpl(cm, fm, mock(ContentAuditService.class));

        FriendChatMessage result = service.sendFileMessage(1L, 2L, "x.png", "../../../etc/passwd", 10L);

        assertNull(result, "含 .. 穿越的 filePath 必须被拒绝");
        verify(cm, never()).insert(any(FriendChatMessage.class));
    }

    @Test
    void 盘符绝对路径_拒绝入库() {
        FriendshipMapper fm = mock(FriendshipMapper.class);
        when(fm.findByUserAndFriend(1L, 2L)).thenReturn(accepted());
        FriendChatMessageMapper cm = mock(FriendChatMessageMapper.class);
        FriendChatServiceImpl service = new FriendChatServiceImpl(cm, fm, mock(ContentAuditService.class));

        FriendChatMessage result = service.sendFileMessage(1L, 2L, "x.png", "C:/Windows/system32/x", 10L);
        assertNull(result);
        verify(cm, never()).insert(any(FriendChatMessage.class));
    }

    @Test
    void 正常uploads路径_放行() {
        FriendshipMapper fm = mock(FriendshipMapper.class);
        when(fm.findByUserAndFriend(1L, 2L)).thenReturn(accepted());
        FriendChatMessageMapper cm = mock(FriendChatMessageMapper.class);
        when(cm.insert(any(FriendChatMessage.class))).thenReturn(1);
        FriendChatServiceImpl service = new FriendChatServiceImpl(cm, fm, mock(ContentAuditService.class));

        FriendChatMessage result = service.sendFileMessage(1L, 2L, "x.png", "/uploads/chat/uuid.png", 10L);
        assertNotNull(result);
        verify(cm).insert(any(FriendChatMessage.class));
    }
}
