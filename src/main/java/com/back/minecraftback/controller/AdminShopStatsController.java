package com.back.minecraftback.controller;

import com.back.minecraftback.shopstats.ShopStatsService;
import com.back.minecraftback.shopstats.dto.ShopStatsDtos.ShopStatsPayloadDto;
import com.back.minecraftback.shopstats.dto.ShopStatsDtos.ShopStatsResetRequestDto;
import com.back.minecraftback.shopstats.dto.ShopStatsDtos.ShopStatsSnapshotsResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * Статистика магазина для админки. Данные считаются только на сервере при подтверждении оплаты.
 * GET — любой админ; сброс архива и удаление снимков — только SUPER_ADMIN.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = { "/admin/shop-stats", "/api/admin/shop-stats" })
public class AdminShopStatsController {

    private final ShopStatsService shopStatsService;

    @GetMapping(value = "/current", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ShopStatsPayloadDto current() {
        return shopStatsService.getCurrentPayload();
    }

    @GetMapping(value = "/snapshots", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ShopStatsSnapshotsResponseDto snapshots() {
        return shopStatsService.listSnapshots();
    }

    @PostMapping(value = "/reset", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> reset(@RequestBody(required = false) ShopStatsResetRequestDto body) {
        try {
            Optional<String> label = Optional.ofNullable(body).map(ShopStatsResetRequestDto::label);
            shopStatsService.resetCurrentPeriod(label);
            return ResponseEntity.ok().build();
        } catch (JsonProcessingException e) {
            log.error("[shopStats] reset serialization failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (IllegalStateException e) {
            log.warn("[shopStats] reset rejected: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Удаляет только запись архива по UUID. Некорректный id — 400; отсутствующий — 404 (фронт трактует как уже удалённый).
     */
    @DeleteMapping("/snapshots/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteSnapshot(@PathVariable("id") String idRaw) {
        UUID id;
        try {
            id = UUID.fromString(idRaw);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        boolean removed = shopStatsService.deleteSnapshot(id);
        if (!removed) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
