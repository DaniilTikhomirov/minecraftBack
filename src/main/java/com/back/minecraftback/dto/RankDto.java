package com.back.minecraftback.dto;

public record RankDto(
        Long id,
        String title,
        Integer priceMonth,
        Integer priceThreeMonths,
        Integer priceYear,
        Boolean allowForever,
        Integer priceForever,
        String[] description,
        String detailedDescription,
        String imageUrl,
        String imageBase64
) {
}
