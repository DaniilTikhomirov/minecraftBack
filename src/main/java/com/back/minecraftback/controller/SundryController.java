package com.back.minecraftback.controller;

import com.back.minecraftback.dto.GetSundryDto;
import com.back.minecraftback.dto.SundryDto;
import com.back.minecraftback.service.SundryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = { "sundry", "/api/sundry" })
@RequiredArgsConstructor
public class SundryController {
    private final SundryService sundryService;

    @PostMapping()
    public ResponseEntity<HttpStatus> saveSundry(@RequestBody List<SundryDto> sundryDtos) {
        sundryService.saveAll(sundryDtos);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /** Активные: GET /sundry/get. Неактивные: GET /sundry/get?inactive=true */
    @GetMapping("/get")
    public ResponseEntity<List<GetSundryDto>> getSundry(
            @RequestParam(name = "inactive", required = false, defaultValue = "false") boolean inactive
    ) {
        if (inactive) {
            return ResponseEntity.ok(sundryService.getAllInactive());
        }
        return ResponseEntity.ok(sundryService.getAll());
    }

    /** Удалить ВСЕ позиции каталога. Только SUPER_ADMIN. */
    @DeleteMapping("/clear")
    public ResponseEntity<HttpStatus> clearAllSundryDelete() {
        sundryService.deleteAll();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/clear")
    public ResponseEntity<HttpStatus> clearAllSundryPost() {
        sundryService.deleteAll();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HttpStatus> swapActive(@PathVariable Long id) {
        sundryService.swapActive(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /** Удалить одну позицию по id. Только SUPER_ADMIN (см. SecurityConfig). */
    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteSundry(@PathVariable Long id) {
        sundryService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
