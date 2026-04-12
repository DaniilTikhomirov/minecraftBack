package com.back.minecraftback.shopstats.repository;

import com.back.minecraftback.shopstats.entity.ShopStatsCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopStatsCategoryRepository extends JpaRepository<ShopStatsCategoryEntity, String> {
}
