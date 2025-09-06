package com.back.minecraftback.service;

import com.back.minecraftback.dto.CasesDto;
import com.back.minecraftback.dto.GetCasesDto;
import com.back.minecraftback.entity.CasesEntity;
import com.back.minecraftback.entity.MainNewsEntity;
import com.back.minecraftback.mapper.CardsMapper;
import com.back.minecraftback.repository.CasesRepository;
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
public class CasesService {
    private final CasesRepository casesRepository;
    private final CardsMapper mapper;
    private final FileStorageService fileStorageService;

    @Transactional
    public void saveAll(List<CasesDto> cases) {
        casesRepository.saveAll(cases
                .stream()
                .map(this::toEntity).collect(Collectors.toList()));
    }

    private CasesEntity toEntity(CasesDto dto) {
        CasesEntity casesEntity = mapper.toCasesEntity(dto);

        if (isNew(dto)) {
            handleNewEntity(dto, casesEntity);
            return casesEntity;
        }

        handleExistingEntity(dto, casesEntity);
        return casesEntity;
    }




    private boolean isNew(CasesDto dto) {
        return Objects.isNull(dto.id()) || dto.id() == 0;
    }

    private void handleNewEntity(CasesDto dto, CasesEntity entity) {
        if (hasImage(dto)) {
            String imageUrl = fileStorageService.save(Base64.getDecoder().decode(dto.imageBase64()));
            entity.setImageUrl(imageUrl);
        }
    }

    private void handleExistingEntity(CasesDto dto, CasesEntity entity) {
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

    private boolean hasImage(CasesDto dto) {
        return Objects.nonNull(dto.imageBase64()) && !dto.imageBase64().isEmpty();
    }

    private boolean missingImage(CasesDto dto) {
        return Objects.isNull(dto.imageBase64()) && Objects.isNull(dto.imageUrl());
    }

    private boolean isImageUnchanged(CasesDto dto) {
        return Objects.isNull(dto.imageBase64());
    }

    public List<GetCasesDto> getAll() {
        return mapper.toGetCasesDto(casesRepository.findAllByActiveIsTrue());
    }

    public void swapActive(long id){
        CasesEntity entity = casesRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        entity.setActive(!entity.getActive());
        casesRepository.save(entity);
    }
}
