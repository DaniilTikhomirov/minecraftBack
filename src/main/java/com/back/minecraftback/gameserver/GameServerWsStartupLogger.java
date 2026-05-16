package com.back.minecraftback.gameserver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameServerWsStartupLogger {

    private final GameServerWsProperties wsProperties;
    private final GameServerPaymentProperties paymentProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void logGameServerChannel() {
        if (wsProperties.isConfigured()) {
            log.info(
                    "[game-ws] ready: token length={}, expect plugin at /api/game/ws, paymentServerId={}",
                    wsProperties.normalizedToken().length(),
                    paymentProperties.serverIdOrDefault()
            );
        } else {
            log.warn("[game-ws] GAME_SERVER_WS_TOKEN missing or shorter than 32 chars — plugin cannot connect");
        }
    }
}
