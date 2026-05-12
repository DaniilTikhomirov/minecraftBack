package com.back.minecraftback.payment.service;

import com.back.minecraftback.gameserver.GamePaymentWebSocketHandler;
import com.back.minecraftback.gameserver.GameServerValidationProperties;
import com.back.minecraftback.gameserver.GameServerWsProperties;
import com.back.minecraftback.gameserver.GameServerWsRpcAwaiter;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class GameServerValidationClient {

    static final String TYPE_VALIDATION_REQUEST = "VALIDATION_REQUEST";

    private static final int MAX_ATTEMPTS = 2;

    private final ObjectMapper objectMapper;
    private final GameServerWsProperties wsProperties;
    private final GameServerValidationProperties validationProperties;
    private final GamePaymentWebSocketHandler webSocketHandler;
    private final GameServerWsRpcAwaiter rpcAwaiter;

    public void validateBeforePaymentInit(String nickname) {
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

        JsonNode existsReply = rpcWithRetry("check nickname " + nickname);
        if (!isNicknameExists(existsReply)) {
            if (isNicknameNotFound(existsReply)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Player nickname not found on game server"
                );
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unexpected validation response for nickname check"
            );
        }

        if (!validationProperties.requireOnlineOrDefault()) {
            return;
        }

        JsonNode onlineReply = rpcWithRetry("check online " + nickname);
        if (!isPlayerOnline(onlineReply)) {
            if (isPlayerOffline(onlineReply)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Player is offline now; connect to server and try again"
                );
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unexpected validation response for online check"
            );
        }
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
                "Game server validation timeout",
                lastTimeout != null ? lastTimeout.getCause() : null
        );
    }

    private JsonNode rpcOnce(String commandMessage) throws ExecutionException, InterruptedException, IOException {
        String requestId = java.util.UUID.randomUUID().toString();
        long timeoutMs = validationProperties.rpcTimeoutMsOrDefault();

        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("type", TYPE_VALIDATION_REQUEST);
        envelope.put("requestId", requestId);
        envelope.put("message", commandMessage);
        String json = objectMapper.writeValueAsString(envelope);

        CompletableFuture<JsonNode> pending = rpcAwaiter.register(requestId, timeoutMs);
        webSocketHandler.sendValidationRequestToFirstSession(json);
        return pending.get();
    }

    private static ResponseStatusException wrap(Throwable cause) {
        if (cause instanceof ResponseStatusException r) {
            return r;
        }
        if (cause instanceof IOException) {
            return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "WebSocket send failed", cause);
        }
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Game server validation failed", cause);
    }

    private static boolean isNicknameExists(JsonNode reply) {
        String msg = reply.path("message").asText("").trim();
        return "nickname exists".equalsIgnoreCase(msg);
    }

    private static boolean isNicknameNotFound(JsonNode reply) {
        String msg = reply.path("message").asText("").trim();
        return "nickname not_found".equalsIgnoreCase(msg) || msg.toLowerCase().contains("not_found");
    }

    private static boolean isPlayerOnline(JsonNode reply) {
        String msg = reply.path("message").asText("").trim();
        return "player online".equalsIgnoreCase(msg);
    }

    private static boolean isPlayerOffline(JsonNode reply) {
        String msg = reply.path("message").asText("").trim();
        return "player offline".equalsIgnoreCase(msg);
    }
}
