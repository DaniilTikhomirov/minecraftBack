package com.back.minecraftback.controller;

import com.back.minecraftback.dto.AllDataDto;
import com.back.minecraftback.dto.CreateAdminDTO;
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

    @PostMapping(value = "/create", consumes = "application/json")
    public ResponseEntity<?> createAdmin(@RequestBody CreateAdminDTO createAdminDTO) {
        if (createAdminDTO == null) {
            return ResponseEntity.badRequest().body("DTO is null (received: null body)");
        }
        String username = createAdminDTO.username();
        String password = createAdminDTO.password();
        Object role = createAdminDTO.role();
        boolean passwordPresent = password != null && !password.isBlank();
        if (!passwordPresent) {
            return ResponseEntity.badRequest().body("password is required (received: username='" + username + "', role=" + role + ", password=empty)");
        }
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body("username is required (received: username=empty, role=" + role + ", password=present)");
        }
        if (role == null) {
            return ResponseEntity.badRequest().body("role is required, ADMIN or SUPER_ADMIN (received: username='" + username + "', role=null)");
        }
        adminUsersService.save(createAdminDTO);
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
