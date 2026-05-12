package com.back.minecraftback.controller;

import com.back.minecraftback.gameserver.GameServerPublicStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = { "/game", "/api/game" })
@RequiredArgsConstructor
public class GameServerStatsController {

    private final GameServerPublicStatsService gameServerPublicStatsService;

    /**
     * Онлайн игроков на сервере для сайта (через WS-RPC к плагину, команда {@code get online {serverId}}).
     */
    @GetMapping(value = "/online", produces = MediaType.APPLICATION_JSON_VALUE)
    public GameServerPublicStatsService.GameServerOnlineDto getOnline() {
        return gameServerPublicStatsService.getServerOnline();
    }
}
