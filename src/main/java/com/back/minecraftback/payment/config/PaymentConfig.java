package com.back.minecraftback.payment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TbankAcquiringProperties.class)
public class PaymentConfig {
}
