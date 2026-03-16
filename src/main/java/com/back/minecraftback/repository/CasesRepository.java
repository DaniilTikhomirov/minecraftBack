package com.back.minecraftback.repository;

import com.back.minecraftback.entity.CasesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CasesRepository extends JpaRepository<CasesEntity, Long> {

    List<CasesEntity> findAllByActiveIsTrue();

    @Query("SELECT e FROM CasesEntity e WHERE e.active IS NULL OR e.active = false")
    List<CasesEntity> findAllInactive();
}
