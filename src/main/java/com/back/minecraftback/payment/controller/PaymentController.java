package com.back.minecraftback.payment.controller;

import com.back.minecraftback.payment.dto.PaymentInitRequestDto;
import com.back.minecraftback.payment.dto.PaymentInitResponseDto;
import com.back.minecraftback.payment.service.TbankPaymentService;
import com.back.minecraftback.payment.entity.PaymentOrderEntity;
import com.back.minecraftback.payment.repository.PaymentOrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Hidden
@RestController
@RequestMapping(value = { "/payments", "/api/payments" })
@RequiredArgsConstructor
public class PaymentController {

    private final TbankPaymentService tbankPaymentService;
    private final PaymentOrderRepository paymentOrderRepository;

    /**
     * Создать платёж в Т‑Банке. Сумма пересчитывается на сервере по типу товара и БД.
     */
    @PostMapping(value = "/init", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public PaymentInitResponseDto init(@RequestBody PaymentInitRequestDto body) {
        return tbankPaymentService.init(body);
    }

    /**
     * Webhook уведомлений Т‑Банка. Должен быть доступен по HTTPS по URL из TBANK_NOTIFICATION_URL.
     * Ответ: HTTP 200 и тело {@code OK} (латиница, без HTML) — по требованиям банка.
     */
    @PostMapping(value = "/tbank/notification", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> tbankNotification(@RequestBody JsonNode body) {
        try {
            tbankPaymentService.handleNotification(body);
            return ResponseEntity.ok("OK");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("FORBIDDEN");
        }
    }

    @GetMapping(value = "/status/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getStatus(@PathVariable String orderId) {
        return paymentOrderRepository.findByTbankOrderId(orderId)
                .<ResponseEntity<?>>map(o -> ResponseEntity.ok(Map.of(
                        "orderId", o.getTbankOrderId(),
                        "status", o.getStatus().name()
                )))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
