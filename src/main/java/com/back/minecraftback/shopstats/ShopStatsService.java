package com.back.minecraftback.shopstats;

import com.back.minecraftback.payment.entity.PaymentOrderEntity;
import com.back.minecraftback.payment.model.PaymentOrderStatus;
import com.back.minecraftback.payment.model.PaymentProductType;
import com.back.minecraftback.shopstats.dto.ShopStatsDtos;
import com.back.minecraftback.shopstats.dto.ShopStatsDtos.*;
import com.back.minecraftback.shopstats.entity.*;
import com.back.minecraftback.shopstats.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopStatsService {

    public static final short META_ID = 1;
    private static final int MAX_SNAPSHOT_LABEL_CHARS = 128;
    private static final int MAX_JSON_CHARS = 2_000_000;

    private final ShopStatsMetaRepository metaRepository;
    private final ShopStatsProductRepository productRepository;
    private final ShopStatsCategoryRepository categoryRepository;
    private final ShopStatsMonthlyRepository monthlyRepository;
    private final ShopStatsSnapshotRepository snapshotRepository;
    private final ShopStatsProductLabelResolver labelResolver;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public void onPaymentConfirmed(PaymentOrderEntity order) {
        if (order == null || order.getStatus() != PaymentOrderStatus.PAID) {
            return;
        }
        if (order.getProductType() == PaymentProductType.SUNDRY) {
            log.debug("[shopStats] skip SUNDRY order");
            return;
        }

        String productKey = labelResolver.buildProductKey(order);
        if (productKey.length() > 384) {
            log.warn("[shopStats] productKey too long, truncated");
            productKey = productKey.substring(0, 384);
        }
        String displayLabel = labelResolver.resolveDisplayLabel(order);
        long units = order.getQuantity() != null && order.getQuantity() > 0 ? order.getQuantity() : 1L;
        long revenue = order.getAmountKopecks();

        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        YearMonth ym = YearMonth.from(now.atZone(ZoneOffset.UTC));
        String monthKey = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String monthLabel = formatMonthLabelRu(ym);

        upsertProduct(productKey, displayLabel, units, revenue);
        upsertCategory(order.getProductType().name(), units, revenue);
        upsertMonthly(productKey, monthKey, monthLabel, units, revenue);
        bumpMetaAfterPurchase(now);
    }

    private void upsertProduct(String key, String label, long units, long revenue) {
        jdbcTemplate.update("""
                        INSERT INTO mc_backend.shop_stats_product (product_key, display_label, purchase_count, revenue_kopecks)
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT (product_key) DO UPDATE SET
                            purchase_count = mc_backend.shop_stats_product.purchase_count + EXCLUDED.purchase_count,
                            revenue_kopecks = mc_backend.shop_stats_product.revenue_kopecks + EXCLUDED.revenue_kopecks,
                            display_label = EXCLUDED.display_label
                        """,
                key, label, units, revenue);
    }

    private void upsertCategory(String category, long units, long revenue) {
        jdbcTemplate.update("""
                        INSERT INTO mc_backend.shop_stats_category (category, purchase_count, revenue_kopecks)
                        VALUES (?, ?, ?)
                        ON CONFLICT (category) DO UPDATE SET
                            purchase_count = mc_backend.shop_stats_category.purchase_count + EXCLUDED.purchase_count,
                            revenue_kopecks = mc_backend.shop_stats_category.revenue_kopecks + EXCLUDED.revenue_kopecks
                        """,
                category, units, revenue);
    }

    private void upsertMonthly(String productKey, String monthKey, String monthLabel, long units, long revenue) {
        jdbcTemplate.update("""
                        INSERT INTO mc_backend.shop_stats_monthly (product_key, month_key, month_label, purchase_count, revenue_kopecks)
                        VALUES (?, ?, ?, ?, ?)
                        ON CONFLICT (product_key, month_key) DO UPDATE SET
                            purchase_count = mc_backend.shop_stats_monthly.purchase_count + EXCLUDED.purchase_count,
                            revenue_kopecks = mc_backend.shop_stats_monthly.revenue_kopecks + EXCLUDED.revenue_kopecks,
                            month_label = EXCLUDED.month_label
                        """,
                productKey, monthKey, monthLabel, units, revenue);
    }

    private void bumpMetaAfterPurchase(Instant now) {
        jdbcTemplate.update("""
                        UPDATE mc_backend.shop_stats_meta
                        SET completed_purchase_orders = completed_purchase_orders + 1,
                            updated_at = ?,
                            period_started_at = COALESCE(period_started_at, ?)
                        WHERE id = ?
                        """,
                now, now, (int) META_ID);
    }

    @Transactional(readOnly = true)
    public ShopStatsPayloadDto getCurrentPayload() {
        ShopStatsMetaEntity meta = metaRepository.findById(META_ID).orElseThrow();
        List<ShopStatsProductEntity> products = productRepository.findAll();
        List<ShopStatsCategoryEntity> categories = categoryRepository.findAll();
        List<ShopStatsMonthlyEntity> monthlyRows = monthlyRepository.findAll();

        return assemblePayload(meta, products, categories, monthlyRows);
    }

    private ShopStatsPayloadDto assemblePayload(
            ShopStatsMetaEntity meta,
            List<ShopStatsProductEntity> products,
            List<ShopStatsCategoryEntity> categories,
            List<ShopStatsMonthlyEntity> monthlyRows
    ) {
        long soldUnits = products.stream().mapToLong(ShopStatsProductEntity::getPurchaseCount).sum();
        long revenue = products.stream().mapToLong(ShopStatsProductEntity::getRevenueKopecks).sum();

        ShopStatsTopProductDto topProduct = null;
        Optional<ShopStatsProductEntity> best = products.stream()
                .max(Comparator
                        .comparingLong(ShopStatsProductEntity::getPurchaseCount)
                        .thenComparingLong(ShopStatsProductEntity::getRevenueKopecks));
        if (best.isPresent()) {
            ShopStatsProductEntity p = best.get();
            topProduct = new ShopStatsTopProductDto(p.getDisplayLabel(), p.getPurchaseCount());
        }

        List<ShopStatsTopEntryDto> topProducts = products.stream()
                .sorted(Comparator
                        .comparingLong(ShopStatsProductEntity::getPurchaseCount).reversed()
                        .thenComparingLong(ShopStatsProductEntity::getRevenueKopecks).reversed())
                .map(p -> new ShopStatsTopEntryDto(p.getDisplayLabel(), p.getPurchaseCount()))
                .toList();

        List<ShopStatsCategoryDto> byCategory = categories.stream()
                .sorted(Comparator
                        .comparingLong(ShopStatsCategoryEntity::getPurchaseCount).reversed()
                        .thenComparingLong(ShopStatsCategoryEntity::getRevenueKopecks).reversed())
                .map(c -> new ShopStatsCategoryDto(c.getCategory(), c.getPurchaseCount(), c.getRevenueKopecks()))
                .toList();

        Map<String, List<ShopStatsMonthlyEntity>> byProduct = monthlyRows.stream()
                .collect(Collectors.groupingBy(ShopStatsMonthlyEntity::getProductKey));

        Map<String, String> productLabels = products.stream()
                .collect(Collectors.toMap(ShopStatsProductEntity::getProductKey, ShopStatsProductEntity::getDisplayLabel, (a, b) -> a));

        List<ShopStatsProductSeriesDto> series = new ArrayList<>(products.stream()
                .sorted(Comparator.comparing(ShopStatsProductEntity::getDisplayLabel, String.CASE_INSENSITIVE_ORDER))
                .map(p -> {
                    List<ShopStatsMonthPointDto> points = byProduct.getOrDefault(p.getProductKey(), List.of()).stream()
                            .sorted(Comparator.comparing(ShopStatsMonthlyEntity::getMonthKey))
                            .map(m -> new ShopStatsMonthPointDto(
                                    m.getMonthKey(),
                                    m.getMonthLabel(),
                                    m.getPurchaseCount(),
                                    m.getRevenueKopecks()
                            ))
                            .toList();
                    return new ShopStatsProductSeriesDto(p.getDisplayLabel(), points);
                })
                .toList());

        for (Map.Entry<String, List<ShopStatsMonthlyEntity>> e : byProduct.entrySet()) {
            if (productLabels.containsKey(e.getKey())) {
                continue;
            }
            List<ShopStatsMonthPointDto> points = e.getValue().stream()
                    .sorted(Comparator.comparing(ShopStatsMonthlyEntity::getMonthKey))
                    .map(m -> new ShopStatsMonthPointDto(
                            m.getMonthKey(),
                            m.getMonthLabel(),
                            m.getPurchaseCount(),
                            m.getRevenueKopecks()
                    ))
                    .toList();
            series.add(new ShopStatsProductSeriesDto(e.getKey(), points));
        }

        Double conversion = null;
        if (meta.getVisitCount() > 0) {
            double raw = 100.0 * meta.getCompletedPurchaseOrders() / (double) meta.getVisitCount();
            double capped = Math.min(100.0, raw);
            conversion = Math.round(capped * 100.0) / 100.0;
        }

        return new ShopStatsPayloadDto(
                meta.getUpdatedAt(),
                meta.getPeriodStartedAt(),
                topProduct,
                soldUnits,
                revenue,
                conversion,
                topProducts,
                byCategory,
                series
        );
    }

    @Transactional(readOnly = true)
    public ShopStatsSnapshotsResponseDto listSnapshots() {
        List<ShopStatsSnapshotItemDto> list = snapshotRepository.findAllByOrderByArchivedAtDesc().stream()
                .map(this::toSnapshotItem)
                .toList();
        return new ShopStatsSnapshotsResponseDto(list);
    }

    private ShopStatsSnapshotItemDto toSnapshotItem(ShopStatsSnapshotEntity e) {
        ShopStatsPayloadDto data = readPayloadJson(e.getDataJson());
        return new ShopStatsSnapshotItemDto(e.getId().toString(), e.getLabel(), e.getArchivedAt(), data);
    }

    private ShopStatsPayloadDto readPayloadJson(String json) {
        if (json == null || json.isBlank()) {
            return emptyPayload(Instant.now());
        }
        try {
            return objectMapper.readValue(json, ShopStatsPayloadDto.class);
        } catch (JsonProcessingException ex) {
            log.warn("[shopStats] snapshot JSON parse failed, returning empty data");
            return emptyPayload(Instant.now());
        }
    }

    private static ShopStatsPayloadDto emptyPayload(Instant updatedAt) {
        return new ShopStatsPayloadDto(
                updatedAt,
                null,
                null,
                0L,
                0L,
                null,
                List.of(),
                List.of(),
                List.of()
        );
    }

    @Transactional
    public void resetCurrentPeriod(Optional<String> labelOptional) throws JsonProcessingException {
        ShopStatsMetaEntity meta = metaRepository.findByIdForUpdate(META_ID).orElseThrow();

        ShopStatsPayloadDto snapshotPayload = getCurrentPayload();
        String json = objectMapper.writeValueAsString(snapshotPayload);
        if (json.length() > MAX_JSON_CHARS) {
            throw new IllegalStateException("snapshot payload too large");
        }

        Instant archivedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        String label = resolveArchiveLabel(labelOptional, archivedAt);

        ShopStatsSnapshotEntity snap = new ShopStatsSnapshotEntity();
        snap.setId(UUID.randomUUID());
        snap.setLabel(label);
        snap.setArchivedAt(archivedAt);
        snap.setDataJson(json);
        snapshotRepository.save(snap);

        monthlyRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();

        meta.setPeriodStartedAt(archivedAt);
        meta.setCompletedPurchaseOrders(0);
        meta.setUpdatedAt(archivedAt);
        metaRepository.save(meta);
    }

    private static String resolveArchiveLabel(Optional<String> labelOptional, Instant archivedAt) {
        String sanitized = labelOptional.map(ShopStatsService::sanitizeSnapshotLabel).orElse(null);
        if (sanitized != null && !sanitized.isBlank()) {
            return sanitized;
        }
        String iso = archivedAt.atZone(ZoneOffset.UTC).toLocalDate().toString();
        return "period-" + iso;
    }

    private static String sanitizeSnapshotLabel(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.length() > MAX_SNAPSHOT_LABEL_CHARS) {
            t = t.substring(0, MAX_SNAPSHOT_LABEL_CHARS);
        }
        StringBuilder sb = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c >= 32 || c == '\t') {
                sb.append(c);
            }
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? null : out;
    }

    @Transactional
    public boolean deleteSnapshot(UUID id) {
        if (!snapshotRepository.existsById(id)) {
            return false;
        }
        snapshotRepository.deleteById(id);
        return true;
    }

    private static String formatMonthLabelRu(YearMonth ym) {
        Locale ru = Locale.forLanguageTag("ru-RU");
        String m = ym.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, ru);
        if (m.endsWith(".")) {
            m = m.substring(0, m.length() - 1);
        }
        return m + " " + ym.getYear();
    }
}
