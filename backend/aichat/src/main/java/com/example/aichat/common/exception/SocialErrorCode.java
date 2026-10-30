package com.example.aichat.common.exception;

/**
 * 社交域错误码（前缀 2000x）。
 *
 * Phase 3 (P1-1) 引入：覆盖动态/评论/点赞/分享/好友/群聊等社交场景，
 * 替代 SocialController 里散落的 R.badRequest("xxx") 手写字符串。
 */
public enum SocialErrorCode implements ErrorCode {

    // 动态相关（20001-20099）
    POST_NOT_FOUND(20001, "动态不存在", 404),
    POST_CONTENT_BLANK(20002, "动态内容不能为空"),
    POST_CONTENT_TOO_LONG(20003, "动态内容不能超过2000个字符"),
    POST_NO_PERMISSION(20004, "无权操作他人动态", 403),

    // 评论相关（20100-20199）
    COMMENT_NOT_FOUND(20100, "评论不存在", 404),
    COMMENT_CONTENT_BLANK(20101, "评论内容不能为空"),
    COMMENT_NO_PERMISSION(20102, "无权删除他人评论", 403),

    // 点赞相关（20200-20299）
    ALREADY_LIKED(20200, "已点赞过该内容"),
    NOT_LIKED_YET(20201, "尚未点赞，无法取消"),

    // 好友相关（20300-20399）
    FRIEND_ALREADY(20300, "已经是好友关系"),
    FRIEND_REQUEST_NOT_FOUND(20301, "好友请求不存在", 404),
    FRIEND_REQUEST_PROCESSED(20302, "好友请求已处理"),
    CANNOT_ADD_SELF(20303, "不能添加自己为好友"),

    // 群聊相关（20400-20499）
    GROUP_NOT_FOUND(20400, "群聊不存在", 404),
    NOT_GROUP_MEMBER(20401, "非群聊成员", 403),
    ALREADY_GROUP_MEMBER(20402, "已是群聊成员"),
    GROUP_NAME_BLANK(20403, "群名称不能为空"),
    GROUP_NAME_TOO_LONG(20404, "群名称不能超过50个字符"),
    GROUP_MESSAGE_BLANK(20405, "群消息内容不能为空"),
    GROUP_MESSAGE_TOO_LONG(20406, "群消息内容不能超过8000个字符"),
    COMMUNITY_POST_BLANK(20407, "帖子内容不能为空"),
    COMMUNITY_POST_TOO_LONG(20408, "帖子内容不能超过2000个字符"),
    NOT_GROUP_OWNER(20409, "仅群主可执行此操作", 403),
    COMMUNITY_NOT_OWNER(20410, "仅社群管理员可执行此操作", 403),
    COMMUNITY_COMMENT_BLANK(20411, "评论内容不能为空"),
    COMMUNITY_COMMENT_TOO_LONG(20412, "评论内容不能超过500个字符"),

    // 内容审核相关（20500-20599）
    CONTENT_VIOLATION(20500, "内容包含违规信息，已被拦截", 400),
    REPORT_ALREADY_SUBMITTED(20501, "已举报过该对象，请勿重复举报");

    private final int code;
    private final String message;
    private final int httpStatus;

    SocialErrorCode(int code, String message) {
        this(code, message, 400);
    }

    SocialErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }
}
