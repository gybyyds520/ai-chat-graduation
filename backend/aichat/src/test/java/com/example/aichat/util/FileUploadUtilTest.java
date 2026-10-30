package com.example.aichat.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileUploadUtil 单元测试。
 *
 * 覆盖点：
 *   1. 合法扩展名（png）应上传成功
 *   2. 合法扩展名（jpg）应上传成功
 *   3. 非法扩展名（exe）应抛出异常
 *   4. 非法扩展名（html）应抛出异常
 *   5. 无扩展名应抛出异常
 *   6. 空文件应抛出异常
 *   7. null 文件应抛出异常
 *   8. 路径穿越文件名（../evil.png）应被净化
 *   9. toUrl 应返回正确的URL路径
 */
class FileUploadUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void save_合法png文件应上传成功() throws IOException {
        // 临时设置 user.dir 到测试目录，避免污染项目目录
        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
        try {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.png", "image/png", "fake-image-data".getBytes());

            File result = FileUploadUtil.save(file, "avatars");

            assertNotNull(result);
            assertTrue(result.exists());
            assertTrue(result.getName().endsWith(".png"));
            assertTrue(result.getAbsolutePath().contains("avatars"));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    void save_合法jpg文件应上传成功() throws IOException {
        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
        try {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", "fake-jpg-data".getBytes());

            File result = FileUploadUtil.save(file, "chat");

            assertNotNull(result);
            assertTrue(result.exists());
            assertTrue(result.getName().endsWith(".jpg"));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    void save_非法扩展名exe应抛出异常() {
        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
        try {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "malware.exe", "application/octet-stream", "fake-exe".getBytes());

            IOException ex = assertThrows(IOException.class,
                    () -> FileUploadUtil.save(file, "uploads"));
            assertTrue(ex.getMessage().contains("不支持的文件类型"));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    void save_非法扩展名html应抛出异常() {
        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
        try {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "evil.html", "text/html", "<script>alert(1)</script>".getBytes());

            IOException ex = assertThrows(IOException.class,
                    () -> FileUploadUtil.save(file, "uploads"));
            assertTrue(ex.getMessage().contains("不支持的文件类型"));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    void save_无扩展名应抛出异常() {
        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
        try {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "noextension", "application/octet-stream", "data".getBytes());

            IOException ex = assertThrows(IOException.class,
                    () -> FileUploadUtil.save(file, "uploads"));
            assertTrue(ex.getMessage().contains("不支持的文件类型"));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    void save_空文件应抛出异常() {
        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
        try {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "empty.png", "image/png", new byte[0]);

            IOException ex = assertThrows(IOException.class,
                    () -> FileUploadUtil.save(file, "uploads"));
            assertTrue(ex.getMessage().contains("上传文件为空"));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    void save_null文件应抛出异常() {
        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
        try {
            IOException ex = assertThrows(IOException.class,
                    () -> FileUploadUtil.save(null, "uploads"));
            assertTrue(ex.getMessage().contains("上传文件为空"));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    void save_路径穿越文件名应被净化() {
        // 文件名包含路径穿越，但扩展名提取逻辑会去除路径部分
        // 因此即使文件名是 "../../evil.png"，扩展名提取仍只取 .png 部分
        // 保存时使用 UUID 重命名，不会保留原始路径
        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
        try {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "../../etc/passwd.png", "image/png", "fake-data".getBytes());

            File result = FileUploadUtil.save(file, "avatars");

            assertNotNull(result);
            // 保存的文件名应该是 UUID 生成的，不含路径穿越
            assertFalse(result.getName().contains(".."));
            assertTrue(result.getName().endsWith(".png"));
            // 文件必须在 uploads 目录内
            String canonicalRoot = new File(tempDir.toString(), "uploads").getAbsolutePath();
            assertTrue(result.getAbsolutePath().startsWith(canonicalRoot),
                    "文件路径必须在uploads目录内，实际: " + result.getAbsolutePath());
        } catch (IOException e) {
            fail("合法png扩展名不应抛出异常: " + e.getMessage());
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    void toUrl_应返回正确URL路径() {
        File mockFile = new File("/tmp/abc-123.png");

        String url = FileUploadUtil.toUrl("chat", mockFile);

        assertEquals("/uploads/chat/abc-123.png", url);
    }
}
