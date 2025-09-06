package com.back.minecraftback.controller;

import com.back.minecraftback.dto.CreateAdminDTO;
import com.back.minecraftback.service.AdminUsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("admin")
public class AdminController {
    private final AdminUsersService adminUsersService;

    @PostMapping("/create")
    public ResponseEntity<HttpStatus> createAdmin(CreateAdminDTO createAdminDTO) {
        adminUsersService.save(createAdminDTO);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/enabled")
    public ResponseEntity<HttpStatus> swapEnabled(@RequestParam String username) {
        if(adminUsersService.swapEnabled(username))
            return new ResponseEntity<>(HttpStatus.OK);

        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @GetMapping()
    public ResponseEntity<List<String>> getAdminsUsername(@RequestParam(required = false) Optional<Boolean> isEnabled) {
        return ResponseEntity.ok(adminUsersService.getUsernames(isEnabled));
    }
}
