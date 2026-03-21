package com.back.minecraftback.service;

import com.back.minecraftback.dto.GetRankDto;
import com.back.minecraftback.dto.RankDto;
import com.back.minecraftback.entity.RankCardsEntity;
import com.back.minecraftback.mapper.CardsMapper;
import com.back.minecraftback.repository.RankCardsRepository;
import com.back.minecraftback.util.TextValidation;
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
        validateSubscriptionPricing(dto);
        TextValidation.requireDetailedDescriptionLength(dto.detailedDescription());
        if (isNew(dto)) {
            RankCardsEntity rankEntity = mapper.toRankCardsEntity(dto);
            rankEntity.setActive(true);
            rankEntity.setAllowForever(Boolean.TRUE.equals(dto.allowForever()));
            rankEntity.setPriceForever(Boolean.TRUE.equals(dto.allowForever()) ? dto.priceForever() : null);
            handleNewEntity(dto, rankEntity);
            return rankEntity;
        }
        RankCardsEntity existing = rankCardsRepository.findById(dto.id()).orElseThrow(EntityNotFoundException::new);
        RankCardsEntity fromDto = mapper.toRankCardsEntity(dto);
        existing.setTitle(fromDto.getTitle());
        existing.setPriceMonth(fromDto.getPriceMonth());
        existing.setPriceThreeMonths(fromDto.getPriceThreeMonths());
        existing.setPriceYear(fromDto.getPriceYear());
        existing.setAllowForever(Boolean.TRUE.equals(fromDto.getAllowForever()));
        existing.setPriceForever(Boolean.TRUE.equals(fromDto.getAllowForever()) ? fromDto.getPriceForever() : null);
        existing.setDescription(fromDto.getDescription());
        existing.setDetailedDescription(fromDto.getDetailedDescription());
        handleExistingEntity(dto, existing);
        return existing;
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

    private void validateSubscriptionPricing(RankDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("rank dto is required");
        }
        if (dto.priceMonth() == null || dto.priceMonth() <= 0) {
            throw new IllegalArgumentException("priceMonth must be positive");
        }
        if (dto.priceThreeMonths() == null || dto.priceThreeMonths() <= 0) {
            throw new IllegalArgumentException("priceThreeMonths must be positive");
        }
        if (dto.priceYear() == null || dto.priceYear() <= 0) {
            throw new IllegalArgumentException("priceYear must be positive");
        }
        boolean allowForever = Boolean.TRUE.equals(dto.allowForever());
        if (allowForever) {
            if (dto.priceForever() == null || dto.priceForever() <= 0) {
                throw new IllegalArgumentException("priceForever must be positive when allowForever=true");
            }
            return;
        }
        if (dto.priceForever() != null) {
            throw new IllegalArgumentException("priceForever must be null when allowForever=false");
        }
    }

    public List<GetRankDto> getAll() {
        return mapper.toGetRankDto(rankCardsRepository.findAllByActiveIsTrue());
    }

    public List<GetRankDto> getAllInactive() {
        return mapper.toGetRankDto(rankCardsRepository.findAllInactive());
    }

    /** Все карточки из БД (активные + неактивные) для админ-просмотра. */
    public List<GetRankDto> getAllFromDb() {
        return mapper.toGetRankDto(rankCardsRepository.findAll());
    }

    @Transactional
    public void swapActive(long id) {
        RankCardsEntity rankCardsEntity = rankCardsRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        boolean currentlyActive = Boolean.TRUE.equals(rankCardsEntity.getActive());
        rankCardsEntity.setActive(!currentlyActive);
        rankCardsRepository.saveAndFlush(rankCardsEntity);
    }

    public void deleteById(long id) {
        if (!rankCardsRepository.existsById(id)) {
            throw new EntityNotFoundException();
        }
        rankCardsRepository.deleteById(id);
    }

    @Transactional
    public void deleteAll() {
        rankCardsRepository.deleteAll();
    }
}
