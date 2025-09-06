package com.back.minecraftback.repository;

import com.back.minecraftback.entity.ExchangeRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, Integer> {
}
