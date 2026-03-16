package com.back.minecraftback.repository;

import com.back.minecraftback.entity.MainNewsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MainNewsRepository extends JpaRepository<MainNewsEntity, Long> {
    List<MainNewsEntity> findAllByActiveIsTrue();

    @Query("SELECT e FROM MainNewsEntity e WHERE e.active IS NULL OR e.active = false")
    List<MainNewsEntity> findAllInactive();
}
