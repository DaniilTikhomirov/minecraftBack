package com.back.minecraftback.payment.service;

import com.back.minecraftback.entity.CasesEntity;
import com.back.minecraftback.entity.RankCardsEntity;
import com.back.minecraftback.entity.SundryEntity;
import com.back.minecraftback.payment.model.PaymentProductType;
import com.back.minecraftback.payment.model.RankSubscriptionPeriod;
import com.back.minecraftback.repository.CasesRepository;
import com.back.minecraftback.repository.RankCardsRepository;
import com.back.minecraftback.repository.SundryRepository;
import com.back.minecraftback.repository.ExchangeRateRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class PaymentPricingService {

    private final CasesRepository casesRepository;
    private final RankCardsRepository rankCardsRepository;
    private final SundryRepository sundryRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    /**
     * Курс в БД трактуем как «рублей за одну единицу внутриигровой валюты» (как в админке курса).
     */
    public long computeAmountKopecks(
            PaymentProductType type,
            Long itemId,
            Integer quantity,
            RankSubscriptionPeriod period
    ) {
        return switch (type) {
            case CURRENCY -> priceCurrencyKopecks(quantity);
            case CASE -> priceCaseKopecks(itemId, quantity);
            case RANK -> priceRankKopecks(itemId, period);
            case SUNDRY -> priceSundryKopecks(itemId, quantity);
        };
    }

    private long priceCurrencyKopecks(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive for CURRENCY");
        }
        var list = exchangeRateRepository.findAll();
        if (list.isEmpty()) {
            throw new EntityNotFoundException("exchange rate not configured");
        }
        BigDecimal rateRubPerUnit = list.getFirst().getRate();
        if (rateRubPerUnit == null || rateRubPerUnit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("invalid exchange rate");
        }
        BigDecimal totalRub = rateRubPerUnit.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        return totalRub.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private long priceCaseKopecks(Long caseId, Integer quantity) {
        if (caseId == null || caseId <= 0) {
            throw new IllegalArgumentException("itemId is required for CASE");
        }
        int qty = requirePositiveQuantity(quantity, "CASE");
        CasesEntity c = casesRepository.findById(caseId)
                .orElseThrow(() -> new EntityNotFoundException("case not found"));
        if (!Boolean.TRUE.equals(c.getActive())) {
            throw new IllegalArgumentException("case is not available for purchase");
        }
        Integer rub = c.getPrice();
        if (rub == null || rub <= 0) {
            throw new IllegalArgumentException("case has no valid price");
        }
        return Math.multiplyExact(rub * 100L, qty);
    }

    private long priceRankKopecks(Long rankId, RankSubscriptionPeriod period) {
        if (rankId == null || rankId <= 0) {
            throw new IllegalArgumentException("itemId is required for RANK");
        }
        if (period == null) {
            throw new IllegalArgumentException("period is required for RANK");
        }
        RankCardsEntity r = rankCardsRepository.findById(rankId)
                .orElseThrow(() -> new EntityNotFoundException("rank card not found"));
        if (!Boolean.TRUE.equals(r.getActive())) {
            throw new IllegalArgumentException("rank is not available for purchase");
        }
        int rub = switch (period) {
            case MONTH -> {
                if (!isPeriodAllowed(r.getAllowMonth())) {
                    throw new IllegalArgumentException("month purchase is not allowed for this rank");
                }
                yield requirePositive(r.getPriceMonth(), "priceMonth");
            }
            case THREE_MONTHS -> {
                if (!isPeriodAllowed(r.getAllowThreeMonths())) {
                    throw new IllegalArgumentException("three months purchase is not allowed for this rank");
                }
                yield requirePositive(r.getPriceThreeMonths(), "priceThreeMonths");
            }
            case YEAR -> {
                if (!isPeriodAllowed(r.getAllowYear())) {
                    throw new IllegalArgumentException("year purchase is not allowed for this rank");
                }
                yield requirePositive(r.getPriceYear(), "priceYear");
            }
            case FOREVER -> {
                if (!isPeriodAllowed(r.getAllowForever())) {
                    throw new IllegalArgumentException("forever purchase is not allowed for this rank");
                }
                yield requirePositive(r.getPriceForever(), "priceForever");
            }
        };
        return rub * 100L;
    }

    private long priceSundryKopecks(Long itemId, Integer quantity) {
        if (itemId == null || itemId <= 0) {
            throw new IllegalArgumentException("itemId is required for SUNDRY");
        }
        int qty = requirePositiveQuantity(quantity, "SUNDRY");
        SundryEntity item = sundryRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("sundry item not found"));
        if (!Boolean.TRUE.equals(item.getActive())) {
            throw new IllegalArgumentException("sundry item is not available for purchase");
        }
        Integer rub = item.getPrice();
        if (rub == null || rub <= 0) {
            throw new IllegalArgumentException("sundry item has no valid price");
        }
        return Math.multiplyExact(rub * 100L, qty);
    }

    /** null трактуем как включённый срок (совместимость со старыми строками до миграции). */
    private static boolean isPeriodAllowed(Boolean allowed) {
        return allowed == null || Boolean.TRUE.equals(allowed);
    }

    private static int requirePositiveQuantity(Integer quantity, String type) {
        int qty = quantity == null ? 1 : quantity;
        if (qty <= 0) {
            throw new IllegalArgumentException("quantity must be positive for " + type);
        }
        return qty;
    }

    private static int requirePositive(Integer v, String field) {
        if (v == null || v <= 0) {
            throw new IllegalArgumentException("invalid rank " + field);
        }
        return v;
    }
}
