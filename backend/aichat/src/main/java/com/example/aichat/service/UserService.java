package com.example.aichat.service;

import com.example.aichat.entity.User;

public interface UserService {
    User register(String username, String password, String nickname);

    /**
     * 注册（带自声明年龄）。
     * @param age 自声明年龄，null/0 表示未声明；0&lt;age&lt;18 判定为未成年人
     */
    User register(String username, String password, String nickname, Integer age);

    User login(String username, String password);
    User getUserById(Long id);
    
    /**
     * 发送重置密码邮件
     * @param email 用户邮箱
     * @return 是否发送成功
     */
    boolean sendResetPasswordEmail(String email);
    
    /**
     * 通过令牌重置密码
     * @param token 重置令牌
     * @param newPassword 新密码
     * @return 是否重置成功
     */
    boolean resetPassword(String token, String newPassword);
    
    /**
     * 修改密码
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 是否修改成功
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);
    
    /**
     * 根据邮箱查找用户
     * @param email 邮箱
     * @return 用户信息
     */
    User getUserByEmail(String email);

    /**
     * 根据手机号查找用户。
     * 重构目的：解除 UserController 直接注入 UserMapper 的分层违规，
     * 将"按手机号查询用户"能力收敛到 Service 层。
     * @param phone 手机号
     * @return 用户信息，未找到返回 null
     */
    User getUserByPhone(String phone);

    /**
     * 根据验证码重置密码。
     * 重构目的：将 UserController 中"校验验证码 + 修改密码"的事务逻辑收敛到 Service，
     * 保证事务边界完整，避免 Controller 持有 Mapper。
     * @param contact 邮箱或手机号
     * @param code 验证码
     * @param newPassword 新密码
     * @return 是否重置成功
     */
    boolean resetPasswordByCode(String contact, String code, String newPassword);
    
    /**
     * 更新用户信息
     * @param user 用户信息
     * @return 更新后的用户
     */
    User updateUser(User user);
    
    /**
     * 发送邮箱验证码
     * @param email 用户邮箱
     * @return 是否发送成功
     */
    boolean sendVerificationEmail(String email);
    
    /**
     * 发送短信验证码
     * @param phone 用户手机号
     * @return 是否发送成功
     */
    boolean sendVerificationSms(String phone);
    
    /**
     * 验证邮箱验证码
     * @param email 用户邮箱
     * @param code 验证码
     * @return 是否验证成功
     */
    boolean verifyEmailCode(String email, String code);
    
    /**
     * 验证短信验证码
     * @param phone 用户手机号
     * @param code 验证码
     * @return 是否验证成功
     */
    boolean verifySmsCode(String phone, String code);

    /**
     * 账号自助注销（软删除）。
     *
     * 安全要求：
     *   - confirmText 必须严格等于 "DELETE"，否则拒绝（二次确认）；
     *   - 仅软删除：将 accountStatus 标记为 deleted，物理记录保留以便审计；
     *   - 清理范围：对话历史（AI/好友）、好友关系、AI 记忆，并擦除头像文件；
     *   - 已注销账号再次调用直接拒绝；
     *   - 注销后该用户的 JWT 立即失效（由 JwtAuthenticationFilter 按 accountStatus 拒绝）。
     *
     * @param userId     当前登录用户 ID
     * @param confirmText 前端二次确认文本，必须为 "DELETE"
     */
    void deleteAccount(Long userId, String confirmText);
}