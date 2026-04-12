package com.back.minecraftback.shopstats.repository;

import com.back.minecraftback.shopstats.entity.ShopStatsProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopStatsProductRepository extends JpaRepository<ShopStatsProductEntity, String> {
}
