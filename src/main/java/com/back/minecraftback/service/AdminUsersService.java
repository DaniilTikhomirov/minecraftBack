package com.back.minecraftback.service;

import com.back.minecraftback.dto.CreateAdminDTO;
import com.back.minecraftback.entity.AdminUsersEntity;
import com.back.minecraftback.mapper.AdminMapper;
import com.back.minecraftback.repository.AdminUsersRepository;
import com.back.minecraftback.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.back.minecraftback.model.Token.JWT_TOKEN;
import static com.back.minecraftback.model.Token.REFRESH_TOKEN;
import static com.back.minecraftback.model.TokenTime.JWT_TOKEN_TIME_IN_SECONDS;
import static com.back.minecraftback.model.TokenTime.REFRESH_TOKEN_TIME_IN_SECONDS;

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

    public void save(CreateAdminDTO createAdminDTO) {
        adminUsersRepository.save(adminMapper.toEntity(createAdminDTO, passwordEncoder));
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
        if (adminUsersRepository.findByUsername(username)
                .map(user -> passwordEncoder.matches(password, user.getPassword()) && user.getEnabled())
                .orElse(false)) {

            UserDetails userDetails = adminUsersDetailService.loadUserByUsername(username);
            String jwt = jwtUtil.generateJwtToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            Cookie jwtCookie = new Cookie(JWT_TOKEN.getToken(), jwt);
            jwtCookie.setHttpOnly(true);
            // TODO true при выгрузке на прод
            jwtCookie.setSecure(false); // Set to true in production for HTTPS
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(JWT_TOKEN_TIME_IN_SECONDS.getTime()); // 15 minutes
            response.addCookie(jwtCookie);

            Cookie refreshCookie = new Cookie(REFRESH_TOKEN.getToken(), refreshToken);
            refreshCookie.setHttpOnly(true);
            // TODO true при выгрузке на прод
            refreshCookie.setSecure(false); // Set to true in production for HTTPS
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(REFRESH_TOKEN_TIME_IN_SECONDS.getTime());
            response.addCookie(refreshCookie);

            return true;
        } else
            return false;
    }

    public void logOut(HttpServletResponse response) {
        Cookie jwtCookie = new Cookie(JWT_TOKEN.getToken(), null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false); // true на продакшене
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0); // удаляем cookie

        Cookie refreshCookie = new Cookie(REFRESH_TOKEN.getToken(), null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); // true на продакшене
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0); // удаляем cookie
        response.addCookie(refreshCookie);
        response.addCookie(jwtCookie);
    }

    public boolean refresh(HttpServletRequest request, HttpServletResponse response) {

        if(Objects.isNull(request.getCookies()))
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

                Cookie jwtCookie = new Cookie(JWT_TOKEN.getToken(), jwtToken);
                jwtCookie.setHttpOnly(false);
                jwtCookie.setPath("/");
                jwtCookie.setMaxAge(JWT_TOKEN_TIME_IN_SECONDS.getTime());
                response.addCookie(jwtCookie);

                return true;
            }
        }
        return false;
    }
}
