package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.aichat.entity.UserProfile;
import com.example.aichat.mapper.UserProfileMapper;
import com.example.aichat.service.UserProfileService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户画像服务实现（简单版，纯本地离线算法）。
 */
@Service
public class UserProfileServiceImpl implements UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileServiceImpl.class);

    /** commonTopics 最多保留的话题数 */
    private static final int MAX_TOPICS = 10;

    private final UserProfileMapper profileMapper;
    private final ObjectMapper objectMapper;

    public UserProfileServiceImpl(UserProfileMapper profileMapper) {
        this.profileMapper = profileMapper;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public UserProfile getOrCreateProfile(Long userId) {
        UserProfile profile = profileMapper.selectByUserId(userId);
        if (profile != null) {
            return profile;
        }
        // 不存在：返回内存默认画像（不落库，避免空对话也建记录）
        UserProfile empty = new UserProfile();
        empty.setUserId(userId);
        empty.setPreferredStyle(null);
        empty.setCommonTopics("[]");
        empty.setActiveTimeSlots("{}");
        return empty;
    }

    @Override
    public UserProfile updateProfile(Long userId, String preferredStyle,
                                     List<String> commonTopics,
                                     Map<String, Integer> activeTimeSlots) {
        UserProfile profile = profileMapper.selectByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setCreatedAt(now);
        }
        if (preferredStyle != null && !preferredStyle.isBlank()) {
            profile.setPreferredStyle(preferredStyle.trim());
        }
        if (commonTopics != null) {
            profile.setCommonTopics(toJsonList(commonTopics));
        }
        if (activeTimeSlots != null) {
            profile.setActiveTimeSlots(toJsonMap(activeTimeSlots));
        }
        profile.setUpdatedAt(now);
        if (profile.getId() == null) {
            if (profile.getCommonTopics() == null) {
                profile.setCommonTopics("[]");
            }
            if (profile.getActiveTimeSlots() == null) {
                profile.setActiveTimeSlots("{}");
            }
            profileMapper.insert(profile);
        } else {
            profileMapper.updateById(profile);
        }
        return profile;
    }

    @Override
    public void recordConversation(Long userId, String message) {
        try {
            UserProfile profile = profileMapper.selectByUserId(userId);
            LocalDateTime now = LocalDateTime.now();
            boolean isNew = profile == null;
            if (isNew) {
                profile = new UserProfile();
                profile.setUserId(userId);
                profile.setCreatedAt(now);
                profile.setCommonTopics("[]");
                profile.setActiveTimeSlots("{}");
            }

            // 1) 累加活跃时段
            Map<String, Integer> slots = readSlots(profile.getActiveTimeSlots());
            String slot = hourToSlot(now.getHour());
            slots.merge(slot, 1, Integer::sum);
            profile.setActiveTimeSlots(toJsonMap(slots));

            // 2) 抽取话题并入 commonTopics
            List<String> topics = readTopics(profile.getCommonTopics());
            for (String t : extractTopics(message)) {
                if (!topics.contains(t)) {
                    topics.add(t);
                }
            }
            // 截断到上限（保留最新）
            while (topics.size() > MAX_TOPICS) {
                topics.remove(0);
            }
            profile.setCommonTopics(toJsonList(topics));

            profile.setUpdatedAt(now);
            if (isNew) {
                profileMapper.insert(profile);
            } else {
                profileMapper.updateById(profile);
            }
        } catch (Exception e) {
            // 画像更新失败绝不影响主对话链路
            log.warn("用户画像自动更新失败 userId={}: {}", userId, e.getMessage());
        }
    }

    // ===================== 内部工具 =====================

    /** 按 24 小时落时段桶。 */
    static String hourToSlot(int hour) {
        if (hour >= 0 && hour < 6) {
            return SLOT_NIGHT;
        } else if (hour < 12) {
            return SLOT_MORNING;
        } else if (hour < 18) {
            return SLOT_AFTERNOON;
        }
        return SLOT_EVENING;
    }

    /**
     * 离线话题抽取：按标点/空白切句，保留 2~10 字的片段作为候选话题。
     * 不依赖分词库，足够简单版画像使用。
     */
    static List<String> extractTopics(String message) {
        List<String> result = new ArrayList<>();
        if (message == null || message.isBlank()) {
            return result;
        }
        String[] parts = message.split("[，。！？!?,.;；：:、\\s\\n\\r]+");
        Set<String> dedup = new LinkedHashSet<>();
        for (String p : parts) {
            if (p == null) {
                continue;
            }
            String seg = p.trim();
            if (seg.length() >= 2 && seg.length() <= 10) {
                dedup.add(seg);
            }
        }
        result.addAll(dedup);
        return result;
    }

    private List<String> readTopics(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Map<String, Integer> readSlots(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new java.util.HashMap<>();
            }
            return objectMapper.readValue(json, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            return new java.util.HashMap<>();
        }
    }

    private String toJsonList(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list == null ? List.of() : list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String toJsonMap(Map<String, Integer> map) {
        try {
            return objectMapper.writeValueAsString(map == null ? Map.of() : map);
        } catch (Exception e) {
            return "{}";
        }
    }
}
