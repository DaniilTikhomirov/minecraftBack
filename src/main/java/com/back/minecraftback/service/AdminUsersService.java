package com.back.minecraftback.service;

import com.back.minecraftback.dto.CreateAdminDTO;
import com.back.minecraftback.entity.AdminUsersEntity;
import com.back.minecraftback.mapper.AdminMapper;
import com.back.minecraftback.model.Role;
import com.back.minecraftback.repository.AdminUsersRepository;
import com.back.minecraftback.util.AdminUsernamePolicy;
import com.back.minecraftback.util.JwtUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.back.minecraftback.model.Token.JWT_TOKEN;
import static com.back.minecraftback.model.Token.REFRESH_TOKEN;
import static com.back.minecraftback.model.TokenTime.JWT_TOKEN_TIME_IN_SECONDS;
import static com.back.minecraftback.model.TokenTime.REFRESH_TOKEN_TIME_IN_SECONDS;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUsersService {

    private final AdminUsersRepository adminUsersRepository;
    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminUsersDetailService adminUsersDetailService;
    private final JwtUtil jwtUtil;

    public void save(AdminUsersEntity adminUsersEntity) {
        adminUsersRepository.save(adminUsersEntity);
    }

    public boolean existsByUsername(String username) {
        return adminUsersRepository.existsByUsername(username);
    }

    /** Есть ли в БД хотя бы один админ (для восстановления при удалении всех). */
    public boolean hasAnyAdmin() {
        return adminUsersRepository.count() > 0;
    }

    public void save(CreateAdminDTO createAdminDTO) {
        log.info("[save] start CreateAdminDTO username='{}' role={}", createAdminDTO != null ? createAdminDTO.username() : null, createAdminDTO != null ? createAdminDTO.role() : null);

        if (createAdminDTO == null) {
            log.error("[save] DTO is null");
            throw new IllegalArgumentException("createAdminDTO is null");
        }

        String username = AdminUsernamePolicy.requireValidUsername(createAdminDTO.username());
        String password = createAdminDTO.password();
        Role role = createAdminDTO.role();

        if (password == null || password.isBlank()) {
            log.warn("[save] password empty");
            throw new IllegalArgumentException("password is required");
        }
        if (role == null) {
            log.warn("[save] role null");
            throw new IllegalArgumentException("role is required (ADMIN or SUPER_ADMIN)");
        }

        log.debug("[save] mapping DTO to entity");
        AdminUsersEntity entity = adminMapper.toEntity(createAdminDTO);
        entity.setUsername(username);

        log.debug("[save] encoding password");
        String encodedPassword = passwordEncoder.encode(password);
        entity.setPassword(encodedPassword);

        log.info("[save] saving entity username='{}' to repository", username);
        adminUsersRepository.save(entity);
        log.info("[save] admin saved successfully username='{}'", username);
    }

    public boolean swapEnabled(String username) {
        AdminUsersEntity adminUsersEntity = adminUsersRepository.findByUsername(username).orElse(null);
        if (adminUsersEntity == null)
            return false;

        adminUsersEntity.setEnabled(!adminUsersEntity.getEnabled());
        adminUsersRepository.save(adminUsersEntity);
        return true;
    }

    public List<String> getUsernames(Optional<Boolean> isEnabled) {
        List<AdminUsersEntity> adminUsersEntities;
        if (isEnabled.isEmpty())
            adminUsersEntities = adminUsersRepository.findAll();
        else
            adminUsersEntities = adminUsersRepository.findAllByEnabled(isEnabled.get());
        return adminUsersEntities.stream().map(AdminUsersEntity::getUsername).collect(Collectors.toList());
    }

    public boolean authenticate(String username, String password, HttpServletResponse response) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return false;
        }
        if (adminUsersRepository.findByUsername(username)
                .map(user -> passwordEncoder.matches(password, user.getPassword()) && user.getEnabled())
                .orElse(false)) {

            UserDetails userDetails = adminUsersDetailService.loadUserByUsername(username);
            String jwt = jwtUtil.generateJwtToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            // Единообразно выставляем cookie через Set-Cookie,
            // чтобы гарантированно передавать SameSite=None.
            response.addHeader("Set-Cookie",
                    JWT_TOKEN.getToken() + "=" + jwt + "; Path=/; Max-Age=" + JWT_TOKEN_TIME_IN_SECONDS.getTime() + "; Secure; HttpOnly; SameSite=None");

            response.addHeader("Set-Cookie",
                    REFRESH_TOKEN.getToken() + "=" + refreshToken + "; Path=/; Max-Age=" + REFRESH_TOKEN_TIME_IN_SECONDS.getTime() + "; Secure; HttpOnly; SameSite=None");

            return true;
        } else {
            return false;
        }
    }


    public void logOut(HttpServletResponse response) {
        // Очистка cookie теми же атрибутами, что и при установке (Path, Secure, SameSite=None),
        // иначе браузер не удалит HttpOnly cookie на HTTPS/cross-origin
        response.addHeader("Set-Cookie",
                JWT_TOKEN.getToken() + "=; Path=/; Max-Age=0; Secure; HttpOnly; SameSite=None");
        response.addHeader("Set-Cookie",
                REFRESH_TOKEN.getToken() + "=; Path=/; Max-Age=0; Secure; HttpOnly; SameSite=None");
    }

    public boolean refresh(HttpServletRequest request, HttpServletResponse response) {

        if (Objects.isNull(request.getCookies()))
            return false;

        Cookie refreshCookie = Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_TOKEN.getToken().equals(c.getName()))
                .findFirst()
                .orElse(null);

        if (refreshCookie != null) {
            String refreshToken = refreshCookie.getValue();
            UserDetails userDetails = adminUsersDetailService
                    .loadUserByUsername(jwtUtil.extractUsernameRefresh(refreshToken));

            if (jwtUtil.validateTokenRefresh(refreshToken, userDetails)) {

                String jwtToken = jwtUtil.generateJwtToken(userDetails);

                // Единообразно выставляем access-cookie с SameSite=None.
                response.addHeader("Set-Cookie",
                        JWT_TOKEN.getToken() + "=" + jwtToken + "; Path=/; Max-Age=" + JWT_TOKEN_TIME_IN_SECONDS.getTime() + "; Secure; HttpOnly; SameSite=None");

                return true;
            }
        }

        return false;
    }

    /**
     * Удаление аккаунта админа. Только для вызова из контроллера под SUPER_ADMIN (проверка в Security).
     */
    @Transactional
    public void deleteAdminAccount(String targetUsernameRaw, String actorUsername) {
        String target = AdminUsernamePolicy.requireValidUsername(targetUsernameRaw);
        if (actorUsername == null || actorUsername.isBlank()) {
            throw new IllegalArgumentException("actor username is required");
        }
        if (target.equals(actorUsername)) {
            throw new IllegalArgumentException("cannot delete your own account");
        }
        AdminUsersEntity victim = adminUsersRepository.findByUsername(target)
                .orElseThrow(() -> new EntityNotFoundException("admin user not found"));

        long total = adminUsersRepository.count();
        if (total <= 1) {
            throw new IllegalStateException("cannot delete the last admin account");
        }
        if (victim.getRole() == Role.SUPER_ADMIN) {
            long supers = adminUsersRepository.countByRole(Role.SUPER_ADMIN);
            if (supers <= 1) {
                throw new IllegalStateException("cannot delete the last SUPER_ADMIN");
            }
        }
        adminUsersRepository.delete(victim);
        log.info("Admin account deleted: target={}, actor={}", target, actorUsername);
    }

}
