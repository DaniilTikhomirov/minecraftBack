package com.back.minecraftback.service;

import com.back.minecraftback.dto.AllDataDto;
import com.back.minecraftback.dto.GetCasesDto;
import com.back.minecraftback.dto.GetNewsDto;
import com.back.minecraftback.dto.GetRankDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис для выдачи полного дампа данных БД (карточки, кейсы, новости).
 * Используется админкой для просмотра во всплывающем окне.
 */
@Service
@RequiredArgsConstructor
public class AdminDataService {

    private static final Logger log = LoggerFactory.getLogger(AdminDataService.class);

    private final RankCardsService rankCardsService;
    private final CasesService casesService;
    private final MainNewsService mainNewsService;
    private final MiniNewsService miniNewsService;

    /**
     * Собрать все данные из БД: карточки привилегий, кейсы, главные и мини-новости.
     */
    public AllDataDto getAllData() {
        log.debug("Запрос полного дампа данных БД");

        List<GetRankDto> rankCards = rankCardsService.getAllFromDb();
        List<GetCasesDto> cases = casesService.getAllFromDb();
        List<GetNewsDto> mainNews = mainNewsService.getAllFromDb();
        List<GetNewsDto> miniNews = miniNewsService.getAllFromDb();

        log.info("Дамп БД: rankCards={}, cases={}, mainNews={}, miniNews={}",
                rankCards.size(), cases.size(), mainNews.size(), miniNews.size());

        return new AllDataDto(rankCards, cases, mainNews, miniNews);
    }
}
