package com.back.minecraftback.repository;


import com.back.minecraftback.entity.RankCardsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RankCardsRepository extends JpaRepository<RankCardsEntity, Long> {
    List<RankCardsEntity> findAllByActiveIsTrue();

    /** Все неактивные: active = false или active IS NULL (старые записи). */
    @Query("SELECT e FROM RankCardsEntity e WHERE e.active IS NULL OR e.active = false")
    List<RankCardsEntity> findAllInactive();
}
