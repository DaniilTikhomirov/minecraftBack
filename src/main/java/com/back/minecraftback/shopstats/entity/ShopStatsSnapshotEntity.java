package com.back.minecraftback.shopstats.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "mc_backend", name = "shop_stats_snapshot")
@Getter
@Setter
public class ShopStatsSnapshotEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 128)
    private String label;

    @Column(name = "archived_at", nullable = false)
    private Instant archivedAt;

    @Column(name = "data_json", nullable = false, columnDefinition = "text")
    private String dataJson;
}
