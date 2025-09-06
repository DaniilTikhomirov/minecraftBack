package com.back.minecraftback.yookasa.client;

import com.back.minecraftback.yookasa.model.PaymentRequest;
import com.back.minecraftback.yookasa.model.PaymentResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class YooKassaClient {

    private final RestTemplate restTemplate;

    @Value("yookassa.url")
    private String url;

    @Value("yookassa.shop.id")
    private String shopId;
    @Value("yookassa.secret.key")
    private String secretKey;


    public PaymentResponse createPayment(PaymentRequest request) {

        String auth = shopId + ":" + secretKey;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.set("Idempotence-Key", UUID.randomUUID().toString());

        HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);

        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                ResponseEntity<PaymentResponse> response =
                        restTemplate.exchange(url, HttpMethod.POST, entity, PaymentResponse.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    return response.getBody();
                } else if (response.getStatusCode().value() == 429) {
                    Thread.sleep(2000);
                } else {
                    throw new RuntimeException("Ошибка ЮKassa: " + response.getStatusCode());
                }

            } catch (Exception e) {
                attempt++;
                if (attempt >= maxRetries) {
                    throw new RuntimeException("Не удалось выполнить запрос в ЮKassa после " + attempt + " попыток", e);
                }
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException ignored) {}
            }
        }
        throw new RuntimeException("Неизвестная ошибка при создании платежа");
    }
}
