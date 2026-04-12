package com.back.minecraftback.shopstats.repository;

import com.back.minecraftback.shopstats.entity.ShopStatsMetaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ShopStatsMetaRepository extends JpaRepository<ShopStatsMetaEntity, Short> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM ShopStatsMetaEntity m WHERE m.id = :id")
    Optional<ShopStatsMetaEntity> findByIdForUpdate(@Param("id") short id);
}
