package com.back.minecraftback.controller;

import com.back.minecraftback.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rate")
@RequiredArgsConstructor
public class ExchangeRateController {
    private final ExchangeRateService exchangeRateService;

    @PutMapping
    public ResponseEntity<HttpStatus> updateRate(@RequestParam String rate){
        exchangeRateService.updateRate(rate);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping("/get")
    public ResponseEntity<String> getRate(){
        return ResponseEntity.ok(exchangeRateService.getRate());
    }
}
