package com.back.minecraftback.controller;

import com.back.minecraftback.dto.TableInfoDto;
import com.back.minecraftback.service.DatabaseMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/database")
@RequiredArgsConstructor
public class DatabaseController {

    private final DatabaseMetadataService dbMetadataService;

    @GetMapping("/tables")
    public ResponseEntity<List<String>> getAllTables() {
        return ResponseEntity.ok(dbMetadataService.getAllTableNames());
    }

    @GetMapping("/table/{tableName}")
    public ResponseEntity<TableInfoDto> getTableInfo(@PathVariable String tableName) {
        return ResponseEntity.ok(dbMetadataService.getTableInfo(tableName));
    }

    @GetMapping("/all")
    public ResponseEntity<List<TableInfoDto>> getAllTablesInfo() {
        return ResponseEntity.ok(dbMetadataService.getAllTablesInfo());
    }
}
