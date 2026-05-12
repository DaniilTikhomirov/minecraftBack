package com.back.minecraftback.payment.config;

import com.back.minecraftback.gameserver.GameServerValidationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({TbankAcquiringProperties.class, GameServerValidationProperties.class})
public class PaymentConfig {
}
