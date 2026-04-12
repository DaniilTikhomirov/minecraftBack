package com.back.minecraftback.shopstats.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(schema = "mc_backend", name = "shop_stats_meta")
@Getter
@Setter
public class ShopStatsMetaEntity {

    @Id
    @Column(nullable = false)
    private Short id;

    @Column(name = "period_started_at")
    private Instant periodStartedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "visit_count", nullable = false)
    private long visitCount;

    @Column(name = "completed_purchase_orders", nullable = false)
    private long completedPurchaseOrders;
}
