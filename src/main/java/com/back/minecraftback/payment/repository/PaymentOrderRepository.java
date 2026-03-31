package com.back.minecraftback.payment.repository;

import com.back.minecraftback.payment.entity.PaymentOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrderEntity, UUID> {

    Optional<PaymentOrderEntity> findByTbankOrderId(String tbankOrderId);
}
