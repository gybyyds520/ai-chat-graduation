package com.example.aichat.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * 语音服务 - STT(语音转文本) + TTS(文本转语音)
 * STT支持OpenAI Whisper API；TTS支持Google Translate(免费)和OpenAI TTS(云端)
 */
@Service
public class SpeechService {

    private static final Logger logger = LoggerFactory.getLogger(SpeechService.class);

    /** 外部语音服务调用超时，防止外部网络不可达时无限阻塞请求线程 */
    private static final Duration EXTERNAL_TIMEOUT = Duration.ofSeconds(8);

    private final WebClient webClient;

    @Value("${voice.stt.provider:openai-whisper}")
    private String sttProvider;

    @Value("${voice.stt.openai-api-key:}")
    private String sttApiKey;

    @Value("${voice.stt.model:whisper-1}")
    private String whisperModel;

    @Value("${voice.tts.provider:google-translate}")
    private String ttsProvider;

    @Value("${voice.tts.openai-api-key:}")
    private String ttsApiKey;

    @Value("${voice.tts.default-voice:alloy}")
    private String defaultVoice;

    @Value("${voice.tts.default-lang:zh-CN}")
    private String defaultLang;

    public SpeechService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    // ==================== STT 语音转文本 ====================

    /**
     * 语音识别 - 将音频转为文本
     * @param audioData 音频字节数据（WebM/WAV/MP3格式）
     * @param fileName 文件名（含扩展名，用于格式识别）
     * @return 识别出的文本，失败返回null
     */
    public String speechToText(byte[] audioData, String fileName) {
        if ("disabled".equals(sttProvider)) {
            logger.warn("STT服务未启用");
            return null;
        }

        if (sttApiKey == null || sttApiKey.isEmpty()) {
            logger.warn("STT API Key未配置，无法进行语音识别");
            return null;
        }

        logger.info("开始语音识别, 文件名={}, 数据大小={}bytes", fileName, audioData.length);

        try {
            // 构建multipart请求体
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            ByteArrayResource fileResource = new ByteArrayResource(audioData) {
                @Override
                public String getFilename() {
                    return fileName != null ? fileName : "audio.webm";
                }
            };
            builder.part("file", fileResource, MediaType.APPLICATION_OCTET_STREAM);
            builder.part("model", whisperModel);

            // 调用OpenAI Whisper API
            String result = webClient.post()
                    .uri("https://api.openai.com/v1/audio/transcriptions")
                    .header("Authorization", "Bearer " + sttApiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(response -> (String) response.get("text"))
                    .timeout(EXTERNAL_TIMEOUT)
                    .block();

            logger.info("语音识别成功, 文本长度={}", result != null ? result.length() : 0);
            return result;
        } catch (Exception e) {
            logger.error("语音识别失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== TTS 文本转语音 ====================

    /**
     * 文本转语音 - 将文本转为音频
     * @param text 要转换的文本
     * @return 音频字节数据（MP3格式），失败返回null
     */
    public byte[] textToSpeech(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        if ("disabled".equals(ttsProvider)) {
            logger.warn("TTS服务未启用");
            return null;
        }

        // 限制文本长度，防止超长请求
        String truncatedText = text.length() > 500 ? text.substring(0, 500) : text;

        if ("openai-tts".equals(ttsProvider)) {
            return textToSpeechWithOpenAI(truncatedText);
        } else {
            return textToSpeechWithGoogleTranslate(truncatedText);
        }
    }

    /**
     * 使用Google Translate TTS（免费，无需API Key）
     * 注意：这是非官方接口，文本需URL编码，单次最多约200字符
     */
    private byte[] textToSpeechWithGoogleTranslate(String text) {
        try {
            // Google Translate TTS单次限制约200字符，需分段
            String chunk = text.length() > 200 ? text.substring(0, 200) : text;
            String encodedText = URLEncoder.encode(chunk, StandardCharsets.UTF_8);

            String url = String.format(
                    "https://translate.google.com/translate_tts?ie=UTF-8&tl=%s&client=tw-ob&q=%s",
                    defaultLang, encodedText
            );

            byte[] audio = webClient.get()
                    .uri(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(EXTERNAL_TIMEOUT)
                    .block();

            logger.info("Google TTS生成成功, 文本长度={}, 音频大小={}bytes", chunk.length(), audio != null ? audio.length : 0);
            return audio;
        } catch (Exception e) {
            logger.error("Google TTS生成失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 使用OpenAI TTS API（需API Key）
     */
    private byte[] textToSpeechWithOpenAI(String text) {
        if (ttsApiKey == null || ttsApiKey.isEmpty()) {
            logger.warn("OpenAI TTS API Key未配置");
            return null;
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", "tts-1",
                    "input", text,
                    "voice", defaultVoice,
                    "response_format", "mp3"
            );

            byte[] audio = webClient.post()
                    .uri("https://api.openai.com/v1/audio/speech")
                    .header("Authorization", "Bearer " + ttsApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(EXTERNAL_TIMEOUT)
                    .block();

            logger.info("OpenAI TTS生成成功, 文本长度={}, 音频大小={}bytes", text.length(), audio != null ? audio.length : 0);
            return audio;
        } catch (Exception e) {
            logger.error("OpenAI TTS生成失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 服务状态检查 ====================

    /**
     * 检查STT服务是否可用
     */
    public boolean isSttAvailable() {
        if ("disabled".equals(sttProvider)) return false;
        return sttApiKey != null && !sttApiKey.isEmpty();
    }

    /**
     * 检查TTS服务是否可用
     */
    public boolean isTtsAvailable() {
        if ("disabled".equals(ttsProvider)) return false;
        if ("openai-tts".equals(ttsProvider)) {
            return ttsApiKey != null && !ttsApiKey.isEmpty();
        }
        // Google Translate TTS无需Key，始终可用
        return true;
    }
}
