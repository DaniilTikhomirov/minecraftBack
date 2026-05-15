package com.back.minecraftback.gameserver;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class GameServerWsRpcAwaiter {

    private final ConcurrentHashMap<String, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();

    public CompletableFuture<JsonNode> register(String requestId, long timeoutMs) {
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        CompletableFuture<JsonNode> timed = future.orTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        pending.put(requestId, future);
        timed.whenComplete((r, ex) -> pending.remove(requestId, future));
        return timed;
    }

    public void complete(String requestId, JsonNode body) {
        CompletableFuture<JsonNode> f = pending.get(requestId);
        if (f != null) {
            f.complete(body);
        }
    }
}
