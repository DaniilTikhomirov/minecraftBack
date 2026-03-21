package com.back.minecraftback.dto;

public record GetCasesDto(
    Long id,
    String title,
    String subtitle,
    String description,
    String detailedDescription,
    String imageUrl,
    Integer price,
    Boolean active
) {
}
