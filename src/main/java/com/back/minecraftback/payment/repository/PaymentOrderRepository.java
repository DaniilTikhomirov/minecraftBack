package com.back.minecraftback.payment.repository;

import com.back.minecraftback.payment.entity.PaymentOrderEntity;
import com.back.minecraftback.payment.model.PaymentOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrderEntity, UUID> {

    Optional<PaymentOrderEntity> findByTbankOrderId(String tbankOrderId);

    List<PaymentOrderEntity> findTop50ByStatusAndGameNotifiedAtIsNullOrderByCreatedAtAsc(PaymentOrderStatus status);
}
