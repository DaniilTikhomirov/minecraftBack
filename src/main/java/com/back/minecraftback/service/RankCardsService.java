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
        String detailedDescription = TextValidation.prepareDetailedDescription(dto.detailedDescription());
        if (isNew(dto)) {
            RankCardsEntity rankEntity = mapper.toRankCardsEntity(dto);
            rankEntity.setActive(true);
            rankEntity.setDetailedDescription(detailedDescription);
            applyPeriodFlags(rankEntity, dto);
            handleNewEntity(dto, rankEntity);
            return rankEntity;
        }
        RankCardsEntity existing = rankCardsRepository.findById(dto.id()).orElseThrow(EntityNotFoundException::new);
        RankCardsEntity fromDto = mapper.toRankCardsEntity(dto);
        existing.setTitle(fromDto.getTitle());
        existing.setPriceMonth(fromDto.getPriceMonth());
        existing.setPriceThreeMonths(fromDto.getPriceThreeMonths());
        existing.setPriceYear(fromDto.getPriceYear());
        existing.setPriceForever(fromDto.getPriceForever());
        applyPeriodFlags(existing, dto);
        existing.setDescription(fromDto.getDescription());
        existing.setDetailedDescription(detailedDescription);
        handleExistingEntity(dto, existing);
        return existing;
    }

    private static void applyPeriodFlags(RankCardsEntity entity, RankDto dto) {
        entity.setAllowMonth(allowPeriodOrDefault(dto.allowMonth()));
        entity.setAllowThreeMonths(allowPeriodOrDefault(dto.allowThreeMonths()));
        entity.setAllowYear(allowPeriodOrDefault(dto.allowYear()));
        entity.setAllowForever(Boolean.TRUE.equals(dto.allowForever()));
    }

    /** null в запросе — срок включён (совместимость со старой админкой). */
    private static boolean allowPeriodOrDefault(Boolean allowed) {
        return allowed == null || Boolean.TRUE.equals(allowed);
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
        boolean allowMonth = allowPeriodOrDefault(dto.allowMonth());
        boolean allowThreeMonths = allowPeriodOrDefault(dto.allowThreeMonths());
        boolean allowYear = allowPeriodOrDefault(dto.allowYear());
        boolean allowForever = Boolean.TRUE.equals(dto.allowForever());

        if (!allowMonth && !allowThreeMonths && !allowYear && !allowForever) {
            throw new IllegalArgumentException("at least one subscription period must be enabled");
        }
        if (allowMonth && (dto.priceMonth() == null || dto.priceMonth() <= 0)) {
            throw new IllegalArgumentException("priceMonth must be positive when allowMonth=true");
        }
        if (allowThreeMonths && (dto.priceThreeMonths() == null || dto.priceThreeMonths() <= 0)) {
            throw new IllegalArgumentException("priceThreeMonths must be positive when allowThreeMonths=true");
        }
        if (allowYear && (dto.priceYear() == null || dto.priceYear() <= 0)) {
            throw new IllegalArgumentException("priceYear must be positive when allowYear=true");
        }
        if (allowForever && (dto.priceForever() == null || dto.priceForever() <= 0)) {
            throw new IllegalArgumentException("priceForever must be positive when allowForever=true");
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

    @Transactional
    public void deleteById(long id) {
        RankCardsEntity entity = rankCardsRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        fileStorageService.deleteStoredFileIfExists(entity.getImageUrl());
        rankCardsRepository.delete(entity);
    }

    @Transactional
    public void deleteAll() {
        rankCardsRepository.deleteAll();
    }
}
