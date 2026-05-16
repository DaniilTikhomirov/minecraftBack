package com.back.minecraftback.gameserver;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Идентификатор сервера только для выдачи доната ({@code PAYMENT_CONFIRMED}), не для {@code /game/online}.
 */
@ConfigurationProperties(prefix = "game-server.payment")
public record GameServerPaymentProperties(String serverId) {

    private static final String DEFAULT_PAYMENT_SERVER_ID = "anarchy-1";

    public String serverIdOrDefault() {
        return serverId == null || serverId.isBlank() ? DEFAULT_PAYMENT_SERVER_ID : serverId.trim();
    }
}
