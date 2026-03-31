package com.back.minecraftback.payment.model;

/**
 * Тип покупки: цена на сервере пересчитывается по БД, клиентский amount не доверяем.
 */
public enum PaymentProductType {
    /** Покупка внутриигровой валюты по курсу из exchange_rate. */
    CURRENCY,
    /** Кейс: цена из cases.price (рубли целые → копейки). */
    CASE,
    /** Привилегия: цена по периоду подписки из rank_cards. */
    RANK,
    /** Зарезервировано под каталог; пока не реализовано. */
    SUNDRY
}
