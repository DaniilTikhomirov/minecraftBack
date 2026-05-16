package com.back.minecraftback.gameserver;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Идентификатор сервера для команды плагину {@code get online {serverId}} (онлайн на сайте).
 */
@ConfigurationProperties(prefix = "game-server.stats")
public record GameServerStatsProperties(String serverId) {

    public String serverIdOrDefault() {
        return serverId == null || serverId.isBlank() ? "anarchy-1" : serverId.trim();
    }
}
