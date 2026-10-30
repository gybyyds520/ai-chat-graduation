package com.example.aichat.service.voice.adapter;

import com.example.aichat.service.voice.VoiceProfile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Mock 语音合成适配器。
 *
 * 生成一个"最小有效 WAV"：合法的 RIFF/WAVE 头 + 一小段静音 PCM 数据。
 * 为支持多音色演示，不同 voiceId 使用不同采样率，并在 PCM 数据中嵌入随音色变化的
 * 采样值，因此不同 voiceId 返回的字节数组可被明确区分（测试可断言）。
 */
@Component
public class MockTtsAdapter implements TtsAdapter {

    @Override
    public byte[] synthesize(String text) {
        return synthesize(text, null);
    }

    @Override
    public byte[] synthesize(String text, String voiceId) {
        VoiceProfile profile = VoiceProfile.fromId(voiceId);
        int sampleRate = profile.getSampleRate();

        // 用音色序号生成一个非零的小采样值，确保不同音色字节内容不同
        short marker = (short) (profile.ordinal() * 1000 + 1000);
        int sampleCount = 4; // 4 个采样，16bit 单声道 -> 8 字节 PCM
        byte[] pcm = new byte[sampleCount * 2];
        ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)
                .putShort(marker).putShort(marker)
                .putShort(marker).putShort(marker);

        return buildWav(sampleRate, pcm);
    }

    /** 构造最小合法 WAV（单声道、16bit PCM）。 */
    private byte[] buildWav(int sampleRate, byte[] pcm) {
        int channels = 1;
        int bitsPerSample = 16;
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        // WAV 头共 44 字节
        int dataLen = pcm.length;
        int fileSizeMinus8 = 36 + dataLen;

        ByteArrayOutputStream out = new ByteArrayOutputStream(44 + dataLen);
        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        header.putInt(fileSizeMinus8);
        header.put("WAVE".getBytes(StandardCharsets.US_ASCII));
        header.put("fmt ".getBytes(StandardCharsets.US_ASCII));
        header.putInt(16);          // fmt chunk size
        header.putShort((short) 1); // PCM
        header.putShort((short) channels);
        header.putInt(sampleRate);
        header.putInt(byteRate);
        header.putShort((short) blockAlign);
        header.putShort((short) bitsPerSample);
        header.put("data".getBytes(StandardCharsets.US_ASCII));
        header.putInt(dataLen);
        out.write(header.array(), 0, header.array().length);
        out.write(pcm, 0, pcm.length);
        return out.toByteArray();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getName() {
        return "mock-tts";
    }

    @Override
    public String getAudioFormat() {
        return "wav";
    }

    @Override
    public List<VoiceProfile> getAvailableVoices() {
        return Arrays.asList(VoiceProfile.values());
    }
}
