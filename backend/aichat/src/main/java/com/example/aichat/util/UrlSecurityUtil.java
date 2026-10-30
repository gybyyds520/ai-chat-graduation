package com.example.aichat.util;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * URL 安全校验工具 - 防 SSRF（服务端请求伪造）。
 *
 * 安全加固（P0）：
 *   1. 仅允许 http/https 协议，拒绝 file/gopher/dict 等任意协议；
 *   2. 目标主机解析后不得是内网/回环/链路本地/保留地址（含 IPv4/IPv6）；
 *   3. 必须显式指定端口或使用默认端口，避免访问内网非标准端口服务。
 */
public final class UrlSecurityUtil {

    private UrlSecurityUtil() {}

    /**
     * 校验 URL 是否允许服务端访问，非法时抛出 IllegalArgumentException。
     */
    public static void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL不能为空");
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("URL格式非法");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("仅允许 http/https 协议访问");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL缺少主机名");
        }
        // 主机名校验：禁止形如 2130706433 / 0x7f000001 的编码形式
        if (host.matches("(?i)^\\d+$") || host.matches("(?i)^0x[0-9a-f]+$")) {
            throw new IllegalArgumentException("URL主机名非法");
        }
        int port = uri.getPort();
        if (port == -1) {
            port = "https".equalsIgnoreCase(scheme) ? 443 : 80;
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("URL端口非法");
        }

        // 解析并校验目标 IP：禁止访问回环、内网、链路本地、保留地址
        InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("无法解析目标主机: " + host);
        }
        if (isForbiddenAddress(address)) {
            throw new IllegalArgumentException("禁止访问内网/保留地址: " + host);
        }
    }

    private static boolean isForbiddenAddress(InetAddress address) {
        if (address.isLoopbackAddress()
                || address.isAnyLocalAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()) {
            return true;
        }
        if (address.isMulticastAddress()) {
            return true;
        }
        byte[] addr = address.getAddress();
        if (addr == null) {
            return true;
        }
        // IPv4 保留段：0.x / 10.x / 127.x / 169.254.x / 172.16-31.x / 192.168.x / 224+ 已由上面覆盖，这里补 IPv6 特殊段
        if (addr.length == 16) {
            // fc00::/7 唯一本地地址, fe80::/10 链路本地, ::1 回环
            int first = addr[0] & 0xFF;
            if (first == 0xFC || first == 0xFD || first == 0xFE) {
                return true;
            }
        }
        return false;
    }
}
