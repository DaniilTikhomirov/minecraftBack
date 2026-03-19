package com.back.minecraftback.controller;

import com.back.minecraftback.dto.AuthDTO;
import com.back.minecraftback.dto.MeDto;
import com.back.minecraftback.service.AdminUsersService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping(path = { "auth", "/api/auth" })
public class AuthController {
    private final AdminUsersService adminUsersService;

    /** Отладка: GET без авторизации, возвращает какой путь видит бэкенд. Вызови и проверь. */
    @GetMapping("/ping")
    public ResponseEntity<?> ping(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        return ResponseEntity.ok(java.util.Map.of(
                "ok", true,
                "servletPath", servletPath != null ? servletPath : "",
                "requestURI", requestURI != null ? requestURI : "",
                "contextPath", contextPath != null ? contextPath : ""
        ));
    }

    @PostMapping()
    public ResponseEntity<HttpStatus> auth(@RequestBody(required = false) AuthDTO authDTO, HttpServletResponse response) {
        if (authDTO == null
                || authDTO.username() == null || authDTO.username().isBlank()
                || authDTO.password() == null || authDTO.password().isBlank()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if (adminUsersService.authenticate(authDTO.username(), authDTO.password(), response))
            return new ResponseEntity<>(HttpStatus.OK);
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    @GetMapping("/me")
    public ResponseEntity<MeDto> me(Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails user)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String username = user.getUsername();
        boolean enabled = user.isEnabled();

        // берем первую роль (у тебя только одна)
        String role = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER");

        return ResponseEntity.ok(new MeDto(username, role, enabled));
    }

    @PostMapping("/logout")
    public ResponseEntity<HttpStatus> logout(HttpServletResponse response) {
        adminUsersService.logOut(response);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<HttpStatus> refresh(HttpServletResponse response, HttpServletRequest request, Authentication authentication) {
        if(adminUsersService.refresh(request, response))
            return new ResponseEntity<>(HttpStatus.OK);
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
}