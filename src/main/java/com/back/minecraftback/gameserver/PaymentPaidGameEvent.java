package com.back.minecraftback.gameserver;

import java.util.UUID;

/**
 * Событие после успешной оплаты; обрабатывается только после commit транзакции,
 * чтобы плагин не получил уведомление раньше записи в БД.
 */
public record PaymentPaidGameEvent(
        String tbankOrderId,
        UUID internalOrderId,
        String nickname,
        long amountKopecks,
        String productType,
        Long productId,
        String subscriptionPeriod,
        Integer quantity,
        String tbankPaymentId
) {
}
