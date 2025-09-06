package com.back.minecraftback.service;

import com.back.minecraftback.dto.GetNewsDto;
import com.back.minecraftback.dto.NewsDTO;
import com.back.minecraftback.entity.MiniNewsEntity;
import com.back.minecraftback.entity.RankCardsEntity;
import com.back.minecraftback.mapper.CardsMapper;
import com.back.minecraftback.repository.MiniNewsRepository;
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
public class MiniNewsService {
    private final MiniNewsRepository miniNewsRepository;
    private final CardsMapper newsMapper;
    private final FileStorageService fileStorageService;

    @Transactional
    public void saveAll(List<NewsDTO> saveNewsDTO) {
        miniNewsRepository.saveAll(
                saveNewsDTO.stream().map(this::toEntity).collect(Collectors.toList())
        );
    }

    private MiniNewsEntity toEntity(NewsDTO dto) {
        MiniNewsEntity newsEntity = newsMapper.toMiniNewsEntity(dto);

        if (isNew(dto)) {
            handleNewEntity(dto, newsEntity);
            return newsEntity;
        }

        handleExistingEntity(dto, newsEntity);
        return newsEntity;
    }


    private boolean isNew(NewsDTO dto) {
        return Objects.isNull(dto.id()) || dto.id() == 0;
    }

    private void handleNewEntity(NewsDTO dto, MiniNewsEntity entity) {
        if (hasImage(dto)) {
            String imageUrl = fileStorageService.save(Base64.getDecoder().decode(dto.imageBase64()));
            entity.setImageUrl(imageUrl);
        }
    }

    private void handleExistingEntity(NewsDTO dto, MiniNewsEntity entity) {
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

    private boolean hasImage(NewsDTO dto) {
        return Objects.nonNull(dto.imageBase64()) && !dto.imageBase64().isEmpty();
    }

    private boolean missingImage(NewsDTO dto) {
        return Objects.isNull(dto.imageBase64()) && Objects.isNull(dto.imageUrl());
    }

    private boolean isImageUnchanged(NewsDTO dto) {
        return Objects.isNull(dto.imageBase64());
    }

    public List<GetNewsDto> getAll() {
        return newsMapper.toGetNewsDtoMini(miniNewsRepository.findAllByActiveIsTrue());
    }

    public void swapActive(long id){
        MiniNewsEntity entity = miniNewsRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        entity.setActive(!entity.getActive());
        miniNewsRepository.save(entity);
    }

}
