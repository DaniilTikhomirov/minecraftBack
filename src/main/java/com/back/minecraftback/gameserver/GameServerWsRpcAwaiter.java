package com.back.minecraftback.gameserver;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Один ожидаемый RPC-ответ за раз (формат плагина без {@code requestId}).
 */
@Component
public class GameServerWsRpcAwaiter {

    private final AtomicReference<CompletableFuture<JsonNode>> pending = new AtomicReference<>();

    public CompletableFuture<JsonNode> register(long timeoutMs) {
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        CompletableFuture<JsonNode> existing = pending.get();
        if (existing != null && !existing.isDone()) {
            future.completeExceptionally(new IllegalStateException("Another plugin RPC is already in progress"));
            return future;
        }
        pending.set(future);
        return future.orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .whenComplete((r, ex) -> pending.compareAndSet(future, null));
    }

    /**
     * @return true если ответ сопоставлен с ожидающим RPC
     */
    public boolean complete(JsonNode body) {
        CompletableFuture<JsonNode> f = pending.get();
        if (f != null && !f.isDone()) {
            f.complete(body);
            return true;
        }
        return false;
    }
}
