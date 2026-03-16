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
@RequestMapping("/rank")
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
    public ResponseEntity<List<GetRankDto>> getRank(@RequestParam(name = "inactive", required = false, defaultValue = "false") boolean inactive) {
        if (inactive) {
            return ResponseEntity.ok(rankCardsService.getAllInactive());
        }
        return ResponseEntity.ok(rankCardsService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<HttpStatus> swapActive(@PathVariable Long id) {
        rankCardsService.swapActive(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /** Удалить карточку навсегда (активную или неактивную). */
    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteRank(@PathVariable Long id) {
        rankCardsService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
