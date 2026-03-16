package com.back.minecraftback.controller;

import com.back.minecraftback.dto.AllDataDto;
import com.back.minecraftback.dto.CreateAdminDTO;
import com.back.minecraftback.dto.CreateAdminRequest;
import com.back.minecraftback.model.Role;
import com.back.minecraftback.service.AdminDataService;
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
    private final AdminDataService adminDataService;
    private final RankCardsService rankCardsService;
    private final CasesService casesService;
    private final MainNewsService mainNewsService;
    private final MiniNewsService miniNewsService;

    @PostMapping(value = "/create", consumes = { "application/json", "application/x-www-form-urlencoded" })
    public ResponseEntity<?> createAdmin(@RequestBody(required = false) CreateAdminDTO createAdminDTO,
                                          @RequestParam(required = false) String username,
                                          @RequestParam(required = false) String password,
                                          @RequestParam(required = false) String role) {

        System.out.println("=== CREATE ADMIN DEBUG ===");
        System.out.println("DTO: " + (createAdminDTO != null ? createAdminDTO : "null"));
        System.out.println("Params - username: " + username);
        System.out.println("Params - password: " + (password != null ? "present" : "null"));
        System.out.println("Params - role: " + role);

        try {
            String finalUsername = null;
            String finalPassword = null;
            Role finalRole = null;

            if (createAdminDTO != null) {
                finalUsername = createAdminDTO.username();
                finalPassword = createAdminDTO.password();
                finalRole = createAdminDTO.role();
            }

            if ((finalUsername == null || finalUsername.isBlank()) && username != null && !username.isBlank()) {
                finalUsername = username.trim();
            }

            if ((finalPassword == null || finalPassword.isBlank()) && password != null && !password.isBlank()) {
                finalPassword = password.trim();
            }

            if (finalRole == null && role != null && !role.isBlank()) {
                try {
                    finalRole = Role.valueOf(role.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body("role must be ADMIN or SUPER_ADMIN");
                }
            }

            if (finalUsername == null || finalUsername.isBlank()) {
                return ResponseEntity.badRequest().body("username is required");
            }

            if (finalPassword == null || finalPassword.isBlank()) {
                return ResponseEntity.badRequest().body("password is required");
            }

            if (finalRole == null) {
                return ResponseEntity.badRequest().body("role is required (ADMIN or SUPER_ADMIN)");
            }

            CreateAdminDTO dtoToSave = new CreateAdminDTO(finalUsername, finalPassword, finalRole);
            System.out.println("Saving admin with: " + dtoToSave);

            adminUsersService.save(dtoToSave);
            return new ResponseEntity<>(HttpStatus.CREATED);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping(value = "/create-v2", consumes = "application/json")
    public ResponseEntity<?> createAdminV2(@RequestBody(required = false) CreateAdminRequest request) {
        System.out.println("=== CREATE ADMIN V2 ===");
        System.out.println("Request: " + request);

        if (request == null) {
            return ResponseEntity.badRequest().body("Request body is required (JSON: username, password, role)");
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body("username is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body("password is required");
        }
        if (request.getRole() == null || request.getRole().isBlank()) {
            return ResponseEntity.badRequest().body("role is required");
        }

        try {
            Role role = Role.valueOf(request.getRole().toUpperCase());
            CreateAdminDTO dto = new CreateAdminDTO(
                    request.getUsername().trim(),
                    request.getPassword().trim(),
                    role
            );
            adminUsersService.save(dto);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("role must be ADMIN or SUPER_ADMIN");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/enabled")
    public ResponseEntity<HttpStatus> swapEnabled(@RequestParam String username) {
        if(adminUsersService.swapEnabled(username))
            return new ResponseEntity<>(HttpStatus.OK);

        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    /**
     * GET /admin — список логинов админов (query: isEnabled).
     * GET /admin?data=full — то же, что GET /admin/data (полный дамп БД через AdminDataService).
     */
    @GetMapping()
    public ResponseEntity<?> getAdmin(
            @RequestParam(required = false) Optional<Boolean> isEnabled,
            @RequestParam(required = false) String data) {
        if ("full".equalsIgnoreCase(data)) {
            return ResponseEntity.ok(adminDataService.getAllData());
        }
        return ResponseEntity.ok(adminUsersService.getUsernames(isEnabled));
    }

    /** Очистка всех записей из БД. Только SUPER_ADMIN. Вызывать POST /admin/clear/rank и т.д. */
    @PostMapping("/clear/rank")
    public ResponseEntity<HttpStatus> clearRank() {
        rankCardsService.deleteAll();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/clear/cases")
    public ResponseEntity<HttpStatus> clearCases() {
        casesService.deleteAll();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/clear/main-news")
    public ResponseEntity<HttpStatus> clearMainNews() {
        mainNewsService.deleteAll();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/clear/mini-news")
    public ResponseEntity<HttpStatus> clearMiniNews() {
        miniNewsService.deleteAll();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
