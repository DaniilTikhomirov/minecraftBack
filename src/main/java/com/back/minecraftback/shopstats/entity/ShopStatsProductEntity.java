package com.back.minecraftback.shopstats.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(schema = "mc_backend", name = "shop_stats_product")
@Getter
@Setter
public class ShopStatsProductEntity {

    @Id
    @Column(name = "product_key", length = 384)
    private String productKey;

    @Column(name = "display_label", nullable = false, length = 500)
    private String displayLabel;

    @Column(name = "purchase_count", nullable = false)
    private long purchaseCount;

    @Column(name = "revenue_kopecks", nullable = false)
    private long revenueKopecks;
}
