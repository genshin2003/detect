package com.example.Kcsj.handler;

import com.alibaba.fastjson.JSON;
import com.example.Kcsj.common.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {
    // userId -> WebSocketSession
    private static final ConcurrentHashMap<Integer, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri().getQuery();
        String token = null;
        if (query != null && query.startsWith("token=")) {
            token = query.substring(6);
        }
        if (token == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }
        try {
            io.jsonwebtoken.Claims claims = JwtUtils.parseToken(token);
            if (claims == null) {
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }
            Integer userId = claims.get("userId", Integer.class);
            if (userId == null) {
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }
            sessions.put(userId, session);
            log.info("WebSocket连接建立: userId={}", userId);
        } catch (Exception e) {
            log.error("WebSocket Token验证失败", e);
            session.close(CloseStatus.NOT_ACCEPTABLE);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
        log.info("WebSocket连接关闭");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 处理客户端消息（如标记已读）
        log.info("收到WebSocket消息: {}", message.getPayload());
        try {
            Map<String, Object> data = JSON.parseObject(message.getPayload(), Map.class);
            String type = (String) data.get("type");
            if ("markRead".equals(type)) {
                // 标记已读消息，可以在这里处理或转发给MessageService
                log.info("标记消息已读: {}", data.get("messageId"));
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
        }
    }

    // 广播给所有在线用户
    public void broadcastToAll(Object data) {
        sessions.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(JSON.toJSONString(data)));
                } catch (IOException e) {
                    log.error("WebSocket广播消息失败: userId={}", userId, e);
                }
            }
        });
    }

    public void sendToUser(Integer userId, Object data) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(JSON.toJSONString(data)));
            } catch (IOException e) {
                log.error("WebSocket发送消息失败: userId={}", userId, e);
            }
        }
    }

    public boolean isUserOnline(Integer userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }
}
