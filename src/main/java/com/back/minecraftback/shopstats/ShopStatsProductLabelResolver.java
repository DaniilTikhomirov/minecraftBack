package com.back.minecraftback.shopstats;

import com.back.minecraftback.entity.CasesEntity;
import com.back.minecraftback.entity.RankCardsEntity;
import com.back.minecraftback.payment.entity.PaymentOrderEntity;
import com.back.minecraftback.payment.model.PaymentProductType;
import com.back.minecraftback.payment.model.RankSubscriptionPeriod;
import com.back.minecraftback.repository.CasesRepository;
import com.back.minecraftback.repository.RankCardsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShopStatsProductLabelResolver {

    private static final int LABEL_MAX = 500;

    private final RankCardsRepository rankCardsRepository;
    private final CasesRepository casesRepository;

    public String buildProductKey(PaymentOrderEntity order) {
        return switch (order.getProductType()) {
            case RANK -> "RANK:" + order.getProductId() + ":" + order.getSubscriptionPeriod();
            case CASE -> "CASE:" + order.getProductId();
            case CURRENCY -> "CURRENCY";
            case SUNDRY -> "SUNDRY";
        };
    }

    public String resolveDisplayLabel(PaymentOrderEntity order) {
        try {
            String raw = switch (order.getProductType()) {
                case RANK -> labelRank(order);
                case CASE -> labelCase(order);
                case CURRENCY -> "Внутриигровая валюта";
                case SUNDRY -> "Прочее";
            };
            return truncate(raw, LABEL_MAX);
        } catch (RuntimeException e) {
            return truncate(fallbackLabel(order), LABEL_MAX);
        }
    }

    private String labelRank(PaymentOrderEntity order) {
        Long id = order.getProductId();
        RankSubscriptionPeriod period = order.getSubscriptionPeriod();
        String periodRu = rankPeriodRu(period);
        if (id == null) {
            return "Ранг — " + periodRu;
        }
        String title = rankCardsRepository.findById(id)
                .map(RankCardsEntity::getTitle)
                .filter(t -> t != null && !t.isBlank())
                .orElse("Ранг #" + id);
        return title + " — " + periodRu;
    }

    private static String rankPeriodRu(RankSubscriptionPeriod period) {
        if (period == null) {
            return "?";
        }
        return switch (period) {
            case MONTH -> "1 мес.";
            case THREE_MONTHS -> "3 мес.";
            case YEAR -> "1 год";
            case FOREVER -> "навсегда";
        };
    }

    private String labelCase(PaymentOrderEntity order) {
        Long id = order.getProductId();
        if (id == null) {
            return "Кейс";
        }
        return casesRepository.findById(id)
                .map(CasesEntity::getTitle)
                .filter(t -> t != null && !t.isBlank())
                .orElse("Кейс #" + id);
    }

    private static String fallbackLabel(PaymentOrderEntity order) {
        PaymentProductType t = order.getProductType();
        if (t == PaymentProductType.RANK) {
            return "Ранг " + order.getProductId();
        }
        if (t == PaymentProductType.CASE) {
            return "Кейс " + order.getProductId();
        }
        return t.name();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
