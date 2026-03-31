package com.back.minecraftback.payment.dto;

public record PaymentInitResponseDto(
        String paymentUrl,
        /** Идентификатор заказа (OrderId в Т‑Банке / внутренний). */
        String orderId
) {
}
