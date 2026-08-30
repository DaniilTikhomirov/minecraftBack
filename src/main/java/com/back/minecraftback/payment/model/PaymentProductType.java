package com.back.minecraftback.payment.model;

/**
 * Тип покупки: цена на сервере пересчитывается по БД, клиентский amount не доверяем.
 */
public enum PaymentProductType {
    /** Покупка внутриигровой валюты по курсу из exchange_rate. */
    CURRENCY,
    /** Кейс: цена из cases.price (рубли целые → копейки), умножается на quantity. */
    CASE,
    /** Привилегия: цена по периоду подписки из rank_cards. */
    RANK,
    /** Позиция каталога «прочее»: цена из sundry.price (рубли целые → копейки), опционально quantity. */
    SUNDRY
}
