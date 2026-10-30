package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.entity.FriendGroup;
import com.example.aichat.entity.FriendGroupMember;
import com.example.aichat.service.FriendGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friend/group")
public class FriendGroupController {

    @Autowired
    private FriendGroupService friendGroupService;

    @PostMapping("/create")
    public R createGroup(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        String name = (String) request.get("name");
        
        if (name == null || name.trim().isEmpty()) {
            return R.error("参数错误");
        }
        
        // 安全修复（P0）：分组归属强制取当前登录用户
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                userIdObj == null ? null : ((Number) userIdObj).longValue());
        
        FriendGroup group = friendGroupService.createGroup(userId, name);
        return R.success(group);
    }

    @GetMapping("/{groupId}")
    public R getGroup(@PathVariable Long groupId) {
        FriendGroup group = friendGroupService.getGroupById(groupId);
        if (group != null) {
            // 安全加固：仅本人可查看自己的分组
            com.example.aichat.common.security.AuthContext.requireSelf(group.getUserId());
            return R.success(group);
        }
        return R.error("分组不存在");
    }

    @GetMapping("/list")
    public R getGroups(@RequestParam Long userId) {
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        List<FriendGroup> groups = friendGroupService.getGroupsByUserId(current);
        return R.success(groups);
    }

    @PutMapping("/{groupId}")
    public R updateGroup(@PathVariable Long groupId, @RequestBody Map<String, String> request) {
        String name = request.get("name");
        if (name == null || name.trim().isEmpty()) {
            return R.error("参数错误");
        }
        
        // 安全加固：仅分组所有者可修改
        com.example.aichat.common.security.AuthContext.requireSelf(ownerOf(groupId));
        FriendGroup group = friendGroupService.updateGroup(groupId, name);
        if (group != null) {
            return R.success(group);
        }
        return R.error("更新失败");
    }

    @DeleteMapping("/{groupId}")
    public R deleteGroup(@PathVariable Long groupId) {
        // 安全加固：仅分组所有者可删除
        com.example.aichat.common.security.AuthContext.requireSelf(ownerOf(groupId));
        friendGroupService.deleteGroup(groupId);
        return R.success("删除成功");
    }

    @GetMapping("/members/{groupId}")
    public R getMembers(@PathVariable Long groupId) {
        // 安全加固：仅分组所有者可查看成员
        com.example.aichat.common.security.AuthContext.requireSelf(ownerOf(groupId));
        List<FriendGroupMember> members = friendGroupService.getMembersByGroupId(groupId);
        return R.success(members);
    }

    /**
     * 获取分组所有者 ID（不存在返回 null，由 requireSelf 拒绝）。
     */
    private Long ownerOf(Long groupId) {
        FriendGroup group = friendGroupService.getGroupById(groupId);
        return group != null ? group.getUserId() : null;
    }

    @PostMapping("/member/add")
    public R addMember(@RequestBody Map<String, Object> request) {
        Object groupIdObj = request.get("groupId");
        Object friendIdObj = request.get("friendId");
        
        if (groupIdObj == null || friendIdObj == null) {
            return R.error("参数错误");
        }
        
        Long groupId, friendId;
        try {
            groupId = ((Number) groupIdObj).longValue();
            friendId = ((Number) friendIdObj).longValue();
        } catch (ClassCastException e) {
            return R.error("参数错误");
        }
        
        // 安全加固：仅分组所有者可添加成员
        com.example.aichat.common.security.AuthContext.requireSelf(ownerOf(groupId));
        String remark = (String) request.get("remark");
        friendGroupService.addMember(groupId, friendId, remark);
        return R.success("添加成功");
    }

    @PostMapping("/member/remove")
    public R removeMember(@RequestBody Map<String, Object> request) {
        Object groupIdObj = request.get("groupId");
        Object friendIdObj = request.get("friendId");
        
        if (groupIdObj == null || friendIdObj == null) {
            return R.error("参数错误");
        }
        
        Long groupId, friendId;
        try {
            groupId = ((Number) groupIdObj).longValue();
            friendId = ((Number) friendIdObj).longValue();
        } catch (ClassCastException e) {
            return R.error("参数错误");
        }
        
        // 安全加固：仅分组所有者可移除成员
        com.example.aichat.common.security.AuthContext.requireSelf(ownerOf(groupId));
        friendGroupService.removeMember(groupId, friendId);
        return R.success("移除成功");
    }

    @PostMapping("/member/remark")
    public R updateRemark(@RequestBody Map<String, Object> request) {
        Object groupIdObj = request.get("groupId");
        Object friendIdObj = request.get("friendId");
        String remark = (String) request.get("remark");
        
        if (groupIdObj == null || friendIdObj == null) {
            return R.error("参数错误");
        }
        
        Long groupId, friendId;
        try {
            groupId = ((Number) groupIdObj).longValue();
            friendId = ((Number) friendIdObj).longValue();
        } catch (ClassCastException e) {
            return R.error("参数错误");
        }
        
        // 安全加固：仅分组所有者可修改成员备注
        com.example.aichat.common.security.AuthContext.requireSelf(ownerOf(groupId));
        friendGroupService.updateRemark(groupId, friendId, remark);
        return R.success("备注更新成功");
    }
}