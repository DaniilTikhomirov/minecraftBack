package com.back.minecraftback.repository;


import com.back.minecraftback.entity.RankCardsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RankCardsRepository extends JpaRepository<RankCardsEntity, Long> {
    List<RankCardsEntity> findAllByActiveIsTrue();

    /** Все неактивные: active = false или active IS NULL. */
    @Query("SELECT e FROM RankCardsEntity e WHERE (e.active = false OR e.active IS NULL)")
    List<RankCardsEntity> findAllInactive();
}
