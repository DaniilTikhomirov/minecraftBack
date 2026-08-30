package com.back.minecraftback.repository;

import com.back.minecraftback.entity.SundryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SundryRepository extends JpaRepository<SundryEntity, Long> {

    List<SundryEntity> findAllByActiveIsTrue();

    @Query("SELECT e FROM SundryEntity e WHERE e.active IS NULL OR e.active = false")
    List<SundryEntity> findAllInactive();
}
