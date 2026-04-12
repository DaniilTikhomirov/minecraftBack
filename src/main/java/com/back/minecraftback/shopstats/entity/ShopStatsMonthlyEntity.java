package com.back.minecraftback.shopstats.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(schema = "mc_backend", name = "shop_stats_monthly")
@Getter
@Setter
@IdClass(ShopStatsMonthlyEntity.Pk.class)
public class ShopStatsMonthlyEntity {

    @Id
    @Column(name = "product_key", length = 384)
    private String productKey;

    @Id
    @Column(name = "month_key", length = 7)
    private String monthKey;

    @Column(name = "month_label", nullable = false, length = 64)
    private String monthLabel;

    @Column(name = "purchase_count", nullable = false)
    private long purchaseCount;

    @Column(name = "revenue_kopecks", nullable = false)
    private long revenueKopecks;

    @Getter
    @Setter
    @lombok.EqualsAndHashCode
    public static class Pk implements java.io.Serializable {
        private String productKey;
        private String monthKey;
    }
}
