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
        if (replyOkFalse(reply)) {
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
        Boolean onlineFlag = replyOnlineFlag(reply);
        if (onlineFlag != null) {
            return onlineFlag;
        }
        if (replyOk(reply) && !isPlayerOffline(reply)) {
            return true;
        }
        String msg = replyMessage(reply);
        if (msg.isEmpty()) {
            return false;
        }
        if (isPlayerOffline(reply)) {
            return false;
        }
        String lower = msg.toLowerCase(Locale.ROOT);
        return "player online".equalsIgnoreCase(msg)
                || lower.contains("player online")
                || lower.contains("is online")
                || lower.contains("в сети")
                || lower.contains("онлайн")
                || lower.equals("online")
                || lower.equals("true")
                || lower.equals("yes")
                || lower.equals("1")
                || lower.equals("success")
                || lower.equals("connected")
                || lower.contains("connected")
                || (lower.contains("online") && !lower.contains("offline") && !lower.contains("not online"));
    }

    private static boolean isPlayerOffline(JsonNode reply) {
        Boolean onlineFlag = replyOnlineFlag(reply);
        if (onlineFlag != null) {
            return !onlineFlag;
        }
        if (replyOkFalse(reply)) {
            String lower = replyMessage(reply).toLowerCase(Locale.ROOT);
            if (lower.contains("offline")
                    || lower.contains("оффлайн")
                    || lower.contains("не в сети")
                    || lower.contains("not online")) {
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
                || lower.contains("is offline")
                || lower.contains("not online")
                || lower.contains("не в сети")
                || lower.equals("offline")
                || lower.equals("false")
                || lower.equals("no")
                || lower.equals("0");
    }

    /** {@code online} / {@code data.online} / {@code payload.online} — если есть, приоритет над текстом. */
    private static Boolean replyOnlineFlag(JsonNode reply) {
        if (reply == null || reply.isNull()) {
            return null;
        }
        for (String field : new String[] {"online", "isOnline", "playerOnline"}) {
            Boolean b = readBooleanField(reply, field);
            if (b != null) {
                return b;
            }
        }
        JsonNode data = reply.get("data");
        if (data != null && data.isObject()) {
            for (String field : new String[] {"online", "isOnline", "playerOnline"}) {
                Boolean b = readBooleanField(data, field);
                if (b != null) {
                    return b;
                }
            }
        }
        return null;
    }

    private static Boolean readBooleanField(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            return null;
        }
        JsonNode v = node.get(field);
        if (v.isBoolean()) {
            return v.asBoolean();
        }
        String text = v.asText("").trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return null;
        }
        if ("true".equals(text) || "yes".equals(text) || "1".equals(text) || "online".equals(text)) {
            return true;
        }
        if ("false".equals(text) || "no".equals(text) || "0".equals(text) || "offline".equals(text)) {
            return false;
        }
        return null;
    }

    private static String replyMessage(JsonNode reply) {
        if (reply == null || reply.isNull()) {
            return "";
        }
        String msg = reply.path("message").asText("").trim();
        if (!msg.isEmpty()) {
            return msg;
        }
        msg = reply.path("result").asText("").trim();
        if (!msg.isEmpty()) {
            return msg;
        }
        return reply.path("status").asText("").trim();
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

    private static boolean replyOkFalse(JsonNode reply) {
        if (reply == null || !reply.has("ok") || reply.get("ok").isNull()) {
            return false;
        }
        JsonNode ok = reply.get("ok");
        if (ok.isBoolean()) {
            return !ok.asBoolean();
        }
        return "false".equalsIgnoreCase(ok.asText(""));
    }
}
