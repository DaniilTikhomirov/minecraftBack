package com.back.minecraftback.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(schema = "mc_backend", name = "exchange_rate")
@Getter
@Setter
public class ExchangeRateEntity {
    @Id
    private int id;

    private BigDecimal rate;
}
