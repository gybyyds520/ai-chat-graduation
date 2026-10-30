package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.entity.AiCharacter;
import com.example.aichat.service.CharacterGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/character")
public class CharacterGeneratorController {

    @Autowired
    private CharacterGeneratorService characterGeneratorService;

    @PostMapping("/generate")
    public R generateCharacter(@RequestBody Map<String, String> request) {
        String theme = request.getOrDefault("theme", "有趣的角色");
        AiCharacter character = characterGeneratorService.generateCharacter(theme);
        if (character != null) {
            return R.success(character);
        }
        return R.error("生成角色失败");
    }

    @PostMapping("/generate/batch")
    public R generateMultipleCharacters(@RequestBody Map<String, Object> request) {
        String theme = (String) request.getOrDefault("theme", "有趣的角色");
        
        // 安全地获取count参数，避免NullPointerException
        int count = 3;
        Object countObj = request.get("count");
        if (countObj != null) {
            try {
                count = ((Number) countObj).intValue();
            } catch (ClassCastException | NumberFormatException e) {
                return R.error("参数错误：数量格式不正确");
            }
        }
        
        // 限制数量范围
        if (count < 1) count = 1;
        if (count > 10) count = 10;
        
        List<AiCharacter> characters = characterGeneratorService.generateMultipleCharacters(theme, count);
        return R.success(characters);
    }
}