package com.back.minecraftback.payment.service;

import com.back.minecraftback.gameserver.GameServerPluginRpcService;
import com.back.minecraftback.gameserver.GameServerValidationProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

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
            log.warn("[game-validation] unexpected nickname check reply: {}", existsReply);
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
            log.warn("[game-validation] unexpected online check reply: {}", onlineReply);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unexpected validation response for online check"
            );
        }
    }

    private static boolean isNicknameExists(JsonNode reply) {
        if (replyOk(reply) && !isNicknameNotFound(reply)) {
            return true;
        }
        String msg = replyMessage(reply);
        if (msg.isEmpty()) {
            return false;
        }
        String lower = msg.toLowerCase(Locale.ROOT);
        if (lower.contains("not_found") || lower.contains("not found")) {
            return false;
        }
        return "nickname exists".equalsIgnoreCase(msg)
                || lower.contains("nickname exists")
                || "nick exists".equalsIgnoreCase(msg)
                || lower.contains("nick exists")
                || lower.equals("exists")
                || (lower.contains("exists") && lower.contains("nick"));
    }

    private static boolean isNicknameNotFound(JsonNode reply) {
        if (reply.has("ok") && reply.get("ok").isBoolean() && !reply.get("ok").asBoolean()) {
            String lower = replyMessage(reply).toLowerCase(Locale.ROOT);
            if (lower.contains("not_found") || lower.contains("not found") || lower.contains("не найден")) {
                return true;
            }
        }
        String msg = replyMessage(reply);
        if (msg.isEmpty()) {
            return false;
        }
        String lower = msg.toLowerCase(Locale.ROOT);
        return "nickname not_found".equalsIgnoreCase(msg)
                || lower.contains("not_found")
                || lower.contains("not found")
                || lower.contains("не найден")
                || lower.contains("nickname not");
    }

    private static boolean isPlayerOnline(JsonNode reply) {
        if (replyOk(reply) && !isPlayerOffline(reply)) {
            return true;
        }
        String msg = replyMessage(reply);
        if (msg.isEmpty()) {
            return false;
        }
        String lower = msg.toLowerCase(Locale.ROOT);
        return "player online".equalsIgnoreCase(msg)
                || lower.contains("player online")
                || lower.equals("online")
                || (lower.contains("online") && !lower.contains("offline"));
    }

    private static boolean isPlayerOffline(JsonNode reply) {
        if (reply.has("ok") && reply.get("ok").isBoolean() && !reply.get("ok").asBoolean()) {
            String lower = replyMessage(reply).toLowerCase(Locale.ROOT);
            if (lower.contains("offline") || lower.contains("оффлайн") || lower.contains("не в сети")) {
                return true;
            }
        }
        String msg = replyMessage(reply);
        if (msg.isEmpty()) {
            return false;
        }
        String lower = msg.toLowerCase(Locale.ROOT);
        return "player offline".equalsIgnoreCase(msg)
                || lower.contains("player offline")
                || lower.contains("offline");
    }

    private static String replyMessage(JsonNode reply) {
        if (reply == null || reply.isNull()) {
            return "";
        }
        String msg = reply.path("message").asText("").trim();
        if (!msg.isEmpty()) {
            return msg;
        }
        return reply.path("result").asText("").trim();
    }

    private static boolean replyOk(JsonNode reply) {
        if (reply == null || !reply.has("ok") || reply.get("ok").isNull()) {
            return false;
        }
        JsonNode ok = reply.get("ok");
        if (ok.isBoolean()) {
            return ok.asBoolean();
        }
        return "true".equalsIgnoreCase(ok.asText(""));
    }
}
