package com.back.minecraftback.payment.service;

import com.back.minecraftback.gameserver.GameServerPluginRpcService;
import com.back.minecraftback.gameserver.GameServerValidationProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameServerValidationClient {

    private final GameServerPluginRpcService pluginRpcService;
    private final GameServerValidationProperties validationProperties;

    public void validateBeforePaymentInit(String nickname) {
        JsonNode existsReply = pluginRpcService.rpc("check nickname " + nickname);
        if (!isNicknameExists(existsReply)) {
            if (isNicknameNotFound(existsReply)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Player nickname not found on game server"
                );
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unexpected validation response for nickname check"
            );
        }

        if (!validationProperties.requireOnlineOrDefault()) {
            return;
        }

        JsonNode onlineReply = pluginRpcService.rpc("check online " + nickname);
        if (!isPlayerOnline(onlineReply)) {
            if (isPlayerOffline(onlineReply)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Player is offline now; connect to server and try again"
                );
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unexpected validation response for online check"
            );
        }
    }

    private static boolean isNicknameExists(JsonNode reply) {
        String msg = reply.path("message").asText("").trim();
        return "nickname exists".equalsIgnoreCase(msg);
    }

    private static boolean isNicknameNotFound(JsonNode reply) {
        String msg = reply.path("message").asText("").trim();
        return "nickname not_found".equalsIgnoreCase(msg) || msg.toLowerCase().contains("not_found");
    }

    private static boolean isPlayerOnline(JsonNode reply) {
        String msg = reply.path("message").asText("").trim();
        return "player online".equalsIgnoreCase(msg);
    }

    private static boolean isPlayerOffline(JsonNode reply) {
        String msg = reply.path("message").asText("").trim();
        return "player offline".equalsIgnoreCase(msg);
    }
}
