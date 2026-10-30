package com.example.aichat.service;

/**
 * 视觉理解服务（M4 ⑫）：调用本地 Ollama llava 多模态模型对图片生成描述。
 *
 * <p>配置开关 {@code aichat.vision.enabled}（默认 false）保持离线基线：
 * 关闭时接口仍可达，但返回"未启用"提示，不发起 Ollama 调用。</p>
 */
public interface VisionService {

    /** 视觉理解是否启用。 */
    boolean isEnabled();

    /**
     * 分析图片并生成描述。
     *
     * @param imageBytes 图片二进制（jpg/png/webp）
     * @param filename   原始文件名（用于日志/校验）
     * @param prompt     可选提示词，为空时使用默认中文描述提示
     * @return 分析结果（enabled/success/description/message）
     */
    VisionAnalysis analyze(byte[] imageBytes, String filename, String prompt);

    /** 视觉分析结果。 */
    record VisionAnalysis(boolean enabled, boolean success, String description, String message) {
    }
}
