package com.back.minecraftback.gameserver;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "game-server.validation")
public record GameServerValidationProperties(
        Boolean requireOnline,
        Integer rpcTimeoutMs
) {
    private static final int DEFAULT_RPC_TIMEOUT_MS = 2000;

    public boolean requireOnlineOrDefault() {
        return requireOnline == null || requireOnline;
    }

    public int rpcTimeoutMsOrDefault() {
        return rpcTimeoutMs == null || rpcTimeoutMs <= 0
                ? DEFAULT_RPC_TIMEOUT_MS
                : rpcTimeoutMs;
    }
}
