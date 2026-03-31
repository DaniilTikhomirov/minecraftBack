package com.back.minecraftback.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tbank.acquiring")
public record TbankAcquiringProperties(
        String baseUrl,
        String terminalKey,
        String password,
        String successUrl,
        String failUrl,
        String notificationUrl
) {
    public boolean isConfigured() {
        return terminalKey != null && !terminalKey.isBlank()
                && password != null && !password.isBlank()
                && baseUrl != null && !baseUrl.isBlank();
    }
}
