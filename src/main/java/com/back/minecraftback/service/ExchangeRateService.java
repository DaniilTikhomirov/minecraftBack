package com.back.minecraftback.service;

import com.back.minecraftback.entity.ExchangeRateEntity;
import com.back.minecraftback.entity.MainNewsEntity;
import com.back.minecraftback.repository.ExchangeRateRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {
    private final ExchangeRateRepository exchangeRateRepository;

    public void updateRate(String rate){
        ExchangeRateEntity entity = new ExchangeRateEntity();
        entity.setId(1);
        entity.setRate(new BigDecimal(rate));
        exchangeRateRepository.save(entity);
    }

    public String getRate(){
        List<ExchangeRateEntity> list = exchangeRateRepository.findAll();
        if(list.isEmpty()){
            throw new EntityNotFoundException("Exchange rate not found in DB");
        }
        return list.getFirst().getRate().toString();
    }

}
