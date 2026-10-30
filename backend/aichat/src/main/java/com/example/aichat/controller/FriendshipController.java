package com.example.aichat.controller;

import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import com.example.aichat.entity.Friendship;
import com.example.aichat.service.FriendshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 好友关系接口。
 *
 * 安全加固（P0）：所有以“当前用户”身份执行的操作（发送/接受/拒绝/删除好友、
 * 查看好友列表/待处理请求/关系状态）一律以登录令牌中的用户为准，
 * 忽略客户端传入的 userId，防止任意用户代他人操作好友关系。
 */
@RestController
@RequestMapping("/api/friend")
public class FriendshipController {

    @Autowired
    private FriendshipService friendshipService;

    @PostMapping("/request")
    public R sendFriendRequest(@RequestBody Map<String, Long> request) {
        Long friendId = request.get("friendId");
        
        if (friendId == null) {
            return R.error("参数错误");
        }
        
        Long userId = AuthContext.requireSelf(request.get("userId"));
        boolean success = friendshipService.sendFriendRequest(userId, friendId);
        if (success) {
            return R.success("好友请求发送成功");
        }
        return R.error("发送失败，可能已发送过请求或已是好友");
    }

    @PostMapping("/accept")
    public R acceptFriendRequest(@RequestBody Map<String, Long> request) {
        Long requesterId = request.get("requesterId");
        
        if (requesterId == null) {
            return R.error("参数错误");
        }
        
        Long userId = AuthContext.requireSelf(request.get("userId"));
        boolean success = friendshipService.acceptFriendRequest(userId, requesterId);
        if (success) {
            return R.success("已添加好友");
        }
        return R.error("操作失败");
    }

    @PostMapping("/reject")
    public R rejectFriendRequest(@RequestBody Map<String, Long> request) {
        Long requesterId = request.get("requesterId");
        
        if (requesterId == null) {
            return R.error("参数错误");
        }
        
        Long userId = AuthContext.requireSelf(request.get("userId"));
        boolean success = friendshipService.rejectFriendRequest(userId, requesterId);
        if (success) {
            return R.success("已拒绝好友请求");
        }
        return R.error("操作失败");
    }

    @PostMapping("/remove")
    public R removeFriend(@RequestBody Map<String, Long> request) {
        Long friendId = request.get("friendId");
        
        if (friendId == null) {
            return R.error("参数错误");
        }
        
        Long userId = AuthContext.requireSelf(request.get("userId"));
        boolean success = friendshipService.removeFriend(userId, friendId);
        if (success) {
            return R.success("已删除好友");
        }
        return R.error("操作失败");
    }

    @GetMapping("/list")
    public R getFriendList(@RequestParam Long userId) {
        Long current = AuthContext.requireSelf(userId);
        List<Friendship> friends = friendshipService.getFriendList(current);
        return R.success(friends);
    }

    @GetMapping("/requests")
    public R getPendingRequests(@RequestParam Long userId) {
        Long current = AuthContext.requireSelf(userId);
        List<Friendship> requests = friendshipService.getPendingRequests(current);
        return R.success(requests);
    }

    @GetMapping("/search")
    public R searchUser(@RequestParam String username) {
        Map<String, Object> user = friendshipService.getUserInfoByUsername(username);
        if (user != null) {
            return R.success(user);
        }
        return R.error("用户不存在");
    }

    @GetMapping("/status")
    public R checkFriendStatus(@RequestParam Long userId, @RequestParam Long targetId) {
        Long current = AuthContext.requireSelf(userId);
        String status = friendshipService.checkFriendStatus(current, targetId);
        return R.success(status);
    }
}
