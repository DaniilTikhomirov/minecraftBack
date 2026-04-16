package com.back.minecraftback.gameserver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Текстовый канал: только сервер → клиент (плагин не шлёт команды выдачи предметов сюда).
 */
@Slf4j
@Component
public class GamePaymentWebSocketHandler extends TextWebSocketHandler {

    private static final int MAX_SESSIONS = 32;

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        if (sessions.size() >= MAX_SESSIONS) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("session limit"));
            log.warn("[game-ws] rejected connection: session limit {}", MAX_SESSIONS);
            return;
        }
        sessions.add(session);
        var attrs = session.getAttributes();
        log.info(
                "[game-ws] connected id={} uri={} remoteSocket={} directPeer={} xForwardedFor={} xRealIp={} host={}",
                session.getId(),
                session.getUri(),
                session.getRemoteAddress(),
                attrs.get(GameServerWebSocketHandshakeInterceptor.ATTR_DIRECT_REMOTE_ADDR),
                attrs.getOrDefault(GameServerWebSocketHandshakeInterceptor.ATTR_X_FORWARDED_FOR, "-"),
                attrs.getOrDefault(GameServerWebSocketHandshakeInterceptor.ATTR_X_REAL_IP, "-"),
                attrs.getOrDefault(GameServerWebSocketHandshakeInterceptor.ATTR_HOST, "-")
        );
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        sessions.remove(session);
        log.info("[game-ws] closed id={} status={}", session.getId(), status);
    }

    /**
     * Рассылка подписанного JSON всем открытым сессиям игрового сервера.
     */
    public void broadcastSignedJson(String json) {
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                sessions.remove(session);
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(json));
                }
            } catch (IOException e) {
                log.warn("[game-ws] send failed id={}", session.getId(), e);
                try {
                    session.close(CloseStatus.SERVER_ERROR);
                } catch (IOException ignored) {
                    // ignore
                }
                sessions.remove(session);
            }
        }
    }

    public int getOpenSessionCount() {
        sessions.removeIf(s -> !s.isOpen());
        return sessions.size();
    }

    /** Канал только на исходящие события; произвольный текст от клиента — закрытие (защита от «команд» в сокет). */
    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        if (message.getPayloadLength() > 0) {
            log.warn("[game-ws] unexpected client message, closing session {}", session.getId());
            session.close(CloseStatus.POLICY_VIOLATION.withReason("push-only channel"));
        }
    }
}
