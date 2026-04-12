package com.back.minecraftback.shopstats.repository;

import com.back.minecraftback.shopstats.entity.ShopStatsMonthlyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopStatsMonthlyRepository extends JpaRepository<ShopStatsMonthlyEntity, ShopStatsMonthlyEntity.Pk> {
}
