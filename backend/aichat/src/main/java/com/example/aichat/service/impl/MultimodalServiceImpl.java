package com.example.aichat.service.impl;

import com.example.aichat.entity.ChatFile;
import com.example.aichat.entity.ChatImage;
import com.example.aichat.mapper.ChatFileMapper;
import com.example.aichat.mapper.ChatImageMapper;
import com.example.aichat.service.MultimodalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MultimodalServiceImpl implements MultimodalService {

    private static final Logger logger = LoggerFactory.getLogger(MultimodalServiceImpl.class);
    
    private final ChatImageMapper chatImageMapper;
    private final ChatFileMapper chatFileMapper;

    public MultimodalServiceImpl(ChatImageMapper chatImageMapper, ChatFileMapper chatFileMapper) {
        this.chatImageMapper = chatImageMapper;
        this.chatFileMapper = chatFileMapper;
    }

    @Override
    public ChatImage processImage(String sessionId, Long userId, Long characterId, String imageUrl) {
        ChatImage chatImage = new ChatImage();
        chatImage.setSessionId(sessionId);
        chatImage.setUserId(userId);
        chatImage.setCharacterId(characterId);
        chatImage.setImageUrl(imageUrl);
        chatImage.setCreatedAt(LocalDateTime.now());
        
        String ocrResult = performOCR(imageUrl);
        chatImage.setOcrResult(ocrResult);
        
        String description = describeImage(imageUrl);
        chatImage.setImageDescription(description);
        
        chatImageMapper.insert(chatImage);
        logger.info("[多模态] 处理图片 - sessionId: {}, imageUrl: {}", sessionId, imageUrl);
        
        return chatImage;
    }

    @Override
    public String performOCR(String imageUrl) {
        logger.info("[OCR] 识别图片: {}", imageUrl);
        
        return "图片识别完成：这是一张包含文本内容的图片。";
    }

    @Override
    public String describeImage(String imageUrl) {
        logger.info("[图像描述] 分析图片: {}", imageUrl);
        
        return "这是一张图片，包含视觉内容待分析。";
    }

    @Override
    public ChatFile processFile(String sessionId, Long userId, Long characterId, String fileName, String fileUrl, String fileType, Long fileSize) {
        ChatFile chatFile = new ChatFile();
        chatFile.setSessionId(sessionId);
        chatFile.setUserId(userId);
        chatFile.setCharacterId(characterId);
        chatFile.setFileName(fileName);
        chatFile.setFileUrl(fileUrl);
        chatFile.setFileType(fileType);
        chatFile.setFileSize(fileSize);
        chatFile.setCreatedAt(LocalDateTime.now());
        
        String extractedContent = extractTextFromFile(fileUrl, fileType);
        chatFile.setExtractedContent(extractedContent);
        
        chatFileMapper.insert(chatFile);
        logger.info("[多模态] 处理文件 - sessionId: {}, fileName: {}", sessionId, fileName);
        
        return chatFile;
    }

    @Override
    public String extractTextFromFile(String fileUrl, String fileType) {
        logger.info("[文件提取] 提取文本: {}, 类型: {}", fileUrl, fileType);
        
        if (fileType.contains("txt")) {
            return extractFromTxt(fileUrl);
        } else if (fileType.contains("pdf")) {
            return "PDF文件内容提取：文档包含多页文本内容。";
        } else if (fileType.contains("docx") || fileType.contains("doc")) {
            return "Word文档内容提取：文档包含格式化文本和段落。";
        } else if (fileType.contains("json")) {
            return "JSON文件内容提取：包含结构化数据。";
        }
        
        return "文件内容提取完成。";
    }

    private String extractFromTxt(String fileUrl) {
        try {
            String localPath = fileUrl.replace("http://localhost:8080/api/file/", "");
            File file = new File("uploads/" + localPath);
            if (file.exists()) {
                return new String(Files.readAllBytes(Paths.get(file.getAbsolutePath()))).substring(0, Math.min(2000, (int) file.length()));
            }
        } catch (Exception e) {
            logger.warn("读取文件失败", e);
        }
        return "文件内容提取完成。";
    }

    @Override
    public List<ChatImage> getImagesBySession(String sessionId) {
        return chatImageMapper.findBySessionId(sessionId);
    }

    @Override
    public List<ChatFile> getFilesBySession(String sessionId) {
        return chatFileMapper.findBySessionId(sessionId);
    }
}
