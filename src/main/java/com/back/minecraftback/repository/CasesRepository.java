package com.back.minecraftback.repository;

import com.back.minecraftback.entity.CasesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CasesRepository extends JpaRepository<CasesEntity, Long> {

    List<CasesEntity> findAllByActiveIsTrue();
}
