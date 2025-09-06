package com.back.minecraftback.service;

import com.back.minecraftback.dto.GetRankDto;
import com.back.minecraftback.dto.RankDto;
import com.back.minecraftback.entity.RankCardsEntity;
import com.back.minecraftback.mapper.CardsMapper;
import com.back.minecraftback.repository.RankCardsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankCardsService {
    private final RankCardsRepository rankCardsRepository;
    private final CardsMapper mapper;
    private final FileStorageService fileStorageService;

    @Transactional
    public void saveAll(List<RankDto> cases) {
        rankCardsRepository.saveAll(cases
                .stream()
                .map(this::toEntity).collect(Collectors.toList()));
    }

    private RankCardsEntity toEntity(RankDto dto) {
        RankCardsEntity rankEntity = mapper.toRankCardsEntity(dto);

        if (isNew(dto)) {
            handleNewEntity(dto, rankEntity);
            return rankEntity;
        }

        handleExistingEntity(dto, rankEntity);
        return rankEntity;
    }




    private boolean isNew(RankDto dto) {
        return Objects.isNull(dto.id()) || dto.id() == 0;
    }

    private void handleNewEntity(RankDto dto, RankCardsEntity entity) {
        if (hasImage(dto)) {
            String imageUrl = fileStorageService.save(Base64.getDecoder().decode(dto.imageBase64()));
            entity.setImageUrl(imageUrl);
        }
    }

    private void handleExistingEntity(RankDto dto, RankCardsEntity entity) {
        if (missingImage(dto)) {
            throw new IllegalArgumentException("image is null");
        }

        if (isImageUnchanged(dto)) {
            entity.setImageUrl(dto.imageUrl());
        }else if(Objects.nonNull(dto.imageUrl())){
            String imageUrl = fileStorageService.save(Base64.getDecoder().decode(dto.imageBase64()), dto.imageUrl());
            entity.setImageUrl(imageUrl);
        } else {
            String imageUrl = fileStorageService.save(Base64.getDecoder().decode(dto.imageBase64()));
            entity.setImageUrl(imageUrl);
        }
    }

    private boolean hasImage(RankDto dto) {
        return Objects.nonNull(dto.imageBase64()) && !dto.imageBase64().isEmpty();
    }

    private boolean missingImage(RankDto dto) {
        return Objects.isNull(dto.imageBase64()) && Objects.isNull(dto.imageUrl());
    }

    private boolean isImageUnchanged(RankDto dto) {
        return Objects.isNull(dto.imageBase64());
    }

    public List<GetRankDto> getAll() {
        return mapper.toGetRankDto(rankCardsRepository.findAllByActiveIsTrue());
    }

    public void swapActive(long id){
        RankCardsEntity rankCardsEntity = rankCardsRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        rankCardsEntity.setActive(!rankCardsEntity.getActive());
        rankCardsRepository.save(rankCardsEntity);
    }
}
