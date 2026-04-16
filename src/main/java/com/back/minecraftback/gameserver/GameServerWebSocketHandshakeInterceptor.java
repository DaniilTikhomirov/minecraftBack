package com.back.minecraftback.gameserver;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Доступ к каналу только с заголовком {@code X-Game-Server-Token}, совпадающим с настроенным секретом.
 * Секрет не логируется; сравнение через {@link MessageDigest#isEqual(byte[], byte[])} при равной длине UTF-8.
 */
@Component
@RequiredArgsConstructor
public class GameServerWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    static final String TOKEN_HEADER = "X-Game-Server-Token";

    /** Атрибуты сессии WebSocket: кто подключился за прокси / с какого сокета (для логов и отладки). */
    static final String ATTR_DIRECT_REMOTE_ADDR = "gameWs.directRemoteAddr";
    static final String ATTR_X_FORWARDED_FOR = "gameWs.xForwardedFor";
    static final String ATTR_X_REAL_IP = "gameWs.xRealIp";
    static final String ATTR_HOST = "gameWs.host";

    private final GameServerWsProperties properties;

    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes
    ) {
        if (!properties.isConfigured()) {
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return false;
        }
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }
        HttpServletRequest req = servletRequest.getServletRequest();
        String presented = req.getHeader(TOKEN_HEADER);
        if (!constantTimeEqualUtf8(properties.token(), presented)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        attributes.put(ATTR_DIRECT_REMOTE_ADDR, req.getRemoteAddr());
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            attributes.put(ATTR_X_FORWARDED_FOR, xff);
        }
        String xri = req.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) {
            attributes.put(ATTR_X_REAL_IP, xri);
        }
        String host = req.getHeader("Host");
        if (host != null && !host.isBlank()) {
            attributes.put(ATTR_HOST, host);
        }
        return true;
    }

    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            Exception exception
    ) {
        // noop
    }

    static boolean constantTimeEqualUtf8(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = actual.getBytes(StandardCharsets.UTF_8);
        if (a.length != b.length) {
            return false;
        }
        return MessageDigest.isEqual(a, b);
    }
}
