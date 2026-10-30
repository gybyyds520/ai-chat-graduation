package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.SocialErrorCode;
import com.example.aichat.entity.SocialComment;
import com.example.aichat.entity.SocialPost;
import com.example.aichat.entity.User;
import com.example.aichat.event.UserDataCaptureEvent;
import com.example.aichat.mapper.SocialCommentMapper;
import com.example.aichat.mapper.SocialLikeMapper;
import com.example.aichat.mapper.SocialPostMapper;
import com.example.aichat.mapper.SocialShareMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 全栈审查 - SocialServiceImpl 单元测试（Mockito 隔离测试）。
 *
 * 覆盖点（验证 P0/P1 修复）：
 *   1. createPost：空内容抛 POST_CONTENT_BLANK；
 *   2. createPost：超长内容（>2000）抛 POST_CONTENT_TOO_LONG；
 *   3. createPost：正常流程设置默认 mediaType/visibility 与初始计数字段；
 *   4. createComment：空内容抛 COMMENT_CONTENT_BLANK；
 *   5. createComment：超长内容（>500）抛异常；
 *   6. createComment：动态不存在抛 POST_NOT_FOUND（验证 P0 NPE 修复，不再返回 null）；
 *   7. createComment：动态已删除抛 POST_NOT_FOUND；
 *   8. createComment：正常流程递增评论数并触发通知。
 *
 * 设计说明：
 *   - 隔离所有 Mapper 与 NotificationService；
 *   - 验证 @Transactional 修饰的方法行为正确性（事务语义由 Spring 容器保证，此处验证业务逻辑）。
 */
@ExtendWith(MockitoExtension.class)
class SocialServiceImplTest {

    @Mock
    private SocialPostMapper socialPostMapper;
    @Mock
    private SocialCommentMapper socialCommentMapper;
    @Mock
    private SocialLikeMapper socialLikeMapper;
    @Mock
    private SocialShareMapper socialShareMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SocialServiceImpl socialService;

    private User sampleUser;
    private SocialPost samplePost;

    @BeforeEach
    void setUp() {
        // @InjectMocks 走构造器注入（构造器无 eventPublisher 参数），此处手动注入字段 mock
        org.springframework.test.util.ReflectionTestUtils.setField(socialService, "eventPublisher", eventPublisher);

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("alice");
        sampleUser.setNickname("Alice");

        samplePost = new SocialPost();
        samplePost.setId(100L);
        samplePost.setUserId(1L);
        samplePost.setUserName("Alice");
        samplePost.setContent("今天天气真好");
        samplePost.setLikeCount(0);
        samplePost.setCommentCount(0);
        samplePost.setIsDeleted(false);
    }

    // ============ createPost 测试 ============

    @Test
    void createPost_空内容应抛POST_CONTENT_BLANK() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> socialService.createPost(1L, "", "text", null, null, null, null, "public"));
        assertEquals(SocialErrorCode.POST_CONTENT_BLANK, ex.getErrorCode());
        verifyNoInteractions(socialPostMapper);
    }

    @Test
    void createPost_超长内容应抛POST_CONTENT_TOO_LONG() {
        String tooLong = "x".repeat(2001);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> socialService.createPost(1L, tooLong, "text", null, null, null, null, "public"));
        assertEquals(SocialErrorCode.POST_CONTENT_TOO_LONG, ex.getErrorCode());
        verifyNoInteractions(socialPostMapper);
    }

    @Test
    void createPost_内容正好2000字符应通过() {
        when(userMapper.selectById(1L)).thenReturn(sampleUser);
        String max = "x".repeat(2000);

        SocialPost result = socialService.createPost(1L, max, null, null, null, null, null, null);

        assertNotNull(result);
        verify(socialPostMapper).insert(any(SocialPost.class));
    }

    @Test
    void createPost_正常流程应设置默认字段值() {
        when(userMapper.selectById(1L)).thenReturn(sampleUser);

        ArgumentCaptor<SocialPost> captor = ArgumentCaptor.forClass(SocialPost.class);

        SocialPost result = socialService.createPost(1L, "hello", null, null, null, null, null, null);

        verify(socialPostMapper).insert(captor.capture());
        SocialPost saved = captor.getValue();

        assertEquals(1L, saved.getUserId());
        assertEquals("Alice", saved.getUserName());
        assertEquals("hello", saved.getContent());
        // 默认值验证
        assertEquals("text", saved.getMediaType(), "mediaType 默认应为 text");
        assertEquals("public", saved.getVisibility(), "visibility 默认应为 public");
        assertEquals(0, saved.getLikeCount());
        assertEquals(0, saved.getCommentCount());
        assertEquals(0, saved.getShareCount());
        assertFalse(saved.getIsDeleted());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void createPost_正常流程应发布数据采集事件() {
        // 数据池自动采集：发布动态成功后应发布 social_feed 采集事件
        when(userMapper.selectById(1L)).thenReturn(sampleUser);

        socialService.createPost(1L, "今天天气真好", null, null, null, null, null, null);

        ArgumentCaptor<UserDataCaptureEvent> captor = ArgumentCaptor.forClass(UserDataCaptureEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        UserDataCaptureEvent event = captor.getValue();
        assertEquals(1L, event.getUserId());
        assertEquals("social_feed", event.getDataType());
        assertEquals("今天天气真好", event.getContent());
        assertNotNull(event.getSourceId());
        assertNotNull(event.getCaptureTime());
        assertNotNull(event.getMetadata());
    }

    // ============ createComment 测试 ============

    @Test
    void createComment_空内容应抛COMMENT_CONTENT_BLANK() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> socialService.createComment(100L, 1L, "", null));
        assertEquals(SocialErrorCode.COMMENT_CONTENT_BLANK, ex.getErrorCode());
        verifyNoInteractions(socialCommentMapper);
    }

    @Test
    void createComment_超长内容应抛异常() {
        String tooLong = "x".repeat(501);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> socialService.createComment(100L, 1L, tooLong, null));
        assertEquals(SocialErrorCode.COMMENT_CONTENT_BLANK, ex.getErrorCode());
        verifyNoInteractions(socialCommentMapper);
    }

    @Test
    void createComment_动态不存在应抛POST_NOT_FOUND_不返回null() {
        // P0 NPE 修复验证：原实现返回 null，Controller 调用 .getXxx() 会 NPE
        when(socialPostMapper.selectById(100L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> socialService.createComment(100L, 1L, "评论内容", null));
        assertEquals(SocialErrorCode.POST_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(socialCommentMapper);
    }

    @Test
    void createComment_动态已删除应抛POST_NOT_FOUND() {
        when(socialPostMapper.selectById(100L)).thenReturn(samplePost);
        samplePost.setIsDeleted(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> socialService.createComment(100L, 1L, "评论内容", null));
        assertEquals(SocialErrorCode.POST_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(socialCommentMapper);
    }

    @Test
    void createComment_正常流程应递增评论数() {
        when(socialPostMapper.selectById(100L)).thenReturn(samplePost);
        when(userMapper.selectById(1L)).thenReturn(sampleUser);

        ArgumentCaptor<SocialComment> captor = ArgumentCaptor.forClass(SocialComment.class);

        SocialComment result = socialService.createComment(100L, 1L, "很好看！", null);

        verify(socialCommentMapper).insert(captor.capture());
        SocialComment saved = captor.getValue();
        assertEquals(100L, saved.getPostId());
        assertEquals(1L, saved.getUserId());
        assertEquals("Alice", saved.getUserName());
        assertEquals("很好看！", saved.getContent());
        assertEquals(0, saved.getLikeCount());
        assertFalse(saved.getIsDeleted());

        // 验证评论数递增
        verify(socialPostMapper).incrementCommentCount(100L);
    }

    @Test
    void createComment_非作者评论应触发通知() {
        // 帖子作者是 1L，评论者是 2L（非作者），应触发通知
        samplePost.setUserId(1L);
        User commenter = new User();
        commenter.setId(2L);
        commenter.setUsername("bob");
        commenter.setNickname("Bob");

        when(socialPostMapper.selectById(100L)).thenReturn(samplePost);
        when(userMapper.selectById(2L)).thenReturn(commenter);

        socialService.createComment(100L, 2L, "好棒！", null);

        // 验证通知被调用，接收者是帖子作者
        verify(notificationService).createNotification(
                eq(1L), eq("comment"), eq("post"), eq(100L), eq(2L),
                anyString(), anyString());
    }

    @Test
    void createComment_作者评论自己不应触发通知() {
        // 帖子作者 == 评论者，不应触发通知
        when(socialPostMapper.selectById(100L)).thenReturn(samplePost);
        when(userMapper.selectById(1L)).thenReturn(sampleUser);

        socialService.createComment(100L, 1L, "自己评论", null);

        verifyNoInteractions(notificationService);
    }
}
