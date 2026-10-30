package com.example.aichat.dto;

import com.example.aichat.dto.admin.AdminLoginRequest;
import com.example.aichat.dto.ai.AiChatRequest;
import com.example.aichat.dto.social.CreateCommentRequest;
import com.example.aichat.dto.social.CreatePostRequest;
import com.example.aichat.dto.user.RegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DTO 校验缺口补测。
 *
 * <p>覆盖此前未被任何测试类触及的请求 DTO：
 * AdminLoginRequest、AiChatRequest、CreatePostRequest、CreateCommentRequest 的
 * @NotBlank/@NotNull/@Size/@Positive/@Min/@Max 违规用例与合法用例。</p>
 */
class DtoGapTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private Set<String> messages(Object dto) {
        return validator.validate(dto).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    private long countViolations(Object dto) {
        return validator.validate(dto).size();
    }

    // ===== AdminLoginRequest =====

    @Test
    void 管理员登录_账号为空_报校验错误() {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setPassword("secret123");
        assertTrue(messages(req).contains("管理员账号不能为空"));
    }

    @Test
    void 管理员登录_账号过短_报长度错误() {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setUsername("ab"); // <3
        req.setPassword("secret123");
        assertTrue(messages(req).contains("管理员账号长度必须在3-50个字符之间"));
    }

    @Test
    void 管理员登录_账号过长_报长度错误() {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setUsername("a".repeat(51));
        req.setPassword("secret123");
        assertTrue(messages(req).contains("管理员账号长度必须在3-50个字符之间"));
    }

    @Test
    void 管理员登录_密码为空_报校验错误() {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setUsername("admin01");
        req.setPassword(null);
        assertTrue(messages(req).contains("管理员密码不能为空"));
    }

    @Test
    void 管理员登录_密码过短_报长度错误() {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setUsername("admin01");
        req.setPassword("123"); // <6
        assertTrue(messages(req).contains("管理员密码长度必须在6-50个字符之间"));
    }

    @Test
    void 管理员登录_合法请求_无校验错误() {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setUsername("admin01");
        req.setPassword("secret123");
        assertEquals(0, countViolations(req));
    }

    // ===== AiChatRequest =====

    @Test
    void AI聊天_消息为空_报校验错误() {
        AiChatRequest req = new AiChatRequest();
        req.setMessage("");
        assertTrue(messages(req).contains("聊天消息内容不能为空"));
    }

    @Test
    void AI聊天_消息超长_报长度错误() {
        AiChatRequest req = new AiChatRequest();
        req.setMessage("啊".repeat(8001));
        assertTrue(messages(req).contains("单次消息内容不能超过8000个字符"));
    }

    @Test
    void AI聊天_模型标识超长_报长度错误() {
        AiChatRequest req = new AiChatRequest();
        req.setMessage("hi");
        req.setModel("m".repeat(101));
        assertTrue(messages(req).contains("模型标识长度超限"));
    }

    @Test
    void AI聊天_会话ID为负_报正数错误() {
        AiChatRequest req = new AiChatRequest();
        req.setMessage("hi");
        req.setConversationId(-1L);
        assertFalse(messages(req).isEmpty());
    }

    @Test
    void AI聊天_角色ID为0_报正数错误() {
        AiChatRequest req = new AiChatRequest();
        req.setMessage("hi");
        req.setCharacterId(0L); // @Positive 不允许 0
        assertFalse(messages(req).isEmpty());
    }

    @Test
    void AI聊天_合法请求_无校验错误() {
        AiChatRequest req = new AiChatRequest();
        req.setMessage("你好");
        req.setModel("ollama:llama3");
        req.setConversationId(10L);
        req.setCharacterId(5L);
        assertEquals(0, countViolations(req));
    }

    @Test
    void AI聊天_仅必填消息_其余可选为空_通过() {
        AiChatRequest req = new AiChatRequest();
        req.setMessage("你好");
        assertEquals(0, countViolations(req));
    }

    // ===== CreatePostRequest =====

    @Test
    void 发动态_内容为空_报校验错误() {
        CreatePostRequest req = new CreatePostRequest();
        req.setContent("");
        assertTrue(messages(req).contains("动态内容不能为空"));
    }

    @Test
    void 发动态_内容超长_报长度错误() {
        CreatePostRequest req = new CreatePostRequest();
        req.setContent("内".repeat(2001));
        assertTrue(messages(req).contains("动态内容不能超过2000个字符"));
    }

    @Test
    void 发动态_图片字段超长_报长度错误() {
        CreatePostRequest req = new CreatePostRequest();
        req.setContent("ok");
        req.setImages("i".repeat(2001));
        assertTrue(messages(req).contains("图片字段长度超限"));
    }

    @Test
    void 发动态_标签超长_报长度错误() {
        CreatePostRequest req = new CreatePostRequest();
        req.setContent("ok");
        req.setTags("t".repeat(201));
        assertTrue(messages(req).contains("标签长度超限"));
    }

    @Test
    void 发动态_合法请求_无校验错误() {
        CreatePostRequest req = new CreatePostRequest();
        req.setContent("今天天气不错");
        req.setImages("a.jpg,b.jpg");
        req.setTags("日常");
        req.setVisibility(1);
        assertEquals(0, countViolations(req));
    }

    // ===== CreateCommentRequest =====

    @Test
    void 评论_动态ID为空_报校验错误() {
        CreateCommentRequest req = new CreateCommentRequest();
        req.setContent("好");
        assertTrue(messages(req).contains("动态 ID 不能为空"));
    }

    @Test
    void 评论_动态ID为负_报正数错误() {
        CreateCommentRequest req = new CreateCommentRequest();
        req.setPostId(-5L);
        req.setContent("好");
        assertTrue(messages(req).contains("动态 ID 必须为正数"));
    }

    @Test
    void 评论_内容为空_报校验错误() {
        CreateCommentRequest req = new CreateCommentRequest();
        req.setPostId(1L);
        req.setContent(null);
        assertTrue(messages(req).contains("评论内容不能为空"));
    }

    @Test
    void 评论_内容超长_报长度错误() {
        CreateCommentRequest req = new CreateCommentRequest();
        req.setPostId(1L);
        req.setContent("评".repeat(501));
        assertTrue(messages(req).contains("评论内容不能超过500个字符"));
    }

    @Test
    void 评论_父ID为负_报正数错误() {
        CreateCommentRequest req = new CreateCommentRequest();
        req.setPostId(1L);
        req.setContent("回复");
        req.setParentId(-1L);
        assertTrue(messages(req).contains("父评论 ID 必须为正数"));
    }

    @Test
    void 评论_合法请求_无校验错误() {
        CreateCommentRequest req = new CreateCommentRequest();
        req.setPostId(10L);
        req.setContent("写得不错");
        req.setParentId(2L);
        assertEquals(0, countViolations(req));
    }

    // ===== RegisterRequest age 边界（原 UserDtoValidationTest 未覆盖）=====

    private RegisterRequest baseValid() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice_01");
        req.setPassword("securePwd123");
        req.setEmail("alice@example.com");
        return req;
    }

    @Test
    void 注册_年龄为负数_报Min错误() {
        RegisterRequest req = baseValid();
        req.setAge(-1);
        assertTrue(messages(req).contains("年龄不能为负数"));
    }

    @Test
    void 注册_年龄超过120_报Max错误() {
        RegisterRequest req = baseValid();
        req.setAge(121);
        assertTrue(messages(req).contains("年龄输入无效"));
    }

    @Test
    void 注册_年龄边界120_通过() {
        RegisterRequest req = baseValid();
        req.setAge(120);
        assertEquals(0, countViolations(req));
    }

    @Test
    void 注册_年龄边界0_视为未声明通过() {
        RegisterRequest req = baseValid();
        req.setAge(0);
        assertEquals(0, countViolations(req));
    }

    @Test
    void 注册_年龄为null_可选字段通过() {
        RegisterRequest req = baseValid();
        req.setAge(null);
        assertEquals(0, countViolations(req));
    }

    @Test
    void 注册_昵称超长_报Size错误() {
        RegisterRequest req = baseValid();
        req.setNickname("昵".repeat(51));
        assertTrue(messages(req).contains("昵称长度不能超过50个字符"));
    }

    @Test
    void 注册_邮箱超长_报Size错误() {
        RegisterRequest req = baseValid();
        req.setEmail("a".repeat(45) + "@x.com");
        assertTrue(messages(req).contains("邮箱长度不能超过50个字符"));
    }

    // ===== 边界恰好通过用例 =====

    @Test
    void 管理员账号恰好3字符_通过() {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setUsername("abc");
        req.setPassword("secret123");
        assertEquals(0, countViolations(req));
    }

    @Test
    void 管理员密码恰好6字符_通过() {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setUsername("admin01");
        req.setPassword("123456");
        assertEquals(0, countViolations(req));
    }

    @Test
    void AI聊天消息恰好8000字符_通过() {
        AiChatRequest req = new AiChatRequest();
        req.setMessage("啊".repeat(8000));
        assertEquals(0, countViolations(req));
    }

    @Test
    void AI聊天模型标识恰好100字符_通过() {
        AiChatRequest req = new AiChatRequest();
        req.setMessage("hi");
        req.setModel("m".repeat(100));
        assertEquals(0, countViolations(req));
    }

    @Test
    void 动态内容恰好2000字符_通过() {
        CreatePostRequest req = new CreatePostRequest();
        req.setContent("内".repeat(2000));
        assertEquals(0, countViolations(req));
    }

    @Test
    void 评论内容恰好500字符_通过() {
        CreateCommentRequest req = new CreateCommentRequest();
        req.setPostId(1L);
        req.setContent("评".repeat(500));
        assertEquals(0, countViolations(req));
    }

    @Test
    void 注册密码恰好20字符_通过() {
        RegisterRequest req = baseValid();
        req.setPassword("p".repeat(20));
        assertEquals(0, countViolations(req));
    }

    @Test
    void 注册用户名恰好20字符_通过() {
        RegisterRequest req = baseValid();
        req.setUsername("u".repeat(20));
        assertEquals(0, countViolations(req));
    }
}
