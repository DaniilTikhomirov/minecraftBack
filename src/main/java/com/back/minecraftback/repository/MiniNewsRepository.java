package com.back.minecraftback.repository;

import com.back.minecraftback.entity.MiniNewsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MiniNewsRepository extends JpaRepository<MiniNewsEntity, Long> {
    List<MiniNewsEntity> findAllByActiveIsTrue();

    @Query("SELECT e FROM MiniNewsEntity e WHERE e.active IS NULL OR e.active = false")
    List<MiniNewsEntity> findAllInactive();
}
