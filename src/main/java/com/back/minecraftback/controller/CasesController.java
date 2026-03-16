package com.back.minecraftback.controller;

import com.back.minecraftback.dto.CasesDto;
import com.back.minecraftback.dto.GetCasesDto;
import com.back.minecraftback.service.CasesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("cases")
@RequiredArgsConstructor
public class CasesController {
    private final CasesService casesService;

    @PostMapping()
    public ResponseEntity<HttpStatus> saveCases(@RequestBody List<CasesDto> casesDtos) {
        casesService.saveAll(casesDtos);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /** Активные: GET /cases/get. Неактивные: GET /cases/get?inactive=true */
    @GetMapping("/get")
    public ResponseEntity<List<GetCasesDto>> getCases(@RequestParam(name = "inactive", required = false, defaultValue = "false") boolean inactive) {
        if (inactive) {
            return ResponseEntity.ok(casesService.getAllInactive());
        }
        return ResponseEntity.ok(casesService.getAll());
    }

    /** Удалить ВСЕ кейсы из БД. Только SUPER_ADMIN. */
    @DeleteMapping("/clear")
    public ResponseEntity<HttpStatus> clearAllCasesDelete() {
        casesService.deleteAll();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/clear")
    public ResponseEntity<HttpStatus> clearAllCasesPost() {
        casesService.deleteAll();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HttpStatus> swapActive(@PathVariable Long id) {
        casesService.swapActive(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
