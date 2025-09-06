package com.back.minecraftback.repository;

import com.back.minecraftback.entity.MainNewsEntity;
import com.back.minecraftback.entity.MiniNewsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MiniNewsRepository extends JpaRepository<MiniNewsEntity, Long> {
    List<MiniNewsEntity> findAllByActiveIsTrue();
}
