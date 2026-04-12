package com.back.minecraftback.gameserver;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@EnableConfigurationProperties(GameServerWsProperties.class)
@RequiredArgsConstructor
public class GameServerWebSocketConfig implements WebSocketConfigurer {

    private final GamePaymentWebSocketHandler gamePaymentWebSocketHandler;
    private final GameServerWebSocketHandshakeInterceptor gameServerWebSocketHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(gamePaymentWebSocketHandler, "/api/game/ws", "/game/ws")
                .addInterceptors(gameServerWebSocketHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
