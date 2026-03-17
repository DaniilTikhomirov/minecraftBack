package com.back.minecraftback.controller;

import com.back.minecraftback.dto.AllDataDto;
import com.back.minecraftback.service.AdminDataService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер для выдачи полного содержимого БД админу.
 * Отдельный класс, чтобы маппинг GET /admin/data регистрировался явно.
 */
@RestController
@RequestMapping(value = { "/admin", "/api/admin" })
@RequiredArgsConstructor
public class AdminDataController {

    private static final Logger log = LoggerFactory.getLogger(AdminDataController.class);

    private final AdminDataService adminDataService;

    /**
     * Всё содержимое БД: rankCards, cases, mainNews, miniNews.
     * Доступ: только SUPER_ADMIN (настраивается в SecurityConfig).
     */
    @GetMapping("/data")
    public ResponseEntity<AllDataDto> getFullData() {
        log.info("GET /admin/data — запрос полного дампа БД");
        AllDataDto data = adminDataService.getAllData();
        log.debug("GET /admin/data — ответ сформирован");
        return ResponseEntity.ok(data);
    }
}
