package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.service.ai.SpeechService;
import com.example.aichat.service.voice.VoiceProfile;
import com.example.aichat.service.voice.adapter.EdgeTtsAdapter;
import com.example.aichat.service.voice.adapter.TtsAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 语音对话控制器
 * 提供STT(语音转文本)和TTS(文本转语音)API
 */
@RestController
@RequestMapping("/api/voice")
public class VoiceChatController {

    private static final Logger logger = LoggerFactory.getLogger(VoiceChatController.class);

    @Autowired
    private SpeechService speechService;

    @Autowired(required = false)
    private List<TtsAdapter> ttsAdapters;

    @Autowired
    private com.example.aichat.service.QuotaService quotaService;

    /**
     * 语音转文本 (STT)
     * POST /api/voice/stt
     * 上传音频文件，返回识别出的文本
     */
    @PostMapping("/stt")
    public R speechToText(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.badRequest("音频文件不能为空");
        }

        // 限制文件大小（10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            return R.badRequest("音频文件不能超过10MB");
        }

        logger.info("收到语音识别请求, 文件名={}, 大小={}bytes", file.getOriginalFilename(), file.getSize());

        try {
            byte[] audioData = file.getBytes();
            String text = speechService.speechToText(audioData, file.getOriginalFilename());

            if (text != null && !text.isEmpty()) {
                return R.success(Map.of("text", text));
            } else {
                return R.error("语音识别失败，请检查服务配置或重试");
            }
        } catch (IOException e) {
            logger.error("读取音频文件失败", e);
            return R.error("读取音频文件失败");
        }
    }

    /**
     * 文本转语音 (TTS)
     * POST /api/voice/tts
     * 输入文本，返回音频；可选 voice 参数指定音色
     */
    @PostMapping("/tts")
    public ResponseEntity<byte[]> textToSpeech(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        // 健壮性加固：限制合成文本长度，防止超长文本造成资源耗尽
        if (text.length() > 2000) {
            return ResponseEntity.badRequest().build();
        }
        String voice = request.get("voice");

        // M3-A5：已登录用户在 TTS 合成前校验 voice_call 每日额度；匿名调用不计数。
        if (quotaService != null) {
            try {
                Long uid = com.example.aichat.common.security.AuthContext.getCurrentUserId().orElse(null);
                if (uid != null) {
                    quotaService.checkAndConsume(uid, "voice_call");
                }
            } catch (com.example.aichat.common.exception.BusinessException be) {
                logger.warn("TTS 配额拦截: {}", be.getMessage());
                return ResponseEntity.status(403).body(be.getMessage().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        }

        logger.info("收到语音合成请求, 文本长度={}, voice={}", text.length(), voice);

        // 优先使用支持音色参数的 TTS 适配器（EdgeTtsAdapter / MockTtsAdapter）
        byte[] audio = synthesizeWithVoice(text.trim(), voice);
        if (audio != null && audio.length > 0) {
            String format = detectAudioFormat(voice);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("audio/" + format));
            headers.setContentLength(audio.length);
            return ResponseEntity.ok().headers(headers).body(audio);
        }

        // 降级到现有 SpeechService
        audio = speechService.textToSpeech(text.trim());
        if (audio != null && audio.length > 0) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("audio/mpeg"));
            headers.setContentLength(audio.length);
            return ResponseEntity.ok().headers(headers).body(audio);
        }

        return ResponseEntity.status(503).build();
    }

    /**
     * 从已注册的 TTS 适配器中挑选支持音色的一个来合成。
     * EdgeTtsAdapter 优先（Java 枚举多音色），其次其它多音色适配器。
     */
    private byte[] synthesizeWithVoice(String text, String voice) {
        if (ttsAdapters == null || ttsAdapters.isEmpty()) {
            return null;
        }
        // 优先 EdgeTtsAdapter
        for (TtsAdapter a : ttsAdapters) {
            if (a instanceof EdgeTtsAdapter && a.isAvailable()) {
                return a.synthesize(text, voice);
            }
        }
        // 其次其它列出了音色的适配器
        for (TtsAdapter a : ttsAdapters) {
            if (a.isAvailable() && !a.getAvailableVoices().isEmpty()) {
                return a.synthesize(text, voice);
            }
        }
        return null;
    }

    private String detectAudioFormat(String voice) {
        // Edge/Mock TTS 返回 wav；SpeechService 降级返回 mpeg（由调用方处理）
        return "wav";
    }

    /**
     * 列出所有可选音色。
     * GET /api/voice/voices
     */
    @GetMapping("/voices")
    public R listVoices() {
        List<Map<String, Object>> list = new ArrayList<>();
        // 优先收集 EdgeTtsAdapter 支持的音色
        List<VoiceProfile> profiles = null;
        if (ttsAdapters != null) {
            for (TtsAdapter a : ttsAdapters) {
                if (a.isAvailable() && !a.getAvailableVoices().isEmpty()) {
                    profiles = a.getAvailableVoices();
                    break;
                }
            }
        }
        if (profiles == null || profiles.isEmpty()) {
            profiles = List.of(VoiceProfile.values());
        }
        for (VoiceProfile p : profiles) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("voiceId", p.getVoiceId());
            item.put("name", p.getName());
            item.put("description", p.getDescription());
            item.put("scene", p.getScene());
            list.add(item);
        }
        return R.success(Map.of("voices", list, "total", list.size()));
    }

    /**
     * 检查语音服务状态
     * GET /api/voice/health
     */
    @GetMapping("/health")
    public R checkHealth() {
        return R.success(Map.of(
                "sttAvailable", speechService.isSttAvailable(),
                "ttsAvailable", speechService.isTtsAvailable()
        ));
    }
}
