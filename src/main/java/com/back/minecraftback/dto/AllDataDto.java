package com.back.minecraftback.dto;

import java.util.List;

/** Всё содержимое БД для просмотра во всплывающем окне (админ). */
public record AllDataDto(
        List<GetRankDto> rankCards,
        List<GetCasesDto> cases,
        List<GetNewsDto> mainNews,
        List<GetNewsDto> miniNews
) {
}
