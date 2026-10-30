package com.example.aichat.service.tool.impl;

import com.example.aichat.entity.Friendship;
import com.example.aichat.service.FriendshipService;
import com.example.aichat.service.tool.Tool;
import com.example.aichat.service.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询用户好友列表工具。
 *
 * 入参 userId（当前登录用户 ID），返回该用户已接受的好友摘要列表。
 * 用于对话中角色回答"我有哪些好友/朋友"之类的问题。
 */
@Component
public class QueryUserFriendsTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(QueryUserFriendsTool.class);

    private final FriendshipService friendshipService;

    public QueryUserFriendsTool(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @Override
    public String getName() {
        return "query_user_friends";
    }

    @Override
    public String getDescription() {
        return "查询当前用户的好友列表，入参 userId";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> userId = new LinkedHashMap<>();
        userId.put("type", "number");
        userId.put("description", "当前登录用户 ID");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("userId", userId);
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", List.of("userId"));
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        Object uid = parameters.get("userId");
        if (uid == null) {
            return ToolResult.failure("缺少用户 ID");
        }
        long userId;
        try {
            userId = ((Number) uid).longValue();
        } catch (ClassCastException e) {
            return ToolResult.failure("用户 ID 格式错误");
        }
        try {
            List<Friendship> friends = friendshipService.getFriendList(userId);
            List<Map<String, Object>> brief = new ArrayList<>();
            if (friends != null) {
                for (Friendship f : friends) {
                    // 仅返回已接受状态的好友
                    if (f.getStatus() != null && !"accepted".equalsIgnoreCase(f.getStatus())) {
                        continue;
                    }
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("friendId", f.getFriendId());
                    item.put("username", f.getUsername());
                    item.put("nickname", f.getNickname());
                    brief.add(item);
                }
            }
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("total", brief.size());
            data.put("friends", brief);
            return ToolResult.success(data);
        } catch (Exception e) {
            log.warn("query_user_friends 执行失败 userId={}: {}", userId, e.getMessage());
            return ToolResult.failure("查询好友列表失败");
        }
    }
}
