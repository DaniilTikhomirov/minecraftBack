package com.back.minecraftback.payment.service;

import com.back.minecraftback.payment.config.TbankAcquiringProperties;
import com.back.minecraftback.payment.dto.PaymentInitRequestDto;
import com.back.minecraftback.payment.dto.PaymentInitResponseDto;
import com.back.minecraftback.payment.entity.PaymentOrderEntity;
import com.back.minecraftback.payment.model.PaymentOrderStatus;
import com.back.minecraftback.payment.model.PaymentProductType;
import com.back.minecraftback.payment.model.RankSubscriptionPeriod;
import com.back.minecraftback.payment.repository.PaymentOrderRepository;
import com.back.minecraftback.gameserver.PaymentPaidGameEvent;
import com.back.minecraftback.shopstats.ShopStatsService;
import com.back.minecraftback.payment.tbank.TbankInitResponse;
import com.back.minecraftback.payment.tbank.TbankTokenSigner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TbankPaymentService {

    private static final int DESCRIPTION_MAX = 140;
    private static final int RAW_RESPONSE_MAX = 8000;
    private static final Pattern NICKNAME = Pattern.compile("^[\\p{L}0-9_.\\- ]{2,64}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final TbankAcquiringProperties properties;
    private final PaymentPricingService pricingService;
    private final PaymentOrderRepository paymentOrderRepository;
    private final TbankAcquiringClient tbankAcquiringClient;
    private final ObjectMapper objectMapper;
    private final ShopStatsService shopStatsService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public PaymentInitResponseDto init(PaymentInitRequestDto dto) {
        if (!properties.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payment gateway not configured");
        }

        PaymentProductType type = parseProductType(dto.type());
        RankSubscriptionPeriod period = parsePeriod(dto.period(), type);
        String nickname = requireNickname(dto.nickname());
        String email = normalizeEmail(dto.email());

        long amountKopecks = pricingService.computeAmountKopecks(type, dto.itemId(), dto.quantity(), period);

        UUID id = UUID.randomUUID();
        String orderKey = id.toString();

        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId(id);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        order.setTbankOrderId(orderKey);
        order.setNickname(nickname);
        order.setEmail(email);
        order.setAmountKopecks(amountKopecks);
        order.setProductType(type);
        order.setProductId(dto.itemId());
        order.setSubscriptionPeriod(period);
        order.setQuantity(dto.quantity());
        order.setStatus(PaymentOrderStatus.PENDING);
        paymentOrderRepository.save(order);

        Map<String, Object> body = buildInitJsonBody(orderKey, amountKopecks, nickname, type, dto.itemId(), dto.quantity(), period);
        try {
            TbankInitResponse resp = tbankAcquiringClient.callInit(body);
            String rawJson = objectMapper.writeValueAsString(resp);
            order.setRawInitResponse(truncate(rawJson, RAW_RESPONSE_MAX));

            if (!resp.isSuccess()) {
                order.setStatus(PaymentOrderStatus.FAILED);
                paymentOrderRepository.save(order);
                log.warn("T-Bank Init declined: orderId={} code={} msg={}", orderKey, resp.getErrorCode(), resp.getMessage());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Payment init declined");
            }
            if (resp.getPaymentUrl() == null || resp.getPaymentUrl().isBlank()) {
                order.setStatus(PaymentOrderStatus.FAILED);
                paymentOrderRepository.save(order);
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No payment URL");
            }
            if (resp.getPaymentId() != null) {
                order.setTbankPaymentId(String.valueOf(resp.getPaymentId()));
            }
            order.setPaymentUrl(resp.getPaymentUrl());
            paymentOrderRepository.save(order);

            return new PaymentInitResponseDto(resp.getPaymentUrl(), orderKey);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("T-Bank Init HTTP error orderId={}", orderKey, e);
            order.setStatus(PaymentOrderStatus.FAILED);
            paymentOrderRepository.save(order);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment init failed", e);
        }
    }

    private Map<String, Object> buildInitJsonBody(
            String orderId,
            long amountKopecks,
            String nickname,
            PaymentProductType type,
            Long itemId,
            Integer quantity,
            RankSubscriptionPeriod period
    ) {
        String terminalKey = properties.terminalKey();
        Map<String, Object> json = new HashMap<>();
        json.put("TerminalKey", terminalKey);
        json.put("Amount", amountKopecks);
        json.put("OrderId", orderId);

        String description = buildDescription(nickname, type, itemId, quantity, period);
        String shortDescription = description.length() <= DESCRIPTION_MAX ? description : description.substring(0, DESCRIPTION_MAX);
        json.put("Description", shortDescription);

        Map<String, String> forSign = new HashMap<>();
        forSign.put("TerminalKey", terminalKey);
        forSign.put("Amount", String.valueOf(amountKopecks));
        forSign.put("OrderId", orderId);
        forSign.put("Description", shortDescription);

        // Язык формы явно "ru" (как в доке)
        json.put("Language", "ru");
        forSign.put("Language", "ru");

        // Простой DATA с ником (не обязателен, но допустим)
        Map<String, Object> data = new HashMap<>();
        data.put("Nickname", nickname);
        json.put("DATA", data);

        if (properties.successUrl() != null && !properties.successUrl().isBlank()) {
            json.put("SuccessURL", properties.successUrl());
            forSign.put("SuccessURL", properties.successUrl());
        }
        if (properties.failUrl() != null && !properties.failUrl().isBlank()) {
            json.put("FailURL", properties.failUrl());
            forSign.put("FailURL", properties.failUrl());
        }
        if (properties.notificationUrl() != null && !properties.notificationUrl().isBlank()) {
            json.put("NotificationURL", properties.notificationUrl());
            forSign.put("NotificationURL", properties.notificationUrl());
        }

        String token = TbankTokenSigner.sign(forSign, properties.password());
        json.put("Token", token);
        return json;
    }

    private static String buildDescription(
            String nickname,
            PaymentProductType type,
            Long itemId,
            Integer quantity,
            RankSubscriptionPeriod period
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(type.name());
        if (itemId != null) {
            sb.append(" #").append(itemId);
        }
        if (quantity != null) {
            sb.append(" x").append(quantity);
        }
        if (period != null) {
            sb.append(" ").append(period.name());
        }
        sb.append(" ").append(nickname);
        String s = sb.toString();
        return s.length() <= DESCRIPTION_MAX ? s : s.substring(0, DESCRIPTION_MAX);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    @Transactional
    public void handleNotification(JsonNode root) {
        if (!properties.isConfigured()) {
            log.warn("T-Bank notification ignored: gateway not configured");
            return;
        }
        String receivedToken = readText(root, "Token");
        if (receivedToken == null || receivedToken.isBlank()) {
            throw new SecurityException("missing Token");
        }

        Map<String, String> forSign = scalarParamsForNotificationSignature(root);
        String expected = TbankTokenSigner.sign(forSign, properties.password());
        if (!TbankTokenSigner.constantTimeEquals(expected, receivedToken)) {
            log.warn("T-Bank notification: invalid signature");
            throw new SecurityException("invalid Token");
        }

        String terminalKey = readText(root, "TerminalKey");
        if (terminalKey != null && properties.terminalKey() != null
                && !properties.terminalKey().equals(terminalKey)) {
            log.warn("T-Bank notification: TerminalKey mismatch");
            throw new SecurityException("TerminalKey mismatch");
        }

        String orderId = readText(root, "OrderId");
        if (orderId == null || orderId.isBlank()) {
            log.warn("T-Bank notification: missing OrderId");
            return;
        }

        PaymentOrderEntity order = paymentOrderRepository.findByTbankOrderId(orderId).orElse(null);
        if (order == null) {
            log.warn("T-Bank notification: unknown orderId={}", orderId);
            return;
        }

        String amountStr = readText(root, "Amount");
        if (amountStr != null && !amountStr.isBlank()) {
            try {
                long notifiedKopecks = Long.parseLong(amountStr);
                if (notifiedKopecks != order.getAmountKopecks()) {
                    log.error("T-Bank notification: amount mismatch orderId={} expected={} got={}",
                            orderId, order.getAmountKopecks(), notifiedKopecks);
                    order.setStatus(PaymentOrderStatus.FAILED);
                    paymentOrderRepository.save(order);
                    return;
                }
            } catch (NumberFormatException e) {
                log.warn("T-Bank notification: bad Amount {}", amountStr);
            }
        }

        boolean success = readBool(root, "Success");
        String errorCode = readText(root, "ErrorCode");
        String status = readText(root, "Status");

        if (order.getStatus() == PaymentOrderStatus.PAID) {
            log.debug("T-Bank notification: order already PAID orderId={}", orderId);
            return;
        }

        boolean paidLike = success && "0".equals(errorCode)
                && status != null
                && ("CONFIRMED".equalsIgnoreCase(status) || "AUTHORIZED".equalsIgnoreCase(status));

        if (paidLike) {
            order.setStatus(PaymentOrderStatus.PAID);
            String pid = readText(root, "PaymentId");
            if (pid != null && !pid.isBlank()) {
                order.setTbankPaymentId(pid);
            }
            paymentOrderRepository.save(order);
            log.info("Payment confirmed orderId={} paymentId={}", orderId, pid);
            shopStatsService.onPaymentConfirmed(order);
            applicationEventPublisher.publishEvent(new PaymentPaidGameEvent(
                    order.getTbankOrderId(),
                    order.getId(),
                    order.getNickname(),
                    order.getAmountKopecks(),
                    order.getProductType().name(),
                    order.getProductId(),
                    order.getSubscriptionPeriod() == null ? null : order.getSubscriptionPeriod().name(),
                    order.getQuantity(),
                    order.getTbankPaymentId()
            ));
            // Начисление на игровой сервер — WebSocket + идемпотентность на стороне плагина; при необходимости — REST-пуллинг из БД
        } else if (!success || (errorCode != null && !"0".equals(errorCode))) {
            order.setStatus(PaymentOrderStatus.FAILED);
            paymentOrderRepository.save(order);
            log.info("Payment failed orderId={} status={} code={}", orderId, status, errorCode);
        }
    }

    private static Map<String, String> scalarParamsForNotificationSignature(JsonNode root) {
        Map<String, String> map = new HashMap<>();
        root.fields().forEachRemaining(e -> {
            String key = e.getKey();
            if ("Token".equals(key)) {
                return;
            }
            JsonNode n = e.getValue();
            if (n == null || n.isNull() || n.isObject() || n.isArray()) {
                return;
            }
            map.put(key, n.asText());
        });
        return map;
    }

    private static String readText(JsonNode root, String field) {
        if (root == null || !root.has(field) || root.get(field).isNull()) {
            return null;
        }
        return root.get(field).asText();
    }

    private static boolean readBool(JsonNode root, String field) {
        if (root == null || !root.has(field) || root.get(field).isNull()) {
            return false;
        }
        JsonNode n = root.get(field);
        if (n.isBoolean()) {
            return n.booleanValue();
        }
        return "true".equalsIgnoreCase(n.asText());
    }

    private static PaymentProductType parseProductType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        try {
            return PaymentProductType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown product type: " + raw);
        }
    }

    private static RankSubscriptionPeriod parsePeriod(String raw, PaymentProductType type) {
        if (type != PaymentProductType.RANK) {
            return null;
        }
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("period is required for RANK");
        }
        try {
            return RankSubscriptionPeriod.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown period: " + raw);
        }
    }

    private static String requireNickname(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("nickname is required");
        }
        String n = raw.trim();
        if (!NICKNAME.matcher(n).matches()) {
            throw new IllegalArgumentException("invalid nickname");
        }
        return n;
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String e = email.trim();
        if (e.length() > 254) {
            throw new IllegalArgumentException("email too long");
        }
        if (!EMAIL.matcher(e).matches()) {
            throw new IllegalArgumentException("invalid email");
        }
        return e;
    }
}
