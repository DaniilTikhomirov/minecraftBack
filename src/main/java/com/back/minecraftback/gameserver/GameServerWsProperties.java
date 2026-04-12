package com.back.minecraftback.gameserver;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Секрет для WebSocket handshake и для HMAC подписи исходящих событий (один ключ — проще ротация).
 */
@ConfigurationProperties(prefix = "game-server.ws")
public record GameServerWsProperties(String token) {

    private static final int MIN_TOKEN_LENGTH = 32;

    public boolean isConfigured() {
        return token != null && token.length() >= MIN_TOKEN_LENGTH;
    }
}
