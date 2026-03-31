package com.back.minecraftback.wiki.repository;

import com.back.minecraftback.wiki.entity.WikiCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WikiCardRepository extends JpaRepository<WikiCardEntity, Long> {

    List<WikiCardEntity> findAllByActiveIsTrueOrderBySortOrderAscIdAsc();
}
