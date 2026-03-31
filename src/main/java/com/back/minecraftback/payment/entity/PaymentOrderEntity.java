package com.back.minecraftback.payment.entity;

import com.back.minecraftback.payment.model.PaymentOrderStatus;
import com.back.minecraftback.payment.model.PaymentProductType;
import com.back.minecraftback.payment.model.RankSubscriptionPeriod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "mc_backend", name = "payment_order")
@Getter
@Setter
public class PaymentOrderEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tbank_order_id", nullable = false, unique = true, length = 64)
    private String tbankOrderId;

    @Column(nullable = false, length = 500)
    private String nickname;

    @Column(length = 255)
    private String email;

    @Column(name = "amount_kopecks", nullable = false)
    private long amountKopecks;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 32)
    private PaymentProductType productType;

    @Column(name = "product_id")
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_period", length = 32)
    private RankSubscriptionPeriod subscriptionPeriod;

    private Integer quantity;

    @Column(name = "tbank_payment_id", length = 64)
    private String tbankPaymentId;

    @Column(name = "payment_url", length = 2000)
    private String paymentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentOrderStatus status;

    @Column(name = "raw_init_response", columnDefinition = "text")
    private String rawInitResponse;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
