package com.back.minecraftback.controller;

import com.back.minecraftback.dto.GetRankDto;
import com.back.minecraftback.dto.RankDto;
import com.back.minecraftback.service.RankCardsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = { "/rank", "/api/rank" })
@RequiredArgsConstructor
public class RankCardController {
    private final RankCardsService rankCardsService;

    @PostMapping()
    public ResponseEntity<HttpStatus> saveRank(@RequestBody List<RankDto> rankDtos) {
        rankCardsService.saveAll(rankDtos);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /** Активные: GET /rank/get. Неактивные: GET /rank/get?inactive=true */
    @GetMapping("/get")
    public ResponseEntity<List<GetRankDto>> getRank(
            @RequestParam(name = "inactive", required = false, defaultValue = "false") boolean inactive,
            @RequestParam(name = "active", required = false) Boolean active,
            @RequestParam(name = "all", required = false, defaultValue = "false") boolean all
    ) {
        if (all) {
            return ResponseEntity.ok(rankCardsService.getAllFromDb());
        }
        if (active != null) {
            inactive = !active;
        }
        if (inactive) {
            return ResponseEntity.ok(rankCardsService.getAllInactive());
        }
        return ResponseEntity.ok(rankCardsService.getAll());
    }

    /** Принудительно удалить ВСЕ карточки привилегий из БД. Только SUPER_ADMIN. */
    @DeleteMapping("/clear")
    public ResponseEntity<HttpStatus> clearAllRanksDelete() {
        rankCardsService.deleteAll();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/clear")
    public ResponseEntity<HttpStatus> clearAllRanksPost() {
        rankCardsService.deleteAll();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HttpStatus> swapActive(@PathVariable Long id) {
        rankCardsService.swapActive(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /** Удалить карточку привилегии по id. Только SUPER_ADMIN (см. SecurityConfig). */
    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteRank(@PathVariable Long id) {
        rankCardsService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
