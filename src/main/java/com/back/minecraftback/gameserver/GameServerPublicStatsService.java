package com.back.minecraftback.gameserver;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameServerPublicStatsService {

    private static final Pattern ONLINE_COUNT = Pattern.compile("(?i)online\\s+(\\d+)");

    private final GameServerPluginRpcService pluginRpcService;
    private final GameServerStatsProperties statsProperties;

    public GameServerOnlineDto getServerOnline() {
        String serverId = statsProperties.serverIdOrDefault();
        boolean tokenOk = pluginRpcService.isWsTokenConfigured();
        int sessions = pluginRpcService.getOpenWebSocketSessionCount();
        boolean connected = tokenOk && sessions > 0;

        if (!connected) {
            return new GameServerOnlineDto(serverId, null, false, tokenOk, sessions);
        }
        try {
            JsonNode reply = pluginRpcService.rpc("get online " + serverId);
            Integer count = parseOnlinePlayers(reply);
            return new GameServerOnlineDto(serverId, count, true, true, sessions);
        } catch (ResponseStatusException e) {
            log.debug("[game-stats] RPC failed for get online {}: {}", serverId, e.getReason());
            return new GameServerOnlineDto(serverId, null, true, true, sessions);
        } catch (Exception e) {
            log.warn("[game-stats] unexpected error get online {}", serverId, e);
            return new GameServerOnlineDto(serverId, null, true, true, sessions);
        }
    }

    private static Integer parseOnlinePlayers(JsonNode reply) {
        String msg = reply.path("message").asText("").trim();
        Matcher m = ONLINE_COUNT.matcher(msg);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * @param pluginConnected токен настроен и есть хотя бы одна WS-сессия плагина
     * @param wsTokenConfigured {@code game-server.ws.token} не пустой и длина ≥ 32
     * @param openWebSocketSessions число открытых сессий к {@code /api/game/ws}
     */
    public record GameServerOnlineDto(
            String serverId,
            Integer onlinePlayers,
            boolean pluginConnected,
            boolean wsTokenConfigured,
            int openWebSocketSessions
    ) {
    }
}
