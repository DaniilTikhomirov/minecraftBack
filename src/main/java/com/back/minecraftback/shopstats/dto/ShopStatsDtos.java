package com.back.minecraftback.shopstats.dto;

import java.time.Instant;
import java.util.List;

public final class ShopStatsDtos {

    private ShopStatsDtos() {
    }

    public record ShopStatsMonthPointDto(
            String key,
            String label,
            long count,
            long revenueKopecks
    ) {
    }

    public record ShopStatsProductSeriesDto(
            String label,
            List<ShopStatsMonthPointDto> series
    ) {
    }

    public record ShopStatsTopProductDto(
            String label,
            long purchaseCount
    ) {
    }

    public record ShopStatsTopEntryDto(
            String label,
            long count
    ) {
    }

    public record ShopStatsCategoryDto(
            String category,
            long purchaseCount,
            long revenueKopecks
    ) {
    }

    public record ShopStatsPayloadDto(
            Instant updatedAt,
            Instant periodStartedAt,
            ShopStatsTopProductDto topProduct,
            long soldUnitsTotal,
            long revenueKopecks,
            Double conversionPercent,
            List<ShopStatsTopEntryDto> topProducts,
            List<ShopStatsCategoryDto> byCategory,
            List<ShopStatsProductSeriesDto> productMonthlySeries
    ) {
    }

    public record ShopStatsSnapshotItemDto(
            String id,
            String label,
            Instant archivedAt,
            ShopStatsPayloadDto data
    ) {
    }

    public record ShopStatsSnapshotsResponseDto(
            List<ShopStatsSnapshotItemDto> snapshots
    ) {
    }

    public record ShopStatsResetRequestDto(
            String label
    ) {
    }
}
