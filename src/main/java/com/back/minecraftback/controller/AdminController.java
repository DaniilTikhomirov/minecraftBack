package com.back.minecraftback.controller;

import com.back.minecraftback.dto.AllDataDto;
import com.back.minecraftback.dto.CreateAdminDTO;
import com.back.minecraftback.model.Role;
import com.back.minecraftback.service.AdminUsersService;
import com.back.minecraftback.service.CasesService;
import com.back.minecraftback.service.MainNewsService;
import com.back.minecraftback.service.MiniNewsService;
import com.back.minecraftback.service.RankCardsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {
    private final AdminUsersService adminUsersService;
    private final RankCardsService rankCardsService;
    private final CasesService casesService;
    private final MainNewsService mainNewsService;
    private final MiniNewsService miniNewsService;

    @PostMapping(value = "/create", consumes = { "application/json", "application/x-www-form-urlencoded", "multipart/form-data" })
    public ResponseEntity<?> createAdmin(
            @RequestBody(required = false) CreateAdminDTO createAdminDTO,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String role) {
        CreateAdminDTO dto = createAdminDTO;
        if (dto == null || dto.password() == null || dto.password().isBlank()) {
            if (username != null && password != null && role != null && !password.isBlank() && !username.isBlank()) {
                try {
                    dto = new CreateAdminDTO(username, password, Role.valueOf(role.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body("role должен быть ADMIN или SUPER_ADMIN");
                }
            }
        }
        if (dto == null || dto.password() == null || dto.password().isBlank()) {
            return ResponseEntity.badRequest().body("Нужны username, password, role. JSON: {\"username\":\"...\", \"password\":\"...\", \"role\":\"ADMIN\" или \"SUPER_ADMIN\"}. Content-Type: application/json");
        }
        if (dto.username() == null || dto.username().isBlank()) {
            return ResponseEntity.badRequest().body("username обязателен");
        }
        adminUsersService.save(dto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/enabled")
    public ResponseEntity<HttpStatus> swapEnabled(@RequestParam String username) {
        if(adminUsersService.swapEnabled(username))
            return new ResponseEntity<>(HttpStatus.OK);

        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    /**
     * GET /admin — список логинов админов (query: isEnabled).
     * GET /admin?data=full — всё содержимое БД для всплывающего окна (rankCards, cases, mainNews, miniNews).
     */
    @GetMapping()
    public ResponseEntity<?> getAdmin(
            @RequestParam(required = false) Optional<Boolean> isEnabled,
            @RequestParam(required = false) String data) {
        if ("full".equalsIgnoreCase(data)) {
            AllDataDto dto = new AllDataDto(
                    rankCardsService.getAllFromDb(),
                    casesService.getAllFromDb(),
                    mainNewsService.getAllFromDb(),
                    miniNewsService.getAllFromDb()
            );
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.ok(adminUsersService.getUsernames(isEnabled));
    }
}
