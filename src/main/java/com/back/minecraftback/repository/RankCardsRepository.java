package com.back.minecraftback.repository;


import com.back.minecraftback.entity.RankCardsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RankCardsRepository extends JpaRepository<RankCardsEntity, Long> {
    List<RankCardsEntity> findAllByActiveIsTrue();
}
