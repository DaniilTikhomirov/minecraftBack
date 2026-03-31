package com.back.minecraftback.wiki.controller;

import com.back.minecraftback.wiki.dto.GetWikiCardDto;
import com.back.minecraftback.wiki.dto.WikiCardSaveDto;
import com.back.minecraftback.wiki.service.WikiCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = { "wiki", "/api/wiki" })
public class WikiController {

    private final WikiCardService wikiCardService;

    /** Публично: только активные, sortOrder ASC, id ASC. */
    @GetMapping("/get")
    public ResponseEntity<List<GetWikiCardDto>> getWiki(
            @RequestParam(name = "inactive", required = false, defaultValue = "false") boolean inactive
    ) {
        if (inactive) {
            return ResponseEntity.ok(wikiCardService.getAllInactive());
        }
        return ResponseEntity.ok(wikiCardService.getAllActive());
    }

    /** Админ: массив из одного объекта (как main-news). */
    @PostMapping
    public ResponseEntity<HttpStatus> save(@RequestBody List<WikiCardSaveDto> body) {
        wikiCardService.saveAll(body);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HttpStatus> swapActive(@PathVariable Long id) {
        wikiCardService.swapActive(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /** Только SUPER_ADMIN (см. SecurityConfig). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        wikiCardService.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
