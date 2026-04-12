package com.back.minecraftback.shopstats.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(schema = "mc_backend", name = "shop_stats_category")
@Getter
@Setter
public class ShopStatsCategoryEntity {

    @Id
    @Column(length = 32)
    private String category;

    @Column(name = "purchase_count", nullable = false)
    private long purchaseCount;

    @Column(name = "revenue_kopecks", nullable = false)
    private long revenueKopecks;
}
