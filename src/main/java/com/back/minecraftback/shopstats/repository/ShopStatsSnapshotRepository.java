package com.back.minecraftback.shopstats.repository;

import com.back.minecraftback.shopstats.entity.ShopStatsSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShopStatsSnapshotRepository extends JpaRepository<ShopStatsSnapshotEntity, UUID> {

    List<ShopStatsSnapshotEntity> findAllByOrderByArchivedAtDesc();
}
