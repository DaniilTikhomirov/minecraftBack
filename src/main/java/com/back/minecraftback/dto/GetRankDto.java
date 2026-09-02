package com.back.minecraftback.dto;

public record GetRankDto(
    Long id,
    String title,
    String imageUrl,
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
    Boolean active
) {
}
