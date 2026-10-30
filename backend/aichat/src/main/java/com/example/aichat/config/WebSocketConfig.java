package com.example.aichat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.example.aichat.websocket.ChatWebSocketHandler;
import com.example.aichat.websocket.DataSyncWebSocketHandler;
import com.example.aichat.websocket.WebRtcSignalingController;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final DataSyncWebSocketHandler dataSyncWebSocketHandler;
    private final WebRtcSignalingController webRtcSignalingController;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler,
                           DataSyncWebSocketHandler dataSyncWebSocketHandler,
                           WebRtcSignalingController webRtcSignalingController) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.dataSyncWebSocketHandler = dataSyncWebSocketHandler;
        this.webRtcSignalingController = webRtcSignalingController;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOriginPatterns("http://localhost:*", "file://*");

        registry.addHandler(dataSyncWebSocketHandler, "/ws/data-sync")
                .setAllowedOriginPatterns("http://localhost:*", "file://*");

        // WebRTC 全双工语音信令（offer/answer/ICE 中转；媒体走 P2P 不经服务器）
        registry.addHandler(webRtcSignalingController, "/ws/voice/signaling")
                .setAllowedOriginPatterns("http://localhost:*", "file://*");
    }
}
