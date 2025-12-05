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
@RequestMapping("auth")
public class AuthController {
    private final AdminUsersService adminUsersService;

    @PostMapping()
    public ResponseEntity<HttpStatus> auth(@RequestBody AuthDTO authDTO, HttpServletResponse response) {
        if (adminUsersService.authenticate(authDTO.username(), authDTO.password(), response))
            return new ResponseEntity<>(HttpStatus.OK);
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    @GetMapping("/me")
    public MeDto me(Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails user)) {
            return null; // или выбросить ошибку
        }

        String username = user.getUsername();
        boolean enabled = user.isEnabled();

        // берем первую роль (у тебя только одна)
        String role = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER");

        return new MeDto(username, role, enabled);
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