package com.back.minecraftback.configuration;

import com.back.minecraftback.entity.AdminUsersEntity;
import com.back.minecraftback.entity.ExchangeRateEntity;
import com.back.minecraftback.model.Role;
import com.back.minecraftback.repository.ExchangeRateRepository;
import com.back.minecraftback.service.AdminUsersService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
@RequiredArgsConstructor
public class InitDb {
    private final AdminUsersService adminUsersService;

    @Value("${super.admin.default.username}")
    private String username;
    @Value("${super.admin.default.password}")
    private String password;

    private final PasswordEncoder passwordEncoder;
    private final ExchangeRateRepository exchangeRateRepository;

    @PostConstruct
    public void createDefaultSuperAdmin() {
        if(adminUsersService.existsByUsername(username))
            return;
        AdminUsersEntity adminUsersEntity = new AdminUsersEntity();
        adminUsersEntity.setUsername(username);
        adminUsersEntity.setPassword(passwordEncoder.encode(password));
        adminUsersEntity.setEnabled(true);
        adminUsersEntity.setRole(Role.SUPER_ADMIN);
        adminUsersService.save(adminUsersEntity);
    }

    @PostConstruct
    public void createDefaultRate() {
        if(!exchangeRateRepository.findAll().isEmpty()){
            return;
        }
        ExchangeRateEntity entity = new ExchangeRateEntity();
        entity.setId(1);
        entity.setRate(BigDecimal.ONE);
        exchangeRateRepository.save(entity);
    }
}
