package com.back.minecraftback.payment.service;

import com.back.minecraftback.payment.config.TbankAcquiringProperties;
import com.back.minecraftback.payment.tbank.TbankInitResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TbankAcquiringClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TbankAcquiringProperties properties;

    public TbankInitResponse callInit(Map<String, Object> jsonBody) throws Exception {
        String base = properties.baseUrl().trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + "/Init";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(jsonBody, headers);

        String raw = restTemplate.postForObject(url, entity, String.class);
        return objectMapper.readValue(raw, TbankInitResponse.class);
    }
}
