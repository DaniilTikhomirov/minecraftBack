package com.back.minecraftback.gameserver;

import com.back.minecraftback.payment.entity.PaymentOrderEntity;
import com.back.minecraftback.payment.model.PaymentOrderStatus;
import com.back.minecraftback.payment.repository.PaymentOrderRepository;
import com.back.minecraftback.payment.tbank.TbankTokenSigner;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Уведомление плагина об оплате.
 * <p>
 * Подпись HMAC-SHA256 (hex) считается по канонической строке UTF-8 (поля через {@code |}, без JSON):
 * {@code version|type|issuedAtMillis|tbankOrderId|internalOrderUuid|nicknameBase64|amountKopecks|productType|productId|subscriptionPeriod|quantity|tbankPaymentId|serverId}
 * где пустые значения — пустая строка, {@code nicknameBase64} — Base64 от UTF-8 ника.
 * </p>
 * <p>
 * Плагин обязан: проверить подпись тем же секретом; отклонить событие при {@code |now - issuedAtMillis| > 5 минут};
 * выдавать донат идемпотентно по {@code tbankOrderId} / {@code internalOrderUuid} (хранить обработанные id);
 * выдавать только если {@code serverId} совпадает с id этого инстанса сервера.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameServerPaymentNotifyService {

    private static final int VERSION = 2;
    private static final String TYPE_PAYMENT_CONFIRMED = "PAYMENT_CONFIRMED";

    private final GameServerWsProperties properties;
    private final GameServerPaymentProperties paymentProperties;
    private final GamePaymentWebSocketHandler webSocketHandler;
    private final PaymentOrderRepository paymentOrderRepository;
    private final ObjectMapper objectMapper;

    public void notifyPaymentPaid(PaymentPaidGameEvent event) {
        if (!properties.isConfigured()) {
            return;
        }
        if (webSocketHandler.getOpenSessionCount() == 0) {
            log.warn("[game-ws] no subscribers, will retry PAYMENT_CONFIRMED order {}", event.tbankOrderId());
            return;
        }

        boolean sent = pushEvent(event);
        if (sent) {
            markNotified(event.internalOrderId());
        } else {
            log.warn("[game-ws] PAYMENT_CONFIRMED not delivered, will retry order {}", event.tbankOrderId());
        }
    }

    /**
     * Повторная отправка оплаченных заказов, если плагин был офлайн в момент webhook.
     * Подпись пересчитывается с текущим {@code issuedAtMillis}, чтобы пройти окно 5 минут.
     */
    @Scheduled(fixedDelay = 15_000)
    public synchronized void replayPendingPayments() {
        if (!properties.isConfigured() || webSocketHandler.getOpenSessionCount() == 0) {
            return;
        }
        List<PaymentOrderEntity> pending =
                paymentOrderRepository.findTop50ByStatusAndGameNotifiedAtIsNullOrderByCreatedAtAsc(PaymentOrderStatus.PAID);
        if (pending.isEmpty()) {
            return;
        }
        log.info("[game-ws] replaying {} unpaid-notify PAID order(s)", pending.size());
        for (PaymentOrderEntity order : pending) {
            PaymentPaidGameEvent event = toEvent(order);
            boolean sent = pushEvent(event);
            if (sent) {
                markNotified(order.getId());
            } else {
                break;
            }
        }
    }

    private boolean pushEvent(PaymentPaidGameEvent event) {
        long issuedAtMillis = System.currentTimeMillis();
        String canonical = buildCanonical(event, issuedAtMillis);
        String signatureHex = GameServerHmac.hmacSha256Hex(properties.normalizedToken(), canonical);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", event.tbankOrderId());
        payload.put("internalOrderId", event.internalOrderId().toString());
        payload.put("serverId", event.serverId());
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
            boolean sent = webSocketHandler.broadcastSignedJson(json);
            if (sent) {
                log.info("[game-ws] pushed PAYMENT_CONFIRMED orderId={} serverId={}", event.tbankOrderId(), event.serverId());
            }
            return sent;
        } catch (Exception e) {
            log.error("[game-ws] failed to serialize or send orderId={}", event.tbankOrderId(), e);
            return false;
        }
    }

    private void markNotified(UUID orderId) {
        paymentOrderRepository.findById(orderId).ifPresent(order -> {
            if (order.getGameNotifiedAt() == null) {
                order.setGameNotifiedAt(Instant.now());
                paymentOrderRepository.save(order);
            }
        });
    }

    private PaymentPaidGameEvent toEvent(PaymentOrderEntity order) {
        return new PaymentPaidGameEvent(
                order.getTbankOrderId(),
                order.getId(),
                paymentProperties.serverIdOrDefault(),
                order.getNickname(),
                order.getAmountKopecks(),
                order.getProductType().name(),
                order.getProductId(),
                order.getSubscriptionPeriod() == null ? null : order.getSubscriptionPeriod().name(),
                order.getQuantity(),
                order.getTbankPaymentId()
        );
    }

    /**
     * Для тестов плагина: проверка подписи без отправки по сети.
     */
    public static boolean verifySignature(String secret, PaymentPaidGameEvent event, long issuedAtMillis, String signatureHex) {
        String expected = GameServerHmac.hmacSha256Hex(secret, buildCanonical(event, issuedAtMillis));
        return TbankTokenSigner.constantTimeEquals(expected, signatureHex);
    }

    private static String buildCanonical(PaymentPaidGameEvent event, long issuedAtMillis) {
        String nickB64 = Base64.getEncoder().encodeToString(event.nickname().getBytes(StandardCharsets.UTF_8));
        String productId = event.productId() == null ? "" : String.valueOf(event.productId());
        String period = event.subscriptionPeriod() == null ? "" : event.subscriptionPeriod();
        String qty = event.quantity() == null ? "" : String.valueOf(event.quantity());
        String payId = event.tbankPaymentId() == null ? "" : event.tbankPaymentId();
        String serverId = event.serverId() == null ? "" : event.serverId();
        return String.join("|",
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
                payId,
                serverId
        );
    }
}
