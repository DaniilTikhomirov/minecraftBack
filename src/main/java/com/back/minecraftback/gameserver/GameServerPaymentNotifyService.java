package com.back.minecraftback.gameserver;

import com.back.minecraftback.payment.tbank.TbankTokenSigner;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Уведомление плагина об оплате.
 * <p>
 * Подпись HMAC-SHA256 (hex) считается по канонической строке UTF-8 (поля через {@code |}, без JSON):
 * {@code version|type|issuedAtMillis|tbankOrderId|internalOrderUuid|nicknameBase64|amountKopecks|productType|productId|subscriptionPeriod|quantity|tbankPaymentId}
 * где пустые значения — пустая строка, {@code nicknameBase64} — Base64(URL_SAFE без переносов) от UTF-8 ника
 * (чтобы символы {@code |} в нике не ломали разбор).
 * </p>
 * <p>
 * Плагин обязан: проверить подпись тем же секретом; отклонить событие при {@code |now - issuedAtMillis| > 5 минут};
 * выдавать донат идемпотентно по {@code tbankOrderId} / {@code internalOrderUuid} (хранить обработанные id).
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameServerPaymentNotifyService {

    private static final int VERSION = 1;
    private static final String TYPE_PAYMENT_CONFIRMED = "PAYMENT_CONFIRMED";

    private final GameServerWsProperties properties;
    private final GamePaymentWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    public void notifyPaymentPaid(PaymentPaidGameEvent event) {
        if (!properties.isConfigured()) {
            return;
        }
        if (webSocketHandler.getOpenSessionCount() == 0) {
            log.debug("[game-ws] no subscribers, skip push for order {}", event.tbankOrderId());
            return;
        }

        long issuedAtMillis = System.currentTimeMillis();
        String nickB64 = Base64.getEncoder().encodeToString(event.nickname().getBytes(StandardCharsets.UTF_8));
        String productId = event.productId() == null ? "" : String.valueOf(event.productId());
        String period = event.subscriptionPeriod() == null ? "" : event.subscriptionPeriod();
        String qty = event.quantity() == null ? "" : String.valueOf(event.quantity());
        String payId = event.tbankPaymentId() == null ? "" : event.tbankPaymentId();

        String canonical = String.join("|",
                String.valueOf(VERSION),
                TYPE_PAYMENT_CONFIRMED,
                String.valueOf(issuedAtMillis),
                event.tbankOrderId(),
                event.internalOrderId().toString(),
                nickB64,
                String.valueOf(event.amountKopecks()),
                event.productType(),
                productId,
                period,
                qty,
                payId
        );

        String signatureHex = GameServerHmac.hmacSha256Hex(properties.token(), canonical);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", event.tbankOrderId());
        payload.put("internalOrderId", event.internalOrderId().toString());
        payload.put("nickname", event.nickname());
        payload.put("amountKopecks", event.amountKopecks());
        payload.put("productType", event.productType());
        payload.put("productId", event.productId());
        payload.put("subscriptionPeriod", event.subscriptionPeriod());
        payload.put("quantity", event.quantity());
        payload.put("tbankPaymentId", event.tbankPaymentId());

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("version", VERSION);
        envelope.put("type", TYPE_PAYMENT_CONFIRMED);
        envelope.put("issuedAtMillis", issuedAtMillis);
        envelope.put("payload", payload);
        envelope.put("signatureHex", signatureHex);

        try {
            String json = objectMapper.writeValueAsString(envelope);
            webSocketHandler.broadcastSignedJson(json);
            log.info("[game-ws] pushed PAYMENT_CONFIRMED orderId={}", event.tbankOrderId());
        } catch (Exception e) {
            log.error("[game-ws] failed to serialize or send orderId={}", event.tbankOrderId(), e);
        }
    }

    /**
     * Для тестов плагина: проверка подписи без отправки по сети.
     */
    public static boolean verifySignature(String secret, PaymentPaidGameEvent event, long issuedAtMillis, String signatureHex) {
        String nickB64 = Base64.getEncoder().encodeToString(event.nickname().getBytes(StandardCharsets.UTF_8));
        String productId = event.productId() == null ? "" : String.valueOf(event.productId());
        String period = event.subscriptionPeriod() == null ? "" : event.subscriptionPeriod();
        String qty = event.quantity() == null ? "" : String.valueOf(event.quantity());
        String payId = event.tbankPaymentId() == null ? "" : event.tbankPaymentId();
        String canonical = String.join("|",
                String.valueOf(VERSION),
                TYPE_PAYMENT_CONFIRMED,
                String.valueOf(issuedAtMillis),
                event.tbankOrderId(),
                event.internalOrderId().toString(),
                nickB64,
                String.valueOf(event.amountKopecks()),
                event.productType(),
                productId,
                period,
                qty,
                payId
        );
        String expected = GameServerHmac.hmacSha256Hex(secret, canonical);
        return TbankTokenSigner.constantTimeEquals(expected, signatureHex);
    }
}
