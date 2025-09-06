package com.back.minecraftback.repository;

import com.back.minecraftback.entity.MainNewsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MainNewsRepository extends JpaRepository<MainNewsEntity, Long> {
    List<MainNewsEntity> findAllByActiveIsTrue();
}
