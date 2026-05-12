package com.back.minecraftback.gameserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
 * Канал к игровому серверу: push оплат (подписанный JSON) и RPC-ответы валидации ({@code VALIDATION_RESPONSE}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GamePaymentWebSocketHandler extends TextWebSocketHandler {

    static final String TYPE_VALIDATION_RESPONSE = "VALIDATION_RESPONSE";

    private static final int MAX_SESSIONS = 32;

    private final ObjectMapper objectMapper;
    private final GameServerWsRpcAwaiter rpcAwaiter;

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
                    log.info("📤 [WS] Отправлено: [session={}] {}", session.getId(), json);
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

    /**
     * Один RPC-запрос на первую открытую сессию (один плагин).
     */
    public void sendValidationRequestToFirstSession(String json) throws IOException {
        WebSocketSession target = null;
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                target = session;
                break;
            }
        }
        if (target == null) {
            throw new IOException("no open game server WebSocket session");
        }
        synchronized (target) {
            log.info("📤 [WS] Отправлено: [session={}] {}", target.getId(), json);
            target.sendMessage(new TextMessage(json));
        }
    }

    public int getOpenSessionCount() {
        sessions.removeIf(s -> !s.isOpen());
        return sessions.size();
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        if (message.getPayloadLength() == 0) {
            log.info("📩 [WS] Получено: [session={}] (empty)", session.getId());
            return;
        }
        String payload = message.getPayload();
        log.info("📩 [WS] Получено: [session={}] {}", session.getId(), payload);
        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception e) {
            log.warn("[game-ws] non-json client message session={}", session.getId());
            return;
        }
        if (root == null || !root.isObject()) {
            return;
        }
        String type = root.path("type").asText("");
        if (TYPE_VALIDATION_RESPONSE.equals(type)) {
            String requestId = root.path("requestId").asText("");
            if (!requestId.isBlank()) {
                rpcAwaiter.complete(requestId, root);
            }
            return;
        }
        log.warn("[game-ws] unexpected client message type={} session={}", type, session.getId());
    }
}
