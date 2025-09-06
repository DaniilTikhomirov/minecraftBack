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

    @GetMapping("/get")
    public ResponseEntity<List<GetCasesDto>> getCases() {
        return ResponseEntity.ok(casesService.getAll());
    }

    @PutMapping("{id}")
    public ResponseEntity<HttpStatus> swapActive(@PathVariable Long id) {
        casesService.swapActive(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
