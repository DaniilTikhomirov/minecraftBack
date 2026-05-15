package com.back.minecraftback.gameserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * RPC по WebSocket: {@code VALIDATION_REQUEST} / {@code VALIDATION_RESPONSE} с {@code requestId}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameServerPluginRpcService {

    public static final String TYPE_VALIDATION_REQUEST = "VALIDATION_REQUEST";

    private static final int MAX_ATTEMPTS = 2;

    private final ObjectMapper objectMapper;
    private final GameServerWsProperties wsProperties;
    private final GameServerValidationProperties validationProperties;
    private final GamePaymentWebSocketHandler webSocketHandler;
    private final GameServerWsRpcAwaiter rpcAwaiter;

    public JsonNode rpc(String commandMessage) {
        if (!wsProperties.isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Game server WebSocket token is not configured"
            );
        }
        if (webSocketHandler.getOpenSessionCount() == 0) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Game server is not connected via WebSocket"
            );
        }
        return rpcWithRetry(commandMessage);
    }

    public boolean isPluginReachable() {
        return isWsTokenConfigured() && getOpenWebSocketSessionCount() > 0;
    }

    /** Токен {@code game-server.ws.token} задан и длина ≥ 32 (иначе handshake не примет плагин). */
    public boolean isWsTokenConfigured() {
        return wsProperties.isConfigured();
    }

    public int getOpenWebSocketSessionCount() {
        return webSocketHandler.getOpenSessionCount();
    }

    private JsonNode rpcWithRetry(String commandMessage) {
        ExecutionException lastTimeout = null;
        IOException lastIo = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return rpcOnce(commandMessage);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof TimeoutException) {
                    lastTimeout = e;
                    log.warn("[game-ws-rpc] timeout command={} attempt={}/{}", commandMessage, attempt, MAX_ATTEMPTS);
                } else {
                    throw wrap(e.getCause());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Interrupted", e);
            } catch (IOException e) {
                lastIo = e;
                log.warn("[game-ws-rpc] io error command={} attempt={}/{} msg={}",
                        commandMessage, attempt, MAX_ATTEMPTS, e.getMessage());
            }
        }
        if (lastIo != null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "WebSocket send failed", lastIo);
        }
        throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Game server plugin RPC timeout",
                lastTimeout != null ? lastTimeout.getCause() : null
        );
    }

    private JsonNode rpcOnce(String commandMessage) throws ExecutionException, InterruptedException, IOException {
        String requestId = java.util.UUID.randomUUID().toString();
        long timeoutMs = validationProperties.rpcTimeoutMsOrDefault();

        ObjectNode request = objectMapper.createObjectNode();
        request.put("type", TYPE_VALIDATION_REQUEST);
        request.put("requestId", requestId);
        request.put("message", commandMessage);
        String json = objectMapper.writeValueAsString(request);

        CompletableFuture<JsonNode> pending = rpcAwaiter.register(requestId, timeoutMs);
        webSocketHandler.sendPluginMessage(json);
        return pending.get();
    }

    private static ResponseStatusException wrap(Throwable cause) {
        if (cause instanceof ResponseStatusException r) {
            return r;
        }
        if (cause instanceof IOException) {
            return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "WebSocket send failed", cause);
        }
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Game server plugin RPC failed", cause);
    }
}
