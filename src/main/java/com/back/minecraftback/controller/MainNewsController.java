package com.back.minecraftback.controller;

import com.back.minecraftback.dto.GetNewsDto;
import com.back.minecraftback.dto.NewsDTO;
import com.back.minecraftback.service.MainNewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = { "main-news", "/api/main-news" })
public class MainNewsController {
    private final MainNewsService mainNewsService;

    @PostMapping()
    public ResponseEntity<HttpStatus> saveNews(@RequestBody List<NewsDTO> saveNewsDTO) {
        mainNewsService.saveAll(saveNewsDTO);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /** Активные: GET /main-news/get. Неактивные: GET /main-news/get?inactive=true */
    @GetMapping("/get")
    public ResponseEntity<List<GetNewsDto>> getNews(@RequestParam(name = "inactive", required = false, defaultValue = "false") boolean inactive) {
        if (inactive) {
            return ResponseEntity.ok(mainNewsService.getAllInactive());
        }
        return ResponseEntity.ok(mainNewsService.getAll());
    }

    /** Удалить ВСЕ главные новости из БД. Только SUPER_ADMIN. */
    @DeleteMapping("/clear")
    public ResponseEntity<HttpStatus> clearAllMainNewsDelete() {
        mainNewsService.deleteAll();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/clear")
    public ResponseEntity<HttpStatus> clearAllMainNewsPost() {
        mainNewsService.deleteAll();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HttpStatus> swapActive(@PathVariable Long id) {
        mainNewsService.swapActive(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
