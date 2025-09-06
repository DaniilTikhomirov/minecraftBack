package com.back.minecraftback.controller;

import com.back.minecraftback.dto.AuthDTO;
import com.back.minecraftback.service.AdminUsersService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



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