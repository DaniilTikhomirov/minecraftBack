package com.back.minecraftback.mapper;

import com.back.minecraftback.dto.*;
import com.back.minecraftback.entity.CasesEntity;
import com.back.minecraftback.entity.MainNewsEntity;
import com.back.minecraftback.entity.MiniNewsEntity;
import com.back.minecraftback.entity.RankCardsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CardsMapper {

    @Mappings({
            @Mapping(target = "active", ignore = true),
            @Mapping(target = "imageUrl", ignore = true)
    })
    MainNewsEntity toMainNewsEntity(NewsDTO news);

    @Mappings({
            @Mapping(target = "active", ignore = true),
            @Mapping(target = "imageUrl", ignore = true)
    })
    MiniNewsEntity toMiniNewsEntity(NewsDTO news);

    @Mappings({
            @Mapping(target = "active", ignore = true),
            @Mapping(target = "imageUrl", ignore = true)
    })
    CasesEntity toCasesEntity(CasesDto cases);

    @Mappings({
            @Mapping(target = "active", ignore = true),
            @Mapping(target = "imageUrl", ignore = true)
    })
    RankCardsEntity toRankCardsEntity(RankDto rank);

    @Mapping(target = "imageUrl", source = "imageUrl")
    GetNewsDto toGetNewsDto(MainNewsEntity mainNewsEntity);

    @Mapping(target = "imageUrl", source = "imageUrl")
    GetNewsDto toGetNewsDto(MiniNewsEntity miniNewsEntity);

    @Mapping(target = "imageUrl", source = "imageUrl")
    GetCasesDto toGetCasesDto(CasesEntity casesEntity);

    @Mapping(target = "imageUrl", source = "imageUrl")
    GetRankDto toGetRankDto(RankCardsEntity rankCardsEntity);

    List<GetNewsDto> toGetNewsDtoMain(List<MainNewsEntity> mainNewsEntity);

    List<GetNewsDto> toGetNewsDtoMini(List<MiniNewsEntity> miniNewsEntity);

    List<GetCasesDto> toGetCasesDto(List<CasesEntity> casesEntity);

    List<GetRankDto> toGetRankDto(List<RankCardsEntity> rankCardsEntity);



}
