package com.example.aichat.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传工具类
 * 统一处理上传路径，解决相对路径基于 Tomcat 临时工作目录导致 FileNotFoundException 的问题。
 * 所有上传文件保存到项目根目录下的 uploads/ 子目录。
 *
 * 安全加固（P0）：
 *   1. 扩展名白名单校验：仅允许图片/文档/音视频等安全类型，拒绝 html/svg/jsp/php 等可执行或可脚本化类型；
 *   2. 扩展名净化：丢弃所有路径分隔符与控制字符，防止 "../" 路径穿越写入任意目录；
 *   3. 目标路径二次校验：保存前确认最终路径仍在 uploads/<subDir> 内。
 */
public final class FileUploadUtil {

    private FileUploadUtil() {}

    /**
     * 允许上传的安全扩展名（小写）。可脚本化的 html/svg/xml/jsp/php 等一律拒绝，
     * 防止上传文件被同源访问时形成存储型 XSS。
     */
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            // 图片
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "ico",
            // 文档
            "txt", "json", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "md",
            // 音频
            "mp3", "wav", "ogg", "m4a", "amr", "aac",
            // 视频
            "mp4", "mov", "avi", "webm",
            // 压缩包
            "zip", "rar", "7z"
    ));

    /**
     * 保存上传文件到指定子目录。
     *
     * @param file      上传的文件
     * @param subDir    uploads 下的子目录名（如 "chat", "avatars", "character"）
     * @return 保存后的文件对象
     * @throws IOException 扩展名非法或保存失败
     */
    public static File save(MultipartFile file, String subDir) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("上传文件为空");
        }

        String originalName = file.getOriginalFilename();
        String extension = extractSafeExtension(originalName);

        // 扩展名白名单校验（可脚本化类型直接拒绝）
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IOException("不支持的文件类型: " + (extension.isEmpty() ? "无扩展名" : "." + extension));
        }

        String newFileName = UUID.randomUUID().toString() + "." + extension;

        // 使用项目工作目录的绝对路径，避免 Tomcat 临时目录问题
        String uploadDir = System.getProperty("user.dir")
                + File.separator + "uploads"
                + File.separator + sanitizeSubDir(subDir)
                + File.separator;
        File dir = new File(uploadDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("无法创建上传目录: " + uploadDir);
        }

        File dest = new File(uploadDir, newFileName);

        // 安全加固：解析后的规范路径必须仍位于上传根目录内，防止任何路径穿越
        String canonicalUploadRoot = new File(System.getProperty("user.dir"), "uploads").getCanonicalPath();
        String canonicalDest = dest.getCanonicalPath();
        if (!canonicalDest.startsWith(canonicalUploadRoot + File.separator)) {
            throw new IOException("非法的文件保存路径");
        }

        file.transferTo(dest.getAbsoluteFile());
        return dest;
    }

    /**
     * 从原始文件名提取安全扩展名：
     *   - 仅保留最后一个 '.' 之后的小写字母数字，长度 ≤ 10；
     *   - 移除路径分隔符、控制字符等，防止 "photo.png/../../evil" 这类穿越载荷；
     *   - 无合法扩展名返回空字符串。
     */
    private static String extractSafeExtension(String originalName) {
        if (originalName == null || originalName.isEmpty()) {
            return "";
        }
        // 去掉路径与查询串，只取文件名部分
        String name = originalName.replace('\\', '/');
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        String ext = name.substring(dot + 1)
                .replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);
        if (ext.length() > 10) {
            return "";
        }
        return ext;
    }

    /**
     * 子目录路径净化：允许字母数字与下划线/连字符，保留多级子目录（如 social/images），
     * 拒绝 ".." 等路径穿越段。返回净化后的相对子路径。
     */
    private static String sanitizeSubDir(String subDir) {
        if (subDir == null || subDir.isEmpty()) {
            return "misc";
        }
        String[] segments = subDir.replace('\\', '/').split("/");
        StringBuilder sb = new StringBuilder();
        for (String segment : segments) {
            String cleaned = segment.replaceAll("[^A-Za-z0-9_-]", "");
            if (cleaned.isEmpty() || ".".equals(cleaned) || "..".equals(cleaned)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(File.separator);
            }
            sb.append(cleaned);
        }
        return sb.length() == 0 ? "misc" : sb.toString();
    }

    /**
     * 返回可访问的相对 URL 路径（如 /uploads/chat/xxx.txt）。
     */
    public static String toUrl(String subDir, File savedFile) {
        return "/uploads/" + sanitizeSubDir(subDir) + "/" + savedFile.getName();
    }
}
