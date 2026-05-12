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
        if (!pluginRpcService.isPluginReachable()) {
            return new GameServerOnlineDto(serverId, null, false);
        }
        try {
            JsonNode reply = pluginRpcService.rpc("get online " + serverId);
            Integer count = parseOnlinePlayers(reply);
            return new GameServerOnlineDto(serverId, count, true);
        } catch (ResponseStatusException e) {
            log.debug("[game-stats] RPC failed for get online {}: {}", serverId, e.getReason());
            return new GameServerOnlineDto(serverId, null, true);
        } catch (Exception e) {
            log.warn("[game-stats] unexpected error get online {}", serverId, e);
            return new GameServerOnlineDto(serverId, null, true);
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

    public record GameServerOnlineDto(String serverId, Integer onlinePlayers, boolean pluginConnected) {
    }
}
