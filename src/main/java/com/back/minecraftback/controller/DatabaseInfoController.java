package com.back.minecraftback.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = { "/db", "/api/db" })
public class DatabaseInfoController {

    private static final String SCHEMA = "mc_backend";

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInfoController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Допустимое имя таблицы (защита от SQL-инъекций). */
    private static boolean isValidTableName(String name) {
        return name != null && name.matches("^[a-zA-Z0-9_]+$");
    }

    /** Проверка существования таблицы в схеме mc_backend. */
    private boolean tableExists(String tableName) {
        String sql = """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = ?
              AND table_name = ?
            """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, SCHEMA, tableName);
        return count != null && count > 0;
    }

    /**
     * 1. Список всех таблиц в схеме mc_backend.
     * GET /api/db/tables
     */
    @GetMapping("/tables")
    public ResponseEntity<?> getTables() {
        try {
            String sql = """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = ?
                ORDER BY table_name
                """;
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(sql, SCHEMA);
            return ResponseEntity.ok(tables);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    /**
     * 2. Структура таблицы: колонки, типы, is_nullable, character_maximum_length.
     * GET /api/db/table/{tableName}/info
     */
    @GetMapping("/table/{tableName}/info")
    public ResponseEntity<?> getTableInfo(@PathVariable String tableName) {
        if (!isValidTableName(tableName)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid table name"));
        }
        if (!tableExists(tableName)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Table not found in mc_backend schema"));
        }
        try {
            String sql = """
                SELECT
                    column_name,
                    data_type,
                    is_nullable,
                    character_maximum_length
                FROM information_schema.columns
                WHERE table_schema = ?
                  AND table_name = ?
                ORDER BY ordinal_position
                """;
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(sql, SCHEMA, tableName);
            return ResponseEntity.ok(columns);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    /**
     * 3. Данные из таблицы (по умолчанию 50 строк, максимум 1000).
     * GET /api/db/table/{tableName}/data?limit=50
     */
    @GetMapping("/table/{tableName}/data")
    public ResponseEntity<?> getTableData(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "50") int limit) {

        if (!isValidTableName(tableName)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid table name"));
        }
        if (!tableExists(tableName)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Table not found in mc_backend schema"));
        }
        if (limit > 1000) limit = 1000;
        if (limit < 1) limit = 1;

        try {
            String sql = "SELECT * FROM " + SCHEMA + "." + tableName + " LIMIT " + limit;
            List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    /**
     * 4. Количество записей в таблице.
     * GET /api/db/table/{tableName}/count
     */
    @GetMapping("/table/{tableName}/count")
    public ResponseEntity<?> getTableCount(@PathVariable String tableName) {
        if (!isValidTableName(tableName)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid table name"));
        }
        if (!tableExists(tableName)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Table not found in mc_backend schema"));
        }
        try {
            String sql = "SELECT COUNT(*) FROM " + SCHEMA + "." + tableName;
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return ResponseEntity.ok(Map.of("count", count != null ? count : 0L));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }
}
