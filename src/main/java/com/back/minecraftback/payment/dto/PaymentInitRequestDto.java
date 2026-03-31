package com.back.minecraftback.payment.dto;

/**
 * Запрос на создание платежа. Сумма с клиента не используется — пересчитывается на сервере.
 */
public record PaymentInitRequestDto(
        String nickname,
        String email,
        /** CURRENCY, CASE, RANK, SUNDRY (регистр не важен). */
        String type,
        /** Для CURRENCY — количество единиц валюты (хромы и т.п.). */
        Integer quantity,
        /** Для CASE / RANK — id сущности в БД. */
        Long itemId,
        /** Для RANK: MONTH, THREE_MONTHS, YEAR, FOREVER. */
        String period
) {
}
