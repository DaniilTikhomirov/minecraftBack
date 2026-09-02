package com.back.minecraftback.service;

import com.back.minecraftback.dto.GetSundryDto;
import com.back.minecraftback.dto.SundryDto;
import com.back.minecraftback.entity.SundryEntity;
import com.back.minecraftback.mapper.CardsMapper;
import com.back.minecraftback.repository.SundryRepository;
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
public class SundryService {
    private final SundryRepository sundryRepository;
    private final CardsMapper mapper;
    private final FileStorageService fileStorageService;

    @Transactional
    public void saveAll(List<SundryDto> items) {
        sundryRepository.saveAll(items
                .stream()
                .map(this::toEntity).collect(Collectors.toList()));
    }

    private SundryEntity toEntity(SundryDto dto) {
        String detailedDescription = TextValidation.prepareDetailedDescription(dto.detailedDescription());
        requirePositivePrice(dto.price());
        if (isNew(dto)) {
            SundryEntity entity = mapper.toSundryEntity(dto);
            entity.setActive(true);
            entity.setDetailedDescription(detailedDescription);
            handleNewEntity(dto, entity);
            return entity;
        }
        SundryEntity existing = sundryRepository.findById(dto.id()).orElseThrow(EntityNotFoundException::new);
        SundryEntity fromDto = mapper.toSundryEntity(dto);
        existing.setTitle(fromDto.getTitle());
        existing.setSubtitle(fromDto.getSubtitle());
        existing.setDescription(fromDto.getDescription());
        existing.setDetailedDescription(detailedDescription);
        existing.setPrice(fromDto.getPrice());
        handleExistingEntity(dto, existing);
        return existing;
    }

    private static void requirePositivePrice(Integer price) {
        if (price == null || price <= 0) {
            throw new IllegalArgumentException("price must be positive");
        }
    }

    private boolean isNew(SundryDto dto) {
        return Objects.isNull(dto.id()) || dto.id() == 0;
    }

    private void handleNewEntity(SundryDto dto, SundryEntity entity) {
        if (hasImage(dto)) {
            String imageUrl = fileStorageService.save(Base64.getDecoder().decode(dto.imageBase64()));
            entity.setImageUrl(imageUrl);
        }
    }

    private void handleExistingEntity(SundryDto dto, SundryEntity entity) {
        if (missingImage(dto)) {
            throw new IllegalArgumentException("image is null");
        }

        if (isImageUnchanged(dto)) {
            entity.setImageUrl(dto.imageUrl());
        } else if (Objects.nonNull(dto.imageUrl())) {
            String imageUrl = fileStorageService.save(Base64.getDecoder().decode(dto.imageBase64()), dto.imageUrl());
            entity.setImageUrl(imageUrl);
        } else {
            String imageUrl = fileStorageService.save(Base64.getDecoder().decode(dto.imageBase64()));
            entity.setImageUrl(imageUrl);
        }
    }

    private boolean hasImage(SundryDto dto) {
        return Objects.nonNull(dto.imageBase64()) && !dto.imageBase64().isEmpty();
    }

    private boolean missingImage(SundryDto dto) {
        return Objects.isNull(dto.imageBase64()) && Objects.isNull(dto.imageUrl());
    }

    private boolean isImageUnchanged(SundryDto dto) {
        return Objects.isNull(dto.imageBase64());
    }

    public List<GetSundryDto> getAll() {
        return mapper.toGetSundryDto(sundryRepository.findAllByActiveIsTrue());
    }

    public List<GetSundryDto> getAllInactive() {
        return mapper.toGetSundryDto(sundryRepository.findAllInactive());
    }

    public List<GetSundryDto> getAllFromDb() {
        return mapper.toGetSundryDto(sundryRepository.findAll());
    }

    @Transactional
    public void swapActive(long id) {
        SundryEntity entity = sundryRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        entity.setActive(!Boolean.TRUE.equals(entity.getActive()));
        sundryRepository.saveAndFlush(entity);
    }

    @Transactional
    public void deleteAll() {
        sundryRepository.deleteAll();
    }

    @Transactional
    public void deleteById(long id) {
        SundryEntity entity = sundryRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        fileStorageService.deleteStoredFileIfExists(entity.getImageUrl());
        sundryRepository.delete(entity);
    }
}
