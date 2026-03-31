package com.back.minecraftback.controller;

import com.back.minecraftback.dto.AllDataDto;
import com.back.minecraftback.dto.CreateAdminDTO;
import com.back.minecraftback.model.Role;
import com.back.minecraftback.service.AdminDataService;
import com.back.minecraftback.service.AdminUsersService;
import com.back.minecraftback.service.CasesService;
import com.back.minecraftback.service.MainNewsService;
import com.back.minecraftback.service.MiniNewsService;
import com.back.minecraftback.service.RankCardsService;
import com.back.minecraftback.wiki.service.WikiCardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = { "/admin", "/api/admin" })
public class AdminController {

    private final AdminUsersService adminUsersService;
    private final AdminDataService adminDataService;
    private final RankCardsService rankCardsService;
    private final CasesService casesService;
    private final MainNewsService mainNewsService;
    private final MiniNewsService miniNewsService;
    private final WikiCardService wikiCardService;

    /**
     * Создание админа. Только JSON.
     * Тело: {"username":"...", "password":"...", "role":"ADMIN" или "SUPER_ADMIN"}
     */
    @PostMapping(value = "/create", consumes = "application/json")
    public ResponseEntity<?> createAdmin(@RequestBody(required = false) CreateAdminDTO body) {
        log.info("[createAdmin] start");

        if (body == null) {
            log.warn("[createAdmin] body is null");
            return ResponseEntity.badRequest().body("Request body is required (JSON: username, password, role)");
        }

        log.debug("[createAdmin] received username='{}', role={}, passwordPresent={}",
                body.username(), body.role(), body.password() != null && !body.password().isBlank());

        if (body.username() == null || body.username().isBlank()) {
            log.warn("[createAdmin] validation failed: username empty");
            return ResponseEntity.badRequest().body("username is required");
        }
        if (body.password() == null || body.password().isBlank()) {
            log.warn("[createAdmin] validation failed: password empty");
            return ResponseEntity.badRequest().body("password is required");
        }
        if (body.role() == null) {
            log.warn("[createAdmin] validation failed: role null");
            return ResponseEntity.badRequest().body("role is required (ADMIN or SUPER_ADMIN)");
        }

        log.info("[createAdmin] calling service.save for username='{}', role={}", body.username(), body.role());
        try {
            adminUsersService.save(body);
            log.info("[createAdmin] success, created username='{}'", body.username());
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.warn("[createAdmin] service validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("[createAdmin] error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/enabled")
    public ResponseEntity<HttpStatus> swapEnabled(@RequestParam String username) {
        if(adminUsersService.swapEnabled(username))
            return new ResponseEntity<>(HttpStatus.OK);

        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    /**
     * Удалить аккаунт админа. Только SUPER_ADMIN.
     * Нельзя удалить себя, последнего админа в системе и последнего SUPER_ADMIN.
     */
    @DeleteMapping("/user/{username}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable String username, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails ud)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        adminUsersService.deleteAdminAccount(username, ud.getUsername());
        return ResponseEntity.noContent().build();
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

    /**
     * Список сущностей, которые можно очистить.
     * POST /admin/clear/rank, /admin/clear/cases, /admin/clear/main-news, /admin/clear/mini-news, /admin/clear/wiki
     */
    @GetMapping("/clear")
    public ResponseEntity<?> listClearTargets() {
        log.info("[clear] GET list clear targets");
        try {
            List<String> targets = List.of("rank", "cases", "main-news", "mini-news", "wiki");
            return ResponseEntity.ok(Map.of("targets", targets));
        } catch (Exception e) {
            log.error("[clear] GET error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    /** Очистка: удаляются все записи из указанной таблицы. Только SUPER_ADMIN. */
    @PostMapping("/clear/rank")
    public ResponseEntity<Map<String, String>> clearRank() {
        log.info("[clear] clear rank_cards");
        rankCardsService.deleteAll();
        log.info("[clear] rank_cards cleared");
        return ResponseEntity.ok(Map.of("cleared", "rank", "message", "Все ранговые карточки удалены"));
    }

    @PostMapping("/clear/cases")
    public ResponseEntity<Map<String, String>> clearCases() {
        log.info("[clear] clear cases");
        casesService.deleteAll();
        log.info("[clear] cases cleared");
        return ResponseEntity.ok(Map.of("cleared", "cases", "message", "Все кейсы удалены"));
    }

    @PostMapping("/clear/main-news")
    public ResponseEntity<Map<String, String>> clearMainNews() {
        log.info("[clear] clear main_news");
        mainNewsService.deleteAll();
        log.info("[clear] main_news cleared");
        return ResponseEntity.ok(Map.of("cleared", "main-news", "message", "Все главные новости удалены"));
    }

    @PostMapping("/clear/mini-news")
    public ResponseEntity<Map<String, String>> clearMiniNews() {
        log.info("[clear] clear mini_news");
        miniNewsService.deleteAll();
        log.info("[clear] mini_news cleared");
        return ResponseEntity.ok(Map.of("cleared", "mini-news", "message", "Все мини-новости удалены"));
    }

    @PostMapping("/clear/wiki")
    public ResponseEntity<Map<String, String>> clearWiki() {
        log.info("[clear] clear wiki_card");
        wikiCardService.deleteAll();
        log.info("[clear] wiki_card cleared");
        return ResponseEntity.ok(Map.of("cleared", "wiki", "message", "Все карточки вики удалены"));
    }
}
