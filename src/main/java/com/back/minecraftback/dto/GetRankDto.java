package com.back.minecraftback.dto;

public record GetRankDto(
    Long id,
    String title,
    String imageUrl,
    Integer priceMonth,
    Integer priceThreeMonths,
    Integer priceYear,
    Boolean allowForever,
    Integer priceForever,
    String[] description,
    Boolean active
) {
}
