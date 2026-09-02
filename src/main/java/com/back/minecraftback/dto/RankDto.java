package com.back.minecraftback.dto;

public record RankDto(
        Long id,
        String title,
        Integer priceMonth,
        Boolean allowMonth,
        Integer priceThreeMonths,
        Boolean allowThreeMonths,
        Integer priceYear,
        Boolean allowYear,
        Boolean allowForever,
        Integer priceForever,
        String[] description,
        String detailedDescription,
        String imageUrl,
        String imageBase64
) {
}
