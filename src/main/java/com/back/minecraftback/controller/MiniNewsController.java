package com.back.minecraftback.controller;

import com.back.minecraftback.dto.GetNewsDto;
import com.back.minecraftback.dto.NewsDTO;
import com.back.minecraftback.service.MiniNewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("mini-news")
public class MiniNewsController {
    private final MiniNewsService miniNewsService;

    @PostMapping()
    public ResponseEntity<HttpStatus> saveNews(@RequestBody List<NewsDTO> saveNewsDTO) {
        miniNewsService.saveAll(saveNewsDTO);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /** Активные: GET /mini-news/get. Неактивные: GET /mini-news/get?inactive=true */
    @GetMapping("/get")
    public ResponseEntity<List<GetNewsDto>> getNews(@RequestParam(name = "inactive", required = false, defaultValue = "false") boolean inactive) {
        if (inactive) {
            return ResponseEntity.ok(miniNewsService.getAllInactive());
        }
        return ResponseEntity.ok(miniNewsService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<HttpStatus> swapActive(@PathVariable Long id) {
        miniNewsService.swapActive(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
