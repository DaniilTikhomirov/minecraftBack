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
@RequestMapping("main-news")
public class MainNewsController {
    private final MainNewsService mainNewsService;

    @PostMapping()
    public ResponseEntity<HttpStatus> saveNews(@RequestBody List<NewsDTO> saveNewsDTO) {
        mainNewsService.saveAll(saveNewsDTO);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/get")
    public ResponseEntity<List<GetNewsDto>> getNews() {
        return ResponseEntity.ok(mainNewsService.getAll());
    }

    @PutMapping("{id}")
    public ResponseEntity<HttpStatus> swapActive(@PathVariable Long id) {
        mainNewsService.swapActive(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
