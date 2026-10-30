package com.example.aichat.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * M3-B1：更新通知设置请求 DTO，统一 Bean Validation。
 */
@Data
public class UpdateNotificationSettingRequest {

    /** 是否接收站内通知。 */
    @NotNull(message = "站内通知开关不能为空")
    private Boolean inAppEnabled;

    /** 是否接收邮件通知。 */
    @NotNull(message = "邮件通知开关不能为空")
    private Boolean emailEnabled;
}
